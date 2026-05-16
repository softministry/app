#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Eroare: acest script este doar pentru macOS."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="${ROOT_DIR}/packaging/dist"
TARGET_DIR="${ROOT_DIR}/target"
PACKAGE_INPUT_DIR="${ROOT_DIR}/package-input"
ICON_DIR="${ROOT_DIR}/packaging/icons"

APP_NAME="${APP_NAME:-Church Administration Platform}"
APP_VERSION="${APP_VERSION:-0.1.0}"
APP_VENDOR="${APP_VENDOR:-Church Administration Platform}"
APP_IDENTIFIER="${APP_IDENTIFIER:-ro.churchoffice.churchadministrationplatform}"
APP_DESCRIPTION="${APP_DESCRIPTION:-Church Administration Platform desktop app}"
SIGN_APP="${SIGN_APP:-false}"
SIGNING_IDENTITY="${SIGNING_IDENTITY:-}"
SIGNING_KEYCHAIN="${SIGNING_KEYCHAIN:-}"
NOTARIZE_DMG="${NOTARIZE_DMG:-false}"
APPLE_ID="${APPLE_ID:-}"
TEAM_ID="${TEAM_ID:-}"
APP_PASSWORD="${APP_PASSWORD:-}"
APPSTORE_PROFILE="${APPSTORE_PROFILE:-}"
MAIN_JAR="${TARGET_DIR}/ministryadmin-web-${APP_VERSION}.jar"
ICON_FILE="${ICON_DIR}/MinistryAdmin.icns"
VOLUME_ICON_FILE="${ICON_DIR}/MinistryAdmin-volume.icns"
SVG_ICON_SOURCE="${ROOT_DIR}/src/main/resources/static/img/church-tab.svg"
PNG_ICON_SOURCE="${ICON_DIR}/church-tab.svg.png"
ICONSET_DIR="${ICON_DIR}/MinistryAdmin.iconset"

if ! command -v jpackage >/dev/null 2>&1; then
  echo "Eroare: jpackage nu este disponibil. Instaleaza JDK 21+ (Temurin/OpenJDK)."
  exit 1
fi

echo "Rulez build Maven pentru a include ultimele modificari..."
(cd "${ROOT_DIR}" && mvn -DskipTests package >/dev/null)

rm -rf "${PACKAGE_INPUT_DIR}"
mkdir -p "${PACKAGE_INPUT_DIR}"
cp -f "${MAIN_JAR}" "${PACKAGE_INPUT_DIR}/"

mkdir -p "${ICON_DIR}"

if [[ ! -f "${ICON_FILE}" ]]; then
  if [[ -f "${SVG_ICON_SOURCE}" ]]; then
    echo "Generez iconul .icns din ${SVG_ICON_SOURCE} ..."
    qlmanage -t -s 1024 -o "${ICON_DIR}" "${SVG_ICON_SOURCE}" >/dev/null 2>&1 || true
    if [[ -f "${ICON_DIR}/$(basename "${SVG_ICON_SOURCE}").png" ]]; then
      mv -f "${ICON_DIR}/$(basename "${SVG_ICON_SOURCE}").png" "${PNG_ICON_SOURCE}"
    fi
  fi
  if [[ -f "${PNG_ICON_SOURCE}" ]]; then
    rm -rf "${ICONSET_DIR}"
    mkdir -p "${ICONSET_DIR}"
    sips -z 16 16 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_16x16.png" >/dev/null
    sips -z 32 32 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_16x16@2x.png" >/dev/null
    sips -z 32 32 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_32x32.png" >/dev/null
    sips -z 64 64 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_32x32@2x.png" >/dev/null
    sips -z 128 128 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_128x128.png" >/dev/null
    sips -z 256 256 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_128x128@2x.png" >/dev/null
    sips -z 256 256 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_256x256.png" >/dev/null
    sips -z 512 512 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_256x256@2x.png" >/dev/null
    sips -z 512 512 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_512x512.png" >/dev/null
    sips -z 1024 1024 "${PNG_ICON_SOURCE}" --out "${ICONSET_DIR}/icon_512x512@2x.png" >/dev/null
    iconutil -c icns "${ICONSET_DIR}" -o "${ICON_FILE}"
  fi
fi

if [[ ! -f "${ICON_FILE}" ]]; then
  echo "Eroare: nu pot genera iconul ${ICON_FILE}. Adauga manual un .icns in packaging/icons."
  exit 1
fi

cp -f "${ICON_FILE}" "${VOLUME_ICON_FILE}"

mkdir -p "${DIST_DIR}"
DMG_FILE="${DIST_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
rm -f "${DMG_FILE}"
rm -rf "${DIST_DIR}/${APP_NAME}.app"

jpackage_args=(
  --name "${APP_NAME}" \
  --type dmg \
  --dest "${DIST_DIR}" \
  --input "${PACKAGE_INPUT_DIR}" \
  --main-jar "$(basename "${MAIN_JAR}")" \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --app-version "${APP_VERSION}" \
  --vendor "${APP_VENDOR}" \
  --description "${APP_DESCRIPTION}" \
  --mac-package-name "${APP_NAME}" \
  --mac-package-identifier "${APP_IDENTIFIER}" \
  --resource-dir "${ICON_DIR}" \
  --icon "${ICON_FILE}" \
  --java-options "-Dspring.profiles.active=desktop" \
  --java-options "-Dapple.awt.UIElement=false" \
  --java-options "-Dfile.encoding=UTF-8"
)

if [[ "${SIGN_APP}" == "true" ]]; then
  if [[ -z "${SIGNING_IDENTITY}" ]]; then
    echo "Eroare: SIGN_APP=true dar SIGNING_IDENTITY nu este setat."
    exit 1
  fi
  jpackage_args+=(--mac-sign --mac-signing-key-user-name "${SIGNING_IDENTITY}")
  if [[ -n "${SIGNING_KEYCHAIN}" ]]; then
    jpackage_args+=(--mac-signing-keychain "${SIGNING_KEYCHAIN}")
  fi
fi

jpackage "${jpackage_args[@]}"

if [[ "${NOTARIZE_DMG}" == "true" ]]; then
  if ! command -v xcrun >/dev/null 2>&1; then
    echo "Eroare: xcrun nu este disponibil (necesar pentru notarizare)."
    exit 1
  fi

  if [[ -n "${APPSTORE_PROFILE}" ]]; then
    xcrun notarytool submit "${DMG_FILE}" --keychain-profile "${APPSTORE_PROFILE}" --wait
  else
    if [[ -z "${APPLE_ID}" || -z "${TEAM_ID}" || -z "${APP_PASSWORD}" ]]; then
      echo "Eroare: NOTARIZE_DMG=true dar lipsesc credențiale."
      echo "Setează APPSTORE_PROFILE sau APPLE_ID, TEAM_ID, APP_PASSWORD."
      exit 1
    fi
    xcrun notarytool submit "${DMG_FILE}" \
      --apple-id "${APPLE_ID}" \
      --team-id "${TEAM_ID}" \
      --password "${APP_PASSWORD}" \
      --wait
  fi

  xcrun stapler staple "${DMG_FILE}"
  xcrun stapler validate "${DMG_FILE}"
fi

echo "DMG generat: ${DIST_DIR}"
ls -1 "${DIST_DIR}"/*.dmg 2>/dev/null || true

if [[ -f "${DMG_FILE}" ]]; then
  echo "Verificare Gatekeeper pentru DMG:"
  spctl --assess -vv --type open "${DMG_FILE}" || true
fi
