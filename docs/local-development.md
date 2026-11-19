# Lokal ausprobieren und entwickeln

Es gibt zwei Wege:

* **A – Entwicklungsumgebung**: Infrastruktur in Docker, Services und Frontend direkt aus Maven/Vite.
  Schnell zum Ausprobieren und Entwickeln, ohne TLS und ohne DNS.
* **B – Kompletter Stack wie auf dem Docker-Host**, nur auf dem eigenen Rechner (Traefik, TLS, alle
  Container).

Einen ESP32 braucht keiner der beiden Wege: `dev/simulate-scanner.sh` spielt einen Scanner per MQTT.

## Voraussetzungen

| Werkzeug | Version | wofür |
|---|---|---|
| Docker + Docker Compose | Compose v2.24 oder neuer | Infrastruktur, Testcontainers |
| JDK | 25 | Services (Weg A, Tests) |
| Maven | 3.9 oder neuer | Services |
| Node.js | 22 oder neuer | Frontend |
| mosquitto-clients | beliebig | Scanner-Simulator (`apt install mosquitto-clients`, `brew install mosquitto`) |
| PlatformIO | 6 | nur für die Firmware |
| optional: API-Key Anthropic/OpenAI oder ~8 GB RAM für Ollama | – | nur für Rezept-Importe |

Freie Ports: 5432, 9092, 8180, 1883, optional 11434 (Weg A) bzw. 80, 443, 1883 (Weg B). Arbeitsspeicher: rund 3–4 GB
für alles zusammen (gemessen: Services je ~380 MB, Keycloak ~900 MB, Kafka ~350 MB, PostgreSQL ~140 MB).

## Weg A – Entwicklungsumgebung

### 1. Infrastruktur starten

```bash
docker compose -f dev/docker-compose.yml up -d
```

| Dienst | Adresse | Zugang |
|---|---|---|
| PostgreSQL | `localhost:5432`, DB `inventory` | `inventory` / `inventory` |
| Kafka | `localhost:9092` | – |
| Keycloak | http://localhost:8180 | Admin `admin` / `admin`, App-Benutzer **`dev` / `dev`** (wird automatisch angelegt) |
| Mosquitto | `localhost:1883` | anonym |

### 2. Services starten

Je Service ein Terminal (oder in der IDE die `*Application`-Klasse starten). Die Voreinstellungen in
`application.yml` passen zur Entwicklungsumgebung, es muss nichts konfiguriert werden.

```bash
cd inventory/services/catalog-service  && mvn spring-boot:run   # :8081
cd inventory/services/stock-service    && mvn spring-boot:run   # :8082
cd inventory/services/shopping-service && mvn spring-boot:run   # :8083
cd inventory/services/device-service   && mvn spring-boot:run   # :8084
cd inventory/services/recipe-service   && mvn spring-boot:run   # :8085
```

KI für Rezept-Importe (nur dafür nötig – alles andere funktioniert ohne):

* lokal: `docker compose -f dev/docker-compose.yml --profile ollama up -d` (lädt einmalig `qwen2.5:7b`,
  ~5 GB), dann recipe-service normal starten (`AI_PROVIDER=ollama` ist Standard);
* oder mit API-Key: `AI_PROVIDER=anthropic ANTHROPIC_API_KEY=… mvn spring-boot:run` (bzw. `openai` /
  `OPENAI_API_KEY`).

Bereit, wenn `curl localhost:8081/actuator/health` (bis 8085) `UP` meldet. Die Swagger-UI liegt unter
`http://localhost:808x/swagger-ui.html`.

### 3. Frontend starten

```bash
cd inventory/frontend
npm install
npm run dev
```

http://localhost:5173 öffnen → Keycloak-Login mit **`dev` / `dev`**. Vite leitet `/api/<service>` an die
Ports 8081–8085 weiter, genau wie Traefik im Betrieb.

### 4. Ausprobieren

1. *Einstellungen → Lagerorte*: z. B. „Vorratsschrank“ anlegen.
2. *Artikel*: einen Artikel mit EAN anlegen, oder direkt scannen (unbekannte EANs landen in der Inbox).
3. Scanner simulieren:

```bash
dev/simulate-scanner.sh status                # Scanner "scanner-dev" meldet sich an
# im Browser: Einstellungen → Scanner → Lagerort zuweisen → Speichern
dev/simulate-scanner.sh in  4006381333931     # einlagern; Antwort wie sie der ESP32 bekäme
dev/simulate-scanner.sh out 4006381333931     # auslagern
```

4. *Rezepte*: ein Rezept anlegen (Zutaten Artikeln zuordnen), dann unter *Kochen* die Personenzahl wählen
   und fehlende Zutaten auf die Einkaufsliste setzen – siehe [recipes.md](recipes.md).

Bei einer unbekannten EAN kommt zuerst `PENDING_ARTICLE`, dann `IN_OK_NEW_ARTICLE` (in einer
Produktdatenbank gefunden) bzw. `IN_OK_UNKNOWN_ARTICLE` (Platzhalter in der Inbox). Die Abfrage der
Produktdatenbanken braucht Internetzugang; ohne ihn dauert sie bis zu ~12 s und endet in der Inbox.

4. *Scannen* (Handykamera): Funktioniert am PC mit Webcam unter `localhost`. Vom Handy aus braucht der
   Browser HTTPS – dafür Weg B nutzen. Ohne Kamera: EAN ins Feld „EAN manuell eingeben“ tippen.

### Aufräumen

```bash
docker compose -f dev/docker-compose.yml down -v    # -v löscht auch die Daten
```

## Weg B – Kompletter Stack lokal

Wie in der [README](../README.md#inbetriebnahme-auf-dem-docker-host) beschrieben, mit diesen Abweichungen:

1. Statt DNS die Hostnamen in `/etc/hosts` (Windows: `C:\Windows\System32\drivers\etc\hosts`) eintragen:

   ```
   127.0.0.1  auth.home.arpa inventar.home.arpa
   ```

2. `infra/scripts/create-local-ca.sh` ausführen und `infra/traefik/certs/ca.pem` im Browser/System als
   vertrauenswürdig importieren (sonst scheitert der Keycloak-Login an der Zertifikatsprüfung).
3. MQTT-Benutzer vor dem ersten `docker compose up` anlegen (`infra/scripts/mqtt-user.sh add device-service`),
   in Keycloak einen Benutzer im Realm `home` anlegen.
4. Scanner simulieren mit Zugangsdaten:

   ```bash
   infra/scripts/mqtt-user.sh add scanner-test
   MQTT_USER=scanner-test MQTT_PASSWORD=<passwort> DEVICE=scanner-test dev/simulate-scanner.sh status
   ```

## Tests

```bash
cd inventory/services/<name>-service && mvn verify   # Testcontainers: Docker muss laufen; JaCoCo 100 %
cd inventory/frontend && npm test                    # Vitest, 100 % Coverage
cd scanner/firmware && pio test -e native            # Unity-Tests der Scanner-Logik
```

`mvn verify` schreibt außerdem `inventory/openapi/<service>.json`; danach im Frontend
`npm run generate-api` ausführen, um die TypeScript-Clients zu aktualisieren.
