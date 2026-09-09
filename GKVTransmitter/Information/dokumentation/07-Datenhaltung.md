---
title: "Teil 07 — Datenhaltung"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Das Modell steht. Jetzt muss es irgendwo liegen — auf dem Rechner der Anwenderin,
ohne dass sie etwas einrichtet (Q1), und ohne dass Daten das Haus verlassen (Q2).

## Ziel dieses Teils

Festlegen, **wie und wo gespeichert wird** — und nachsehen, was aus dem Modell in
den Tabellen tatsächlich angekommen ist.

## Überlegungen

### Was als Ablage in Frage kommt

| Möglichkeit | Warum sie erwogen wurde | Warum nicht |
|---|---|---|
| Dateien im Dateisystem, eine je Datensatz | keinerlei Voraussetzung | Suchen, Zählen, Verknüpfen müsste alles von Hand entstehen |
| Ein Datenbankserver | mächtig, mehrbenutzerfähig | Muss installiert und betrieben werden — verstößt gegen Q1 |
| Eine Datenbank **in einer Datei**, ohne Server | volle Abfragesprache, nichts einzurichten | nur ein Schreiber gleichzeitig |

Gewählt wurde die dritte. Die Einschränkung — ein Schreiber — ist keine, weil das
Programm laut Abgrenzung aus Teil 03 ohnehin von einer Person bedient wird. **Aus
einer technischen Grenze wurde eine dokumentierte Entscheidung**, und das ist der
Unterschied zwischen einer Beschränkung und einer Überraschung.

### Der Zugriff geht durch eine Tür

Nach Teil 05 darf die Fachlichkeit die Datenbank nicht kennen. Sie beschreibt
stattdessen, was sie braucht — Patienten laden, Gruppe speichern, nächste Nummer
ziehen —, und ein Anschluss erfüllt das.

Der Nutzen zeigt sich im Prüflauf: Dort tritt an die Stelle der Datei ein
Speicher im Arbeitsspeicher, und die Fachlichkeit merkt nichts.

Ebenso wichtig ist, was **nicht** durch die Tür geht: Es gibt keine Stelle, an
der eine Maske selbst eine Abfrage formuliert. Fachlogik in einer Maske wäre
sonst nur eine Zeile entfernt.

### Zwei Eigenheiten, die man kennen muss

**Nur ein Schreiber.** Zwei gleichzeitige Schreibzugriffe scheitern. Dagegen hilft
eine Wartezeit — die Verbindung wartet, statt sofort abzubrechen.

**Die Wartezeit hilft nicht immer.** Wenn eine Transaktion erst liest und dann
schreibt, bricht die Datenbank sofort ab, statt zu warten — sonst könnten sich
zwei solche Vorgänge gegenseitig blockieren. Genau dieses Muster hat die Vergabe
der laufenden Nummer: erst nachsehen, dann erhöhen.

Das ließ sich nicht mit einer Einstellung lösen. **Die Vergabe der Nummer musste
im Programm selbst so geführt werden, dass immer nur einer sie zieht.**

### Wie das Schema entsteht

Es wird nicht von Hand geschrieben, sondern aus den Klassen abgeleitet. Neue
Felder wachsen mit.

Diese Bequemlichkeit hat eine scharfe Kante. Die Ableitung kennt einen Modus, der
das Schema bei jedem Start **neu anlegt** — und damit alle Daten verwirft. Dieser
Modus stand einmal fest in der Konfiguration. Das Programm startete, war leer,
startete wieder, war leer.

Daraus wurde eine Regel: **Was Daten löschen kann, wird nicht voreingestellt.**

## Entscheidung

**Eine serverlose Datenbank in einer Datei, im Benutzerprofil abgelegt, hinter
einer Schnittstelle, mit aus den Klassen abgeleitetem Schema.**

Der Ablageort wird ausdrücklich am Benutzerprofil festgemacht und nicht am
Arbeitsverzeichnis. Ein relativer Pfad läge sonst je nach Startart woanders — bei
einer ausgelieferten Anwendung womöglich an einem Ort, an dem sich gar nicht
schreiben lässt.

![Datenbankstruktur](../GKVTransmitter_Datenbank_ER.png)

### Warum nicht der einfachere Weg

Man hätte das Schema von Hand schreiben können, mit Fremdschlüsseln und
Bedingungen. Das wäre strenger.

Dagegen sprach der Aufwand bei jeder Modelländerung — und die Erwartung, dass die
Ableitung schon das Richtige tut. Diese Erwartung war, wie sich zeigte, zu
freundlich.

## Ergebnis

Ein Schema mit acht Tabellen. **Und drei Feststellungen, die erst beim Nachsehen
im tatsächlichen Schema auftauchten — nicht beim Betrachten der Klassen:**

| Feststellung | Was sie bedeutet |
|---|---|
| **Es gibt keine Fremdschlüssel** | Nichts hindert die Datenbank daran, eine Zuordnung auf eine gelöschte Person zu behalten. Die Beziehungen bestehen nur, solange das Programm sie einhält |
| **Die Oberklasse ohne eigene Tabelle dupliziert ihre Felder** | Neun Spalten stehen zweimal da, in zwei Tabellen. Eine Änderung an der Oberklasse ändert beide — das ist gewollt. Eine Auswertung über *alle* Personen ist damit aber nicht möglich |
| **Das Feld für die Blaupausen-Werte ist eine Textspalte mit Längenbegrenzung** | Es trägt eine ganze Feldsammlung. Ob die Begrenzung im Betrieb reicht, hat niemand geprüft |

Die dritte Zeile ist die unangenehmste, weil sie **still** ist: Wird der Inhalt zu
lang, ist das kein Fehler im Programm, sondern eine abgeschnittene oder
abgelehnte Speicherung.

> **Ein Schema, das man nicht angesehen hat, ist eine Vermutung — auch wenn man
> die Klassen kennt, aus denen es entstanden ist.**

Genau darum wurde das Diagramm oben nicht aus den Klassen gezeichnet, sondern aus
der Datenbank ausgelesen.

## Offen geblieben

- **Ob Fremdschlüssel nachgerüstet werden sollen.** Sie würden Fehler früher
  sichtbar machen, aber bestehende Daten müssten sie erfüllen.
- **Ob die Textspalte für die Blaupause groß genug ist.** Ungeprüft.
- **Wie eine Auswertung über alle Personen aussähe**, solange sie in zwei
  Tabellen liegen. Bisher wurde sie nicht gebraucht — das ist ein Argument auf
  Zeit.
