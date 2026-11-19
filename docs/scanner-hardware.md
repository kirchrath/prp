# ESP32-Scanner

## Einkaufsliste (pro Scanner)

| Anzahl | Teil | Worauf achten | Richtwert* |
|---|---|---|---|
| 1 | **ESP32-Entwicklungsboard**, z. B. ESP32-DevKitC V4 (ESP32-WROOM-32/-32E) | klassischer ESP32 (nicht S2/S3/C3 – andere Pins), GPIO 14/16/17/25/26/27/32/33 herausgeführt; USB-Anschluss (Micro-USB oder USB-C) für das passende Kabel merken | 8–15 € |
| 1 | **Barcode-Scanmodul GM65** (alternativ GM805) | Variante mit **TTL/UART-Kabel** bestellen (nicht nur USB); liest 1D-Codes (EAN-13/EAN-8/UPC) und QR | 20–35 € |
| 2 | **IR-Näherungsschalter E18-D80NK** | einstellbare Reichweite 3–80 cm, NPN-Ausgang, 5 V. *Nicht* TCRT5000 – der reicht nur wenige Millimeter bis ~2 cm | 4–8 € je Stück |
| 1 | **RGB-LED 5 mm, gemeinsame Kathode** | „common cathode“, sonst passt die Ansteuerung nicht | < 1 € |
| 3 | **Widerstand 220 Ω** | für die drei LED-Farben | < 1 € |
| 1 | **Passiver Buzzer** (z. B. Modul KY-006) | *passiv* – ein aktiver Buzzer kann keine verschiedenen Töne | 1–3 € |
| 1 | **USB-Netzteil 5 V, ≥ 2 A** + USB-Kabel | GM65 und Sensoren hängen mit am 5-V-Pin des Boards | 8–15 € |
| 1 | Breadboard + Steckkabel (Jumper, m-w/w-w) | für den Aufbau auf dem Tisch | 5–10 € |
| 1 | Gehäuse/Halter | Abzweigdose oder 3D-Druck; die Sensoren 5–10 cm links/rechts vom Scanfenster | – |
| optional | Widerstände 1 kΩ + 2 kΩ | Spannungsteiler, falls dein GM65 5-V-TTL-Pegel ausgibt (siehe unten) | < 1 € |
| optional | Lochrasterplatine oder ESP32-Schraubklemmen-Adapter | für den dauerhaften Einbau statt Breadboard | 5–10 € |

\*Grobe Richtwerte für Einzelstücke im Elektronikhandel; nicht tagesaktuell geprüft. Insgesamt etwa
50–90 € pro Scanner, Mehrfachpackungen (Sensoren, LEDs, Widerstände) machen weitere Scanner günstiger.

Werkzeug: Lötkolben (für Stiftleisten/feste Verdrahtung), Seitenschneider, Multimeter hilfreich.

### Docker-Host

Für den Server-Teil (Traefik, Keycloak, Kafka, Mosquitto, PostgreSQL, vier Services, Frontend) wurden
im Betrieb rund 3,5 GB RAM gemessen. Empfehlung: **8 GB RAM** (mit lokaler KI über Ollama **16 GB**, eine
GPU beschleunigt Importe stark), 2+ CPU-Kerne, 20 GB freier Speicher (+ ~5 GB für das KI-Modell),
Docker Engine mit Compose v2. Getestet auf x86_64; ARM64 (z. B. Raspberry Pi 5 mit 8 GB) sollte mit
denselben Images funktionieren, ist aber nicht geprüft.

## Verdrahtung

| ESP32 | Bauteil |
|---|---|
| GPIO16 (RX2) | GM65 TX |
| GPIO17 (TX2) | GM65 RX |
| 5V / GND | GM65 VCC / GND |
| GPIO32 | Sensor A (Raumseite) OUT |
| GPIO33 | Sensor B (Regalseite) OUT |
| GPIO25 / 26 / 27 | LED rot / grün / blau (je 220 Ω) |
| GPIO14 | Buzzer + |
| GPIO0 (BOOT-Taste) | 5 s halten: Einstellungen löschen |

Die Sensoreingänge nutzen die internen Pull-ups (3,3 V). E18-D80NK mit 5 V versorgen (braun = 5 V,
blau = GND, schwarz = Ausgang); der NPN-Ausgang zieht nur nach GND und ist damit 3,3-V-sicher.
Die Reichweite am Poti so einstellen, dass der Sensor einen vorbeigeführten Artikel erkennt, aber nicht
das Regal gegenüber. Die GM65-UART arbeitet mit
3,3-V-Pegeln; falls dein Modul 5-V-Pegel ausgibt, einen Spannungsteiler (z. B. 1 kΩ/2 kΩ) vor GPIO16 setzen.

## Anordnung und Richtung

```
  Raum            Scanfenster            Regal
   ──▶   [Sensor A]   [GM65]   [Sensor B]   ──▶   = Einlagern (A vor B)
   ◀──                                      ◀──   = Auslagern (B vor A)
```

Die Sensoren werden links und rechts vom Scanfenster montiert (Abstand ca. 5–10 cm), sodass ein Artikel
beim Durchziehen nacheinander beide auslöst. Barcode und Bewegung dürfen in beliebiger Reihenfolge
innerhalb von 2 s auftreten. Ist die Richtung falsch herum, im Konfigurationsportal
„Richtung invertieren“ auf `1` setzen.

Barcode ohne erkannte Richtung → magenta + tiefer Ton, es wird nichts gebucht.
Bewegung ohne Barcode (z. B. Hand) wird ignoriert.

## GM65 einrichten

Mit den Einstell-Barcodes aus dem GM65-Handbuch:

1. **Sensing mode** (Induktionsmodus) – das Modul liest automatisch, sobald sich das Bild ändert.
2. Ausgabe über **UART/TTL**, 9600 Baud (Werkseinstellung).
3. Optional „Same barcode reading delay“ auf ca. 1 s.

Die Firmware erkennt das Ende eines Codes an CR/LF oder an einer Pause von 50 ms, ein Suffix ist also
nicht zwingend.

## Signale

| LED | Ton | Bedeutung |
|---|---|---|
| weiß (gedimmt) | – | gesendet, warte auf Server (bei unbekanntem Artikel bis 20 s) |
| grün | hoch | eingelagert |
| gelb | 2 × hoch | eingelagert, Artikel unbekannt → in der Inbox ergänzen |
| blau | mittel | ausgelagert |
| rot | lang tief | abgelehnt (unbekannt, kein Bestand, kein Lagerort, keine Antwort) |
| magenta | tief | Richtung nicht erkannt |
| cyan | kurz | offline zwischengespeichert (bis 32 Scans), wird beim Verbinden gesendet |
| orange blinkend | – | kein WLAN/MQTT |

## Firmware bauen, testen und flashen

```bash
cd scanner/firmware
pio test -e native            # Unity-Tests der Logik (lib/scanner_logic), läuft auf dem PC
pio run -e esp32dev -t upload
pio device monitor
```

Die hardwareunabhängige Logik – Richtungserkennung, Zuordnung Barcode ↔ Bewegung, Barcode-Erkennung im
seriellen Datenstrom, Feedback-Timeout, Offline-Puffer und MQTT-Nachrichten – liegt in
`lib/scanner_logic` und hat keine Arduino-Abhängigkeit.

Beim ersten Start (oder mit gedrückter BOOT-Taste beim Einschalten) öffnet der Scanner den Access Point
`Vorrat-<geräte-id>`. Dort WLAN, MQTT-Broker (IP des Docker-Hosts), Port, Geräte-ID und das mit
`infra/scripts/mqtt-user.sh add <geräte-id>` erzeugte Passwort eintragen.

# Knopfleiste (Artikel ohne Barcode)

Für Gemüse, Obst und andere Ware ohne Barcode: eine Leiste mit bis zu **8 Tastern**, z. B. am Gemüsefach.
Jeder Taster ist in der App einem Artikel zugeordnet (*Einstellungen → Geräte*):

* **kurz drücken** = 1 Stück einlagern (LED grün)
* **lang drücken** (1 s, ohne loszulassen) = 1 Stück entnehmen (LED blau)
* rot = Fehler, z. B. nichts mehr da oder Taster keinem Artikel zugeordnet

## Einkaufsliste (pro Knopfleiste)

| Anzahl | Teil | Worauf achten | Richtwert* |
|---|---|---|---|
| 1 | **ESP32-Entwicklungsboard**, z. B. ESP32-DevKitC V4 | klassischer ESP32; GPIO 13/16/17/18/19/21/22/23 und 14/25/26/27 herausgeführt | 8–15 € |
| 1–8 | **Taster**, z. B. 12-mm-Drucktaster oder kleine Arcade-Taster (Schließer) | einfacher Schließer (NO), keine Rastfunktion | 0,50–2 € je Stück |
| 1 | **RGB-LED 5 mm, gemeinsame Kathode** + 3× **220 Ω** | wie beim Scanner | < 1 € |
| 1 | **Passiver Buzzer** (z. B. KY-006) | wie beim Scanner | 1–3 € |
| 1 | **USB-Netzteil 5 V, ≥ 1 A** + Kabel | ohne Scanmodul reicht weniger Strom | 6–10 € |
| 1 | Gehäuse + Beschriftung | z. B. Aufputzdose oder 3D-Druck; Etiketten oder Schilder mit dem Artikelnamen je Taster | – |

\*Grobe Richtwerte, nicht tagesaktuell geprüft. Insgesamt etwa 20–35 €.

## Verdrahtung

Jeder Taster zwischen GPIO und **GND** (interner Pull-up, kein Widerstand nötig). Nicht bestückte Eingänge
bleiben offen.

| Taster | GPIO | | Bauteil | GPIO |
|---|---|---|---|---|
| 1 | 13 | | LED rot (über 220 Ω) | 25 |
| 2 | 16 | | LED grün (über 220 Ω) | 26 |
| 3 | 17 | | LED blau (über 220 Ω) | 27 |
| 4 | 18 | | LED Kathode | GND |
| 5 | 19 | | Buzzer + | 14 |
| 6 | 21 | | Buzzer − | GND |
| 7 | 22 | | BOOT-Taste (Konfiguration) | 0 (auf dem Board) |
| 8 | 23 | | | |

LED, Buzzer und BOOT-Taste sind wie beim Scanner verdrahtet; die Signale (Farben, Töne, Offline-Blinken,
Offline-Puffer) sind dieselben.

## Firmware und Einrichtung

```bash
cd scanner/firmware
pio test -e native              # Unity-Tests, inkl. Entprellen und kurz/lang
pio run -e buttons -t upload    # Knopfleisten-Firmware flashen
```

1. MQTT-Benutzer anlegen: `infra/scripts/mqtt-user.sh add knopf-gemuese` (die Geräte-ID ist der Benutzername).
2. Beim ersten Start öffnet die Leiste den Access Point `Vorrat-<geräte-id>` (Standard-ID `knopf-xxxxxx`):
   WLAN, Broker-Adresse, Geräte-ID und Passwort eintragen. 5 s BOOT gedrückt halten setzt alles zurück.
3. In der App unter *Einstellungen → Geräte* erscheint sie als **Knopfleiste**: Lagerort wählen und den
   Tastern 1–8 Artikel zuordnen (Artikel ohne EAN legst du unter *Artikel → + Neuer Artikel* an).

Ohne Hardware ausprobieren: `DEVICE=knopf-dev dev/simulate-scanner.sh buttons`, dann `press 1` (kurz) bzw.
`hold 1` (lang) – siehe [local-development.md](local-development.md).
