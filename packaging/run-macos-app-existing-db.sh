#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Acest script este doar pentru macOS."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_PATH="${ROOT_DIR}/packaging/dist/Church Administration Platform.app"
APP_BIN="${APP_PATH}/Contents/MacOS/Church Administration Platform"

CFG_DIR="${HOME}/ChurchAdministrationPlatform/config"
RUNTIME_CFG="${CFG_DIR}/runtime-mode.properties"
DATA_DIR="${HOME}/ChurchAdministrationPlatform/data"
EXISTING_DB_BASE="${DATA_DIR}/ministryadmin.sqlite.db"
RUN_LOG="${HOME}/ChurchAdministrationPlatform/logs/ministryadmin-launch-existing.log"

echo "1/4 Verificare aplicație..."
if [[ ! -x "${APP_BIN}" ]]; then
  echo "Nu găsesc executabilul: ${APP_BIN}"
  exit 1
fi

echo "2/4 Setare runtime pe baza existentă..."
mkdir -p "${CFG_DIR}" "${DATA_DIR}" "${HOME}/ChurchAdministrationPlatform/logs"
cat > "${RUNTIME_CFG}" <<EOF
ministryadmin.desktop.runtime-mode=prod
ministryadmin.desktop.database-file-base=${EXISTING_DB_BASE}
EOF

echo "3/4 Eliminare atribut quarantine..."
xattr -dr com.apple.quarantine "${APP_PATH}" >/dev/null 2>&1 || true

echo "4/4 Pornire aplicație..."
nohup "${APP_BIN}" >"${RUN_LOG}" 2>&1 </dev/null &
sleep 2

if pgrep -f "${APP_BIN}" >/dev/null 2>&1; then
  echo "Aplicația a fost lansată pe baza existentă."
else
  echo "Aplicația nu a rămas pornită. Verifică:"
  echo "  tail -n 120 ${RUN_LOG}"
fi
