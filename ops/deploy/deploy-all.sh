#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INVENTORY="${SCRIPT_DIR}/inventory.env"

if [[ ! -f "$INVENTORY" ]]; then
  cat >&2 <<MSG
Missing ${INVENTORY}.
Copy ops/deploy/inventory.env.example to ops/deploy/inventory.env and edit:
  cp ops/deploy/inventory.env.example ops/deploy/inventory.env
MSG
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$INVENTORY"
set +a

SSH_OPTS=(-i "$SSH_KEY" -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=8)
REMOTE_VMS=("$VM1_FRONTEND_IP" "$VM3_API1_IP" "$VM4_API2_IP" "$VM5_DB1_IP" "$VM6_DB2_IP")

log() {
  echo "[$(date -Iseconds)] $*"
}

ssh_vm() {
  local host="$1"
  shift
  ssh "${SSH_OPTS[@]}" "${SSH_USER}@${host}" "$@"
}

copy_repo_to() {
  local host="$1"
  log "Syncing repository to ${host}:${REMOTE_APP_DIR}"
  ssh_vm "$host" "sudo mkdir -p '${REMOTE_APP_DIR}' && sudo chown '${SSH_USER}:${SSH_USER}' '${REMOTE_APP_DIR}'"
  rsync -az --delete \
    -e "ssh ${SSH_OPTS[*]}" \
    --exclude '.git/' \
    --exclude 'target/' \
    --exclude 'frontend/node_modules/' \
    --exclude 'frontend/dist/' \
    --exclude '*.log' \
    "${REPO_ROOT}/" "${SSH_USER}@${host}:${REMOTE_APP_DIR}/"
}

remote_compose() {
  local host="$1"
  local compose_file="$2"
  shift 2
  ssh_vm "$host" "cd '${REMOTE_APP_DIR}' && docker compose --project-directory '${REMOTE_APP_DIR}' --env-file '${REMOTE_APP_DIR}/ops/deploy/inventory.env' -f '${REMOTE_APP_DIR}/${compose_file}' $*"
}

local_compose() {
  local compose_file="$1"
  shift
  cd "$REPO_ROOT"
  docker compose --project-directory "$REPO_ROOT" --env-file "$INVENTORY" -f "$REPO_ROOT/$compose_file" "$@"
}

wait_port() {
  local host="$1"
  local port="$2"
  local label="$3"
  local retries="${4:-60}"
  log "Waiting for ${label} at ${host}:${port}"
  for _ in $(seq 1 "$retries"); do
    if nc -z "$host" "$port" >/dev/null 2>&1; then
      log "${label} is reachable"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for ${label} at ${host}:${port}" >&2
  return 1
}

wait_http() {
  local url="$1"
  local label="$2"
  local retries="${3:-60}"
  log "Waiting for ${label}: ${url}"
  for _ in $(seq 1 "$retries"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "${label} is healthy"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for ${label}: ${url}" >&2
  return 1
}

update_source() {
  log "Updating source code in ${REPO_ROOT}"
  if [[ ! -d "${REPO_ROOT}/.git" ]]; then
    echo "This script must run from a checked-out repository on VM2." >&2
    echo "Clone ${GIT_REPO_URL} first or place the repository ZIP in ${REMOTE_APP_DIR} and unpack it." >&2
    exit 1
  fi
  if [[ "$GIT_REPO_URL" == *"TU-USUARIO/TU-REPO"* ]]; then
    log "GIT_REPO_URL still has the example value; using the existing checkout."
    git -C "$REPO_ROOT" fetch --all --prune || true
    return
  fi
  git -C "$REPO_ROOT" remote set-url origin "$GIT_REPO_URL"
  git -C "$REPO_ROOT" fetch origin "$GIT_BRANCH"
  git -C "$REPO_ROOT" checkout "$GIT_BRANCH"
  git -C "$REPO_ROOT" pull --ff-only origin "$GIT_BRANCH"
}

validate_local_tools() {
  log "Validating local tools on VM2"
  command -v ssh >/dev/null
  command -v rsync >/dev/null
  command -v docker >/dev/null
  command -v curl >/dev/null
  command -v nc >/dev/null
  docker compose version >/dev/null
}

validate_remote() {
  local host="$1"
  log "Validating SSH and Docker on ${host}"
  ssh_vm "$host" "hostname >/dev/null && command -v docker >/dev/null && docker compose version >/dev/null"
}

main() {
  validate_local_tools
  update_source

  for host in "${REMOTE_VMS[@]}"; do
    validate_remote "$host"
  done

  for host in "${REMOTE_VMS[@]}"; do
    copy_repo_to "$host"
  done

  log "Starting VM5 DB writer + reader1"
  remote_compose "$VM5_DB1_IP" "ops/deploy/vm5-db-writer-reader.compose.yml" up -d --build
  wait_port "$VM5_DB1_IP" 5432 "VM5 writer"
  wait_port "$VM5_DB1_IP" 5433 "VM5 reader1"

  log "Starting VM6 readers"
  remote_compose "$VM6_DB2_IP" "ops/deploy/vm6-db-readers.compose.yml" up -d --build
  wait_port "$VM6_DB2_IP" 5432 "VM6 reader2"
  wait_port "$VM6_DB2_IP" 5433 "VM6 reader3"

  log "Starting VM2 control plane locally"
  local_compose "ops/deploy/vm2-control-plane.compose.yml" up -d --build
  wait_port "$VM2_CONTROL_IP" "${API_LB_PORT}" "VM2 nginx API LB"
  wait_port "$VM2_CONTROL_IP" "${DB_WRITE_PORT}" "VM2 DB write endpoint"
  wait_port "$VM2_CONTROL_IP" "${DB_READ_PORT}" "VM2 DB read endpoint"

  log "Starting VM3 API1"
  remote_compose "$VM3_API1_IP" "ops/deploy/vm3-api.compose.yml" up -d --build
  wait_http "http://${VM3_API1_IP}:${API_HTTP_PORT}/actuator/health" "API1"

  log "Starting VM4 API2"
  remote_compose "$VM4_API2_IP" "ops/deploy/vm4-api.compose.yml" up -d --build
  wait_http "http://${VM4_API2_IP}:${API_HTTP_PORT}/actuator/health" "API2"

  log "Starting VM1 frontend"
  remote_compose "$VM1_FRONTEND_IP" "ops/deploy/vm1-frontend.compose.yml" up -d --build
  wait_http "http://${VM1_FRONTEND_IP}:${FRONTEND_PORT}/" "Frontend"

  log "Running final health checks"
  wait_http "http://${VM2_CONTROL_IP}:${API_LB_PORT}/health" "Nginx LB health"
  wait_http "http://${VM2_CONTROL_IP}:${API_LB_PORT}/actuator/health" "API through LB"
  wait_http "http://${VM2_CONTROL_IP}:${API_LB_PORT}/api/products?page=0&size=1" "Catalog through LB"

  log "Deployment completed"
  cat <<MSG

Frontend:          http://${VM1_FRONTEND_IP}:${FRONTEND_PORT}
API Load Balancer: http://${VM2_CONTROL_IP}:${API_LB_PORT}
DB write endpoint: ${VM2_CONTROL_IP}:${DB_WRITE_PORT}
DB read endpoint:  ${VM2_CONTROL_IP}:${DB_READ_PORT}
MSG
}

main "$@"
