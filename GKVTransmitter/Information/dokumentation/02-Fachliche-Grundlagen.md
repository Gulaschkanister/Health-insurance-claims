---
title: "Teil 02 — Fachliche Grundlagen"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Teil 01 hat festgelegt, **was** entstehen soll: ein Programm, das eine prüffähige
Abrechnungsdatei erzeugt. Was dabei stillschweigend vorausgesetzt wurde: dass
jemand weiß, wie eine solche Datei aussieht.

Das war zu diesem Zeitpunkt nicht der Fall. Bekannt war das Ziel, nicht das
Verfahren.

## Ziel dieses Teils

Die Fachlichkeit so weit durchdringen, dass sich darüber reden lässt — welche
Beteiligten es gibt, wie eine Nachricht aufgebaut ist, welche Regeln zwingend
sind. **Kein Entwurf, keine Klassen; erst die Begriffe.**

## Überlegungen

### Woher die Wahrheit kommt

Die erste Frage war nicht fachlich, sondern eine der Quellen: Wonach richtet man
sich?

| Quelle | Taugt wofür | Taugt nicht wofür |
|---|---|---|
| Verbindliche Anlagen und Anhänge der Vereinbarung | die Regel selbst — sie sind die Vorgabe | einen schnellen Einstieg; sie sind umfangreich und setzen viel voraus |
| Eine echte, angenommene Beispieldatei | zu sehen, wie es aussehen muss | Verallgemeinerung — sie zeigt **einen** Fall, nicht die Regel |
| Erklärungen im Netz | Orientierung | alles, worauf es ankommt |

Daraus wurde eine Arbeitsregel, die das ganze Projekt trägt:

> **Ein Wert, den man aus einer Beispieldatei abschreibt, ist so lange eine
> Vermutung, bis er in einer Vorgabe steht.**

Diese Regel ist nicht theoretisch. Sie wurde erst aufgestellt, nachdem genau das
schiefgegangen war — siehe *Was dabei schiefging*.

### Wer beteiligt ist

Jede Einrichtung im Abrechnungsverkehr trägt ein **Institutionskennzeichen**:
neun Stellen, die letzte davon eine Prüfziffer. Es ist keine frei gewählte
Nummer, sondern wird vergeben.

In einer einzigen Zeile der Nachricht stehen **vier** solcher Kennzeichen
nebeneinander:

| Rolle | Wer das ist |
|---|---|
| Leistungserbringer | wer die Leistung erbracht hat |
| Kostenträger | wer bezahlt |
| Kasse von Karte oder Verordnung | die Kasse laut Versichertenkarte |
| Rechnungssteller | wer abrechnet — das kann eine Abrechnungsstelle sein |

Leistungserbringer und Rechnungssteller sind oft, aber nicht zwingend dieselbe
Einrichtung. Genau dieser Unterschied ist der Grund, warum es überhaupt
Abrechnungsstellen gibt — und warum das eigene Programm beide Rollen mit
demselben Kennzeichen besetzt.

### Wie eine Nachricht aufgebaut ist

Die Datei ist reiner Text, aber kein Fließtext. Sie besteht aus **Segmenten**:
Jedes beginnt mit einem dreistelligen Bezeichner, endet mit einem festen Zeichen,
und dazwischen stehen Felder in fester Reihenfolge.

| Gruppe | Was darin steht |
|---|---|
| **Rahmen** | Anfang und Ende der Übertragung, Absender, Empfänger, Zeitpunkt, ob es sich um einen Test oder den Echtbetrieb handelt |
| **Rechnung** | Verarbeitungskennzeichen, Rechnungsnummer, Rechnungsdatum, Rechnungsart |
| **Versicherte Person** | Belegnummer, Versichertennummer, Versichertenstatus — oder ersatzweise die vollständige Anschrift |
| **Leistung** | Abrechnungscode, Tarifkennzeichen, Positionsnummer, Menge, Einzelbetrag, Leistungsdatum |
| **Summen** | je Fall eine Summe, darüber eine Gesamtsumme, dazu der Umsatzsteuersatz |
| **Zähler** | wie viele Segmente die Nachricht hat und wie viele Nachrichten die Übertragung |

Die Reihenfolge ist nicht Geschmackssache. Wer ein Feld auslässt, statt es leer
zu lassen, **verschiebt alle folgenden** — und dann steht der Betrag im Feld für
das Datum.

### Zwei Regeln, die keine Empfehlungen sind

**Entweder Versichertennummer und Status — oder die vollständige Anschrift.**
Fehlt beides, kann die Kasse die Leistung keinem Versicherungsverhältnis
zuordnen. Die Leistung wurde erbracht, die Rechnung wird trotzdem
zurückgewiesen.

**Die Summen müssen zueinander passen.** Die Summe je Fall ergibt sich aus Menge
mal Einzelbetrag; die Gesamtsumme aus allen Fallsummen. Eine Abweichung ist kein
Rundungsproblem, sondern ein Zurückweisungsgrund.

### Was dabei schiefging

Beim Übertragen der Beispieldatei in das eigene Verständnis wurden drei Werte
übernommen, die dort plausibel aussahen:

| Wert | Was daran falsch war |
|---|---|
| Ein Buchstabe im Nachrichtenkopf | Er kennzeichnet den **Leistungsbereich**. In der Beispieldatei stand der für einen anderen Bereich. Für Hebammenhilfe ist es ein anderer |
| Der Name der Datei | Frei erfunden nach dem Muster der Beispieldatei. Tatsächlich ist er **vorgeschrieben** und aus dem eigenen Kennzeichen und dem Abrechnungsmonat zu bilden |
| Die Länge der Positionsnummer | Aus der Beispieldatei übernommen; für den eigenen Bereich gilt eine andere |

**Alle drei Dateien übersetzten fehlerfrei und hätten fehlerfrei geprüft.** Der
Fehler war nicht sichtbar, weil nichts danach gesucht hat — die Prüfung kannte
die Regel ja auch nicht.

Daraus folgt die zweite Arbeitsregel des Projekts:

> **Ein erfundener Wert, der plausibel aussieht, ist schlimmer als einer, den die
> Prüfung abfängt.**

### Eine Falle, die dreimal zuschlägt

Die Felder einer Zeile lassen sich auf drei Arten zählen, und alle drei kommen im
Projekt vor:

| Zählweise | Der Bezeichner zählt mit? | Beginnt bei |
|---|---|---|
| in der fachlichen Prüfliste | ja | 1 |
| in den Segmentbeschreibungen | nein | 1 |
| im Quelltext beim Zugriff auf ein Feld | nein | 0 |

Dieselbe Angabe heißt damit je nach Zusammenhang „Feld 5", „Position 4" oder
„Element 3". Das ist eine verlässliche Fehlerquelle. Wer eine Position nennt,
muss die Zählweise dazusagen; wer eine übernimmt, muss sie gegen ein Beispiel
prüfen.

## Entscheidung

**Die verbindlichen Vorgaben sind die Quelle, die Beispieldatei ist nur die
Gegenprobe.** Wo eine Frage über das Belegte hinausgeht, wird nachgeschlagen und
nicht geraten.

Daraus folgen zwei Festlegungen für den weiteren Aufbau:

| Festlegung | Warum |
|---|---|
| Segmente und ihre Felder werden **beschrieben, nicht einprogrammiert** | Ändert sich eine Vorgabe, soll eine Beschreibung angepasst werden und kein Quelltext |
| Eine angenommene Beispieldatei wird als **Messlatte** aufbewahrt | Sie muss unbeanstandet durchlaufen. Tut sie es nicht mehr, hat eine Änderung etwas kaputtgemacht |

### Warum nicht der einfachere Weg

Man hätte die Nachricht als Zeichenkette zusammensetzen können, ohne
Beschreibung der Felder. Das ist schneller und für einen einzigen Leistungsbereich
ausreichend.

Dagegen spricht die Erfahrung aus diesem Teil: **die Vorgaben ändern sich, und
sie ändern sich in den Werten, nicht in der Struktur.** Eine Beschreibung, die
sich anpassen lässt, überlebt eine neue Fassung. Fest verdrahtete Zeichenketten
nicht.

## Ergebnis

Ein Begriffsmodell des Gegenstands — vollständig, aber noch ohne Bezug zu einer
Umsetzung:

![Fachliches Übersichtsmodell](../GKVTransmitter_Vision_Fachmodell.png)

Verglichen mit dem Zielbild aus Teil 01 sind drei Begriffe hinzugekommen, die
dort noch nicht sichtbar waren:

| Begriff | Warum er nötig wurde |
|---|---|
| **Datenannahmestelle** | Empfänger der Lieferung ist nicht die Kasse |
| **Prüfbericht** mit **Befunden** | Die Prüfung liefert nicht ja oder nein, sondern eine Liste mit Gewichtung |
| **Rückmeldung** mit fünf Einstufungen | Eine Antwort kann auch *nicht einzuordnen* sein — und das ist der wichtigste Fall |

Dazu ein Satz Arbeitsregeln, die in allen folgenden Teilen gelten:

1. Was nicht belegt ist, ist eine Vermutung — und wird als solche gekennzeichnet.
2. Ein erfundener Wert, der plausibel aussieht, ist schlimmer als einer, den die
   Prüfung abfängt.
3. Wer eine Feldposition nennt, sagt die Zählweise dazu.

## Offen geblieben

- **Welche Werte für Hebammenhilfe konkret gelten** — Abrechnungscode,
  Tarifkennzeichen, Positionsnummern. Das steht in Verträgen, die nicht vorliegen;
  Teil 03 macht daraus eine Anforderung statt einer Annahme.
- **Wie die Datei zur Kasse gelangt.** Der Aufbau ist geklärt, der Weg nicht.
  Teil 04.
- **Wie man die Vorgaben nachhält**, wenn sie sich ändern. Das wird in Teil 22
  zur Betriebsfrage.
