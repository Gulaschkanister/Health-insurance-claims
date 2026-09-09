---
title: "Teil 10 — Domänenmodell"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Der Zugang zur Datenbank steht. Was fehlt, sind die Gegenstände, die durch ihn
hindurchgehen: Personen, Gruppen, Blaupausen — und die Abrechnung, die alles
zusammenführt.

Der Entwurf dafür liegt seit Teil 06 vor. Hier wird er gebaut, und dabei zeigt
sich, was der Entwurf nicht vorhergesehen hatte.

## Ziel dieses Teils

Die Klassen des Modells umsetzen, samt Prüfläufen — und die Stellen benennen, an
denen die Umsetzung vom Entwurf abweicht.

## Überlegungen

### Wie Gruppen und Personen zusammenhängen

Eine Gruppe enthält Teilnehmerinnen, und eine Person kann in mehreren Gruppen
sein. Das ist eine Beziehung, die auf beiden Seiten vielfach ist.

Zwei Möglichkeiten:

| Möglichkeit | Folge |
|---|---|
| Die Gruppe kennt ihre Mitglieder, die Person kennt ihre Gruppen | Beide Seiten müssen beim Ändern gepflegt werden — vergisst man eine, widersprechen sie sich |
| Nur die Gruppe kennt ihre Mitglieder | Die Frage „in welchen Gruppen ist diese Person?" braucht eine Abfrage statt eines Feldes |

Gewählt wurde die zweite. **Eine Beziehung mit einer verantwortlichen Seite kann
nicht in sich widersprüchlich werden.** Die Abfrage ist der Preis dafür, und sie
kostet bei diesem Umfang nichts.

### Was eine Abrechnung ist

Der Entwurf beschreibt sie als Verbindung aus versicherter Person,
Leistungserbringerin, Beschreibung und Anzahl. Genau so wurde sie gebaut — mit
einem Zusatz, über den zu reden ist: **sie trägt den Zeitpunkt ihrer Entstehung.**

Das klingt harmlos und ist die einzige Spur, die ein Abrechnungslauf heute
hinterlässt. Die Lieferung selbst hinterlässt keine (Teil 06).

### Die Rückfallebene, die zu freundlich war

Die Werte einer Leistung stehen in der Blaupause, gemeinsam in einem Textfeld
(Teil 06). Wenn dieses Feld unlesbar ist, hat das Programm zwei Möglichkeiten:
abbrechen oder mit Vorbelegungen weitermachen.

Gewählt wurde das Weitermachen, mit der Begründung: Die Nachricht durchläuft
ohnehin die Prüfung, und ein Abbruch beim Erzeugen wäre für die Anwenderin
schwerer zu verstehen als ein Prüfbefund.

Die Begründung trägt — **aber nur, wenn die Vorbelegung selbst auffällig ist.**
Und genau das war sie nicht:

> Der Einzelbetrag stand in der Rückfallebene auf einem plausiblen Wert. Damit
> wurde jeder Termin mit einem Betrag in fünfstelliger Höhe abgerechnet, ohne
> dass eine Prüfung anschlug — er war ja formal gültig.

Der Wert steht heute auf null. Ein Betrag von null fällt in jeder Summenprüfung
auf; ein plausibler Betrag fällt nirgends auf.

> **Eine Rückfallebene muss falsch aussehen, nicht plausibel. Ihr Zweck ist, dass
> jemand sie bemerkt.**

### Prüfläufe, die etwas beweisen

Beim Bauen der Prüfläufe fiel ein zweites Muster auf. Ein Prüflauf, der nur
feststellt, dass ein Wert nach dem Speichern noch da ist, beweist wenig. Nützlich
sind die, die **eine Regel gegen eine Verletzung** stellen:

| Prüflauf | Was er wirklich zeigt |
|---|---|
| Eine gespeicherte Person kommt unverändert zurück | dass die Ablage funktioniert |
| Eine Gruppe ohne Namen wird abgelehnt | dass die Regel gilt |
| Zwei Nummern hintereinander sind verschieden | dass der Zähler zählt |
| Jeder Feldname, den die Blaupause liest, kommt in einer Beschreibung vor | **dass niemand einen Namen erfunden hat** |

Der letzte ist der wertvollste, weil er einen Fehler findet, den kein Mensch
sieht: Ein Feldname, den es nirgends gibt, liefert stillschweigend nichts.

## Entscheidung

**Das Modell aus Teil 06 wird unverändert umgesetzt**, mit drei ergänzenden
Festlegungen:

| Festlegung | Grund |
|---|---|
| Die Gruppe ist die verantwortliche Seite der Beziehung | Widersprüche unmöglich machen |
| Die Rückfallebene der Leistungswerte steht auf null | Sie soll auffallen, nicht durchrutschen |
| Die Liste der gelesenen Feldnamen ist öffentlich einsehbar | damit ein Prüflauf sie gegen die Beschreibungen halten kann |

Die dritte Festlegung sieht wie ein Verstoß gegen guten Stil aus — eine
Innerlichkeit wird nach außen gegeben. Sie ist es auch. **Der Preis ist geringer
als der Fehler, den sie verhindert**, und der Grund steht als Kommentar an Ort und
Stelle.

### Warum nicht der einfachere Weg

Man hätte auf die Rückfallebene ganz verzichten und bei unlesbarer Blaupause
abbrechen können. Dann bräuchte es keine Diskussion über plausible Werte.

Dagegen spricht die Anwenderin: Ein Abbruch mitten im Erzeugen nennt eine
technische Ursache. Ein Prüfbefund nennt die betroffene Zeile und was fehlt — und
das ist Q3 aus Teil 03.

## Ergebnis

Das Modell steht und ist geprüft. Die zwei bemerkenswerten Ergebnisse sind keine
Klassen, sondern Einsichten:

**Vorbelegungen sind gefährlicher als Lücken.** Eine Lücke wird beanstandet, eine
plausible Vorbelegung nicht.

**Ein Prüflauf, der nie fehlschlagen kann, ist kein Prüflauf.** Diese Regel wird
in Teil 21 zum Maßstab für die ganze Teststrategie.

Verwendete Mittel:

| Mittel | Wozu |
|---|---|
| Wertobjekte für Leistungswerte | unveränderlich, mit klaren Feldern statt einer losen Sammlung |
| Eine verantwortliche Seite je Beziehung | Widersprüche ausschließen |
| Auffällige statt plausibler Rückfallwerte | Fehler sichtbar machen |

## Offen geblieben

- **Der fehlende Leistungsnachweis** aus Teil 06. Die Anzahl der Einheiten hängt
  weiterhin an der Abrechnung.
- **Die fehlende Lieferung.** Sie bleibt ein Vorgang ohne Spur.
- **Ob die öffentliche Feldnamenliste dauerhaft bleibt.** Sie ist ein Zugeständnis
  an die Prüfbarkeit — sauberer wäre, die Blaupause selbst prüfen zu lassen.
