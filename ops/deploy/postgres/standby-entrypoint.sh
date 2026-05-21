#!/bin/sh
set -eu

: "${PRIMARY_HOST:?PRIMARY_HOST is required}"
: "${PRIMARY_PORT:=5432}"
: "${REPL_USER:?REPL_USER is required}"
: "${REPL_PASSWORD:?REPL_PASSWORD is required}"

DATA_DIR="${PGDATA:-/var/lib/postgresql/data}"

if [ ! -s "$DATA_DIR/PG_VERSION" ]; then
  echo "Initializing standby from ${PRIMARY_HOST}:${PRIMARY_PORT}"
  rm -rf "$DATA_DIR"/*
  until PGPASSWORD="$REPL_PASSWORD" pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U "$REPL_USER" >/dev/null 2>&1; do
    echo "Waiting for primary ${PRIMARY_HOST}:${PRIMARY_PORT}"
    sleep 2
  done
  export PGPASSWORD="$REPL_PASSWORD"
  pg_basebackup \
    -h "$PRIMARY_HOST" \
    -p "$PRIMARY_PORT" \
    -D "$DATA_DIR" \
    -U "$REPL_USER" \
    -Fp -Xs -P -R
  chmod 700 "$DATA_DIR"
fi

exec postgres -c hot_standby=on -c listen_addresses='*'
