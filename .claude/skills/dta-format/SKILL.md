---
name: dta-format
description: Aufbau, Segmente und Feldregeln des DTA-Formats für die GKV-Abrechnung (SLGA/SLLA, UNB..UNZ). Verwenden beim Lesen, Schreiben, Prüfen oder Debuggen von DTA-Nachrichten, bei Fragen zu Segmenten wie UNB, UNH, FKT, REC, INV, NAD, ENF, BES, GES, UST, UNT, UNZ, zu Zählern, Beträgen, Datumsformaten oder wenn eine Kasse eine Lieferung wegen Syntaxfehlers abgewiesen hat.
---

# DTA-Format der GKV-Abrechnung

Diese Anleitung beschreibt das Format so, wie es in diesem Projekt umgesetzt und
belegt ist. Grundlagen sind die Segmentdefinitionen unter
`GKVTransmitter/gkv-core/src/main/resources/segments/`, die Codelisten unter
`GKVTransmitter/Information/codes/` und die Referenznachricht
`GKVTransmitter/Information/Valide.DTA`.

## Was zuerst zu tun ist

**Bei jeder Frage zu einem konkreten Feld: erst die Segmentdefinition lesen.**
Für jedes Segment liegt eine JSON-Datei unter `gkv-core/src/main/resources/segments/`
mit Position, Typ, Länge, Pflichtangabe und Beispiel je Feld. Diese Dateien sind
die Wahrheit im Projekt — nicht das Gedächtnis und nicht dieser Text.

```
segments/unb.json  segments/unh.json  segments/unt.json  segments/unz.json
segments/fkt.json  segments/rec.json  segments/ust.json  segments/ges.json
segments/nam.json  segments/inv.json  segments/nad.json  segments/enf.json
segments/bes.json
```

Für eine vollständige, nachweislich gültige Nachricht als Vergleich:
`Information/Valide.DTA`. Sie durchläuft die Validierung des Projekts
unbeanstandet und dient in `DtaValidationServiceTest` als Messlatte.

## Grundaufbau

Eine Lieferung ist eine Textdatei aus Segmenten, ein Segment je Zeile:

```
Bezeichner + Datenelement + Datenelement + ... '
```

- `+` trennt Datenelemente
- `:` trennt Komponenten **innerhalb** eines Datenelements (Kompositfeld)
- `'` schließt das Segment ab
- Der Bezeichner besteht aus genau drei Großbuchstaben

Ein leeres Datenelement wird durch zwei aufeinanderfolgende `+` dargestellt und
darf **nicht** weggelassen werden. `FKT+01++123456780` hat ein leeres zweites
Element. Eine Feldverschiebung durch ein ausgelassenes Element ist einer der
häufigsten Abweisungsgründe.

## Die Schachtelung

```
UNB                          Kopf der Übertragung (genau einer)
├── UNH  (SLGA)              Nachrichtenkopf: Summen und Begleitdaten
│   ├── FKT, REC, UST
│   ├── GES, GES
│   ├── NAM
│   └── UNT                  Abschluss mit Segmentanzahl
├── UNH  (SLLA)              Nachrichtenkopf: Fall- und Leistungsdaten
│   ├── FKT, REC
│   ├── INV, NAD
│   ├── ENF  (mindestens einer)
│   ├── BES
│   └── UNT
└── UNZ                      Abschluss der Übertragung (genau einer)
```

**SLGA** trägt Summen und Begleitdaten, **SLLA** die Fall- und Leistungsdaten.
Beide sind eigenständige Nachrichten mit eigenem `UNH`/`UNT`-Paar innerhalb
derselben Übertragung.

## Die Zähler — hier entstehen die meisten Fehler

Drei Angaben müssen zur tatsächlichen Struktur passen. Sie verschieben sich bei
jeder Änderung an der Nachricht mit, und eine falsche Zahl führt zur Abweisung
der gesamten Lieferung wegen Syntaxfehlers.

| Angabe | Ort | Bedeutung | Format |
|---|---|---|---|
| Segmentanzahl | `UNT`, Element 1 | Segmente dieser Nachricht **einschließlich UNH und UNT** | 6-stellig |
| Nachrichtennummer | `UNT`, Element 2 | muss die Nummer aus dem zugehörigen `UNH` wiederholen | 5-stellig |
| Nachrichtenanzahl | `UNZ`, Element 1 | Anzahl der `UNH` in der Übertragung | 6-stellig |
| Datenaustauschreferenz | `UNZ`, Element 2 | muss die Referenz aus `UNB` Element 5 wiederholen | 5-stellig |

Im Beispiel aus `Valide.DTA` hat die SLGA acht Segmente (UNH, FKT, REC, UST,
GES, GES, NAM, UNT) — daher `UNT+000008+00001`.

**Die Datenaustauschreferenz steht im `UNB` an Position 5**, nicht 4. Aufbau:

```
UNB + UNOC:3 + Absender-IK + Empfänger-IK + Datum:Zeit + Referenz + H + Anwendungsref + 1 '
      1         2             3              4            5          6   7               8
```

Die Referenz muss je Absender eindeutig sein. Eine doppelt vergebene Nummer
führt zur Abweisung. Im Projekt vergibt sie
`DataRepository.nextDtaInterchangeReference()`.

## Formate

Belegt in `Information/codes/05_formate_und_pflichtkonventionen.json`:

- **Datum:** `yyyyMMdd`, also `20240301`. Keine Punkte, kein ISO-Bindestrich.
- **Beträge und Mengen:** Komma als Dezimaltrennzeichen, immer zwei
  Nachkommastellen: `08,00`, `15000,00`, `0,00`. Ein Punkt ist falsch.
- **Pflichtkonvention:** Fehlt der INV-Status, muss `NAD` die vollständige
  Anschrift enthalten.
- **Pflichtkonvention:** Im `ENF` darf kein Feld verschoben werden; der
  Doppelpunkt kommt dort ausschließlich im Kompositfeld
  `Abrechnungscode:Tarifkennzeichen` vor.

Beim Einlesen von Beträgen in Java scheitert `new BigDecimal("15000,00")`. Es
braucht ein `DecimalFormat` mit `Locale.GERMAN` und `setParseBigDecimal(true)` —
so macht es `BetragskonsistenzRegel`.

## Die Summen und wie sie zusammenhängen

Belegt in `Information/codes/04_summen_und_statuscodes.json`:

```
BES     = ENF.Menge × ENF.Einzelbetrag   (je Abrechnungsfall, ggf. zzgl. MWS)
GES 00  = Summe aller BES                (Gesamtsumme aller Status)
GES 99  = gemäß Prüfvorgabe              (in der Referenzdatei gleich GES 00)
```

Die Unterscheidung ist wichtig: **eine Abweichung bei GES 00 ist ein Fehler**,
eine bei GES 99 kann der Prüfvorgabe der Kasse entsprechen. Die
`BetragskonsistenzRegel` behandelt sie deshalb unterschiedlich (Fehler
gegenüber Hinweis).

Im Beispiel: 8 Termine × 15000,00 = 120000,00 in `BES`, `GES+00` und `GES+99`.

## Institutionskennzeichen

Neun Stellen, die letzte ist eine Prüfziffer:

```
1 0 8 3 1 0 4 0 0
│ │ └───┬───┘ │ │
│ │     │     │ └── Stelle 9: Prüfziffer
│ │     │     └──── Stellen 3-8: Regionalbereich und Seriennummer
│ └─────┴────────── Stellen 1-2: Klassifikation
```

Berechnung der Prüfziffer: Stellen 3 bis 8 abwechselnd mit 2 und 1 gewichten
(beginnend mit 2 auf Stelle 3), von jedem Produkt die Quersumme bilden,
aufsummieren; die letzte Ziffer der Summe ist die Prüfziffer.

Beispiel `108310400`: Stellen 3-8 sind `8,3,1,0,4,0`; Produkte `16,3,2,0,8,0`;
Quersummen `7,3,2,0,8,0`; Summe 20; Prüfziffer 0. ✓

Im Projekt umgesetzt in `de.gkvtransmitter.util.Institutionskennzeichen`. Der
Algorithmus ist gegen zehn echte Kassen-IK aus
`gkv-core/src/main/resources/billing-office-endpoints.json` verifiziert.

**Beim Anlegen von Testdaten immer ein IK mit gültiger Prüfziffer verwenden**,
sonst weist die Validierung zu Recht ab. Brauchbare echte Werte:

| IK | Kasse |
|---|---|
| 108310400 | AOK Bayern |
| 104940005 | BARMER |
| 102137985 | Techniker Krankenkasse |
| 101560000 | DAK-Gesundheit |
| 105508890 | Knappschaft |

## Die Leistungszeile ENF

Das inhaltlich dichteste Segment. Feldreihenfolge laut `segments/enf.json`:

```
ENF + 01 + 61:00000 + 306050601 + 08,00 + 15000,00 + 20240301 + 0,00 '
      1     2          3           4       5          6          7
```

1. Zeilen-Identifikationsnummer, fortlaufend innerhalb einer `INV`
2. Leistungserbringergruppe als Kompositfeld `Abrechnungscode:Tarifkennzeichen`
3. Abrechnungspositionsnummer, muss vertraglich gültig sein
4. Anzahl/Menge, zwei Nachkommastellen
5. Durchschnittlicher Einzelbetrag, zwei Nachkommastellen
6. Leistungsdatum `yyyyMMdd`
7. Zuzahlung des Versicherten (Kannfeld)

Feld 4 mal Feld 5 muss die Fallsumme im `BES` ergeben. Im Projekt stellt
`DtaFactory` sicher, dass Menge und Summe aus derselben Zahl entstehen — das
war einmal nicht so und erzeugte bei null Terminen eine Rechnung über
15.000,00 bei einer Menge von 0,00.

## Codes

| Code | Bedeutung | Ort |
|---|---|---|
| `SLGA` / `SLLA` | Nachrichtentypen | `UNH` |
| `01` | Normale Abrechnung | `FKT`, Verarbeitungskennzeichen |
| `1` | Rechnungsart | `REC`, Feld 4 |
| `00000000:0` | Rechnungsnummer im Format `Sammel:Einzel` | `REC` |
| `61` | Abrechnungscode SGS H (Rehabilitationssport) | `ENF` |
| `00` / `99` | Summenstatus | `GES` |
| `19` | Umsatzsteuersatz | `UST` |

Vollständig in `Information/codes/00_code_liste_kompakt.json` und den
nummerierten Einzeldateien daneben.

## Vorgehen bei einer abgewiesenen Lieferung

1. Datei durch die Validierung des Projekts schicken:
   `DtaValidationService.standard().pruefe(inhalt)`. Sie meldet alle Befunde auf
   einmal.
2. Meldet die Kasse einen **Syntaxfehler**, sind die Zähler und die Struktur zu
   prüfen: `UNT`-Segmentanzahl, `UNZ`-Nachrichtenanzahl, Abschlusszeichen,
   Feldverschiebung im `ENF`.
3. Meldet die Kasse eine **Zurückweisung**, ist es inhaltlich: IK-Prüfziffer,
   Versichertennummer, Geburtsdatum, Beträge.
4. Zum Vergleich `Information/Valide.DTA` danebenlegen — die Datei ist bekannt
   gültig.

## Fallstricke

- **`'` am Zeilenende vergessen.** Fällt beim Lesen kaum auf, macht das Segment
  aber ungültig.
- **Punkt statt Komma im Betrag.** `15000.00` ist falsch.
- **Zähler nach einer Änderung nicht angepasst.** Wer ein Segment ergänzt, muss
  `UNT` mitzählen.
- **Feldverschiebung durch weggelassenes Leerfeld.** Zwei `++` sind bedeutsam.
- **IK ohne gültige Prüfziffer**, besonders in Testdaten.
- **Datenaustauschreferenz doppelt vergeben.** Die Nummer kommt aus der
  Datenbank und darf nicht fest gesetzt werden.
