#!/bin/sh
set -eu

DB_HOST="${DB_HOST:?DB_HOST is required}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-nexamind}"
DB_USER="${DB_USER:-nexamind}"
export PGPASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"

echo "Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."
attempt=1
until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; do
    if [ "$attempt" -ge 60 ]; then
        echo "PostgreSQL did not become ready in time" >&2
        exit 1
    fi
    attempt=$((attempt + 1))
    sleep 2
done

if [ "${NEXAMIND_DB_INIT_ENABLED:-true}" = "true" ]; then
    schema_marker="$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc \
        "SELECT to_regclass('public.users')" | tr -d '[:space:]')"

    if [ -z "$schema_marker" ]; then
        echo "Initializing NexaMind database schema..."
        psql -v ON_ERROR_STOP=1 -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
            -f /app/db/01_init.sql
    else
        echo "Database schema already initialized"
    fi
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar --server.port="${PORT:-8088}"
