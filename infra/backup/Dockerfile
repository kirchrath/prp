# Backup of the home server: PostgreSQL dumps (inventory, keycloak), ntfy accounts, configuration.
# pg_dump/pg_restore come with the postgres image (same major version as the databases).
FROM postgres:18.7-alpine
RUN apk add --no-cache restic sqlite tzdata
COPY backup.sh restore.sh entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/backup.sh /usr/local/bin/restore.sh /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["cron"]
