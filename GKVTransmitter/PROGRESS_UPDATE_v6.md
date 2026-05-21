# GKVTransmitter - Progress Update (v6.0)

**Datum:** 21. Mai 2026  
**Vergleich:** v5.0 → v6.0  
**Fokus:** Rückbildungskurs-Abrechnung, Patienten-/Dienstleister-Vorlagen im Rechnungsformular

---

## 1. Zusammenfassung

In dieser Session wurden zwei neue Features umgesetzt:

1. **Rückbildungskurs zur Abrechnungsliste hinzugefügt** ✅  
   Neue Rechnungsvorlage mit validem DTA-Format (SLGA + SLLA) und korrekter Positionsnummer für SGS H.

2. **Patienten und Dienstleister in Rechnungsformularen einbindbar** ✅  
   Jedes Rechnungsformular zeigt jetzt am Anfang optionale Vorlagen-Dropdowns für Patienten und Dienstleister – bei Auswahl werden die relevanten NAD- und FKT-Felder automatisch vorausgefüllt.

3. **Bugfix NAD-Segment: Postleitzahl-Eingabefeld korrigiert** ✅  
   `inputType` war fälschlicherweise `"INTEGER"` (kein gültiger `InputOption`-Wert), was dazu führte, dass das PLZ-Feld im Formular leer blieb. Jetzt korrekt auf `"NUMBER"` gesetzt.

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
