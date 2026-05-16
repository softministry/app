#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="${ROOT_DIR}/packaging/dist"
TARGET_DIR="${ROOT_DIR}/target"
PACKAGE_INPUT_DIR="${ROOT_DIR}/package-input"
APP_NAME="${APP_NAME:-Church Administration Platform}"
APP_VERSION="${APP_VERSION:-0.1.0}"
APP_IDENTIFIER="${APP_IDENTIFIER:-ro.churchoffice.churchadministrationplatform.sqlite}"
MAIN_JAR="${TARGET_DIR}/ministryadmin-web-${APP_VERSION}.jar"
ICON_FILE="${ROOT_DIR}/packaging/icons/MinistryAdmin.icns"

echo "1/3 Build Maven (skip tests)..."
cd "${ROOT_DIR}"
mvn -Dmaven.test.skip=true package

if [[ ! -f "${MAIN_JAR}" ]]; then
  echo "Jar inexistent: ${MAIN_JAR}"
  exit 1
fi

echo "2/4 Build launcher jar..."
LAUNCHER_SRC_DIR="${ROOT_DIR}/packaging/launcher"
LAUNCHER_BUILD_DIR="${TARGET_DIR}/launcher-build"
LAUNCHER_JAR="${TARGET_DIR}/ministryadmin-launcher.jar"
rm -rf "${LAUNCHER_BUILD_DIR}"
mkdir -p "${LAUNCHER_BUILD_DIR}"
javac -d "${LAUNCHER_BUILD_DIR}" "${LAUNCHER_SRC_DIR}/MinistryAdminLauncher.java"
jar --create --file "${LAUNCHER_JAR}" --main-class ro.church_office.launcher.MinistryAdminLauncher -C "${LAUNCHER_BUILD_DIR}" .

rm -rf "${PACKAGE_INPUT_DIR}"
mkdir -p "${PACKAGE_INPUT_DIR}"
cp -f "${MAIN_JAR}" "${PACKAGE_INPUT_DIR}/"
cp -f "${LAUNCHER_JAR}" "${PACKAGE_INPUT_DIR}/"

mkdir -p "${DIST_DIR}"
rm -f "${DIST_DIR}/${APP_NAME}-${APP_VERSION}.dmg"

echo "3/4 Build DMG with Swing launcher..."
jpackage \
  --name "${APP_NAME}" \
  --type dmg \
  --dest "${DIST_DIR}" \
  --input "${PACKAGE_INPUT_DIR}" \
  --main-jar "$(basename "${LAUNCHER_JAR}")" \
  --main-class ro.church_office.launcher.MinistryAdminLauncher \
  --app-version "${APP_VERSION}" \
  --vendor "Church Administration Platform" \
  --description "Church Administration Platform SQLite Launcher" \
  --mac-package-name "${APP_NAME}" \
  --mac-package-identifier "${APP_IDENTIFIER}" \
  --resource-dir "${ROOT_DIR}/packaging/icons" \
  --icon "${ICON_FILE}" \
  --jlink-options "--strip-debug --no-man-pages --no-header-files" \
  --java-options "-Dapple.awt.UIElement=false" \
  --java-options "-Dfile.encoding=UTF-8"

echo "4/4 Gata."
echo "DMG: ${DIST_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
