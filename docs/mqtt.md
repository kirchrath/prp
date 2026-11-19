# MQTT-Protokoll Scanner ↔ device-service

Präfix: `inventory/scanner/<deviceId>/…` – `deviceId` ist zugleich der MQTT-Benutzername des Scanners
(per ACL erzwungen). Alle Nutzdaten sind JSON.

## `…/status` (Scanner → Backend, retained)

Beim Verbinden:

```json
{"online": true, "fw": "0.1.0", "ip": "192.168.1.50", "rssi": -61, "bootId": "8f3a01c2"}
```

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
| `NOT_IN_STOCK` | am Lagerort nicht vorhanden | rot |
| `NO_LOCATION` | Scanner hat keinen Lagerort (oder Lagerort gelöscht) | rot |
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
