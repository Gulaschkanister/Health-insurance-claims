---
title: "Teil 09 — Persistenz"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Ab hier wird gebaut. Der erste Baustein ist der Zugang zur Datenbank — nicht,
weil er der interessanteste ist, sondern weil ohne ihn nichts anderes läuft.

Die Entscheidungen dafür stehen: eine Datei-Datenbank (Teil 07), hinter einer
Schnittstelle (Teil 05).

## Ziel dieses Teils

Den Zugang so bauen, dass die Fachlichkeit ihn benutzen kann, **ohne ihn zu
kennen** — und dass er sich im Prüflauf ersetzen lässt.

## Überlegungen

### Was in die Schnittstelle gehört

Die Versuchung ist, sie allgemein zu halten: „speichere irgendein Objekt", „lade
irgendeine Liste". Das spart Methoden.

Dagegen spricht, dass eine allgemeine Schnittstelle **nichts aussagt**. Wer sie
liest, weiß nicht, was das Programm mit der Datenbank tut. Eine Schnittstelle mit
sprechenden Methoden — Patienten laden, Gruppe speichern, nächste Nummer ziehen —
ist zugleich eine Liste dessen, was überhaupt gebraucht wird.

Sie wird dadurch länger. Das ist in Ordnung: **Sie wächst nur, wenn ein neuer
Bedarf entsteht, und dann soll man das sehen.**

### Wiederholung, die niemand sehen will

Jeder Zugriff hat denselben Rahmen: Sitzung öffnen, Transaktion beginnen, Arbeit
tun, festschreiben — und im Fehlerfall zurückrollen und die Sitzung schließen.

Das sechsmal hinzuschreiben ist nicht nur mühsam, sondern gefährlich: **Beim
sechsten Mal fehlt das Zurückrollen.** Und ein fehlendes Zurückrollen fällt nicht
auf, solange nichts schiefgeht.

Also bekommt der Rahmen eine eigene Stelle. Ein Aufruf übergibt nur noch, was
zwischen Beginn und Ende geschehen soll, und einen Namen für den Fall, dass es
schiefgeht:

| Statt | Jetzt |
|---|---|
| Sitzung öffnen, Transaktion, Arbeit, festschreiben, Fehlerbehandlung, schließen | „lies dies" oder „schreib das", mit einem Namen für die Meldung |

Der Name ist kein Schmuck. Wenn ein Zugriff scheitert, steht in der Meldung, was
versucht wurde — nicht nur, dass etwas mit der Datenbank war.

### Die Nummer, die nicht zweimal kommen darf

Die laufende Nummer der Lieferung wird gelesen und dann erhöht. Genau dieses
Muster — erst lesen, dann schreiben — bricht die Datenbank sofort ab, statt zu
warten (Teil 07).

Erwogen wurden drei Auswege:

| Ausweg | Warum nicht |
|---|---|
| Die Wartezeit erhöhen | Hilft bei genau diesem Muster nicht |
| Die Nummer im Arbeitsspeicher führen | Nach einem Neustart beginnt sie von vorn — der Fehler, den es zu vermeiden gilt |
| Im Programm sicherstellen, dass immer nur einer sie zieht | — |

Gewählt wurde der dritte. Die Vergabe ist die einzige Stelle im Programm, an der
das nötig ist, und sie ist als solche gekennzeichnet.

### Wie sich der Zugang im Prüflauf ersetzen lässt

Weil die Fachlichkeit nur die Schnittstelle kennt, genügt für einen Prüflauf eine
zweite Umsetzung, die alles im Arbeitsspeicher hält. Kein Dateipfad, keine
Aufräumarbeit, kein Zustand zwischen zwei Läufen.

Für die Prüfung des Zugangs selbst gilt aber das Gegenteil: **Dort wird gegen eine
echte Datei geprüft, nicht gegen einen Ersatz.** Ein Ersatz würde beweisen, dass
der Ersatz funktioniert. Ob die Datenbank den Datensatz wirklich annimmt, zeigt
nur die Datenbank.

## Entscheidung

**Eine Schnittstelle mit sprechenden Methoden, eine Umsetzung darauf, und ein
gemeinsamer Rahmen für alle Zugriffe.** Eigene Sitzungs- und Transaktionsblöcke
kommen nicht mehr vor.

Dazu drei Festlegungen:

| Festlegung | Grund |
|---|---|
| Der Ablageort ist von außen einstellbar | Prüfläufe dürfen nie in die echte Ablage schreiben |
| Ein fehlgeschlagener Zugriff wirft eine **eigene** Ausnahme | „Irgendwas mit der Datenbank" hilft niemandem; die Meldung trägt den Namen des Vorgangs |
| Der Modus, der das Schema neu anlegt, ist **nie** voreingestellt | Er verwirft alle Daten |

### Warum nicht der einfachere Weg

Die Fachlichkeit hätte die Datenbank direkt ansprechen können. Das spart die
Schnittstelle und eine ganze Klasse.

Dagegen spricht Q5: **Ohne die Schnittstelle wäre kein Prüflauf ohne Datei
möglich** — und damit würde jeder Fachprüflauf eine Datenbank anlegen, füllen und
aufräumen. Das ist langsam genug, dass man es lässt.

## Ergebnis

Ein Zugang, der von außen wie eine Liste von Fachaufgaben aussieht und innen aus
einem einzigen Muster besteht. Dazu Prüfläufe gegen eine echte Datei in einem
Wegwerfverzeichnis.

**Was dabei auffiel:** Der Einstellungsbereich des Programms brauchte zunächst
eine eigene Datei — eine Ablage neben der Ablage. Beim Nachfragen zeigte sich,
dass sie nichts konnte, was die Datenbank nicht auch kann. Sie wurde durch eine
Tabelle mit zwei Spalten ersetzt.

> **Ein zweiter Ablageort rechtfertigt sich nur, wenn er etwas kann, was der
> erste nicht kann.**

Verwendete Mittel:

| Mittel | Wozu |
|---|---|
| Schnittstelle und Anschluss | die Fachlichkeit von der Datenbank lösen |
| Gemeinsamer Rahmen für Zugriffe | Wiederholung und vergessene Fehlerbehandlung vermeiden |
| Übergabe im Konstruktor, ohne Rahmenwerk | Abhängigkeiten sichtbar halten |

## Offen geblieben

- **Ob die Nummernvergabe auch bei zwei Programmfenstern hält.** Sie ist gegen
  gleichzeitige Zugriffe im selben Programm geschützt, nicht gegen zwei
  Programmstarts.
- **Ob die Textspalte der Blaupause reicht** (aus Teil 07). Weiterhin ungeprüft.
