# ESP32-Scanner

## Teile

| Teil | Hinweis |
|---|---|
| ESP32 DevKit (ESP32-WROOM-32) | jedes Board mit freien GPIOs 14/16/17/25/26/27/32/33 |
| GM65 / GM805 Barcode-Modul | UART (TTL), 5 V Versorgung |
| 2 × IR-Sensor | z. B. TCRT5000-Reflexmodul oder E18-D80NK (NPN, open collector); Ausgang LOW = Objekt |
| RGB-LED, gemeinsame Kathode | + 3 × 220 Ω |
| Passiver Buzzer | |
| 5-V-Netzteil (≥ 1 A) | |

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

Die Sensoreingänge nutzen die internen Pull-ups (3,3 V). E18-D80NK mit 5 V versorgen, der
Open-Collector-Ausgang zieht nur nach GND und ist damit 3,3-V-sicher. Die GM65-UART arbeitet mit
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
