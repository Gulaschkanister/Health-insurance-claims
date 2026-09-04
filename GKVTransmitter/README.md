# GKVTransmitter

Abrechnung medizinischer Leistungen mit gesetzlichen Krankenkassen: Daten
erfassen, prüfen, in das DTA-Format umwandeln und an die zuständige Kasse
übergeben.

## Voraussetzungen

**Zum Benutzen:** nichts. Die ausgelieferte Anwendung bringt ihre eigene
Java-Laufzeit mit (siehe [Auslieferung](#auslieferung)).

**Zum Entwickeln:** JDK 21 oder neuer und Maven 3.9+. Auf diesem
Entwicklungsrechner installiert sind Temurin JDK 25 (LTS) unter
`C:\Program Files\Eclipse Adoptium` und Maven 3.9.16 unter
`C:\Tools\apache-maven-3.9.16`, beide im PATH.

Zusätzlich für die Dokumentation: PlantUML (`C:\Tools\plantuml\plantuml.jar`)
zum Rendern der Diagramme und Pandoc für die Word-Fassung.

## Aufbau

Das Projekt besteht aus zwei Maven-Modulen:

| Modul | Inhalt |
|---|---|
| `gkv-core` | Domäne, DTA-Erzeugung, Validierung, Versand, Persistenz — **ohne JavaFX** |
| `gkv-ui` | JavaFX-Oberfläche |

Die Trennung ist im Build durchgesetzt: eine Enforcer-Regel in `gkv-core`
lässt den Bau fehlschlagen, sobald JavaFX in den Kern gelangt. Nur so bleibt
die Fachlichkeit ohne laufende Oberfläche testbar.

## Bauen und starten

```bash
mvn clean test                        # übersetzen und alle Tests
mvn clean package                     # Pakete bauen
mvn install -DskipTests               # Kern ins lokale Repository legen
mvn -pl gkv-ui javafx:run             # Anwendung starten
```

`-pl gkv-ui` baut `gkv-core` **nicht** mit. Vor dem ersten Start und nach jeder
Änderung am Kern muss deshalb `mvn install -DskipTests` gelaufen sein — sonst
bricht der Start mit „Could not find artifact de.gkv:gkv-core" ab.

## Debuggen

Die Anwendung mit Debug-Anschluss starten:

```bash
mvn install -DskipTests
mvn -Pdebug -pl gkv-ui javafx:run
```

Die JVM wartet dann auf Port 5005, bis sich ein Debugger verbunden hat. Das
Warten ist Absicht: sonst wären Bootstrap, Laden der JSON-Profile und
Datenbankaufbau längst durch, bevor sich ein Haltepunkt setzen lässt. Soll die
Anwendung sofort loslaufen, stattdessen `-Pdebug-nowait` verwenden.

Anschließend in VS Code die Konfiguration **„An laufende Anwendung anhaengen
(Port 5005)"** starten.

> Zum Debuggen in VS Code wird die Erweiterung *Extension Pack for Java*
> benötigt. Sie ist auf diesem Rechner nicht installiert, ebenso wenig ein JDK
> unter Windows. Die Konfiguration *„GKVTransmitter starten"* setzt beides
> voraus; die Anhäng-Variante kommt damit aus, dass die Anwendung in WSL läuft.

Einzelnen Test debuggen:

```bash
mvn -Dmaven.surefire.debug -Dtest=DtaValidationServiceTest test -pl gkv-core
```

Auch hier wartet die JVM auf Port 5005.

Statische Prüfung (meldet, blockiert nicht):

```bash
mvn checkstyle:check
mvn pmd:check
```

## Auslieferung

```bash
mvn -Ppaket clean package
```

Ergebnis: `gkv-ui/target/paket/GKVTransmitter/` — ein Ordner mit `GKVTransmitter.exe`
und mitgelieferter Java-Laufzeit, rund 166 MB. Auf dem Zielrechner muss **nichts**
installiert sein, auch kein Java. Kopieren, Doppelklick, fertig.

`jpackage` erzeugt immer für das System, auf dem es läuft — ein Windows-Paket
entsteht also nur unter Windows.

## Wo die Daten liegen

Alle Daten liegen im Profil des angemeldeten Benutzers, **nicht** im
Arbeitsverzeichnis:

| Betriebssystem | Ort |
|---|---|
| Windows | `%LOCALAPPDATA%\GKVTransmitter` |
| macOS | `~/Library/Application Support/GKVTransmitter` |
| Linux | `$XDG_DATA_HOME/gkvtransmitter`, sonst `~/.local/share/gkvtransmitter` |

Darunter `database.db` sowie `dta_output/` mit den erzeugten und zugestellten
Dateien. Für eine Sicherung genügt eine Kopie dieses Ordners.

Der Ablageort hängt am Benutzer und nicht daran, von wo gestartet wurde — sonst
läge die Datenbank je nach Startart auf dem Desktop oder in `C:\Program Files`,
wo sich gar nicht schreiben lässt.

## Konfiguration

| Systemeigenschaft | Umgebungsvariable | Standard | Bedeutung |
|---|---|---|---|
| `gkv.home` | `GKV_HOME` | Benutzerprofil | Ablageort aller Daten |
| `gkv.db.path` | `GKV_DB_PATH` | `database.db` im Ablageort | Ort der SQLite-Datei |
| `gkv.db.schema` | `GKV_DB_SCHEMA` | `update` | Umgang mit dem Schema |
| `gkv.testdaten` | – | `false` | Testdaten beim Start anlegen |

> `gkv.db.schema=create` **verwirft sämtliche gespeicherten Daten.** Der
> Standard `update` ergänzt fehlende Tabellen und Spalten und lässt Vorhandenes
> stehen.

Beispiel für einen Lauf gegen eine getrennte Datenbank mit Testdaten:

```bash
mvn -pl gkv-ui javafx:run -Dgkv.db.path=/tmp/probe.db -Dgkv.testdaten=true
```

Die Versandziele stehen in
`gkv-core/src/main/resources/billing-office-endpoints.json`. Eine weitere Kasse
wird dort eingetragen; am Code ist nichts zu ändern.

## Ablauf einer Abrechnung

```
Stammdaten + Blaupause + Terminanzahl
   ↓  AbrechnungService
Abrechnung
   ↓  DtaFactory
DTA-Nachricht (SLGA + SLLA)
   ↓  DtaValidationService     ← Prüfung; ein Fehler hält den gesamten Lauf auf
   ↓  BillingOfficeTransport
Zustellung an die Kasse nach Kassen-IK
```

Geprüft wird, bevor irgendetwas das Haus verlässt. Erst werden alle Nachrichten
erzeugt und geprüft; nur wenn keine beanstandet wird, geht eine hinaus.
Andernfalls wäre bei einem Fehler in der Mitte eines Laufs bereits ein Teil bei
der Kasse und müsste dort einzeln storniert werden.

`DtaDispatchService.pruefe(abrechnungen)` prüft, ohne zu versenden.

### Erproben ohne echte Gegenstelle

`SimulierterKassenTransport` umschließt einen beliebigen Transport und lässt
eine simulierte Kasse antworten. Neben jeder zugestellten Datei liegt danach
ein `*.antwort.txt` mit einer Rückmeldung in der Form einer Kassenantwort —
einschließlich der Unterscheidung zwischen Syntaxfehler und fachlicher
Zurückweisung.

## Prüfregeln

| Regel | Prüft |
|---|---|
| `SegmentSyntaxRegel` | Abschlusszeichen, Bezeichnerform, bekannte Segmente |
| `NachrichtenRahmenRegel` | UNB/UNZ, Nachrichtenanzahl, Datenaustauschreferenz |
| `NachrichtenAbschlussRegel` | UNH/UNT, Segmentanzahl je Nachricht |
| `InstitutionskennzeichenRegel` | Länge und Prüfziffer der IK |
| `VersichertenangabenRegel` | Name, Geburtsdatum, Versichertennummer |
| `BetragskonsistenzRegel` | ENF gegen BES, BES gegen GES |

Nur Fehler halten den Versand auf, Warnungen nicht. Neue Regeln setzen
`ValidationRule` um und kommen über `DtaValidationService` dazu.

`Information/Valide.DTA` ist die Messlatte: die Datei muss unbeanstandet
durchlaufen.

## Verzeichnisse

```
GKVTransmitter/
├── pom.xml                    Elternprojekt
├── build/checkstyle.xml       Regelsatz der statischen Prüfung
├── gkv-core/                  Fachlicher Kern
├── gkv-ui/                    JavaFX-Oberfläche
└── Information/               Fachdokumentation
    ├── GKVTransmitter_Dokumentation.md    Gesamtdokumentation (Quelle)
    ├── GKVTransmitter_Dokumentation.docx  daraus erzeugte Word-Fassung
    ├── Vision.md              Ziel und Architektur
    ├── *.puml / *.png         UML-Diagramme mit gerenderten Bildern
    ├── Abrechnung_Checkliste_kurz.md
    ├── codes/                 Codelisten des Verfahrens
    ├── Valide.DTA             gültige Referenznachricht
    └── Anlage_*.pdf           verbindliche Vorgaben
```

## Dokumentation aktualisieren

Die Diagramme werden aus den `.puml`-Quellen gerendert, die Word-Fassung aus der
Markdown-Quelle erzeugt — beide sind **abgeleitet** und nicht von Hand zu
bearbeiten:

```bash
cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

## Agentische Unterstützung

Unter `.claude/skills/` liegen fünf Skills mit dem Fachwissen des Projekts:

| Skill | Thema |
|---|---|
| `dta-format` | Segmente, Felder, Zähler, Formate |
| `gkv-abrechnung` | Fachdomäne, IK-Rollen, Pflichtangaben, Abweisungsgründe |
| `gkvtransmitter-architektur` | Modulaufbau, Schichtenregeln, wo was hingehört |
| `dta-validierung` | Prüfregeln schreiben und testen |
| `kassen-versand` | Transporte, Endpunkte, Rückmeldungen |
