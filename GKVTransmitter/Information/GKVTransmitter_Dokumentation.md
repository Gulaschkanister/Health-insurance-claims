---
title: "GKVTransmitter"
subtitle: "Abrechnung mit gesetzlichen Krankenkassen — Dokumentation"
lang: de-DE
toc: true
toc-depth: 2
---

# Worum es geht

Der GKVTransmitter unterstützt die Abrechnung erbrachter Leistungen mit gesetzlichen Krankenkassen. Leistungen für gesetzlich Versicherte werden nicht mit den Versicherten abgerechnet, sondern mit deren Krankenkasse — und zwar als strukturierte Datenlieferung im DTA-Format, nicht als Rechnung auf Papier.

Das Programm erfasst die dafür nötigen Angaben, erzeugt daraus die Lieferung, prüft sie gegen ein Regelwerk und übergibt sie an die zuständige Kasse.

Der fachliche Zuschnitt entspricht der Tätigkeit einer Hebamme: die Belegnummern tragen das Präfix `HEB`, abgedeckt ist der Leistungsbereich SGS H mit dem Abrechnungscode `61`.

# Betrieb

## Systemvoraussetzungen

Ein Windows-Rechner. **Mehr nicht** — insbesondere kein installiertes Java, kein Datenbankdienst, kein Server und keine Administratorrechte.

Die ausgelieferte Fassung bringt ihre eigene Java-Laufzeit mit. Der Ordner wird kopiert, `GKVTransmitter.exe` per Doppelklick gestartet.

Eine Internetverbindung wird ausschließlich für den Versand an die Krankenkassen benötigt. Stammdatenpflege, Abrechnungserstellung, DTA-Erzeugung und Prüfung funktionieren ohne Netz.

## Wo die Daten liegen

Alle Daten liegen im Profil des angemeldeten Benutzers:

| Betriebssystem | Ort |
|---|---|
| Windows | `%LOCALAPPDATA%\GKVTransmitter` |
| macOS | `~/Library/Application Support/GKVTransmitter` |
| Linux | `$XDG_DATA_HOME/gkvtransmitter`, ersatzweise `~/.local/share/gkvtransmitter` |

Darunter befinden sich:

- `database.db` — die Datenbank mit Personen, Gruppen und Blaupausen
- `dta_output/staging/<Kassen-IK>/` — die erzeugten Lieferungen, unverändert aufbewahrt
- `dta_output/outbox/<Kasse>/` — die zugestellten Dateien je Kasse

Der Ablageort hängt am Benutzerprofil und **nicht** daran, von wo das Programm gestartet wurde. Ob per Doppelklick auf dem Desktop, aus der Eingabeaufforderung oder von einem USB-Stick — die Daten werden immer am selben Ort gefunden.

## Sicherung

Eine Kopie des oben genannten Ordners genügt. Sie enthält Datenbank, Blaupausen und sämtliche erzeugten DTA-Dateien.

Das Programm sollte dabei geschlossen sein, damit die Datenbank in einem stimmigen Zustand gesichert wird.

## Mehrere Stände nebeneinander

Über die Umgebungsvariable `GKV_HOME` lässt sich ein anderer Ablageort festlegen — etwa um einen Übungsstand von den echten Daten zu trennen:

```
set GKV_HOME=D:\GKV-Uebung
GKVTransmitter.exe
```

# Ablauf einer Abrechnung

## Aus Sicht der Anwenderin

1. **Stammdaten pflegen.** Patientinnen und Leistungserbringer anlegen. Wichtig ist das Institutionskennzeichen der Krankenkasse — daran entscheidet sich später das Versandziel.
2. **Gruppe bilden.** Personen, die gemeinsam abgerechnet werden — etwa die Teilnehmerinnen eines Kurses — in einer Gruppe zusammenfassen. Die Gruppe braucht mindestens einen Leistungserbringer.
3. **Blaupause anlegen.** Eine Rechnungsvorlage ausfüllen und unter einem Namen speichern. Sie enthält die Leistungsangaben: Einzelbetrag, Abrechnungscode, Tarifkennzeichen, Positionsnummer. Blaupausen sind wiederverwendbar.
4. **Abrechnung erstellen.** Blaupause und Gruppe auswählen, je Teilnehmerin die Terminanzahl eintragen, Versand auslösen.
5. **Prüfergebnis lesen.** Bei Beanstandungen erscheinen alle auf einmal, getrennt nach zu Behebendem und Hinweisen. Es wurde in diesem Fall **nichts** versendet.
6. **Ergebnis kontrollieren.** Nach erfolgreichem Versand nennt das Programm die erzeugten Dateien je Kassen-IK.

## Was dabei im Hintergrund geschieht

![Übersicht der Verarbeitung](GKVTransmitter_Uebersicht.png)

Der Weg von der Eingabe zur Kasse verläuft in klar getrennten Schritten:

1. Aus Patientin, Leistungserbringer, Blaupause und Terminanzahl entsteht eine `Abrechnung`.
2. Daraus erzeugt das Programm die DTA-Nachricht — bestehend aus einer SLGA-Nachricht mit den Summen und einer SLLA-Nachricht mit den Falldaten.
3. Die erzeugte Nachricht wird **wieder eingelesen** und gegen das Regelwerk geprüft.
4. Erst wenn keine Nachricht des Laufs beanstandet wird, beginnt die Zustellung.
5. Die Rückmeldung der Kasse wird eingeordnet.

Schritt 3 verdient eine Erläuterung: geprüft wird nicht, was das Programm zu erzeugen *glaubt*, sondern was tatsächlich in der Datei steht. Dadurch lässt sich auf demselben Weg auch eine Datei aus fremder Quelle prüfen.

Schritt 4 ist ebenso bewusst gewählt: Prüfung und Zustellung sind zwei getrennte Durchgänge über alle Abrechnungen. Würde je Abrechnung geprüft und sofort zugestellt, läge bei einem Fehler in der Mitte eines Laufs bereits ein Teil bei der Kasse und müsste dort einzeln storniert werden.

# Die Prüfung

## Warum sie da ist

Eine fehlerhafte Lieferung wird von der Kasse abgewiesen — häufig erst Tage später und für die gesamte Lieferung, nicht nur für den fehlerhaften Fall. Die Prüfung soll solche Fehler feststellen, solange sie noch billig zu beheben sind.

## Was geprüft wird

| Regel | Gegenstand |
|---|---|
| Segmentsyntax | Abschlusszeichen, Form der Segmentbezeichner, bekannte Segmente |
| Nachrichtenrahmen | UNB und UNZ, Anzahl der Nachrichten, Datenaustauschreferenz |
| Nachrichtenabschluss | UNH und UNT, Segmentanzahl je Nachricht |
| Institutionskennzeichen | Länge und Prüfziffer der IK |
| Versichertenangaben | Name, Geburtsdatum, Versichertennummer |
| Betragskonsistenz | Leistungspositionen gegen Fallsumme, Fallsumme gegen Gesamtsumme |

## Fehler und Hinweise

Nur **Fehler** halten den Versand auf. **Hinweise** werden gemeldet, verhindern aber nichts.

Diese Abstufung ist bewusst gewählt: Ein Hinweis, den man nicht braucht, ist lästig. Ein Fehler, den man nicht braucht, verhindert eine berechtigte Abrechnung.

Als Hinweis gemeldet werden etwa ein Alter über 120 Jahren (auffällig, aber nicht ausgeschlossen) oder ein Segment, das das Programm noch nicht kennt.

## Die Prüfziffer der Institutionskennzeichen

Ein Institutionskennzeichen hat neun Stellen; die letzte ist eine Prüfziffer:

```
1 0 8 3 1 0 4 0 0
│ │ └───┬───┘ │ │
│ │     │     │ └── Stelle 9: Prüfziffer
│ │     │     └──── Stellen 3-8: Regionalbereich und Seriennummer
│ └─────┴────────── Stellen 1-2: Klassifikation
```

Die Prüfziffer entsteht aus den Stellen 3 bis 8: abwechselnd mit 2 und 1 gewichten (beginnend mit 2), von jedem Produkt die Quersumme bilden, aufsummieren — die letzte Ziffer der Summe ist die Prüfziffer.

Ein IK mit falscher Prüfziffer führt zur Abweisung der gesamten Lieferung, ohne dass der Inhalt überhaupt geprüft wird. Deshalb wird es vorab kontrolliert.

# Der Versand

## Ziele

Die Zuordnung erfolgt über das Institutionskennzeichen der Krankenkasse. Welche Kasse in welches Verzeichnis geliefert wird, steht in `billing-office-endpoints.json`; derzeit sind 23 Kassen hinterlegt. Eine weitere Kasse wird dort eingetragen — am Programm ist dafür nichts zu ändern.

Ist für eine Kasse kein Ziel hinterlegt, landet die Lieferung in einem Sammelordner. Sie geht also nicht verloren.

## Rückmeldungen

| Einstufung | Bedeutung | Was zu tun ist |
|---|---|---|
| Angenommen | Die Kasse hat die Lieferung übernommen | nichts |
| Abgelehnt | fachlich beanstandet | Abrechnung korrigieren |
| Syntaxfehler | Formfehler in der Datei | Datei neu erzeugen |
| Technischer Fehler | Übertragungsproblem | erneut senden |
| Unbekannt | nicht einzuordnen | **muss ein Mensch ansehen** |

Eine Rückmeldung, die sich nicht sicher einordnen lässt, gilt bewusst **nicht** als Annahme. Eine stillschweigend als angenommen verbuchte Ablehnung wäre der teurere Fehler.

## Erprobung ohne echte Kasse

Für Übung und Abnahme steht eine simulierte Gegenstelle bereit. Sie nimmt eine Lieferung entgegen, prüft sie mit demselben Regelwerk und schreibt ein Antwortprotokoll in der Form einer Kassenrückmeldung — einschließlich der Unterscheidung zwischen Syntaxfehler und fachlicher Zurückweisung.

Damit lässt sich der vollständige Ablauf bis zur Rückmeldung durchspielen, ohne Zugangsdaten und ohne Zertifikate.

**Das ersetzt keinen echten Versand** und keine Prüfung durch eine reale Kasse.

# Aufbau des Programms

## Zwei Module

| Modul | Inhalt |
|---|---|
| `gkv-core` | Domäne, DTA-Erzeugung, Validierung, Versand, Persistenz — ohne JavaFX |
| `gkv-ui` | ausschließlich die JavaFX-Oberfläche |

Die Trennung ist im Build verankert: eine Enforcer-Regel lässt das Übersetzen fehlschlagen, sobald eine JavaFX-Abhängigkeit in den Kern gelangt. Nur ein oberflächenfreier Kern lässt sich ohne laufende Anwendung testen, und daran hängt die automatisierte Prüfung der gesamten Fachlogik.

## Fachliches Modell

![Fachliches Modell](GKVTransmitter_Domaene.png)

`Person` ist die gemeinsame Oberklasse von `Patient` und `ServiceProvider`. Eine `PersonGroup` fasst beide zusammen. Eine `Abrechnung` verbindet Patientin, Leistungserbringer und Blaupause mit der Terminanzahl und ist die Vorlage für die DTA-Nachricht.

## Erzeugung, Prüfung und Versand

![DTA-Verarbeitung und Versand](GKVTransmitter_DTA_und_Versand.png)

## Oberfläche und Datenhaltung

![Oberfläche und Datenzugriff](GKVTransmitter_Praesentation_und_Persistenz.png)

`DataRepository` ist der Vertrag, `HibernateSqllite` die Umsetzung. Die Oberfläche kennt nur den Vertrag — die Datenhaltung wäre austauschbar, ohne die Oberfläche zu berühren.

## Erweiterungspunkte

An drei Stellen ist ein Vertrag bewusst von seiner Umsetzung getrennt, damit sich das Programm erweitern lässt, ohne Bestehendes anzufassen:

| Vertrag | Heutige Umsetzung | Erweiterbar um |
|---|---|---|
| `DataRepository` | `HibernateSqllite` | eine andere Datenhaltung |
| `BillingOfficeTransport` | Datei-Zustellung, simulierte Kasse | einen echten Übermittlungsweg |
| `ValidationRule` | sechs mitgelieferte Regeln | weitere fachliche Prüfungen |

# Konfiguration

Alle Einstellungen sind optional; ohne Angabe gelten die Vorgabewerte.

| Systemeigenschaft | Umgebungsvariable | Vorgabe | Bedeutung |
|---|---|---|---|
| `gkv.home` | `GKV_HOME` | Benutzerprofil | Ablageort aller Daten |
| `gkv.db.path` | `GKV_DB_PATH` | `database.db` im Ablageort | Ort der Datenbankdatei |
| `gkv.db.schema` | `GKV_DB_SCHEMA` | `update` | Umgang mit dem Datenbankschema |
| `gkv.testdaten` | — | `false` | Testdaten beim Start anlegen |

> **Achtung:** `gkv.db.schema=create` legt das Schema neu an und **verwirft dabei sämtliche gespeicherten Daten**. Die Vorgabe `update` ergänzt fehlende Tabellen und Spalten und lässt Vorhandenes stehen.

# Entwicklung

## Bauen und testen

```
mvn clean test                     # übersetzen und alle Tests
mvn install -DskipTests            # Kern ins lokale Repository legen
mvn -pl gkv-ui javafx:run          # Anwendung starten
```

`-pl gkv-ui` baut `gkv-core` nicht mit; vor dem ersten Start und nach jeder Änderung am Kern muss deshalb `mvn install` gelaufen sein.

## Auslieferungspaket erzeugen

```
mvn -Ppaket clean package
```

Ergebnis ist `gkv-ui/target/paket/GKVTransmitter/` — ein Ordner mit Startprogramm und mitgelieferter Java-Laufzeit, rund 166 MB.

`jpackage` erzeugt immer für das System, auf dem es läuft. Ein Windows-Paket entsteht also nur unter Windows.

## Debuggen

```
mvn -Ppaket -pl gkv-ui javafx:run   # normal starten
mvn -Pdebug -pl gkv-ui javafx:run   # auf Debugger warten (Port 5005)
```

## Statische Prüfung

```
mvn checkstyle:check
mvn pmd:check
```

Beide melden, blockieren aber nicht.

# Fachliche Grundlagen

## Aufbau einer Lieferung

```
UNB                          Kopf der Übertragung
├── UNH  (SLGA)              Summen und Begleitdaten
│   ├── FKT, REC, UST
│   ├── GES, GES
│   ├── NAM
│   └── UNT
├── UNH  (SLLA)              Fall- und Leistungsdaten
│   ├── FKT, REC
│   ├── INV, NAD
│   ├── ENF
│   ├── BES
│   └── UNT
└── UNZ                      Abschluss der Übertragung
```

Ein Segment besteht aus einem dreibuchstabigen Bezeichner und den Datenelementen, getrennt durch `+` und abgeschlossen mit `'`. Ein `:` trennt Komponenten innerhalb eines Datenelements.

## Formate

- **Datum:** `JJJJMMTT`, also `20240301`
- **Beträge und Mengen:** Komma als Dezimaltrennzeichen, stets zwei Nachkommastellen: `08,00`, `15000,00`
- Ein leeres Datenelement wird durch zwei aufeinanderfolgende `+` dargestellt und darf nicht weggelassen werden

## Summen

```
BES     = Menge × Einzelbetrag        je Abrechnungsfall
GES 00  = Summe aller BES             Gesamtsumme aller Status
GES 99  = gemäß Prüfvorgabe           oft gleich GES 00
```

Eine Abweichung bei `GES 00` ist ein Fehler; eine bei `GES 99` kann der Prüfvorgabe der Kasse entsprechen.

## Weiterführende Unterlagen

Im Ordner `Information` liegen:

- `Anlage_1_TP5_V21_20260115.pdf` und `Anlage_3_TP5_V22_20260218.pdf` — die verbindlichen Vorgaben
- `Abrechnung_Checkliste_kurz.md` — Prüfliste vor dem Export
- `Abrechnung_Datenerklaerung_test.md` — Felderklärung anhand einer Beispieldatei
- `codes/` — die Codelisten des Verfahrens
- `Valide.DTA` — eine gültige Referenznachricht, die im Projekt als Messlatte dient

# Grenzen

Was das Programm heute **nicht** leistet:

- Der Versand ist dateibasiert. Ein signierter und verschlüsselter Übermittlungsweg an reale Annahmestellen ist nicht umgesetzt. Dafür wären Zertifikate einer anerkannten Stelle, Zugangsdaten und ein eigenes Betriebsstätten-IK erforderlich.
- Positionsnummern, Tarifkennzeichen und Abrechnungscodes werden auf Form, nicht aber auf fachliche Zulässigkeit geprüft. Das setzt den jeweiligen Vertrag und Anlage 3 voraus.
- Es gibt keine Stornierung und keine Nachberechnung.
- Der Umsatzsteuersatz ist mit 19 fest hinterlegt.
- Abgedeckt ist der Leistungsbereich SGS H mit Abrechnungscode `61`.
- Die Anwendung ist auf einen Benutzer und einen Rechner ausgelegt. Ein gemeinsamer Zugriff mehrerer Arbeitsplätze auf dieselbe Datenbank ist nicht vorgesehen.
