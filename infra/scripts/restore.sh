#!/usr/bin/env bash
# Restore from a backup (see docs/backup.md).
#   scripts/restore.sh list                          available backups
#   scripts/restore.sh <stand> [part]                part: inventory | keycloak | ntfy | config | all (default)
#     e.g. scripts/restore.sh daily/2026-11-19_0230 inventory
# Stops the affected services, restores, starts them again. "config" only unpacks the configuration
# files into <BACKUP_DIR>/restored/<stand>/ for comparison.
set -euo pipefail
cd "$(dirname "$0")/.."

stand=${1:-list}; part=${2:-all}
if [[ "$stand" == list ]]; then
  docker compose run --rm --no-deps -T backup list
  exit 0
fi
case "$part" in inventory|keycloak|ntfy|config|all) ;; *) sed -n '2,8p' "$0"; exit 1 ;; esac
read -r -p "Stand $stand ($part) wiederherstellen? Aktuelle Daten werden überschrieben. [j/N] " answer
[[ "$answer" == j || "$answer" == J ]] || { echo "abgebrochen"; exit 1; }

want() { [[ "$part" == all || "$part" == "$1" ]]; }
inventory_services=()
if want inventory; then
  mapfile -t inventory_services < <(cd ../inventory && docker compose config --services | grep -- '-service$')
  (cd ../inventory && docker compose stop "${inventory_services[@]}")
fi
want keycloak && docker compose stop keycloak
want ntfy && docker compose stop ntfy

docker compose run --rm --no-deps -T backup restore "$stand" "$part"

want ntfy && docker compose start ntfy
want keycloak && docker compose start keycloak
if want inventory; then
  (cd ../inventory && docker compose start "${inventory_services[@]}")
fi
echo "Fertig."
