# MHD-Benachrichtigungen (ntfy)

Einmal täglich kommt eine Zusammenfassung aufs Handy: was **abgelaufen** ist, was **heute** abläuft und was
**bald** abläuft. Gesendet wird über einen eigenen [ntfy](https://ntfy.sh)-Server im infra-Stack an ein
gemeinsames Haushalts-Topic (Standard `vorrat`). Ein Tipp auf die Nachricht öffnet *Einstellungen →
Benachrichtigungen* in der Vorrat-App.

```
MHD: 1 abgelaufen · 1 heute · 2 bald
**Abgelaufen**
- Milch (Weide), 2× – seit 18.11.
**Läuft heute ab**
- Joghurt, 1×
**Läuft bald ab**
- Käse, 1× – 21.11. (in 2 Tagen)
```

## Verhalten

* Der notification-service prüft jede Minute, ob die eingestellte **Uhrzeit** (Standard 08:00, Zeitzone
  `TZ`) erreicht ist. Ab dann wird **einmal pro Tag** gesendet – startet der Service später am Tag, sendet er
  sofort.
* Steht nichts an, wird keine Nachricht gesendet (im Protokoll: „Nichts fällig“).
* **„Bald“** heißt: höchstens *n* Tage vor dem MHD. *n* ist global einstellbar (Standard 3) und kann je
  **Artikelkategorie** überschrieben werden (z. B. Milchprodukte 1 Tag, Konserven 30 Tage).
* Gleiche Artikel mit gleichem MHD werden über alle Lagerorte zusammengezählt. Chargen ohne MHD, aufgebrauchte
  Chargen und gelöschte Artikel zählen nicht.
* Priorität: hoch, wenn etwas abgelaufen ist oder heute abläuft, sonst normal.
* Ist ntfy nicht erreichbar, wird es alle 15 Minuten erneut versucht, höchstens 4-mal am Tag.

Einstellen unter *Einstellungen → Benachrichtigungen*: an/aus, Uhrzeit, globale Schwelle, Schwellen je
Kategorie, **Testnachricht senden**. Dort stehen auch die aktuell fälligen Artikel und das Versandprotokoll
der letzten 14 Tage.

## Einrichtung

1. **DNS**: `NTFY_HOST` (Standard `ntfy.home.arpa`) auf den Docker-Host zeigen lassen, wie die anderen Hosts.
   In `infra/.env` `NTFY_HOST` setzen.
2. **ntfy starten**: `cd infra && docker compose up -d` (der Dienst `ntfy` gehört zum infra-Stack).
   Ohne Konto darf niemand lesen oder schreiben (`deny-all`).
3. **Konto für den notification-service** (darf nur ins Topic schreiben):
   ```bash
   infra/scripts/ntfy-user.sh service          # gibt NTFY_TOKEN=tk_… aus
   ```
   Den Token in `inventory/.env` als `NTFY_TOKEN` eintragen, dann `cd inventory && docker compose up -d`.
4. **Konto je Haushaltsmitglied** (darf nur lesen):
   ```bash
   infra/scripts/ntfy-user.sh member anna      # gibt ein Passwort aus
   ```
5. **App auf dem Handy** (ntfy, Android: Play Store/F-Droid, iOS: App Store):
   * In den Einstellungen der App den eigenen Server `https://NTFY_HOST` als Standardserver eintragen und
     mit Benutzer/Passwort aus Schritt 4 anmelden.
   * Topic `vorrat` abonnieren.
6. In der Vorrat-App *Einstellungen → Benachrichtigungen → Testnachricht senden*.

Ein anderes Topic: `infra/scripts/ntfy-user.sh service <topic>` bzw. `member <user> <topic>` und
`NTFY_TOPIC=<topic>` in `inventory/.env`. Konto löschen: `infra/scripts/ntfy-user.sh delete <user>`.

### Zertifikate

Die ntfy-App vertraut nur Zertifikaten, die das Handy akzeptiert. Mit der **lokalen CA** muss deren
Zertifikat (`infra/traefik/certs/ca.pem`) auf dem Handy installiert sein. Android-Apps vertrauen vom Benutzer
installierten CAs nicht in jedem Fall; klappt die Verbindung nicht, ist **Let's Encrypt** (siehe README) die
zuverlässige Lösung.

### iOS

Apple erlaubt Push nur über Apples Dienst. Der eigene Server schickt deshalb für jede Nachricht nur eine
inhaltslose Weckanfrage an `ntfy.sh` (`NTFY_UPSTREAM_BASE_URL`); die App holt den Inhalt danach vom eigenen
Server. Dafür braucht der ntfy-Container Internetzugang. Ohne iOS-Geräte kann
`NTFY_UPSTREAM_BASE_URL=` leer gesetzt werden.

## Technik

* Service `notification-service`, Pfad `/api/notification`, Schema `notification`, Port 8086 (lokal).
* Lokale Kopien aus `catalog_article` (Name, Marke, Kategorie) und `stock_batch` (Artikel, MHD, Menge).
* Aggregate: `NotificationSettings` (genau eine Instanz) und `Digest` (eine je Tag, mit Status `SENT`,
  `NOTHING_DUE` oder `FAILED`). Topics `notification_settings`, `notification_digest`.
* Gesendet wird per JSON an ntfy (`topic`, `title`, `message` in Markdown, `priority`, `tags`, `click`) mit
  dem Token als `Authorization: Bearer`.
* Tests: ntfy wird durch einen kleinen Test-HTTP-Server ersetzt, der Scheduler ist abgeschaltet und die
  Uhrzeit wird übergeben; 100 % JaCoCo-Abdeckung. Zusätzlich gegen einen echten ntfy-Server (v2.20.0)
  geprüft: Rechte (`deny-all`, Service nur schreiben, Mitglieder nur lesen) und Darstellung.
