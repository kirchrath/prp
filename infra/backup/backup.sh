#!/bin/sh
# Daily backup: dumps and files into /backup/daily/<stamp>, copies on Sundays to weekly/ and on the 1st
# to monthly/, keeps BACKUP_KEEP_DAILY/WEEKLY/MONTHLY (7/4/6) of each. Optional: restic copy to
# RESTIC_REPOSITORY. Failures (and weekly a success) are reported via ntfy.
set -u
BACKUP_ROOT=${BACKUP_ROOT:-/backup}
SOURCE=${SOURCE:-/source}
stamp=$(date +%Y-%m-%d_%H%M)
work="$BACKUP_ROOT/daily/.$stamp.tmp"

notify() { # priority title message
  [ -n "${NTFY_BACKUP_TOKEN:-}" ] || [ "${NTFY_BACKUP_ANONYMOUS:-false}" = true ] || return 0
  auth=""
  [ -n "${NTFY_BACKUP_TOKEN:-}" ] && auth="Authorization: Bearer $NTFY_BACKUP_TOKEN"
  wget -q -O /dev/null --header "Title: $2" --header "Priority: $1" --header "Tags: floppy_disk" \
    ${auth:+--header "$auth"} --post-data "$3" "${NTFY_URL:-http://ntfy}/${NTFY_BACKUP_TOPIC:-backup}" \
    || echo "ntfy notification failed" >&2
}

fail() {
  echo "Backup FEHLGESCHLAGEN: $1" >&2
  notify high "Backup fehlgeschlagen" "$1 ($stamp)"
  rm -rf "$work"
  exit 1
}

prune() { # dir keep
  [ -d "$1" ] || return 0
  count=$(ls -1 "$1" | wc -l)
  ls -1 "$1" | sort | head -n $((count > $2 ? count - $2 : 0)) | while read -r old; do
    rm -rf "${1:?}/$old"
    echo "removed $1/$old"
  done
}

echo "Backup $stamp started"
mkdir -p "$work" "$BACKUP_ROOT/weekly" "$BACKUP_ROOT/monthly" || fail "Backup-Verzeichnis nicht beschreibbar"

# inventory database (all service schemas in one consistent dump)
inv_pw=${INVENTORY_DB_PASSWORD:-}
if [ -z "$inv_pw" ] && [ -f "$SOURCE/inventory.env" ]; then
  inv_pw=$(sed -n 's/^DB_PASSWORD=//p' "$SOURCE/inventory.env" | tail -1)
fi
PGPASSWORD="$inv_pw" pg_dump -h "${INVENTORY_DB_HOST:-inventory-db}" -U inventory -d inventory -Fc \
  -f "$work/inventory.dump" || fail "Inventar-Datenbank"

# keycloak (users, realm)
PGPASSWORD="${KEYCLOAK_DB_PASSWORD:-}" pg_dump -h "${KEYCLOAK_DB_HOST:-keycloak-db}" -U keycloak -d keycloak -Fc \
  -f "$work/keycloak.dump" || fail "Keycloak-Datenbank"

# ntfy accounts and tokens (SQLite; consistent copy if sqlite3 is available)
if [ -f "$SOURCE/ntfy/auth.db" ]; then
  if command -v sqlite3 >/dev/null; then
    sqlite3 "$SOURCE/ntfy/auth.db" ".backup '$work/ntfy-auth.db'" || fail "ntfy-Konten"
  else
    cp "$SOURCE/ntfy/auth.db" "$work/ntfy-auth.db" || fail "ntfy-Konten"
  fi
fi

# configuration: .env files, MQTT users/ACL, TLS certificates
files=""
for f in infra/.env infra/mosquitto/config infra/traefik/certs infra/traefik/acme inventory.env; do
  [ -e "$SOURCE/$f" ] && files="$files $f"
done
# shellcheck disable=SC2086
tar -czf "$work/config.tar.gz" -C "$SOURCE" $files || fail "Konfigurationsdateien"

(cd "$work" && sha256sum ./* > SHA256SUMS) || fail "Prüfsummen"
mv "$work" "$BACKUP_ROOT/daily/$stamp" || fail "Backup abschließen"
dir="$BACKUP_ROOT/daily/$stamp"
[ "$(date +%u)" = 7 ] && cp -a "$dir" "$BACKUP_ROOT/weekly/$stamp"
[ "$(date +%d)" = 01 ] && cp -a "$dir" "$BACKUP_ROOT/monthly/$stamp"
prune "$BACKUP_ROOT/daily" "${BACKUP_KEEP_DAILY:-7}"
prune "$BACKUP_ROOT/weekly" "${BACKUP_KEEP_WEEKLY:-4}"
prune "$BACKUP_ROOT/monthly" "${BACKUP_KEEP_MONTHLY:-6}"

# optional encrypted copy outside the house
if [ -n "${RESTIC_REPOSITORY:-}" ]; then
  restic cat config >/dev/null 2>&1 || restic init || fail "restic: Repository anlegen"
  (cd "$dir" && restic backup --host home-server --tag inventory .) || fail "restic: Sicherung"
  restic forget --host home-server --keep-daily "${BACKUP_KEEP_DAILY:-7}" --keep-weekly "${BACKUP_KEEP_WEEKLY:-4}" \
    --keep-monthly "${BACKUP_KEEP_MONTHLY:-6}" --prune || fail "restic: Aufräumen"
fi

size=$(du -sh "$dir" | cut -f1)
echo "Backup $stamp done ($size)"
case "${BACKUP_NOTIFY_SUCCESS:-weekly}" in
  always) notify default "Backup ok" "$stamp, $size" ;;
  weekly) [ "$(date +%u)" = 7 ] && notify low "Backup ok" "Wochenmeldung: $stamp, $size" ;;
esac
exit 0
