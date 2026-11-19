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
  services/device-service/     ESP32-Scanner und Knopfleisten, MQTT ↔ Kafka
  services/recipe-service/     Rezepte, Import mit KI (Spring AI), „Was kann ich kochen?“
  services/notification-service/  tägliche MHD-Zusammenfassung per ntfy aufs Handy
  frontend/                    React · TypeScript · Vite · Bulma · generierte OpenAPI-Clients
  openapi/<service>.json       OpenAPI-Dokumente (von den Services exportiert)
  docker-compose.yml
scanner/firmware/              ESP32-Firmware für Scanner und Knopfleiste (PlatformIO/Arduino) + Logik mit Unity-Tests
docs/                          Architektur, Events, MQTT-Protokoll, Scanner-Hardware
```

## Architektur in Kürze

```
Browser/Handy ──HTTPS──▶ Traefik ─┬─ /api/catalog  ─▶ catalog-service ─┐
                                  ├─ /api/stock    ─▶ stock-service   ─┤   PostgreSQL „inventory“
                                  ├─ /api/shopping ─▶ shopping-service─┤   (ein Schema je Service)
                                  ├─ /api/device   ─▶ device-service  ─┤
                                  ├─ /api/recipe   ─▶ recipe-service  ─┤ ─▶ KI: Ollama | Anthropic | OpenAI
                                  ├─ /api/notification ─▶ notification-service ─┘ ─▶ ntfy ─▶ Handy
                                  ├─ /             ─▶ frontend (nginx)
                                  ├─ auth.…        ─▶ Keycloak (OIDC/PKCE)
                                  └─ ntfy.…        ─▶ ntfy (Push, Login nötig)
Services ◀──── Kafka (ein Topic je Aggregat, <service>_<aggregat>) ────▶ Services
ESP32-Scanner ──MQTT──▶ Mosquitto ◀──▶ device-service
```

Details: [docs/architecture.md](docs/architecture.md) · MQTT: [docs/mqtt.md](docs/mqtt.md) ·
Hardware & Einkaufsliste (Scanner, Knopfleiste für Gemüse & Co.): [docs/scanner-hardware.md](docs/scanner-hardware.md) ·
Rezepte & KI: [docs/recipes.md](docs/recipes.md) ·
MHD-Benachrichtigungen: [docs/notifications.md](docs/notifications.md) ·
**Konfiguration (alle Einstellungen und Standardwerte): [docs/configuration.md](docs/configuration.md)**

## Schnellstart: lokal ausprobieren

Ohne Server, DNS, TLS oder Scanner-Hardware – Schritt für Schritt in
**[docs/local-development.md](docs/local-development.md)**:

```bash
docker compose -f dev/docker-compose.yml up -d                       # PostgreSQL, Kafka, Keycloak, Mosquitto
cd inventory/services/catalog-service && mvn spring-boot:run          # ebenso stock-, shopping-, device-, recipe-, notification-service
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

### 4. MHD-Benachrichtigungen (optional)

`NTFY_HOST` in DNS und `infra/.env`, dann `infra/scripts/ntfy-user.sh service` (Token → `NTFY_TOKEN` in
`inventory/.env`) und je Person `infra/scripts/ntfy-user.sh member <name>`; die ntfy-App auf dem Handy
abonniert das Topic `vorrat`. Schritt für Schritt: [docs/notifications.md](docs/notifications.md).

### 5. Scanner

1. Pro Scanner einen MQTT-Benutzer anlegen: `infra/scripts/mqtt-user.sh add scanner-kueche`
2. Firmware flashen (`scanner/firmware`, siehe [docs/scanner-hardware.md](docs/scanner-hardware.md)).
3. Der Scanner öffnet beim ersten Start das WLAN `Vorrat-<geräte-id>`; dort WLAN, Broker-Adresse,
   Geräte-ID (= MQTT-Benutzer) und Passwort eintragen.
4. Er erscheint dann unter *Einstellungen → Geräte*; dort einen Lagerort zuweisen.

## KI-Modell-Empfehlung

Die KI wird **nur beim Rezept-Import** gebraucht (Link ohne strukturierte Daten, Freitext, Zuordnung der
Zutaten zu Artikeln) – also ein paar Mal pro Woche, nicht im laufenden Betrieb. Die Aufgabe ist reine
Extraktion in eine feste JSON-Struktur; dafür reichen kleine, günstige Modelle. Große Modelle bringen hier
kaum Mehrwert.

**Kurz:** Ohne GPU im Docker-Host → **remote mit Claude Haiku 5.5** (`AI_PROVIDER=anthropic`, Standardmodell).
Mit GPU (≥ 8 GB VRAM) oder wenn keine Rezeptdaten das Haus verlassen sollen → **lokal mit `qwen3:8b`**.

### Remote

Grundlage der Kosten: ein Import mit bis zu ~10 000 Eingabe-Tokens (Seitentext bis 20 000 Zeichen + bis zu
400 Artikelnamen + Anweisungen) und ~3 000 Ausgabe-Tokens (JSON + Denk-Tokens). Ein Link mit
schema.org-Daten braucht deutlich weniger. Listenpreise Stand November 2026, bitte vor Nutzung prüfen.

| Modell (`…_MODEL`) | Preis Ein-/Ausgabe je 1 Mio. Tokens | ca. je Import | 30 Importe/Monat | Einschätzung |
|---|---|---|---|---|
| **`claude-haiku-5-5`** (Standard) | 0,10 $ / 0,50 $ | **~0,25 ct** | **~0,08 $** | **Empfehlung.** Bestes Kosten-Nutzen-Verhältnis, für Extraktion ausreichend. |
| `claude-sonnet-5-5` | 2 $ / 10 $ | ~5 ct | ~1,50 $ | Wenn Haiku bei unübersichtlichen Freitexten oder der Artikelzuordnung häufiger danebenliegt. |
| `claude-opus-5-5`, `claude-fable-5-2` | 4 $ / 20 $ bzw. 8 $ / 40 $ | ~10–20 ct | 3–6 $ | Nicht nötig – für diese Aufgabe kein spürbarer Gewinn. |
| `gpt-5-mini` (OpenAI, Standard bei `openai`) | 0,25 $ / 2 $ | ~0,9 ct | ~0,25 $ | Gute Alternative, wenn schon ein OpenAI-Konto besteht. |
| `gpt-5-nano` (OpenAI) | 0,05 $ / 0,40 $ | ~0,2 ct | ~0,06 $ | Am günstigsten, aber spürbar schwächer bei der Artikelzuordnung. |

Selbst Sonnet kostet bei normaler Nutzung nur ein bis zwei Dollar im Monat; Haiku bleibt bei Centbeträgen
im Jahr. An den Anbieter gehen nur Rezepttext, Kategorienamen und die Namen/Marken der Katalogartikel.

### Lokal (Ollama, `AI_PROVIDER=ollama`)

| Modell (`OLLAMA_MODEL`) | Download | Speicher | Dauer je Import* | Einsatz |
|---|---|---|---|---|
| `qwen3:4b` | ~2,5 GB | ~4–5 GB RAM | CPU ~1 min | Kleine Hosts (≤ 8 GB RAM). Lange Rezepte werden öfter unvollständig. |
| **`qwen3:8b`** (Standard) | ~5 GB | ~8 GB RAM bzw. 8 GB VRAM | CPU 1–3 min, GPU ~10–20 s | **Empfehlung lokal.** Brauchbare Qualität, läuft auch ohne GPU. |
| `qwen3:14b` | ~9 GB | ~16 GB RAM bzw. 12–16 GB VRAM | CPU 3–6 min, GPU ~20–30 s | Mit passender GPU: zuverlässigere Artikelzuordnung, nahe an Haiku. |

\* grobe Richtwerte, stark abhängig von CPU/GPU und Rezeptlänge.

`OLLAMA_THINK=false` (Standard) schaltet den Denkmodus von qwen3 ab. Das macht den Import um ein Mehrfaches
schneller und reicht für die Strukturierung. Lokal fallen keine API-Kosten an, nur Strom (ein Import auf der
CPU braucht wenige Wattstunden, also Bruchteile eines Cents). Ollama gibt den Speicher nach einigen Minuten
ohne Anfrage wieder frei. **Eine GPU nur für die Rezept-Importe lohnt sich nicht** – Haiku kostet über Jahre
weniger als eine Grafikkarte; lokal lohnt sich, wenn eine GPU ohnehin vorhanden ist oder die Daten zuhause
bleiben sollen.

Modellwechsel: `OLLAMA_MODEL` in `infra/.env` **und** `inventory/.env` setzen, dann
`docker compose --profile ollama up -d` (lädt das neue Modell) und den recipe-service neu starten.
Details zu den Anbietern: [docs/recipes.md](docs/recipes.md#ki-anbindung-spring-ai), alle Einstellungen:
[docs/configuration.md](docs/configuration.md).

## Entwicklung

Lokale Umgebung, Tests und Code-Generierung: [docs/local-development.md](docs/local-development.md).

Die API-Clients unter `frontend/src/api/<service>` sind eingecheckt, damit der Frontend-Container ohne
Java gebaut werden kann. Ablauf bei API-Änderungen: Service ändern → `mvn verify` → `npm run generate-api`
→ alles committen.
