# Ein Monat von Hand

Stand: 5. September 2026.

Dieser Text geht den Weg, den jemand geht, der einen Kurs abrechnet — Schritt
für Schritt, mit dem, was dabei auf dem Bildschirm steht. Er ist Punkt 3 aus
Abschnitt J der Übergabe und die Vorbereitung der Retrospektive.

Der Durchlauf ist **nicht beschrieben, sondern gefahren.** Die Befehlsfolge
steht in `gkv-ui/src/test/resources/ablaeufe/durchlauf-mit-bildern.txt` und
lässt sich wiederholen:

```bash
mvn -q -pl gkv-ui exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=de.gkvtransmitter.presentation.Bedienung \
    -Dexec.args="gkv-ui/src/test/resources/ablaeufe/durchlauf-mit-bildern.txt target/durchlauf"
```

Danach liegen elf PNG unter `target/durchlauf`. Wer nur wissen will, ob der
Ablauf hält, lässt `AblaufTest` laufen — dieselbe Mechanik, ohne Bilder, gegen
eine leere Datenbank.

## Was dabei zu sehen war

### 1. Der Start

Die Anwendung öffnet den ersten Bereich, „Abrechnung". Blaupause und Gruppe
sind leer, darunter steht „Bitte zuerst oben eine Gruppe wählen." Die
Statuszeile sagt „2 Vorlagen geladen"; welche, sagt das Info-Zeichen daneben.

**Gut so:** der Bereich, um den es geht, steht zuerst.

### 2. Eine Teilnehmerin anlegen

„Teilnehmer" → „+ Neuer Teilnehmer". Neun Felder in zwei Spalten, jedes mit
einem Info-Zeichen; nur unter den beiden IK steht die Erklärung offen.

**Absichtlich so:** was man sich nicht ausdenken kann, wird erklärt, ohne dass
man danach fragen muss. Alles andere — Vorname, Straße, Ort — steht hinter dem
Zeichen. Ob das die richtige Grenze ist, ist die Frage an die Retrospektive.

### 3. Ein ausgedachtes IK

Mit `123456789` im Feld „IK der Krankenkasse" und dann auf „Speichern":

> **Nicht gespeichert — so ließe sich damit nicht abrechnen:**
> · Ein IK wird von der Datenannahmestelle vergeben und lässt sich nicht frei
> wählen: die letzte Ziffer muss zu den übrigen passen. Zu 12345678 gehört die
> Prüfziffer 0, das gültige IK lautete also 123456780.

Das war Simons erster Punkt. Die alte Meldung lautete „Die Prüfziffer stimmt
nicht. Ein IK hat neun Ziffern." — und ließ jemanden vor einer Ablehnung ohne
Ausweg stehen. Die Zahl ist tatsächlich ungültig; **die Meldung hatte unrecht,
nicht das Programm.**

Nichts wird gespeichert, das Formular bleibt gefüllt stehen, und die
Beanstandung steht zusätzlich unter dem Feld.

### 4. Berichtigen und speichern

`102137985` eingesetzt, „Speichern":

> **Anna Berger als Teilnehmerin angelegt.**

Der Name steht jetzt darin. Vorher hieß es „Teilnehmer erfolgreich erstellt!" —
wer zehn Frauen hintereinander einträgt, hat nach der fünften keine Gewissheit
mehr über die vierte.

### 5. Eine Gruppe

„Gruppen" → „+ Neue Gruppe". Name eintragen, Teilnehmerinnen und Dienstleister
anhaken. Über jeder Liste ein Suchfeld und „0 von 7 ausgewählt".

Zwei Frauen können gleich heißen; die Zeile nennt deshalb die laufende Nummer
(`Anna Berger (ID: 1)` und `Anna Berger (ID: 7)`). Das ist unschön, aber
notwendig — ohne sie hakte man die falsche an.

### 6. Eine Blaupause

Ein Klick auf den Kursnamen in der Seitenleiste öffnet das Blaupausenformular.
Sechs Felder:

| Feld | woher der Wert kommt |
|---|---|
| Umsatzsteuersatz | Auswahl 19 / 7 / 0, beschreibbar |
| Abrechnungscode | schon ausgefüllt: `61` |
| Tarifkennzeichen | **aus dem Vertrag** |
| Abrechnungspositionsnummer | **aus Anlage 3, Abschnitt 8.2** |
| Durchschnittlicher Einzelbetrag | der Preis je Termin |
| Zuzahlung pro Position | meist 0,00 |

Die vier unteren tragen ihre Erklärung offen — dort lässt sich der richtige
Wert nicht erraten und ein falscher kostet die Lieferung.

**Zwei Antworten auf Simons Fragen stecken hier drin.** Der Abrechnungscode
hat kein Aufklappmenü mehr: es gab genau einen Eintrag, und ein Menü mit einer
Zeile ist Bedienlast ohne Nutzen. Der Umsatzsteuersatz hat jetzt Vorschläge —
die Mechanik dafür gab es längst, sie war nur am falschen Feld.

**Offen bleibt**, ob der Abrechnungscode überhaupt ins Formular gehört. Wäre er
je Vorlage fest, könnte er ganz verschwinden. Das steht in Anlage 3.

### 7. Abrechnen

„Abrechnung": Blaupause wählen, Gruppe wählen. Die Teilnehmerinnen erscheinen
mit Kästchen und je einem Zähler für die Termine. „Alle auswählen", „Termine
für alle: 8", „Setzen".

> 1 von 1 ausgewählt · 8 Termine insgesamt

**Genau hier steckte ein teurer Fehler.** Bis zum 05.09.2026 stand nach diesem
Schritt in jeder Zeile eine **1**. Ein beschreibbarer Zähler übernimmt
getippten Text nur bei der Eingabetaste; wer stattdessen auf „Setzen" klickte,
rechnete mit dem alten Wert ab — auf dem Bildschirm stand die ganze Zeit die 8,
und die Rechnung an die Kasse lautete auf ein Achtel. Gefunden hat das dieses
Werkzeug, kein Test: alle Tests setzten den Wert am Editor vorbei.

„Abrechnung starten":

> **Eine Abrechnungsdatei erzeugt:**
> IK der Krankenkasse: 102137985
> …\dta_output\outbox\techniker-krankenkasse\patient_1_20260905173636.dta

Die Datei ist da, sie ist geprüft, und sie liegt im Ordner der richtigen Kasse.

## Was der Durchlauf sonst noch gefunden hat

**Die Fehlermeldung verdeckte die Felder, die sie zu berichtigen verlangte.**
Die Meldungsecke lag oben rechts — bei einem zweispaltigen Formular genau über
„Nachname" und „Land". Sie steht jetzt unten rechts, der leersten Ecke jeder
Maske.

**Eine Beanstandung stand noch drei Bereiche später da.** „Nicht gespeichert",
während seither dreimal erfolgreich gespeichert worden war. Ein Fehler bleibt
weiterhin stehen, bis jemand ihn zur Kenntnis nimmt — aber woandershin zu gehen
*ist* Kenntnisnahme. Beim Wechsel des Bereichs wird die Ecke geräumt; beim
Speichern innerhalb desselben Bereichs nicht, sonst verschwände die
Erfolgsmeldung, ehe jemand sie liest.

**„1 DTA-Batches erzeugt:"** — ein englisches Wort in einer deutschen
Oberfläche, und eine Eins vor einer Mehrzahl. Das ist die Schlussmeldung des
ganzen Ablaufs.

## Was für die Retrospektive offen bleibt

1. **Die Info-Zeichen.** Die Grenze zwischen „steht offen da" und „liegt hinter
   dem Zeichen" ist gesetzt, nicht bewiesen. Am Bild lässt sich jetzt sehen,
   was sie kostet und was sie bringt.
2. **Die Bildlaufleisten** sind gestaltet und in `teilnehmer.png` zu sehen —
   einmal am laufenden Fenster nachsehen schadet trotzdem nicht.
3. **Der Name des Programms** (Abschnitt I der Übergabe).
4. **Tarifkennzeichen, Positionsnummer, echter Versand** — dafür fehlen
   Unterlagen, nicht Programmierung. Das gehört gesagt, nicht kaschiert.
