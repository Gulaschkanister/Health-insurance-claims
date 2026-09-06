# Nächste Schritte

Stand: 6. September 2026, Branch `feature/kern-architektur`.

Dieses Dokument ist die Übergabe. Es hält fest, wo das Projekt steht, was als
Nächstes ansteht und welche Fallstricke bereits bekannt sind — damit die Arbeit
ohne Anlauf weitergehen kann.

## Wo das Projekt steht

Erledigt und geprüft:

- Aufteilung in `gkv-core` (ohne JavaFX, per Enforcer erzwungen) und `gkv-ui`
- Hibernate 6.6 mit `jakarta.persistence`, SQLite-Persistenz instand gesetzt
- Validierungsstufe als Tor vor dem Versand, sechs Regeln
- Kassen-Kommunikation: Antwortauswertung berichtigt, simulierte Gegenstelle
- Datenablage im Benutzerprofil, eigenständiges Windows-Paket über `jpackage`
- `View` von 1438 auf rund 300 Zeilen zerlegt, fünf Masken herausgelöst
- **Oberfläche neu: Seitenleiste, Listen, Meldungsecke, Stylesheet**
- **Feldprüfung mit Erklärung unter jedem Feld, IK gegen die Prüfziffer**
- **Blaupausen: Übersicht mit Suche, Bearbeiten und Löschen; der Preis je Termin
  ist einstellbar** (war er nie, siehe D)
- **344 Tests**, davon 187 in `gkv-ui`, `BUILD SUCCESS`, Checkstyle 10 Warnungen
- **Abschnitte B (soweit ohne Anlage 3), C, G, H, I und J sind abgearbeitet**
- **Das Paket ist gebaut und gestartet**, das Programm heißt „GKV-Abrechnung"
- Fünf Skills unter `.claude/skills/`, Dokumentation und Diagramme aktuell

**Der lokale Stand ist 33 Commits vor `origin`.** Das ist der letzte offene
Punkt vor der Retrospektive: einmal `git push`, dann Pull Request oder Merge —
siehe „Sofort zu entscheiden". Sonst hängt viel unveröffentlichte Arbeit an
einem Rechner.

## Was zuletzt geschah (06.09.2026)

Fünf Commits: `3f05830` (das Fenster bei wenig Platz), `e05c050` (zwei
Kleinigkeiten aus F), `2d889c8` (Abschnitt I: die Umbenennung), dazu die
Nacharbeit an Dokumentation und Übergabe.

**Damit ist Abschnitt J vollständig** — der Stand, den Simon sich ansehen
sollte, steht.

Der aufschlussreichste Punkt war der scheinbar kleinste: „einmal das Fenster
klein ziehen und hinsehen". Er brachte **drei echte Fehler**, darunter zwei,
die bei der *Startgröße* der Anwendung auftraten — also bei jedem Start. Die
Einzelheiten stehen in J.

**Merksatz daraus:** eine Startgröße veraltet still. Sie stand auf 900×600 aus
einer Zeit, in der die Oberfläche aus einer Menüleiste bestand; inzwischen hat
sie eine Seitenleiste von 224 Punkten und Listen mit vier Spalten und zwei
Schaltflächen je Zeile.

## Was davor geschah (05.09.2026, Abend)

Sechs Commits, in dieser Reihenfolge: `7307125` (Abschnitt B), `f72b656` (G5),
`d61f382` (G2), `38db315` (H), `51d01c2` (Abschnitt C), `7ff85dc` (J-3).

**Das Wesentliche daran ist nicht, was umgesetzt wurde, sondern was dabei
herauskam.** Vier Fehler, die kein Test gefunden hätte, und alle vier von
demselben neuen Werkzeug — dem Bedienwerkzeug aus Abschnitt H, das die
Anwendung wirklich bedient statt sie nachzubauen:

| Fund | Wirkung |
|---|---|
| „Termine für alle: 8" → „Setzen" ergab **eine** Sitzung je Zeile | Rechnung auf ein Achtel des Betrags |
| Das Bearbeiten schrieb **gar nichts** und meldete Erfolg | jede Korrektur an Stammdaten verpuffte |
| `000000000` galt als gültiges IK | Person ganz ohne IK kam bis zur Kasse |
| Die Fehlermeldung verdeckte die Felder, die sie zu berichtigen verlangte | Sackgasse am Bildschirm |

Die ersten beiden gehören zusammen mit dem Einzelbetrag vom Vortag in dieselbe
Familie: **ein Bruch zwischen zwei je für sich fehlerfreien Bausteinen, bei dem
nichts fehlschlägt.** Ein Test, der prüft, *dass* gespeichert wird, findet
keinen davon; einer, der prüft, *was* ankommt, findet alle.

## Was davor geschah (05.09.2026, Nachmittag)

Drei Dinge, in dieser Reihenfolge.

**1. Die offenen Punkte B1, B2 und C abgearbeitet.** Die Vorlage
„Rückbildungskurs nach Geburten", Suche und Zähler in den Mitgliederlisten der
Gruppenmaske, und `EditFormControllerTest` mit 14 Tests. Die Einzelheiten stehen
in den jeweiligen Abschnitten; der wichtigste Fund war, dass **das Bearbeiten
gar nicht prüfte** — die schärfere Eingangsprüfung vom selben Tag hatte nur den
Weg verlagert, auf dem falsche Stammdaten entstehen.

**2. Simons Sammlung aufgenommen** — vierzehn Punkte, jetzt die Abschnitte
B 1–4, G, H, I und J. Vier davon enthielten eine Faktenfrage; die ist
nachgeprüft und nicht übernommen worden. Was dabei herauskam, steht bei den
Punkten selbst.

**3. Code-Pflege.** Zwei tote Klassen entfernt (siehe C), fünfzehn unbenutzte
Meldungsschlüssel aus `ui-messages.json`, und zwei Meldungen berichtigt:

- Das **Bearbeiten** meldete „Erfolgreich gespeichert!", ohne zu sagen, *was*.
  Beim Anlegen war genau das schon einmal berichtigt worden; die passenden Texte
  lagen seither unbenutzt in `ui-messages.json`. `EditFormController` bildet den
  Schlüssel jetzt aus dem Typnamen (`msg.patientUpdated`), wie er es bei
  `title.<typ>.edit` und `msg.no<Typ>s` schon tat, und weicht über das neue
  `AppMessages.get(schluessel, ersatz)` auf `msg.saved` aus, wenn es für eine Art
  keinen eigenen Text gibt. Ohne diesen Ausweg stünde bei einer Lücke der
  Schlüsselname in der Oberfläche.
- Die Meldung nach dem Anlegen der Testdaten nannte „2 Blaupausen", nachdem es
  drei geworden waren. Sie **zählt jetzt**, statt die Zahl im Text zu führen.
  Eine Zahl im Meldungstext veraltet beim nächsten Zusatz still.

## Was davor geschah: die Oberfläche

Vier Wünsche standen im Raum — keine Popups, effiziente Monatsbearbeitung,
automatische Abrechnung, schöneres Aussehen. Drei davon sind umgesetzt; die
**automatische Abrechnung wurde bewusst zurückgestellt** („lassen wir das
erstmal weg, ist sonst zu kompliziert“).

### Keine Dialogfenster mehr

Im laufenden Betrieb öffnet die Anwendung **kein Fenster**. Meldungen
erscheinen in der Ecke oben rechts und gehen nach sechs Sekunden von selbst;
Fehler und Rückfragen bleiben stehen, bis jemand sie zur Kenntnis nimmt.

Der Schnitt der Schnittstelle ist dabei das Wesentliche: aus
`Optional<T> waehleAus(...)` wurde `void waehleAus(..., Consumer<T>)` — und
inzwischen ist die Methode ganz verschwunden. Ein `Optional` als Rückgabe muss
den aufrufenden Faden anhalten, bis jemand geklickt hat, und genau dieses
Anhalten *ist* das Fenster.

Einzige Ausnahme: ein Fehler beim Programmstart. Die Meldungsecke hängt in der
Hauptszene, die es zu diesem Zeitpunkt noch nicht gibt.

### Seitenleiste und Listen

Die Menüleiste ist weg. Links stehen die Bereiche, der gewählte bleibt
hervorgehoben. Teilnehmer, Dienstleister und Gruppen haben je eine Übersicht
mit Suchfeld und „Bearbeiten“/„Löschen“ in der Zeile — statt eines
Auswahlfensters, in dem man wissen musste, wen man sucht.

### Neue Klassen in `gkv-ui`

| Klasse | Was sie übernimmt |
|---|---|
| `Hauptfenster` | Seitenleiste, Kopfzeile, Inhalt, Statuszeile |
| `Benachrichtigungen` | die Meldungsecke oben rechts |
| `meldung.Meldungen` | melden und nachfragen, ohne Fenster |
| `meldung.Bildschirmmeldungen` | die Umsetzung in der Ecke |
| `meldung.Pruefbefunde` | Reihenfolge der Beanstandungen, ohne JavaFX |
| `Listenbau` | durchsuchbare Liste mit Schaltflächen je Zeile |
| `Maskenkopf` | „+ Neu“ über einer Übersicht |
| `style/gkv.css` | Farben, Abstände, Schriftgrößen an einem Ort |

### Fehler, die dabei ans Licht kamen

1. Scheiterte der Start, rief `App` `view.showErrorDialog(...)` auf. War aber
   schon der `Controller` gescheitert — der häufigste Fall —, war `view` noch
   `null`, und statt der Ursache erschien eine `NullPointerException`.
2. Der `statusText` wurde seit jeher an `createMainScene` übergeben und dann
   nicht verwendet. Eine Statuszeile gab es gar nicht.
3. „Keine Patienten vorhanden.“ stand in der Abrechnungsmaske auch dann, wenn
   lediglich noch keine Gruppe gewählt war.
4. Die Rückfrage vor dem Löschen im Bearbeiten-Formular zeigte
   „Soll %s wirklich gelöscht werden?“ mit unersetztem Platzhalter — der Text
   wurde nie formatiert.
5. Die Kästchen neben den Dienstleistern in der Abrechnungsmaske ließen sich
   anhaken, hatten aber keine Wirkung. (Stand als Fundstück in der vorigen
   Übergabe; jetzt behoben.)

## Sofort zu entscheiden

### 1. Branch nach `main` bringen

Der Branch ist grün. Zur Wahl stehen:

- **Pull Request** über GitHub — vorher `git push`, damit der Remote-Stand
  vollständig ist
- **Merge nach `main`** — `git switch main && git merge --no-ff feature/kern-architektur`

Zu beachten: Die drei Commits `632e906`, `475321d`, `6460b78` stammen aus
`origin/refactor` und liegen bereits auf dem Remote. Die übrigen Commits tragen
`simi_9@web.de`; die drei aus `refactor` weiterhin `Simon.Bausch@steep.de`.

Der Sicherungs-Branch `backup/vor-identitaetswechsel` kann nach dem Merge weg:

```
git branch -D backup/vor-identitaetswechsel
```

### 2. Verwaiste Remote-Branches aufräumen

Auf `origin` liegen sieben `copilot/*`-Branches aus abgeschlossenen PRs sowie
`dev` (vollständig in `main` enthalten) und `restart`.

## Priorisierte nächste Schritte

**Das Ziel steht in Abschnitt J**: ein Stand, den Simon sich ansieht und
ausprobiert — „fast ein fertiges Produkt". Alles Übrige läuft darauf zu. Dort
steht auch, was dafür erledigt sein muss und was offen bleiben darf.

### A. Automatische Abrechnung — zurückgestellt, nicht vergessen

Der Wunsch bleibt sinnvoll, sobald das Übrige steht. Was dafür nötig wäre:

1. Eine **Kursvorlage** speichert Blaupause, Gruppe und Terminzahl. Neue
   Entität in `gkv-core/entity`, Schema wächst über `hbm2ddl=update` mit.
2. Ein **Lauf-Verzeichnis** hält fest, wann welcher Kurs zuletzt abgerechnet
   wurde — Voraussetzung für „Vormonat übernehmen“ und für jede Fälligkeit.
3. Erst darauf lässt sich eine Fälligkeitsanzeige beim Programmstart bauen.

Von einem Versand ohne Rückfrage ist abzuraten: es gehen echte Forderungen an
die Kasse, eine falsche Terminzahl bemerkt niemand vor der Ablehnung, und ein
Storno gibt es fachlich noch gar nicht (siehe D).

Bis dahin helfen die Werkzeuge in der Abrechnungsmaske: „Alle auswählen“,
„Termine für alle“ und die mitrechnende Zusammenfassung.

### B. Von Simon gewünscht, noch offen

#### Fachliche Felder: was fehlt, was überflüssig ist

**Stand:** 1, 2 und 3 sind erledigt (05.09.2026). 4 und 5 brauchen Anlage 3
beziehungsweise den Vertrag und bleiben offen — das ist keine Lücke im
Programm, sondern eine in den Unterlagen.

1. ~~**Die Meldung zum IK ist irreführend.**~~ **Erledigt.** Sie lautet jetzt:

   > Ein IK wird von der Datenannahmestelle vergeben und lässt sich nicht frei
   > wählen: die letzte Ziffer muss zu den übrigen passen. **Zu 12345678 gehört
   > die Prüfziffer 0, das gültige IK lautete also 123456780.**

   Aus der Sackgasse ist ein Hinweis geworden. Rechnen konnte
   `Institutionskennzeichen.berechnePruefziffer` das längst; es wurde nur
   nirgends angezeigt. Eine zu kurze Eingabe bekommt eine eigene Meldung — sie
   hat keine Prüfziffer, die zu nennen wäre.

   **Dabei gefunden: `000000000` galt als gültig.** Alle Stellen 3 bis 8 sind
   0, die Summe ist 0, und 0 mod 10 ist genau die letzte Ziffer. `Person.ik`
   ist ein `int`, ein nicht ausgefülltes Feld ist also exakt diese Zahl — eine
   Person ganz ohne IK kam durch die Eingabeprüfung, durch die Speicherprüfung
   und durch die Validierung vor dem Versand. Aufgefallen, weil die
   Beanstandung für `000000000` keine Prüfziffer zu nennen hatte.

   Auch die Erklärungen unter den IK-Feldern sagen jetzt, dass das Kennzeichen
   *vergeben* wird. Und das angehängte „Höchstens 9 Zeichen." ist weg: bei
   einem Feld mit **fester** Länge ist eine Höchstlänge nicht nur redundant,
   sondern falsch — ein achtstelliges IK ist nicht kürzer und damit in Ordnung,
   es ist ungültig.

2. ~~**Der Umsatzsteuersatz soll Vorschläge machen.**~~ **Erledigt.** Er bietet
   19, 7 und 0 an, in einem **beschreibbaren** Auswahlfeld.

   Die Mechanik war, wie vermutet, verkehrt herum eingesetzt. Jetzt gilt:
   **hinterlegte Werte schlagen die Art des Feldes.** Wo Vorschläge liegen, gibt
   es ein Auswahlfeld, gleich ob das Feld als `PERCENT`, `CODE` oder
   `NUMBER_SUGGESTION` beschrieben ist.

   Die drei Sätze stehen in `View.UMSATZSTEUERSAETZE` und nicht in einer
   JSON-Datei: sie sind keine Codeliste aus Anlage 3, sondern allgemeines
   Steuerrecht. Welcher gilt, sagt die Steuerberatung — deshalb Vorschläge und
   keine Auswahl.

3. ~~**Der Abrechnungscode braucht kein Auswahlfeld.**~~ **Erledigt, soweit es
   ohne Anlage 3 geht.** Ein einziger Vorschlag ergibt jetzt ein **ausgefülltes
   Textfeld** statt eines Aufklappmenüs mit einer Zeile. Die Regel ist
   allgemein und gilt für jede Codeliste, die auf einen Eintrag schrumpft.

   **Die Frage dahinter bleibt offen:** ist der Code je Vorlage fest, gehört er
   gar nicht ins Formular, sondern als `internal`-Feld in `enf.json`. Dafür
   spricht alles, was hier belegt ist — aber ob ein künftiger Leistungsbereich
   einen anderen Code bringt, steht in Anlage 3. Der Umbau wäre klein; ihn ohne
   die Antwort zu machen hieße, eine Tür zuzumauern.

4. **Für die Abrechnungspositionsnummer gibt es keine Liste** — Simons Frage,
   nachgesehen: nein, im Projekt existiert keine. `codes/` enthält
   Abrechnungscodes, Statuscodes, Nachrichtentypen, Rechnungsarten und
   Verarbeitungskennzeichen; `Information/codes/03_leistungscodes_sgs_h.json`
   führt die Positionsnummer nur als **Beispiel** `306050601` mit dem Vermerk
   „Muss vertraglich/fachlich gültig sein".

   Die echte Liste steht in **Anlage 3, Abschnitt 8.2**. Sie dort zu entnehmen
   und als JSON abzulegen wäre lohnend: daraus würde ein Auswahlfeld *mit*
   Inhalt, und aus der bloßen Formprüfung eine echte
   `PositionsnummerRegel` — heute wird nur geprüft, dass etwas dasteht, nicht,
   ob es zulässig ist. Ohne die Anlage ist das nicht zu machen und **nicht zu
   raten**: eine erfundene Positionsnummer führt zur Zurückweisung der ganzen
   Lieferung.

5. **Woher kommt das Tarifkennzeichen — und wovon hängt es ab?**
   Offene Frage von Simon, die vor dem ersten echten Versand beantwortet sein
   muss.

   **Was das Projekt belegt** (`Abrechnung_Datenerklaerung_test.md`,
   `codes/03_leistungscodes_sgs_h.json`): fünfstellig numerisch, zweiter Teil
   des Kompositfelds `Abrechnungscode:Tarifkennzeichen` im `ENF`, im Beispiel
   `61:00000`. Herkunft laut Unterlagen: **Vertrag, dazu Anlage 3,
   Schlüssel 8.1.5.2.** Eine allgemeine Codeliste gibt es nicht — deshalb steht
   im Formular ein Textfeld und kein Auswahlfeld.

   **Personenabhängig ist es nicht.** Es steht in der Leistungszeile, nicht in
   den Angaben zur versicherten Person: es beschreibt die erbrachte Leistung
   und die Gruppe des Leistungserbringers, nicht die Teilnehmerin. Ob es sich
   je nach gesetzlicher oder privater Versicherung unterschiede, stellt sich
   nicht — privat Versicherte werden gar nicht über diesen Weg abgerechnet.

   **Offen ist, ob es je Kasse verschieden ist.** Der Vertrag besteht zwischen
   Leistungserbringerin und Kassenseite; ob dabei alle Kassen dasselbe
   Kennzeichen führen oder jede ihr eigenes, geht aus den Unterlagen hier nicht
   hervor. **Das ist nicht zu raten** — die Antwort steht in Anlage 3,
   Abschnitt 8.1.5.2, im eigenen Vertrag oder ist bei der Datenannahmestelle
   beziehungsweise dem Verband zu erfragen.

   **Warum das für den Entwurf zählt:** heute steht das Tarifkennzeichen in der
   Blaupause, und eine Blaupause gilt für einen Kurs. Ist das Kennzeichen je
   Kasse verschieden, reicht das nicht — dann bräuchte es eine Zuordnung
   Kasse → Tarifkennzeichen, und die Blaupause dürfte es nicht mehr fest
   führen. Solange die Frage offen ist, sollte niemand mehrere Blaupausen nur
   deshalb anlegen.

   **Nachtrag, Simons Rückfrage: hängt es am Dienstleister?** Das ist die
   richtige Frage, und sie ist plausibler als die nach der Kasse. Das
   Kompositfeld heißt `Leistungserbringergruppe` — es beschreibt, *wer* die
   Leistung erbracht hat, und der Vertrag wird von der Leistungserbringerin
   geschlossen. Zwei Hebammen mit verschiedenen Verträgen hätten dann
   verschiedene Kennzeichen.

   **Belegen lässt sich das hier nicht**, und geraten wird es nicht. Aber die
   Konsequenz, die Simon zieht, ist richtig: **hängt es am Dienstleister, gehört
   es aus dem Blaupausenformular heraus und an den Dienstleister.** Dann wäre es
   eine Eigenschaft der Person — neues Feld an `ServiceProvider`, Schema wächst
   über `hbm2ddl=update` mit —, und `DtaFactory` nähme es von dort statt aus der
   Blaupause. Das hätte den Nebennutzen, ein Feld aus dem Formular zu nehmen,
   das heute jeder bei jeder Blaupause neu eintragen muss und dabei verschieden
   eintragen kann.

   **Die drei Möglichkeiten und wo das Kennzeichen jeweils hingehört:**

   | Wovon es abhängt | Wohin es gehört | Aufwand |
   |---|---|---|
   | von nichts, immer gleich | `internal` in `enf.json`, wie der Abrechnungscode | klein |
   | vom Dienstleister | Feld an `ServiceProvider` | mittel |
   | von der Kasse | eigene Zuordnungstabelle Kasse → Kennzeichen | groß |

   **Erst die Antwort, dann der Umbau.** Alle drei sind machbar; zwei davon
   wären falsch. Die Frage ist bei der Datenannahmestelle, im Vertrag oder in
   Anlage 3, Abschnitt 8.1.5.2 zu klären — und sie blockiert nichts anderes,
   weil die heutige Lösung (in der Blaupause) für **einen** Dienstleister mit
   **einem** Vertrag richtige Ergebnisse liefert.

**Erledigt am 05.09.2026:** die Blaupausenmaske. Sie liegt jetzt als
`BlaupausenMaske` neben den anderen (Übersicht mit Suche, Bearbeiten und
Löschen), `View` ist von 531 auf rund 300 Zeilen geschrumpft, und
`DataRepository` hat endlich ein `deleteBlueprint`.

**Erledigt am 05.09.2026: die Vorlage „Rückbildungskurs nach Geburten".** Sie
liegt als `invoices/postnatal_class_single.json` und steht in
`JsonParserFactory.INVOICE_FILES`.

Sie ist bis auf den Anzeigenamen mit der Geburtsvorbereitung identisch, und das
ist richtig so: eine Vorlage legt **nur die Segmentfolge und den Namen** fest.
Beide Kurse werden im Leistungsbereich SGS H abgerechnet und ergeben dieselbe
Nachricht. Was sie unterscheidet — Abrechnungspositionsnummer, Abrechnungscode,
Preis — steht seit dem Umbau vom 05.09.2026 in der **Blaupause**, nicht in der
Vorlage. Die passende Positionsnummer für die Rückbildungsgymnastik steht in
Anlage 3 beziehungsweise im Vertrag; sie wird beim Anlegen der Blaupause
eingetragen und ist hier nicht zu raten.

Dabei aufgefallen: der Block `codes` in den Vorlagendateien wird zwar eingelesen
und hängt als `headerCodes` an der `DtaMessage`, aber **niemand liest ihn aus**.
Wer dort einen Code ändert, ändert nichts. Vermerkt im Javadoc von
`INVOICE_FILES`.

`VorlagenTest` hält fest, was dabei zu beachten ist: Vorlagennamen müssen
**paarweise verschieden** sein. `GlobalDefinitions.registerInvoiceTemplate` legt
sie in einer Abbildung über den Namen ab — zwei gleiche verdrängten einander
stillschweigend, und jede Blaupause, die auf die verdrängte zeigt, ließe sich
nicht mehr öffnen. Genau so verhielt sich die alte Testdaten-Blaupause mit ihrem
`test-template`.

**Erledigt am 05.09.2026: Suche in den Mitgliederlisten der Gruppenmaske.**
Über Teilnehmern und Dienstleistern steht je ein Suchfeld und ein Zähler
„x von y ausgewählt".

Zwei Entscheidungen dabei, die nicht offensichtlich sind:

- Die Suche **blendet aus, statt zu entfernen** (`setVisible` *und* `setManaged`,
  sonst bliebe die Lücke stehen). Ein ausgeblendetes Kästchen behält seinen
  Haken; wer erst Anna sucht und anhakt und dann Bea, verliert Anna nicht. Der
  Zähler steht genau deshalb daneben — sonst wäre eine Auswahl zu sehen, die
  kleiner ist als sie ist.
- Die Zuordnung Kästchen → Person liegt jetzt in einer `Map`, nicht mehr in zwei
  parallelen Listen mit gemeinsamem Index. Der alte Gleichlauf war eine stille
  Bedingung: hätte jemand die Personen sortiert oder gefiltert, ohne die
  Kästchen mitzuziehen, wären **die falschen Personen** in der Gruppe gelandet,
  ohne dass irgendetwas fehlschlägt. Erst dadurch ist die Suche gefahrlos.

Neun Tests in `GruppenMaskeTest$MitgliederSuchen`.

### C. Oberflächentests: was noch fehlt

**Erledigt am 05.09.2026:** `EditFormController` hat 14 Tests
(`EditFormControllerTest`). Er trug das gesamte **Bearbeiten** von Personen und
war die größte ungedeckte Stelle. Der Test fand dabei sofort etwas, das kein
anderer Test finden konnte — siehe unten.

Der Zugang führt über `onFormReady`: der Controller reicht dort den Behälter
heraus. Ein `lookup` auf das zurückgegebene `ScrollPane` geht ins Leere, solange
dessen Inhalt noch nicht im Knotenbaum hängt; wer das braucht, hängt es kurz in
eine `Scene` und ruft `applyCss()` und `layout()` — so machen es
`EditFormControllerTest.aufbauen` und `PersonenMaskeTest.aktualisieren`.

**Erledigt am 05.09.2026 (Abend): die Populatoren — und dabei ein stiller
Totalausfall.** `PopulatorTest` prüft sie jetzt mit dem **echten** `Feldbau`,
so wie die Maske die Felder baut. Beim Schreiben kam heraus, was die Übergabe
für diese Klassen vorhergesagt hatte:

`unwrapField` nahm das erste Kind der Hülle. Seit demselben Tag ist das die
Zeile aus Bedienelement **und Info-Zeichen** (G5). `extractFieldValue` bekam
damit eine `HBox`, lieferte `""`, und `setEntityFieldValue` überging den
Leerwert. **Das Bearbeiten schrieb gar nichts mehr und meldete Erfolg.**

Kein Test schlug fehl — die Maskentests benutzen einen eigenen, einfachen
Populator, und `PersonenMaskeTest$Bearbeiten` prüft, *dass* gespeichert wird,
nicht *was*. Die neuen Tests fallen nachweislich, wenn man den Fehler wieder
einbaut; vier von ihnen, darunter „Sonst meldet die Maske Erfolg und hat nichts
geändert".

**Die Gegenmaßnahme:** `Feldbau.bedienelement` ist jetzt **statisch**, und alle
drei Stellen, die ein Bedienelement aus seiner Hülle holen, gehen dorthin —
`BlaupausenMaske`, `EntityFieldPopulator` und das Bedienwerkzeug. Der Weg zum
Bedienelement gehört an eine Stelle. Wer die Hülle umbaut, ändert eine Zeile.

**Ebenfalls erledigt:** `ControllerTest` (was der Programmstart lädt, samt der
Kette zu `Leistungsparameter.BLAUPAUSENFELDER`) und `BildschirmmeldungenTest` —
die einzige Umsetzung von `Meldungen`, die wirklich etwas anzeigt, und damit
der Weg, den im Betrieb jede Meldung geht und in den Tests keine.

Dazu kommt `AblaufTest`: ein ganzer Monat gegen die echte Anwendung mit echter
Datenbank (siehe H). Er berührt nebenbei `View`, `Maskenkopf`,
`Abrechnungslauf` und `JavaFxUiFactory` — nicht als Ersatz für eigene Tests,
aber als Nachweis, dass sie zusammen laufen.

Was in `gkv-ui` **weiterhin ohne eigenen Test** ist:

| Klasse | Zeilen | Warum das zählt |
|---|---|---|
| `View` | 320 | Der Einstieg: Navigation, Vorlagen, Testdaten. Ueber `AblaufTest` mitgelaufen, aber nicht fuer sich geprueft. |
| `Maskenkopf`, `Abrechnungslauf` | 45 + 32 | Klein; `Maskenkopf` steht in jeder Uebersicht und laeuft damit in jedem Maskentest mit, `Abrechnungslauf` ist nur eine Schnittstelle. |
| `JavaFxUiFactory` | 240 | Reine Fabrik. Seit dem Zaehler-Fund nicht mehr ganz entscheidungsfrei — die Uebernahme beim Fokusverlust steckt dort. |

**Erledigt am 05.09.2026: `FormBuilder` ist gelöscht** (234 Zeilen), er wurde
von nichts benutzt — kein Aufruf, kein Import, in keinem Modul. Zusammen mit
`DtaProfile` (8 Zeilen, leeres Markierungsinterface, keine Implementierung).
`Vision.md` und `PROGRESS_UPDATE_v3.md` führten den `FormBuilder` weiter als
eingesetztes Entwurfsmuster, zusammen mit einem `MenuBuilder`, den es gar nicht
mehr gibt; beide Dokumente sind nachgezogen. In `Vision.md` steht in der Zeile
jetzt, was das Muster hier **wirklich** verkörpert: `Listenbau` und `Feldbau`.

Die Suche danach lohnt sich gelegentlich wieder — sie kostet eine Zeile:

```bash
for f in $(find gkv-core/src/main/java gkv-ui/src/main/java -name "*.java"); do
  n=$(basename "$f" .java)
  [ "$(grep -rl "\b$n\b" --include=*.java gkv-core/src gkv-ui/src | grep -vc "/$n.java$")" = 0 ] \
    && echo "unreferenziert: $f"
done
```

#### Erledigt am 05.09.2026: das Bearbeiten prüfte nichts

Beim Schreiben der Tests kam heraus, dass die schärfere Eingangsprüfung vom
Vortag nur den Weg verlagert hatte, auf dem falsche Stammdaten entstehen.

*Anlegen* prüfte seit dem 05.09.2026 — `PersonenMaske.speichere` ruft
`pruefe(...)`. *Bearbeiten* lief über `EditFormController.saveEntity`, und der
übertrug die Eingaben in die Person und reichte sie **ungeprüft** weiter. Eine
richtig angelegte Person ließ sich also nachträglich kaputtbearbeiten: IK auf
`101` gesetzt, Geburtsdatum geleert, „Erfolgreich gespeichert!". Der Fehler kam
erst beim Versand zurück — genau das, was verhindert werden sollte.

Umgesetzt als `PersonenMaske.gepruefteSpeicherung(...)`: die Beanstandung reist
als Ausnahme, `saveEntity` fängt sie und meldet ihren Text, ohne das Formular zu
leeren. **Kein dreizehnter Konstruktorparameter** — der Konstruktor trägt bereits
zwölf und steht deshalb als Checkstyle-Befund in Abschnitt F.

Geprüft wird an der fertigen Person, nicht an den Feldern: Vor- und Nachname,
Geburtsdatum, beide IK gegen die Prüfziffer. Dieselbe Auswahl wie beim Anlegen.
Die drei Tests in `PersonenMaskeTest$Bearbeiten` schlagen nachweislich fehl,
wenn man die Prüfung wieder herausnimmt.

### D. Fachliche Lücken

Nach Nutzen geordnet:

1. **Stornierung und Nachberechnung** — heute gar nicht vorhanden. Sobald real
   abgerechnet wird, wird das gebraucht.
2. **Weitere Leistungsbereiche** — abgedeckt ist nur SGS H mit Abrechnungscode
   `61`. Die Struktur dafür steht, es fehlen die Daten aus Anlage 3.
3. **Positionsnummern und Tarifkennzeichen** werden auf Form, nicht auf
   fachliche Zulässigkeit geprüft. Dafür bräuchte es die Schlüsseltabellen aus
   Anlage 3 als JSON — dann wäre es eine weitere `ValidationRule`.
4. **Rechnungsnummer und Belegnummer sind fest verdrahtet.** `DtaFactory`
   schreibt `REC+00000000:0` und leitet die Belegnummer aus Zeit und laufender
   Nummer ab. Eine echte Sammel- und Einzelrechnungsnummer führt das Programm
   nicht. Vor einem echten Versand zu klären.

#### Erledigt am 05.09.2026: der Rechnungsbetrag war nicht einstellbar

Steht hier, weil der Fehler lehrreich ist und die Gegenmaßnahmen erhalten
bleiben müssen.

`Leistungsparameter.ausBlueprint` suchte in der Blaupause nach
**„Durchschnittlicher Einzelbetrag"**. Dieses Feld trug in `segments/enf.json`
eine `person`-Markierung, und `View.createFormular` blendet solche Felder aus.
Es stand damit in keinem Formular, konnte in keiner Blaupause landen, und
`ausBlueprint` fiel jedes Mal auf `VORBELEGUNG` zurück: **jede Rechnung lautete
auf 15.000,00 € je Termin.** Abrechnungscode und Tarifkennzeichen waren aus
einem zweiten Grund unerreichbar — sie sind Komponenten eines Kompositfelds,
und `JsonParserFactory` las `components` überhaupt nicht.

Kein Test schlug fehl, weil alle gegen die Vorbelegung prüften. Der Bruch lag
**zwischen** zwei je für sich fehlerfreien Bausteinen: einer JSON-Datei und
einer Klasse, die Namen darin sucht.

Geändert wurde:

- `enf.json`: `person`-Markierungen entfernt; Einzelbetrag,
  Abrechnungspositionsnummer und Zuzahlung stehen im Formular; Anzahl/Menge und
  Leistungsdatum auf `internal`, weil die Anwendung sie aus der Abrechnung setzt
- `JsonParserFactory` liest `components` (Streckung der Positionen um
  `KOMPOSITFAKTOR`, damit die Reihenfolge hält)
- `ges.json`, `bes.json`, `fkt.json`, `rec.json`, `unb.json`: die Felder, die
  `DtaFactory` selbst setzt, auf `internal` — das Formular fragte nach Werten,
  die es anschließend verwarf
- Umsatzsteuersatz kommt aus der Blaupause statt aus einer Konstanten
- `Leistungsparameter.BLAUPAUSENFELDER` macht die gesuchten Namen prüfbar

**Die Gegenmaßnahme nicht entfernen:** `BlaupausenfelderTest` geht denselben Weg
wie die Maske (`parseInvoices()`, gleiche Filterbedingung) und schlägt fehl,
sobald `Leistungsparameter` einen Namen sucht, den kein Formular anbietet.
`DtaFactoryTest$AusDerBlaupause` prüft am anderen Ende, dass die Werte in der
Nachricht ankommen. Wer eine Angabe zur Blaupause hinzufügt, trägt sie in
`BLAUPAUSENFELDER` ein — dann sagt der Test, ob die Kette hält.

Die Blaupausenmaske fragt jetzt sechs Felder ab, und alle sechs werden gelesen:
Umsatzsteuersatz, Abrechnungscode, Tarifkennzeichen, Abrechnungspositionsnummer,
Durchschnittlicher Einzelbetrag, Zuzahlung pro Position.

**Offen bleibt:** `VORBELEGUNG.einzelbetrag` steht weiter auf 15.000,00. Als
Rückfallebene für eine leere Blaupause ist das ein stiller Fehlbetrag — besser
wäre, eine Blaupause ohne Betrag gar nicht erst versenden zu lassen.

### E. Echter Übermittlungsweg

Der Versand ist dateibasiert. Ein realer Weg gehört hinter
`BillingOfficeTransport`; der übrige Ablauf bleibt unverändert.

Voraussetzungen, die **nicht** durch Programmierung zu beschaffen sind:
Zertifikate einer anerkannten Stelle, Zugangsdaten der Annahmestelle, ein
eigenes Betriebsstätten-IK. Solange die fehlen, ist die simulierte Gegenstelle
das Beste, was geht.

### F. Kleinigkeiten

- **`msg.invalidNumbers` ist praktisch unerreichbar.** PLZ und IK sind laut
  `person-tags.json` `NUMBER`; seit sie eine Höchstlänge tragen, sind sie
  Textfelder, und die Prüfung greift vorher. Der Zweig bleibt, weil sich die
  Feldbeschreibung ändern kann.
- ~~**Die Meldungsecke liegt über der Maske.**~~ **Erledigt am 05.09.2026** —
  und es war keine Kleinigkeit: sie lag oben rechts und damit bei einem
  zweispaltigen Formular über „Nachname" und „Land", also über den Feldern, zu
  deren Berichtigung die Fehlermeldung auffordert. Jetzt unten rechts. Siehe J.
- ~~**„1 von 1 ausgewählt · 1 Termine insgesamt".**~~ **Erledigt am
  06.09.2026.** Einzahl und Mehrzahl getrennt. Der Einwand („eine
  Einzahlfassung je Zahl multipliziert die Kombinationen") galt nur, solange
  man *alle* drei Zahlen beugen will; es ist aber nur eine — und das ist die
  Zeile, an der man vor dem Versand abliest, ob die Zahlen stimmen. Sie sollte
  selbst stimmen.
- ~~**Der Anzeigename einer Person trägt ihre laufende Nummer.**~~ **Erledigt
  am 06.09.2026** über `getPlainName` neben `getDisplayName`. Die Nummer steht
  weiterhin in der Auswahlliste und in der Rückfrage vor dem Löschen — zwei
  Frauen können gleich heißen, und gelöscht wird nur eine. In der Meldung
  danach steht jetzt der Name allein.
- **Checkstyle: 10 Warnungen** (`mvn checkstyle:check`). Die
  Warnungen waren am 05.09.2026 rund vierzig; übrig sind nur noch die, die eine
  Entwurfsentscheidung verlangen und sich nicht mechanisch beheben lassen:

  | Regel | Anzahl | Wo |
  |---|---|---|
  | `ParameterNumber` | 5 | `Patient`, `Person`, `ServiceProvider`, `FieldDefinition` (je 9), `EditFormController` (12) |
  | `CyclomaticComplexity` | 5 | `BetragskonsistenzRegel` (11), `Feldbau` an zwei Stellen (je 12), beide `…FieldPopulator` (je 16) |

  Die vier Konstruktoren mit neun Parametern sind der eigentliche Befund: eine
  Person hat mehr Eigenschaften, als ein Konstruktor tragen sollte. Ein Builder
  oder ein `record` für die Adresse wäre die Antwort — das ist aber ein
  Eingriff in die Entitäten und gehört nicht nebenbei erledigt.

  Die 216 Hinweise sind `INFO`: 145-mal fehlende `@param`/`@return`, 63-mal ein
  Ternär-Operator, 8-mal fehlende Tags am Typ. Das ist eine
  Dokumentationsrückstände, keine Warnung — und die Regel gegen den
  Ternär-Operator ist Geschmackssache: aus `a ? b : c` vier Zeilen zu machen,
  verschlechtert den Text.
- **`-Xlint:all` ist seit dem 05.09.2026 im Bau eingeschaltet** (Eltern-`pom.xml`).
  Vorher war gar kein Lint gesetzt, weshalb javac zu veralteten Aufrufen und
  hängenden Javadoc-Blöcken schwieg. Bewusst **ohne** `-Werror`.
- **CI baut mit JDK 21**, lokal wird JDK 25 genutzt. Bewusst, weil das Projekt
  `release=21` setzt — bei Problemen aber die erste Verdachtsstelle.
- **`.docx` wird nicht automatisch erzeugt.** Nach Änderungen an
  `GKVTransmitter_Dokumentation.md` neu erzeugen, siehe README.

### G. Aussehen und Bedienung

Simons Sammlung vom 05.09.2026. Einzeln klein, zusammen der Unterschied zwischen
„läuft" und „fertig".

**Stand: vollständig erledigt (05.09.2026).** Wie es aussieht, steht unter
„Die Vorschau" weiter unten — **jeder dieser Punkte wurde am gezeichneten Bild
geprüft, nicht am Quelltext.**

1. ~~Die Bildlaufleisten sehen aus wie von woanders.~~ **Erledigt.** `gkv.css`
   regelte `.scroll-pane` mit zwei Zeilen und die Leiste selbst gar nicht; jetzt
   sind `.scroll-bar`, `.thumb` und `.track` gestaltet, die Pfeilknöpfe sind auf
   Null gesetzt (nicht nur unsichtbar — sonst blieben zwölf leere Pixel je
   Ende), und der Griff färbt sich erst beim Überfahren.

   **Inzwischen am Bild bestätigt.** Die Leiste steht in `teilnehmer.png`: die
   Testdaten ergeben mehr Zeilen, als bei 720 Punkten Höhe hineinpassen. Der
   Umweg über die Fensterhöhe war überflüssig — genug Inhalt tut es auch.

2. ~~**Die Kopfzeile des Fensters gestalten.**~~ **Entschieden und erledigt,
   aber anders als gefragt.**

   **Gegen `StageStyle.UNDECORATED.`** Wer die Titelleiste selbst zeichnet, baut
   Verschieben, Größenänderung, Maximieren und Schließen von Hand nach und
   verliert dabei das Andocken am Bildschirmrand. Für eine Anwendung, in der
   jemand einen Monat lang arbeitet, ist das ein schlechter Tausch: man gewänne
   Aussehen und verlöre Bedienung. Die Begründung steht bei
   `Hauptfenster.fenstertitel`, damit sie beim nächsten Anlauf nicht neu geführt
   werden muss.

   Was sich **ohne** diesen Tausch verbessern ließ, ist umgesetzt:

   - In der Titelleiste stand immer nur „GKVTransmitter". In der Taskleiste und
     beim Umschalten zwischen Fenstern sagte das nichts darüber, wo man ist.
     Jetzt steht der offene Bereich davor — gebunden, nicht gesetzt.
   - Die Kopfzeile über der Maske trug nur das Wort, das daneben in der
     Seitenleiste ohnehin hervorgehoben steht. Darunter steht jetzt ein Satz,
     was der Bereich ist („Die Frauen, für die abgerechnet wird — mit Anschrift
     und dem IK ihrer Krankenkasse."). Die Texte liegen als `intro.*` in
     `ui-messages.json`; fehlt einer, bleibt die Zeile weg und nimmt keinen
     Platz.

   **Bei der Retrospektive zu entscheiden**, ob das reicht oder ob die eigene
   Titelleiste doch gewünscht ist. Der Aufwand dafür ist ungefähr ein Tag.

3. ~~Ein Programmsymbol fehlt vollständig.~~ **Erledigt.** Sieben PNG von 16
   bis 256 Pixel unter `resources/symbol/`, dazu eine `symbol.ico` für
   `jpackage` (`--icon` steht in `gkv-ui/pom.xml`). `Programmsymbol.alle()`
   hängt sie ans Fenster; `ProgrammsymbolTest` hält fest, dass sie mitkommen
   und die versprochene Kantenlänge haben.

   **Sie sind gezeichnet, nicht gefunden**: `SymbolErzeugen` unter
   `src/test/java` erzeugt sie mit denselben Farben wie `gkv.css`. Wer das
   Symbol ändern will, ändert dort und lässt es neu laufen.

   Der erste Entwurf — Haken auf einem Blatt mit Zeilen — sah bei 128 Pixel gut
   aus und war bei 16 ein grauer Fleck. Ein Symbol muss in der Taskleiste
   lesbar sein, nicht in der Vergrößerung: **ein** Zeichen und sonst nichts. Die
   Andeutung eines Belegs erscheint erst ab 48 Pixel, hinter dem Haken.

4. ~~Lange Texte werden zu `xxxxx…` gekürzt.~~ **Erledigt**, und es war mehr,
   als von außen zu sehen war. Gefunden hat es die Vorschau, nicht ein Test:

   | Wo | Was dastand | Was jetzt geschieht |
   |---|---|---|
   | Blaupausenliste | `Bearbei…` und `Lösc…` | Schaltflächen schrumpfen nicht mehr |
   | Blaupausenliste | `Preis je Ter…`, `05.09.2…` | jede Spalte hält ihre Breite |
   | Blaupausenliste | `Rückbildungskurs nach Geburten, Einzelabrechn…` | nur noch der Kursname |
   | Seitenleiste | `Geburtsvorbereitungsk…` | Kursname, voller Name als Kurzhinweis, Umbruch statt Kürzung |
   | Statuszeile | die ganze Dateiliste | siehe Punkt 6 |

   **Eine abgeschnittene Schaltfläche ist kein Schönheitsfehler.** „Lösc…"
   neben „Bearbei…" ist ein Bedienfehler in Wartestellung: die beiden sind auf
   einen Blick nicht mehr zu unterscheiden, und eine davon löscht.

   Die Ursache lag in `Listenbau`: nur die erste Spalte hatte eine Vorgabe.
   Wurde die Summe aller Spalten breiter als die Liste, schrumpften **alle
   zugleich** — bis hin zu den Schaltflächen. Jetzt bekommt die erste Spalte den
   freien Platz, alle anderen behalten ihre bevorzugte Breite.

5. ~~**Erklärungen hinter ein Info-Zeichen statt unter jedes Feld.**~~
   **Erledigt, als Mittelweg.** Simons Vorschlag und der Einwand dagegen — *was
   hinter einem Zeichen liegt, liest niemand* — sind beide richtig. Umgesetzt
   ist deshalb: **eingeklappt ist die Regel, ausgeklappt die Ausnahme.**

   Offen stehen bleibt die Erklärung dort, wo sich der richtige Wert nicht
   erraten lässt und ein falscher teuer ist: an den beiden IK, an
   Positionsnummer und Tarifkennzeichen, an den beiden Beträgen. Die Liste
   steht als `Feldbau.STETS_ERKLAERT` im Quelltext und nicht in den
   JSON-Dateien — sie ist eine Aussage darüber, was *jemand am Bildschirm*
   braucht, keine über das Datenformat.

   Alles andere — Vorname, Straße, Ort — liegt hinter dem Zeichen, mit dem Text
   zusätzlich als Kurzhinweis daran. **Ohne Erklärung kein Zeichen**; eines,
   hinter dem nichts liegt, ist eine Falle (dieselbe Regel wie in der
   Statuszeile). Der Platz daneben bleibt trotzdem frei, sonst wären die Felder
   mit stehender Erklärung siebzehn Punkte breiter als die übrigen — am
   gezeichneten Bild sofort zu sehen gewesen.

   Das Teilnehmerformular ist dadurch von neun Erklärungszeilen auf zwei
   geschrumpft und passt ohne Bildlauf ins Fenster.

   **Bei der Retrospektive zu entscheiden:** ob die Grenze richtig liegt. Sie
   ist gesetzt, nicht bewiesen.

6. ~~Die Statuszeile zeigt Dateinamen.~~ **Erledigt.** Dort stand:

   ```
   GKVTransmitter geladen - Profile: 2 (SLGA, SLLA) | Invoices: 2
   (antenatal_class_single.json [SLGA, SLLA], postnatal_class_single.json [SLGA, SLLA])
   ```

   Jetzt steht dort „**2 Vorlagen geladen**" und daneben ein Info-Zeichen, das
   die **Anzeigenamen** in der Meldungsecke zeigt. Ohne Vorlagen verschwindet
   das Zeichen — ein Zeichen, hinter dem nichts liegt, ist eine Falle.

   Gebaut wird der Text jetzt in `View`, nicht mehr in `App`: er gehört dorthin,
   wo auch die übrigen Texte liegen. Drei Tests in `HauptfensterTest`.

   Beim Nachsehen am Bild fiel auf, dass das Zeichen ein **grauer Fleck ohne
   erkennbares i** war. Die Ursache ist lehrreich: `.info-zeichen` stand im
   Stylesheet **vor** `.button`, und JavaFX entscheidet bei gleicher Spezifität
   nach der Reihenfolge — die spätere Regel gewinnt. Aus dem Kreis wurde ein
   abgerundetes Viereck, und das Innenmaß `8px 16px` quetschte das i in einem
   17 Pixel breiten Knopf auf null. Der Block steht jetzt nach `.button`, mit
   einem Vermerk, dass er dort bleiben muss.

#### Die zwei Werkzeuge, die keine Tests sind

`Vorschau` **zeichnet**, `Bedienung` **bedient**. Beide liegen unter
`src/test/java`, beide laufen nur von Hand, und beide haben an ihrem ersten Tag
Fehler gefunden, die kein Test gefunden hätte:

| Werkzeug | findet | Beispiel |
|---|---|---|
| `Vorschau` | was falsch **aussieht** | „Lösc…" neben „Bearbei…", das unsichtbare Info-Zeichen |
| `Bedienung` | was falsch **passiert** | acht Termine wurden zu einem, das Bearbeiten schrieb nichts |

Der gemeinsame Nenner: **beide gehen den Weg, den jemand am Bildschirm geht.**
Ein Test, der sich sein Feld selbst zusammensteckt oder einen Wert am Editor
vorbei setzt, prüft einen Weg, den niemand nimmt.

#### Die Vorschau — und was sie am ersten Tag gefunden hat

`Vorschau` unter `gkv-ui/src/test/java/.../presentation/` zeichnet die
Oberfläche in PNG-Dateien, ohne ein Fenster zu öffnen. **Kein Test:** Tests
sagen, ob etwas *funktioniert*; ob es *aussieht* wie gedacht, sagt kein Test.

```bash
mvn -q -pl gkv-ui exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=de.gkvtransmitter.presentation.Vorschau \
    -Dexec.args="target/vorschau"
```

Am ersten Tag fand sie vier Dinge, von denen keines in einem Test stand:

1. Die abgeschnittenen Schaltflächen (Punkt 4).
2. Das unsichtbare Info-Zeichen (Punkt 6).
3. **Die Felder im Personenformular standen in willkürlicher Reihenfolge**:
   „Land, IK, Straße, Geburtsdatum, PLZ" neben „Vorname, Hausnummer,
   Kassen-IK, Nachname" — Vor- und Nachname durch zwei fremde Felder getrennt.
   `person-tags.json` war immer richtig sortiert; `TagConfigLoader` las sie in
   eine **`HashMap`**. Jetzt `LinkedHashMap`.
4. **PLZ und IK waren immer noch Zähler mit Pfeilchen** und einer
   vorbelegten `0`. Simons Einwand dagegen war im September behoben worden — aber
   nur für das Blaupausenformular. Dort steht die Höchstlänge in den
   Segmentdefinitionen, und `Feldbau` macht aus allem über zwei Stellen ein
   Textfeld. `person-tags.json` führte für `plz`, `ik` und `kassenIk`
   **gar keine** Höchstlänge, also blieb `grenze = 0`, und daraus wurde ein
   Zähler. Jetzt stehen dort 5 beziehungsweise 9 — das nimmt die Pfeilchen weg
   *und* begrenzt die Eingabe.

**Merksatz daraus:** eine Regel, die von einer Angabe in einer Datei abhängt,
gilt nur dort, wo die Angabe auch gepflegt ist. Wer `Feldbau` ändert, muss
**beide** Quellen ansehen — `segments/*.json` und `tags/person-tags.json`.

**Und ein zweiter, vom selben Abend:** wer die Hülle eines Feldes ändert, ändert
sie für alle. Das Info-Zeichen aus G5 schob eine `HBox` zwischen Hülle und
Bedienelement — und legte damit das Bearbeiten still, weil
`EntityFieldPopulator` sich sein Bedienelement selbst suchte. Deshalb ist
`Feldbau.bedienelement` jetzt **statisch und die einzige Stelle**, an der der
Weg zum Bedienelement steht.

Beim Nachziehen der Tests kam noch ein echter Fehler heraus: `Feldbau` hatte die
Anzeige der Beanstandung **zweimal** — einmal für den Fokuswechsel, einmal für
den Abruf beim Speichern. Nur die erste merkte sich, dass beanstandet wurde. Wer
also auf Speichern drückte, eine Beanstandung bekam und das Feld berichtigte,
**sah die rote Zeile stehenbleiben**, bis er das Feld verließ. Jetzt gibt es die
Logik einmal.

### H. Ein Bedienwerkzeug für die Entwicklung

**Erledigt am 05.09.2026.** Simons Idee, und sie war gut — sie hat noch am
selben Abend zwei Fehler gefunden, die kein Test finden konnte.

`Bedienung` unter `gkv-ui/src/test/java/.../presentation/` führt eine Folge von
Befehlen auf der laufenden Anwendung aus:

```
oeffne Teilnehmer            einen Bereich aus der Seitenleiste
klick person-neu             eine Schaltfläche über ihre Kennung
setze person-feld-firstname Anna
setze person-feld-birthDate 1990-04-17     ein Datum als JJJJ-MM-TT
lies person-feld-plz         schreibt den Wert des Feldes
zeige                        alles, was auf dem Bildschirm steht
erwarte Anna Berger          irgendwo auf dem Bildschirm
erwarte-meldung gespeichert  in der Meldungsecke
erwarte-nicht Fehler         nirgends auf dem Bildschirm
bild schritt3                ein PNG ins Zielverzeichnis
#                            eine Zeile Anmerkung
```

```bash
mvn -q -pl gkv-ui exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=de.gkvtransmitter.presentation.Bedienung \
    -Dexec.args="gkv-ui/src/test/resources/ablaeufe/ein-monat.txt target/bedienung"
```

**Warum es unter `src/test/java` liegt.** Die Übergabe schlug einen Schalter
vor, etwa `-Dgkv.dev=true`. Ein Schalter ist aber nur eine Zusage. Dieser
Quelltext ist im ausgelieferten Paket **gar nicht vorhanden** — und der beste
Weg, sicherzustellen, dass eine Anwendung mit echten Forderungen an
Krankenkassen nicht fernsteuerbar ist, ist, die Fernsteuerung nicht
mitzuliefern.

**`zeige` ist der wichtigste Befehl.** Er schreibt allen sichtbaren Text mitsamt
den Kennungen. Wer die Anwendung nicht vor sich hat, sieht damit, was
*dasteht* — und nicht, was dem Quelltext nach dastehen müsste. Ein Bild sagt
mehr, lässt sich aber nicht durchsuchen. Umgekehrt gilt: **eine gerade erst
gezeigte Meldung fehlt auf dem Bild.** Die Meldungsecke blendet ihre Karten
ein, und die Überblendung braucht einen Zeichentakt, den es innerhalb eines
Befehlsblocks nicht gibt.

Zwei Abläufe liegen unter `gkv-ui/src/test/resources/ablaeufe/`:

| Datei | wozu |
|---|---|
| `ein-monat.txt` | fährt `AblaufTest`, gegen eine leere Datenbank |
| `durchlauf-mit-bildern.txt` | der Durchlauf aus `Information/Durchlauf.md`, mit elf PNG |

**`AblaufTest` ist der Test, der bisher fehlte.** Jeder andere Test in `gkv-ui`
prüft *eine* Maske für sich, mit einem Ersatzspeicher darunter und einem
Ersatzrahmen darum. Hier läuft die echte Anwendung: echter `Controller`, echte
SQLite-Datenbank, echte Masken, echte Meldungsecke. Er tritt **nicht** an die
Stelle der Maskentests — die sagen genau, was kaputt ist; er sagt nur, dass
irgendwo etwas klemmt. Er tritt an die Stelle der Tests, die es *nicht* gab:
der über mehrere Masken hinweg. Wird er rot und kein anderer, liegt der Fehler
**zwischen** zwei Masken — und genau dort hatte dieses Projekt schon dreimal
einen.

Er benutzt bewusst **kein** `@TempDir`: die Anwendung hält ihre
SQLite-Verbindung offen, solange sie läuft, und Windows lässt eine offene Datei
nicht löschen — JUnit machte daraus einen roten Test bei grünem Ablauf.

#### Voraussetzung, die dabei entstanden ist

Die Felder des Blaupausenformulars trugen **überhaupt keine Kennung**, und ihre
Namen kommen aus den Segmentdefinitionen und enthalten Leerzeichen
(„Durchschnittlicher Einzelbetrag"). Ein `lookup("#…")` liest ein Leerzeichen
als Trennung zwischen zwei Bedingungen und findet nie etwas.

Die Normalisierung dafür lag in `Hauptfenster` und heißt jetzt `Kennungen`.
Betroffen sind beide Stellen, an denen eine Kennung aus einem *Text* entsteht
statt aus einer Konstanten: die Seitenleiste und das Blaupausenformular.
**Was nicht adressierbar ist, lässt sich nicht steuern — und auch nicht
prüfen.**

### I. Heißt das Projekt richtig?

**Erledigt am 06.09.2026. Das Programm heißt jetzt „GKV-Abrechnung".**

Simons Einwand traf zu: „Transmitter" beschrieb den einen Schritt, den das
Programm noch gar nicht wirklich kann — der Versand ist dateibasiert, ein
echter Übermittlungsweg fehlt (Abschnitt E). Was es tatsächlich tut, ist
Stammdaten verwalten, Kurse und Preise festhalten, daraus DTA erzeugen und
**prüfen**.

**Geändert:**

| Was | Wo |
|---|---|
| Anzeigename | `View.PROGRAMMNAME` — Fenstertitel und Seitenleiste hängen daran |
| Paketname der Auslieferung | `gkv-ui/pom.xml`, `--name` und `--vendor`; das Ergebnis heißt `GKV-Abrechnung.exe` |
| Projektnamen | die drei `<name>` in den POM |
| Datenpfad | `Anwendungsverzeichnis` — **mit Umzug**, siehe unten |
| Dokumentation | README, `Vision.md`, `GKVTransmitter_Dokumentation.md` (+ `.docx` neu erzeugt), Architektur-Skill |

**Bewusst nicht geändert**, weil nach außen unsichtbar und ein breiter Eingriff
ohne Gewinn für den, der damit arbeitet: der Java-Paketname
`de.gkvtransmitter`, der Projektordner `GKVTransmitter/` und die Dateinamen der
Dokumentation und der Diagramme. Wer das später doch will, hat einen rein
mechanischen Umbau vor sich — die IDE kann ihn.

#### Der Umzug des Datenordners

Das war der einzige Teil mit Risiko, und er ist der Grund, warum diese
Umbenennung nicht nur Suchen und Ersetzen war. Eine Umbenennung **ohne** Umzug
hätte die bestehende Datenbank unter `%LOCALAPPDATA%\GKVTransmitter` liegen
lassen, und die Anwendung wäre mit leeren Listen aufgegangen. Für jemanden mit
einem Jahr Stammdaten ist das von Datenverlust nicht zu unterscheiden — und die
erste Reaktion wäre, alles noch einmal einzugeben.

`Anwendungsverzeichnis.umgezogen(neu, alt)` benennt den alten Ordner beim
ersten Start um. Nur dann, wenn es den neuen noch nicht gibt und den alten
schon; sonst würde ein zweiter Stand einen bestehenden überschreiben.

**Scheitert der Umzug, wird weiter am alten Ort gearbeitet.** Das ist die
entscheidende Eigenschaft: die Datei kann gesperrt sein, weil noch eine zweite
Programmfassung läuft, oder die Berechtigungen können fehlen. Mit einem leeren
neuen Ordner aufzugehen wäre in beiden Fällen das Schlimmste. Vier Tests halten
das fest, darunter der Fall des gescheiterten Umzugs.

### J. Der Zwischenstand, den Simon sich ansieht

**Das ist das Ziel, auf das alles Übrige zuläuft.** Simon: ein Stand, der
„komplett läuft ohne mögliche Fehler", danach eine Retrospektive, bei der er
selbst herumprobiert und seine Meinung sagt.

Der Maßstab dabei ist ausdrücklich: **fast ein fertiges Produkt.** Nicht
„die Tests sind grün", sondern: jemand setzt sich davor, arbeitet einen Monat
durch und stößt an keine Stelle, an der es klemmt, unverständlich wird oder
etwas Falsches zulässt.

**Stand am 06.09.2026: vollständig.**

| Punkt | Stand |
|---|---|
| 1. Abschnitt **G** vollständig | ✔ |
| 2. Die Feldfragen aus **B 1–4**, soweit ohne Anlage 3 | ✔ (B 1, 2, 3) |
| 3. **Ein Durchlauf von Hand, aufgeschrieben** | ✔ `Information/Durchlauf.md` |
| 4. Die Populatoren aus **C** prüfen | ✔ und ein Fehler gefunden |
| 5. Das Paket bauen **und starten** | ✔ 06.09.2026 |
| 6. Die Umbenennung aus **I** | ✔ 06.09.2026 |
| 7. Das Fenster klein ziehen | ✔ als zweite, kleinere Szene |

#### Was der 06.09.2026 noch gefunden hat

Punkt 7 war als „einmal hinsehen" gedacht und brachte **drei echte Fehler**.
Nachstellen ließ er sich nicht von Hand — ein Schnappschuss nimmt die Größe der
Szene, und die ändert sich nicht, wenn man dem ungezeigten Fenster nachträglich
eine andere Höhe gibt. Eine **zweite, kleinere Szene** tut es aber; sie braucht
eine eigene `View`, weil ein Knoten in genau einer Szene hängt.

1. **Eine `StackPane` zentriert ihre Kinder.** Wurde der Rahmen größer als das
   Fenster, verlor man oben **und** unten gleichzeitig: bei 760×500 fehlte der
   Programmname in der Ecke, während unten „Testdaten anlegen" abgeschnitten
   war. Jetzt oben links verankert — der Überhang läuft nach rechts unten, wo
   ihn die Bildlaufleisten auffangen.
2. **Die Seitenleiste wächst mit jedem Kurs** und passte bei 600 Punkten Höhe
   schon nicht mehr — der damaligen *Startgröße* der Anwendung. Die
   umbrechenden Beschriftungen wurden auf eine Zeile gequetscht und gekürzt
   („Rückbildungskurs nach …"). Sie rollt jetzt, wenn sie muss.
3. **Bei 900×600 lag „Löschen" außerhalb des Fensters.** Erreichbar war es, die
   Ansicht rollt quer — aber wer eine Anwendung startet, soll nicht erst rollen
   müssen, um zu sehen, was sie kann.

Das Fenster geht deshalb jetzt mit **1180×760** auf und lässt sich nicht
kleiner als **900×600** ziehen. Die Grenze nennt die Anwendung, durchsetzen tut
sie das Betriebssystem — eine Zeile in `App`.

**Merksatz:** eine Startgröße veraltet still. Sie stand seit einer Zeit auf
900×600, in der die Oberfläche aus einer Menüleiste bestand; inzwischen hat sie
eine Seitenleiste von 224 Punkten und Listen mit vier Spalten und zwei
Schaltflächen je Zeile.

#### Das Paket

`mvn -Ppaket clean package` läuft durch, das Ergebnis startet, und die
Titelleiste sagt „Teilnehmer – GKV-Abrechnung". Geprüft wurde das durch
Aufzählen der Fenster, nicht über `MainWindowTitle` des gestarteten Prozesses —
der ist der jpackage-Starter und hat selbst kein Fenster. **Eine leere Anzeige
dort heißt nicht, dass der Titel leer ist.**

Der Fenstertitel hat außerdem einen eigenen Test bekommen. Die Bindung in `App`
war richtig, aber ungeprüft: ginge sie ins Leere, bliebe die Titelleiste
schlicht leer, und das sähe man erst im gebauten Paket.

#### Was für die Retrospektive offen bleibt

1. **Die Info-Zeichen.** Die Grenze zwischen „steht offen da" und „liegt hinter
   dem Zeichen" ist gesetzt, nicht bewiesen.
2. **Die eigene Titelleiste** (G 2). Entschieden ist dagegen, mit Begründung —
   die Entscheidung lässt sich umdrehen, sie kostet etwa einen Tag.
3. **Der Branch liegt noch lokal.** Siehe „Sofort zu entscheiden": `git push`,
   dann Pull Request oder Merge nach `main`.

**Was bewusst offen bleiben darf:** alles, wofür Anlage 3, der Vertrag oder die
Datenannahmestelle nötig sind (Tarifkennzeichen, Positionsnummern, echter
Versand). Das ist keine Lücke im Programm, sondern eine in den Unterlagen — und
es gehört bei der Retrospektive gesagt statt kaschiert.

## Fallstricke, die schon Zeit gekostet haben

**Nach jeder Änderung an `gkv-core` muss `mvn install -DskipTests` laufen**,
bevor `gkv-ui` gestartet wird. `mvn test` allein installiert nichts. Sonst
läuft die Anwendung gegen ein altes `gkv-core`-Jar aus `~/.m2` — und man sucht
den Fehler im Quelltext, der dort längst behoben ist.

**`mvn test` ohne `clean` kann grün lügen.** Der Übersetzer arbeitet
inkrementell; nach dem Entfernen einer Methode meldete er einmal
`BUILD SUCCESS`, obwohl ein Test sie noch aufrief. Vor jedem Urteil über den
Stand: `mvn clean test`.

**Ältere Dateien haben CRLF-Zeilenenden.** Ein mehrzeiliges `perl -0pi -e`
findet dort nichts, obwohl das Muster stimmt. Entweder auf `\r?\n` prüfen oder
das Bearbeitungswerkzeug nehmen.

**Oberflächentests brauchen eine laufende JavaFX-Umgebung.** `JavaFxLaufzeit`
fährt sie hoch und führt Code auf ihrem Faden aus. Ausnahmen aus dem Faden
werden zurückgereicht — ohne das gälte ein fehlgeschlagener Test als bestanden.

**Im Bauknecht braucht JavaFX eine Anzeige.** Der Workflow ruft deshalb
`xvfb-run -a mvn test`.

**Eine Maske liefert ihren nackten Bereich, nicht das `ScrollPane`.** Dessen
Inhalt hängt erst nach dem Aufbau der Darstellung im Knotenbaum; ein `lookup`
auf die Bedienelemente liefe vorher ins Leere. Das Einhüllen macht
`Hauptfenster`. Aus demselben Grund gibt es `Hauptfenster.gezeigterInhalt()`.

**Es gibt kein TestFX, und das ist Absicht.** Dessen Wert liegt im Nachstellen
echter Eingaben. Seit die Meldungen hinter `Meldungen` liegen, blockiert
nichts mehr. Das erspart Monocle, das jeder JavaFX-Fassung hinterherhinkt.
Für den einen Fall, in dem das Nachstellen wirklich zählt, gibt es seit dem
05.09.2026 `Bedienung` — siehe Abschnitt H.

**Ein beschreibbarer `Spinner` übernimmt getippten Text nur bei der
Eingabetaste.** Wer eine Zahl eintippt und dann auf eine Schaltfläche klickt,
für den liefert `getValue()` weiterhin den alten Wert — nichts schlägt fehl,
die Zahl steht sichtbar im Feld und wird trotzdem nicht verwendet. Deshalb
`commitValue()` bei jedem Auslesen und beim Fokusverlust. **Ein Test, der den
Wert über `getValueFactory().setValue(...)` setzt, geht am Editor vorbei und
findet das nie.**

**Keine SQLite-Datenbank in einem `@TempDir`.** Die Anwendung hält ihre
Verbindung offen, solange sie läuft — und sie läuft über das Testende hinaus,
weil die JavaFX-Laufzeit für alle Tests *einmal* hochgefahren wird. Windows
lässt die offene Datei nicht löschen, und JUnit macht daraus einen roten Test
bei grünem Ablauf. Ein Verzeichnis unter `target` nehmen.

**Ein relativer Datenbankpfad landet unter `%LOCALAPPDATA%`**, nicht neben den
Bildern — `Anwendungsverzeichnis` löst ihn auf. Ein `rm -rf target/vorschau`
trifft ihn deshalb nicht, und die Testdaten häufen sich von Lauf zu Lauf.

**Eine `StackPane` zentriert ihre Kinder.** Wird ein Kind größer als die
Fläche — weil seine Mindestgröße das erzwingt —, ragt es auf *beiden* Seiten
hinaus, und man verliert oben und unten gleichzeitig. Wer eine Überlagerung
baut, verankert den Untergrund mit `StackPane.setAlignment(…, Pos.TOP_LEFT)`.

**`mvn clean` scheitert, solange das gebaute Paket noch läuft.** Windows gibt
die `.exe` nicht frei; die Fehlermeldung nennt „Failed to delete", nicht „ist
in Benutzung". Erst den Prozess beenden, im Zweifel
`rm -rf gkv-ui/target/paket`.

**`MainWindowTitle` des gestarteten Prozesses ist leer** — das heißt *nicht*,
dass die Titelleiste leer ist. Der `jpackage`-Starter hat selbst kein Fenster.
Wer den Titel prüfen will, zählt die Fenster auf:

```powershell
Get-Process | Where-Object { $_.MainWindowTitle -like "*Abrechnung*" }
```

**In PowerShell 5.1 kein `2>&1` auf native Programme.** `java -version`
schreibt auf stderr; die Umleitung erzeugt einen `NativeCommandError`, obwohl
der Aufruf erfolgreich war.

**`jpackage` baut nur für das System, auf dem es läuft.**

**SQLite legt keine Verzeichnisse an.** Siehe
`DatabaseSettings.sicherstelleVerzeichnis()`.

**Neue Objekte über `persist` speichern, nicht `merge`.** Nur `persist` trägt
die vergebene ID in das übergebene Objekt ein.

**IK in Testdaten brauchen eine gültige Prüfziffer** — jetzt erst recht, weil
die Oberfläche sie prüft. Verwendbar: `108310400`, `104940005`, `102137985`,
`101560000`.

## Nützliche Befehle

```bash
mvn clean test                       # alle 344 Tests
mvn clean test -pl gkv-ui            # nur die 187 Oberflächentests
mvn install -DskipTests              # Kern bereitstellen (siehe Fallstricke)
mvn -Ppaket clean package            # eigenständiges Windows-Paket
mvn -Pdebug -pl gkv-ui javafx:run    # mit Debug-Anschluss auf Port 5005
mvn checkstyle:check                 # 10 Warnungen, nicht blockierend
```

Die beiden Werkzeuge, die keine Tests sind — sie zeigen, was jemand *sieht*:

```bash
# Die Oberfläche in PNG zeichnen, ohne ein Fenster zu öffnen
mvn -q -pl gkv-ui exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=de.gkvtransmitter.presentation.Vorschau \
    -Dexec.args="target/vorschau"

# Die Anwendung über eine Befehlsfolge bedienen (Abschnitt H)
mvn -q -pl gkv-ui exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=de.gkvtransmitter.presentation.Bedienung \
    -Dexec.args="gkv-ui/src/test/resources/ablaeufe/durchlauf-mit-bildern.txt target/durchlauf"
```

```bash
cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

In VS Code: `F5` → „Starten mit Testdaten (eigene Datenbank)".
