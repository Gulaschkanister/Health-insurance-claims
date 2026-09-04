# Nächste Schritte

Stand: 4. September 2026, Branch `feature/kern-architektur` (14 Commits vor `main`).

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
- 138 Tests, `BUILD SUCCESS`, Anwendung startet und legt Testdaten an
- Fünf Skills unter `.claude/skills/`, Dokumentation und Diagramme aktuell

Der Branch liegt auf `origin`; der jeweils letzte Commit kann noch fehlen
(`git status` zeigt es als „ahead“).

## Sofort zu entscheiden

### 1. Branch nach `main` bringen

Der Branch ist fertig und grün. Zur Wahl stehen:

- **Pull Request** über GitHub — vorher `git push`, damit der Remote-Stand
  vollständig ist
- **Merge nach `main`** — `git switch main && git merge --no-ff feature/kern-architektur`

Zu beachten: Die drei Commits `632e906`, `475321d`, `6460b78` stammen aus
`origin/refactor` und liegen bereits auf dem Remote. Meine 14 Commits tragen
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

### A. `View.java` zerlegen — 1438 Zeilen

Der größte verbliebene Posten und mit Abstand die größte Klasse des Projekts
(zweitgrößte: `JsonParserFactory` mit 530).

Die Fachlogik ist bereits heraus (`AbrechnungService`, `DtaDispatchService`),
die Klasse selbst aber unangetastet. Sie macht heute mindestens fünf Dinge:
Menüaufbau, Formularerzeugung, Abrechnungsmaske, Personen- und Gruppenpflege,
Dialoge.

Vorschlag für den Schnitt — je eine Klasse unter `presentation/`:

| Neue Klasse | Übernimmt aus `View` |
|---|---|
| `AbrechnungsMaske` | `createAbrechnung`, `zeigePruefbericht` |
| `PersonenMaske` | `createPerson`, `editPatient`, `deletePatient`, `savePerson`, … |
| `GruppenMaske` | `createGroup`, `editGroup`, `deleteGroup`, `showGroupForm`, … |
| `FormularMaske` | `createFormular`, `collectVisibleFieldValues`, Code-Dropdowns |
| `Dialoge` | `showInfoDialog`, `showErrorDialog`, `selectEntity` |

`View` bleibt als Rahmen: Hauptszene, Menüleiste, Verteilung auf die Masken.

**Vorgehen:** eine Maske nach der anderen, nach jedem Schritt `mvn test`. Nicht
alles auf einmal — die Klasse hat keine Testabdeckung, die einen Fehler auffangen
würde. Siehe dazu Punkt B.

### B. Tests für `gkv-ui`

`gkv-ui` hat **null Tests**. Alle 138 liegen in `gkv-core`.

Nötig wäre TestFX (`org.testfx:testfx-junit5`), das JavaFX-Oberflächen im
Headless-Betrieb bedienen kann. Sinnvolle erste Fälle:

- Abrechnungsmaske: Auswahl ohne Teilnehmer → Hinweis statt Absturz
- Prüfbericht: fehlerhafte Abrechnung → Dialog mit den Beanstandungen
- Personenmaske: Speichern mit unvollständigen Pflichtfeldern

Das sollte **vor** der Zerlegung aus Punkt A entstehen, zumindest für die
Abrechnungsmaske — sonst ist der Umbau ungesichert.

### C. Zwei Validierungswelten zusammenführen

Es gibt zwei Dinge namens Validierung, die nichts miteinander zu tun haben:

- `util.FieldValidator` mit verschachteltem `ValidationResult` — prüft
  Formulareingaben in der Oberfläche
- `validator.*` mit `ValidationReport` — prüft die erzeugte DTA-Nachricht

Beide sind für sich richtig, aber die Namensgleichheit führt in die Irre. Ein
Umbenennen von `FieldValidator.ValidationResult` zu etwa `Feldpruefung` würde
das entschärfen. Kein dringender Fehler, aber eine Stolperstelle.

### D. Fachliche Lücken

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

### E. Echter Übermittlungsweg

Der Versand ist dateibasiert. Ein realer Weg gehört hinter
`BillingOfficeTransport`; der übrige Ablauf bleibt unverändert.

Voraussetzungen, die **nicht** durch Programmierung zu beschaffen sind:

- Zertifikate einer anerkannten Stelle
- Zugangsdaten der Annahmestelle
- ein eigenes Betriebsstätten-IK

Solange die fehlen, ist die simulierte Gegenstelle das Beste, was geht.

### F. Kleinigkeiten

- **69 Checkstyle-Hinweise** (`mvn checkstyle:check`), nicht blockierend.
  Lohnt sich am ehesten während der Zerlegung aus Punkt A abzuarbeiten.
- **Fünf `TODO`-Kommentare** im Code, überwiegend in `View` und
  `JsonParserFactory`.
- **CI baut mit JDK 21**, lokal wird JDK 25 genutzt. Bewusst, weil das Projekt
  `release=21` setzt — bei Problemen aber die erste Verdachtsstelle.
- **`.docx` wird nicht automatisch erzeugt.** Nach Änderungen an
  `GKVTransmitter_Dokumentation.md` neu erzeugen, siehe README.

## Fallstricke, die schon Zeit gekostet haben

**Nach jeder Änderung an `gkv-core` muss `mvn install -DskipTests` laufen**,
bevor `gkv-ui` gestartet wird. `mvn test` allein installiert nichts. Sonst
läuft die Anwendung gegen ein altes `gkv-core`-Jar aus `~/.m2` — und man sucht
den Fehler im Quelltext, der dort längst behoben ist. Genau das ist passiert.

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
mvn clean test                       # alle 138 Tests
mvn install -DskipTests              # Kern bereitstellen (siehe Fallstricke)
mvn -Ppaket clean package            # eigenständiges Windows-Paket
mvn -Pdebug -pl gkv-ui javafx:run    # mit Debug-Anschluss auf Port 5005
mvn checkstyle:check                 # 69 Hinweise, nicht blockierend

cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

In VS Code: `F5` → „Starten mit Testdaten (eigene Datenbank)".
