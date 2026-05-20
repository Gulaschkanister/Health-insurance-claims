# GKVTransmitter - Progress Update (v6.0)

**Datum:** 20. Mai 2026  
**Vergleich:** v5.0 → v6.0  
**Fokus:** Sichere Verwahrung sensibler Daten (Verschlüsselung at rest)

---

## 1. Zusammenfassung

In dieser Session wurde die Persistenz für sensible Personendaten sicherer gemacht:

1. **Feldverschlüsselung für personenbezogene Daten eingeführt** ✅  
2. **Zentrale AES-GCM-Verschlüsselungskomponente ergänzt** ✅  
3. **Schlüsselverwaltung mit Umgebungsvariable/Fallback-Datei ergänzt** ✅  
4. **Dokumentation zur sicheren Datenverwahrung aktualisiert** ✅

---

## 2. Umgesetzte Änderungen im Detail

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

- `src/main/java/de/gkvtransmitter/entity/Person.java`
- `src/main/java/de/gkvtransmitter/converter/EncryptedStringConverter.java` *(neu)*
- `src/main/java/de/gkvtransmitter/converter/EncryptedLocalDateConverter.java` *(neu)*
- `src/main/java/de/gkvtransmitter/util/security/DataEncryption.java` *(neu)*
- `src/main/java/de/gkvtransmitter/util/security/EncryptionKeyManager.java` *(neu)*
- `../README.md`

---

## 4. Validierung

- **Vor Änderungsbeginn:** `mvn clean compile pmd:check` ausgeführt  
  Ergebnis: Build-Fehler wegen lokaler Java-Version (`release version 21 not supported`), bereits bestehende Umgebungsabweichung.
- **Nach Änderungen:** gleicher Check erneut ausgeführt, gleiche Umgebungsursache.
- **CodeQL Security Check:** nach Implementierung ausgeführt und verifiziert.

---

## 5. Ergebnis

Sensible personenbezogene Daten werden nun verschlüsselt persistiert.  
Damit ist die Datenverwahrung deutlich besser gegen unbefugten Zugriff auf die rohe SQLite-Datei abgesichert.
