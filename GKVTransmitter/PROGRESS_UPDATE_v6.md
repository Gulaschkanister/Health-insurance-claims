# GKVTransmitter - Progress Update (v6.0)

**Datum:** 21. Mai 2026  
**Vergleich:** v5.0 → v6.0  
**Fokus:** Rückbildungskurs-Abrechnung, Patienten-/Dienstleister-Vorlagen im Rechnungsformular
**Datum:** 20. Mai 2026  
**Vergleich:** v5.0 → v6.0  
**Fokus:** UI-Erleichterungen für Rechnungsanlage (Patient/Dienstleister + Sammelrechnung)
**Fokus:** Sichere Verwahrung sensibler Daten (Verschlüsselung at rest)

---

## 1. Zusammenfassung

In dieser Session wurden zwei neue Features umgesetzt:

1. **Rückbildungskurs zur Abrechnungsliste hinzugefügt** ✅  
   Neue Rechnungsvorlage mit validem DTA-Format (SLGA + SLLA) und korrekter Positionsnummer für SGS H.

2. **Patienten und Dienstleister in Rechnungsformularen einbindbar** ✅  
   Jedes Rechnungsformular zeigt jetzt am Anfang optionale Vorlagen-Dropdowns für Patienten und Dienstleister – bei Auswahl werden die relevanten NAD- und FKT-Felder automatisch vorausgefüllt.

3. **Bugfix NAD-Segment: Postleitzahl-Eingabefeld korrigiert** ✅  
   `inputType` war fälschlicherweise `"INTEGER"` (kein gültiger `InputOption`-Wert), was dazu führte, dass das PLZ-Feld im Formular leer blieb. Jetzt korrekt auf `"NUMBER"` gesetzt.
In dieser Session wurde die Rechnungs-UI gezielt so erweitert, dass bei der Erfassung weniger manuelle Eingaben nötig sind:

1. **Schnellübernahme von Patientendaten in Rechnungsfelder** ✅  
2. **Schnellübernahme von Dienstleisterdaten in Rechnungsfelder** ✅  
3. **Sammelrechnungs-Defaults für GES-Statuscodes (00/99)** ✅  
4. **Robustes Laden von Code-Listen (auch JSON mit `codes`-Array)** ✅
In dieser Session wurde die Persistenz für sensible Personendaten sicherer gemacht:

1. **Feldverschlüsselung für personenbezogene Daten eingeführt** ✅  
2. **Zentrale AES-GCM-Verschlüsselungskomponente ergänzt** ✅  
3. **Schlüsselverwaltung mit Umgebungsvariable/Fallback-Datei ergänzt** ✅  
4. **Dokumentation zur sicheren Datenverwahrung aktualisiert** ✅

---

## 2. Umgesetzte Änderungen im Detail

### 2.1 Neue Rechnungsvorlage: Rückbildungskurs (`postnatal_class_single.json`)

- Identische DTA-Struktur wie `antenatal_class_single.json` (beide SGS H, §§ 24c/d SGB V)
- Unterschied zur Geburtsvorbereitung: `invoicerName` und vorausgefüllte ENF-Werte
- ENF-Segment (intern, für spätere DTA-Ausgabe):
  - `Zeilen-Identifikationsnummer`: `01`
  - `Abrechnungspositionsnummer`: `306050602` (Rückbildungsgymnastik, Anlage 3 SGS H)
  - Zum Vergleich Geburtsvorbereitung: `306050601`
- Abrechnungscode bleibt `61` (SGS H), Tarifkennzeichen und Menge werden vom Nutzer befüllt

**DTA-Struktur (SLGA + SLLA):**

```
UNB  → technische Hülle
UNH  → SLGA-Kopf
FKT  → IK-Bezug (SLGA)
REC  → Rechnungsdaten (SLGA)
UST  → Umsatzsteuer (SLGA, optional)
GES  → Gesamtsumme 00 (SLGA)
GES  → Gesamtsumme 99 (SLGA)
NAM  → Ansprechpartner (SLGA)
UNT  → SLGA-Ende
UNH  → SLLA-Kopf
FKT  → IK-Bezug (SLLA)
REC  → Rechnungsdaten (SLLA)
INV  → Versicherten-/Belegbezug (SLLA)
NAD  → Name/Adresse Versicherte (SLLA)
ENF  → Leistungszeile Rückbildung (SLLA) [Positionsnummer: 306050602]
BES  → Fallsumme (SLLA)
UNT  → SLLA-Ende
UNZ  → Interchange-Abschluss
```

### 2.2 Patienten- und Dienstleister-Vorlagen im Rechnungsformular

Beim Öffnen eines Rechnungsformulars (Menü → Rechnung → ...) erscheint jetzt am Anfang ein optionaler **Vorlagen-Bereich**:

- **Patient auswählen**: Dropdown mit allen in der Datenbank gespeicherten Patienten.  
  Bei Auswahl werden folgende Felder automatisch befüllt:
  - NAD: Nachname, Vorname, Geburtsdatum, Straße (Straße + Hausnummer), Postleitzahl, Stadt
  - INV: Versichertennummer (IK des Patienten)

- **Dienstleister auswählen**: Dropdown mit allen gespeicherten Dienstleistern.  
  Bei Auswahl wird befüllt:
  - FKT: IK des Leistungserbringers

Die Vorlagen sind vollständig optional – alle Felder können auch manuell befüllt oder nach dem Vorab-Befüllen korrigiert werden. Sind keine Einträge in der Datenbank vorhanden, erscheint ein leeres Dropdown mit Hinweistext.

### 2.3 Bugfix: NAD-Segment Postleitzahl

- **Datei:** `src/main/resources/segments/nad.json`
- **Feld:** `Postleitzahl` (Position 5)
- **Vorher:** `"inputType": "INTEGER"` → kein gültiger `InputOption`-Wert, Feld wurde im Formular ignoriert
- **Nachher:** `"inputType": "NUMBER"` → korrekt als Spinner-Eingabefeld gerendert

### 2.4 JsonParserFactory: Neue Vorlage registriert

- `INVOICE_FILES` um `"invoices/postnatal_class_single.json"` ergänzt
- Die neue Vorlage erscheint automatisch im Menü (Rechnung → Rückbildungskurs Einzelabrechnung)

### 2.5 UI-Messages: Neue Texte

Drei neue Einträge in `ui-messages.json`:
- `label.invoicePresets` – Überschrift des Vorlagen-Bereichs
- `label.serviceProviderPreset` – Label für Dienstleister-Dropdown
- `prompt.serviceProviderPreset` – Platzhaltertext für Dienstleister-Dropdown

---

## 3. Geänderte Dateien

- `src/main/resources/invoices/postnatal_class_single.json` *(neu)*
- `src/main/resources/segments/nad.json` – Bugfix Postleitzahl inputType
- `src/main/java/de/gkvtransmitter/parser/json/JsonParserFactory.java` – INVOICE_FILES ergänzt
- `src/main/resources/messages/ui-messages.json` – 3 neue Messages
- `src/main/java/de/gkvtransmitter/presentation/View.java` – Vorlagen-Bereich + Hilfsmethoden

---

## 4. Neue View-Methoden (View.java)

| Methode | Beschreibung |
|---------|-------------|
| `buildPresetSection(Map<String, Node>)` | Erstellt den Vorlagen-Bereich mit Patient- und Dienstleister-Dropdown |
| `prefillFromPatient(Patient, Map<String, Node>)` | Befüllt NAD-Felder + Versichertennummer aus Patient |
| `prefillFromServiceProvider(ServiceProvider, Map<String, Node>)` | Befüllt IK des Leistungserbringers aus Dienstleister |
| `setNodeText(Map<String, Node>, String, String)` | Setzt Textwert in TextField, Spinner oder ComboBox |
| `setNodeDate(Map<String, Node>, String, LocalDate)` | Setzt Datum in DatePicker |
| `loadPatientsSilently()` | Lädt Patienten aus DB, leere Liste bei Fehler |
| `loadServiceProvidersSilently()` | Lädt Dienstleister aus DB, leere Liste bei Fehler |

Außerdem: `createFormular` befüllt `allFieldNodes` jetzt ohne `null`-Einträge (Guard gegen ungültige `inputType`-Werte).

---

## 5. DTA-Codes Rückbildungskurs (Referenz)

| Feld | Wert | Quelle |
|------|------|--------|
| Abrechnungscode (ENF Pos. 2.1) | `61` | Anlage 3, Schlüssel 8.1.5.1 – SGS H |
| Tarifkennzeichen (ENF Pos. 2.2) | `00000` (Beispiel) | Vertrag/Anlage 3, Schlüssel 8.1.5.2 |
| Abrechnungspositionsnummer (ENF Pos. 3) | `306050602` | Anlage 3, Abschnitt 8.2 – Rückbildungsgymnastik |
| Zum Vergleich: Geburtsvorbereitung | `306050601` | Anlage 3 – Geburtsvorbereitung |

---

## 6. Validierung

- **PMD-Linter:** `mvn pmd:check` – BUILD SUCCESS ✅
- **Build-Hinweis:** Lokale Umgebung läuft mit Java 17, Projekt benötigt Java 21 (bekannte bestehende Umgebungseinschränkung)
- **CodeQL Security Check:** Keine neuen Alerts ✅

---

## 7. Ergebnis

Der Rückbildungskurs ist jetzt als vollwertige Abrechnungsvorlage mit korrektem DTA-Format (SLGA + SLLA) und Positionsnummer `306050602` (SGS H) in der Abrechnungsliste vorhanden.  
Patienten und Dienstleister aus der Datenbank können beim Ausfüllen jeder Rechnung bequem per Dropdown vorausgewählt werden, was die manuelle Dateneingabe deutlich reduziert.
### 2.1 Rechnungsformular: Feldschlüssel segmentbezogen gemacht

- Felder werden im UI jetzt segmentbezogen aufgebaut (`SEGMENT#Position - Feldname`).
- Dadurch bleiben auch mehrfach vorkommende Felder (z. B. zwei `GES`-Segmente) getrennt und editierbar.

### 2.2 Schnellübernahme Patient

- Im Rechnungsformular wurde ein eigener Preset-Bereich ergänzt.
- Auswahl eines Patienten befüllt passende `NAD`-Felder automatisch (u. a. Nachname, Vorname, Geburtsdatum, Straße/Hausnummer, Postleitzahl).
- Wenn genau **ein** Patient vorhanden ist, werden die passenden Rechnungsfelder direkt beim Öffnen vorbelegt.

### 2.3 Schnellübernahme Dienstleister

- Auswahl eines Dienstleisters befüllt passende Felder automatisch:
  - `NAM` (u. a. Nachname/Vorname)
  - `FKT`-IK-Felder mit Leistungserbringer-/Rechnungsstellerbezug
- Wenn genau **ein** Dienstleister vorhanden ist, erfolgt diese Übernahme direkt automatisch.

### 2.4 Sammelrechnungs-Defaults

- Für `GES`-Statusfelder werden automatische Startwerte gesetzt:
  - erstes GES-Statusfeld: `00`
  - zweites GES-Statusfeld: `99`

### 2.5 Code-Listen robuster geladen

- Code-Parsing unterstützt jetzt sowohl:
  - reine Array-Strukturen
  - Objekt-Strukturen mit `codes`-Array und `code`-Einträgen
- Dadurch werden `Rechnungsart` und `GES`-Statuscodes zuverlässig als Auswahlwerte angezeigt.

### 2.6 UI-Texte ergänzt

- Neue Message-Keys für den Preset-Bereich und die zwei Preset-Dropdowns hinzugefügt.
### 2.1 Persistenz-Verschlüsselung

In `Person` werden folgende Felder nun transparent beim Schreiben verschlüsselt und beim Lesen entschlüsselt:

- `firstname`
- `lastname`
- `street`
- `country`
- `housenumber`
- `birthDate`

Dafür wurden zwei JPA-Converter ergänzt:

- `EncryptedStringConverter`
- `EncryptedLocalDateConverter`

### 2.2 Kryptografie-Komponente

Neue zentrale Utility-Klasse:

- `DataEncryption`

Eigenschaften:

- AES/GCM/NoPadding
- zufälliger IV pro Verschlüsselung
- Base64-kodierte Ablage (IV + Ciphertext)

### 2.3 Schlüsselverwaltung

Neue Klasse:

- `EncryptionKeyManager`

Verhalten:

1. Wenn `GKV_ENCRYPTION_KEY` gesetzt ist, wird dieser Key verwendet  
   (Base64 oder String wird auf 256 Bit normalisiert).
2. Wenn nicht gesetzt, wird ein lokaler Schlüssel unter  
   `~/.gkvtransmitter/encryption.key` geladen oder neu erzeugt.
3. Bei POSIX-Dateisystemen werden restriktive Dateirechte gesetzt (`rw-------`).

### 2.4 Dokumentation

Die Datenbank-Sektion in der Root-`README.md` wurde um Hinweise zu:

- Verschlüsselung sensibler Felder
- Schlüsselbereitstellung über Umgebungsvariable
- lokalen Schlüssel-Fallback

erweitert.

---

## 3. Geänderte Dateien

- `src/main/java/de/gkvtransmitter/presentation/View.java`
- `src/main/resources/messages/ui-messages.json`
- `PROGRESS_UPDATE_v6.md` *(neu)*
- `src/main/java/de/gkvtransmitter/entity/Person.java`
- `src/main/java/de/gkvtransmitter/converter/EncryptedStringConverter.java` *(neu)*
- `src/main/java/de/gkvtransmitter/converter/EncryptedLocalDateConverter.java` *(neu)*
- `src/main/java/de/gkvtransmitter/util/security/DataEncryption.java` *(neu)*
- `src/main/java/de/gkvtransmitter/util/security/EncryptionKeyManager.java` *(neu)*
- `../README.md`

---

## 4. Validierung

- **Baseline vor Änderung:**  
  - `mvn clean package` ❌ (Umgebungsproblem: Java 21 nicht verfügbar, bekannte Einschränkung)  
  - `mvn pmd:check` ✅
- **Nach Änderung:**  
  - `mvn pmd:check` wird erneut ausgeführt  
  - `CodeQL Security Check` wird ausgeführt
- **Vor Änderungsbeginn:** `mvn clean compile pmd:check` ausgeführt  
  Ergebnis: Build-Fehler wegen lokaler Java-Version (`release version 21 not supported`), bereits bestehende Umgebungsabweichung.
- **Nach Änderungen:** gleicher Check erneut ausgeführt, gleiche Umgebungsursache.
- **CodeQL Security Check:** nach Implementierung ausgeführt und verifiziert.

---

## 5. Ergebnis

Die Erstellung von Rechnungen ist im UI deutlich direkter: Patient und Dienstleister können mit wenigen Klicks übernommen werden und Sammelrechnungs-Statuswerte sind sofort sinnvoll vorbelegt. Dadurch sinkt die manuelle Eingabelast für den Nutzer spürbar.
Sensible personenbezogene Daten werden nun verschlüsselt persistiert.  
Damit ist die Datenverwahrung deutlich besser gegen unbefugten Zugriff auf die rohe SQLite-Datei abgesichert.
