---
name: gkv-abrechnung
description: Fachdomäne der Abrechnung mit gesetzlichen Krankenkassen — Beteiligte, Institutionskennzeichen, Rechnungsaufbau, Pflichtangaben, Abrechnungscodes und typische Abweisungsgründe. Verwenden bei fachlichen Fragen zur Abrechnung, zu IK-Rollen, Versichertenstatus, Rechnungsnummern, Leistungspositionen, Summenbildung, oder um zu beurteilen, ob eine Regel fachlich richtig ist.
---

# Abrechnung mit gesetzlichen Krankenkassen

Dieser Text hält fest, was im Projekt belegt ist. Quellen sind
`Information/Abrechnung_Checkliste_kurz.md`,
`Information/Abrechnung_Datenerklaerung_test.md`, die Codelisten unter
`Information/codes/` und die Segmentdefinitionen unter
`gkv-core/src/main/resources/segments/`.

Die verbindlichen Vorgaben stehen in den beiliegenden PDF:
`Anlage_1_TP5_V21_20260115.pdf` und `Anlage_3_TP5_V22_20260218.pdf`. **Bei einer
Frage, die über das hier Belegte hinausgeht, dort nachsehen und nicht raten** —
eine falsch erfundene Regel führt zu abgewiesenen Abrechnungen.

## Worum es geht

Eine Leistungserbringerin — im Fall dieses Projekts eine Hebamme, erkennbar an
den Belegnummern mit Präfix `HEB` — erbringt Leistungen für gesetzlich
Versicherte und rechnet sie nicht mit den Versicherten ab, sondern mit deren
Krankenkasse. Das geschieht als strukturierte Datenlieferung im DTA-Format,
nicht als Rechnung auf Papier.

Der Ablauf im Programm:

```
Stammdaten (Patient, Leistungserbringer)
  + Blaupause (welche Leistung, welcher Betrag)
  + Terminanzahl
        ↓
   Abrechnung
        ↓
   DTA-Nachricht (SLGA + SLLA)
        ↓
   Prüfung
        ↓
   Versand an die Kasse (nach Kassen-IK)
        ↓
   Rückmeldung: angenommen / zurückgewiesen / Syntaxfehler
```

## Die Beteiligten und ihre Kennzeichen

Jede Einrichtung im Abrechnungsverkehr hat ein **Institutionskennzeichen (IK)**,
neunstellig mit Prüfziffer an letzter Stelle. Im `FKT`-Segment stehen vier
IK-Rollen nebeneinander:

```
FKT + 01 + + IK-Leistungserbringer + IK-Kostenträger + IK-Kasse-von-Karte + IK-Rechnungssteller '
```

| Rolle | Wer das ist |
|---|---|
| Leistungserbringer | wer die Leistung erbracht hat |
| Kostenträger | wer bezahlt |
| Kasse von Karte/Verordnung | die Kasse laut Versichertenkarte |
| Rechnungssteller | wer abrechnet — kann eine Abrechnungsstelle sein |

Leistungserbringer und Rechnungssteller sind oft, aber nicht zwingend
dieselbe Einrichtung. Kostenträger und Kasse von Karte fallen meist zusammen.

Das leere zweite Element (`01++`) ist bedeutsam und darf nicht wegfallen —
sonst verschieben sich alle folgenden Felder.

Zum Aufbau und zur Prüfziffer siehe Skill `dta-format`.

## Die Rechnung

Belegt in `Information/codes/02_rechnungs_und_verarbeitungskennzeichen.json`:

- **Verarbeitungskennzeichen** (`FKT`): `01` = normale Abrechnung
- **Rechnungsnummer** (`REC`): Format `Sammel:Einzel`, Beispiel `00000000:0`.
  Eine Sammelrechnung kann mehrere Einzelrechnungen enthalten.
- **Rechnungsdatum** (`REC`): `JJJJMMTT`
- **Rechnungsart** (`REC`): `1`
- **Belegnummer** (`INV`): z. B. `HEB2403001` — Präfix, Jahr/Monat, laufende
  Nummer

## Angaben zur versicherten Person

**Entweder oder** — das ist eine echte Pflichtkonvention und keine Empfehlung:

- Versichertennummer **und** Versichertenstatus sind im `INV` gepflegt,
- **oder**, wenn der Status fehlt: `NAD` muss die vollständige Anschrift
  enthalten — Nachname, Vorname, Geburtsdatum, Straße mit Hausnummer, PLZ, Ort.

Fehlt beides, kann die Kasse die Leistung keinem Versicherungsverhältnis
zuordnen und weist die Rechnung zurück, obwohl die Leistung erbracht wurde.

Die `VersichertenangabenRegel` prüft Name, Geburtsdatum und Versichertennummer.
Eine Versichertennummer aus lauter Nullen gilt als fehlend.

## Die Leistungszeile

Für den Leistungsbereich SGS H (Abrechnungscode `61`):

```
ENF+01+61:00000+306050601+08,00+15000,00+20240301+0,00'
```

| Angabe | Wert im Beispiel | Bedeutung |
|---|---|---|
| Zeilen-Identifikationsnummer | `01` | fortlaufend innerhalb einer `INV` |
| Leistungserbringergruppe | `61:00000` | Abrechnungscode `:` Tarifkennzeichen |
| Abrechnungspositionsnummer | `306050601` | muss vertraglich gültig sein |
| Anzahl/Menge | `08,00` | z. B. Sitzungen |
| Einzelbetrag | `15000,00` | durchschnittlicher Einzelbetrag |
| Leistungsdatum | `20240301` | oder Beginn der Abrechnungsperiode |
| Zuzahlung | `0,00` | Anteil des Versicherten, Kannfeld |

Abrechnungscode `61` steht laut `Information/codes/03_leistungscodes_sgs_h.json`
für Rehabilitationssport im Bereich SGS H. Das Tarifkennzeichen ist fünfstellig
und vertraglich vereinbart. Die Positionsnummer muss für den Leistungsbereich
gültig sein — welche Nummern zulässig sind, steht in Anlage 3, Abschnitt 8.2.

### Achtung bei der Feldzählung

`Information/Abrechnung_Checkliste_kurz.md` zählt den **Segmentbezeichner als
Feld 1**, die Segmentdefinition `segments/enf.json` zählt ihn **nicht mit**. Die
Anzahl/Menge ist damit „Feld 5" in der Checkliste und „position 4" in der JSON.

Im Code gilt eine dritte Zählweise: `DtaSegment.element(n)` ist **0-basiert und
ohne Bezeichner**. Die Menge ist dort `element(3)`.

Das ist eine verlässliche Fehlerquelle. Wer eine Position angibt, sollte die
Zählweise dazusagen; wer eine übernimmt, sollte sie gegen ein Beispiel prüfen.

## Summenbildung

Belegt in `Information/codes/04_summen_und_statuscodes.json`:

```
BES     = ENF.Menge × ENF.Einzelbetrag  (+ ggf. MWS)   je Abrechnungsfall
GES 00  = Summe aller BES                              Gesamtsumme aller Status
GES 99  = gemäß Prüfvorgabe                            oft gleich GES 00
```

`GES` trägt zwei Beträge: den Rechnungsbetrag der Einzelrechnungen und den
Bruttobetrag. Im Referenzbeispiel sind beide gleich.

**Fachlich wichtig:** Eine Abweichung bei `GES 00` ist ein Fehler, eine bei
`GES 99` kann der Prüfvorgabe der Kasse entsprechen. Die
`BetragskonsistenzRegel` bildet das ab — Fehler gegenüber Hinweis.

`UST+19` gibt den Umsatzsteuersatz an.

## Prüfliste vor dem Versand

Aus `Information/Abrechnung_Checkliste_kurz.md`, verkürzt:

**Stammdaten** — IK für Leistungserbringer, Kostenträger und Kasse vorhanden;
Rechnungsnummer im Format `Sammel:Einzel`; Rechnungsdatum `JJJJMMTT`;
Rechnungsart; Belegnummer.

**Versicherte Person** — Versichertennummer und -status **oder** vollständige
Anschrift im `NAD`.

**Leistungszeile** — Abrechnungscode passend zum Leistungsbereich;
Tarifkennzeichen fünfstellig; Positionsnummer gültig; alle Geld- und
Mengenfelder mit zwei Nachkommastellen; Leistungsdatum `JJJJMMTT`.

**Summen** — `BES` je Fall gerechnet; `GES 00` vorhanden und stimmig; `GES 99`
sofern gefordert.

**Zähler** — `UNT` sechsstellig und richtig gezählt; `UNZ` entspricht der
Anzahl `UNH`.

**Endkontrolle** — keine Feldverschiebung im `ENF`, Doppelpunkt nur im
Kompositfeld; Schlüsselwerte passen zum Sammelgruppenschlüssel; `BES`, `GES`,
`UNT`, `UNZ` rechnerisch und formal stimmig.

Diese Liste ist in `DtaValidationService` weitgehend umgesetzt. Was sich nicht
automatisch prüfen lässt, ist die **fachliche Gültigkeit** von Positionsnummer,
Tarifkennzeichen und Abrechnungscode — dafür braucht es den Vertrag und
Anlage 3.

## Typische Abweisungsgründe

Nach Art der Rückmeldung geordnet:

**Syntaxfehler** — die Datei ist technisch falsch aufgebaut:
- Segmentzähler in `UNT` oder `UNZ` stimmen nicht
- Feldverschiebung, meist durch ein weggelassenes Leerfeld
- Betrag mit Punkt statt Komma, oder ohne zwei Nachkommastellen
- Datum nicht als `JJJJMMTT`
- fehlendes Abschlusszeichen `'`

**Zurückweisung** — die Datei ist lesbar, aber inhaltlich zu beanstanden:
- IK mit falscher Prüfziffer oder nicht vergeben
- Versichertennummer fehlt und `NAD` ist unvollständig
- Summen passen nicht zueinander
- Positionsnummer für den Leistungsbereich nicht zulässig
- Datenaustauschreferenz doppelt vergeben

**Technischer Fehler** — nichts an der Datei, sondern an der Übertragung.
Erneut senden, nicht korrigieren.

Die Unterscheidung zwischen den ersten beiden entscheidet über die Reaktion:
Syntaxfehler heißt neu erzeugen, Zurückweisung heißt fachlich korrigieren.

## Was das Programm heute nicht kann

Ehrlich zu benennen, damit niemand sich darauf verlässt:

- Der Versand ist dateibasiert. Ein echter, signierter und verschlüsselter
  Übermittlungsweg an reale Annahmestellen ist nicht umgesetzt — dafür wären
  Zertifikate einer anerkannten Stelle, Zugangsdaten und ein eigenes
  Betriebsstätten-IK nötig.
- Positionsnummern, Tarifkennzeichen und Abrechnungscodes werden auf Form,
  aber nicht auf fachliche Zulässigkeit geprüft.
- Es gibt keine Stornierung und keine Nachberechnung.
- Der Umsatzsteuersatz ist fest mit 19 hinterlegt.
- Es wird nur der Leistungsbereich SGS H mit Abrechnungscode `61` abgedeckt.
