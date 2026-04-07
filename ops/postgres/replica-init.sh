#!/bin/sh
set -eu

DATA_DIR="/var/lib/postgresql/data"
umask 0077

if [ ! -s "$DATA_DIR/PG_VERSION" ]; then
  rm -rf "$DATA_DIR"/*
  until pg_isready -h "${POSTGRES_MASTER_HOST}" -U "${POSTGRES_REPLICATION_USER}" >/dev/null 2>&1; do
    sleep 1
  done
  export PGPASSWORD="${POSTGRES_REPLICATION_PASSWORD}"
  pg_basebackup \
    -d "host=${POSTGRES_MASTER_HOST} dbname=postgres user=${POSTGRES_REPLICATION_USER}" \
    -D "$DATA_DIR" \
    -U "${POSTGRES_REPLICATION_USER}" \
    -Fp -Xs -P -R
  chmod 700 "$DATA_DIR"
fi

exec postgres
