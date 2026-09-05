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
- **255 Tests**, davon 108 in `gkv-ui`, `BUILD SUCCESS`
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

1. **Weitere Vorlage: „Rückbildungskurs nach Geburten".** Anzulegen als eigene
   Datei unter `gkv-core/src/main/resources/invoices/`, nach dem Muster von
   `antenatal_class_single.json`, und in `JsonParserFactory.INVOICE_FILES`
   einzutragen. Fachlich zu klären ist, ob sich Abrechnungscode,
   Positionsnummer oder Tarifkennzeichen vom Geburtsvorbereitungskurs
   unterscheiden — das steht in Anlage 3 beziehungsweise im Vertrag und ist
   nicht zu raten.
2. **Auswahl und Suche von Personen in einer Gruppe verbessern.** Die
   `GruppenMaske` zeigt Teilnehmer und Dienstleister heute als eine ungefilterte
   Liste von Kontrollkästchen. Bei mehr als einer Handvoll Personen wird das
   unübersichtlich; ein Suchfeld wie in den Übersichten fehlt, und man sieht
   nicht auf einen Blick, wer schon angehakt ist. `Listenbau` bringt Suche und
   Zeilenaufbau bereits mit.

**Erledigt am 05.09.2026:** die Blaupausenmaske. Sie liegt jetzt als
`BlaupausenMaske` neben den anderen (Übersicht mit Suche, Bearbeiten und
Löschen), `View` ist von 531 auf rund 300 Zeilen geschrumpft, und
`DataRepository` hat endlich ein `deleteBlueprint`.

### C. `EditFormController` hat keine Tests

Die knapp 300 Zeilen tragen das gesamte **Bearbeiten** von Personen. Die
Personenmaske prüft nur, dass das Formular erscheint — was darin geschieht,
ist ungeprüft. Das ist jetzt die größte ungedeckte Stelle.

Der Weg dahin ist frei: die Klasse bekommt alles über den Konstruktor,
inzwischen auch `Meldungen`, und die Ersatzstücke liegen unter
`gkv-ui/src/test/`.

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
  | `CyclomaticComplexity` | 5 | `BetragskonsistenzRegel`, `Feldbau`, beide `…FieldPopulator`, `View` |

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
mvn clean test                       # alle 232 Tests
mvn clean test -pl gkv-ui            # nur die 94 Oberflächentests
mvn install -DskipTests              # Kern bereitstellen (siehe Fallstricke)
mvn -Ppaket clean package            # eigenständiges Windows-Paket
mvn -Pdebug -pl gkv-ui javafx:run    # mit Debug-Anschluss auf Port 5005
mvn checkstyle:check                 # 55 Hinweise, nicht blockierend

cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

In VS Code: `F5` → „Starten mit Testdaten (eigene Datenbank)".
