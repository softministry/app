#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Acest script este doar pentru macOS."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_PATH="${ROOT_DIR}/packaging/dist/Church Administration Platform.app"
APP_BIN="${APP_PATH}/Contents/MacOS/Church Administration Platform"
RUN_LOG="${HOME}/ChurchAdministrationPlatform/logs/ministryadmin-launch-sqlite.log"
SQLITE_DB_PATH="${SQLITE_DB_PATH:-${HOME}/ChurchAdministrationPlatform/data/ministryadmin.sqlite.db}"

echo "1/4 Verificare app bundle..."
if [[ ! -x "${APP_BIN}" ]]; then
  echo "Nu găsesc executabilul app bundle:"
  echo "  BIN: ${APP_BIN}"
  exit 1
fi

echo "2/4 Oprire instanțe vechi..."
pkill -f "${APP_BIN}" >/dev/null 2>&1 || true
if command -v lsof >/dev/null 2>&1; then
  PORT_PIDS="$(lsof -ti tcp:8085 || true)"
  if [[ -n "${PORT_PIDS}" ]]; then
    echo "Eliberez portul 8085 (PID: ${PORT_PIDS})..."
    kill ${PORT_PIDS} >/dev/null 2>&1 || true
    sleep 1
  fi
fi

echo "3/4 Pornire pe SQLite..."
mkdir -p "${HOME}/ChurchAdministrationPlatform/logs"
mkdir -p "$(dirname "${SQLITE_DB_PATH}")"

SPRING_PROFILES_ACTIVE="sqlite" \
SPRING_DATASOURCE_URL="jdbc:sqlite:${SQLITE_DB_PATH}" \
nohup "${APP_BIN}" >"${RUN_LOG}" 2>&1 </dev/null &

sleep 2

echo "4/4 Verificare proces..."
if pgrep -f "${APP_BIN}" >/dev/null 2>&1; then
  echo "Aplicația a fost lansată pe SQLite."
  echo "DB : ${SQLITE_DB_PATH}"
  echo "Log: ${RUN_LOG}"
  echo "URL: http://localhost:8085"
else
  echo "Aplicația nu a rămas pornită. Verifică:"
  echo "  tail -n 120 ${RUN_LOG}"
  exit 1
fi

