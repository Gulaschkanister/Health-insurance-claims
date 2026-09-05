# Nächste Schritte

Stand: 5. September 2026, Branch `feature/kern-architektur`.

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
- **296 Tests**, davon 144 in `gkv-ui`, `BUILD SUCCESS`, Checkstyle 10 Warnungen
- Fünf Skills unter `.claude/skills/`, Dokumentation und Diagramme aktuell

**Der lokale Stand ist 21 Commits vor `origin`.** Vor dem nächsten Umbau einmal
`git push` — sonst hängt viel unveröffentlichte Arbeit an einem Rechner.

## Was zuletzt geschah (05.09.2026, letzte Sitzung)

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

1. **Die Meldung zum IK ist irreführend.** Simon: „IK der Krankenkasse sagt
   9 Ziffern, jedoch geht `123456789` nicht."

   **Das Programm hat recht, die Meldung hat unrecht.** Nachgerechnet: für
   `12345678…` lautet die richtige Prüfziffer `0`, also ist `123456780` gültig
   und `123456789` nicht. Die Regel ist in Ordnung.

   Der Fehler liegt in dem, was dasteht. Hilfetext („Neunstelliges
   Institutionskennzeichen. Die letzte Ziffer ist die Prüfziffer.") und Meldung
   („Die Prüfziffer stimmt nicht. Ein IK hat neun Ziffern.") sagen beide **nicht,
   dass man sich kein IK ausdenken kann**. Wer eine plausible Zahl eintippt,
   steht vor einer Ablehnung ohne Ausweg — es gibt keinen Weg, von neun frei
   gewählten Ziffern zu einem gültigen IK zu kommen, außer die Prüfziffer
   auszurechnen.

   Zu tun: Meldung umschreiben („Ein IK wird von der Datenannahmestelle
   vergeben und lässt sich nicht frei wählen; die letzte Ziffer muss zu den
   übrigen passen"). **Zusätzlich anbieten, die richtige Prüfziffer zu nennen** —
   `Institutionskennzeichen.berechnePruefziffer` kann das längst, es wird nur
   nirgends angezeigt: „Zu 12345678 gehört die Prüfziffer 0." Damit wird aus
   einer Sackgasse ein Hinweis. Gilt für beide IK-Felder.

2. **Der Umsatzsteuersatz soll Vorschläge machen** (19 %, 7 %, 0), so wie es
   `NUMBER_SUGGESTION` einmal vorsah.

   Der Befund dazu ist bemerkenswert: **die Vorschlagsmechanik ist genau
   verkehrt herum eingesetzt.** `Feldbau` macht aus `NUMBER_SUGGESTION` und
   `CODE` ein Auswahlfeld, sobald `Vorlagen.auswahlFuer(feldname)` etwas
   liefert — aber `View` liefert nur für `Abrechnungscode` etwas, und dort gibt
   es genau einen Wert (siehe Punkt 3). Für den Umsatzsteuersatz, wo drei
   Vorschläge hilfreich wären, liefert sie nichts, und `ust.json` steht ohnehin
   auf `PERCENT` mit `maxLength: 3`, wird also zum Textfeld.

   Zu tun: `auswahlFuer("Umsatzsteuersatz")` gibt `19`, `7`, `0` zurück, und das
   Auswahlfeld wird **beschreibbar** (`ComboBox.setEditable(true)`) — ein
   Vorschlag darf nichts ausschließen, falls ein anderer Satz gilt.

3. **Der Abrechnungscode braucht kein Auswahlfeld.** Simon hat den Verdacht
   geäußert, und er stimmt: `codes/abrechnungscodes.json` enthält **einen
   einzigen Eintrag**, `61`. Ein Aufklappmenü mit einer Zeile ist Bedienlast
   ohne Nutzen.

   Die eigentliche Frage ist die dahinter, und die ist offen: **ist der
   Abrechnungscode je Vorlage fest?** Wenn ja — und dafür spricht alles, was
   hier belegt ist, denn `61` gehört zum Leistungsbereich SGS H, den beide Kurse
   teilen — dann gehört er **gar nicht ins Formular**, sondern als
   `internal`-Feld in die Segmentdefinition, so wie es mit Anzahl und
   Leistungsdatum schon geschehen ist. Das wäre ein Feld weniger, das jemand
   ausfüllen muss und falsch ausfüllen kann. Vor dem Umbau ist zu klären, ob ein
   künftiger Leistungsbereich einen anderen Code brächte; dann bliebe er im
   Formular, aber mit einer Liste, die diesen Namen verdient.

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

Was in `gkv-ui` **weiterhin ohne Test** ist, nach Nutzen geordnet:

| Klasse | Zeilen | Warum das zählt |
|---|---|---|
| `EntityFieldPopulator` + die zwei Populatoren | 194 + 89 + 90 | Sie übersetzen zwischen Eingabefeld und Entität, **in beide Richtungen**. Ein Fehler dort schreibt einen Wert in das falsche Feld, ohne dass etwas fehlschlägt — dieselbe Art Fehler wie beim Einzelbetrag. `EditFormControllerTest` benutzt bewusst einen eigenen, einfachen Populator und prüft die echten deshalb *nicht* mit. |
| `View` | 292 | Der Einstieg: Navigation, Vorlagen, Abrechnungscodes, Testdaten. Sein Aufbau lässt sich prüfen, seit die Masken heraus sind. |
| `Controller` | 75 | Lädt Profile und Vorlagen beim Start. Schlägt das fehl, startet die Anwendung nicht. |
| `Maskenkopf`, `Abrechnungslauf` | 45 + 32 | Klein und unauffällig; ein Test wäre schnell geschrieben. |
| `Bildschirmmeldungen` | 88 | Die einzige Umsetzung von `Meldungen`, die wirklich etwas anzeigt. `Benachrichtigungen` ist geprüft, dieser Weg dahin nicht. |
| `JavaFxUiFactory` | 210 | Reine Fabrik ohne eigene Entscheidungen. Am ehesten verzichtbar. |

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
  `person-tags.json` `NUMBER` und damit Zähler, deren `getValue()` immer eine
  Zahl liefert. Der Zweig bleibt, weil sich die Feldbeschreibung ändern kann.
- **Die Meldungsecke liegt über der Maske.** Bei schmalem Fenster kann eine
  stehende Fehlermeldung ein Eingabefeld verdecken. Sie lässt sich wegklicken;
  falls es stört, wäre die Maske in der Breite zu begrenzen.
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

1. **Die Bildlaufleisten sehen aus wie von woanders.** `gkv.css` regelt
   `.scroll-pane` mit zwei Zeilen (durchsichtiger Hintergrund und Rahmen) und
   die Leiste selbst **gar nicht** — `.scroll-bar`, `.thumb`, `.track` und die
   Pfeilknöpfe stehen unberührt auf dem JavaFX-Standard und passen zu nichts.
   Zu tun: mit den Farbnamen aus dem Stylesheet gestalten, schmaler, ohne
   Pfeilknöpfe.

2. **Die Kopfzeile des Fensters gestalten.** Die Titelleiste ist heute die von
   Windows. Wer sie selbst zeichnen will, braucht `StageStyle.UNDECORATED` und
   muss Verschieben, Größenänderung, Maximieren und Schließen selbst bauen —
   das ist mehr Arbeit, als es aussieht, und man verliert das
   Fenster-Andocken. Erst entscheiden, ob es das wert ist; die schmalere
   Alternative ist, die Kopfzeile *innerhalb* der Anwendung (`Hauptfenster`)
   aufzuwerten und die Titelleiste zu lassen.

3. **Ein Programmsymbol fehlt vollständig.** Nachgesehen: kein
   `stage.getIcons()`, keine Bilddatei unter `resources/`, kein `--icon` in der
   `jpackage`-Konfiguration. In der Taskleiste steht deshalb das
   Java-Standardsymbol. Zu tun: ein `.png` für `stage.getIcons()` (mehrere
   Größen, 16/32/48/256) **und** ein `.ico` für `jpackage` — beides wird
   gebraucht, das eine im laufenden Fenster, das andere am Paket.

4. **Lange Texte werden zu `xxxxx…` gekürzt.** JavaFX kürzt Beschriftungen
   voreingestellt mit Auslassungspunkten. Der auffälligste Fall ist die
   Statuszeile (Punkt 6) — dort steht der längste Text im Programm. Zu tun:
   dort, wo Text vollständig lesbar sein muss, `setWrapText(true)` oder ein
   Tooltip mit dem ganzen Text; wo gekürzt wird, `OverrunStyle.LEADING_ELLIPSIS`
   erwägen, wenn das Ende aussagekräftiger ist als der Anfang. **Nicht überall
   umbrechen** — in Listenzeilen zerstörte das die Ausrichtung.

5. **Erklärungen hinter ein Info-Zeichen statt unter jedes Feld.** Simons
   Vorschlag. Dafür spricht viel: die Formulare sind lang, und die Erklärung
   wird nur beim ersten Mal gebraucht. Dagegen spricht eines: **was hinter einem
   Zeichen liegt, liest niemand.** Ein Mittelweg wäre, die Erklärung
   einzuklappen und beim ersten Öffnen eines Formulars ausgeklappt zu zeigen —
   oder sie nur an den Feldern stehen zu lassen, wo sie vor einem Fehler
   bewahrt (IK, Beträge), und den Rest hinter das Zeichen zu legen. Die
   Beanstandung unter dem Feld bleibt in jedem Fall sichtbar; sie ist keine
   Erklärung, sondern eine Antwort.

6. **Die Statuszeile zeigt Dateinamen.** Sie liest heute:

   ```
   GKVTransmitter geladen - Profile: 2 (SLGA, SLLA) | Invoices: 2
   (antenatal_class_single.json [SLGA, SLLA], postnatal_class_single.json [SLGA, SLLA])
   ```

   Der Kommentar an der Stelle in `App` nennt sie selbst „sichtbare Debug-Hilfe
   im UI" — sie wurde nie ersetzt. Mit der zweiten Vorlage ist sie noch länger
   geworden und wird jetzt gekürzt (Punkt 4). Dazu steht „Invoices" und
   „Profile" englisch in einer sonst deutschen Oberfläche.

   Zu tun: „**2 Vorlagen geladen**", und die Namen — die **Anzeigenamen**, nicht
   die Dateinamen — hinter ein Info-Zeichen. Der Dateiname interessiert
   niemanden, der abrechnet.

### H. Ein Bedienwerkzeug für die Entwicklung

Simons Idee, und sie ist gut: eine Möglichkeit, die Anwendung **von außen zu
bedienen** — über eine Folge von Befehlen mit Kennungen und Werten —, damit sich
Abläufe durchspielen lassen, ohne dass jemand klickt.

Was das brächte:

- Ein Ablauf ließe sich **als Ganzes** prüfen: Person anlegen, Gruppe bilden,
  Blaupause wählen, abrechnen. Heute prüft jeder Test eine Maske für sich; dass
  die vier zusammenpassen, prüft nichts.
- Ich könnte die Anwendung selbst bedienen und **sehen, was Simon sieht**,
  statt es aus dem Quelltext zu erschließen.
- Die Bedienelemente tragen bereits feste Kennungen (`PersonenMaske.ID_SPEICHERN`
  und so fort) — genau das, was ein solches Werkzeug ansteuern müsste. Die
  Vorarbeit ist also getan.

Entwurfsgedanken, ehe damit angefangen wird:

- Es tritt **nicht an die Stelle** der Maskentests. Die sind schnell und sagen
  genau, was kaputt ist; ein Ablaufskript sagt nur, dass irgendwo etwas klemmt.
  Es tritt an die Stelle der Tests, die es *nicht* gibt — der über mehrere
  Masken hinweg.
- Es gehört hinter einen Schalter wie `-Dgkv.testdaten=true` und **darf im
  ausgelieferten Paket nichts tun**. Eine Anwendung, die echte Forderungen an
  Krankenkassen stellt, sollte nicht fernsteuerbar sein.
- Die einfachste tragfähige Form: eine Textdatei mit Zeilen wie
  `klick person-speichern` / `setze person-feld-firstname Anna` /
  `erwarte meldung msg.patientCreated`, ausgeführt auf dem JavaFX-Faden.
  Kein Netzwerk, keine Fremdbibliothek.

### I. Heißt das Projekt richtig?

Simons Frage. Sie ist berechtigt: **„Transmitter" beschreibt den einen Schritt,
den das Programm noch gar nicht wirklich kann.** Der Versand ist dateibasiert,
ein echter Übermittlungsweg fehlt (Abschnitt E). Was es tatsächlich tut, ist
Stammdaten verwalten, Kurse und Preise festhalten, daraus DTA erzeugen und
**prüfen** — und die Prüfung ist der Teil, der den Nutzen stiftet.

Zu bedenken, ehe umbenannt wird:

- Der Name steckt an vielen Stellen: Paketname `de.gkvtransmitter`,
  Verzeichnis `GKVTransmitter/`, `%LOCALAPPDATA%\GKVTransmitter`, Fenstertitel,
  `jpackage`-Ausgabe, Dokumentation, Skills. Ein Umbenennen ist ein
  mechanischer, aber breiter Eingriff.
- **Der Datenpfad ist der heikle Teil**: eine Umbenennung ließe eine bestehende
  Datenbank unter dem alten Pfad zurück. Wer umbenennt, muss den alten Ort
  weiter lesen oder umziehen.
- „GKV" ist gut und sollte bleiben — es sagt, worum es geht.

Ein Vorschlag zur Entscheidung, kein Beschluss: der **Anzeigename** (Fenster,
Symbol, Paket) lässt sich sofort ändern, ohne dass Paketnamen oder Datenpfad
folgen müssen. Damit wäre der sichtbare Teil richtig benannt und der teure Teil
aufgeschoben.

### J. Der Zwischenstand, den Simon sich ansieht

**Das ist das Ziel, auf das alles Übrige zuläuft.** Simon: ein Stand, der
„komplett läuft ohne mögliche Fehler", danach eine Retrospektive, bei der er
selbst herumprobiert und seine Meinung sagt.

Der Maßstab dabei ist ausdrücklich: **fast ein fertiges Produkt.** Nicht
„die Tests sind grün", sondern: jemand setzt sich davor, arbeitet einen Monat
durch und stößt an keine Stelle, an der es klemmt, unverständlich wird oder
etwas Falsches zulässt.

Zu erledigen, ehe der Stand vorgelegt wird:

1. Abschnitt **G** vollständig — das Aussehen ist bei einer Retrospektive das
   Erste, was auffällt.
2. Die Feldfragen aus **B 1–4**, soweit sie ohne Anlage 3 zu beantworten sind:
   die IK-Meldung, die Vorschläge zum Umsatzsteuersatz, das überflüssige
   Auswahlfeld beim Abrechnungscode.
3. **Ein Durchlauf von Hand, aufgeschrieben**: Teilnehmerin anlegen, Gruppe
   bilden, Blaupause anlegen, abrechnen, Rückmeldung ansehen — jeder Schritt
   mit dem, was dabei auf dem Bildschirm steht. Was daran hakt, kommt vor der
   Retrospektive weg, nicht danach.
4. Die Populatoren aus **C** prüfen — sie sind der letzte ungeprüfte Weg, auf
   dem ein Wert unbemerkt im falschen Feld landen kann.

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
mvn clean test                       # alle 296 Tests
mvn clean test -pl gkv-ui            # nur die 144 Oberflächentests
mvn install -DskipTests              # Kern bereitstellen (siehe Fallstricke)
mvn -Ppaket clean package            # eigenständiges Windows-Paket
mvn -Pdebug -pl gkv-ui javafx:run    # mit Debug-Anschluss auf Port 5005
mvn checkstyle:check                 # 10 Warnungen, nicht blockierend

cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

In VS Code: `F5` → „Starten mit Testdaten (eigene Datenbank)".
