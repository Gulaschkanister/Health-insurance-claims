---
name: gkvtransmitter-architektur
description: Aufbau des GKVTransmitter, Schichtenregeln und wo welche Änderung hingehört. Verwenden vor jeder Änderung am Code — beim Anlegen neuer Klassen, beim Ändern von Persistenz, Versand, Validierung oder Oberfläche, beim Bauen, Testen und Paketieren, sowie bei Fragen zu Modulen, Paketen, Hibernate, SQLite, JavaFX oder zur Auslieferung dieses Projekts.
---

# Aufbau des GKVTransmitter

## Bauen und Testen

Die Toolchain steht unter Windows **und** in WSL zur Verfügung:

```bash
mvn -B test                                      # Windows: JDK 25, Maven 3.9.16
mvn -B test -pl gkv-core                         # nur ein Modul
mvn -B test -pl gkv-core -Dtest=DtaValidationServiceTest
```

Übersetzt wird gegen `maven.compiler.release=21`, gebaut mit JDK 25.

**Für das Auslieferungspaket ist die Windows-Toolchain zwingend.** `jpackage`
erzeugt immer nur für das System, auf dem es läuft — aus WSL entstünde ein
Linux-Paket.

Beim Bauen erscheinen JVM-Warnungen zu `sun.misc.Unsafe` (Lombok) und
`System::load` (SQLite-Treiber). Beide sind harmlos und kein Projektfehler.

Bei PowerShell 5.1: `2>&1` auf ein natives Programm wie `java -version` erzeugt
einen `NativeCommandError`, obwohl der Aufruf erfolgreich war. Nicht als Fehler
missdeuten — die Umleitung einfach weglassen.

## Auslieferung

```bash
mvn -Ppaket clean package
```

Ergebnis: `gkv-ui/target/paket/GKVTransmitter/` — Startprogramm plus
mitgelieferte Java-Laufzeit, rund 166 MB. Auf dem Zielrechner muss nichts
installiert sein, auch kein Java.

**Datenablage:** `Anwendungsverzeichnis` legt sie am Benutzerprofil fest
(`%LOCALAPPDATA%\GKVTransmitter`), nicht am Arbeitsverzeichnis. Wer einen neuen
Pfad einführt, nutzt `Anwendungsverzeichnis.aufloesen(...)` — ein relativer Pfad
läge sonst je nach Startart woanders, bei einer ausgelieferten Anwendung etwa in
`C:\Program Files`, wo sich gar nicht schreiben lässt.

## Die zwei Module

```
GKVTransmitter/
├── pom.xml            Elternprojekt, Abhängigkeitsverwaltung
├── gkv-core/          Fachlichkeit — ohne JavaFX
└── gkv-ui/            JavaFX-Oberfläche
```

**`gkv-core` darf keine Oberflächenabhängigkeit haben.** Das ist keine
Vereinbarung, sondern durchgesetzt: eine `maven-enforcer-plugin`-Regel in
`gkv-core/pom.xml` lässt den Build fehlschlagen, sobald `org.openjfx` in den
Kern gelangt. Wer eine Klasse mit JavaFX-Bezug in `gkv-core` anlegt, bekommt
einen Buildfehler — das ist beabsichtigt und kein Hindernis, das man umgeht.

Der Grund: nur ein oberflächenfreier Kern lässt sich ohne laufende Anwendung
testen. 138 der 183 Tests hängen daran.

## Pakete in gkv-core

| Paket | Inhalt |
|---|---|
| `entity` | Hibernate-Entitäten: `Patient`, `ServiceProvider`, `PersonGroup`, `Blueprint`, `DtaCounter` |
| `model` | Fachobjekte ohne Persistenz: `Abrechnung`, `Invoice`, `DtaMessage`, `model.segment.*` |
| `repository` | `DataRepository` — der **Port** zur Persistenz |
| `hibernate.sqllite` | der **Adapter** dazu: `HibernateSqllite`, `DatabaseSettings`, `SessionFactoryProvider`, `TransactionRunner` |
| `dta` | Erzeugen und Einlesen: `DtaFactory`, `DtaDocument`, `DtaSegment`, `Leistungsparameter` |
| `validator` | Prüfwerk: `DtaValidationService`, `ValidationRule`, `ValidationReport`, `validator.rules.*` |
| `dispatch` | Versand: `DtaDispatchService`, Transporte, Endpunkte, Antwortauswertung |
| `application` | Anwendungsfälle: `AbrechnungService` |
| `parser.json` | `JsonParserFactory` — liest Profile, Segmente, Vorlagen aus JSON |
| `bootstrap`, `definition`, `factory` | Start und Registries |
| `util` | `Institutionskennzeichen`, `FieldValidator`, `TagConfigLoader`, Modifier |

`gkv-ui` enthält nur `App`, `Main`, `presentation.*` und `util.AppMessages`.

## Aufbau von gkv-ui

| Klasse | Aufgabe |
|---|---|
| `View` | Rahmen: Hauptszene, Menüleiste, Verteilung auf die Masken |
| `AbrechnungsMaske` | Abrechnung zusammenstellen und anstoßen |
| `GruppenMaske` | Gruppen anlegen, bearbeiten, löschen |
| `PersonenMaske` | Teilnehmer und Dienstleister |
| `Feldbau` | Eingabefelder erzeugen und auslesen |
| `Maskenrahmen` | wo eine Maske erscheint |
| `dialog.Dialoge` | melden, auswählen lassen, nachfragen |

Eine Maske bekommt alles über den Konstruktor: Daten, Meldungen, Rahmen. Sie
liefert aus ihrer Aufbaumethode einen nackten `Region` — **kein `ScrollPane`**.
Dessen Inhalt hängt erst nach dem Aufbau der Darstellung im Knotenbaum, ein
`lookup` auf die Bedienelemente liefe vorher ins Leere. Das Einhüllen macht
`Maskenrahmen`.

Bedienelemente, die ein Test erreichen muss, tragen eine feste Kennung als
Konstante der Maske (`AbrechnungsMaske.ID_START` und so fort).

## Wo eine Änderung hingehört

| Vorhaben | Ort |
|---|---|
| Neue Prüfregel für DTA | `validator/rules/`, siehe Skill `dta-validierung` |
| Neuer Versandweg (SMTP, HTTP) | `dispatch/`, `BillingOfficeTransport` umsetzen |
| Neues Feld an einer Person | `entity/Person.java`; Schema wächst über `hbm2ddl=update` mit |
| Änderung an der DTA-Erzeugung | `dta/DtaFactory.java` — danach `DtaFactoryValidierungTest` laufen lassen |
| Neuer Wert aus der Blaupause | `dta/Leistungsparameter.java` |
| Segment- oder Feldregeln | JSON unter `gkv-core/src/main/resources/segments/` |
| Neue Kasse als Ziel | `gkv-core/src/main/resources/billing-office-endpoints.json` |
| Etwas an der Oberfläche | die zuständige Maske unter `gkv-ui/presentation/` |
| Eine neue Maske | eigene Klasse nach dem Muster von `GruppenMaske`, nicht in `View` |
| Eine neue Art Eingabefeld | `presentation/Feldbau.java` |
| Eine neue Art Meldung | `presentation/dialog/Dialoge.java` und die beiden Umsetzungen |

**Fachlogik gehört nie in eine Maske.** Sie kommt nach `application/` oder in
den zuständigen Dienst und wird von der Oberfläche nur aufgerufen.

**Und nichts Neues in `View`.** Die Klasse war einmal 1.438 Zeilen lang und
ist auf 610 zurückgebaut; sie ist der Rahmen, nicht der Ort für neue Masken.
Eine neue Maske wird eine eigene Klasse nach dem Muster von `GruppenMaske`.

## Persistenz

Die Datenbank ist SQLite über Hibernate 6.6 mit `jakarta.persistence`.

```java
try (HibernateSqllite repo = HibernateSqllite.open()) {
    repo.savePatient(patient);
}
```

`open()` nimmt den per Umgebung konfigurierten Ort, `open(DatabaseSettings)`
einen bestimmten. Für Tests:

```java
HibernateSqllite.open(DatabaseSettings.forFile(tempDir.resolve("test.db")))
```

Steuerung von außen:

| Systemeigenschaft | Umgebungsvariable | Standard |
|---|---|---|
| `gkv.db.path` | `GKV_DB_PATH` | `database.db` im Arbeitsverzeichnis |
| `gkv.db.schema` | `GKV_DB_SCHEMA` | `update` |

**`gkv.db.schema=create` verwirft sämtliche Daten.** Der Wert stand einmal fest
in der Konfiguration und leerte die Datenbank bei jedem Programmstart. Nicht
ohne Not setzen.

Neue Repository-Methoden nutzen `TransactionRunner`, nicht eigene
`openSession`/`commit`/`rollback`-Blöcke:

```java
return runner.read("Patienten laden",
        session -> session.createQuery("FROM Patient", Patient.class).getResultList());

runner.writeVoid("Patient speichern", session -> session.merge(patient));
```

Zu SQLite ist zweierlei zu wissen. Erstens lässt es nur einen Schreiber zu; die
Verbindungs-URL setzt deshalb `busy_timeout` und `journal_mode=WAL`. Zweitens
hilft `busy_timeout` **nicht**, wenn eine Transaktion erst liest und dann
schreibt — SQLite meldet dann sofort `SQLITE_BUSY`, um eine Verklemmung zu
vermeiden. Solche Abläufe müssen anwendungsseitig serialisiert werden, wie bei
`nextDtaInterchangeReference()`.

## Der Versandablauf

```
AbrechnungsMaske
 └── Abrechnungslauf  (= AbrechnungService::createAndDispatch)
      └── AbrechnungService.createAndDispatch(...)
           └── DtaDispatchService.generateAndRoute(...)
                ├── 1. alle Nachrichten erzeugen  (DtaFactory)
                ├── 2. alle prüfen                (DtaValidationService)
                │      └── ein Fehler ⇒ DtaValidierungsException, nichts wird versendet
                └── 3. erst dann zustellen        (BillingOfficeTransport)
```

Die Reihenfolge ist wesentlich: erst alles prüfen, dann zustellen. Sonst wäre
bei einem Fehler in der Mitte eines Laufs bereits ein Teil bei der Kasse und
müsste dort einzeln storniert werden. Wer den Ablauf ändert, muss das
beibehalten.

`DtaDispatchService.pruefe(abrechnungen)` prüft, ohne zu versenden — der
Einstieg für eine Vorschau in der Oberfläche.

## Transporte

`BillingOfficeTransport` ist die Schnittstelle:

- `FileBillingOfficeTransport` — legt die Datei im Zielverzeichnis ab
- `SimulierterKassenTransport` — **umschließt** einen anderen Transport und
  lässt zusätzlich eine `SimulierteKassenGegenstelle` antworten

Ein neuer Versandweg setzt die Schnittstelle um; der umschließende Transport
funktioniert dann auch damit, ohne geändert zu werden.

## Fehlerbehandlung

Keine nackten `RuntimeException`. Vorhanden sind:

| Ausnahme | Bedeutung |
|---|---|
| `PersistenceOperationException` | Datenbankzugriff fehlgeschlagen |
| `DispatchException` | Zustellung fehlgeschlagen |
| `DtaValidierungsException` | Prüfung nicht bestanden; trägt den Bericht |

## Testkonventionen

- Testnamen und `@DisplayName` auf Deutsch, in ganzen Sätzen
- `@Nested` zur Gliederung nach Thema
- Persistenztests gegen eine echte Datei über `@TempDir`, nicht gegen Mocks
- **IK in Testdaten müssen eine gültige Prüfziffer haben**, sonst weist die
  Validierung ab. Verwendbar: `108310400`, `104940005`, `102137985`,
  `101560000`
- `Information/Valide.DTA` ist die Messlatte: sie muss unbeanstandet
  durchlaufen; jeder Einzeltest verletzt danach genau eine Vorgabe

### Oberflächentests

Es gibt **kein TestFX**, und das ist Absicht: dessen Wert liegt im Nachstellen
echter Eingaben. Seit die Dialoge hinter `Dialoge` liegen, blockiert nichts
mehr, und die Masken lassen sich unmittelbar aufbauen und auswerten. Das
erspart die Abhängigkeit von Monocle, das jeder JavaFX-Fassung hinterherhinkt.

```java
JavaFxLaufzeit.aufFxFaden(() -> {
    Region maske = new GruppenMaske(new JavaFxUiFactory(), texte, dialoge,
            datenbank, rahmen).formular(null);
    ((Button) maske.lookup("#" + GruppenMaske.ID_SPEICHERN)).fire();
    assertEquals(texte.get("msg.groupNameRequired"), dialoge.einzige().text());
});
```

Die Ersatzstücke liegen unter `gkv-ui/src/test/java/.../presentation/`:
`SpeicherRepository` (Daten im Speicher), `AufzeichnendeDialoge` (sammelt
Meldungen, gibt vorgegebene Antworten), `AufzeichnenderRahmen` (merkt sich,
was gezeigt wurde).

Jede Änderung an einem Bedienelement muss auf dem JavaFX-Faden laufen — sonst
schlägt sie fehl. Im Bauknecht braucht die Laufzeit eine Anzeige; der Workflow
ruft deshalb `xvfb-run -a mvn test`.

## Was im Projekt bewusst so ist

- Die Prüfung liest die erzeugte DTA **zurück** und arbeitet gegen die
  Struktur, nicht gegen Zeichenketten im Ergebnis. Dadurch lässt sich auch eine
  fremde Datei prüfen.
- `Leistungsparameter` fällt bei unlesbarer Blaupause auf Vorbelegungen zurück,
  statt abzubrechen — die Nachricht durchläuft danach ohnehin die Prüfung.
- Testdaten werden **nicht** mehr automatisch angelegt. Nur mit
  `-Dgkv.testdaten=true` oder über den Menüpunkt unter „Dev".
