# Nächste Schritte

Stand: 4. September 2026, Branch `feature/kern-architektur`.

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
- **`View` von 1438 auf 610 Zeilen zerlegt** — vier Masken herausgelöst
- **`gkv-ui` hat jetzt 45 Tests**, insgesamt 183, `BUILD SUCCESS`
- Fünf Skills unter `.claude/skills/`, Dokumentation und Diagramme aktuell

Der Branch liegt auf `origin`; die jeweils letzten Commits können noch fehlen
(`git status` zeigt es als „ahead“).

## Was zuletzt geschah

Die Zerlegung von `View` und die Oberflächentests aus Punkt A und B der
vorigen Übergabe sind zum großen Teil abgearbeitet. Der Reihe nach:

| Neue Klasse | Was sie übernimmt |
|---|---|
| `dialog.Dialoge` | Melden, auswählen lassen, nachfragen — als Schnittstelle |
| `dialog.JavaFxDialoge` | die Umsetzung mit `Alert` und `ChoiceDialog` |
| `dialog.Pruefberichttext` | Wortlaut des Prüfberichts, ohne JavaFX |
| `Maskenrahmen` | wo eine Maske erscheint |
| `AbrechnungsMaske` | Zusammenstellen und Anstoßen einer Abrechnung |
| `GruppenMaske` | Gruppen anlegen, bearbeiten, löschen |
| `PersonenMaske` | Teilnehmer und Dienstleister |
| `Feldbau` | Eingabefelder erzeugen und auslesen |
| `Abrechnungslauf` | die eine Methode, die die Maske vom Fachdienst braucht |

Der Schlüssel war `Dialoge`. Solange jede Meldung als `Alert.showAndWait()`
abging, ließ sich kein Ablauf prüfen, der im Fehlerfall etwas anzeigt — und das
sind genau die Abläufe, auf die es ankommt. Erst hinter der Schnittstelle
wurde alles Weitere prüfbar.

Drei Fehler kamen dabei ans Licht und sind behoben:

1. Beim Anlegen eines **Dienstleisters** meldete die Anwendung „Teilnehmer
   erfolgreich erstellt!“. Die Rolle entschied über das Ziel in der Datenbank,
   aber nicht über die Meldung.
2. Schlug das **Speichern fehl**, wurde nach der Fehlermeldung trotzdem
   geräumt — die Eingaben waren weg, obwohl nichts gespeichert war. Jetzt
   bleibt das Formular stehen.
3. Die Mitglieder einer Gruppe landeten in einem `HashSet`, die Reihenfolge
   der Auswahl ging verloren. Jetzt `LinkedHashSet`.

Ebenfalls erledigt: **Punkt C der vorigen Übergabe.**
`FieldValidator.ValidationResult` heißt jetzt `FieldValidator.Feldbefund` und
ist damit nicht mehr mit `validator.ValidationReport` zu verwechseln.

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
`dev` (vollständig in `main` enthalten) und `restart`. Sie stiften nur
Verwirrung.

## Priorisierte nächste Schritte

### A. Die letzte Maske: `createFormular`

`View` ist mit 610 Zeilen die zweitgrößte Klasse des Projekts (größte:
`JsonParserFactory` mit 530). Was übrig ist:

- `createFormular` samt `collectVisibleFieldValues` — die Blaupausenmaske
- `createCodeDropdownForInvoiceField`, `resolveCodeOptionsForField`,
  `normalizeFieldKey`, `loadInvoiceCodeOptions` — die Code-Auswahllisten
- Hauptszene, Menüleiste, `seedTestData`

Vorschlag: eine `BlaupausenMaske` nach dem Muster der drei vorhandenen, und
die Code-Listen als eigene Klasse `Codelisten` daneben — sie sind reine
JSON-Auswertung und ließen sich ohne JavaFX prüfen.

`View` bliebe als Rahmen: Hauptszene, Menüleiste, Verteilung auf die Masken.

**Vorgehen:** wie bei den anderen dreien. Erst die Naht, dann die Maske, dann
die Tests, nach jedem Schritt `mvn test`.

### B. `EditFormController` hat keine Tests

Die 296 Zeilen dahinter tragen das gesamte **Bearbeiten** von Personen. Die
Personenmaske prüft nur, dass das Formular überhaupt erscheint — was darin
geschieht, ist ungeprüft. Das ist jetzt die größte ungedeckte Stelle.

Der Weg dahin ist frei: die Klasse bekommt bereits alles über den Konstruktor,
und `SpeicherRepository`, `AufzeichnendeDialoge` und `AufzeichnenderRahmen`
liegen unter `gkv-ui/src/test/`.

### C. Fachliche Lücken

Nach Nutzen geordnet:

1. **Stornierung und Nachberechnung** — heute gar nicht vorhanden. Sobald real
   abgerechnet wird, wird das gebraucht.
2. **Umsatzsteuersatz** ist mit 19 fest in `DtaFactory` verdrahtet
   (`UMSATZSTEUERSATZ`). Gehört in die Blaupause, analog zu
   `Leistungsparameter`.
3. **Weitere Leistungsbereiche** — abgedeckt ist nur SGS H mit Abrechnungscode
   `61`. Die Struktur dafür steht (`Leistungsparameter`, Segment-JSONs), es
   fehlen die Daten aus Anlage 3.
4. **Positionsnummern und Tarifkennzeichen** werden auf Form, nicht auf
   fachliche Zulässigkeit geprüft. Dafür bräuchte es die Schlüsseltabellen aus
   Anlage 3 als JSON — dann wäre es eine weitere `ValidationRule`.

### D. Echter Übermittlungsweg

Der Versand ist dateibasiert. Ein realer Weg gehört hinter
`BillingOfficeTransport`; der übrige Ablauf bleibt unverändert.

Voraussetzungen, die **nicht** durch Programmierung zu beschaffen sind:

- Zertifikate einer anerkannten Stelle
- Zugangsdaten der Annahmestelle
- ein eigenes Betriebsstätten-IK

Solange die fehlen, ist die simulierte Gegenstelle das Beste, was geht.

### E. Zwei Fundstücke aus der Zerlegung

Beides absichtlich unverändert gelassen, um die Zerlegung nicht mit
Verhaltensänderungen zu vermischen:

- **Die Dienstleister-Kästchen in der Abrechnungsmaske haben keine Wirkung.**
  Sie lassen sich anhaken, aber der Dienstleister wird ohnehin aus der Gruppe
  genommen (`AbrechnungService.findProvider` nimmt den ersten). Entweder die
  Auswahl auswerten oder die Kästchen entfernen — so ist die Oberfläche
  irreführend.
- **`msg.invalidNumbers` ist unerreichbar.** PLZ und IK sind laut
  `person-tags.json` `NUMBER` und damit Zähler, deren `getValue()` immer eine
  Zahl liefert. Die Meldung „PLZ und IK müssen Zahlen sein!“ kann nicht mehr
  erscheinen. Der Zweig in `PersonenMaske.speichere` bleibt vorerst, weil sich
  die Feldbeschreibung ändern kann.

### F. Kleinigkeiten

- **54 Checkstyle-Hinweise** (`mvn checkstyle:check`): 38 in `gkv-core`, 16 in
  `gkv-ui`. Vor der Zerlegung waren es 69; der Rest lässt sich beim nächsten
  Durchgang mitnehmen. Nicht blockierend.
- **`TODO`-Kommentare** stehen noch in `JavaFxUiFactory` und `Feldbau`.
- **CI baut mit JDK 21**, lokal wird JDK 25 genutzt. Bewusst, weil das Projekt
  `release=21` setzt — bei Problemen aber die erste Verdachtsstelle.
- **`.docx` wird nicht automatisch erzeugt.** Nach Änderungen an
  `GKVTransmitter_Dokumentation.md` neu erzeugen, siehe README.

## Fallstricke, die schon Zeit gekostet haben

**Nach jeder Änderung an `gkv-core` muss `mvn install -DskipTests` laufen**,
bevor `gkv-ui` gestartet wird. `mvn test` allein installiert nichts. Sonst
läuft die Anwendung gegen ein altes `gkv-core`-Jar aus `~/.m2` — und man sucht
den Fehler im Quelltext, der dort längst behoben ist. Genau das ist passiert.

**Oberflächentests brauchen eine laufende JavaFX-Umgebung.** `JavaFxLaufzeit`
fährt sie hoch und führt Code auf ihrem Faden aus. Ohne
`JavaFxLaufzeit.aufFxFaden(...)` schlägt jede Änderung an einem Bedienelement
fehl. Ausnahmen aus dem Faden werden zurückgereicht — ohne das gälte ein
fehlgeschlagener Test als bestanden.

**Im Bauknecht braucht JavaFX eine Anzeige.** Der Workflow ruft deshalb
`xvfb-run -a mvn test`. Ohne das scheitert dort jeder Oberflächentest mit
„Unable to open DISPLAY“, obwohl lokal alles grün ist.

**Eine Maske liefert ihren nackten Bereich, nicht das `ScrollPane`.** Dessen
Inhalt hängt erst nach dem Aufbau der Darstellung im Knotenbaum; ein `lookup`
auf die Bedienelemente liefe vorher ins Leere. Das Einhüllen macht
`Maskenrahmen`.

**Es gibt kein TestFX im Projekt, und das ist Absicht.** Dessen Wert liegt im
Nachstellen echter Maus- und Tastatureingaben. Seit die Dialoge hinter
`Dialoge` liegen, blockiert nichts mehr, und die Masken lassen sich unmittelbar
aufbauen und auswerten. Das erspart die Abhängigkeit von Monocle, das jeder
JavaFX-Fassung hinterherhinkt.

**In PowerShell 5.1 kein `2>&1` auf native Programme.** `java -version` schreibt
auf stderr; die Umleitung erzeugt einen `NativeCommandError`, obwohl der Aufruf
erfolgreich war.

**`jpackage` baut nur für das System, auf dem es läuft.** Ein Windows-Paket
entsteht nur unter Windows, nicht aus WSL heraus.

**SQLite legt keine Verzeichnisse an.** Wer einen neuen Pfad einführt, muss das
Verzeichnis selbst anlegen — siehe `DatabaseSettings.sicherstelleVerzeichnis()`.

**Neue Objekte über `persist` speichern, nicht `merge`.** Nur `persist` trägt
die vergebene ID in das übergebene Objekt ein. `merge` legt eine Kopie an und
lässt das Original mit ID 0 zurück, was beim Verknüpfen sofort auffällt.

**IK in Testdaten brauchen eine gültige Prüfziffer**, sonst weist die
Validierung zu Recht ab. Verwendbar: `108310400`, `104940005`, `102137985`,
`101560000`.

## Nützliche Befehle

```bash
mvn clean test                       # alle 183 Tests
mvn test -pl gkv-ui                  # nur die 45 Oberflächentests
mvn install -DskipTests              # Kern bereitstellen (siehe Fallstricke)
mvn -Ppaket clean package            # eigenständiges Windows-Paket
mvn -Pdebug -pl gkv-ui javafx:run    # mit Debug-Anschluss auf Port 5005
mvn checkstyle:check                 # 54 Hinweise, nicht blockierend

cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

In VS Code: `F5` → „Starten mit Testdaten (eigene Datenbank)".
