#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="${ROOT_DIR}/packaging/dist"
TARGET_DIR="${ROOT_DIR}/target"
PACKAGE_INPUT_DIR="${ROOT_DIR}/package-input"
APP_NAME="Church Administration Platform"
APP_VERSION="${APP_VERSION:-0.1.0}"
MAIN_JAR="${TARGET_DIR}/ministryadmin-web-${APP_VERSION}.jar"

if ! command -v jpackage >/dev/null 2>&1; then
  echo "Eroare: jpackage nu este disponibil. Instaleaza un JDK 21+ (Temurin/OpenJDK)."
  exit 1
fi

if [[ ! -f "${MAIN_JAR}" ]]; then
  echo "Jar-ul nu exista. Rulez build..."
  (cd "${ROOT_DIR}" && mvn -DskipTests package)
fi

rm -rf "${PACKAGE_INPUT_DIR}"
mkdir -p "${PACKAGE_INPUT_DIR}"
cp -f "${MAIN_JAR}" "${PACKAGE_INPUT_DIR}/"

OS="$(uname -s)"
TYPE=""
EXTRA_ARGS=()
case "${OS}" in
  Darwin)
    TYPE="dmg"
    ;;
  Linux)
    TYPE="deb"
    ;;
  MINGW*|MSYS*|CYGWIN*|Windows_NT)
    TYPE="msi"
    EXTRA_ARGS+=(--win-menu --win-shortcut)
    ;;
  *)
    echo "Sistem neacoperit automat: ${OS}. Seteaza manual tipul jpackage."
    exit 1
    ;;
esac

mkdir -p "${DIST_DIR}"

jpackage \
  --name "${APP_NAME}" \
  --type "${TYPE}" \
  --dest "${DIST_DIR}" \
  --input "${PACKAGE_INPUT_DIR}" \
  --main-jar "$(basename "${MAIN_JAR}")" \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --app-version "${APP_VERSION}" \
  --vendor "Church Administration Platform" \
  --description "Church Administration Platform desktop app" \
  --java-options "-Dspring.profiles.active=desktop" \
  --java-options "-Dfile.encoding=UTF-8" \
  "${EXTRA_ARGS[@]}"

echo "Pachet generat in: ${DIST_DIR}"
