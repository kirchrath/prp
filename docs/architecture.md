# Architektur

## Komponenten

| Komponente | Technik | Aufgabe |
|---|---|---|
| Traefik | v3 | TLS-Terminierung, Routing per Hostname für alle Dienste |
| Keycloak | 26 | OIDC-Provider (Realm `home`), zentrale Anmeldung für künftige Dienste |
| Mosquitto | 2.1 | MQTT-Broker für die ESP32-Scanner, ein Benutzer je Gerät + ACL |
| inventory-frontend | React/Vite, nginx | SPA; nginx liefert sie aus und leitet `/api` an das Backend weiter |
| inventory-backend | Spring Boot 4.1, Java 25 | REST-API, Geschäftslogik, MQTT-Anbindung |
| PostgreSQL | 18 | Datenbank des Inventars (je Dienst eine eigene Instanz) |

Netzwerke: `proxy` (Traefik ↔ Dienste, Backend ↔ Keycloak für JWKS), `mqtt` (Broker ↔ Backend),
dazu je Stack ein internes Netz für die Datenbank.

## Authentifizierung

* Das Frontend meldet sich per Authorization Code Flow mit PKCE (public client `inventory-frontend`) an
  und sendet das Access-Token als Bearer-Token.
* Das Backend ist OAuth2 Resource Server: Signatur über JWKS (`http://keycloak:8080/...`, intern),
  `iss` muss `https://AUTH_HOST/realms/home` sein. Benutzername = `preferred_username`
  (wird in der Historie gespeichert).
* Jeder angemeldete Benutzer des Realms darf alles (Haushalt). Rollen lassen sich später ergänzen.
* Scanner authentifizieren sich am Broker, nicht am Backend. Die ACL erzwingt, dass ein Scanner nur unter
  seiner eigenen Geräte-ID publizieren kann – das Backend vertraut deshalb der ID im Topic.

## Datenmodell

```
Category 1───* Article *───* Barcode (EAN, eindeutig)
                  │
StorageLocation ──┼──* StockBatch (Artikel × Lagerort × MHD|ohne, Menge > 0)
   (parent_id)    │
                  └──* StockMovement (+/-Menge, Typ IN/OUT/CORRECTION, Quelle SCANNER/MOBILE/UI,
                                      Scanner, Benutzer, Zeitpunkt)
Scanner (device_id, Lagerort, online, last_boot_id/last_seq)
ShoppingListItem (Artikel oder Freitext, automatic = aus Mindestbestand)
```

* **Lagerorte** bilden einen Baum beliebiger Tiefe. Löschen nur, wenn leer und ohne Unterorte;
  Verschieben unter sich selbst wird verhindert. Bestandsfilter nach Lagerort schließt Unterorte ein.
* **Bestand** wird in Chargen geführt: gleiche Artikel am gleichen Ort mit gleichem MHD (oder ohne MHD)
  bilden eine Charge. Scans buchen immer ohne MHD; in der UI kann man Teilmengen einer Charge ein MHD
  zuweisen. Auslagern ohne MHD-Angabe entnimmt FIFO: frühestes MHD zuerst, Chargen ohne MHD zuletzt.
* **Unbekannte EAN** beim Einlagern: Abfrage bei Open Food Facts, Open Products Facts, Open Beauty Facts
  (erster Treffer gewinnt). Ohne Treffer entsteht ein Platzhalter-Artikel mit Status `UNKNOWN` (Inbox),
  der Bestand wird trotzdem gebucht. In der Inbox kann man ihn ergänzen oder mit einem vorhandenen Artikel
  zusammenführen (Barcodes, Bestand und Historie wandern mit). Unbekannte EAN beim Auslagern → Fehler.
* **Einkaufsliste**: Fällt der Gesamtbestand eines Artikels unter `minStock`, wird ein automatischer
  Eintrag mit der Fehlmenge gepflegt; ist der Mindestbestand wieder erreicht, verschwindet er.
  Daneben gibt es manuelle Einträge (Freitext).

## Scan-Verarbeitung

```
ESP32 ──scan {ean, direction, seq, bootId}──▶ Mosquitto ──▶ MqttScannerGateway (seriell, in Reihenfolge)
                                                              │
                                                    ScannerService: Gerät registrieren, Duplikate
                                                    (bootId+seq) verwerfen, Lagerort prüfen
                                                              │
                                                    ScanService: IN → Artikel finden/anlegen + 1 einlagern
                                                                 OUT → 1 auslagern (FIFO)
                                                              │
ESP32 ◀──feedback {seq, result, article, quantity}────────────┘
```

Der Kamera-Scan im Browser nutzt denselben `ScanService` über `POST /api/stock/scan`.

## API

Code-first mit springdoc: Das Backend erzeugt das OpenAPI-Dokument; der Test `OpenApiExportTest`
schreibt es nach `inventory/openapi/openapi.json`. Felder von Records sind dort `required`, außer sie
sind mit JSpecify `@Nullable` markiert – so entstehen präzise TypeScript-Typen. Swagger-UI: `/swagger-ui.html`
(nur direkt am Backend-Container, nicht über nginx erreichbar).
