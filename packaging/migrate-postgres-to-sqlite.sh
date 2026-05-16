#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker nu este instalat. Migrarea necesită Docker."
  exit 1
fi

PG_HOST="${PG_HOST:-host.docker.internal}"
PG_PORT="${PG_PORT:-5432}"
PG_DB="${PG_DB:-church_db_container}"
PG_USER="${PG_USER:-root}"
PG_PASS="${PG_PASS:-pass}"
OUT_DB="${OUT_DB:-${HOME}/ChurchAdministrationPlatform/data/ministryadmin.sqlite.db}"

OUT_DIR="$(dirname "${OUT_DB}")"
OUT_FILE="$(basename "${OUT_DB}")"

mkdir -p "${OUT_DIR}"
rm -f "${OUT_DB}"
TMP_LOAD_FILE="${OUT_DIR}/.pgloader-load-sqlite.load"

cat > "${TMP_LOAD_FILE}" <<EOF
LOAD DATABASE
     FROM postgresql://${PG_USER}:${PG_PASS}@${PG_HOST}:${PG_PORT}/${PG_DB}
     INTO sqlite:///out/${OUT_FILE}
 WITH include drop, create tables, create indexes, reset sequences;
EOF

echo "Migrare one-time PostgreSQL -> SQLite"
echo "Source: ${PG_HOST}:${PG_PORT}/${PG_DB} (${PG_USER})"
echo "Target: ${OUT_DB}"

docker run --rm \
  -v "${OUT_DIR}:/out" \
  dimitri/pgloader:latest \
  pgloader /out/$(basename "${TMP_LOAD_FILE}")

rm -f "${TMP_LOAD_FILE}"

if [[ ! -f "${OUT_DB}" ]]; then
  echo "Migrarea s-a terminat fără fișier rezultat: ${OUT_DB}"
  exit 1
fi

if [[ ! -s "${OUT_DB}" ]]; then
  echo "Fișierul SQLite rezultat este gol: ${OUT_DB}"
  exit 1
fi

echo "Migrare finalizată cu succes."
echo "Poți porni aplicația pe SQLite cu:"
echo "  SQLITE_DB_PATH=\"${OUT_DB}\" ${PWD}/packaging/run-sqlite.sh"
