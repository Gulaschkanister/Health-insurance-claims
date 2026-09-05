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
- **293 Tests**, davon 141 in `gkv-ui`, `BUILD SUCCESS`
- Fünf Skills unter `.claude/skills/`, Dokumentation und Diagramme aktuell

Der Branch liegt auf `origin`; die jeweils letzten Commits können noch fehlen
(`git status` zeigt es als „ahead“).

## Was zuletzt geschah: die Oberfläche

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

1. **Woher kommt das Tarifkennzeichen — und wovon hängt es ab?**
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

**`FormBuilder` (234 Zeilen) braucht keinen Test, sondern eine Entscheidung: er
wird von nichts benutzt.** Kein Aufruf, kein Import, in keinem Modul. `Vision.md`
und `PROGRESS_UPDATE_v3.md` führen ihn weiter als eingesetztes Entwurfsmuster,
zusammen mit einem `MenuBuilder`, den es gar nicht mehr gibt — die Menüleiste
wurde durch die Seitenleiste ersetzt. Entweder löschen (und die beiden Dokumente
nachziehen) oder tatsächlich verwenden; ihn ungenutzt und dokumentiert stehen zu
lassen ist die schlechteste der drei Möglichkeiten.

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
mvn clean test                       # alle 293 Tests
mvn clean test -pl gkv-ui            # nur die 141 Oberflächentests
mvn install -DskipTests              # Kern bereitstellen (siehe Fallstricke)
mvn -Ppaket clean package            # eigenständiges Windows-Paket
mvn -Pdebug -pl gkv-ui javafx:run    # mit Debug-Anschluss auf Port 5005
mvn checkstyle:check                 # 55 Hinweise, nicht blockierend

cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

In VS Code: `F5` → „Starten mit Testdaten (eigene Datenbank)".
