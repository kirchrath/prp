# Konfiguration

Diese Seite beschreibt jede Einstellung: wo sie gesetzt wird, was sie bewirkt und welchen Standardwert sie hat.

## Überblick: Wo wird was konfiguriert?

| Ebene | Datei / Ort | Gilt für |
|---|---|---|
| Infrastruktur | `infra/.env` (Vorlage `infra/.env.example`) | Traefik, Keycloak, Kafka, Mosquitto, Ollama |
| Inventar-Stack | `inventory/.env` (Vorlage `inventory/.env.example`) | Datenbank, alle Services, Frontend |
| Service-Standardwerte | `inventory/services/<name>/src/main/resources/application.yml` | Werte, wenn keine Umgebungsvariable gesetzt ist |
| Keycloak-Realm | `infra/keycloak/realm-home.json` | Login, Client, Token-Laufzeiten (nur beim ersten Start importiert) |
| MQTT-Broker | `infra/mosquitto/config/` (`mosquitto.conf`, `acl`, `passwd`) | Scanner-Anbindung |
| TLS | `infra/traefik/dynamic/tls.yml`, `infra/traefik/certs/`, `docker-compose.letsencrypt.yml` | Zertifikate |
| Entwicklung | `dev/docker-compose.yml` | lokale Infrastruktur auf `localhost` |
| Frontend | Umgebungsvariablen des Containers → `/config.js` | Login-Daten der Oberfläche |
| Scanner | Konfigurationsportal des ESP32, `scanner/firmware/include/pins.h` | WLAN, Broker, Verdrahtung |

Grundregel: Docker Compose liest die `.env`-Datei im jeweiligen Verzeichnis und reicht die Werte an die
Container weiter. Die Services lesen Umgebungsvariablen; ist eine nicht gesetzt, gilt der Standardwert aus
`application.yml` (Schreibweise `${VARIABLE:standard}`). Die Standardwerte in `application.yml` passen zur
lokalen Entwicklungsumgebung (`localhost`), die Compose-Dateien setzen die Werte für den Betrieb in Docker.

Nach einer Änderung an einer `.env`-Datei den Stack neu starten: `docker compose up -d` (Compose erkennt
geänderte Variablen und erstellt die betroffenen Container neu).

---

## 1. Infrastruktur – `infra/.env`

| Variable | Standard (`.env.example`) | Bedeutung |
|---|---|---|
| `DOMAIN` | `home.arpa` | Basisdomain. Wird für das Wildcard-Zertifikat (`*.DOMAIN`) und bei Let's Encrypt für die Zertifikatsdomain verwendet. `home.arpa` ist für Heimnetze reserviert und kollidiert mit keiner echten Domain. |
| `AUTH_HOST` | `auth.home.arpa` | Hostname von Keycloak. Traefik routet ihn zu Keycloak; Keycloak trägt `https://AUTH_HOST` als Aussteller (`iss`) in die Tokens ein. **Muss mit `AUTH_HOST` in `inventory/.env` übereinstimmen.** |
| `INVENTORY_HOST` | `inventar.home.arpa` | Hostname der Inventar-Oberfläche. Wird beim ersten Keycloak-Start in die erlaubten Redirect-URIs des Clients eingetragen. **Muss mit `INVENTORY_HOST` in `inventory/.env` übereinstimmen.** |
| `COMPOSE_FILE` | `docker-compose.yml` | Wählt die TLS-Variante (siehe [TLS](#tls)). Mit `docker-compose.yml:docker-compose.letsencrypt.yml` wird Let's Encrypt aktiv. |
| `ACME_EMAIL` | – (auskommentiert) | Nur Let's Encrypt: E-Mail für das ACME-Konto (Ablaufwarnungen). |
| `ACME_DNS_PROVIDER` | – (auskommentiert) | Nur Let's Encrypt: Kürzel des DNS-Providers für die DNS-01-Prüfung, z. B. `cloudflare`, `hetzner`, `ionos` (Liste in der Traefik-Doku). |
| Provider-Zugangsdaten | – | Nur Let's Encrypt: die vom Provider verlangten Variablen, z. B. `CF_DNS_API_TOKEN` (Cloudflare). Sie werden komplett an Traefik übergeben. |
| `TZ` | `Europe/Berlin` | Zeitzone aller Container (Logs, Zeitstempel). |
| `KEYCLOAK_ADMIN` | `admin` | Benutzername des Keycloak-Administrators (nur beim allerersten Start angelegt). |
| `KEYCLOAK_ADMIN_PASSWORD` | `change-me` | Passwort dazu. **Vor dem ersten Start ändern.** Spätere Änderungen hier wirken nicht – dann in der Keycloak-Admin-Oberfläche ändern. |
| `KEYCLOAK_DB_PASSWORD` | `change-me-too` | Passwort der Keycloak-Datenbank (PostgreSQL im infra-Stack). Wird beim ersten Start der Datenbank gesetzt; später nur zusammen mit der Datenbank änderbar. |
| `MQTT_PORT` | `1883` | Port auf dem Docker-Host, unter dem die Scanner den Broker erreichen. |
| `NTFY_HOST` | `ntfy.home.arpa` | Hostname des ntfy-Servers (Push-Benachrichtigungen). Die ntfy-App auf dem Handy verbindet sich mit `https://NTFY_HOST`. |
| `NTFY_UPSTREAM_BASE_URL` | `https://ntfy.sh` (nicht in `.env.example`) | Nur für iOS: Weckanfragen ohne Inhalt gehen an diesen Server. Leer = kein iOS-Push. |
| `OLLAMA_MODEL` | `qwen3:8b` | Nur mit Profil `ollama`: Modell, das der Dienst `ollama-pull` einmalig herunterlädt. Muss zu `OLLAMA_MODEL` in `inventory/.env` passen. |

### Dienste des infra-Stacks

| Dienst | Feste Einstellungen (in `docker-compose.yml`) | Bedeutung |
|---|---|---|
| **traefik** | Ports 80/443; HTTP wird auf HTTPS umgeleitet; nur Container mit Label `traefik.enable=true` werden veröffentlicht; Netzwerk `proxy` | Einziger Eingang von außen. Routing per Docker-Labels der Dienste. |
| **keycloak** | `start --import-realm`, `KC_HTTP_ENABLED=true`, `KC_PROXY_HEADERS=xforwarded`, `KC_HOSTNAME=https://AUTH_HOST`, `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true` | Läuft intern per HTTP hinter Traefik. Durch „backchannel dynamic“ können die Services die Schlüssel intern über `http://keycloak:8080` holen, während Tokens den öffentlichen Aussteller tragen. |
| **keycloak-db** | PostgreSQL 18, Datenbank/Benutzer `keycloak` | Nur für Keycloak. |
| **kafka** | Apache Kafka 4.2 im KRaft-Modus (Broker + Controller in einem Prozess, ohne ZooKeeper), Listener `kafka:9092`, Replikationsfaktor 1, Standard-Partitionen 1 | Nachrichtenbus der Services. Nicht von außen erreichbar. |
| **mosquitto** | Port `MQTT_PORT`, Konfiguration aus `mosquitto/config` | MQTT-Broker der Scanner (siehe [Mosquitto](#mosquitto-mqtt)). |
| **ollama** (Profil `ollama`) | Volume `ollama`, Netzwerk `proxy`, GPU-Abschnitt auskommentiert | Lokale KI für Rezept-Importe. Nur gestartet mit `docker compose --profile ollama up -d`. |
| **ollama-pull** (Profil `ollama`) | lädt `OLLAMA_MODEL` und beendet sich | Einmaliger Modell-Download. |

### Docker-Netzwerke

| Netzwerk | Angelegt von | Wer hängt dran | Zweck |
|---|---|---|---|
| `proxy` | infra | Traefik, Keycloak, ntfy, Ollama, alle Services, Frontend | Routing durch Traefik; Services holen Keycloak-Schlüssel; recipe-service erreicht Ollama, notification-service erreicht ntfy (`http://ntfy`). |
| `kafka` | infra | Kafka, alle Services | Nachrichtenbus. |
| `mqtt` | infra | Mosquitto, device-service | Scanner-Anbindung. |
| `keycloak` | infra (intern) | Keycloak, keycloak-db | Datenbank von Keycloak. |
| `internal` | inventory (intern) | PostgreSQL `db`, alle Services | Datenbank des Inventars. |

Deshalb muss der infra-Stack vor dem Inventar-Stack laufen: Der Inventar-Stack erwartet die Netzwerke
`proxy`, `kafka` und `mqtt` als „external“.

### TLS

| Variante | Einstellung | Was passiert |
|---|---|---|
| **Lokale CA** (Standard) | `COMPOSE_FILE=docker-compose.yml`, `scripts/create-local-ca.sh` | Das Skript erzeugt eine eigene CA (`traefik/certs/ca.pem`, 10 Jahre gültig) und ein Wildcard-Zertifikat für `DOMAIN` und `*.DOMAIN` (825 Tage – Höchstwert, den Apple-Geräte akzeptieren). `traefik/dynamic/tls.yml` setzt es als Standardzertifikat. `ca.pem` muss auf allen Geräten als vertrauenswürdig installiert werden. Erneutes Ausführen erzeugt ein neues Zertifikat mit derselben CA. |
| **Eigenes Zertifikat** | `cert.pem` und `key.pem` nach `traefik/certs/` kopieren | Wird wie bei der lokalen CA als Standardzertifikat verwendet. |
| **Let's Encrypt (DNS-01)** | `COMPOSE_FILE=docker-compose.yml:docker-compose.letsencrypt.yml`, `ACME_*`, Provider-Zugangsdaten | Traefik holt ein Wildcard-Zertifikat für `DOMAIN`/`*.DOMAIN` per DNS-Prüfung – der Server muss dafür nicht aus dem Internet erreichbar sein. Gespeichert in `traefik/acme/acme.json`, automatisch erneuert. |
| Ohne alles | – | Traefik verwendet ein eigenes selbstsigniertes Zertifikat; Browser warnen, Login und Kamera funktionieren nicht zuverlässig. |

### Keycloak-Realm (`infra/keycloak/realm-home.json`)

Wird **nur beim ersten Start** importiert (existiert der Realm schon, bleibt er unverändert). Spätere
Änderungen in der Admin-Oberfläche unter `https://AUTH_HOST` vornehmen.

| Einstellung | Wert | Bedeutung |
|---|---|---|
| `realm` | `home` | Name des Realms; steckt in allen URLs (`/realms/home`). |
| `registrationAllowed` | `false` | Keine Selbstregistrierung – Benutzer legt der Admin an. |
| `resetPasswordAllowed` | `false` | Kein „Passwort vergessen“ (es ist kein Mailversand eingerichtet). |
| `bruteForceProtected` | `true` | Sperrt Konten nach wiederholten Fehlanmeldungen vorübergehend. |
| `sslRequired` | `external` | HTTPS für Zugriffe von außerhalb privater Netze. |
| `defaultLocale` | `de` | Login-Seite auf Deutsch (Englisch wählbar). |
| `accessTokenLifespan` | 300 s | Gültigkeit eines Access-Tokens; die Oberfläche erneuert es automatisch. |
| `ssoSessionIdleTimeout` | 30 Tage | Ohne Nutzung bleibt man so lange angemeldet. |
| `ssoSessionMaxLifespan` | 90 Tage | Spätestens dann ist eine neue Anmeldung nötig. |
| Client `inventory-frontend` | öffentlicher Client, Authorization Code Flow mit PKCE (S256), keine Passwort-Anmeldung per API | Die Oberfläche meldet sich damit an. Erlaubte Redirects: `https://INVENTORY_HOST/*` und `http://localhost:5173/*` (Entwicklung). |

Rollen gibt es nicht: Jeder Benutzer des Realms darf alles.

### Mosquitto (MQTT)

| Datei | Inhalt | Bedeutung |
|---|---|---|
| `mosquitto.conf` | `listener 1883`, `allow_anonymous false`, `password_file`, `acl_file`, Persistenz in `/mosquitto/data` | Nur angemeldete Clients; Nachrichten mit QoS 1 und „retained“ Status überstehen Neustarts. |
| `passwd` | wird von `scripts/mqtt-user.sh` gepflegt (nicht im Git) | Benutzer und Passwort-Hashes. **Vor dem ersten Start anlegen**, sonst startet Mosquitto nicht. |
| `acl` | `device-service` darf alles unter `inventory/scanner/#`; jeder andere Benutzer nur `inventory/scanner/<eigener-name>/scan` (schreiben), `/status` (schreiben/lesen), `/feedback` (lesen) | Ein Scanner kann sich nicht als anderer Scanner ausgeben. Der MQTT-Benutzername ist daher die Geräte-ID. |

`scripts/mqtt-user.sh add <name> [passwort]` legt Benutzer an (ohne Passwort wird eines erzeugt und
ausgegeben), `delete <name>` entfernt sie; danach lädt Mosquitto die Dateien automatisch neu.

---

## 2. Inventar-Stack – `inventory/.env`

| Variable | Standard (`.env.example`) | Wird verwendet von | Bedeutung |
|---|---|---|---|
| `INVENTORY_HOST` | `inventar.home.arpa` | Traefik-Labels | Hostname der Oberfläche und der APIs (`/api/<service>`). |
| `AUTH_HOST` | `auth.home.arpa` | alle Services, Frontend | Daraus werden der erwartete Token-Aussteller `https://AUTH_HOST/realms/home` und die Login-Adresse der Oberfläche gebildet. |
| `TZ` | `Europe/Berlin` | alle Container | Zeitzone. |
| `DB_PASSWORD` | `change-me` | PostgreSQL `db`, alle Services | Passwort des Datenbankbenutzers `inventory`. Wird beim ersten Start der Datenbank gesetzt. **Vorher ändern.** |
| `MQTT_PASSWORD` | `change-me` | device-service | Passwort des MQTT-Benutzers `device-service` (ausgegeben von `mqtt-user.sh add device-service`). |
| `LOOKUP_ENABLED` | `true` | catalog-service | Unbekannte Barcodes in Open Food Facts, Open Products Facts und Open Beauty Facts nachschlagen. `false`: keine Internetabfragen, unbekannte Barcodes landen direkt in der Inbox. |
| `NTFY_TOPIC` | `vorrat` | notification-service | ntfy-Topic des Haushalts. |
| `NTFY_TOKEN` | leer | notification-service | Token des ntfy-Kontos `inventory` (`infra/scripts/ntfy-user.sh service`). Leer = anonym (scheitert am `deny-all` des infra-Stacks). |
| `AI_PROVIDER` | `ollama` | recipe-service | KI für Rezept-Importe: `ollama`, `anthropic` oder `openai`. Ohne erreichbare KI funktioniert alles außer dem Import. |
| `OLLAMA_MODEL` | `qwen3:8b` | recipe-service | Ollama-Modell. Muss in Ollama vorhanden sein (siehe `OLLAMA_MODEL` im infra-Stack). Empfehlung: [README → KI-Modell](../README.md#ki-modell-empfehlung). |
| `OLLAMA_THINK` | `false` | recipe-service | Denkmodus von „Thinking“-Modellen wie qwen3 (`true`, `false`, `low`, `medium`, `high`). `false` ist deutlich schneller und reicht für die Strukturierung. |
| `ANTHROPIC_API_KEY` | – | recipe-service | API-Key, nur bei `AI_PROVIDER=anthropic`. |
| `ANTHROPIC_MODEL` | `claude-haiku-5-5` | recipe-service | Modellname bei Anthropic. |
| `OPENAI_API_KEY` | – | recipe-service | API-Key, nur bei `AI_PROVIDER=openai`. |
| `OPENAI_MODEL` | `gpt-5-mini` | recipe-service | Modellname bei OpenAI. |

Die Modellnamen sind Vorschläge; eintragen, was das eigene Konto bzw. die Ollama-Installation anbietet.

### Was die Compose-Datei zusätzlich fest setzt

Für alle Services (`x-service-env` in `inventory/docker-compose.yml`):

| Variable | Wert | Bedeutung |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://db:5432/inventory` | Gemeinsame Datenbank; jeder Service arbeitet in seinem eigenen Schema. |
| `DB_USER` | `inventory` | Datenbankbenutzer. |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka im infra-Stack. |
| `OIDC_ISSUER_URI` | `https://AUTH_HOST/realms/home` | Nur Tokens dieses Ausstellers werden akzeptiert. |
| `OIDC_JWK_SET_URI` | `http://keycloak:8080/realms/home/protocol/openid-connect/certs` | Signaturschlüssel werden intern von Keycloak geholt (ohne Umweg über Traefik/TLS). |

Nur device-service: `MQTT_URL=tcp://mosquitto:1883`, `MQTT_USERNAME=device-service`.
Nur recipe-service: `OLLAMA_BASE_URL=http://ollama:11434`.
Frontend: `OIDC_AUTHORITY=https://AUTH_HOST/realms/home`, `OIDC_CLIENT_ID=inventory-frontend`.

Traefik-Routen (Labels): `Host(INVENTORY_HOST) && PathPrefix(/api/catalog|stock|shopping|device|recipe|notification)`
gehen an den jeweiligen Service (Port 8080 im Container), alles andere unter `INVENTORY_HOST` an das
Frontend.

### Weitere Werte ändern (ohne die Compose-Datei anzufassen)

Eine Datei `inventory/docker-compose.override.yml` wird von Compose automatisch mitgelesen:

```yaml
services:
  catalog-service:
    environment:
      CATALOG_LOOKUP_TIMEOUT: 8s
  recipe-service:
    environment:
      RECIPE_MAXARTICLES: "800"
      ANTHROPIC_MODEL: claude-opus-5-5
```

Jede Einstellung aus `application.yml` oder den Tabellen unten lässt sich so als Umgebungsvariable setzen
(Spring „relaxed binding“: Punkte → Unterstriche, Bindestriche weglassen, Großbuchstaben; z. B.
`recipe.max-articles` → `RECIPE_MAXARTICLES`, `catalog.lookup.sources` →
`CATALOG_LOOKUP_SOURCES=https://a,https://b`).

---

## 3. Services – `application.yml`

### Gemeinsam für alle Services

| Einstellung | Umgebungsvariable | Standard | Bedeutung |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | catalog 8081, stock 8082, shopping 8083, device 8084, recipe 8085, notification 8086 (im Container 8080) | HTTP-Port. |
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/inventory` | Datenbank. |
| `spring.datasource.username` / `password` | `DB_USER` / `DB_PASSWORD` | `inventory` / `inventory` | Zugang zur Datenbank. |
| `spring.datasource.hikari.schema` | – | Name des Services (`catalog`, `stock`, …) | Schema, in dem der Service liest und schreibt. |
| `spring.flyway.schemas` / `default-schema` | – | Name des Services | Flyway legt das Schema an und versioniert es (`db/migration/V*.sql`); Migrationen laufen beim Start automatisch. |
| `spring.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka. |
| `spring.kafka.consumer.group-id` | – | Name des Services | Jeder Service liest jedes Topic vollständig in seiner eigenen Gruppe. |
| `spring.kafka.consumer.auto-offset-reset` | – | `earliest` | Eine neue Gruppe liest alle Events ab Beginn – so baut ein neuer oder zurückgesetzter Service seine Kopien vollständig auf. |
| `inventory.kafka.retry.attempts` | `INVENTORY_KAFKA_RETRY_ATTEMPTS` | `4` | Versuche je Nachricht (erster Versuch + Wiederholungen über das Retry-Topic), danach Dead Letter. |
| `inventory.kafka.retry.backoff` | `INVENTORY_KAFKA_RETRY_BACKOFF` | `10s` | Wartezeit zwischen den Versuchen. |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `OIDC_ISSUER_URI` | `http://localhost:8180/realms/home` | Erwarteter Token-Aussteller. |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `OIDC_JWK_SET_URI` | `http://localhost:8180/realms/home/protocol/openid-connect/certs` | Woher die Signaturschlüssel kommen. |
| `spring.threads.virtual.enabled` | – | `true` | Requests laufen auf virtuellen Threads (Java 25). |
| `spring.jackson.default-property-inclusion` | – | `non_null` | Leere Felder werden in JSON weggelassen. |
| `management.endpoints.web.exposure.include` | – | `health,info` | Nur Health/Info über Actuator; `/actuator/health/readiness` nutzt der Docker-Healthcheck. |
| `springdoc.api-docs.path` | – | `/v3/api-docs` | OpenAPI-Dokument; Swagger-UI unter `/swagger-ui.html` (nur direkt am Service, nicht über Traefik). |

Fest im Code (nicht konfigurierbar): Topics werden mit 1 Partition, Replikationsfaktor 1 und
unbegrenzter Aufbewahrung (`retention.ms=-1`) angelegt; Events werden nach dem Datenbank-Commit gesendet.
Je Service kommen Retry-Topics (`<topic>_<service>_retry`, Standard-Aufbewahrung des Brokers) und ein
Dead-Letter-Topic (`<service>_dead_letter`, unbegrenzt) hinzu; nicht lesbares JSON wird nicht wiederholt.

Container (`Dockerfile`): `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` – die JVM
nutzt bis zu 75 % des Container-Speichers und beendet sich bei Speichermangel (Docker startet sie neu).
Ein Speicherlimit lässt sich per `mem_limit` in der Override-Datei setzen.

### catalog-service

| Einstellung | Umgebungsvariable | Standard | Bedeutung |
|---|---|---|---|
| `catalog.lookup.enabled` | `LOOKUP_ENABLED` | `true` | Produktdatenbanken abfragen. |
| `catalog.lookup.sources` | `CATALOG_LOOKUP_SOURCES` | Open Food Facts, Open Products Facts, Open Beauty Facts (`https://world.open…facts.org`) | Abgefragte Datenbanken in dieser Reihenfolge; der erste Treffer zählt. |
| `catalog.lookup.timeout` | `CATALOG_LOOKUP_TIMEOUT` | `4s` | Zeitlimit je Datenbank (Verbindung und Antwort). Im ungünstigsten Fall dauert eine Abfrage 3 × 4 s. |
| `catalog.lookup.user-agent` | `CATALOG_LOOKUP_USERAGENT` | `HomeInventory/0.1 (self-hosted)` | Kennung gegenüber Open Food Facts (deren Nutzungsbedingungen verlangen eine). |

### device-service

| Einstellung | Umgebungsvariable | Standard | Bedeutung |
|---|---|---|---|
| `device.mqtt.url` | `MQTT_URL` | `tcp://localhost:1883` | Broker. |
| `device.mqtt.client-id` | `MQTT_CLIENT_ID` | `device-service` | Basis der MQTT-Client-IDs (`-in` zum Empfangen, `-out` zum Senden). Bei mehreren Instanzen eindeutig wählen. |
| `device.mqtt.username` / `password` | `MQTT_USERNAME` / `MQTT_PASSWORD` | `device-service` / leer | Anmeldung am Broker; leerer Benutzername = anonym (nur Entwicklung). |
| `device.mqtt.topic-prefix` | `MQTT_TOPIC_PREFIX` | `inventory/scanner` | Topic-Präfix; muss zur ACL und zur Firmware passen. |

Fest im Code: QoS 1, automatische Wiederverbindung, Keep-Alive 30 s, Clean Session.

### recipe-service

| Einstellung | Umgebungsvariable | Standard | Bedeutung |
|---|---|---|---|
| `spring.ai.model.chat` | `AI_PROVIDER` | `ollama` | Welcher KI-Anbieter aktiv ist (`ollama`, `anthropic`, `openai`). Embedding-, Bild-, Audio- und Moderationsmodelle sind abgeschaltet. |
| `spring.ai.ollama.base-url` | `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama-Adresse. |
| `spring.ai.ollama.chat.model` / `…chat.think` | `OLLAMA_MODEL` / `OLLAMA_THINK` | `qwen3:8b` / `false` | Ollama-Modell und Denkmodus. |
| `spring.ai.ollama.init.pull-model-strategy` | – | `never` | Der Service lädt kein Modell selbst (das macht `ollama-pull`), damit der Start nicht minutenlang blockiert. |
| `spring.ai.anthropic.api-key` / `…chat.model` | `ANTHROPIC_API_KEY` / `ANTHROPIC_MODEL` | `unset` / `claude-haiku-5-5` | Anthropic-Zugang und Modell. `max-tokens` 16000 (Denk-Tokens zählen mit). |
| `spring.ai.openai.api-key` / `…chat.model` | `OPENAI_API_KEY` / `OPENAI_MODEL` | `unset` / `gpt-5-mini` | OpenAI-Zugang und Modell. |
| `spring.ai.ollama.chat.temperature` | – | `0.1` | Niedrig = möglichst gleichbleibende, sachliche Strukturierung. Bei Anthropic und OpenAI bewusst nicht gesetzt: aktuelle Claude- und GPT-5-Modelle lehnen abweichende Werte mit HTTP 400 ab. |
| `recipe.fetch-timeout` | `RECIPE_FETCHTIMEOUT` | `10s` | Zeitlimit beim Laden einer Rezeptseite. |
| `recipe.user-agent` | `RECIPE_USERAGENT` | `Mozilla/5.0 (compatible; HomeInventory/0.1)` | Kennung beim Laden von Rezeptseiten (manche Portale lehnen unbekannte Clients ab). |
| `recipe.max-page-text` | `RECIPE_MAXPAGETEXT` | `20000` | Zeichen des Seitentexts, die bei Seiten ohne strukturierte Daten an die KI gehen (begrenzt Kosten/Laufzeit). |
| `recipe.max-articles` | `RECIPE_MAXARTICLES` | `400` | Wie viele Katalogartikel (alphabetisch) die KI zur Zuordnung bekommt. |

Fest im Code: Standard-Personenzahl 4, wenn ein Import keine nennt; höchstens 100 Personen; die
Startkategorien (Vorspeise, Hauptgang, Nachtisch, Beilage, Suppe, Salat, Gebäck, Longdrink, Aperitif,
Cocktail) werden nur angelegt, solange es noch gar keine Kategorie gibt.

### notification-service

| Eigenschaft | Umgebungsvariable | Standard | Bedeutung |
|---|---|---|---|
| `inventory.notification.ntfy-url` | `NTFY_URL` | `http://localhost:8090` (Compose: `http://ntfy`) | ntfy-Server. |
| `inventory.notification.ntfy-topic` | `NTFY_TOPIC` | `vorrat` | Topic des Haushalts. |
| `inventory.notification.ntfy-token` | `NTFY_TOKEN` | leer | Zugangstoken (nur Schreibrecht auf das Topic). |
| `inventory.notification.app-url` | `INVENTORY_URL` | `http://localhost:5173` (Compose: `https://INVENTORY_HOST`) | Wird beim Antippen der Nachricht geöffnet (`/benachrichtigungen`). |
| `inventory.notification.retry-after` | `INVENTORY_NOTIFICATION_RETRYAFTER` | `15m` | Wartezeit, bevor eine fehlgeschlagene Tageszusammenfassung erneut gesendet wird. |
| `inventory.notification.max-attempts` | `INVENTORY_NOTIFICATION_MAXATTEMPTS` | `4` | Versuche pro Tag. |
| `inventory.notification.cron` | `INVENTORY_NOTIFICATION_CRON` | `0 * * * * *` | Wie oft geprüft wird, ob die Zusammenfassung fällig ist; `-` schaltet ab. |

Uhrzeit, Schwellen und an/aus werden **in der App** eingestellt (*Einstellungen → Benachrichtigungen*) und
in der Datenbank gespeichert. Standard nach der Installation: an, 08:00 Uhr, „bald“ = 3 Tage, keine
Kategorie-Schwellen. Die Uhrzeit gilt in der Zeitzone `TZ`.

### stock- und shopping-service

Keine eigenen Einstellungen außer den gemeinsamen.

---

## 4. Frontend

| Umgebungsvariable (Container) | Standard | Bedeutung |
|---|---|---|
| `OIDC_AUTHORITY` | leer (Compose setzt `https://AUTH_HOST/realms/home`) | Keycloak-Realm für den Login. Leer = kein Login (nur Testzwecke; die Services lehnen Anfragen ohne Token ab). |
| `OIDC_CLIENT_ID` | `inventory-frontend` | Client im Realm. |

Beim Start schreibt der Container daraus `/config.js` (wird nie gecacht). In der Entwicklung gilt
`frontend/public/config.js` (`http://localhost:8180/realms/home`). Der Vite-Entwicklungsserver
(`vite.config.ts`) leitet `/api/catalog|stock|shopping|device|recipe|notification` an die Ports 8081–8086 weiter.
Im Browser gespeichert: Anmeldedaten (localStorage, von oidc-client-ts) und der zuletzt gewählte Lagerort
auf der Scan-Seite.

---

## 5. Scanner (ESP32)

### Konfigurationsportal

Beim ersten Start, nach dem Löschen der Einstellungen (BOOT-Taste 5 s halten) oder wenn die BOOT-Taste
beim Einschalten gedrückt ist, öffnet der Scanner das WLAN `Vorrat-<geräte-id>`. Dort:

| Feld | Standard | Bedeutung |
|---|---|---|
| WLAN / Passwort | – | Heimnetz. |
| MQTT-Broker | leer | IP oder Hostname des Docker-Hosts. Ohne Eintrag startet immer das Portal. |
| MQTT-Port | `1883` | `MQTT_PORT` des infra-Stacks. |
| Geräte-ID = MQTT-Benutzer | `scanner-<6 Hexziffern der MAC>` | Muss dem mit `mqtt-user.sh` angelegten Benutzer entsprechen. |
| MQTT-Passwort | leer | Passwort dieses Benutzers. |
| Richtung invertieren | `0` | `1` tauscht Ein- und Auslagern, falls die Sensoren andersherum montiert sind. |

Das Portal schließt sich nach 5 Minuten ohne Eingabe; ohne gültiges WLAN startet der Scanner neu. Die
Werte liegen im Flash (NVS, Namensraum `scanner`).

### Fest in der Firmware (Änderung erfordert neues Flashen)

| Wert | Standard | Datei | Bedeutung |
|---|---|---|---|
| Pins | Scanner RX 16 / TX 17, Sensor A 32, Sensor B 33, LED 25/26/27, Buzzer 14, Taste 0; Knopfleiste: Taster 13/16/17/18/19/21/22/23 | `include/pins.h`, `include/button_pins.h` | Verdrahtung, siehe [hardware.md](hardware.md). |
| `SCANNER_BAUD` | 9600 | `include/pins.h` | Baudrate des GM65 (Werkseinstellung). |
| Topic-Präfix | `inventory/scanner/` | `src/main.cpp` | Muss zum device-service passen. |
| Zeitfenster Barcode ↔ Bewegung | 2000 ms | `lib/scanner_logic/src/pairing.h` | Barcode und Bewegung müssen so nah beieinander liegen. |
| Pause für denselben Barcode | 1500 ms | `pairing.h` | Mehrfaches Lesen desselben Codes in dieser Zeit zählt nur einmal. |
| Durchgang abbrechen nach | 2000 ms | `direction.h` | Bleibt etwas vor den Sensoren, wird der Durchgang verworfen. |
| Sensoren frei seit | 80 ms | `direction.h` | Dann gilt ein Durchgang als beendet. |
| Ende eines Barcodes | CR/LF oder 50 ms Pause, max. 64 Zeichen | `barcode_reader.h` | Erkennung im seriellen Datenstrom. |
| Warten auf Antwort | 5 s, bei „Artikel wird angelegt“ 20 s | `feedback_logic.h` | Danach rote LED. |
| Offline-Puffer | 32 Scans | `scan_queue.h` | Scans ohne Verbindung werden gespeichert und beim Verbinden gesendet. |
| LED-Anzeigedauer | 1,5 s | `src/feedback.cpp` | Wie lange eine Rückmeldung leuchtet. |
| MQTT | Keep-Alive 30 s, Puffer 512 Byte, Wiederverbindung alle 3 s | `src/main.cpp` | Verbindung zum Broker. |

---

## 6. Entwicklungsumgebung – `dev/docker-compose.yml`

| Dienst | Adresse | Feste Werte |
|---|---|---|
| PostgreSQL | `localhost:5432` | DB/Benutzer/Passwort `inventory` |
| Kafka | `localhost:9092` | KRaft, ein Broker |
| Keycloak | `http://localhost:8180` | `start-dev`, Admin `admin`/`admin`, Realm-Import mit `INVENTORY_HOST=localhost`; Benutzer `dev`/`dev` wird automatisch angelegt |
| Mosquitto | `localhost:1883` | anonym erlaubt, keine ACL |
| Ollama (Profil `ollama`) | `localhost:11434` | lädt `qwen3:8b` |

Diese Werte entsprechen genau den Standardwerten in `application.yml` und `frontend/public/config.js` –
deshalb starten die Services lokal ohne weitere Konfiguration. **Nur für die Entwicklung verwenden**
(feste, bekannte Passwörter, anonymer Broker).

Der Scanner-Simulator `dev/simulate-scanner.sh` liest `MQTT_HOST` (`localhost`), `MQTT_PORT` (`1883`),
`DEVICE` (`scanner-dev`) und optional `MQTT_USER`/`MQTT_PASSWORD`.

---

## Checkliste vor dem ersten Start

1. `infra/.env`: `DOMAIN`, `AUTH_HOST`, `INVENTORY_HOST`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_DB_PASSWORD`, TLS-Variante.
2. `inventory/.env`: dieselben Hostnamen, `DB_PASSWORD`, `MQTT_PASSWORD`, `AI_PROVIDER` (+ Key oder Ollama).
3. MQTT-Benutzer `device-service` (und je Scanner einen) anlegen – vor `docker compose up`.
4. Werte, die nur beim **ersten** Start wirken: Keycloak-Admin, Realm (Hostnamen!), Datenbankpasswörter.
   Wer sie später ändern will, ändert sie in Keycloak bzw. in der Datenbank oder setzt die Volumes zurück
   (`docker compose down -v` löscht alle Daten).
