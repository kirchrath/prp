# Hinweise für Claude

## Hardware-Liste aktuell halten

`docs/hardware.md` ist die Einkaufs- und Verdrahtungsliste für die selbst gebauten Geräte. Sie
unterscheidet **Scanner** und **Knopfleiste** (jeweils eigene, vollständige Einkaufsliste) plus einen
Abschnitt für Teile, die beide brauchen.

Bei jeder Änderung, die Hardware betrifft, diese Datei im selben Commit mitpflegen – insbesondere bei:

* Pins (`scanner/firmware/include/pins.h`, `include/button_pins.h`) → Verdrahtungstabellen
* neuen oder geänderten Bauteilen, Stromversorgung, Anzahl Taster/Sensoren → Einkaufslisten, Summen
* neuen Gerätearten oder PlatformIO-Environments → Übersichtstabelle und eigener Abschnitt
* geänderten Signalen (LED/Ton) → Tabelle „Signale“ mit der Spalte je Gerät
* geändertem Server-Umfang (Services, Container) → Abschnitt „Docker-Host“

Preise sind Richtwerte und als solche gekennzeichnet; nicht als geprüft ausgeben.

## Commit-Nachrichten: Conventional Commits

Jede Commit-Nachricht folgt [Conventional Commits 1.0.0](https://www.conventionalcommits.org/de/v1.0.0/):

```
<type>(<scope>): <kurze Beschreibung im Imperativ, klein, ohne Punkt, ≤ 72 Zeichen>

<optionaler Text: was und warum>

<optionale Footer, z. B. BREAKING CHANGE: …, Refs: #12>
```

* **type**: `feat` (neue Funktion), `fix` (Fehlerbehebung), `docs`, `test`, `refactor`, `perf`, `build`
  (Maven, npm, Dockerfiles), `ci`, `chore` (Sonstiges, z. B. Abhängigkeiten), `style`, `revert`
* **scope** (optional, ein Bereich; bei mehreren den wichtigsten oder weglassen): `catalog`, `stock`,
  `shopping`, `device`, `recipe`, `notification`, `frontend`, `firmware`, `infra`, `backup`, `dev`, `docs`
* Inkompatible Änderungen (z. B. API, Event-Format, Flash per USB nötig): `!` nach type/scope und Footer
  `BREAKING CHANGE: <was ist zu tun>`
* Beispiele: `feat(device): add OTA firmware updates`, `fix(infra): allow button topic in mosquitto acl`,
  `docs: describe backup and restore`
* Bereits gepushte Commits werden nicht umgeschrieben.
