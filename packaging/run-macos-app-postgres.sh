#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Acest script este doar pentru macOS."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_PATH="${ROOT_DIR}/packaging/dist/Church Administration Platform.app"
APP_BIN="${APP_PATH}/Contents/MacOS/Church Administration Platform"
RUN_LOG="${HOME}/ChurchAdministrationPlatform/logs/ministryadmin-launch-postgres.log"

# PostgreSQL defaults (override via env vars if needed)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-church_db_container}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-pass}"

echo "1/4 Verificare runtime app..."
if [[ ! -x "${APP_BIN}" ]]; then
  echo "Nu găsesc executabilul app bundle:"
  echo "  BIN: ${APP_BIN}"
  exit 1
fi

echo "2/4 Oprire instanțe vechi..."
pkill -f "${APP_BIN}" >/dev/null 2>&1 || true
# Free default app port if another stale process is listening.
if command -v lsof >/dev/null 2>&1; then
  PORT_PIDS="$(lsof -ti tcp:8085 || true)"
  if [[ -n "${PORT_PIDS}" ]]; then
    echo "Eliberez portul 8085 (PID: ${PORT_PIDS})..."
    kill ${PORT_PIDS} >/dev/null 2>&1 || true
    sleep 1
  fi
fi

echo "3/5 Verificare PostgreSQL și bază..."
if command -v psql >/dev/null 2>&1; then
  if ! PGPASSWORD="${DB_PASS}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -tAc "SELECT 1" >/dev/null 2>&1; then
    echo "Nu mă pot conecta la PostgreSQL cu user/parola curente."
    echo "Setează DB_USER/DB_PASS corect și rulează din nou."
    exit 1
  fi

  DB_EXISTS="$(PGPASSWORD="${DB_PASS}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" || true)"
  if [[ "${DB_EXISTS}" != "1" ]]; then
    echo "Baza '${DB_NAME}' nu există. O creez acum..."
    PGPASSWORD="${DB_PASS}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -c "CREATE DATABASE \"${DB_NAME}\";" >/dev/null
    echo "Baza '${DB_NAME}' a fost creată."
  fi
else
  echo "Atenție: comanda 'psql' lipsește. Sar peste verificare/creare DB."
  echo "Aplicația va porni direct pe '${DB_NAME}' și poate eșua dacă baza nu există."
fi

echo "4/5 Pornire pe PostgreSQL (${DB_HOST}:${DB_PORT}/${DB_NAME})..."
mkdir -p "${HOME}/ChurchAdministrationPlatform/logs"

launch_app() {
  local user="$1"
  local pass="$2"
  SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  SPRING_DATASOURCE_USERNAME="${user}" \
  SPRING_DATASOURCE_PASSWORD="${pass}" \
  SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.PostgreSQLDialect" \
  SPRING_PROFILES_ACTIVE="desktop" \
  nohup "${APP_BIN}" >"${RUN_LOG}" 2>&1 </dev/null &
}

launch_app "${DB_USER}" "${DB_PASS}"

sleep 2

echo "5/5 Verificare proces..."
if pgrep -f "${APP_BIN}" >/dev/null 2>&1; then
  echo "Aplicația a fost lansată pe PostgreSQL."
  echo "Log: ${RUN_LOG}"
  echo "URL: http://localhost:8085"
else
  if [[ -z "${DB_USER:-}" || "${DB_USER}" == "root" ]]; then
    if grep -q 'password authentication failed for user "root"' "${RUN_LOG}" 2>/dev/null; then
      echo "Autentificarea cu root/pass a eșuat. Încerc fallback postgres/postgres..."
      launch_app "postgres" "postgres"
      sleep 2
      if pgrep -f "${APP_BIN}" >/dev/null 2>&1; then
        echo "Aplicația a fost lansată cu fallback postgres/postgres."
        echo "Log: ${RUN_LOG}"
        echo "URL: http://localhost:8085"
        exit 0
      fi
    fi
  fi
  echo "Aplicația nu a rămas pornită. Verifică:"
  echo "  tail -n 120 ${RUN_LOG}"
fi
