#!/bin/sh
set -eu

: "${APP_DB_NAME:?APP_DB_NAME is required}"
: "${APP_DB_USER:?APP_DB_USER is required}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"
: "${REPL_USER:?REPL_USER is required}"
: "${REPL_PASSWORD:?REPL_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${APP_DB_USER}') THEN
    CREATE ROLE ${APP_DB_USER} LOGIN PASSWORD '${APP_DB_PASSWORD}';
  ELSE
    ALTER ROLE ${APP_DB_USER} WITH LOGIN PASSWORD '${APP_DB_PASSWORD}';
  END IF;
END
\$\$;

DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${REPL_USER}') THEN
    CREATE ROLE ${REPL_USER} WITH REPLICATION LOGIN PASSWORD '${REPL_PASSWORD}';
  ELSE
    ALTER ROLE ${REPL_USER} WITH REPLICATION LOGIN PASSWORD '${REPL_PASSWORD}';
  END IF;
END
\$\$;
SQL

if ! psql --username "$POSTGRES_USER" --dbname postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${APP_DB_NAME}'" | grep -q 1; then
  createdb --username "$POSTGRES_USER" --owner "$APP_DB_USER" "$APP_DB_NAME"
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$APP_DB_NAME" <<SQL
GRANT ALL PRIVILEGES ON DATABASE ${APP_DB_NAME} TO ${APP_DB_USER};
GRANT USAGE, CREATE ON SCHEMA public TO ${APP_DB_USER};
ALTER SCHEMA public OWNER TO ${APP_DB_USER};
SQL

append_hba_once() {
  LINE="$1"
  if ! grep -Fq "$LINE" "$PGDATA/pg_hba.conf"; then
    echo "$LINE" >> "$PGDATA/pg_hba.conf"
  fi
}

append_hba_once "host all all 10.43.0.0/16 scram-sha-256"
append_hba_once "host replication ${REPL_USER} 10.43.0.0/16 scram-sha-256"

cat >> "$PGDATA/postgresql.conf" <<CONF
listen_addresses = '*'
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10
hot_standby = on
wal_keep_size = 512MB
CONF
