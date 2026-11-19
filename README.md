# Home Services – Vorrat

Selbst gehostete Dienste für den heimischen Docker-Host. Erster Dienst: **Vorrat** – ein Inventar für
Verbrauchsmaterial (Lebensmittel, Putzmittel, …) mit hierarchischen Lagerorten, Barcode-Scan per
ESP32-Scanner oder Handykamera, MHD-Verwaltung, Mindestbestand/Einkaufsliste, Bewegungshistorie und
**Rezepte** („Was kann ich kochen für x Personen?“, Import per Link oder Freitext mit KI).

```
infra/                         Traefik, Keycloak, Kafka (KRaft), Mosquitto, optional Ollama – gemeinsam für alle Dienste
inventory/
  services/catalog-service/    Artikel, Barcodes, Kategorien, Produktdatenbank-Abfrage
  services/stock-service/      Lagerorte, Bestand (Chargen), Bewegungen, Scan-Buchungen
  services/shopping-service/   Einkaufsliste (manuell + automatisch aus Mindestbestand)
  services/device-service/     ESP32-Scanner, MQTT ↔ Kafka
  services/recipe-service/     Rezepte, Import mit KI (Spring AI), „Was kann ich kochen?“
  frontend/                    React · TypeScript · Vite · Bulma · generierte OpenAPI-Clients
  openapi/<service>.json       OpenAPI-Dokumente (von den Services exportiert)
  docker-compose.yml
scanner/firmware/              ESP32-Firmware (PlatformIO/Arduino) + Logik-Bibliothek mit Unity-Tests
docs/                          Architektur, Events, MQTT-Protokoll, Scanner-Hardware
```

## Architektur in Kürze

```
Browser/Handy ──HTTPS──▶ Traefik ─┬─ /api/catalog  ─▶ catalog-service ─┐
                                  ├─ /api/stock    ─▶ stock-service   ─┤   PostgreSQL „inventory“
                                  ├─ /api/shopping ─▶ shopping-service─┤   (ein Schema je Service)
                                  ├─ /api/device   ─▶ device-service  ─┤
                                  ├─ /api/recipe   ─▶ recipe-service  ─┘ ─▶ KI: Ollama | Anthropic | OpenAI
                                  ├─ /             ─▶ frontend (nginx)
                                  └─ auth.…        ─▶ Keycloak (OIDC/PKCE)
Services ◀──── Kafka (ein Topic je Aggregat, <service>_<aggregat>) ────▶ Services
ESP32-Scanner ──MQTT──▶ Mosquitto ◀──▶ device-service
```

Details: [docs/architecture.md](docs/architecture.md) · MQTT: [docs/mqtt.md](docs/mqtt.md) ·
Hardware & Einkaufsliste: [docs/scanner-hardware.md](docs/scanner-hardware.md) ·
Rezepte & KI: [docs/recipes.md](docs/recipes.md)

## Schnellstart: lokal ausprobieren

Ohne Server, DNS, TLS oder Scanner-Hardware – Schritt für Schritt in
**[docs/local-development.md](docs/local-development.md)**:

```bash
docker compose -f dev/docker-compose.yml up -d                       # PostgreSQL, Kafka, Keycloak, Mosquitto
cd inventory/services/catalog-service && mvn spring-boot:run          # ebenso stock-, shopping-, device-, recipe-service
cd inventory/frontend && npm install && npm run dev                   # http://localhost:5173, Login dev / dev
dev/simulate-scanner.sh status                                       # Scanner ohne Hardware simulieren
```

## Inbetriebnahme auf dem Docker-Host

Anforderungen: Docker Engine mit Compose v2 (≥ 2.24), empfohlen 8 GB RAM (gemessen ~3,5 GB Bedarf),
mit lokaler KI (Ollama) 16 GB; Ports 80, 443 und 1883 frei.

### 1. DNS

`AUTH_HOST` und `INVENTORY_HOST` (z. B. `auth.home.arpa`, `inventar.home.arpa`) müssen im Heimnetz auf
den Docker-Host zeigen (Router/Pi-hole/AdGuard: Wildcard `*.home.arpa → <IP>`).

### 2. Infrastruktur

```bash
cd infra
cp .env.example .env              # Domain, Passwörter, TLS-Variante anpassen
```

TLS – per `.env` wählbar:

* **Lokale CA** (Standard): `scripts/create-local-ca.sh` erzeugt `traefik/certs/cert.pem` für `*.DOMAIN`.
  `traefik/certs/ca.pem` auf Handy/PC als vertrauenswürdige CA installieren – sonst funktionieren
  Kamera-Scan und Login nicht.
* **Let's Encrypt (DNS-01)**: `COMPOSE_FILE=docker-compose.yml:docker-compose.letsencrypt.yml` sowie
  `ACME_EMAIL`, `ACME_DNS_PROVIDER` und die Zugangsdaten des DNS-Providers setzen.

MQTT-Benutzer **vor** dem ersten Start anlegen (Mosquitto startet nicht ohne Passwortdatei):

```bash
scripts/mqtt-user.sh add device-service          # Passwort notieren -> inventory/.env MQTT_PASSWORD
docker compose up -d
# optional lokale KI für Rezept-Importe (lädt einmalig ~5 GB):
docker compose --profile ollama up -d
```

Keycloak-Admin (`https://AUTH_HOST`, Zugang aus `.env`): Im Realm **home** unter *Users* die Benutzer
anlegen (Passwort unter *Credentials*). Der Client `inventory-frontend` wird beim ersten Start importiert.

### 3. Vorrat

```bash
cd inventory
cp .env.example .env              # Hosts, DB-Passwort, MQTT_PASSWORD, AI_PROVIDER (+ API-Key)
docker compose up -d --build
```

Danach `https://INVENTORY_HOST` öffnen, anmelden, unter *Einstellungen → Lagerorte* die Lagerorte anlegen.

### 4. Scanner

1. Pro Scanner einen MQTT-Benutzer anlegen: `infra/scripts/mqtt-user.sh add scanner-kueche`
2. Firmware flashen (`scanner/firmware`, siehe [docs/scanner-hardware.md](docs/scanner-hardware.md)).
3. Der Scanner öffnet beim ersten Start das WLAN `Vorrat-<geräte-id>`; dort WLAN, Broker-Adresse,
   Geräte-ID (= MQTT-Benutzer) und Passwort eintragen.
4. Er erscheint dann unter *Einstellungen → Scanner*; dort einen Lagerort zuweisen.

## Entwicklung

Lokale Umgebung, Tests und Code-Generierung: [docs/local-development.md](docs/local-development.md).

Die API-Clients unter `frontend/src/api/<service>` sind eingecheckt, damit der Frontend-Container ohne
Java gebaut werden kann. Ablauf bei API-Änderungen: Service ändern → `mvn verify` → `npm run generate-api`
→ alles committen.
