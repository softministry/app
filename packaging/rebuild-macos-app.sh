#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Acest script este doar pentru macOS."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET_DIR="${ROOT_DIR}/target"
DIST_DIR="${ROOT_DIR}/packaging/dist"
PACKAGE_INPUT_DIR="${ROOT_DIR}/package-input"
ICON_DIR="${ROOT_DIR}/packaging/icons"

APP_NAME="${APP_NAME:-Church Administration Platform}"
APP_VERSION="${APP_VERSION:-0.1.0}"
APP_VENDOR="${APP_VENDOR:-Church Administration Platform}"
APP_IDENTIFIER="${APP_IDENTIFIER:-ro.churchoffice.churchadministrationplatform}"
APP_DESCRIPTION="${APP_DESCRIPTION:-Church Administration Platform desktop app}"
MAIN_JAR="${MAIN_JAR:-ministryadmin-web-${APP_VERSION}.jar}"
MAIN_CLASS="${MAIN_CLASS:-org.springframework.boot.loader.launch.JarLauncher}"

echo "1/4 Build Maven (fără teste)..."
(cd "${ROOT_DIR}" && mvn -Dmaven.test.skip=true package)

echo "2/4 Verificare artefact..."
if [[ ! -f "${TARGET_DIR}/${MAIN_JAR}" ]]; then
  echo "Jar-ul nu există: ${TARGET_DIR}/${MAIN_JAR}"
  exit 1
fi

rm -rf "${PACKAGE_INPUT_DIR}"
mkdir -p "${PACKAGE_INPUT_DIR}"
cp -f "${TARGET_DIR}/${MAIN_JAR}" "${PACKAGE_INPUT_DIR}/"

mkdir -p "${DIST_DIR}"
rm -rf "${DIST_DIR}/${APP_NAME}.app"

echo "3/4 Generare app-image cu jpackage..."
jpackage \
  --name "${APP_NAME}" \
  --type app-image \
  --dest "${DIST_DIR}" \
  --input "${PACKAGE_INPUT_DIR}" \
  --main-jar "${MAIN_JAR}" \
  --main-class "${MAIN_CLASS}" \
  --app-version "${APP_VERSION}" \
  --vendor "${APP_VENDOR}" \
  --description "${APP_DESCRIPTION}" \
  --mac-package-name "${APP_NAME}" \
  --mac-package-identifier "${APP_IDENTIFIER}" \
  --resource-dir "${ICON_DIR}" \
  --icon "${ICON_DIR}/MinistryAdmin.icns" \
  --java-options "-Dspring.profiles.active=desktop" \
  --java-options "-Dapple.awt.UIElement=false" \
  --java-options "-Dfile.encoding=UTF-8"

echo "4/4 Gata."
echo "App generată la:"
echo "  ${DIST_DIR}/${APP_NAME}.app"
echo ""
echo "Pentru rulare pe PostgreSQL:"
echo "  ${ROOT_DIR}/packaging/run-macos-app-postgres.sh"

