---
title: "Teil 06 — Fachliches Modell"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Teil 02 hat die Begriffe geordnet, Teil 05 den Rahmen gesetzt. Jetzt werden aus
den Begriffen Klassen — und das ist keine Übersetzung eins zu eins.

## Ziel dieses Teils

Das fachliche Modell als Klassen festlegen. Und, mindestens ebenso wichtig:
**benennen, wo es vom Begriffsmodell abweicht und warum.**

## Überlegungen

### Zwei Sorten Klassen

Beim ersten Entwurf zeigte sich eine Trennung, die sich durchhält: Manche Klassen
müssen einen Programmlauf überdauern, andere nicht.

| Sorte | Beispiele | Kennzeichen |
|---|---|---|
| **Gespeicherte Klassen** | Person, Gruppe, Blaupause, Zähler, Einstellung | tragen eine Kennung, werden in der Datenbank abgelegt |
| **Reine Fachobjekte** | Abrechnung, Nachricht, Segment, Prüfbericht | entstehen im Lauf, werden nie gespeichert |

Die Trennung ist keine Ordnungsliebe. Eine gespeicherte Klasse muss einen leeren
Konstruktor haben, veränderbare Felder und eine Kennung — Zugeständnisse an die
Speicherung, die eine reine Fachklasse nicht machen muss. **Wer beides in einer
Klasse mischt, bekommt die Zugeständnisse überall.**

### Wo eine Person aufhört, eine Person zu sein

Versicherte und Leistungserbringerin haben dieselben Angaben: Name, Anschrift,
Geburtsdatum, Kennzeichen. Drei Möglichkeiten:

| Möglichkeit | Folge |
|---|---|
| Eine Klasse mit einem Merkmal „ist Versicherte / ist Erbringerin" | Jede Abfrage muss das Merkmal mitprüfen. Vergisst es eine, mischt sich beides |
| Zwei völlig getrennte Klassen | Jede Änderung an den Feldern muss zweimal gemacht werden |
| Eine gemeinsame Oberklasse, zwei Unterklassen | Felder einmal beschrieben, Typen getrennt |

Gewählt wurde die dritte, mit einer Besonderheit: **die Oberklasse bekommt keine
eigene Tabelle.** Sie beschreibt nur die Felder; gespeichert wird in zwei
getrennten Tabellen.

Der Preis dafür wird in Teil 07 sichtbar, und er ist höher als hier gedacht.

### Der Kompromiss bei der Blaupause

Eine Leistungsbeschreibung trägt Abrechnungsposition, Tarifkennzeichen, Betrag,
Umsatzsteuersatz — und je nach Segment noch weitere Felder. Wie viele es sind,
hängt an den Vorgaben und ändert sich mit ihnen.

Zwei Wege:

| Weg | Dafür | Dagegen |
|---|---|---|
| Für jedes Feld eine eigene Spalte | durchsuchbar, prüfbar, sichtbar | Jede neue Vorgabe ändert das Schema |
| Alle ausgefüllten Felder gemeinsam in einem Textfeld | Eine neue Vorgabe ändert nichts an der Struktur | Nicht durchsuchbar; ein Tippfehler im Feldnamen fällt nirgends auf |

Gewählt wurde der zweite Weg — **bewusst und mit bekanntem Nachteil.** Der
Ausschlag gab Q7 aus Teil 03: Vorgaben sollen Beschreibungen sein, keine
Struktur.

Der Nachteil hat sich später gerächt: Die Liste der Feldnamen, die aus der
Blaupause gelesen werden, konnte Namen enthalten, die in keinem Formular
vorkamen. Niemand merkte es, weil ein Textfeld nichts verweigert. Erst eine
eigene Gegenprobe deckte es auf.

### Der Zähler als Klasse

Eine laufende Nummer klingt nach einer Variablen, nicht nach einer Klasse. Sie
wurde trotzdem eine — mit eigener Tabelle und eigener Zeile.

Der Grund steht schon im Datenmodell aus Teil 01: **Eine Nummer, die nach einem
Neustart von vorn zählt, erzeugt eine doppelte Datenaustauschreferenz.** Das ist
ein Zurückweisungsgrund, der sich nicht aus der Datei heraus reparieren lässt.
Also gehört sie dorthin, wo sie einen Neustart überlebt.

## Entscheidung

Das Modell zerfällt in drei Bereiche, sichtbar an den Paketen:

| Bereich | Inhalt | Gespeichert |
|---|---|---|
| **Entitäten** | Person mit Patient und Leistungserbringer, Gruppe, Blaupause, Zähler, Einstellung | ja |
| **Modell** | Abrechnung, Nachricht, Rechnung | nein |
| **Segmentmodell** | Segment, Segmentbeschreibung, Feld, Feldbeschreibung, Feldwert | nein — kommt aus Beschreibungen |

![Fachliches Modell](../GKVTransmitter_Domaene.png)

### Warum nicht der einfachere Weg

Der Segmentbereich sieht überbaut aus: fünf Klassen, um zu beschreiben, was ein
Feld ist. Man hätte die Felder auch direkt im Erzeugungscode benennen können.

Dagegen spricht Q7 und die Erfahrung aus Teil 02: **Die Vorgaben ändern sich in
den Werten, nicht in der Struktur.** Eine Feldbeschreibung, die aus einer Datei
kommt, lässt sich anpassen, ohne das Programm neu zu bauen. Fest verdrahtete
Felder nicht.

## Ergebnis

Das Klassenbild oben — und, wichtiger, der Vergleich mit dem Begriffsmodell aus
Teil 02. **Sie sehen nicht gleich aus, und die Unterschiede sind die eigentliche
Aussage dieses Teils:**

| Begriff aus Teil 02 | Was daraus wurde | Was dabei verloren ging |
|---|---|---|
| Versicherte Person | `Patient` | nichts |
| Leistungserbringerin | `ServiceProvider` | nichts |
| Leistungsfall (Kurs) | `PersonGroup` | **fast alles**: Zeitraum und Anzahl der Einheiten fehlen. Die Gruppe trägt nur einen Namen und ihre Mitglieder |
| Leistungsnachweis | — | **gar nicht vorhanden.** Die Anzahl der Einheiten sitzt an der Abrechnung, nicht am Nachweis |
| Leistungsbeschreibung | `Blueprint` | die einzelnen Felder — sie stecken gemeinsam in einem Textfeld |
| **Lieferung** | — | **keine Klasse.** Eine Lieferung ist heute nur ein Programmlauf. Sie hat keine Kennung, keinen Zustand, keine Spur |
| Prüfbericht, Befund | `ValidationReport`, `ValidationMessage` | nichts — hier stimmt das Modell |
| Rückmeldung | Antwortobjekt im Versandbereich | nichts |
| Krankenkasse, Annahmestelle | **nur eine Zahl** an der Person | beides ist kein Gegenstand des Modells, sondern ein Feld |

Zwei Zeilen davon sind mehr als Abweichungen — sie sind Mängel:

**Die Lieferung fehlt.** Im Begriffsmodell trug sie eine laufende Nummer, eine
Art und einen Bezug zu Abrechnungen und Befunden. Im Programm ist sie ein
Vorgang, der abläuft und nichts hinterlässt. Deshalb lässt sich heute nicht
beantworten, was in einer bestimmten Lieferung enthalten war — und genau das
braucht man, wenn eine Zurückweisung eintrifft.

**Der Leistungsnachweis fehlt.** Dass die Anzahl der Einheiten an der Abrechnung
hängt statt am Nachweis, heißt: Eine Teilnehmerin, die an sechs von zehn
Terminen war, lässt sich nicht als solche festhalten — nur als Abrechnung über
sechs.

Beides ist behebbar. Beides steht hier, weil eine Dokumentation, die nur das
Gelungene zeigt, wertlos ist.

## Offen geblieben

- **Wie sich das Modell in Tabellen niederschlägt** — insbesondere, was die
  Oberklasse ohne eigene Tabelle kostet. Teil 07.
- **Ob die Lieferung eine Klasse werden soll.** Die Antwort ist absehbar ja; wann,
  entscheidet Teil 23.
- **Wie die Blaupause durchsuchbar wird**, ohne das Schema an die Vorgaben zu
  binden. Offen.
