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
