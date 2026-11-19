#!/usr/bin/env bash
# Manage MQTT users (backend and one user per ESP32 scanner).
#   scripts/mqtt-user.sh add <user> [password]   (password is generated if omitted)
#   scripts/mqtt-user.sh delete <user>
set -euo pipefail
cd "$(dirname "$0")/.."

action=${1:-}; user=${2:-}
[[ -z "$action" || -z "$user" ]] && { sed -n '2,4p' "$0"; exit 1; }

passwd_file=mosquitto/config/passwd
touch "$passwd_file"

case "$action" in
  add)
    password=${3:-$(head -c 18 /dev/urandom | base64 | tr -d '/+=')}
    docker compose run --rm --no-deps -T mosquitto \
      mosquitto_passwd -b /mosquitto/config/passwd "$user" "$password"
    echo "User:     $user"
    echo "Password: $password"
    ;;
  delete)
    docker compose run --rm --no-deps -T mosquitto mosquitto_passwd -D /mosquitto/config/passwd "$user"
    ;;
  *) echo "unknown action: $action"; exit 1 ;;
esac
# mosquitto reloads password/acl files on SIGHUP
docker compose kill -s HUP mosquitto >/dev/null 2>&1 || true
