# MQTT-Protokoll Scanner/Knopfleiste ↔ device-service

Präfix: `inventory/scanner/<deviceId>/…` – `deviceId` ist zugleich der MQTT-Benutzername des Scanners
(per ACL erzwungen). Alle Nutzdaten sind JSON.

## `…/status` (Scanner → Backend, retained)

Beim Verbinden:

```json
{"online": true, "fw": "0.1.0", "ip": "192.168.1.50", "rssi": -61, "bootId": "8f3a01c2", "kind": "scanner"}
```

`kind`: `scanner` oder `buttons` (Knopfleiste). Fehlt es (ältere Firmware), gilt das Gerät als Scanner.

Last Will (vom Broker gesendet, wenn der Scanner verschwindet): `{"online": false}`

Unbekannte Geräte werden dabei automatisch registriert (ohne Lagerort). Ein entfernter Scanner wird beim
nächsten Verbinden erneut registriert.

## `…/scan` (Scanner → Backend)

```json
{"ean": "4006381333931", "direction": "IN", "seq": 17, "bootId": "8f3a01c2"}
```

* `direction`: `IN` oder `OUT`
* `seq`: fortlaufend ab 1 je Neustart, `bootId`: zufällig je Neustart. Nachrichten mit gleicher
  `bootId` und `seq <=` der zuletzt verarbeiteten werden ignoriert (Mehrfachzustellung).
* Nachrichten werden in Empfangsreihenfolge nacheinander verarbeitet.
* Der device-service speichert den Scan und gibt ihn über Kafka (`device_scan`) an den stock-service;
  ohne zugeordneten Lagerort antwortet er sofort mit `NO_LOCATION`.

## `…/button` (Knopfleiste → Backend)

```json
{"button": 3, "direction": "IN", "seq": 18, "bootId": "8f3a01c2"}
```

* `button`: 1–8; `direction`: `IN` (kurz gedrückt) oder `OUT` (lang gedrückt)
* `seq`/`bootId` wie bei `…/scan`
* Der device-service bucht den Artikel, der dem Taster in der App zugeordnet ist (`device_scan` mit
  `articleId` statt `ean`). Ohne Lagerort antwortet er mit `NO_LOCATION`, ohne zugeordneten Artikel mit
  `NO_ARTICLE`.

## `…/ota` (Backend → Gerät) und `…/otastate` (Gerät → Backend)

Firmware-Update, ausgelöst in der App:

```json
{"job": "7c1e…", "version": "0.3.0", "size": 1032192, "sha256": "9f2b…", "port": 8088, "path": "/ota/Xk3…"}
```

Das Gerät lädt `http://<Broker-Adresse>:<port><path>` (Token nur für diesen Auftrag, 15 min gültig) und
installiert nur bei passender Größe und SHA-256. Fortschritt:

```json
{"job": "7c1e…", "state": "downloading"}
{"job": "7c1e…", "state": "installed"}
{"job": "7c1e…", "state": "failed", "error": "Prüfsumme falsch – Firmware nicht installiert"}
```

Nach `installed` startet das Gerät neu; meldet es in `…/status` die neue Version, ist das Update erfolgreich.

## `…/feedback` (Backend → Scanner)

```json
{"seq": 17, "result": "IN_OK", "success": true, "article": "Spaghetti", "quantity": 4,
 "message": "Spaghetti eingelagert"}
```

| `result` | Bedeutung | LED |
|---|---|---|
| `PENDING_ARTICLE` | unbekannter Barcode, Artikel wird gerade angelegt – weiter warten (bis 20 s) | weiß |
| `IN_OK` | bekannter Artikel eingelagert | grün |
| `IN_OK_NEW_ARTICLE` | aus Produktdatenbank angelegt und eingelagert | grün |
| `IN_OK_UNKNOWN_ARTICLE` | unbekannt, als Platzhalter in der Inbox angelegt und eingelagert | gelb |
| `OUT_OK` | ausgelagert | blau |
| `UNKNOWN_EAN` | Auslagern eines unbekannten Barcodes | rot |
| `INVALID_EAN` | Code ist keine gültige EAN (Länge, Ziffern, Prüfziffer); die Firmware prüft das schon selbst und sendet solche Codes gar nicht erst | rot |
| `NOT_IN_STOCK` | am Lagerort nicht vorhanden | rot |
| `NO_LOCATION` | Scanner hat keinen Lagerort (oder Lagerort gelöscht) | rot |
| `NO_ARTICLE` | Knopfleiste: Taster ist keinem Artikel zugeordnet | rot |
| `ARTICLE_NOT_FOUND` | Knopfleiste: zugeordneter Artikel existiert nicht mehr | rot |
| `ERROR` | ungültige Nachricht / interner Fehler | rot |

`quantity` ist der neue Bestand des Artikels an diesem Lagerort.

## Testen ohne Hardware

```bash
mosquitto_sub -h <host> -u scanner-test -P <pw> -t 'inventory/scanner/scanner-test/feedback' -v &
mosquitto_pub -h <host> -u scanner-test -P <pw> -t inventory/scanner/scanner-test/status -r \
  -m '{"online":true,"fw":"manual","bootId":"t1"}'
mosquitto_pub -h <host> -u scanner-test -P <pw> -t inventory/scanner/scanner-test/scan \
  -m '{"ean":"4006381333931","direction":"IN","seq":1,"bootId":"t1"}'
```
