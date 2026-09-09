---
title: "Teil 08 — Technologieauswahl"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Aufbau, Modell und Ablage sind entschieden — bisher ohne festzulegen, **womit**
gebaut wird. Das war Absicht: Eine Technikwahl, die vor den Anforderungen kommt,
begründet sich hinterher selbst.

Jetzt ist der Zeitpunkt, und die Anforderungen geben die Kriterien vor.

## Ziel dieses Teils

Sprache, Oberfläche, Ablage und Bauweise festlegen — jede Wahl gegen eine
Anforderung begründet, und mit Nennung dessen, was verworfen wurde.

## Überlegungen

### Was überhaupt entscheidet

Aus Teil 03 lassen sich vier Kriterien ableiten, die hier zählen:

| Kriterium | Woher |
|---|---|
| Auf dem Zielrechner ist nichts vorbereitet | Q1 |
| Nichts geht ins Netz | Q2 |
| Die Fachlichkeit muss ohne Fenster prüfbar sein | Q5 |
| Vorgaben sind Beschreibungen, kein Quelltext | Q7 |

Bemerkenswert ist, was **nicht** in der Liste steht: Geschwindigkeit,
Datenmenge, Anzahl gleichzeitiger Benutzer. Ein Einzelplatzprogramm mit einigen
hundert Datensätzen stellt an keinen dieser Punkte Ansprüche. Wer hier nach
Leistungsfähigkeit auswählt, optimiert etwas, das niemand misst.

### Die Sprache

| Möglichkeit | Dafür | Dagegen |
|---|---|---|
| Eine Sprache mit mitlieferbarer Laufzeit und großem Bestand an Bibliotheken | Q1 erfüllbar, viel Vorhandenes | Die Laufzeit macht das Paket groß |
| Eine Sprache, die zu einer einzelnen ausführbaren Datei übersetzt | kleinste Auslieferung | Weniger Vorhandenes für Datenbank und Oberfläche |
| Eine Sprache mit Laufzeit, die auf dem Zielrechner erwartet wird | kleinstes Paket | **Verstößt gegen Q1** — jemand müsste sie installieren |

Gewählt wurde die erste: **Java**, mit mitgeliefertem Laufzeitumfeld. Die dritte
Zeile schied aus, weil Q1 nicht verhandelbar ist — die Anwenderin richtet nichts
ein.

Das Paket wird dadurch groß. Das ist ein Preis, der einmal beim Kopieren anfällt
und danach nie wieder.

### Die Oberfläche

Erwogen wurden drei Wege:

| Weg | Dafür | Dagegen |
|---|---|---|
| Eine Oberfläche im Browser, bedient von einem lokalen Dienst | vertraute Bedienung | Ein Dienst, der läuft und einen Anschluss öffnet — Q1 und Q2 werden unnötig berührt |
| Der ältere mitgelieferte Oberflächenbaukasten | überall vorhanden | Aussehen und Gestaltungsmittel veraltet; Trennung von Stil und Aufbau schwierig |
| Der neuere Baukasten mit eigenem Stilblatt | Aussehen getrennt vom Aufbau, moderne Bedienelemente | Muss mit ausgeliefert werden |

Gewählt wurde der dritte. Ausschlaggebend war die **Trennung von Stil und
Aufbau**: Farben, Abstände und Schriftgrößen stehen an einer Stelle, nicht
verteilt im Quelltext. Das macht später aus einer Gestaltungsfrage eine
Textänderung.

### Der Zugang zur Datenbank

| Weg | Dafür | Dagegen |
|---|---|---|
| Abfragen von Hand schreiben | volle Kontrolle, keine Zusatzschicht | Jede Modelländerung ist Handarbeit an mehreren Stellen |
| Eine Abbildung zwischen Klassen und Tabellen | Schema wächst mit dem Modell | Eine Schicht, die man verstehen muss; sie tut Dinge ungefragt |

Gewählt wurde die Abbildung — mit einer Einschränkung, die aus Teil 07 stammt:
**Was sie ungefragt tut, muss man wissen.** Der Modus, der das Schema bei jedem
Start neu anlegt, ist genau so ein Fall.

### Wie gebaut und ausgeliefert wird

Zwei Module, deren Grenze im Bauvorgang erzwungen wird — das ist die Entscheidung
aus Teil 05, und sie setzt ein Bauwerkzeug voraus, das Module und Regeln kennt.

Für die Auslieferung wird ein Paket erzeugt, das Programm **und** Laufzeitumfeld
enthält. Damit ist Q1 erfüllt: Auf dem Zielrechner muss nichts installiert sein.

Eine Eigenheit dabei, die früh wehtut, wenn man sie nicht kennt: **Das Paket
entsteht immer für das System, auf dem es gebaut wird.** Ein Paket für Windows
lässt sich nicht auf einem anderen System erzeugen.

## Entscheidung

| Bereich | Gewählt | Wegen |
|---|---|---|
| Sprache | Java, Laufzeit wird mitgeliefert | Q1 |
| Oberfläche | der neuere Baukasten mit eigenem Stilblatt | Trennung von Stil und Aufbau |
| Ablage | serverlose Datenbank in einer Datei | Q1, Q2 |
| Datenbankzugang | Abbildung Klassen ↔ Tabellen | Modelländerungen ohne Handarbeit |
| Bauweise | zwei Module, Grenze erzwungen | Q5 |
| Auslieferung | Paket mit eigener Laufzeit | Q1 |
| Vorgaben | Beschreibungen in Textdateien, kein Quelltext | Q7 |

### Warum nicht der einfachere Weg

Die naheliegendste Vereinfachung wäre gewesen, auf das mitgelieferte Laufzeitumfeld
zu verzichten und es auf dem Zielrechner vorauszusetzen. Das Paket wäre um ein
Vielfaches kleiner.

Dagegen steht Q1, und dahinter eine Person: **Wer abends nach der Arbeit
abrechnen will, installiert keine Laufzeitumgebung.** Sie würde stattdessen
jemanden anrufen — und genau das soll das Programm überflüssig machen.

## Ergebnis

Eine vollständige Wahl, jede Zeile an eine Anforderung gebunden. Zwei Punkte sind
festzuhalten, weil sie später wiederkehren:

**Die Paketgröße ist bekannt und akzeptiert.** Sie ist kein Versehen und keine
Nachlässigkeit, sondern der Preis für Q1.

**Die Abbildung zwischen Klassen und Tabellen ist die riskanteste Wahl.** Sie
nimmt Arbeit ab und trifft dabei Entscheidungen, die man nicht sieht — das Schema
aus Teil 07 ist der Beleg: keine Fremdschlüssel, doppelte Spalten, eine zu enge
Textspalte. Nichts davon stand in einer Klasse.

## Offen geblieben

- **Ob das Paket auch für andere Systeme gebaut werden soll.** Technisch möglich,
  aber nur auf dem jeweiligen System selbst.
- **Wie die Abbildung überwacht wird.** Bisher gilt: nachsehen, was sie erzeugt
  hat. Das ist eine Gewohnheit, kein Verfahren — siehe Teil 21.
