---
title: "Teil 01 — Vision"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Eine freiberuflich tätige Hebamme gibt Kurse — Geburtsvorbereitung, Rückbildung,
Beratung in Gruppen. Die Teilnehmerinnen sind gesetzlich krankenversichert und
zahlen diese Kurse in der Regel nicht selbst: **abgerechnet wird mit ihrer
Krankenkasse.**

Das ist kein Rechnungsschreiben. Die Kassen nehmen dafür weder Briefe noch
PDF-Dateien entgegen, sondern eine strukturierte Datenlieferung in einem
festgelegten Format — **bis auf das einzelne Zeichen vorgegeben.**

Für eine einzelne Hebamme gibt es dafür zwei Wege:

| Weg | Was er bedeutet | Was er kostet |
|---|---|---|
| **Über eine Abrechnungsstelle** | Ein Dienstleister übernimmt Erstellung, Übermittlung und das Nachhalten der Zahlungen | einen Anteil des abgerechneten Betrags, üblich sind wenige Prozent |
| **Selbst** | Ein eigenes Programm erzeugt die Datei | die Einarbeitung in das Verfahren |

Die Daten, aus denen eine Abrechnung besteht, liegen ohnehin vor: wer war in
welchem Kurs, an wie vielen Terminen, bei welcher Kasse versichert. **Sie liegen
nur in Listen und Köpfen statt in einer Datei.**

## Ziel dieses Teils

- Festlegen, was das Programm können soll — und ebenso wichtig, was nicht.
- Ein erstes Bild der Fachlichkeit: welche Begriffe es gibt, wie sie
  zusammenhängen, was von ihnen dauerhaft festgehalten werden muss.

## Überlegungen

### Was der eigentliche Wert wäre

Der naheliegende Gedanke — ein Programm, das eine Datei erzeugt — ist richtig
und zu kurz gedacht. Eine Datei zu erzeugen ist der leichte Teil. Der schwere
ist, dass sie **stimmt**.

Eine Abrechnung, die formal fehlerfrei aussieht und einen falschen Schlüsselwert
enthält, wird angenommen, verarbeitet und zurückgewiesen — oft Wochen später.
**Fehler in diesem Verfahren sind selten laut. Sie sind still und teuer.**

> **Der Leitgedanke des Projekts: nicht das Abrechnen ist schwer, sondern das
> Richtigsein.**

Daraus folgt die Festlegung, die den ganzen Aufbau prägt: **das Programm prüft,
bevor etwas das Haus verlässt** — nicht als Zusatzfunktion, sondern als Tor, an
dem jede Lieferung vorbeimuss.

### Wer es bedient

Eine einzelne Person, an einem einzelnen Rechner, ohne besondere
Computerkenntnisse und ohne jemanden, der bei Problemen hilft. Meist abends,
nach den Kursen.

| Umstand | Was daraus für das Programm folgt |
|---|---|
| Niemand richtet etwas ein | Es darf **nichts zusätzlich installiert** werden müssen — keine Datenbank, keine Laufzeitumgebung, kein Serverdienst |
| Es geht um Gesundheitsdaten von Schwangeren | Die Daten bleiben **auf dem Rechner der Anwenderin**, nicht im Netz |
| Niemand hilft, wenn etwas klemmt | Eine Meldung sagt, **was zu tun ist** — nicht, was schiefging. „Feld 4 ungültig" hilft niemandem |
| Abends, nach den Kursen | Der Weg von den Kursdaten zur fertigen Datei muss kurz sein |

### Was es ausdrücklich nicht wird

So wichtig wie die andere Liste — ein Projekt ohne Grenzen wird nie fertig.

| Nicht | Warum |
|---|---|
| Kursverwaltung, Terminplanung, Erinnerungen | ein eigenes Feld, und dafür gibt es Werkzeuge |
| Buchhaltung, Steuer, Einnahmenüberschussrechnung | ebenso, und mit eigenen Vorschriften |
| Privatabrechnung mit Selbstzahlern | ein anderes Verfahren mit anderen Regeln |
| Mehrbenutzerbetrieb, Praxisverwaltung | eine bewusste Grenze; die gewählte Datenhaltung lässt genau einen Schreiber zu |
| Fachliche Beratung, welche Leistung abrechenbar ist | das entscheidet der Vertrag, nicht ein Programm |

## Entscheidung

**Das Projekt wird ein Einzelplatzprogramm, das aus vorhandenen Kursdaten eine
prüffähige Abrechnungsdatei erzeugt — und das jede Datei prüft, bevor sie
hinausgeht.**

| # | Kernfähigkeit | Was dahintersteckt |
|---|---|---|
| 1 | **Stammdaten führen** | Teilnehmerinnen mit Versicherungsangaben, die Leistungserbringerin selbst, Gruppen als Zusammenfassung eines Kurses |
| 2 | **Leistungen beschreiben** | was ein Kurs kostet, unter welcher Position er abgerechnet wird — einmal festgelegt, mehrfach verwendet |
| 3 | **Abrechnung erzeugen** | aus Gruppe, Leistungsbeschreibung und Terminanzahl entsteht die Nachricht im vorgeschriebenen Format |
| 4 | **Prüfen** | Aufbau, Zähler, Summen, Pflichtangaben — ein Fehler hält den gesamten Lauf auf |
| 5 | **Zustellen und einordnen** | die Datei an den richtigen Empfänger, und die Antwort verständlich machen |

### Warum nicht der einfachere Weg

Man könnte auf Nummer 4 verzichten und darauf setzen, dass die Empfängerseite
schon meckern wird — das wäre erheblich weniger Arbeit. Dagegen steht der Preis
einer Beanstandung:

| Wo der Fehler auffällt | Was er kostet |
|---|---|
| im eigenen Programm, vor dem Versand | dreißig Sekunden |
| bei der Kasse, Wochen später | ein vollständiger Wiederholungsdurchlauf |

Ohne die Prüfung wäre das Programm ein Dateischreiber, und dafür lohnt der
Aufwand nicht.

## Ergebnis

### Das Zielbild

| Station | Was dort geschieht |
|---|---|
| **Kursdaten** | liegen ohnehin vor — in Listen und Köpfen |
| **Das Programm** | erzeugt daraus die Abrechnungsdatei **und prüft sie, bevor sie das Haus verlässt** |
| **Die Krankenkasse** | zahlt oder beanstandet |

### Die Begriffe und ihre Beziehungen

Bevor entschieden werden kann, wie etwas gebaut wird, muss feststehen, **wovon
überhaupt die Rede ist** — noch nicht als Klassen eines Programms, sondern als
Landkarte des Gegenstands.

![Fachliches Übersichtsmodell](../GKVTransmitter_Vision_Fachmodell.png)

Drei Beobachtungen aus diesem Bild prägen alles Weitere:

| Beobachtung | Was daraus folgt |
|---|---|
| **Die Teilnehmerin ist nicht die Zahlende** — zwischen Leistung und Geld steht die Kasse | An jeder Teilnehmerin hängt ein zweiter Satz Angaben: Versichertennummer, Status, Kasse. Mit dem Kurs hat er nichts zu tun, ohne ihn geht trotzdem nichts |
| **Die Lieferung ist eine eigene Sache**, kein bloßer Versandvorgang | Sie bündelt mehrere Abrechnungen an *einen* Empfänger und führt eine laufende Nummer, die sich nie wiederholen darf. Was eine Nummer führt und einen Zustand hat, gehört ins Modell |
| **Zwischen Erzeugen und Versenden steht ein Tor** | Der Prüfbericht ist kein Nebenprodukt, sondern die Bedingung dafür, dass die Lieferung hinausgeht. Er hängt deshalb an der Lieferung und nicht neben ihr |

Auffällig ist außerdem: Die Lieferung geht **nicht an die Krankenkasse**, sondern
an eine Datenannahmestelle. Wie sich diese Unterscheidung auswirkt, war zu
diesem Zeitpunkt nicht absehbar — sie wird in Teil 04 zum Problem.

### Was dauerhaft festgehalten werden muss

Aus denselben Begriffen ergibt sich ein erster Entwurf der Datenhaltung. Er ist
bewusst noch fachlich gehalten: **die Schlüssel sind hier Gedanken, keine
Spalten.**

![Erstes Datenmodell](../GKVTransmitter_Vision_ER.png)

Die Dreiteilung ist die eigentliche Aussage des Bildes:

| Bereich | Kennzeichen |
|---|---|
| **Stammdaten** | werden gepflegt, ändern sich selten |
| **Bewegungsdaten** | entstehen im Betrieb, wachsen mit jedem Lauf |
| **Betriebsdaten** | einmal eingerichtet — gehören trotzdem in die Ablage und nicht in eine Datei daneben |

Vier Festlegungen fallen hier bereits, jede mit einem Grund, der später gebraucht
wird:

| Festlegung | Grund |
|---|---|
| Eine Abrechnung gehört zu **genau einer** Lieferung | Trifft eine Zurückweisung ein, muss beantwortbar sein, was eigentlich wohin gegangen ist |
| Ein Befund hängt an der **Lieferung**, nicht an der Abrechnung | Zähler, Summen und Rahmen betreffen die Lieferung als Ganzes und hätten an keiner einzelnen Abrechnung einen Platz |
| Der Zähler bekommt eine **eigene Ablage** statt eines Werts im Arbeitsspeicher | Eine Nummer, die nach dem Neustart von vorn zählt, erzeugt eine doppelte Datenaustauschreferenz — ein Zurückweisungsgrund, der sich nicht mehr aus der Datei heraus reparieren lässt |
| Einstellungen als **Schlüssel-Wert-Vorrat** statt fester Spalten | Eine neue Einstellung soll keine Änderung am Datenmodell verlangen |

### Woran sich Erfolg messen lässt

In dieser Reihenfolge:

| Maßstab | Warum dieser |
|---|---|
| Eine erzeugte Datei wird ohne Beanstandung angenommen | der eigentliche Zweck |
| Ein Fehler in den Daten fällt vor dem Versand auf | der eigentliche Wert |
| Der Weg von den Kursdaten zur Datei dauert Minuten | sonst benutzt es niemand |
| Die Anwenderin versteht, was das Programm ihr sagt | sonst ruft sie doch wieder an |

## Offen geblieben

- **Wie das Format genau aussieht** und welche Vorgaben verbindlich sind — das
  ist Gegenstand von Teil 02.
- **Was von der Vision überhaupt erreichbar ist.** Der Versand an eine
  Krankenkasse ist kein technisches Problem allein; es gibt Zulassungen und
  Nachweise, die niemand programmieren kann. Teil 04 grenzt das ab.
- **Ob sich der eigene Weg wirtschaftlich lohnt** gegenüber einer
  Abrechnungsstelle. Diese Frage stellt sich am Ende noch einmal, mit Zahlen —
  siehe Teil 23.
- **Wie aus den Begriffen Klassen werden** und aus dem Datenmodell ein Schema.
  Die Umsetzung folgt in den Teilen 06 und 07. Dass die beiden Bilder dort nicht
  mehr gleich aussehen werden, ist zu erwarten — **wo sie abweichen, ist die
  interessante Stelle.**
