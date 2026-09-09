---
title: "Teil 03 — Anforderungen"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Nach Teil 01 steht das Ziel, nach Teil 02 der Gegenstand. Was fehlt, ist die
Übersetzung dazwischen: **eine Liste, gegen die man später prüfen kann, ob etwas
fertig ist.**

Ohne sie bleibt „das Programm soll prüfen" ein Satz, dem jede Umsetzung genügt —
auch eine, die nur nachsieht, ob die Datei nicht leer ist.

## Ziel dieses Teils

Aus der Vision eine Anforderungsliste machen. Jede Anforderung so formuliert,
dass sich **feststellen lässt, ob sie erfüllt ist** — und nicht nur behaupten.

## Überlegungen

### Was eine brauchbare Anforderung ausmacht

Drei Formulierungen desselben Gedankens, in aufsteigender Brauchbarkeit:

| Formulierung | Was daran fehlt |
|---|---|
| „Das Programm soll benutzerfreundlich sein" | Nichts daran ist prüfbar. Jede Oberfläche erfüllt das oder keine |
| „Fehlermeldungen sollen verständlich sein" | Besser, aber wer entscheidet das? |
| „Eine Meldung nennt die betroffene Stelle und was zu tun ist" | Prüfbar: man liest eine Meldung und sieht nach, ob beides darin steht |

Die dritte Form ist die, die im Folgenden verwendet wird.

### Was sich nicht als Anforderung schreiben lässt

Beim Durchgehen der Vision blieb ein Rest übrig, der sich sträubt:

- **„Die Positionsnummer muss gültig sein."** Das Programm kann Form und Länge
  prüfen. Ob die Nummer für den Vertrag zulässig ist, weiß nur der Vertrag.
- **„Die Kasse nimmt die Lieferung an."** Das hängt nicht am Programm allein.

Solche Punkte werden nicht weichgespült, sondern **als Grenze notiert**. Sie
gehören nach Teil 04, nicht in die Anforderungsliste.

## Entscheidung

Die Anforderungen werden in drei Gruppen geführt, mit sprechenden Kennungen.

### Funktionale Anforderungen

| Kennung | Anforderung | Erfüllt, wenn |
|---|---|---|
| **F1** | Stammdaten erfassen, ändern, löschen — versicherte Personen, Leistungserbringerin, Gruppen | Ein Datensatz überlebt einen Programmneustart |
| **F2** | Leistungen beschreiben und wiederverwenden | Dieselbe Beschreibung dient mehreren Abrechnungen, ohne erneut eingegeben zu werden |
| **F3** | Aus Gruppe, Beschreibung und Anzahl eine Nachricht erzeugen | Die erzeugte Datei entspricht im Aufbau der Vorgabe |
| **F4** | Jede Nachricht vor dem Versand prüfen | Ein eingebauter Fehler wird gemeldet, ein fehlerfreier Fall nicht |
| **F5** | Ein Fehler hält den **gesamten** Lauf auf | Bei drei Abrechnungen mit einem Fehler verlässt **keine** das Programm |
| **F6** | Die Nachricht an den zuständigen Empfänger zustellen | Die Datei liegt danach am richtigen Ziel |
| **F7** | Eine Rückmeldung einordnen und die nötige Reaktion nennen | Vier Antwortarten werden unterschieden, eine fünfte gilt als *nicht einzuordnen* |
| **F8** | Fortlaufende Nummern vergeben, die sich nie wiederholen | Nach einem Neustart wird nicht erneut dieselbe Nummer vergeben |
| **F9** | Betriebsdaten hinterlegen — Kennzeichen, Ablageort, Betriebsart | Eine Änderung wirkt beim nächsten Lauf, ohne dass etwas neu gebaut wird |

### Qualitätsanforderungen

| Kennung | Anforderung | Erfüllt, wenn |
|---|---|---|
| **Q1** | Keine zusätzliche Installation auf dem Zielrechner | Das Programm läuft auf einem Rechner ohne vorbereitete Umgebung |
| **Q2** | Daten bleiben auf dem Rechner der Anwenderin | Kein Bestandteil sendet Daten fort, außer der Lieferung selbst |
| **Q3** | Eine Meldung nennt Stelle und Handlung | Beides ist im Meldungstext nachweisbar |
| **Q4** | Ein Abrechnungslauf dauert Minuten, nicht Stunden | Von der Auswahl bis zur fertigen Datei ohne Wartezeit, die auffällt |
| **Q5** | Die Fachlichkeit ist ohne Oberfläche prüfbar | Ein erheblicher Teil der Prüfungen läuft ohne geöffnetes Fenster |
| **Q6** | Ein weiterer Leistungsbereich kommt ohne Umbau hinzu | Nur Werte und Beschreibungen ändern sich, keine Abläufe |
| **Q7** | Vorgaben liegen als Beschreibung vor, nicht als Quelltext | Eine geänderte Vorgabe ist eine geänderte Beschreibung |

### Abgrenzungen

Was ausdrücklich **nicht** gefordert wird — jede Zeile eine bewusste Grenze:

| Nicht gefordert | Warum |
|---|---|
| Kurs- und Terminverwaltung | ein eigenes Feld mit eigenen Werkzeugen |
| Buchhaltung und Steuer | eigene Vorschriften, eigene Verantwortung |
| Abrechnung mit Selbstzahlern | ein anderes Verfahren |
| Gleichzeitiger Betrieb durch mehrere Personen | die gewählte Datenhaltung lässt genau einen Schreiber zu |
| Ein mitgelieferter Katalog zulässiger Positionsnummern | ergibt sich aus dem Vertrag, nicht aus dem Programm |
| Auskunft, ob eine Leistung abrechenbar ist | dasselbe |

### Was zuerst gebaut wird

Nicht alles gleichzeitig. Die Reihenfolge folgt der Abhängigkeit, nicht dem
Interesse:

| Rang | Was | Warum in dieser Reihenfolge |
|---|---|---|
| 1 | F1, F2 — Stammdaten und Beschreibungen | Ohne Daten gibt es nichts zu erzeugen |
| 2 | F3 — Erzeugung | Der Kern; alles Weitere hängt daran |
| 3 | F4, F5 — Prüfung und Tor | Der eigentliche Wert des Vorhabens |
| 4 | F6, F7 — Zustellung und Rückmeldung | Braucht die drei darüber |
| 5 | F8, F9 — Zähler und Betriebsdaten | Fällt erst im Betrieb auf, gehört aber früh entschieden |

**F8 wurde bewusst nicht nach hinten geschoben**, obwohl es wie eine Kleinigkeit
aussieht. Eine doppelt vergebene Nummer lässt sich nachträglich nicht mehr aus
der Datei heraus reparieren.

## Ergebnis

Neun funktionale und sieben Qualitätsanforderungen, jede mit einer Bedingung,
unter der sie als erfüllt gilt. Dazu sechs ausdrückliche Abgrenzungen.

Die Liste ist der Maßstab für alle folgenden Teile. Wo ein Teil eine Anforderung
erfüllt, wird ihre Kennung genannt — so lässt sich am Ende nachhalten, was offen
blieb.

**Auffällig ist Q5.** Dass die Fachlichkeit ohne Oberfläche prüfbar sein soll,
klingt nach einer technischen Vorliebe. Es ist aber eine fachliche Forderung: Was
sich nur mit geöffnetem Fenster prüfen lässt, wird selten geprüft — und die
Prüfung ist der Sinn dieses Programms. Diese Anforderung entscheidet in Teil 05
über den ganzen Aufbau.

## Offen geblieben

- **Ob F6 überhaupt erreichbar ist.** Zustellen setzt einen zugelassenen Weg
  voraus. Teil 04.
- **Wie Q1 erreicht wird**, ohne der Anwenderin eine Laufzeitumgebung zuzumuten.
  Teil 08 und Teil 22.
- **Wie sich Q6 nachweisen lässt**, solange nur ein Leistungsbereich bedient
  wird. Ein Nachweis, der nie geführt wird, ist keiner — die Frage kommt in
  Teil 23 zurück.
