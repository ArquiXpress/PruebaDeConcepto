#!/usr/bin/env bash
set -euo pipefail

# Academic best-effort failover controller.
# This is intentionally small for the POC and does not replace Patroni,
# etcd/Consul, fencing with STONITH, synchronous replication, or a managed DB.

: "${SSH_USER:?SSH_USER is required}"
: "${SSH_KEY_PATH:?SSH_KEY_PATH is required}"
: "${APP_DB_NAME:?APP_DB_NAME is required}"
: "${APP_DB_USER:?APP_DB_USER is required}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"
: "${VM5_DB1_IP:?VM5_DB1_IP is required}"
: "${VM6_DB2_IP:?VM6_DB2_IP is required}"
: "${WRITER_HOST:?WRITER_HOST is required}"
: "${WRITER_PORT:?WRITER_PORT is required}"
: "${READER2_HOST:?READER2_HOST is required}"
: "${READER2_PORT:?READER2_PORT is required}"
: "${READER2_CONTAINER:?READER2_CONTAINER is required}"
: "${READER3_HOST:?READER3_HOST is required}"
: "${READER3_PORT:?READER3_PORT is required}"
: "${READER3_CONTAINER:?READER3_CONTAINER is required}"

STATE_DIR=/state
ACTIVE_FILE="$STATE_DIR/active_writer"
PROMOTED_FILE="$STATE_DIR/promoted"
HAPROXY_CFG=/tmp/db-write-lb.cfg
HAPROXY_PID=

mkdir -p "$STATE_DIR"
chmod 600 "$SSH_KEY_PATH" 2>/dev/null || true
export PGPASSWORD="$APP_DB_PASSWORD"

log() {
  echo "[$(date -Iseconds)] $*"
}

write_haproxy_cfg() {
  local host="$1"
  local port="$2"
  cat > "$HAPROXY_CFG" <<CFG
global
  log stdout format raw local0

defaults
  log global
  mode tcp
  timeout connect 3s
  timeout client 30s
  timeout server 30s

frontend pg_write_front
  bind *:15432
  default_backend pg_writer

backend pg_writer
  option tcp-check
  tcp-check connect
  server writer ${host}:${port} check
CFG
}

start_or_reload_haproxy() {
  local host="$1"
  local port="$2"
  write_haproxy_cfg "$host" "$port"
  if [[ -n "${HAPROXY_PID}" ]] && kill -0 "$HAPROXY_PID" >/dev/null 2>&1; then
    haproxy -f "$HAPROXY_CFG" -p /tmp/haproxy.pid -sf "$HAPROXY_PID" &
  else
    haproxy -f "$HAPROXY_CFG" -p /tmp/haproxy.pid &
  fi
  sleep 1
  HAPROXY_PID="$(cat /tmp/haproxy.pid)"
  echo "${host}:${port}" > "$ACTIVE_FILE"
  log "db-ha-controller routing writes to ${host}:${port}"
}

is_primary() {
  local host="$1"
  local port="$2"
  psql "host=${host} port=${port} dbname=${APP_DB_NAME} user=${APP_DB_USER} connect_timeout=2" \
    -tAc "select not pg_is_in_recovery();" 2>/dev/null | grep -q '^t$'
}

ssh_vm() {
  local host="$1"
  shift
  ssh -i "$SSH_KEY_PATH" \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o ConnectTimeout=5 \
    "${SSH_USER}@${host}" "$@"
}

fence_old_writer() {
  log "Attempting best-effort fencing of pg-writer on ${VM5_DB1_IP}"
  ssh_vm "$VM5_DB1_IP" "docker stop pg-writer || true" || log "Fencing failed or VM5 unreachable"
}

promote_candidate() {
  local host="$1"
  local port="$2"
  local container="$3"
  local other_container="$4"

  log "Trying to promote ${container} on ${host}:${port}"
  if ! ssh_vm "$host" "docker exec -u postgres ${container} pg_ctl -D /var/lib/postgresql/data promote"; then
    log "Promotion command failed for ${container}"
    return 1
  fi

  for _ in $(seq 1 20); do
    if is_primary "$host" "$port"; then
      log "${container} is now primary"
      echo "${host}:${port}:${container}" > "$PROMOTED_FILE"
      start_or_reload_haproxy "$host" "$port"
      log "Stopping ${other_container} to avoid serving stale reads after promotion"
      ssh_vm "$host" "docker stop ${other_container} || true" || true
      return 0
    fi
    sleep 2
  done

  log "${container} did not become primary in time"
  return 1
}

failover() {
  fence_old_writer
  if promote_candidate "$READER2_HOST" "$READER2_PORT" "$READER2_CONTAINER" "$READER3_CONTAINER"; then
    return 0
  fi
  if promote_candidate "$READER3_HOST" "$READER3_PORT" "$READER3_CONTAINER" "$READER2_CONTAINER"; then
    return 0
  fi
  log "No reader could be promoted. Manual recovery is required."
  return 1
}

if [[ -f "$PROMOTED_FILE" ]]; then
  IFS=: read -r CURRENT_HOST CURRENT_PORT _ < "$PROMOTED_FILE"
else
  CURRENT_HOST="$WRITER_HOST"
  CURRENT_PORT="$WRITER_PORT"
fi

start_or_reload_haproxy "$CURRENT_HOST" "$CURRENT_PORT"

while true; do
  if is_primary "$CURRENT_HOST" "$CURRENT_PORT"; then
    sleep 5
    continue
  fi

  log "Writer ${CURRENT_HOST}:${CURRENT_PORT} is not reachable as primary"
  if [[ -f "$PROMOTED_FILE" ]]; then
    log "Already promoted once. Manual intervention required."
    sleep 5
    continue
  fi

  if failover; then
    IFS=: read -r CURRENT_HOST CURRENT_PORT _ < "$PROMOTED_FILE"
  fi
  sleep 5
done
