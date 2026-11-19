#!/usr/bin/env bash
# Manage ntfy accounts (the ntfy container must be running: docker compose up -d ntfy).
#   scripts/ntfy-user.sh service [topic]          account "inventory" for the notification-service:
#                                                 may only write to the topic, prints NTFY_TOKEN
#   scripts/ntfy-user.sh member <user> [topic]    household member: may only read the topic (ntfy app login),
#                                                 password is generated
#   scripts/ntfy-user.sh delete <user>
# topic defaults to "vorrat" (NTFY_TOPIC in inventory/.env)
set -euo pipefail
cd "$(dirname "$0")/.."

action=${1:-}
ntfy() { docker compose exec -T "$@"; }

case "$action" in
  service)
    topic=${2:-vorrat}
    password=$(head -c 18 /dev/urandom | base64 | tr -d '/+=')
    ntfy -e NTFY_PASSWORD="$password" ntfy ntfy user add --role=user inventory >/dev/null 2>&1 \
      || echo "(user inventory exists)"
    ntfy ntfy ntfy access inventory "$topic" write-only
    token=$(ntfy ntfy ntfy token add inventory | grep -o 'tk_[A-Za-z0-9]*')
    echo "Topic:      $topic"
    echo "NTFY_TOKEN=$token   -> inventory/.env"
    ;;
  member)
    user=${2:-}; topic=${3:-vorrat}
    [[ -z "$user" ]] && { sed -n '2,8p' "$0"; exit 1; }
    password=$(head -c 12 /dev/urandom | base64 | tr -d '/+=')
    ntfy -e NTFY_PASSWORD="$password" ntfy ntfy user add --role=user "$user"
    ntfy ntfy ntfy access "$user" "$topic" read-only
    echo "User:     $user"
    echo "Password: $password"
    echo "Topic:    $topic"
    ;;
  delete)
    user=${2:-}
    [[ -z "$user" ]] && { sed -n '2,8p' "$0"; exit 1; }
    ntfy ntfy ntfy user del "$user"
    ;;
  *) sed -n '2,8p' "$0"; exit 1 ;;
esac
