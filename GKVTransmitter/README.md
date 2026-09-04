# GKVTransmitter

Abrechnung medizinischer Leistungen mit gesetzlichen Krankenkassen: Daten
erfassen, prüfen, in das DTA-Format umwandeln und an die zuständige Kasse
übergeben.

## Voraussetzungen

- JDK 21
- Maven 3.9+

> **Hinweis für diesen Entwicklungsrechner:** Unter Windows sind weder `java`
> noch `mvn` installiert. Die Toolchain liegt in WSL:
>
> ```bash
> wsl -e bash -lc "cd /mnt/c/Projekte/gkv/Health-insurance-claims/GKVTransmitter && mvn -B test"
> ```

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
mvn clean test           # übersetzen und alle Tests
mvn clean package        # Pakete bauen
mvn -pl gkv-ui javafx:run   # Anwendung starten
```

Statische Prüfung (meldet, blockiert nicht):

```bash
mvn checkstyle:check
mvn pmd:check
```

## Konfiguration

| Systemeigenschaft | Umgebungsvariable | Standard | Bedeutung |
|---|---|---|---|
| `gkv.db.path` | `GKV_DB_PATH` | `database.db` | Ort der SQLite-Datei |
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
    ├── Vision.md              Ziel und Architektur
    ├── Abrechnung_Checkliste_kurz.md
    ├── codes/                 Codelisten des Verfahrens
    ├── Valide.DTA             gültige Referenznachricht
    └── Anlage_*.pdf           verbindliche Vorgaben
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
