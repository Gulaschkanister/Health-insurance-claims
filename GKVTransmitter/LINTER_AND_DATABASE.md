# Linter-Konfiguration und Datenbank-Verwaltung

## 📋 Code-Qualität mit Linter

Das Projekt ist jetzt mit **PMD** konfiguriert, um die Code-Qualität zu überprüfen.

### PMD (Static Code Analysis)
PMD analysiert Java-Code auf Qualitätsprobleme, Design-Fehler und Best-Practices.

**Linter ausführen:**
```bash
mvn pmd:check
```

**PMD-Bericht generieren:**
```bash
mvn pmd:pmd
# Der Report wird in target/pmd.html generiert
```

### PMD-Regeln
Die folgenden PMD-Ruleset-Kategorien sind aktiviert:
- `category/java/bestpractices.xml` - Best Practices Regeln
- `category/java/design.xml` - Design-Pattern Regeln

**Beispiele von überprüften Problemen:**
- Ungenutzte Variablen und Parameter
- Zu komplexe Methoden (zu viele Bedingungen)
- Zu große Klassen/Methoden
- Verstöße gegen Design-Patterns
- Performance-Probleme

---

## 💾 Datenbank-Verwaltung (SQLite)

### Problem gelöst: Datenbankpersistierung
**Vorher:** Die Datenbank `database.db` wurde bei jedem `mvn clean` gelöscht.
**Nachher:** Die Datenbank wird erhalten und wiederverwendet zwischen Sessions.

### Wie die Lösung funktioniert

#### 1. **Maven Clean Plugin konfiguriert**
Die `maven-clean-plugin` wurde so konfiguriert, dass sie die `database.db` Datei **nicht** löscht:

```xml
<excludes>
    <exclude>database.db</exclude>
</excludes>
```

#### 2. **Hibernate auf "update" konfiguriert**
In `hibernate.cfg.xml`:
```xml
<property name="hibernate.hbm2ddl.auto">update</property>
```

Das bedeutet:
- **Beim ersten Start:** Tabellen werden automatisch erstellt
- **Bei später Starts:** Bestehende Tabellen bleiben, nur Schema wird aktualisiert
- **Daten:** Alle existierenden Daten bleiben erhalten

#### 3. **Hibernat-Performance optimiert**
Zusätzliche Konfigurationen für bessere Performance:
- `hibernate.connection.pool_size=5` - Verbindungs-Pooling
- `hibernate.jdbc.batch_size=20` - Batch-Processing
- `hibernate.format_sql=true` - Formatiertes SQL-Output

### Datenbankdatei

**Location:** `database.db` im Projekt-Stammverzeichnis
**Format:** SQLite3
**Automatisches Backup:** Das alte `database.db` sollte regelmäßig gesichert werden

### Datenbankoperationen

**Datenbankversion zurücksetzen (wenn nötig):**
```bash
# Einfach die Datei löschen - wird beim nächsten Start neu erstellt
rm database.db
```

**Datenbank-Dump erstellen (für Backup):**
```bash
sqlite3 database.db ".dump" > database_backup.sql
```

**Datenbank-Inhalt inspizieren:**
```bash
sqlite3 database.db
> .tables         # Zeigt alle Tabellen
> SELECT * FROM Patient;  # Zeigt alle Patienten
> .quit
```

---

## 🚀 Maven Build-Prozess

### Standard Build (Linter wird NICHT automatisch ausgeführt)
```bash
# Compile und Package
mvn clean compile

# Mit Tests (if available)
mvn clean package
```

### Mit Linter-Checks
```bash
# Alle Checks
mvn clean compile pmd:check

# Oder separater Check
mvn pmd:check
mvn pmd:pmd  # Report generieren
```

### Schneller Build (ohne Linter)
```bash
mvn -DskipTests clean compile
```

---

## 📊 Wichtige Dateien

| Datei | Zweck |
|-------|-------|
| `pom.xml` | Maven-Konfiguration mit Linter-Plugins |
| `src/main/resources/hibernate.cfg.xml` | Hibernate-Konfiguration (Datenbank-Einstellungen) |
| `database.db` | SQLite-Datenbankdatei (wird erhalten bei `mvn clean`) |

---

## ✅ Best Practices

### Code-Qualität
1. Regelmäßig `mvn pmd:check` ausführen
2. PMD-Reports reviewen und Probleme beheben
3. Die refaktorierten Abstraktion verwenden (EntityFieldPopulator, EditFormController)

### Datenbank-Verwaltung
1. Regelmäßige Backups von `database.db` erstellen
2. Nur `mvn clean` verwenden, um target/ zu löschen (database.db wird geschützt)
3. Bei Schema-Änderungen bleibt die Datenbank erhalten und wird automatisch aktualisiert

### Versionskontrolle
```
# .gitignore sollte enthalten:
target/
*.class
.DS_Store
# database.db sollte NICHT ignoriert werden, da Testdaten wichtig sind
```

---

## 🔧 Troubleshooting

### Problem: "Datenbank-Datei ist zu groß"
**Lösung:** Alte Test-Daten löschen und neu starten
```bash
rm database.db
```

### Problem: "PMD meldet zu viele Warnungen"
**Lösung:** 
1. PMD-Report anschauen: `mvn pmd:pmd && open target/pmd.html`
2. False-positives mit `@SuppressWarnings` ignorieren
3. Regelset anpassen (bei Bedarf)

### Problem: "Hibernate-Fehler beim Starten"
**Lösung:** Prüfe `hibernate.cfg.xml` auf korrekte Pfade und Eigenschaften

---

## 📚 Weitere Ressourcen

- [PMD-Dokumentation](https://pmd.github.io/)
- [Hibernate-Dokumentation](https://hibernate.org/)
- [SQLite-Dokumentation](https://www.sqlite.org/docs.html)
- [Maven-Dokumentation](https://maven.apache.org/)

