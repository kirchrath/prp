#!/bin/sh
# cron   - run backup.sh on BACKUP_SCHEDULE (default 30 2 * * *, local time TZ)
# backup - run one backup now
# restore <stand> [part] / list - see restore.sh
set -eu
case "${1:-cron}" in
  cron)
    # cron jobs do not inherit the environment: keep it in a file the job sources
    export -p > /etc/backup.env
    echo "${BACKUP_SCHEDULE:-30 2 * * *} . /etc/backup.env; /usr/local/bin/backup.sh > /proc/1/fd/1 2>&1" > /etc/crontabs/root
    echo "Backup scheduled: ${BACKUP_SCHEDULE:-30 2 * * *} ($(date +%Z))"
    exec crond -f -l 8
    ;;
  backup) exec /usr/local/bin/backup.sh ;;
  restore) shift; exec /usr/local/bin/restore.sh "$@" ;;
  list) exec /usr/local/bin/restore.sh list ;;
  *) exec "$@" ;;
esac
