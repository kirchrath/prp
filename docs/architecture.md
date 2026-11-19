# Architektur

## Services

| Service | Pfad | Schema | Besitzt (Aggregate) | Lokale Kopien aus |
|---|---|---|---|---|
| catalog-service | `/api/catalog` | `catalog` | Category, Article (+ Barcodes) | – |
| stock-service | `/api/stock` | `stock` | Location, StockBatch, Movement, Booking | `catalog_article` |
| shopping-service | `/api/shopping` | `shopping` | ShoppingItem | `catalog_article`, `stock_batch` (+ `recipe_shopping_request`) |
| device-service | `/api/device` | `device` | Scanner (Scanner und Knopfleisten, + Taster→Artikel), Scan | (reagiert auf `stock_booking`) |
| recipe-service | `/api/recipe` | `recipe` | RecipeCategory, Recipe (+ Zutaten, Schritte, Tags), ShoppingRequest | `catalog_article`, `stock_batch` |
| notification-service | `/api/notification` | `notification` | NotificationSettings (+ Schwellen je Kategorie), Digest | `catalog_article`, `stock_batch` |

Jeder Service ist ein eigenständiges Maven-Projekt (Spring Boot 4.1, Java 25, Image auf Basis Amazon
Corretto). Alle nutzen dieselbe PostgreSQL-Datenbank `inventory`, aber jeweils ein eigenes Schema, das
Flyway anlegt und versioniert. Untereinander kommunizieren die Services ausschließlich über Kafka.

Infrastruktur (`infra/`): Traefik (TLS, Routing nach Pfad), Keycloak (OIDC), Kafka im KRaft-Modus
(ein Broker), Mosquitto (MQTT für die Scanner), optional Ollama (lokale KI für Rezept-Importe).

## Persistenz

* **Spring Data JDBC** mit unveränderlichen **Records** als Aggregate; kein JPA/JPQL. Abfragen sind
  abgeleitete Repository-Methoden oder `Criteria`/`Query` über `JdbcAggregateTemplate`.
* **Primärschlüssel**: UUID, vergeben in einem `BeforeConvertCallback` (Lifecycle-Event).
* **Auditing** an jedem Aggregat: `createdDate`, `lastModifiedDate` (`@EnableJdbcAuditing`) und
  `version` (`@Version`, Optimistic Locking).
* **Soft Delete**: Löschen setzt `deleted = true`; Listen liefern gelöschte Einträge nicht mehr aus, die
  Daten bleiben erhalten.
* Kind-Records gehören zum Aggregat (z. B. `article_barcode` am Artikel) und haben keinen eigenen
  Lebenszyklus.

## API-Konventionen

* Einzelobjekt: `GET /…/{id}` → `{ "audit": AuditDto, "data": XDataDto }` (id steckt im Pfad).
* Listen: `XListDto` mit `id` und nur den angezeigten Feldern.
* Anlegen: `POST { "data": … }` → `201 Created`, `Location`-Header mit der neuen id.
* Ändern: `PUT /…/{id}  { "audit": <unverändert zurückgesendet>, "data": … }` – weicht
  `audit.version` ab → `409 Conflict`.
* Löschen: `DELETE /…/{id}?version=<n>` (Soft Delete).
* OpenAPI code-first (springdoc); Felder sind `required`, außer sie sind mit JSpecify `@Nullable`
  markiert. Das Frontend generiert daraus je Service einen `typescript-fetch`-Client.

## Events (Kafka)

* Ein Topic je Aggregat-Typ, Name `<service>_<aggregat>` (snake_case), z. B. `catalog_article`.
* Nur **Speicher-Events**: jedes Speichern publiziert den kompletten Zustand
  `{ "id", "audit", "data" }` (Key = id), auch Löschungen (`audit.deleted = true`).
* Publiziert wird **nach dem Commit** (`@TransactionalEventListener(AFTER_COMMIT)`).
* Aufbewahrung unbegrenzt (`retention.ms = -1`): ein Service kann seine Kopien jederzeit durch Lesen ab
  Offset 0 neu aufbauen. Kopien übernehmen nur neuere Versionen (`audit.version`), sind also idempotent.
* Jeder Service verarbeitet alle seine Eingangs-Topics in **einem** Listener, also nacheinander.
* **Fehler** blockieren nicht: Eine Nachricht, deren Verarbeitung fehlschlägt, wandert in das Retry-Topic des
  Services (`<topic>_<service>_retry`, z. B. `catalog_article_stock_retry` – nur dieser Service liest es) und
  wird nach 10 s erneut verarbeitet, insgesamt bis zu 4 Versuche. Die nächsten Nachrichten laufen sofort weiter.
  Danach landet sie im **Dead-Letter-Topic** `<service>_dead_letter` (unbegrenzt aufbewahrt) und wird als
  Fehler geloggt (Original-Topic, Key, Fehler). Nicht lesbares JSON geht ohne Wiederholung in den Dead Letter.
  Dead Letters werden nicht automatisch erneut verarbeitet; zum Nachholen die Nachricht von Hand wieder ins
  Original-Topic schreiben (z. B. mit `kafka-console-producer.sh`).
* Weil wiederholte Nachrichten parallel und in anderer Reihenfolge laufen können, ist jedes Speichern über
  Versionen abgesichert: Kopien übernehmen nur eine **neuere** `audit.version` (ältere werden verworfen),
  Entitäten haben Optimistic Locking (`@Version`), und eindeutige Schlüssel (Artikel-/Chargen-ID der Kopien,
  `scan_id` der Buchung, Anfrage-ID der Rezept-Anfragen, ein automatischer Einkaufseintrag je Artikel)
  verhindern doppelte Anlage. Kollisionen enden als Fehler und laufen über das Retry-Topic erneut – dann greift
  die Versions- bzw. Duplikatprüfung. Eine Buchung, die dabei auf „Artikel wird angelegt“ hängen bleibt, wird
  abgeschlossen, sobald der catalog-service den Artikel erneut veröffentlicht (das tut er für jede wartende
  Buchung).

| Topic | Inhalt | Konsumenten |
|---|---|---|
| `catalog_category` | Kategorien | – |
| `catalog_article` | Artikel inkl. EANs, Mindestbestand, Packungsinhalt, `mergedInto` | stock, shopping, recipe, notification |
| `stock_location` | Lagerorte | – |
| `stock_batch` | Chargen (Artikel × Ort × MHD, Menge) | shopping, recipe, notification |
| `stock_movement` | Bewegungen (Historie) | – |
| `stock_booking` | Scan-Buchungen und ihr Ergebnis | catalog, device |
| `shopping_item` | Einkaufsliste | – |
| `device_scanner` | Scanner | – |
| `device_scan` | Scans der ESP32-Scanner | stock |
| `recipe_category` | Rezeptkategorien | – |
| `recipe_recipe` | Rezepte | – |
| `recipe_shopping_request` | fehlende Zutaten eines Rezepts | shopping |
| `notification_settings` | Einstellungen der MHD-Benachrichtigung | – |
| `notification_digest` | tägliche MHD-Zusammenfassung und ihr Versandstatus | – |

## Barcodes (EAN/GTIN)

Alle Eingänge prüfen Barcodes formal – Buchungen (stock-service), Artikel und Produktabfrage (catalog-service),
Scanner-Nachrichten (device-service), das Frontend und die Scanner-Firmware, jeweils mit denselben Regeln
(`common/Gtin.java`, `frontend/src/api/gtin.ts`, `scanner_logic/gtin.cpp`):

* nur Ziffern; EAN-8, UPC-A (12), EAN-13 oder GTIN-14, jeweils mit gültiger Prüfziffer
* UPC-E (8 Stellen, verkürzte UPC-A) wird akzeptiert, wenn der Code keine gültige EAN-8 ist
* Gespeichert und verglichen wird die **normierte** Form, damit derselbe Artikel immer denselben Code hat:
  UPC-A und UPC-E werden mit führender 0 zu EAN-13, GTIN-14 mit führender 0 wird zu EAN-13, EAN-8 bleibt 8-stellig.
  UPC-E-Codes mit Endziffer 5–9 haben immer auch eine gültige EAN-8-Prüfziffer und bleiben deshalb 8-stellig.

Ungültige Codes: REST-Aufrufe antworten mit HTTP 400 („Ungültige EAN …“), Scans eines Scanners werden mit
`INVALID_EAN` abgelehnt (Firmware: rotes Signal, nichts wird gesendet). Beim Update (Flyway) werden vorhandene
12- und 14-stellige Codes normiert; Codes, die keine gültige EAN sind, bleiben stehen und müssen beim nächsten
Bearbeiten des Artikels entfernt werden.

## Abläufe

### Scan am ESP32

```
ESP32 ─mqtt scan─▶ device: Scan{REQUESTED} speichern ─▶ device_scan
                   stock: Booking buchen ─▶ stock_booking (IN_OK, OUT_OK, NOT_IN_STOCK, …)
                   device: Scan{PROCESSED} ─▶ mqtt feedback ─▶ ESP32 (LED/Buzzer)
```

Doppelte MQTT-Zustellungen erkennt der device-service an `bootId` + `seq`, doppelte Kafka-Zustellungen
der stock-service an der Scan-id.

### Unbekannter Barcode beim Einlagern

```
stock: Booking{PENDING_ARTICLE} ─▶ stock_booking ─▶ device: Feedback „warten“
catalog: Produktdatenbank (Open Food/Products/Beauty Facts) oder Platzhalter in der Inbox
         ─▶ catalog_article
stock: Kopie aktualisiert, wartende Buchungen gebucht ─▶ stock_booking (IN_OK_NEW_ARTICLE /
       IN_OK_UNKNOWN_ARTICLE) ─▶ device ─▶ ESP32
```

Der Kamera-Scan im Browser (`POST /api/stock/bookings`) nutzt denselben Ablauf; das Frontend fragt die
Buchung ab, bis sie nicht mehr `PENDING_ARTICLE` ist.

### Rezept: fehlende Zutaten einkaufen

```
recipe: Zutaten × Personen vs. Packungen × Packungsinhalt ─▶ ShoppingRequest ─▶ recipe_shopping_request
shopping: Einträge anlegen bzw. offene Einträge erhöhen, Hinweis „für <Rezept>“, jede Anfrage einmal
```

Details und KI-Anbindung: [recipes.md](recipes.md).

### Artikel zusammenführen (Inbox)

Der catalog-service markiert den Inbox-Artikel als gelöscht mit `mergedInto` und übernimmt die Barcodes
in den Zielartikel. Der stock-service verschiebt beim Empfang den Bestand (Bewegungen vom Typ `MERGE`),
der shopping-service rechnet den Mindestbestand neu.

## Sicherheit

* Frontend: Authorization Code Flow mit PKCE (public client `inventory-frontend`).
* Jeder Service ist OAuth2 Resource Server; JWKS intern von `http://keycloak:8080`, `iss` muss
  `https://AUTH_HOST/realms/home` sein. Benutzername (`preferred_username`) wird an Bewegungen und
  Buchungen gespeichert.
* Scanner authentifizieren sich am Broker; die ACL erlaubt jedem Scanner nur Topics unter seiner
  eigenen Geräte-ID.

## Tests

* Services: Testcontainers (PostgreSQL, Kafka, beim device-service zusätzlich Mosquitto). JaCoCo prüft
  in `mvn verify` 100 % Instruction-, Branch- und Line-Coverage; ausgenommen sind Startklasse,
  Controller, DTOs und Konfiguration – Logik liegt ausschließlich in Services.
* Frontend: Vitest + Testing Library, 100 % Coverage (ohne generierte API-Clients).
* Firmware: Unity-Tests der hardwareunabhängigen Logik (`pio test -e native`).
