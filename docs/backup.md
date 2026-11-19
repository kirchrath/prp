# Datensicherung

Der Dienst `backup` im infra-Stack sichert jede Nacht (Standard 02:30 Uhr, Zeitzone `TZ`):

| Was | Datei im Stand | Warum |
|---|---|---|
| Inventar-Datenbank (alle Service-Schemas in einem konsistenten Dump) | `inventory.dump` | Artikel, Bestand, Historie, Rezepte, Einkaufsliste, Geräte, Einstellungen |
| Keycloak-Datenbank | `keycloak.dump` | Benutzer und Passwörter, Realm |
| ntfy-Konten und Tokens | `ntfy-auth.db` | Konten der Haushaltsmitglieder und Service-Tokens |
| Konfiguration: `infra/.env`, `inventory/.env`, Mosquitto-Benutzer/ACL, TLS-Zertifikate | `config.tar.gz` | alles, was nicht im Git liegt |
| Prüfsummen | `SHA256SUMS` | Restore bricht bei beschädigten Dateien ab |

Nicht gesichert wird **Kafka**: Alle Services haben ihre Daten (inkl. lokaler Kopien) in der Datenbank; nach
einem Restore laufen sie ohne Kafka-Wiederherstellung weiter. Die Topics enthalten danach zwar noch Events,
die neuer sind als der Datenbankstand – sie werden aber nicht erneut verarbeitet, solange die Consumer-Gruppen
nicht zurückgesetzt werden.

## Ablage und Aufbewahrung

```
BACKUP_DIR/
  daily/2026-11-19_0230/     7 Stände (BACKUP_KEEP_DAILY)
  weekly/…                   sonntags eine Kopie, 4 Stände (BACKUP_KEEP_WEEKLY)
  monthly/…                  am 1. eine Kopie, 6 Stände (BACKUP_KEEP_MONTHLY)
  restored/…                 ausgepackte Konfiguration nach einem Restore
```

`BACKUP_DIR` in `infra/.env` (Standard `./backups` im infra-Verzeichnis). Für eine Kopie außerhalb des
Rechners ein NAS-Verzeichnis einhängen und dorthin zeigen lassen. Ein Stand ist ohne Kafka/Ollama etwa
0,1–10 MB groß, je nach Datenmenge.

### Optional: verschlüsselt außer Haus (restic)

Mit `RESTIC_REPOSITORY` und `RESTIC_PASSWORD` in `infra/.env` wird jeder Stand zusätzlich mit
[restic](https://restic.net) verschlüsselt und dedupliziert kopiert, mit derselben Aufbewahrung (7/4/6).
Beispiele: `s3:https://s3.eu-central-1.amazonaws.com/bucket/home` (+ `AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`), `b2:bucket:home` (+ `B2_ACCOUNT_ID`, `B2_ACCOUNT_KEY`), `sftp:user@nas:/backup`.
Das Repository wird beim ersten Lauf angelegt. **Das restic-Passwort separat aufbewahren** – ohne es ist die
Kopie nicht lesbar.

## Meldungen

Schlägt eine Sicherung fehl, kommt eine Nachricht mit hoher Priorität auf das ntfy-Topic `backup`;
sonntags zusätzlich eine kurze Erfolgsmeldung (`BACKUP_NOTIFY_SUCCESS`: `weekly`, `always`, `never`).

```bash
infra/scripts/ntfy-user.sh backup             # Konto "backup", gibt NTFY_BACKUP_TOKEN aus -> infra/.env
infra/scripts/ntfy-user.sh grant anna backup  # anna darf das Topic lesen; in der App "backup" abonnieren
```

## Bedienung

```bash
infra/scripts/backup-now.sh                                 # sofort sichern
infra/scripts/restore.sh list                               # vorhandene Stände
infra/scripts/restore.sh daily/2026-11-19_0230              # alles wiederherstellen
infra/scripts/restore.sh daily/2026-11-19_0230 inventory    # nur einen Teil
```

Teile: `inventory`, `keycloak`, `ntfy`, `config`, `all`. Das Skript fragt nach, stoppt die betroffenen
Dienste (z. B. alle Inventar-Services, die Datenbank läuft weiter), prüft die Prüfsummen, spielt den Stand
ein und startet die Dienste wieder. `config` überschreibt nichts, sondern packt die Dateien nach
`BACKUP_DIR/restored/<stand>/` aus – dort vergleichen und von Hand übernehmen.

Neuinstallation auf einem anderen Rechner: Repository klonen, `infra/.env` und `inventory/.env` aus
`config.tar.gz` übernehmen, Stacks starten (legt leere Datenbanken an), dann
`infra/scripts/restore.sh <stand> all`.

## Technik

* Image `infra/backup` auf Basis `postgres:18.7-alpine` (passende `pg_dump`-Version) + restic, sqlite.
* Netzwerke: `keycloak` (keycloak-db), `backup` (Inventar-Datenbank, Alias `inventory-db`), `proxy` (ntfy).
* Getestet mit echten PostgreSQL-18-Datenbanken und ntfy: Sicherung, Aufbewahrung, Fehlermeldung,
  Erfolgsmeldung, Restore aller Teile, Abbruch bei beschädigtem Stand, Zeitplan. **Nicht getestet:** die
  restic-Kopie und der Image-Build (Paketquellen hier nicht erreichbar).
