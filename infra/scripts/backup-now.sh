#!/usr/bin/env bash
# Runs a backup right now (same as the nightly job); the backup service must be running.
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose exec backup /usr/local/bin/backup.sh
