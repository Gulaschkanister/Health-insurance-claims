# GKVTransmitter (JavaFX)

Dieses Projekt ist ein JavaFX-Grundgeruest auf Basis von Maven.

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Projekt starten

```bash
mvn clean javafx:run
```

## Projekt bauen

```bash
mvn clean package
```

## Struktur

- `src/main/java/de/gkvtransmitter/App.java`: JavaFX-Anwendung
- `src/main/java/de/gkvtransmitter/Main.java`: Main-Einstiegspunkt
- `src/main/resources/`: JSON-Definitionen fuer Codes, Segmente, Profile und Rechnungsbeispiele
- `src/main/resources/invoices/invoice-catalog.json`: konfigurierbare Liste der ladbaren Invoice-Templates
- `src/main/resources/profiles/profile-catalog.json`: konfigurierbare Liste der ladbaren Profile
