---
title: "GKV-Abrechnung — schulische Aufarbeitung"
subtitle: "Sitzungsplan, Berichtsvorlage und Abgabemappe"
date: "9. September 2026"
lang: de
toc-title: "Inhaltsverzeichnis"
---

# Was verlangt ist

Das Projekt soll für die Schule aufgearbeitet werden. Aus der Aufgabe:

- **Einteilung des Fortschritts** in die verbleibenden Wochen bis Ende Februar,
  je **90 Minuten** (zwei Unterrichtsstunden) an einem Tag
- **jedes Mal ein Dokument** über den aktuellen Fortschritt
- am Ende **mehrere Dokumentationen**, die den Fortschritt belegen
- **Mockups**
- **neu erstellte UML-Diagramme**, eventuell weitere Diagramme
- eine Darstellung, **welche architektonischen Mittel** eingesetzt wurden

# Der Grundsatz, auf dem alles steht

> **Das Dokument ist das Ergebnis, nicht der Code.**

Das klingt nach Formsache und ist das Gegenteil. Es bedeutet: **eine Sitzung, in
der nichts funktioniert hat, ist keine verlorene Sitzung.** Sie ist ein Bericht
darüber, warum etwas nicht ging — und das ist regelmäßig der lehrreichere Text.

Dieses Projekt hat davon reichlich, und zwar gute:

- Der Leistungsbereich im `UNB` stand fest auf `H` — dem Wert aus der
  Beispieldatei. Für eine Hebamme ist er `F`. Übersetzt fehlerfrei, prüft
  fehlerfrei, **wird von der Kasse zurückgewiesen.**
- 98 Testdateien landeten im echten Ausgangsordner, weil ein Werkzeug nur den
  Datenbankpfad umlenkte und nicht den Ordner.
- Warnungen erreichen den Bildschirm bis heute nicht, obwohl die Dokumentation
  jahrelang das Gegenteil behauptete.

**Solche Stellen sind in einer Schulmappe mehr wert als zwanzig Seiten „hat
funktioniert".** Sie zeigen, dass jemand hingesehen hat.

# Wie viele Sitzungen es sind

Vom 9. September 2026 bis Ende Februar 2027:

| Monat | Kalenderwochen | Sitzungen |
|---|---|---|
| September | 37 – 40 | 4 |
| Oktober | 41 – 44 | 4 |
| November | 45 – 48 | 4 |
| Dezember | 49 – 51 | 3 |
| Januar | 2 – 5 | 4 |
| Februar | 6 – 9 | 4 |
| | **rechnerisch** | **23** |

Abzüglich Herbstferien (je nach Bundesland ein bis zwei Wochen) und
Weihnachtsferien bleiben **rund 20 Sitzungen**. Der Plan unten hat **20
nummerierte Sitzungen plus zwei Puffer** — wer schneller ist, zieht vor; wer
hängenbleibt, verliert nicht den Abschluss.

**90 Minuten sind kurz.** Nach Hochfahren, Hineindenken und Aufräumen bleiben
etwa 60 Minuten echte Arbeit. Deshalb hat jede Sitzung **ein** Ziel, nicht drei.

# Die Vorlage für den Fortschrittsbericht

Eine Datei je Sitzung, `dokumentation/NN-kurzname.md`. Immer dieselben
Abschnitte — dann sieht man beim Durchblättern die Entwicklung statt
verschiedener Aufsätze.

```markdown
# Sitzung NN — <Thema>

Datum · Dauer · Kalenderwoche

## Ziel dieser Sitzung
Ein Satz. Was sollte am Ende können, was vorher nicht ging?

## Was entstanden ist
Kurz und sachlich. Dateien, Klassen, Diagramme.

## Die Entscheidung dieser Sitzung
Jede Sitzung enthält eine. Welche Möglichkeiten gab es, welche wurde
gewählt, und **warum die andere nicht**?

## Was nicht funktioniert hat
Ehrlich. Auch „nichts" ist eine Antwort — aber selten die richtige.

## Belege
- Commit: `abc1234`
- Testlauf: 430 Tests, grün
- Bildschirmfoto: `bilder/NN-....png`

## Nächster Schritt
Ein Satz, damit die nächste Sitzung nicht mit Suchen beginnt.
```

Der Abschnitt **„Die Entscheidung dieser Sitzung"** ist der wichtigste. Eine
Projektdokumentation, die nur beschreibt *was* gebaut wurde, ist ein
Bautagebuch. Eine, die zeigt *warum so und nicht anders*, ist eine
Projektdokumentation.

> **Jede Sitzung endet mit einem Commit.** Dann belegt `git log` den Fortschritt
> von selbst — mit Datum, Umfang und Beschreibung. Das ist der billigste Nachweis
> der Welt, und er lässt sich nicht nachträglich erfinden.

# Der Sitzungsplan

## Teil 1 — Grundlage schaffen (Sitzungen 1–4)

Kein Code. Der Ist-Zustand wird beschrieben, und dabei entstehen die Diagramme,
die man später ohnehin braucht.

| Nr | Thema | Am Ende liegt vor |
|---|---|---|
| **1** | **Projektauftrag und Ausgangslage** — worum geht es fachlich, wer sind die Beteiligten, was ist der Nutzen | `01-Projektauftrag.md` |
| **2** | **Ist-Analyse** — was kann das Programm heute, was nicht | `02-Ist-Analyse.md` + **Komponentendiagramm** (die zwei Module und ihre Grenze) |
| **3** | **Architekturmittel** — siehe eigenes Kapitel unten | `03-Architektur.md` + **Klassendiagramm** des Kerns |
| **4** | **Fachlicher Ablauf** — von den Stammdaten bis zur zugestellten Datei | `04-Ablauf.md` + **Sequenzdiagramm** des Abrechnungslaufs |

Sitzung 3 ist die, die der Aufgabenstellung am unmittelbarsten entspricht. Sie
ist gut vorbereitet: die Mittel sind vorhanden, sie müssen nur benannt und
belegt werden.

## Teil 2 — Bauen mit Nachweis (Sitzungen 5–14)

Jede Sitzung nimmt ein Arbeitspaket aus dem `Umsetzungsplan.md` und
dokumentiert es. **Das ist der Teil, der echten Fortschritt belegt.**

| Nr | Thema | Am Ende liegt vor |
|---|---|---|
| **5** | **Spezifikation schreiben** für „Warnungen sichtbar machen" (1.1) nach `spezifikationen/VORLAGE.md` | `05-Spezifikation.md` — zeigt die Arbeitsweise, nicht nur das Ergebnis |
| **6** | 1.1 umsetzen, Teil 1: der Kern liefert den Bericht mit | Commit, Tests |
| **7** | 1.1 abschließen: die Oberfläche zeigt Warnungen | `07-Warnungen.md` mit **Vorher/Nachher-Bild** |
| **8** | **Verarbeitungskennzeichen** (1.4) — Korrekturrechnung wird möglich | `08-Korrekturrechnung.md` |
| **9** | **Kostenträgerdatei lesen**, Teil 1: das Format verstehen und eine echte Datei zerlegen | `09-Kostentraegerdatei.md` |
| **10** | Kostenträgerdatei, Teil 2: der Leser | Commit, Tests |
| **11** | **Empfängerermittlung** fertig — aus 23 Kassen wird die richtige Annahmestelle | `11-Empfaenger.md` + **Sequenzdiagramm** der Auflösung |
| **12** | **Lebenszyklus einer Abrechnung** entwerfen | `12-Lebenszyklus.md` + **Zustandsdiagramm** |
| **13** | **Übermittlungsprotokoll** (1.5), Teil 1: Datenmodell und Speicherung | Commit + **ER-Diagramm** |
| **14** | Protokoll, Teil 2: die Übersicht in der Oberfläche | `14-Protokoll.md` |

Die Sitzungen 9 bis 11 sind der fachlich interessanteste Block: eine echte
EDIFACT-Datei einer Behörde einlesen und daraus den richtigen Empfänger
ermitteln. **Das ist der Teil, den man in einer Prüfung erzählen kann.**

## Teil 3 — Oberfläche und Bilder (Sitzungen 15–17)

| Nr | Thema | Am Ende liegt vor |
|---|---|---|
| **15** | **Mockups** für die Betriebsdatenmaske — erst zeichnen, dann bauen | `15-Mockups.md` + Entwurfsbilder |
| **16** | Betriebsdaten (1.7) umsetzen | Commit + Bildschirmfotos aus `Vorschau` |
| **17** | **Alle Diagramme neu erzeugen** und mit dem Stand abgleichen | `17-Diagramme.md` + aktualisierte `.puml` und `.png` |

Zu den Bildschirmfotos: das Projekt hat mit `Vorschau` bereits ein Werkzeug, das
alle Masken ohne Fenster in PNG zeichnet — **hell und dunkel.** Damit kostet ein
vollständiger Satz Bilder einen Befehl statt einer halben Stunde Klickerei.

## Teil 4 — Abgabe (Sitzungen 18–20 + Puffer)

| Nr | Thema | Am Ende liegt vor |
|---|---|---|
| **18** | **Gesamtdokumentation** zusammenführen, Widersprüche zwischen den Einzelberichten beseitigen | `00-Gesamtdokumentation.md` (+ `.docx`) |
| **19** | **Präsentation** — roter Faden, was zeigt man, was lässt man weg | Folien |
| **20** | **Probevortrag**, Nacharbeit | fertige Mappe |
| **P1/P2** | Puffer | — |

# Die Diagramme

Was verlangt ist, und woher es kommt:

| Diagramm | Zeigt | Sitzung | Grundlage |
|---|---|---|---|
| **Komponentendiagramm** | `gkv-core` und `gkv-ui`, die erzwungene Grenze | 2 | vorhanden, zu aktualisieren |
| **Klassendiagramm** | Kern: Entitäten, Repository-Port, Prüfwerk | 3 | `Projektklassen.puml` vorhanden |
| **Sequenzdiagramm** | Abrechnungslauf: erzeugen → prüfen → zustellen | 4 | neu |
| **Sequenzdiagramm** | Empfängerauflösung über die Kostenträgerdatei | 11 | neu |
| **Zustandsdiagramm** | Abrechnung: erzeugt → geprüft → zugestellt → beantwortet → bezahlt | 12 | neu |
| **ER-Diagramm** | Datenmodell der Persistenz | 13 | neu |
| **Aktivitätsdiagramm** | Fehlerkreislauf bei einer Zurückweisung | 17 | neu, Vorlage in `Umsetzungsplan.md` Teil 3 |
| **Verteilungsdiagramm** | Rechner, Datenannahmestelle, Kasse | 17 | neu |
| **Mockups** | die geplanten Masken | 15 | neu |

Die Diagramme entstehen als **PlantUML** im Quelltext und werden gerendert —
dann sind sie versioniert, änderbar und lassen sich in die Word-Fassung
einbetten:

```bash
cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
```

> **Ein Diagramm, das nicht aus einer Quelle erzeugt wird, veraltet.** Das ist
> derselbe Grund, aus dem die Word-Fassung aus der Markdown-Quelle entsteht und
> nicht von Hand gepflegt wird.

# Die architektonischen Mittel

Ein eigenes Dokument (Sitzung 3), weil die Aufgabe ausdrücklich danach fragt.
**Alle folgenden sind im Projekt tatsächlich umgesetzt** — es geht ums Benennen
und Belegen, nicht ums Erfinden.

| Mittel | Wo im Projekt | Wozu |
|---|---|---|
| **Ports und Adapter** | `DataRepository` (Port) ↔ `HibernateSqllite` (Adapter) | die Fachlichkeit kennt keine Datenbank |
| **Modulgrenze im Build erzwungen** | `maven-enforcer-plugin` verbietet `org.openjfx` in `gkv-core` | die Regel ist nicht Vereinbarung, sondern Bauergebnis |
| **Strategie** | `ValidationRule` mit acht Umsetzungen, gesammelt in `DtaValidationService` | eine neue Prüfregel ändert keinen bestehenden Code |
| **Registry** | `BillingOfficeEndpointRegistry` aus JSON | eine neue Kasse ist ein Eintrag, kein Commit |
| **Fabrikmethode** | `DtaFactory` | die Nachricht entsteht an einer Stelle |
| **Anwendungsfall-Dienst** | `AbrechnungService` | die Oberfläche ruft einen Satz auf, keine Kette |
| **Konstruktorinjektion ohne Rahmenwerk** | jede Maske bekommt alles übergeben | testbar ohne Behälter |
| **Wertobjekte** | `Leistungsparameter`, `ValidationMessage` als `record` | unveränderlich, vergleichbar |
| **Testdoubles statt Attrappen-Bibliothek** | `SpeicherRepository`, `AufzeichnendeMeldungen`, `AufzeichnenderRahmen` | lesbar, und sie können mehr als eine Attrappe |
| **Rückruf statt blockierender Rückgabe** | `Meldungen.frageNach(frage, knopf, wennBejaht)` | kein Dialogfenster, kein angehaltener Faden |
| **Konfiguration von außen** | `gkv.home`, `gkv.db.path` mit Umgebungsvariablen-Rückfall | derselbe Code in Test, Entwicklung und Auslieferung |
| **Sichtbares Scheitern** | Prüfbericht mit Stufe, Code, Ort, Text | ein Fehler, den niemand sieht, ist der teuerste |

Dazu zwei Entscheidungen, die keine Muster sind, aber die Architektur prägen:

- **Erst alles prüfen, dann alles zustellen.** Würde je Abrechnung geprüft und
  sofort zugestellt, läge bei einem Fehler in der Mitte eines Laufs bereits ein
  Teil bei der Kasse — und müsste dort einzeln storniert werden.
- **Die Prüfung liest die erzeugte Datei zurück** und arbeitet gegen die
  Struktur, nicht gegen Zeichenketten im Erzeuger. Dadurch lässt sich auch eine
  fremde Datei prüfen.

# Die Abgabemappe

```
Information/
├── Schulprojekt.md                 dieser Plan
├── dokumentation/
│   ├── 00-Gesamtdokumentation.md   am Ende zusammengeführt
│   ├── 01-Projektauftrag.md
│   ├── 02-Ist-Analyse.md
│   ├── …                           ein Bericht je Sitzung
│   └── bilder/                     Bildschirmfotos, Mockups
├── *.puml / *.png                  alle Diagramme mit Quelle
└── GKVTransmitter_Dokumentation.*  die fachliche Gesamtdokumentation
```

Am Ende liegen vor: **rund fünfzehn Fortschrittsberichte**, ein zusammengeführtes
Gesamtdokument, **neun Diagramme**, Mockups, Bildschirmfotos in zwei
Darstellungen, ein Architekturdokument — und ein `git log`, der jeden Schritt mit
Datum belegt.

# Zwei Ratschläge

**Schreib den Bericht in der Sitzung, nicht danach.** Die letzten zehn Minuten
gehören dem Dokument. Wer sich vornimmt, es abends nachzuholen, hat nach drei
Wochen sechs offene Berichte und erfindet sie am Ende — und man sieht es ihnen an.

**Kürz nicht die Fehlschläge heraus.** Die Versuchung ist groß, weil eine Mappe
ohne Fehler ordentlicher aussieht. Sie ist es nicht — sie sieht aus, als hätte
niemand etwas Schwieriges versucht. Die drei Funde aus dem ersten Kapitel sind
das Beste, was dieses Projekt vorzuweisen hat.
