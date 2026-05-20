# GKVTransmitter

Eine JavaFX-Desktopanwendung zur Erstellung und Verwaltung von GKV-Abrechnungsdateien im EDIFACT-DTA-Format (§ 302 SGB V).

## Überblick

GKVTransmitter unterstützt Leistungserbringer im Gesundheitswesen dabei, Abrechnungsdaten gegenüber gesetzlichen Krankenkassen elektronisch aufzubereiten. Die Anwendung generiert strukturierte DTA-Dateien (Datenträgeraustausch) auf Basis der EDIFACT-Nachrichtentypen **SLLA** (Einzelleistungsdaten) und **SLGA** (Sammelrechnungsdaten) gemäß Anlage 1/3 zu § 302 SGB V.

Unterstützte EDIFACT-Segmente:

| Segment | Bedeutung |
|---------|-----------|
| `UNB` / `UNZ` | Interchange-Kopf und -Ende |
| `UNH` / `UNT` | Nachrichten-Kopf und -Ende |
| `FKT` | Funktionskennzeichen |
| `REC` | Rechnungsdaten |
| `INV` | Versicherungsdaten |
| `NAD` | Name und Adresse |
| `ENF` | Einzelleistungsnachweis |
| `BES` | Betragssumme je Fall |
| `GES` | Gesamtbetrag |
| `UST` / `NAM` | Umsatzsteuerkennzeichen / Namensdaten |

## Voraussetzungen

- **JDK 21** oder höher
- **Maven 3.9+**

## Schnellstart

```bash
# Anwendung starten
mvn clean javafx:run

# Ausführbares JAR erzeugen
mvn clean package
```

## Projektstruktur

```
GKVTransmitter/
├── src/main/java/de/gkvtransmitter/
│   ├── App.java                        # JavaFX-Einstiegspunkt
│   ├── Main.java                       # Launcher
│   ├── bootstrap/                      # Startinitialisierung
│   ├── definition/                     # Globale Laufzeitdefinitionen (GlobalDefinitions, InvoiceType)
│   ├── entity/                         # Hibernate-Entitäten (Patient, ServiceProvider)
│   ├── factory/                        # Factory-Interface
│   ├── hibernate/                      # SQLite-Datenbankzugriff (HibernateSqllite)
│   ├── model/                          # Domänenmodell (Invoice, Segment, FieldValue, …)
│   ├── parser/                         # JSON-Parser für Profile und Segmentdefinitionen
│   ├── presentation/                   # UI-Schicht (Controller, View, Builder, Populator)
│   ├── util/                           # Hilfsfunktionen (Validierung, Modifikatoren, Tags)
│   └── validator/                      # Validierungsregeln und -ergebnisse
└── src/main/resources/
    ├── segments/                       # Segmentdefinitionen als JSON (enf.json, fkt.json, …)
    ├── profiles/                       # Nachrichtenprofile (slla-profile.json, slga-profile.json)
    ├── invoices/                       # Rechnungsvorlagen (z. B. antenatal_class_single.json)
    ├── codes/                          # Schlüsseldateien (Abrechnungscodes, Rechnungsarten, …)
    ├── tags/                           # UI-Tag-Konfigurationen
    └── hibernate.cfg.xml               # Hibernate-/SQLite-Konfiguration
```

## Architektur

Die Anwendung folgt einer geschichteten Architektur mit klarer Trennung von Präsentation, Domäne, Definition und Persistenz.

```
Presentation  →  Controller  →  ApplicationBootstrap
                     │                   │
              GlobalDefinitions    JsonParserFactory
                     │
           ┌─────────┴─────────┐
         SLLA-Profil       SLGA-Profil
         (Invoice)         (Invoice)
              │
    Segment → FieldValue → FieldDefinition
```

- **GlobalDefinitions** (Singleton): Hält zur Laufzeit alle geladenen Profile und Rechnungsvorlagen.
- **JsonParserFactory**: Liest Segmentdefinitionen, Profile und Rechnungsvorlagen aus JSON-Dateien.
- **FactoryManager**: Verwaltet registrierte Fabriken und liefert die passende Implementierung.
- **Hibernate + SQLite**: Speichert Stammdaten (Patienten, Leistungserbringer) dauerhaft in `database.db`.

## Datenbank

Die Anwendung nutzt **SQLite** mit **Hibernate** für die Datenpersistenz.

- Datenbankdatei: `database.db` im Projektstammverzeichnis
- Schema wird beim Start automatisch erstellt bzw. aktualisiert (`hbm2ddl.auto=update`)
- Die Datei wird bei `mvn clean` **nicht** gelöscht (Maven Clean Plugin konfiguriert)

```bash
# Datenbank-Inhalt inspizieren
sqlite3 database.db
> .tables
> SELECT * FROM Patient;
> .quit

# Backup erstellen
sqlite3 database.db ".dump" > database_backup.sql

# Datenbank zurücksetzen
rm database.db
```

## Code-Qualität

Das Projekt ist mit **PMD** (Static Code Analysis) ausgestattet.

```bash
# PMD-Check ausführen
mvn pmd:check

# HTML-Report generieren (target/pmd.html)
mvn pmd:pmd

# Kompilieren und PMD gemeinsam ausführen
mvn clean compile pmd:check
```

Aktive PMD-Regelkategorien:
- `category/java/bestpractices.xml`
- `category/java/design.xml`

## Abhängigkeiten

| Bibliothek | Version | Zweck |
|-----------|---------|-------|
| OpenJFX | 21.0.2 | Desktop-UI |
| Jackson Databind | 2.18.2 | JSON-Parsing |
| Lombok | 1.18.44 | Boilerplate-Reduktion |
| Hibernate Core | 5.6.0 | ORM / Datenbankzugriff |
| SQLite JDBC | 3.40.1.0 | SQLite-Treiber |

## Weiterführende Informationen

Fachliche Hintergrundinformationen zur Abrechnung nach § 302 SGB V sowie Beispieldateien befinden sich im Verzeichnis [`GKVTransmitter/Information/`](GKVTransmitter/Information/).
