# Rezepte: „Was kann ich kochen?“

Der **recipe-service** verwaltet Rezepte und vergleicht sie mit dem Vorrat. Fehlende Zutaten lassen sich
mit einem Klick auf die Einkaufsliste setzen.

## Rezepte anlegen

| Weg | Ablauf |
|---|---|
| **Manuell** | *Rezepte → + Neues Rezept* |
| **Per Link** | *Rezepte → Importieren → Link*. Die meisten Rezeptportale liefern strukturierte Daten (schema.org/Recipe); die werden direkt übernommen, die KI strukturiert nur die Zutatenzeilen und ordnet Artikel zu. Fehlen die strukturierten Daten, bekommt die KI den Seitentext. |
| **Per Freitext** | *Rezepte → Importieren → Freitext*, z. B. aus einem Kochbuch abgetippt oder aus einer Nachricht kopiert. Die KI bringt alles in die Struktur. |

Ein Import speichert nichts: Das Ergebnis öffnet sich als Entwurf im Editor, du prüfst und speicherst.

Pro Rezept: Titel, Beschreibung, Personenzahl, Vorbereitungs- und Kochzeit, Bild, Quelle, Kategorie
(Vorspeise, Hauptgang, Nachtisch, Longdrink, Aperitif, … – pflegbar unter *Einstellungen →
Rezeptkategorien*), Tags, Zubereitungsschritte und Zutaten.

### Zutaten

Jede Zutat hat Name, Menge, Einheit, optional einen **Artikel** aus dem Katalog und das Kennzeichen
**Basis**:

* **Artikel**: Nur zugeordnete Zutaten können gegen den Bestand gerechnet und gezielt eingekauft werden.
  Beim Import schlägt die KI passende Artikel vor; im Editor lässt sich das ändern.
* **Basis**: Grundzutaten wie Salz, Pfeffer, Wasser, Öl zählen nie als fehlend. Die KI setzt das Kennzeichen
  vor.
* Einheiten: g, kg, ml, l, Stück, Packung, EL, TL, Prise, sonstige.

## Was kann ich kochen?

*Kochen*: Personenzahl wählen (optional Kategorie). Die Mengen werden auf die Personenzahl umgerechnet
und mit dem Bestand verglichen. Gezählt werden **ganze Packungen** × **Inhalt einer Packung** (am Artikel
gepflegt bzw. aus der Packungsgröße „500 g“ gelesen):

| Zutat | Verfügbar, wenn |
|---|---|
| g/kg bzw. ml/l | Packungen × Inhalt (g bzw. ml) ≥ benötigte Menge |
| Stück | Packungen × Stück pro Packung (Artikel mit Inhalt in Stück) bzw. 1 Stück je Packung |
| Packung | genug Packungen da |
| EL, TL, Prise, ohne Menge, Einheit passt nicht zum Inhalt | mindestens eine Packung da |
| Basis | immer |
| ohne Artikel | nie (wird als Text auf die Einkaufsliste gesetzt) |

Die Liste zeigt kochbare Rezepte zuerst, dann nach Anzahl fehlender Zutaten. *Fehlendes auf die
Einkaufsliste* legt die fehlenden Packungen an (bzw. Freitext für Zutaten ohne Artikel) mit dem Hinweis
„für Lasagne (4 Pers.)“. Steht derselbe Artikel schon offen auf der Liste, wird die Menge erhöht.
Der Bestand wird beim Kochen nicht automatisch reduziert.

## KI-Anbindung (Spring AI)

Die KI wird nur für Importe gebraucht. Auswahl in `inventory/.env`:

| `AI_PROVIDER` | Voraussetzung | Hinweise |
|---|---|---|
| `ollama` (Standard) | infra-Stack mit `docker compose --profile ollama up -d` | läuft lokal, keine Kosten, Daten bleiben zuhause. Modell per `OLLAMA_MODEL` (Standard `qwen2.5:7b`, ~5 GB Download, ~6–8 GB RAM; GPU beschleunigt deutlich). Der Dienst `ollama-pull` lädt das Modell einmalig. |
| `anthropic` | `ANTHROPIC_API_KEY` | sehr zuverlässige Strukturierung; kostet pro Import. Modell per `ANTHROPIC_MODEL`. |
| `openai` | `OPENAI_API_KEY` | wie Anthropic; Modell per `OPENAI_MODEL`. |

Die Modellnamen in `.env.example` sind Vorschläge – trage ein Modell ein, das dein Konto bzw. deine
Ollama-Installation anbietet. Kleinere Ollama-Modelle liefern bei langen Rezepten gelegentlich unvollständige
Strukturen; der Editor zeigt das Ergebnis immer zur Kontrolle an. Ist die KI nicht erreichbar oder liefert
nichts Verwertbares, meldet der Import einen Fehler (HTTP 502), manuelles Anlegen funktioniert weiter.

An die KI gehen: der Rezepttext bzw. die Zutatenzeilen, die Kategorienamen und die Namen/Marken deiner
Katalogartikel (für die Zuordnung, höchstens 400).

## Technik

* Service `recipe-service`, Pfad `/api/recipe`, Schema `recipe`.
* Topics: `recipe_category`, `recipe_recipe`, `recipe_shopping_request` (vom shopping-service verarbeitet,
  jede Anfrage genau einmal).
* Lokale Kopien aus `catalog_article` (Name, Inhalt einer Packung) und `stock_batch` (Packungen je Artikel).
* Tests: Das KI-Modell wird durch ein Fake-`ChatModel` ersetzt, Rezeptseiten durch Fixtures; 100 %
  JaCoCo-Abdeckung.
