# Vorlage für eine Spezifikation

Eine Spezifikation je Arbeitspaket, abgelegt als
`Information/spezifikationen/<kurzname>.md`. Sie wird **vor** der Umsetzung
geschrieben und bleibt danach stehen — sie ist die Begründung, die ein Test
allein nicht liefert.

Warum es sie gibt, steht in `Umsetzungsplan.md`, Teil 4. Die Kurzfassung: in
diesem Projekt fängt weder der Übersetzer noch ein Test eine erfundene
Fachregel. Drei Werte waren aus einer Beispieldatei übernommen und nie gegen
die Vorgabe gehalten — sie sahen plausibel aus und wären erst Wochen später
von der Kasse zurückgekommen.

Alles unterhalb dieser Zeile kopieren und ausfüllen.

---

# <Kurzname>

## Ziel

Ein Satz. Was kann jemand danach, was vorher nicht ging?

## Fundstelle

Anlage, Abschnitt, Version — dazu das wörtliche Zitat der maßgeblichen Stelle.

> „…"

**Ohne Fundstelle keine Spezifikation.** Wer keine findet, hat entweder nicht
gesucht oder baut gerade etwas, das nicht vorgeschrieben ist. Beides ist
wissenswert, bevor Code entsteht.

Wenn es wirklich keine gibt (eine reine Bedienentscheidung etwa), steht hier
ausdrücklich: *keine Vorgabe, Entscheidung des Projekts* — mit Begründung.

## Beispiel

Konkret und nachrechenbar, nicht abstrakt:

```
Eingabe:  IK 261914007, Selbstabrechner, September
Ausgabe:  SL191400S09
```

## Abnahmekriterien

Als Testnamen formuliert, deutsch, ganze Sätze — sie werden später wörtlich zu
`@DisplayName`:

- „…"
- „…"

## Nicht-Ziele

Was ausdrücklich **nicht** dazugehört. Verhindert, dass aus einem Arbeitspaket
drei werden.

## Gegenprobe

Welcher Test schlägt fehl, wenn die Änderung fehlt?

Ein Test, der in beiden Fassungen besteht, prüft nichts. Das ist in diesem
Projekt schon vorgekommen und nur aufgefallen, weil jemand die Änderung
versuchsweise zurückgenommen hat.

## Offen

Was zum Zeitpunkt des Schreibens noch niemand weiß — und wer es beantworten
kann.
