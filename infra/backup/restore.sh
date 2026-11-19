#!/bin/sh
# restore.sh list                     available backups (daily/weekly/monthly)
# restore.sh <stand> [part]           part: inventory | keycloak | ntfy | config | all (default)
#   <stand> is e.g. daily/2026-11-19_0230. The calling script (infra/scripts/restore.sh) stops the
#   affected services before and starts them afterwards. "config" only unpacks to
#   /backup/restored/<stand>/ - compare and copy the files yourself.
set -eu
BACKUP_ROOT=${BACKUP_ROOT:-/backup}
SOURCE=${SOURCE:-/source}

if [ "${1:-list}" = list ]; then
  for k in daily weekly monthly; do
    [ -d "$BACKUP_ROOT/$k" ] && ls -1 "$BACKUP_ROOT/$k" | grep -v '^\.' | sed "s#^#$k/#"
  done
  exit 0
fi
stand=$1; part=${2:-all}
dir="$BACKUP_ROOT/$stand"
[ -d "$dir" ] || { echo "no backup $stand"; exit 1; }
(cd "$dir" && sha256sum -c -s SHA256SUMS) || { echo "checksum mismatch in $stand - aborted"; exit 1; }

inv_pw=${INVENTORY_DB_PASSWORD:-}
if [ -z "$inv_pw" ] && [ -f "$SOURCE/inventory.env" ]; then
  inv_pw=$(sed -n 's/^DB_PASSWORD=//p' "$SOURCE/inventory.env" | tail -1)
fi

want() { [ "$part" = all ] || [ "$part" = "$1" ]; }

if want inventory; then
  echo "restoring inventory database from $stand"
  PGPASSWORD="$inv_pw" pg_restore -h "${INVENTORY_DB_HOST:-inventory-db}" -U inventory -d inventory \
    --clean --if-exists --no-owner "$dir/inventory.dump"
fi
if want keycloak; then
  echo "restoring keycloak database from $stand"
  PGPASSWORD="${KEYCLOAK_DB_PASSWORD:-}" pg_restore -h "${KEYCLOAK_DB_HOST:-keycloak-db}" -U keycloak -d keycloak \
    --clean --if-exists --no-owner "$dir/keycloak.dump"
fi
if want ntfy && [ -f "$dir/ntfy-auth.db" ]; then
  echo "restoring ntfy accounts from $stand"
  cp "$dir/ntfy-auth.db" "$SOURCE/ntfy/auth.db"
fi
if want config; then
  target="$BACKUP_ROOT/restored/$stand"
  mkdir -p "$target"
  tar -xzf "$dir/config.tar.gz" -C "$target"
  echo "configuration unpacked to $target (backup directory on the host) - compare and copy by hand"
fi
echo "restore of $stand ($part) done"
