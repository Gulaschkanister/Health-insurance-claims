---
name: dta-validierung
description: Prüfregeln für DTA-Nachrichten schreiben, erweitern und testen. Verwenden beim Anlegen einer neuen ValidationRule, beim Ändern des Prüfwerks, bei Fragen zu Fehlercodes, Fehler-gegen-Warnung-Abstufung, ValidationReport, oder wenn die Prüfung etwas meldet beziehungsweise etwas nicht meldet, das sie melden sollte.
---

# Prüfregeln für DTA-Nachrichten

Das Prüfwerk liegt in `gkv-core/src/main/java/de/gkvtransmitter/validator/`.
Es ist die Stufe, die zwischen Erzeugung und Versand entscheidet, ob eine
Abrechnung das Haus verlässt.

## Die Bestandteile

| Klasse | Aufgabe |
|---|---|
| `ValidationRule` | Schnittstelle einer einzelnen Regel |
| `DtaValidationService` | führt die Regeln aus, sammelt die Befunde |
| `ValidationReport` | Gesamtergebnis, entscheidet über Versandfähigkeit |
| `ValidationMessage` | ein Befund: Gewicht, Code, Ort, Text |
| `ValidationSeverity` | `INFO`, `WARNING`, `ERROR` |
| `validator.rules.*` | die mitgelieferten Regeln |

Geprüft wird gegen `DtaDocument` — die zurückgelesene Nachricht mit Segmenten
und Datenelementen. **Keine Regel sucht in Zeichenketten.**

## Eine Regel schreiben

```java
package de.gkvtransmitter.validator.rules;

public final class MeineRegel implements ValidationRule {

    @Override
    public String getName() {
        return "Sprechender Name";   // erscheint in Protokollen
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        for (DtaSegment segment : document.mitTag("REC")) {
            String wert = segment.element(0).trim();
            if (wert.isEmpty()) {
                bericht.error("REC_NUMMER_FEHLT", segment.ort(),
                        "Im REC-Segment fehlt die Rechnungsnummer.");
            }
        }
    }
}
```

Danach in `DtaValidationService.standard()` eintragen — oder zur Laufzeit über
`service.mitZusaetzlicherRegel(neueRegel)` ergänzen, ohne bestehende anzufassen.

### Zugriff auf die Nachricht

```java
document.mitTag("ENF")            // alle ENF-Segmente, in Reihenfolge
document.erstesMitTag("UNB")      // Optional<DtaSegment>
document.anzahlMitTag("UNH")      // int
document.getSegments()            // alle, in Reihenfolge
document.istLeer()

segment.element(3)                // Datenelement, 0-basiert ohne Bezeichner
segment.komponente(1, 0)          // Komponente in "61:00000" → "61"
segment.elementAnzahl()
segment.ort()                     // "ENF (Zeile 15)" für Meldungen
segment.raw()                     // Originalzeile mit Abschlusszeichen
```

`element()` liefert bei fehlendem Element **einen leeren Text, kein `null` und
keine Ausnahme**. Das ist Absicht: Regeln prüfen gerade auf fehlende Elemente
und sollen dabei nicht selbst abbrechen.

## Fehler oder Warnung?

Diese Entscheidung ist die wichtigste an einer Regel, denn nur `ERROR` hält den
Versand auf.

**`ERROR`** — die Kasse würde die Lieferung abweisen, oder die Abrechnung wäre
sachlich falsch:
- Zähler stimmen nicht (`UNT`, `UNZ`)
- IK mit falscher Prüfziffer
- Summen passen nicht zusammen (GES 00 gegen BES)
- Pflichtangabe fehlt (Name, Geburtsdatum, Versichertennummer)
- Geburtsdatum in der Zukunft

**`WARNING`** — auffällig, aber vertretbar. Es wäre falsch, deswegen eine
korrekte Abrechnung aufzuhalten:
- unbekanntes Segment (die Liste bildet den heutigen Umfang ab, nicht den
  gesamten Standard)
- Alter über 120 Jahren (auffällig, aber nicht ausgeschlossen)
- Abweichung bei GES 99 (richtet sich nach der Prüfvorgabe der Kasse)

Im Zweifel gilt: **eine Warnung, die man nicht braucht, ist lästig; ein Fehler,
den man nicht braucht, verhindert eine berechtigte Abrechnung.** Wer sich nicht
sicher ist, nimmt die Warnung.

## Fehlercodes

Der Code ist bewusst vom Meldungstext getrennt, damit sich Befunde auswerten
und filtern lassen, ohne Texte zu vergleichen. Er ist stabil — **einen
vorhandenen Code nicht umbenennen**, Tests und die simulierte Gegenstelle
hängen daran.

Namensschema: `BEREICH_GEGENSTAND`, Großbuchstaben mit Unterstrich.

Vergeben sind unter anderem:

```
SYNTAX_LEER  SYNTAX_ABSCHLUSS  SYNTAX_BEZEICHNER  SYNTAX_UNBEKANNT  SYNTAX_OHNE_INHALT
RAHMEN_UNB_FEHLT  RAHMEN_UNZ_FEHLT  RAHMEN_REFERENZ  RAHMEN_UNB_MEHRFACH
UNZ_ANZAHL  UNZ_ANZAHL_FEHLT  UNT_ANZAHL  UNT_ANZAHL_FEHLT
NACHRICHT_NUMMER  NACHRICHT_NICHT_GESCHLOSSEN  UNH_OHNE_UNT  UNT_OHNE_UNH
IK_FEHLT  IK_LAENGE  IK_ZIFFERN  IK_PRUEFZIFFER
VERSICHERTER_NACHNAME  VERSICHERTER_VORNAME  VERSICHERTER_GEBURTSDATUM
GEBURTSDATUM_ZUKUNFT  GEBURTSDATUM_ALT  GEBURTSDATUM_FORMAT
VERSICHERTENNUMMER_FEHLT  VERSICHERTENNUMMER_NULL
BETRAG_ENF_BES  BETRAG_GES_BES  BETRAG_GES99_ABWEICHUNG
REGEL_ABGEBROCHEN
```

Die Präfixe `SYNTAX_`, `RAHMEN_`, `UNT_` und `UNZ_` haben eine Nebenwirkung:
`SimulierteKassenGegenstelle` stuft eine Lieferung als **Syntaxfehler** statt
als fachliche Zurückweisung ein, wenn ausschließlich solche Codes gemeldet
werden. Ein neuer Strukturcode sollte deshalb eines dieser Präfixe tragen.

## Meldungstexte

An Anwenderinnen gerichtet, die die Abrechnung korrigieren müssen — nicht an
Entwickler. Ganze Sätze, konkrete Werte nennen:

```java
// gut
"Das UNZ-Segment nennt 5 Nachrichten, enthalten sind aber 2."
"Das Absender-IK '108310401' hat eine falsche Prüfziffer, erwartet wurde 0 an letzter Stelle."

// schlecht
"UNZ invalid"
"Prüfung fehlgeschlagen"
```

Den Ort immer über `segment.ort()` mitgeben, sonst ist die Stelle nicht
auffindbar.

## Testen

Muster in `DtaValidationServiceTest`. Grundlage ist die Referenznachricht aus
`Information/Valide.DTA` als Konstante im Test.

```java
@Test
@DisplayName("Eine falsche Nachrichtenanzahl im UNZ faellt auf")
void erkenntFalscheNachrichtenanzahl() {
    ValidationReport bericht = service.pruefe(ersetze("UNZ+000002+00001'", "UNZ+000005+00001'"));

    assertTrue(enthaeltCode(bericht, "UNZ_ANZAHL"), bericht.alsText());
    assertFalse(bericht.istVersandfaehig());
}
```

Zwei Dinge gehören zu jeder neuen Regel:

1. **Die Referenz muss weiterhin unbeanstandet durchlaufen.** Der Test
   `referenzIstGueltig` deckt das ab. Schlägt er nach einer neuen Regel fehl,
   ist zuerst die Regel verdächtig — nicht die Referenzdatei.
2. **Ein Test, der genau eine Vorgabe verletzt.** Sonst ist nicht belegt, dass
   die Regel den Fehler findet und nicht nur zufällig etwas meldet.

Die Zusicherungen immer mit `bericht.alsText()` als Meldung versehen — sonst
steht bei einem Fehlschlag nur „expected true but was false" da.

## Regeln dürfen nicht abstürzen

`DtaValidationService` fängt eine geworfene `RuntimeException` ab und nimmt sie
als `REGEL_ABGEBROCHEN` in den Bericht auf, damit die übrigen Regeln
weiterlaufen. Darauf sollte sich keine Regel verlassen: wer einen Wert nicht
findet, meldet einen Befund und kehrt zurück.

## Berichte zusammenführen

```java
ValidationReport gesamt = ValidationReport.leer();
for (String nachricht : nachrichten) {
    gesamt = gesamt.plus(service.pruefe(nachricht));
}
if (gesamt.hatFehler()) { ... }
```

`ValidationReport` ist unveränderlich; `plus` liefert einen neuen Bericht.
