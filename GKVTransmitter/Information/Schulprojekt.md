---
title: "GKV-Abrechnung — Aufbau der Projektdokumentation"
subtitle: "Dreiundzwanzig Teile von der Vision bis zum Abschluss"
lang: de
toc-title: "Inhaltsverzeichnis"
---

# Wozu dieses Dokument

Das Projekt soll vollständig dokumentiert werden — **von der ersten Idee bis zum
fertigen Programm**, in dreiundzwanzig aufeinander aufbauenden Teilen.

Jeder Teil entsteht in einer Doppelstunde. Planung und Umsetzung wechseln sich
ab: erst wird entschieden, dann gebaut, dann wieder entschieden. Am Ende liegt
eine Reihe vor, die den Weg nachvollziehbar macht — nicht als Rückblick in einem
Stück, sondern als Kette, in der jeder Teil auf dem vorigen steht.

Dieses Dokument ist der Bauplan dafür. **Es gehört nicht in die Abgabe**; die
Abgabe sind die dreiundzwanzig Teile selbst.

# Der Kniff, und wie man ihn ehrlich handhabt

Das Programm steht bereits. Die Dokumentation beschreibt also einen Weg, der
schon gegangen wurde.

Das ist kein Nachteil, sondern ein Vorteil — **aber nur, wenn man es richtig
anfasst.** Zwei Regeln:

**Beschreibe, was entschieden wurde, und warum.** Alle Entscheidungen in diesem
Projekt sind echt getroffen worden: die Trennung in zwei Module, die Prüfung als
Tor vor dem Versand, der Verzicht auf Dialogfenster. Sie zu beschreiben ist keine
Erfindung, sondern Rekonstruktion.

**Behalte die Sackgassen.** Ein Weg ohne Umwege sieht erfunden aus, und er ist
langweilig. Dieses Projekt hat echte:

- Ein Wert im Nachrichtenkopf stand fest auf einem Buchstaben aus der
  Beispieldatei. Die Datei übersetzt fehlerfrei, prüft fehlerfrei — und wird von
  der Kasse zurückgewiesen.
- Achtundneunzig Testdateien landeten im echten Ausgabeordner, weil ein Werkzeug
  nur den Datenbankpfad umlenkte und nicht das Verzeichnis.
- Eine Rückfallebene stand auf einem plausiblen Betrag statt auf null — und
  rechnete jeden Termin mit fünfzehntausend Euro ab, ohne dass eine Prüfung
  anschlug.

**Jede dieser Stellen ist mehr wert als zwei Seiten „hat funktioniert".** Sie
zeigen, dass jemand hingesehen hat.

# Stil

| | |
|---|---|
| **Keine Datumsangaben** | Die Teile sind nummeriert, nicht datiert. Ein Datum bindet die Reihe an einen Kalender, den niemand nachprüfen kann und der nichts erklärt. |
| **Keine Werkzeugnamen** | Es geht um Entscheidungen und ihre Gründe, nicht um die Hilfsmittel, mit denen getippt wurde. |
| **Sachliche Prosa** | Ganze Sätze, keine Stichpunktwüsten. Tabellen dort, wo etwas vergleichbar ist. |
| **Fachbegriffe erklärt** | Beim ersten Auftreten. Wer das liest, kennt die Abrechnung mit Krankenkassen nicht. |

> Falls die Schule einen Nachweis über verwendete Hilfsmittel verlangt, gehört
> der in den Anhang der Abgabe. Das ist eine Formfrage und keine Stilfrage.

# Aufbau eines Teils

```markdown
# Teil NN — <Titel>

## Ausgangslage
Wo steht das Projekt zu Beginn dieses Teils? Ein Absatz, der an den
vorigen anschließt.

## Ziel
Was soll am Ende dieses Teils entschieden oder gebaut sein?

## Überlegungen
Der eigentliche Text. Welche Möglichkeiten gab es, was spricht wofür.

## Entscheidung
Was wurde gewählt — und **warum die Alternative nicht**.

## Ergebnis
Was liegt jetzt vor: Diagramm, Klasse, Datei, Erkenntnis.

## Offen geblieben
Was in diesem Teil nicht beantwortet wurde und wo es wieder auftaucht.
```

Der Abschnitt **Entscheidung** ist der wichtigste. Eine Dokumentation, die nur
beschreibt *was* gebaut wurde, ist ein Bautagebuch. Eine, die zeigt *warum so
und nicht anders*, ist eine Projektdokumentation.

# Die dreiundzwanzig Teile

## Grundlegung

| Nr | Titel | Worum es geht | Ergebnis |
|---|---|---|---|
| **01** | **Vision** | Was soll das Programm können, für wen, und woran misst man Erfolg | Visionsdokument |
| **02** | **Fachliche Grundlagen** | Wie funktioniert die Abrechnung mit gesetzlichen Krankenkassen überhaupt | Begriffsmodell |
| **03** | **Anforderungen** | Was das Programm können muss, was es leisten muss, was es nicht soll | Anforderungsliste |
| **04** | **Machbarkeit und Grenzen** | Was ist erreichbar, was hängt an Dritten | Abgrenzungsdokument |

## Entwurf

| Nr | Titel | Worum es geht | Ergebnis |
|---|---|---|---|
| **05** | **Architektur** | Aufteilung in Module, Schichten, warum die Grenze erzwungen wird | **Komponentendiagramm** |
| **06** | **Fachliches Modell** | Die Begriffe der Domäne als Klassen | **Klassendiagramm** |
| **07** | **Datenhaltung** | Was gespeichert wird und wie | **ER-Diagramm** |
| **08** | **Technologieauswahl** | Sprache, Oberfläche, Datenbank — mit Begründung und Verworfenem | Entscheidungsdokument |

## Umsetzung, erster Durchgang

| Nr | Titel | Worum es geht | Ergebnis |
|---|---|---|---|
| **09** | **Persistenz** | Der Zugang zur Datenbank hinter einer Schnittstelle | Code + Tests |
| **10** | **Domänenmodell** | Personen, Gruppen, Blaupausen, Abrechnungen | Code + Tests |
| **11** | **Nachrichtenformat** | Aufbau der Abrechnungsdatei, Segmente, Felder | **Aufbaudiagramm** |
| **12** | **Nachrichtenerzeugung** | Aus Stammdaten wird eine Datei | Code + Beispieldatei |

## Planung und Umsetzung der Prüfung

| Nr | Titel | Worum es geht | Ergebnis |
|---|---|---|---|
| **13** | **Warum geprüft wird** | Prüfstufen, Fehler gegen Warnung, was eine Zurückweisung kostet | Prüfkonzept |
| **14** | **Das Prüfwerk** | Regeln als austauschbare Einheiten | Code + **Klassendiagramm** |
| **15** | **Der Ablauf eines Laufs** | Erst alles prüfen, dann alles zustellen | **Sequenzdiagramm** |

## Oberfläche

| Nr | Titel | Worum es geht | Ergebnis |
|---|---|---|---|
| **16** | **Bedienkonzept** | Wer bedient das, unter welchen Umständen, und was folgt daraus | **Mockups** |
| **17** | **Stammdatenmasken** | Erfassen, ändern, löschen | Code + Bildschirmfotos |
| **18** | **Abrechnungsmaske und Meldungen** | Der Weg von der Auswahl bis zur Rückmeldung | Code + Bildschirmfotos |

## Versand, Güte, Auslieferung

| Nr | Titel | Worum es geht | Ergebnis |
|---|---|---|---|
| **19** | **Versandwege** | Empfänger, Transporte, Rückmeldungen einordnen | **Verteilungsdiagramm** |
| **20** | **Fehlerbehandlung** | Was geschieht bei einer Zurückweisung | **Aktivitätsdiagramm** |
| **21** | **Qualitätssicherung** | Teststrategie, was wie geprüft wird, was bewusst nicht | Testkonzept |
| **22** | **Auslieferung** | Paket, Datenablage, Konfiguration | Betriebsdokument |

## Abschluss

| Nr | Titel | Worum es geht | Ergebnis |
|---|---|---|---|
| **23** | **Rückblick und Ausblick** | Was gelernt wurde, was offen ist, wie es weiterginge | Abschlussdokument |

# Die Diagramme

Elf Stück, verteilt über die Reihe — nicht am Ende in einem Schwung.

| Diagramm | Teil |
|---|---|
| Fachliches Übersichtsmodell (Entwurf) | 01 |
| Erstes Datenmodell (Entwurf) | 01 |
| Komponenten- und Abhängigkeitsdiagramm | 05 |
| Klassendiagramm der Domäne | 06 |
| ER-Diagramm | 07 |
| Aufbau der Nachricht | 11 |
| Klassendiagramm des Prüfwerks | 14 |
| Sequenzdiagramm des Abrechnungslaufs | 15 |
| Mockups | 16 |
| Verteilungsdiagramm | 19 |
| Aktivitätsdiagramm der Fehlerbehandlung | 20 |

**Acht Bilder liegen bereits vor**: `GKVTransmitter_Vision_Fachmodell` und
`_Vision_ER` in Teil 01, dazu `_Uebersicht`, `_Domaene`, `_DTA_und_Versand`,
`_Praesentation_und_Persistenz`, `_Abhaengigkeiten` und `_Datenbank_ER` in der
Gesamtdokumentation.

Von den elf Zeilen oben sind damit **fünf abgedeckt** — die Teile 01, 05, 06 und
07. Die übrigen sechs entstehen dort, wo sie gebraucht werden.

Die beiden Bilder aus Teil 01 sind **absichtlich Entwürfe**. Sie zeigen die
Begriffe, bevor entschieden war, wie sie umgesetzt werden. Ihre späteren
Gegenstücke in den Teilen 06 und 07 weichen davon ab — und genau diese
Abweichung ist ein eigener Gegenstand der Dokumentation: sie zeigt, was sich
beim Bauen als anders herausgestellt hat.

Das ER-Diagramm ist **aus dem tatsächlichen Datenbankschema ausgelesen**, nicht
aus den Klassen abgeleitet. Der Unterschied ist keiner auf dem Papier: dabei kam
heraus, dass das Schema **keine Fremdschlüssel** enthält und dass `Person` als
Oberklasse ohne eigene Tabelle ihre Felder in zwei Tabellen dupliziert. Beides
steht so in keiner Klasse.

Alle als Quelltext, aus dem die Bilder erzeugt werden — **ein Diagramm, das man
von Hand malt, veraltet.** Dasselbe gilt für die Bildschirmfotos: das Projekt
zeichnet alle Masken ohne geöffnetes Fenster, in heller und dunkler Fassung.

# Die architektonischen Mittel

Sie tauchen dort auf, wo sie eingesetzt wurden — nicht in einer Liste am Ende.
Zur Übersicht, welcher Teil welches Mittel behandelt:

| Mittel | Teil |
|---|---|
| Ports und Adapter | 05, 09 |
| Modulgrenze im Bauvorgang erzwungen | 05 |
| Schichtung | 05 |
| Strategiemuster für die Prüfregeln | 14 |
| Registry für die Empfänger | 19 |
| Fabrikmethode für die Nachricht | 12 |
| Anwendungsfall-Dienst | 15 |
| Konstruktorinjektion ohne Rahmenwerk | 09, 17 |
| Wertobjekte | 10, 14 |
| Testdoubles statt Attrappenbibliothek | 21 |
| Rückruf statt blockierender Rückgabe | 18 |
| Konfiguration von außen | 22 |

Teil 23 fasst sie zusammen und beantwortet die Frage, welche sich bewährt haben
und welche man beim nächsten Mal anders wählen würde.

# Ablage

```
Information/dokumentation/
├── erzeugen.sh                 erzeugt alle .docx
├── 00-Vorlage.md               Muster für einen Teil
├── 00-Gesamtdokumentation.md   zusammengefügt, abgeleitet
├── 01-Vision.md
├── 02-Fachliche-Grundlagen.md
├── …
├── 23-Rueckblick.md
└── bilder/
```

Erzeugt werden die Word-Fassungen mit einem Aufruf:

```bash
cd Information/dokumentation
bash erzeugen.sh
```

Daraus entstehen **je Teil eine `.docx`** und zusätzlich
`00-Gesamtdokumentation.docx`, in der alle Teile als Kapitel stehen — mit
Inhaltsverzeichnis. Die Markdown-Dateien sind die Quelle, die Word-Fassungen
sind abgeleitet und nicht von Hand zu bearbeiten.

Die einzelnen Teile bleiben neben der Gesamtfassung stehen: sie sind der
Nachweis, dass der Weg schrittweise gegangen wurde.
