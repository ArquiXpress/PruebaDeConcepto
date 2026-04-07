#!/bin/sh
set -eu

PRIMARY_HOST="postgres"
REPLICA_HOST="postgres-replica"
STATE_FILE="/state/active_host"
PROMOTED_FLAG="/state/promoted"

apk add --no-cache postgresql16-client >/dev/null

echo "postgres" > "$STATE_FILE"
rm -f "$PROMOTED_FLAG"

while true; do
  if pg_isready -h "$PRIMARY_HOST" -U arquixpress >/dev/null 2>&1; then
    echo "postgres" > "$STATE_FILE"
    rm -f "$PROMOTED_FLAG"
  else
    if [ ! -f "$PROMOTED_FLAG" ]; then
      psql "host=$REPLICA_HOST dbname=arquixpress user=arquixpress password=arquixpress" -c "select pg_promote();" >/dev/null
      echo "postgres-replica" > "$STATE_FILE"
      touch "$PROMOTED_FLAG"
    fi
  fi
  sleep 2
done
