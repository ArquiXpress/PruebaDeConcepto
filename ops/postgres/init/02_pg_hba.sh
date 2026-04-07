#!/bin/sh
set -eu

echo "host replication repl_user 0.0.0.0/0 trust" >> "$PGDATA/pg_hba.conf"
echo "host all all 0.0.0.0/0 trust" >> "$PGDATA/pg_hba.conf"
