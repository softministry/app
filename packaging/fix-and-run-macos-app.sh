#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Acest script este doar pentru macOS."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_PATH="${ROOT_DIR}/packaging/dist/Church Administration Platform.app"
APP_BIN="${APP_PATH}/Contents/MacOS/Church Administration Platform"
RUN_LOG="${HOME}/ChurchAdministrationPlatform/logs/ministryadmin-launch.log"

RUNTIME_CFG="${HOME}/ChurchAdministrationPlatform/config/runtime-mode.properties"
CFG_DIR="${HOME}/ChurchAdministrationPlatform/config"
DATA_DIR="${HOME}/ChurchAdministrationPlatform/data"
DB_MAIN="${DATA_DIR}/ministryadmin-db.mv.db"
DB_TRACE="${DATA_DIR}/ministryadmin-db.trace.db"

timestamp="$(date +%Y%m%d-%H%M%S)"

echo "1/6 Oprire instanțe vechi Church Administration Platform..."
pkill -f "${APP_BIN}" >/dev/null 2>&1 || true

echo "2/6 Verificare aplicație..."
if [[ ! -x "${APP_BIN}" ]]; then
  echo "Nu găsesc executabilul: ${APP_BIN}"
  echo "Asigură-te că ai generat app-ul în packaging/dist."
  exit 1
fi

echo "3/6 Curățare runtime config vechi..."
rm -f "${RUNTIME_CFG}" || true

echo "4/6 Backup bază de date curentă (dacă există)..."
mkdir -p "${DATA_DIR}"
if [[ -f "${DB_MAIN}" ]]; then
  mv "${DB_MAIN}" "${DB_MAIN}.bak-${timestamp}"
  echo "Backup creat: ${DB_MAIN}.bak-${timestamp}"
fi
if [[ -f "${DB_TRACE}" ]]; then
  mv "${DB_TRACE}" "${DB_TRACE}.bak-${timestamp}"
  echo "Backup creat: ${DB_TRACE}.bak-${timestamp}"
fi

echo "4b/6 Setare runtime pe bază nouă (fresh)..."
mkdir -p "${CFG_DIR}"
FRESH_BASE="${DATA_DIR}/ministryadmin-db-fresh-${timestamp}"
cat > "${RUNTIME_CFG}" <<EOF
ministryadmin.desktop.runtime-mode=prod
ministryadmin.desktop.database-file-base=${FRESH_BASE}
EOF
echo "Runtime DB base: ${FRESH_BASE}"

echo "5/6 Eliminare atribut quarantine..."
xattr -dr com.apple.quarantine "${APP_PATH}" >/dev/null 2>&1 || true

echo "6/6 Pornire aplicație..."
mkdir -p "${HOME}/ChurchAdministrationPlatform/logs"
nohup "${APP_BIN}" >"${RUN_LOG}" 2>&1 </dev/null &
sleep 2

if pgrep -f "${APP_BIN}" >/dev/null 2>&1; then
  echo "Aplicația a fost lansată."
else
  echo "Aplicația nu a rămas pornită. Verifică:"
  echo "  tail -n 120 ${RUN_LOG}"
fi

echo ""
echo "Script finalizat."
echo "Dacă nu pornește, verifică logurile:"
echo "  tail -n 120 ${RUN_LOG}"
echo "  tail -n 120 ${HOME}/ChurchAdministrationPlatform/logs/ministryadmin.log"
