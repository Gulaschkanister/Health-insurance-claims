# GKV-Abrechnung — Einstieg für eine neue Sitzung

Abrechnung medizinischer Leistungen (Hebammenhilfe, Kurse) mit gesetzlichen
Krankenkassen: Stammdaten erfassen, Nachricht im DTA-Format erzeugen, **prüfen**,
an die Kasse zustellen. Java 21, Maven-Multimodul, JavaFX, SQLite über Hibernate.

## Wo was liegt

| Pfad | Inhalt |
|---|---|
| `GKVTransmitter/` | das eigentliche Projekt (`pom.xml`, `gkv-core`, `gkv-ui`) |
| `GKVTransmitter/Information/` | Dokumentation, Diagramme (`*.puml`), Spezifikationen, Codelisten |
| `GKVTransmitter/Information/dokumentation/` | die 23-teilige Projektdokumentation (Schulabgabe) |
| `.claude/skills/` | fünf Fachskills — **vor Änderungen laden**, sie sind ausführlicher als diese Datei |

Der übergeordnete Ordner `..` (außerhalb dieses Repos) enthält ältere
Python-Skripte zur PDF-Auswertung und eine veraltete `database.db`. **Nicht die
Arbeitskopie** — gearbeitet wird hier.

## Was zuerst zu lesen ist

1. `GKVTransmitter/Information/Naechste_Schritte.md` — die Übergabe: Stand,
   offene Punkte, Merksätze, Fallstricke. Der Abschnitt **„Der Stand aller
   Abschnitte"** beantwortet „wo stehen wir" in einer Tabelle.
2. `GKVTransmitter/Information/Umsetzungsplan.md` — Arbeitspakete Richtung
   Echtbetrieb.
3. `GKVTransmitter/Information/Schulprojekt.md` — Bauplan und **Stilregeln** der
   23-teiligen Dokumentation (gehört nicht in die Abgabe).

Die Skills decken Fachdomäne (`gkv-abrechnung`), Nachrichtenformat
(`dta-format`), Prüfwerk (`dta-validierung`), Versand (`kassen-versand`) und
Aufbau (`gkvtransmitter-architektur`) ab.

## Befehle

```bash
cd GKVTransmitter
bash run.sh                      # gkv-core installieren + Oberfläche starten
mvn -B test                      # alle Tests, Oberflächentests eingeschlossen
mvn -B test -pl gkv-core         # nur der Kern
mvn -Ppaket clean package        # Auslieferungspaket (nur unter Windows sinnvoll)
```

`mvn -pl gkv-ui javafx:run` **allein** scheitert mit „Could not find artifact
de.gkv:gkv-core" — der Kern muss vorher installiert sein. Genau das nimmt
`run.sh` ab.

Unter WSL brauchen weder die Anwendung noch die Oberflächentests eine
Einrichtung: WSLg stellt eine Anzeige bereit, `AblaufTest` und die übrigen
JavaFX-Tests laufen dort ohne `xvfb-run` (nachgeprüft 2026-09-17). Das
`xvfb-run` in `.github/workflows/build.yml` gilt dem CI-Läufer, der keine
Anzeige hat.

Dokumente erzeugen (Markdown ist jeweils die Quelle, `.docx`/`.pdf` sind
abgeleitet und **nicht von Hand zu bearbeiten**):

```bash
bash Information/dokumentation/erzeugen.sh        # die 23 Teile (pandoc, LibreOffice)
bash Information/technische-doku-erzeugen.sh      # Technische Dokumentation (pandoc, XeLaTeX)
plantuml -tpng Information/D05_Klassen_Persistenz.puml   # ein Diagramm neu zeichnen
```

## Regeln, die leicht zu brechen sind

- **`gkv-core` darf kein JavaFX sehen.** Eine Enforcer-Regel lässt den Bau
  sonst fehlschlagen — das ist Absicht, kein Hindernis, das man umgeht.
- **Erst prüfen, dann zustellen.** `DtaDispatchService.generateAndRoute` erzeugt
  alle Nachrichten, prüft alle, und stellt nur zu, wenn keine einen Fehler
  trägt. Diese Reihenfolge ist der Kern des Entwurfs.
- **Nichts raten, was die Kasse prüft.** Positionsnummer, Tarifkennzeichen,
  Abrechnungscode: ein erfundener Wert führt zur Zurückweisung der ganzen
  Lieferung. Lieber offen lassen und fragen (siehe B4/B5 in der Übergabe).
- **Rückfallwerte müssen auffallen.** Der Einzelbetrag fällt auf `0,00`, nicht
  auf einen plausiblen Betrag; `Uebermittlungsart` fällt auf `ERPROBUNG`, nie
  auf `ECHT`. Beides sind Lehren aus echten Fehlern.
- **Diagramme nur über `.puml` ändern.** Das PNG daneben ist abgeleitet.
- Zahlen veralten (Testanzahl, Checkstyle-Befunde). Im Zweifel messen, nicht
  aus der Dokumentation abschreiben.

## Sprache und Stil

Projektsprache ist **Deutsch** — Dokumentation, Commit-Nachrichten, Meldungen
der Oberfläche, Klassennamen teilweise. Java-Quelltextkommentare vermeiden
Umlaute (`Pruefung`), Dokumentation schreibt sie aus. Die Dokumentation erklärt
**warum so und nicht anders** und behält die Sackgassen; ein Weg ohne Umwege
sieht erfunden aus.
