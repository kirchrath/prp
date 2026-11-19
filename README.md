# Home Services – Vorrat

Selbst gehostete Dienste für den heimischen Docker-Host. Erster Dienst: **Vorrat** – ein Inventar für
Verbrauchsmaterial (Lebensmittel, Putzmittel, …) mit hierarchischen Lagerorten, Barcode-Scan per
ESP32-Scanner oder Handykamera, MHD-Verwaltung, Mindestbestand/Einkaufsliste und Bewegungshistorie.

```
infra/                 Traefik, Keycloak, Mosquitto (gemeinsam für alle Dienste)
inventory/
  backend/             Spring Boot 4.1 · Java 25 · PostgreSQL · Flyway
  frontend/            React · TypeScript · Vite · Bulma · generierter OpenAPI-Client
  openapi/openapi.json OpenAPI-Dokument (vom Backend exportiert)
  docker-compose.yml
scanner/firmware/      ESP32-Firmware (PlatformIO/Arduino)
docs/                  Architektur, MQTT-Protokoll, Scanner-Hardware
```

## Architektur in Kürze

```
Browser/Handy ──HTTPS──▶ Traefik ──▶ inventory-frontend (nginx) ──/api──▶ inventory-backend ──▶ PostgreSQL
      │                    └──────▶ Keycloak (OIDC, PKCE)            ▲  JWT-Prüfung (JWKS intern)
      └──── Login ─────────────────────┘                             │
ESP32-Scanner ──MQTT (1883)──▶ Mosquitto ◀───────────────────────────┘
```

Details: [docs/architecture.md](docs/architecture.md) · MQTT: [docs/mqtt.md](docs/mqtt.md) ·
Hardware: [docs/scanner-hardware.md](docs/scanner-hardware.md)

## Inbetriebnahme auf dem Docker-Host

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

```bash
docker compose up -d
scripts/mqtt-user.sh add inventory-backend      # Passwort notieren -> inventory/.env MQTT_PASSWORD
```

Keycloak-Admin (`https://AUTH_HOST`, Zugang aus `.env`): Im Realm **home** unter *Users* die
Benutzer anlegen (Passwort unter *Credentials*). Der Client `inventory-frontend` wird beim ersten Start
importiert.

### 3. Vorrat

```bash
cd inventory
cp .env.example .env              # Hosts, DB-Passwort, MQTT_PASSWORD
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

Voraussetzungen: JDK 25, Maven, Node 22+, PostgreSQL (lokal oder Container).

```bash
# Backend (ohne Keycloak/MQTT)
cd inventory/backend
SECURITY_ENABLED=false mvn spring-boot:run      # DB: localhost:5432/inventory, user/pass inventory

# Tests (eingebettetes PostgreSQL, kein Docker nötig) – exportiert auch inventory/openapi/openapi.json
mvn verify

# Frontend
cd ../frontend
npm install
npm run generate-api      # TS-Client aus ../openapi/openapi.json neu erzeugen (benötigt Java)
npm run dev               # http://localhost:5173, /api wird auf :8080 weitergeleitet
```

Für die Entwicklung ohne Login in `frontend/public/config.js` den `oidc`-Block entfernen.

Der API-Client unter `frontend/src/api/generated` ist eingecheckt, damit der Frontend-Container ohne Java
gebaut werden kann. Ablauf bei API-Änderungen: Backend ändern → `mvn verify` → `npm run generate-api` →
beides committen.
