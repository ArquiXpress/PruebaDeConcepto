#!/bin/sh
set -eu

PRIMARY_HOST="${PRIMARY_HOST:-postgres}"
REPLICA_HOST="${REPLICA_HOST:-postgres-replica}"
REPLICA_HOSTS="${REPLICA_HOSTS:-$REPLICA_HOST}"
DB_NAME="${DB_NAME:-arquixpress}"
DB_USER="${DB_USER:-arquixpress}"
DB_PASSWORD="${DB_PASSWORD:-arquixpress}"
STATE_FILE="/state/active_host"
PROMOTED_FLAG="/state/promoted"
CURRENT_TARGET=""
SOCAT_PID=""

apk add --no-cache postgresql16-client socat >/dev/null

set_target() {
  TARGET="$1"
  CURRENT="$(cat "$STATE_FILE" 2>/dev/null || true)"
  if [ "$CURRENT" != "$TARGET" ]; then
    echo "$TARGET" > "$STATE_FILE"
  fi
}

start_proxy() {
  TARGET="$(cat "$STATE_FILE")"
  if [ "$TARGET" = "$CURRENT_TARGET" ] && [ -n "$SOCAT_PID" ] && kill -0 "$SOCAT_PID" >/dev/null 2>&1; then
    return
  fi

  if [ -n "$SOCAT_PID" ] && kill -0 "$SOCAT_PID" >/dev/null 2>&1; then
    kill "$SOCAT_PID" >/dev/null 2>&1 || true
    wait "$SOCAT_PID" >/dev/null 2>&1 || true
  fi

  CURRENT_TARGET="$TARGET"
  socat TCP-LISTEN:5432,fork,reuseaddr TCP:"$TARGET":5432 &
  SOCAT_PID=$!
  echo "DB HA Controller routing writes to $TARGET"
}

promote_replica() {
  export PGPASSWORD="$DB_PASSWORD"
  for HOST in $REPLICA_HOSTS; do
    if pg_isready -h "$HOST" -U "$DB_USER" >/dev/null 2>&1; then
      psql "host=$HOST dbname=$DB_NAME user=$DB_USER" -c "select pg_promote();" >/dev/null
      echo "$HOST" > "$PROMOTED_FLAG"
      set_target "$HOST"
      echo "Replica $HOST promoted to writer"
      return 0
    fi
  done
  echo "No healthy replica candidate is available for promotion"
  return 1
}

if [ -f "$PROMOTED_FLAG" ]; then
  set_target "$(cat "$PROMOTED_FLAG")"
else
  set_target "$PRIMARY_HOST"
fi

while true; do
  if [ -f "$PROMOTED_FLAG" ]; then
    set_target "$(cat "$PROMOTED_FLAG")"
  elif pg_isready -h "$PRIMARY_HOST" -U "$DB_USER" >/dev/null 2>&1; then
    set_target "$PRIMARY_HOST"
  else
    echo "Primary $PRIMARY_HOST is down; attempting replica promotion"
    promote_replica || echo "Replica promotion failed; retrying"
  fi

  start_proxy
  sleep 2
done
