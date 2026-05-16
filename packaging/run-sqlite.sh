#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SQLITE_DB_PATH="${SQLITE_DB_PATH:-${HOME}/ChurchAdministrationPlatform/data/ministryadmin.sqlite.db}"

mkdir -p "$(dirname "${SQLITE_DB_PATH}")"

echo "Pornire Church Administration Platform pe profil SQLite..."
echo "DB: ${SQLITE_DB_PATH}"

cd "${ROOT_DIR}"
SPRING_DATASOURCE_URL="jdbc:sqlite:${SQLITE_DB_PATH}" \
SPRING_PROFILES_ACTIVE="sqlite" \
mvn spring-boot:run

