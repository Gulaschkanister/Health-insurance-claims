# GKVTransmitter - Progress Update (v6.0)

**Datum:** 20. Mai 2026  
**Vergleich:** v5.0 → v6.0  
**Fokus:** UI-Erleichterungen für Rechnungsanlage (Patient/Dienstleister + Sammelrechnung)
**Fokus:** Sichere Verwahrung sensibler Daten (Verschlüsselung at rest)

---

## 1. Zusammenfassung

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
