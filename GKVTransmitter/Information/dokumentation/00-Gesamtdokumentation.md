---
title: "GKV-Abrechnung — Projektdokumentation"
subtitle: "Von der Vision bis zum Abschluss"
lang: de
toc-title: "Inhaltsverzeichnis"
---

# Teil 01 — Vision


## Ausgangslage

Wer als Freiberuflerin Leistungen für gesetzlich Versicherte erbringt, rechnet
sie nicht mit den Versicherten ab, sondern **mit deren Krankenkasse**. Das gilt
für Hebammenhilfe ebenso wie für Heilmittel oder Rehabilitationssport — die
Leistungen unterscheiden sich, das Verfahren dahinter kaum.

Der konkrete Anlass dieses Projekts sind Kurse einer freiberuflich tätigen
Hebamme: Geburtsvorbereitung, Rückbildung, Beratung in Gruppen. Er ist der erste
Fall, nicht der einzige denkbare.

Abgerechnet wird nicht mit einer Rechnung. Die Kassen nehmen dafür weder Briefe
noch PDF-Dateien entgegen, sondern eine strukturierte Datenlieferung in einem
festgelegten Format — **bis auf das einzelne Zeichen vorgegeben.**

Für eine einzelne Leistungserbringerin gibt es dafür zwei Wege:

| Weg | Was er bedeutet | Was er kostet |
|---|---|---|
| **Über eine Abrechnungsstelle** | Ein Dienstleister übernimmt Erstellung, Übermittlung und das Nachhalten der Zahlungen | einen Anteil des abgerechneten Betrags, üblich sind wenige Prozent |
| **Selbst** | Ein eigenes Programm erzeugt die Datei | die Einarbeitung in das Verfahren |

Die Daten, aus denen eine Abrechnung besteht, liegen ohnehin vor: wer hat wann
welche Leistung erhalten, in welchem Umfang, bei welcher Kasse versichert.
**Sie liegen nur in Listen und Köpfen statt in einer Datei.**

## Ziel dieses Teils

- Festlegen, was das Programm können soll — und ebenso wichtig, was nicht.
- Entscheiden, wie weit es tragen soll: ein Programm für einen Kursanbieter oder
  eines für die Abrechnung mit gesetzlichen Kassen überhaupt.
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

### Wie weit soll es tragen

Man könnte das Programm auf Hebammenkurse zuschneiden. Das wäre die kleinere
Aufgabe und für den ersten Fall vollkommen ausreichend.

Dagegen spricht ein Blick auf das Verfahren selbst: **fast alles daran ist nicht
leistungsspezifisch.**

| Bleibt über die Leistungsbereiche gleich | Wechselt je Leistungsbereich |
|---|---|
| Der Aufbau der Nachricht — Rahmen, Rechnungs-, Versicherten-, Leistungs- und Summenteile | Der Sammelgruppenschlüssel im Nachrichtenkopf |
| Die Beteiligten und ihre Institutionskennzeichen samt Prüfziffer | Der Abrechnungscode |
| Prüfung, Zählerführung, Summenbildung | Positionsnummern und Tarifkennzeichen |
| Lieferung, Zustellweg, Einordnung der Rückmeldung | Der Vertrag, aus dem sich die zulässigen Werte ergeben |

Die rechte Spalte ist kurz, und sie besteht ausschließlich aus **Werten**, nicht
aus Abläufen. Ein zweiter Leistungsbereich verlangt also Einträge, keinen Umbau
— vorausgesetzt, das Modell trennt beides von Anfang an.

Genau das kostet in der Vision fast nichts und später sehr viel: ein Begriff,
der von Beginn an „Leistungsfall" heißt statt „Kurs", trägt eine
Behandlungsserie mit, ohne dass jemand ihn umbenennen muss.

### Wer es bedient

Eine einzelne Person, an einem einzelnen Rechner, ohne besondere
Computerkenntnisse und ohne jemanden, der bei Problemen hilft. Meist abends,
nach der Arbeit.

| Umstand | Was daraus für das Programm folgt |
|---|---|
| Niemand richtet etwas ein | Es darf **nichts zusätzlich installiert** werden müssen — keine Datenbank, keine Laufzeitumgebung, kein Serverdienst |
| Es geht um Gesundheitsdaten | Die Daten bleiben **auf dem Rechner der Anwenderin**, nicht im Netz |
| Niemand hilft, wenn etwas klemmt | Eine Meldung sagt, **was zu tun ist** — nicht, was schiefging. „Feld 4 ungültig" hilft niemandem |
| Abends, nebenher | Der Weg von den erfassten Daten zur fertigen Datei muss kurz sein |

### Was es ausdrücklich nicht wird

So wichtig wie die andere Liste — ein Projekt ohne Grenzen wird nie fertig.

| Nicht | Warum |
|---|---|
| Kurs- und Terminverwaltung, Erinnerungen | ein eigenes Feld, und dafür gibt es Werkzeuge |
| Buchhaltung, Steuer, Einnahmenüberschussrechnung | ebenso, und mit eigenen Vorschriften |
| Privatabrechnung mit Selbstzahlern | ein anderes Verfahren mit anderen Regeln |
| Mehrbenutzerbetrieb, Praxisverwaltung | eine bewusste Grenze; die gewählte Datenhaltung lässt genau einen Schreiber zu |
| Ein mitgelieferter Katalog gültiger Positionsnummern je Bereich | die ergeben sich aus dem Vertrag. Das Programm hält den Rahmen bereit, die Werte trägt ein, wer den Vertrag hat |
| Fachliche Beratung, welche Leistung abrechenbar ist | das entscheidet ebenfalls der Vertrag, nicht ein Programm |

## Entscheidung

**Das Projekt wird ein Einzelplatzprogramm, das aus vorhandenen Leistungsdaten
eine prüffähige Abrechnungsdatei erzeugt — und das jede Datei prüft, bevor sie
hinausgeht.** Der Aufbau folgt dem Verfahren, nicht einem einzelnen
Leistungsbereich; der erste bediente ist die Hebammenhilfe.

| # | Kernfähigkeit | Was dahintersteckt |
|---|---|---|
| 1 | **Stammdaten führen** | versicherte Personen mit ihren Versicherungsangaben, die Leistungserbringerin selbst, Leistungsfälle als Zusammenfassung — ein Kurs, eine Behandlungsserie |
| 2 | **Leistungen beschreiben** | was eine Einheit kostet, unter welcher Position sie abgerechnet wird — einmal festgelegt, mehrfach verwendet. Hier sitzt das Bereichsspezifische |
| 3 | **Abrechnung erzeugen** | aus Leistungsfall, Beschreibung und Anzahl der Einheiten entsteht die Nachricht im vorgeschriebenen Format |
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
| **Leistungsdaten** | liegen ohnehin vor — in Listen und Köpfen |
| **Das Programm** | erzeugt daraus die Abrechnungsdatei **und prüft sie, bevor sie das Haus verlässt** |
| **Die Krankenkasse** | zahlt oder beanstandet |

### Die Begriffe und ihre Beziehungen

Bevor entschieden werden kann, wie etwas gebaut wird, muss feststehen, **wovon
überhaupt die Rede ist** — noch nicht als Klassen eines Programms, sondern als
Landkarte des Gegenstands.

![Fachliches Übersichtsmodell](../GKVTransmitter_Vision_Fachmodell.png)

Die Begriffe sind bewusst so gewählt, dass sie den ersten Anwendungsfall
überdauern: **Leistungsfall** statt Kurs, **versicherte Person** statt
Teilnehmerin, **Leistungsnachweis** statt Anwesenheit. Drei Beobachtungen aus
dem Bild prägen alles Weitere:

| Beobachtung | Was daraus folgt |
|---|---|
| **Die versicherte Person ist nicht die Zahlende** — zwischen Leistung und Geld steht die Kasse | An ihr hängt ein zweiter Satz Angaben: Versichertennummer, Status, Kasse. Mit der Leistung hat er nichts zu tun, ohne ihn geht trotzdem nichts |
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

Alles Bereichsspezifische sammelt sich in einer einzigen Entität, der
**Leistungsbeschreibung**. Das ist keine Schönheit, sondern der Preis dafür, dass
ein weiterer Leistungsbereich später ein Eintrag bleibt und kein Umbau wird.

### Woran sich Erfolg messen lässt

In dieser Reihenfolge:

| Maßstab | Warum dieser |
|---|---|
| Eine erzeugte Datei wird ohne Beanstandung angenommen | der eigentliche Zweck |
| Ein Fehler in den Daten fällt vor dem Versand auf | der eigentliche Wert |
| Der Weg von den Leistungsdaten zur Datei dauert Minuten | sonst benutzt es niemand |
| Die Anwenderin versteht, was das Programm ihr sagt | sonst ruft sie doch wieder an |
| Ein weiterer Leistungsbereich kommt ohne Änderung am Aufbau hinzu | die Probe darauf, ob die Verallgemeinerung getragen hat |

## Offen geblieben

- **Wie das Format genau aussieht** und welche Vorgaben verbindlich sind — das
  ist Gegenstand von Teil 02.
- **Welche Leistungsbereiche tatsächlich bedient werden.** Die Vision hält den
  Rahmen offen; welche Werte darin gültig sind, ergibt sich aus Verträgen, die
  nicht vorliegen. Teil 03 grenzt das ein.
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

