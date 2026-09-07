# Nächste Schritte

Stand: 7. September 2026, Branch `feature/kern-architektur`.

Dieses Dokument ist die Übergabe: **wo das Projekt steht, was als Nächstes
ansteht, und welche Fallstricke schon Zeit gekostet haben.**

Es enthält bewusst **keine Chronik mehr.** Bis zum 06.09.2026 war es auf 1.436
Zeilen angewachsen, gut die Hälfte davon Nacherzählung behobener Fehler — und
das ist die Aufgabe von `git log`. Was von einem behobenen Fehler bleiben muss,
steht dort, wo es jemanden warnt: als Begründung im Quelltext, als Test, oder
hier unter „Merksätze" und „Fallstricke". Alles andere ist abrufbar:

```bash
git log --oneline feature/kern-architektur          # was wann geschah
git log -S "Suchbegriff" -p                          # wann eine Zeile entstand
git show ccaedda                                     # ein einzelner Schritt
```

## Wo das Projekt steht

Erledigt und geprüft:

- Aufteilung in `gkv-core` (ohne JavaFX, per Enforcer erzwungen) und `gkv-ui`
- Hibernate 6.6 mit `jakarta.persistence`, SQLite-Persistenz instand gesetzt
- Validierungsstufe als Tor vor dem Versand, **sieben Regeln**
- Kassen-Kommunikation: Antwortauswertung berichtigt, simulierte Gegenstelle
- Datenablage im Benutzerprofil, eigenständiges Windows-Paket über `jpackage`
- `View` von 1438 auf rund 300 Zeilen zerlegt, fünf Masken herausgelöst
- Oberfläche: Seitenleiste, Listen, Meldungsecke, Stylesheet, Programmsymbol
- Feldprüfung mit Erklärung am Feld, IK gegen die Prüfziffer
- Blaupausen mit einstellbarem Preis je Termin
- **415 Tests** (177 Kern, 238 Oberfläche), `BUILD SUCCESS`, Checkstyle 10 Warnungen
- **Einstellungen** in einer eigenen JSON-Datei, samt Dunkelmodus
- Zwei Reviews über den Branch gelaufen, **alle vierzehn Funde behoben**
- Das Paket ist gebaut und gestartet; das Programm heißt „GKV-Abrechnung"
- Fünf Skills unter `.claude/skills/`, Dokumentation und Diagramme aktuell

**Der Stand liegt auf `main`** (Merge `a596ac5` vom 06.09.2026, von Simon
freigegeben). Weitergearbeitet wird auf `feature/kern-architektur`.

## Sofort zu entscheiden

Drei Punkte, die niemand außer Simon entscheiden kann.

### 1. Der Branch nach `main` — freigegeben

**Erledigt am 06.09.2026.** Simon: *„Dazu darf der aktuelle Stand in den Main
Branch."* Der Stand mit den beiden Reviews und dem Aufräumen liegt auf `main`.
Was hier steht, gilt für den nächsten Zusammenführungsschritt:

```bash
git switch main && git merge --no-ff feature/kern-architektur
```

Zu beachten: die drei Commits `632e906`, `475321d`, `6460b78` stammen aus
`origin/refactor` und tragen weiterhin `Simon.Bausch@steep.de`; alle übrigen
tragen `simi_9@web.de`. Der Sicherungs-Branch `backup/vor-identitaetswechsel`
kann nach dem Merge weg (`git branch -D backup/vor-identitaetswechsel`).

### 2. Vier Branches auf `origin`

Die acht `copilot/*`-Branches sind am 06.09.2026 gelöscht — Simons Auskunft,
das seien Versuche gewesen. Was noch liegt:

| Branch | Stand | Empfehlung |
|---|---|---|
| `dev` | **null eigene Commits**, vollständig in `main` | kann ohne Bedenken weg |
| `restart` | **ein eigener Commit** (30.05.2026) | erst hineinsehen |
| `refactor` | Herkunft von `632e906`, `475321d`, `6460b78` | nach dem Merge entbehrlich |
| `ai` | in der Übergabe nie erwähnt, ungeprüft | erst hineinsehen |

**Nicht ohne ausdrückliche Ansage löschen.** Bei den `copilot/*`-Branches war
die erste Zählung in dieser Datei falsch (sieben statt acht), und jeder trug
Commits, die nicht in `main` lagen — sichtbar wurde das erst beim Nachzählen.

> **Nebenbei gelernt:** dieses Repository lässt höchstens **fünf** Branches je
> Push zu („Pushes can not update more than 5 branches or tags"). Ein
> `git push --delete` mit acht Namen wird als Ganzes abgewiesen.

### 3. Die Retrospektive — gehalten

**Am 06.09.2026 gelaufen.** Simon hat den Stand durchgesehen; das Ergebnis sind
die acht Punkte in Abschnitt K und die Reihenfolge unter „Der Weg nach
Produktion". Sein Urteil zur Oberfläche: **„Sonst sieht es gut aus."**

Nicht angesprochen und damit weiter offen:

1. **Die eigene Titelleiste** (G2). Dagegen entschieden, mit Begründung bei
   `Hauptfenster.fenstertitel`. Umzudrehen kostet etwa einen Tag.
2. **Was ohne Anlage 3 offen bleibt** — Tarifkennzeichen, Positionsnummern,
   echter Versand. Das ist keine Lücke im Programm, sondern eine in den
   Unterlagen, **und es gehört gesagt statt kaschiert.**
3. **Der Preis je Termin.** Eine Blaupause ohne Preis wird seit dem 06.09.2026
   nicht mehr abgerechnet, sondern aufgehalten (siehe „Was zuletzt geschah").
   Wer bestehende Blaupausen hat, sollte einmal hineinsehen.

Die Grenze bei den Info-Zeichen (G5) hat den Durchgang überstanden, ohne
angesprochen zu werden — das ist kein Beweis, aber der erste Hinweis darauf,
dass sie richtig liegt.

## Der Stand aller Abschnitte

Damit sich beim nächsten Öffnen nicht die ganze Datei lesen muss. **Die
Buchstaben bleiben, auch bei erledigten Abschnitten** — Quelltextkommentare
verweisen darauf („Abschnitt H", „G5").

| | Abschnitt | Stand |
|---|---|---|
| **A** | Automatische Abrechnung | **zurückgestellt** — Simons Entscheidung („zu kompliziert"), nicht vergessen |
| **B** | Fachliche Felder | 1–3 ✔ · **4 und 5 blockiert**: dafür braucht es Anlage 3 beziehungsweise den Vertrag |
| **C** | Oberflächentests | ✔ jede Klasse in `gkv-ui` hat einen Test, bis auf `Abrechnungslauf` (siehe unten) |
| **D** | Fachliche Lücken | **blockiert oder groß**: Storno, weitere Leistungsbereiche, echte Rechnungsnummern |
| **E** | Echter Übermittlungsweg | **Konzept steht** (07.09.2026), Umsetzung blockiert: Zertifikate, Zugangsdaten, IK |
| **F** | Kleinigkeiten | ✔ bis auf die Checkstyle-Befunde — die verlangen einen Eingriff in die Entitäten |
| **G** | Aussehen und Bedienung | ✔ vollständig (G1 Bildlaufleisten, G2 Kopfzeile, G3 Symbol, G4 Kürzungen, G5 Info-Zeichen, G6 Statuszeile) |
| **H** | Bedienwerkzeug | ✔ `Bedienung` unter `src/test/java`, siehe unten |
| **I** | Der Name | ✔ „GKV-Abrechnung", samt Umzug des Datenordners |
| **J** | Der Zwischenstand | ✔ angesehen und durchgespielt, die Rückmeldung steht als **K** |
| **K** | Simons Rückmeldung | ✔ alle acht, einschließlich Einstellungsseite und Dunkelmodus |

**Alles, was ohne Anlage 3, ohne den Vertrag und ohne Zertifikate zu machen
war, ist gemacht.** Was offen bleibt, ist entweder eine Entscheidung, eine
Lücke in den Unterlagen oder ein Vorhaben eigener Größe.

## Was offen ist

### A. Automatische Abrechnung — zurückgestellt, nicht vergessen

Der Wunsch bleibt sinnvoll, sobald das Übrige steht. Was dafür nötig wäre:

1. Eine **Kursvorlage** speichert Blaupause, Gruppe und Terminzahl. Neue
   Entität in `gkv-core/entity`, Schema wächst über `hbm2ddl=update` mit.
2. Ein **Lauf-Verzeichnis** hält fest, wann welcher Kurs zuletzt abgerechnet
   wurde — Voraussetzung für „Vormonat übernehmen" und für jede Fälligkeit.
3. Erst darauf lässt sich eine Fälligkeitsanzeige beim Programmstart bauen.

Von einem Versand **ohne Rückfrage** ist abzuraten: es gehen echte Forderungen
an die Kasse, eine falsche Terminzahl bemerkt niemand vor der Ablehnung, und
ein Storno gibt es fachlich noch gar nicht (D).

Bis dahin helfen die Werkzeuge in der Abrechnungsmaske: „Alle auswählen",
„Termine für alle" und die mitrechnende Zusammenfassung.

### B4/B5. Zwei Fragen, die Anlage 3 oder der Vertrag beantworten muss

**B4 — Für die Abrechnungspositionsnummer gibt es keine Liste.** Nachgesehen:
im Projekt existiert keine. `Information/codes/03_leistungscodes_sgs_h.json`
führt `306050601` nur als **Beispiel** mit dem Vermerk „Muss
vertraglich/fachlich gültig sein". Die echte Liste steht in **Anlage 3,
Abschnitt 8.2**. Sie dort zu entnehmen und als JSON abzulegen wäre lohnend:
daraus würde ein Auswahlfeld *mit* Inhalt und aus der bloßen Formprüfung eine
echte `PositionsnummerRegel`. **Nicht zu raten** — eine erfundene
Positionsnummer führt zur Zurückweisung der ganzen Lieferung.

**B5 — Woher kommt das Tarifkennzeichen, und wovon hängt es ab?** Muss vor dem
ersten echten Versand beantwortet sein. Belegt ist: fünfstellig numerisch,
zweiter Teil des Kompositfelds `Abrechnungscode:Tarifkennzeichen` im `ENF`,
Herkunft laut Unterlagen **Vertrag plus Anlage 3, Schlüssel 8.1.5.2**.
Personenabhängig ist es nicht — es steht in der Leistungszeile, nicht bei der
versicherten Person.

Offen ist, **wovon** es abhängt, und davon hängt ab, wohin es gehört:

| Wovon es abhängt | Wohin es gehört | Aufwand |
|---|---|---|
| von nichts, immer gleich | `internal` in `enf.json`, wie der Abrechnungscode | klein |
| vom Dienstleister | Feld an `ServiceProvider` | mittel |
| von der Kasse | eigene Zuordnungstabelle Kasse → Kennzeichen | groß |

Simons Vermutung — es hänge am Dienstleister — ist die plausibelste: das
Kompositfeld heißt `Leistungserbringergruppe`, und den Vertrag schließt die
Leistungserbringerin. **Erst die Antwort, dann der Umbau.** Alle drei sind
machbar, zwei davon wären falsch. Solange die Frage offen ist, sollte niemand
mehrere Blaupausen nur wegen des Kennzeichens anlegen; für **einen**
Dienstleister mit **einem** Vertrag liefert die heutige Lösung (in der
Blaupause) richtige Ergebnisse.

Dieselbe Frage hängt an **B3**: ist der Abrechnungscode je Vorlage fest, gehört
er als `internal`-Feld in `enf.json` statt ins Formular. Der Umbau wäre klein;
ihn ohne die Antwort zu machen hieße, eine Tür zuzumauern.

### C. Was in `gkv-ui` ohne eigenen Test bleibt

Nur noch `Abrechnungslauf` — **und das mit Absicht.** Es ist eine Schnittstelle
mit einer Methode und ohne Rumpf; ein Test würde eine Lambda bauen und sie
aufrufen, also die JVM prüfen und nicht das Projekt. Dass die Signatur zu
`AbrechnungService::createAndDispatch` passt, sichert der Übersetzer. Ein Test
dort erhöhte die Zahl und nicht die Sicherheit.

Die Suche nach totem Quelltext lohnt sich gelegentlich wieder und kostet eine
Zeile (zuletzt am 06.09.2026 gelaufen, ohne Fund):

```bash
for f in $(find gkv-core/src/main/java gkv-ui/src/main/java -name "*.java"); do
  n=$(basename "$f" .java)
  [ "$(grep -rl "\b$n\b" --include=*.java gkv-core/src gkv-ui/src | grep -vc "/$n.java$")" = 0 ] \
    && echo "unreferenziert: $f"
done
```

### D. Fachliche Lücken

Nach Nutzen geordnet:

1. **Stornierung und Nachberechnung** — heute gar nicht vorhanden. Sobald real
   abgerechnet wird, wird das gebraucht.
2. **Weitere Leistungsbereiche** — abgedeckt ist nur SGS H mit Abrechnungscode
   `61`. Die Struktur steht, es fehlen die Daten aus Anlage 3.
3. **Positionsnummern und Tarifkennzeichen** werden auf Form, nicht auf
   fachliche Zulässigkeit geprüft. Dafür bräuchte es die Schlüsseltabellen aus
   Anlage 3 als JSON — dann wäre es eine weitere `ValidationRule`.
4. **Rechnungsnummer und Belegnummer sind fest verdrahtet.** `DtaFactory`
   schreibt `REC+00000000:0` und leitet die Belegnummer aus Zeit und laufender
   Nummer ab. Eine echte Sammel- und Einzelrechnungsnummer führt das Programm
   nicht. Vor einem echten Versand zu klären.

### E. Echter Übermittlungsweg — durchdacht am 07.09.2026

**Das Konzept steht jetzt in der Dokumentation**, Kapitel „Der Weg zur Kasse".
Grundlage ist die Technische Anlage (Anlage 1, Version 21), die im Projekt
liegt und bis dahin niemand für diese Frage gelesen hatte. Hier nur, was daraus
für die Arbeit folgt.

#### Drei Annahmen im Projekt waren falsch

**1. Empfänger ist nicht die Kasse, sondern die Datenannahmestelle.** Die
Technische Anlage ist eindeutig: *„Für jede Datenannahmestelle mit
Entschlüsselungsbefugnis ist je Kassenart eine Nutzdatendatei (UNB bis UNZ) zu
erstellen."* Unser `billing-office-endpoints.json` führt 23 einzelne Kassen und
schreibt deren IK als Empfänger ins UNB. **Für den Dateiversand ins
Testverzeichnis ist das folgenlos, für einen echten Versand ist es falsch.**
Die Zuordnung Kasse → Annahmestelle steht in der **Kostenträgerdatei** der
Kassenart, die wir nicht haben.

**2. Die Auftragsdatei fehlt.** Zu jedem Übermittlungsvorgang gehört neben der
Nutzdatendatei eine Auftragsdatei; sie ist in den „Richtlinien für den
Datenaustausch mit den gesetzlichen Krankenkassen" beschrieben — eine
Unterlage, die im Projekt ebenfalls fehlt.

**3. Das Testkennzeichen stand fest auf „Erprobung".** *Erledigt am
07.09.2026*, siehe unten.

#### Was zu beschaffen ist, und was das für die Reihenfolge heißt

Nichts davon lässt sich programmieren: Betriebsstätten-IK, Zertifikat einer
anerkannten Stelle, Zugangsdaten, Kostenträgerdatei, und die beiden fehlenden
Anhänge (Übermittlungsverfahren und Testverfahren).

**Die Empfehlung steht in der Dokumentation: erst C, dann B, dann A.**
Erzeugen und prüfen (heute), dann über eine Abrechnungsstelle abrechnen
(Rechnungsart 2 ist dafür vorgesehen), und selbst übermitteln erst, wenn das
Übrige läuft. Der Grund ist nicht Bequemlichkeit: **die Beanstandungen, die
über eine Abrechnungsstelle zurückkommen, sind die beste Vorbereitung auf die
Erprobung.**

#### Wie sich das mit den Kassen erproben lässt

Das war Simons vierte Frage, und die Antwort ist erfreulich: **es ist ein
vorgesehener Schritt, kein Behelf.** Die Technische Anlage schreibt vor, dass
vor der ersten Übermittlung die Einzelheiten mit dem Empfänger abzustimmen und
die ordnungsgemäße Verarbeitung zu **erproben** ist. Dazu kommt ein eigenes
Testverfahren (Abschnitt 10), beschrieben in einem Anhang, der uns fehlt.

Praktisch heißt das: **eine** Datenannahmestelle ansprechen, das Verfahren
abstimmen, mit `uebermittlungsart = erprobung` liefern, die Rückmeldungen
auswerten — und erst danach auf `echt` stellen.

#### Zwei Pflichten, die nicht in der Datei stehen

Die Technische Anlage verlangt eine **Dokumentation des Datenaustauschs**, zwei
Jahre aufzubewahren, und eine **Sicherungskopie bis zur Bezahlung**. Beides
bildet das Programm heute nicht ab, und beides gehört zum Weg in den
Echtbetrieb. Das ist kein großer Umbau — die erzeugten Dateien liegen bereits
in `staging`, es fehlt der Nachweis darüber, wann was wohin ging und was
zurückkam.

### F. Kleinigkeiten

- **Checkstyle: 10 Warnungen** (`mvn checkstyle:check`), von rund vierzig am
  05.09.2026. Übrig sind nur die, die eine Entwurfsentscheidung verlangen:

  | Regel | Anzahl | Wo |
  |---|---|---|
  | `ParameterNumber` | 5 | `Patient`, `Person`, `ServiceProvider`, `FieldDefinition` (je 9), `EditFormController` (12) |
  | `CyclomaticComplexity` | 5 | `BetragskonsistenzRegel` (11), `Feldbau` an zwei Stellen (je 12), beide `…FieldPopulator` (je 16) |

  Die vier Konstruktoren mit neun Parametern sind der eigentliche Befund: eine
  Person hat mehr Eigenschaften, als ein Konstruktor tragen sollte. Ein Builder
  oder ein `record` für die Anschrift wäre die Antwort — **das ist ein Eingriff
  in die Entitäten und gehört nicht nebenbei erledigt.** Die 216 Hinweise sind
  `INFO` (fehlende `@param`, Ternär-Operatoren) und keine Warnung.
- **`msg.invalidNumbers` ist praktisch unerreichbar.** PLZ und IK sind laut
  `person-tags.json` `NUMBER`; seit sie eine Höchstlänge tragen, sind sie
  Textfelder, und die Prüfung greift vorher. Der Zweig bleibt, weil sich die
  Feldbeschreibung ändern kann.
- **`-Xlint:all` ist im Bau eingeschaltet** (Eltern-`pom.xml`), bewusst **ohne**
  `-Werror`.
- **CI baut mit JDK 21**, lokal wird JDK 25 genutzt. Bewusst, weil das Projekt
  `release=21` setzt — bei Problemen aber die erste Verdachtsstelle.
- **`.docx` wird nicht automatisch erzeugt.** Nach Änderungen an
  `GKVTransmitter_Dokumentation.md` neu erzeugen, siehe README.

### K. Simons Rückmeldung vom 06.09.2026 (Abend)

Nach dem ersten eigenen Durchgang. Acht Punkte, dazu das Urteil: **„Sonst sieht
es gut aus."**

| | Punkt | Stand |
|---|---|---|
| **K1** | Beim Umsatzsteuersatz fehlt das `%` hinter der Zahl | ✔ 06.09.2026 |
| **K2** | Felder, die niemand ausfüllen soll, sehen aus wie Eingabefelder | ✔ 06.09.2026 |
| **K3** | Beim Einzelbetrag fehlt das `€` — und entsprechend bei anderen Feldern | ✔ 06.09.2026 |
| **K4** | Nach dem Speichern einer Blaupause steht der Vorlagenname als Überschrift | ✔ 06.09.2026 |
| **K5** | Ein Geburtsdatum lässt sich in die Zukunft legen | ✔ 06.09.2026, **nachgebessert** (K5a) |
| **K6** | Um das Aufklappfeld liegt ein zweiter, unnötiger Rahmen | ✔ 06.09.2026 |
| **K7** | Die Eingabefelder verschwinden auf der hellen Fläche | ✔ 06.09.2026 |
| **K8** | Eine Seite „Einstellungen" mit Dunkelmodus | ✔ 07.09.2026 |

**Alle acht sind erledigt.** Was dabei zu entscheiden war, steht bei den
Punkten selbst.

**K1 und K3 — die Einheit gehört ans Feld.** Beides steht heute nur in der
Erklärung darunter („mit Komma und zwei Nachkommastellen"), und die ist
eingeklappt, sobald sie nicht zu `STETS_ERKLAERT` gehört. Die Einheit steht in
den Segmentdefinitionen schon fest: `inputType: PERCENT` heißt Prozent,
`COST` heißt Euro. Sie muss nicht gepflegt, nur angezeigt werden.

**K2 — was das Programm selbst setzt, ist kein Eingabefeld.** Betroffen ist der
**Abrechnungscode**: er trägt genau einen hinterlegten Wert (`61`), steht
vorbelegt im Formular und sieht aus, als wäre er zu ändern. Simon nennt selbst
beide Wege: kennzeichnen oder ganz in den Hintergrund. **Der zweite ist die
bessere Lösung und hängt an B3** — ob der Code je Vorlage fest ist, steht in
Anlage 3. Bis das beantwortet ist, wird gekennzeichnet: ein Feld, das man lesen
und nicht bearbeiten soll, sieht auch so aus. Das mauert keine Tür zu.

*Erledigt.* Das Feld ist `setEditable(false)` mit eigener Stilklasse — kein
Rahmen, ruhig getönt, stillere Schrift. Bewusst **nicht** `setDisable(true)`:
ausgegraut ließe es sich nicht mehr markieren und kopieren.

**Hier stand bis dahin die entgegengesetzte Begründung** („das Feld bleibt
bewusst beschreibbar, ein gesperrtes wäre eine Sackgasse"). Sie fällt mit der
Mechanik selbst weg: gesperrt ist das Feld nur, solange die Liste **einen**
Eintrag hat. Bringt Anlage 3 einen zweiten Code, wird daraus von selbst wieder
ein beschreibbares Auswahlfeld — ohne dass jemand etwas ändert. Ein Test hält
beide Fälle fest.

**K4 — die Überschrift bleibt stehen.** `BlaupausenMaske` ruft nach dem
Speichern `rahmen.zeige(liste())`. Das tauscht den Inhalt aus, mehr nicht: die
Kopfzeile, die Seitenleiste und der Fenstertitel gehören zum *Bereich*, und der
ist weiterhin die Vorlage, aus der man hereingekommen ist. Der Rahmen braucht
einen zweiten Weg — „wechsle in diesen Bereich" statt „zeige das hier".

**Achtung dabei:** der Bereichswechsel räumt die Meldungsecke. Wer erst meldet
und dann wechselt, löscht seine eigene Erfolgsmeldung.

**K5 — ein Geburtsdatum in der Zukunft.** Im Kern gibt es die Regel längst
(`VERSICHERTENANGABEN`, Code `GEBURTSDATUM_ZUKUNFT`), aber sie greift erst vor
dem Versand. Am Feld darf so etwas gar nicht erst entstehen. Simons zweiter
Gedanke — „eventuell keine 1-Jährigen" — ist richtig, verlangt aber Maß:
**eine Altersgrenze, die zu hoch liegt, weist eine echte Teilnehmerin ab.**
Fünfzehnjährige Mütter gibt es. Die Grenze gehört deshalb dorthin, wo sie nur
noch Tippfehler trifft, nicht Lebensläufe.

*Erledigt, zweifach.* Der Kalender lässt unmögliche Tage gar nicht erst
anklicken (**ein Fehler, der nicht entsteht, muss auch nicht erklärt werden**),
und die Feldprüfung fängt ab, was daran vorbeikommt — über
`EntityFieldPopulator` kommen Werte herein, die nie durch den Kalender gegangen
sind. Die Grenzen stehen als `Feldbau.MINDESTALTER` (10) und `HOECHSTALTER`
(120). Sie hängen am Feld**namen**, nicht an `InputOption.DATE`: ein
Leistungsdatum darf in der Zukunft liegen, und eine Regel über alle
Datumsfelder wäre beim nächsten Terminfeld im Weg.

**K5a — nachgebessert am selben Abend, auf Simons Hinweis.** Die erste Fassung
sperrte im Kalender **nur die Zukunft**, während die Prüfung zusätzlich das
Alter maß. Wer einen Tag von vor zwei Jahren anklickte, durfte ihn wählen — und
bekam danach eine rote Zeile. Simon: *„bis zu einem gewissen Punkt macht es
keinen Sinn auszuwählen, dann soll es auch nicht möglich sein eins
auszuwählen, was im Programm zu Fehlern führt."*

> **Merksatz:** eine Auswahl anzubieten und sie anschließend abzulehnen, ist
> schlechter als sie gar nicht erst anzubieten. Wer beides prüft — die
> Auswahlmöglichkeit und den Wert —, muss beides aus **einer** Regel speisen.

Umgesetzt als `Feldbau.befundZu(LocalDate)`, die einzige Stelle, die über ein
Geburtsdatum entscheidet; Kalender und Prüfung gehen beide hindurch. Ein Test
vergleicht sie Tag für Tag und wird rot, sobald jemand eine Grenze nur an einer
Stelle ändert.

**Und die Erklärung stimmte nicht mehr.** Hinter dem „i" stand weiterhin
„Geburtsdatum der teilnehmenden Person." — kein Wort davon, was wählbar ist,
obwohl das Feld seit demselben Tag Grenzen hat. Sie nennt sie jetzt. *Dritter
Fall derselben Art in drei Tagen* (Maskenkopf-Begründung, Maskenkopf-Kurzfassung,
jetzt ein Hilfetext): **wer eine Regel schärft, muss den Satz mitnehmen, der
sie erklärt.**

**K6 — der zweite Rahmen.** Nachgesehen, und es ist erklärbar: ein
beschreibbares `ComboBox` und ein `DatePicker` enthalten *innen* ein
`.text-field`, und `gkv.css` gibt jedem `.text-field` einen Rahmen. Zwei
Rahmen, einer im anderen. Bei den nicht beschreibbaren Auswahlfeldern der
Abrechnungsmaske tritt es deshalb nicht auf.

**K7 — die Felder verschwinden.** Weiß auf einem fast weißen Grund
(`#FFFFFF` auf `#F4F6F8`) mit einer sehr hellen Linie (`#DDE3E8`). Simons Wort
war „dezenter", sein Grund „sie verschwinden" — gemeint ist erkennbar:
**ruhig, aber deutlich abgesetzt.** Das ist eine Frage von zwei Farbwerten.

*Erledigt* über einen eigenen Wert `-farbe-feld-linie` (`#B3BFCA`). Er wird
**nur** für Feldkanten benutzt: eine Feldkante und eine Trennlinie haben
verschiedene Aufgaben, und wer sie an denselben Wert bindet, kann nur noch
beide zugleich ändern.

**Beim Nachsehen am Bild fiel eine zweite Sache auf**, die kein Test gefunden
hätte: mit der Einheit dahinter waren die Felder mit `%` und `€` zwanzig Punkte
schmaler als die übrigen, und im Blaupausenformular lief die rechte Kante
sichtbar aus. Dieselbe Falle wie beim Info-Zeichen, dieselbe Antwort — die
Einheit hat jetzt eine feste Spalte, die auch dann Platz hält, wenn nichts
darin steht.

**K8 — Einstellungen mit Dunkelmodus.** Der einzige große Punkt.

Die Farben selbst sind der kleinste Teil: sie stehen als benannte Werte in
einem Block oben in `gkv.css` und werden unten nur benutzt. Ein zweiter Block,
umgeschaltet über eine Stilklasse an der Wurzel, ist der ganze Kern. Dazu eine
neue Maske nach dem Muster von `GruppenMaske` (**nicht** in `View`) und ein
Eintrag in `View.baueNavigation()`.

### Das Problem an der Einstellungsseite — gelöst am 07.09.2026

**Die Anwendung hatte keinen Ort für Einstellungen.** Nichts, was jemand einmal
wählte, überstand den Programmstart. Eine Einstellungsseite ohne diesen Ort
wäre eine Seite, die beim nächsten Start vergisst, was man ihr gesagt hat;
**das ist schlechter als keine Seite.** Das war der eigentliche Aufwand an K8,
nicht der Dunkelmodus.

**Simons Entscheidung: die eigene JSON-Datei** — *„ähnlich wie die Datenbank,
die ja auch nicht in der EXE liegt."* Genau der Punkt: sie liegt im Datenordner
und erbt damit alles, was dafür schon geregelt ist — den Umzug beim Umbenennen
der Anwendung, die Sicherung, und die Umlenkung über `gkv.home` in Tests und
Werkzeugen.

Die drei erwogenen Wege, mit ihren Folgen:

| Ort | Dafür | Dagegen |
|---|---|---|
| **Tabelle in der SQLite-Datei** | liegt beim Rest der Daten, wird mitgesichert, `hbm2ddl=update` legt sie an | Einstellungen sind keine Fachdaten; wer die Datenbank austauscht, verliert sie |
| **Eigene Datei im Datenordner** (`einstellungen.json`) | unabhängig von der Datenbank, von Hand zu lesen und zu berichtigen | eine zweite Ablage mit eigenen Fehlerfällen (gesperrt, unlesbar, halb geschrieben) |
| **`java.util.prefs`** | nichts selbst zu bauen | landet in der Windows-Registry, entzieht sich `Anwendungsverzeichnis` und damit jedem Umzug und jeder Sicherung |

#### Was daraus wurde

`einstellung.Einstellungen` liest und schreibt `einstellungen.json` über
`Anwendungsverzeichnis`; `EinstellungenMaske` zeigt sie. Vier Punkte standen
vorher fest und haben sich gehalten:

- **Ein Fehlschlag beim Lesen hält den Start nicht auf.** Vorgaben nehmen und
  weiterlaufen — wie `Leistungsparameter` bei unlesbarer Blaupause.
- **Geschrieben wird über eine Nebendatei**, die anschließend umbenannt wird.
  Sonst bliebe nach einem Abbruch mitten im Schreiben eine halbe Datei zurück,
  und die vorige Fassung wäre weg, obwohl sie gereicht hätte.
- **Die Umschaltung greift sofort.** Ein Schalter, der erst beim nächsten Start
  wirkt, sieht aus wie ein kaputter.
- **Scheitert das Speichern, wirkt die Wahl trotzdem** und die Meldungsecke
  sagt, dass sie die Sitzung nicht überlebt. Die Wirkung darf nicht davon
  abhängen, ob ein Ordner beschreibbar ist.

**Und die dunkle Fassung ist mehr als ein zweiter Farbblock.** Die Vorschau hat
am ersten Bild zwei Stellen gefunden, die kein Test gefunden hätte: die Schrift
in den Auswahlfeldern und die Namen in allen Listen standen dunkelgrau auf
Anthrazit. Beides kam aus JavaFX' eigenem Stylesheet, das für `.label` und
`.text-input` eine von einem hellen Grund abgeleitete Farbe mitbringt — die
Angabe an `.root` wirkt darauf nicht.

> **Merksatz:** ein Dunkelmodus ist nicht fertig, wenn es ihn gibt. Was im
> Hellen knapp lesbar ist, verschwindet im Dunkeln — und das sieht kein Test.
> `Vorschau` zeichnet deshalb beide Fassungen.

**Der zweite Kandidat für die Datei war sofort da**, und er ist der wichtigere:
die **Übermittlungsart** (Test / Erprobung / Echt). Sie stand bis dahin fest im
Quelltext, siehe E.

## Der Weg nach Produktion

Simons Reihenfolge vom 06.09.2026, wörtlich: erst nach `main`, dann
ausführlichere Tests, dann Planung.

1. **Der Stand darf nach `main`.** Ausdrücklich freigegeben. Damit ist der
   erste Punkt unter „Sofort zu entscheiden" beantwortet.
2. **Die Tests sollen ausführlicher werden.** Heute sind es 372, und sie decken
   die riskanten Stellen — was fehlt, ist nicht Menge, sondern Art: Abläufe
   über mehrere Masken (`AblaufTest` ist der einzige), Fehlerpfade, und die
   Fälle, die aus einer echten Abrechnung kommen und nicht aus einer erdachten.
   **Vor dem Ausbau gehört festgelegt, was ein Test hier beweisen soll** —
   sonst wächst die Zahl und nicht die Sicherheit, siehe `Abrechnungslauf`.
3. **Datensicherheit.** Zu planen, nicht nebenbei zu machen — siehe unten.
4. ~~**Wie sich das mit den Kassen erproben lässt.**~~ **Beantwortet am
   07.09.2026**, siehe Abschnitt E und das Kapitel „Der Weg zur Kasse" in der
   Dokumentation. Kurz: die Erprobung ist ein **vorgeschriebener Schritt** des
   Verfahrens, kein Behelf. Was dafür zu beschaffen ist, steht dort — und es
   sind Fragen an Menschen, nicht an den Quelltext.

### Datensicherheit — der Stand und die Reihenfolge

Die Anwendung hält **besondere Kategorien personenbezogener Daten** nach
Art. 9 DSGVO: Namen, Geburtsdaten, Anschriften, Versichertennummern,
Kassenzugehörigkeit, dazu die Tatsache, dass jemand einen
Geburtsvorbereitungskurs besucht. Was heute gilt:

| | Stand |
|---|---|
| Datenbank | **unverschlüsselt** im Benutzerprofil |
| Erzeugte DTA-Dateien | **unverschlüsselt** im Ausgangsordner, unbegrenzt lange |
| Anmeldung | keine — wer den Rechner öffnet, öffnet die Daten |
| Protokoll, wer was geändert hat | keines |
| Sicherung | keine; die Technische Anlage verlangt eine bis zur Bezahlung |
| Löschfristen | keine |

**Die Reihenfolge sollte nicht nach Aufwand gehen, sondern nach Wirkung.** Der
Rechner selbst ist die erste Verteidigungslinie: **BitLocker auf dem
Arbeitsgerät** schützt gegen den häufigsten realen Fall — ein verlorenes oder
gestohlenes Notebook — und kostet keine Zeile Quelltext. Danach:

1. **Aufbewahrung begrenzen.** Erzeugte DTA-Dateien sind Kopien von
   Gesundheitsdaten. Sie brauchen eine Frist und einen Ort, der mitgesichert
   wird — und nach der Bezahlung nichts weiter.
2. **Sicherung.** Sie ist ohnehin vorgeschrieben (Anlage 1, Abschnitt 3) und
   fehlt bisher ganz. Eine Sicherung, die neben der Datenbank liegt, ist keine.
3. **Verschlüsselung der Datenbank** (etwa SQLCipher). Wirkt aber nur, solange
   die Anwendung *nicht* läuft, und verlangt eine Schlüsselverwaltung, die
   heute niemand hat. **Nach BitLocker der kleinere Gewinn.**
4. **Anmeldung und Protokoll** — sinnvoll erst, wenn mehr als eine Person mit
   dem Programm arbeitet. Bei einer Einzelpraxis wäre es Aufwand ohne Schutz.

**Was ohnehin gilt und nichts kostet:** Zugangsdaten und Schlüssel gehören
nicht in `einstellungen.json` (Klartext im Benutzerprofil), sondern in den
Anmeldeinformationsspeicher des Betriebssystems. Das steht als Warnung im
Quelltext von `Einstellungen`.

## Was zuletzt geschah (07.09.2026)

Drei Dinge: die Einstellungsdatei, der Weg zu den Kassen — und ein Fund, der
aus dem zweiten in den ersten hineinragte.

**Die Einstellungen liegen jetzt in `einstellungen.json`**, neben der
Datenbank, mit Dunkelmodus und Einstellungsseite. Einzelheiten unter K8.

**Das Übermittlungskonzept steht** (Abschnitt E und das neue Kapitel „Der Weg
zur Kasse" in der Dokumentation). Grundlage war die Technische Anlage, die seit
Juli im Projekt liegt und für diese Frage niemand gelesen hatte. Sie hat drei
Annahmen widerlegt.

**Der Fund dazwischen: das Testkennzeichen.** Die letzte Stelle des UNB sagt,
wofür sich eine Datei ausgibt — 0 Test, 1 Erprobung, 2 Echt. Bei uns stand dort
seit jeher eine fest verdrahtete **1**.

Das war zum Stand des Programms sogar richtig. Der Punkt ist ein anderer:
**niemand konnte es sehen, und niemand konnte es ändern.** Beim ersten
Echtversand wäre es die stillste denkbare Zurückweisung gewesen — die Datei ist
formal fehlerfrei, wird angenommen, verarbeitet, und nicht bezahlt, weil sie
sich selbst als Erprobung ausweist. Kein Prüfbericht hätte etwas gemeldet.

> **Merksatz:** eine fest verdrahtete Angabe ist nicht deshalb harmlos, weil
> sie stimmt. Sie ist es erst, wenn jemand sie sehen kann. Was über Bezahlung
> oder Nichtbezahlung entscheidet, gehört dorthin, wo man es liest.

Die Angabe steht jetzt in den Einstellungen, mit `erprobung` als Vorgabe — auch
bei fehlender oder unlesbarer Datei. **Ein Programm, das Forderungen an
Krankenkassen stellt, wechselt nicht von selbst in den Echtbetrieb.**

## Was davor geschah (06.09.2026, Abend)

Aufräumen im ganzen Projekt, und dabei drei Funde.

**Vier Dokumente entfernt**, die einen Stand beschrieben, den es nicht mehr
gibt: `CODE_QUALITY_BASELINE_v1.md` (15.04.2026, „Testbarkeit 25/100") und
`PROGRESS_UPDATE_v2/v3/v4.md`. Sie verglichen eine Baseline mit einer
Entwicklung, die längst weitergelaufen ist, standen in keinem Verzeichnis der
README und wurden von nichts verlinkt. Dazu `UML.puml` im Wurzelverzeichnis
(Zwischenstand vom 14.04.2026, ersetzt durch `Information/Projektklassen.puml`)
und ein `.gitkeep` in einem Ordner mit 30 Dateien.

**`Information/test.DTA` war Byte für Byte dieselbe Datei wie `Valide.DTA`** —
zwei Namen für einen Inhalt, gepflegt wurde nur einer. Die Erklärung daneben
heißt jetzt `Abrechnung_Datenerklaerung.md` und verweist auf die Referenz.

**Die Dokumentation begann mit drei verirrten Tabellenzeilen** — einer Kopie
aus ihrer eigenen Mitte, oberhalb des YAML-Kopfes. Damit stand der Kopf nicht
mehr am Dateianfang und war für Pandoc keiner: Titel, Untertitel und
Inhaltsverzeichnis der Word-Fassung kamen aus einem Block, der als Fließtext
gelesen wurde. Beim Nachsehen fiel zweierlei Veraltetes auf: „sechs
mitgelieferte Regeln" (es sind sieben) und eine Zeile über `Dialoge`, die es
seit dem Umbau auf `Meldungen` nicht mehr gibt.

**Und der letzte offene Punkt aus D ist zu:** `Leistungsparameter.VORBELEGUNG`
setzte den Einzelbetrag auf **15.000,00**, wenn die Blaupause keinen Preis
führte — den Betrag aus der Beispieldatei. Das kam durch jede Prüfung, weil die
Nachricht in sich stimmig war: ENF, BES und GES rechneten alle mit demselben
erfundenen Wert. Die Oberfläche lässt eine Blaupause ohne Preis seit dem
05.09.2026 nicht mehr speichern, aber ältere Blaupausen, eine unlesbare
Nutzlast und fremde Nachrichten gingen daran vorbei.

Jetzt ist die Vorbelegung **null**, und `LeistungspositionRegel` weist eine
Position mit Betrag oder Menge 0,00 als Fehler zurück. Null ist hier kein
besserer Preis, sondern einer, der nicht durchkommt. Dieselbe Entscheidung wie
beim fehlenden Geburtsdatum: lieber eine Abrechnung, die nicht losgeht und sagt
warum.

Nebenbei stellte sich heraus, dass **die Tests denselben erfundenen Wert
benutzten**: zwölfmal stand dort eine Blaupause `"{}"`, und die Summen, gegen
die geprüft wurde, stammten aus der Vorbelegung. `Testblaupause.mitPreis()`
trägt den Preis jetzt ausdrücklich — die Tests bekommen ihre Zahl von derselben
Stelle wie die Anwendung. Ein Test verlangte außerdem ausdrücklich, dass eine
Abrechnung über **null Termine** versandfähig sei; er prüft jetzt beides
getrennt: die Summen stimmen, hinaus geht sie trotzdem nicht.

**98 DTA-Dateien im Versandordner des Benutzers.** Beim Nachsehen, wohin der
Durchlauf schreibt: `%LOCALAPPDATA%\GKV-Abrechnung\dta_output\outbox` enthielt
98 Abrechnungsdateien aus Testläufen, verteilt über 23 Kassenordner, dazu 53 in
`staging` — vom 04. bis 06.09.2026, und **von einer echten Lieferung nicht zu
unterscheiden.** Denselben Weg ging `AblaufTest`, also jeder `mvn test`.

Die Ursache ist dieselbe wie bei den sechs Testdatenbanken vom Vortag, nur eine
Ebene höher: `billing-office-endpoints.json` gibt seine Ziele relativ an, und
`Anwendungsverzeichnis` löst sie gegen den Datenordner der Anwendung auf. Die
Werkzeuge lenkten bisher nur `gkv.db.path` um — die Datenbank lag richtig, die
Ausgabe daneben.

`Bedienung.aufbauen` und `Vorschau` setzen jetzt **`gkv.home`** auf ihr
Zielverzeichnis und lenken damit den ganzen Datenordner um: Datenbank,
Zwischenablage und Ausgangsordner. Nachgeprüft — nach einem vollen `mvn clean
test` und zwei Durchläufen steht der echte Ordner unverändert bei 98 Dateien,
und die neue Abrechnung liegt unter `target/ablauf/dta_output/`.

> **Merksatz:** wer einen Pfad umlenkt, muss den Ordner umlenken. Eine
> Anwendung schreibt mehr als eine Datei, und die zweite fällt erst auf, wenn
> jemand hinsieht.

**Die 98 Dateien liegen noch dort** — sie zu löschen ist Simons Entscheidung,
nicht meine. Der Ordner heißt „Ausgang", und was darin liegt, sieht aus wie
gestellte Forderungen.

Kleinigkeit am Rande: `status.info` lag seit dem 05.09.2026 unbenutzt in
`ui-messages.json` — der Kurzhinweis für das „i" neben der Statuszeile. Ein
Zeichen ohne Erklärung ist dieselbe Falle wie eine Erklärung ohne Zeichen; er
hängt jetzt als Kurzhinweis daran.

## Die zwei Werkzeuge, die keine Tests sind

`Vorschau` **zeichnet**, `Bedienung` **bedient**. Beide liegen unter
`gkv-ui/src/test/java/.../presentation/`, beide laufen nur von Hand, und beide
haben an ihrem ersten Tag Fehler gefunden, die kein Test gefunden hätte:

| Werkzeug | findet | Beispiel |
|---|---|---|
| `Vorschau` | was falsch **aussieht** | „Lösc…" neben „Bearbei…", ein unsichtbares Info-Zeichen |
| `Bedienung` | was falsch **passiert** | acht Termine wurden zu einem, das Bearbeiten schrieb nichts |

Der gemeinsame Nenner: **beide gehen den Weg, den jemand am Bildschirm geht.**
Ein Test, der sich sein Feld selbst zusammensteckt oder einen Wert am Editor
vorbei setzt, prüft einen Weg, den niemand nimmt.

### Die Befehlssprache von `Bedienung` (Abschnitt H)

```
oeffne Teilnehmer            einen Bereich aus der Seitenleiste
klick person-neu             eine Schaltfläche über ihre Kennung
setze person-feld-firstname Anna
setze person-feld-birthDate 1990-04-17     ein Datum als JJJJ-MM-TT
lies person-feld-plz         schreibt den Wert des Feldes
zeige                        alles, was auf dem Bildschirm steht
erwarte Anna Berger          irgendwo auf dem Bildschirm
erwarte-meldung gespeichert  in der Meldungsecke
erwarte-nicht Fehler         nirgends auf dem Bildschirm
bild schritt3                ein PNG ins Zielverzeichnis
#                            eine Zeile Anmerkung
```

Zwei Abläufe liegen unter `gkv-ui/src/test/resources/ablaeufe/`:
`ein-monat.txt` (fährt `AblaufTest`, gegen eine leere Datenbank) und
`durchlauf-mit-bildern.txt` (der Durchlauf aus `Information/Durchlauf.md`, mit
elf PNG).

**`zeige` ist der wichtigste Befehl.** Er schreibt allen sichtbaren Text
mitsamt den Kennungen — wer die Anwendung nicht vor sich hat, sieht damit, was
*dasteht*, und nicht, was dem Quelltext nach dastehen müsste. Umgekehrt gilt:
**eine gerade erst gezeigte Meldung fehlt auf dem Bild**, weil die Meldungsecke
ihre Karten einblendet und die Überblendung einen Zeichentakt braucht.

**Warum das Werkzeug unter `src/test/java` liegt:** dieser Quelltext ist im
ausgelieferten Paket gar nicht vorhanden. Der beste Weg, sicherzustellen, dass
eine Anwendung mit echten Forderungen an Krankenkassen nicht fernsteuerbar ist,
ist, die Fernsteuerung nicht mitzuliefern.

## Merksätze

Die Ernte aus zwei Wochen. Jeder steht hier, weil er einmal Geld oder Stunden
gekostet hat.

> **Eine Begründung im Quelltext, die niemand nachgerechnet hat, ist eine
> Vermutung mit Anspruch auf Autorität.** Dreimal in zwei Tagen hat sich eine
> als falsch erwiesen — alle drei selbstsicher formuliert, alle drei erst durch
> einen Test widerlegt. Das ist der Grund, warum ein Review nicht der Autor
> macht.

> **Wer eine Begründung nachzieht, muss die Zusammenfassung darüber
> mitnehmen.** Eine veraltete Kurzfassung ist genauso irreführend wie eine
> veraltete Begründung — und sie wird öfter gelesen. **Und wer eine Regel
> schärft, muss den Satz mitnehmen, der sie erklärt:** die Erklärung hinter dem
> „i" am Geburtsdatum stand noch auf dem Stand von davor.

> **Eine Auswahl anzubieten und sie anschließend abzulehnen, ist schlechter als
> sie gar nicht erst anzubieten.** Wer beides prüft — was wählbar ist und was
> gilt —, muss beides aus einer Regel speisen. Sonst sperrt der Kalender die
> Zukunft, während die Prüfung zusätzlich das Alter misst.

> **Eine Gegenprobe, die den falschen Fehler einbaut, beweist nichts.** Sie
> muss den Fehler nachstellen, den der Test *nicht* offensichtlich findet — den
> stillen, nicht den lauten.

> **Ein Testhelfer, der eine Vorbedingung auslässt, prüft nicht die Maske,
> sondern eine Maske, die es nicht gibt.** `gruppeWaehlen` wählte nur die
> Gruppe; sämtliche Tests rechneten mit `blaupause == null` erfolgreich ab.

> **Ein erfundener Wert, der plausibel aussieht, ist schlimmer als einer, den
> das Tor abfängt.** Ein Ersatz-Geburtsdatum und ein Ersatzpreis von 15.000,00
> kamen durch jede Prüfung, weil die Nachricht in sich stimmig war.

> **Ein Bruch zwischen zwei je für sich fehlerfreien Bausteinen schlägt nicht
> fehl.** Ein Test, der prüft, *dass* gespeichert wird, findet ihn nicht; einer,
> der prüft, *was* ankommt, findet ihn.

> **Eine Regel, die von einer Angabe in einer Datei abhängt, gilt nur dort, wo
> die Angabe auch gepflegt ist.** Wer `Feldbau` ändert, muss `segments/*.json`
> **und** `tags/person-tags.json` ansehen.

> **Wer die Hülle eines Feldes ändert, ändert sie für alle.** Deshalb ist
> `Feldbau.bedienelement` statisch und die einzige Stelle, an der der Weg zum
> Bedienelement steht.

> **Ein Werkzeug, das einen Fehler verschweigt und stattdessen hängt, ist
> schlimmer als keines.** `main` gehört in ein `try/finally`, und der Ausstieg
> ins `finally`.

> **Eine Startgröße veraltet still.** Sie stand auf 900×600 aus einer Zeit, in
> der die Oberfläche aus einer Menüleiste bestand.

> **Eine Zahl im Meldungstext veraltet beim nächsten Zusatz still.** Lieber
> zählen als schreiben.

> **Was nicht adressierbar ist, lässt sich nicht steuern — und auch nicht
> prüfen.** Deshalb `Kennungen`.

> **Zwei Dateien mit gleichem Inhalt und verschiedenem Namen sind eine
> Verabredung, dass eine von beiden veraltet.**

> **Wer einen Pfad umlenkt, muss den Ordner umlenken.** Eine Anwendung schreibt
> mehr als eine Datei, und die zweite fällt erst auf, wenn jemand hinsieht — im
> Zweifel nach 98 Dateien im Ausgangsordner des Benutzers.

## Fallstricke, die schon Zeit gekostet haben

**Nach jeder Änderung an `gkv-core` muss `mvn install -DskipTests` laufen**,
bevor `gkv-ui` gestartet wird. `mvn test` allein installiert nichts. Sonst
läuft die Anwendung gegen ein altes `gkv-core`-Jar aus `~/.m2` — und man sucht
den Fehler im Quelltext, der dort längst behoben ist.

**`mvn test` ohne `clean` kann grün lügen.** Der Übersetzer arbeitet
inkrementell; nach dem Entfernen einer Methode meldete er einmal
`BUILD SUCCESS`, obwohl ein Test sie noch aufrief. Vor jedem Urteil über den
Stand: `mvn clean test`.

**Ältere Dateien haben CRLF-Zeilenenden.** Ein mehrzeiliges `perl -0pi -e`
findet dort nichts, obwohl das Muster stimmt. Entweder auf `\r?\n` prüfen oder
das Bearbeitungswerkzeug nehmen.

**Oberflächentests brauchen eine laufende JavaFX-Umgebung.** `JavaFxLaufzeit`
fährt sie hoch und führt Code auf ihrem Faden aus. Ausnahmen aus dem Faden
werden zurückgereicht — ohne das gälte ein fehlgeschlagener Test als bestanden.

**Im Bauknecht braucht JavaFX eine Anzeige.** Der Workflow ruft deshalb
`xvfb-run -a mvn test`.

**Eine Maske liefert ihren nackten Bereich, nicht das `ScrollPane`.** Dessen
Inhalt hängt erst nach dem Aufbau der Darstellung im Knotenbaum; ein `lookup`
auf die Bedienelemente liefe vorher ins Leere. Das Einhüllen macht
`Hauptfenster`. Wer ein `ScrollPane` prüfen muss, hängt es kurz in eine `Scene`
und ruft `applyCss()` und `layout()`.

**Es gibt kein TestFX, und das ist Absicht.** Dessen Wert liegt im Nachstellen
echter Eingaben. Seit die Meldungen hinter `Meldungen` liegen, blockiert nichts
mehr. Das erspart Monocle, das jeder JavaFX-Fassung hinterherhinkt. Für den
einen Fall, in dem das Nachstellen wirklich zählt, gibt es `Bedienung`.

**Ein beschreibbarer `Spinner` übernimmt getippten Text nur bei der
Eingabetaste.** Wer eine Zahl eintippt und dann auf eine Schaltfläche klickt,
für den liefert `getValue()` weiterhin den alten Wert — nichts schlägt fehl,
die Zahl steht sichtbar im Feld und wird trotzdem nicht verwendet. Deshalb
`commitValue()` bei jedem Auslesen und beim Fokusverlust. **Ein Test, der den
Wert über `getValueFactory().setValue(...)` setzt, geht am Editor vorbei und
findet das nie.**

**`Spinner.commitValue()` wirft bei unlesbarem Text.** Es fällt *nicht* auf den
letzten gültigen Wert zurück. In `gkv-ui` geht deshalb jeder Aufruf über
`JavaFxUiFactory.uebernimm(…)`.

**Keine SQLite-Datenbank in einem `@TempDir`.** Die Anwendung hält ihre
Verbindung offen, solange sie läuft — und sie läuft über das Testende hinaus,
weil die JavaFX-Laufzeit für alle Tests *einmal* hochgefahren wird. Windows
lässt die offene Datei nicht löschen, und JUnit macht daraus einen roten Test
bei grünem Ablauf. Ein Verzeichnis unter `target` nehmen.

**Ein relativer `gkv.db.path` landet unter `%LOCALAPPDATA%`**, nicht unter
`target` — `Anwendungsverzeichnis.aufloesen` löst ihn gegen den Datenordner der
Anwendung auf. Ein `rm -rf target/…` trifft ihn deshalb nicht, `mvn clean` auch
nicht, und die Testdaten häufen sich von Lauf zu Lauf. **Das gilt auch für
Tests, und dort fällt es länger nicht auf:** bis zum 06.09.2026 lagen sechs
Testdatenbanken neben der echten `database.db` im Benutzerprofil. Jeder Test
und jedes Werkzeug, das `gkv.db.path` setzt, gibt den Pfad **absolut** an —
`Path.of("target", …).toAbsolutePath()`.

**Und `gkv.db.path` allein genügt nicht.** Der Versandordner kommt aus
`billing-office-endpoints.json` und wird ebenso aufgelöst; wer nur die
Datenbank umlenkt, schreibt seine Abrechnungsdateien weiter in den
Ausgangsordner des Benutzers — dort lagen am 06.09.2026 98 Stück. **Wer die
Anwendung außerhalb ihres Zwecks laufen lässt, setzt `gkv.home`** und lenkt
damit den ganzen Datenordner um. So machen es `Bedienung.aufbauen` (und über
sie `AblaufTest`) und `Vorschau`.

**Eine `StackPane` zentriert ihre Kinder.** Wird ein Kind größer als die
Fläche — weil seine Mindestgröße das erzwingt —, ragt es auf *beiden* Seiten
hinaus, und man verliert oben und unten gleichzeitig. Wer eine Überlagerung
baut, verankert den Untergrund mit `StackPane.setAlignment(…, Pos.TOP_LEFT)`.

**In `gkv.css` gewinnt bei gleicher Spezifität die spätere Regel.**
`.info-zeichen` stand vor `.button` und wurde davon überschrieben — aus dem
Zeichen wurde ein grauer Fleck. Der Block steht jetzt nach `.button`, mit einem
Vermerk, dass er dort bleiben muss.

**`mvn clean` scheitert, solange das gebaute Paket noch läuft.** Windows gibt
die `.exe` nicht frei; die Fehlermeldung nennt „Failed to delete", nicht „ist
in Benutzung". Erst den Prozess beenden, im Zweifel
`rm -rf gkv-ui/target/paket`.

**`MainWindowTitle` des gestarteten Prozesses ist leer** — das heißt *nicht*,
dass die Titelleiste leer ist. Der `jpackage`-Starter hat selbst kein Fenster.
Wer den Titel prüfen will, zählt die Fenster auf:

```powershell
Get-Process | Where-Object { $_.MainWindowTitle -like "*Abrechnung*" }
```

**In PowerShell 5.1 kein `2>&1` auf native Programme.** `java -version`
schreibt auf stderr; die Umleitung erzeugt einen `NativeCommandError`, obwohl
der Aufruf erfolgreich war.

**`\b` arbeitet in Java nach ASCII.** Ein Suchmuster, dessen Begriff mit einem
Umlaut *beginnt*, findet nie etwas — „Übertragungsfehler" traf nicht, während
„Uebertragungsfehler" traf. Wer mit Wortgrenzen sucht und deutsche Texte
erwartet, setzt `Pattern.UNICODE_CHARACTER_CLASS` dazu. **Und prüft den Begriff
mit Umlaut**, nicht nur seine Umschrift.

**`exec:java` verschweigt eine Ausnahme, solange ein Nicht-Daemon-Faden lebt.**
Der JavaFX-Faden ist einer. Fliegt in einem Werkzeug wie `Bedienung` oder
`Vorschau` eine Ausnahme vor dem `Platform.exit()`, hängt der Aufruf still —
statt den Fehler zu melden, wartet Maven auf einen Faden, den nur die nie
erreichte Zeile beenden könnte. Wer einen solchen Hänger sieht, ruft das
Werkzeug einmal ohne Maven auf; dann steht die Ursache sofort da:

```bash
mvn -q -pl gkv-ui dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -Dmdep.includeScope=test
java -cp "gkv-ui/target/classes;gkv-ui/target/test-classes;$(cat /tmp/cp.txt)" \
    de.gkvtransmitter.presentation.Bedienung <ablauf> <ziel>
```

**`jpackage` baut nur für das System, auf dem es läuft.**

**SQLite legt keine Verzeichnisse an.** Siehe
`DatabaseSettings.sicherstelleVerzeichnis()`.

**SQLite lässt nur einen Schreiber zu**, und `busy_timeout` hilft **nicht**,
wenn eine Transaktion erst liest und dann schreibt — dann meldet SQLite sofort
`SQLITE_BUSY`. Solche Abläufe müssen anwendungsseitig serialisiert werden, wie
bei `nextDtaInterchangeReference()`. Aus demselben Grund bekommt
`AbrechnungService` die vorhandene Datenbank mit, statt eine zweite Verbindung
auf dieselbe Datei zu öffnen.

**Neue Objekte über `persist` speichern, nicht `merge`.** Nur `persist` trägt
die vergebene ID in das übergebene Objekt ein.

**IK in Testdaten brauchen eine gültige Prüfziffer** — jetzt erst recht, weil
die Oberfläche sie prüft. Verwendbar: `108310400`, `104940005`, `102137985`,
`101560000`. **`000000000` ist keines**: die Prüfziffer stimmt rechnerisch, das
Kennzeichen ist trotzdem keines, und ein nicht ausgefülltes `int`-Feld ist
genau diese Zahl.

## Nützliche Befehle

```bash
mvn clean test                       # alle 415 Tests
mvn clean test -pl gkv-ui            # nur die 238 Oberflächentests
mvn install -DskipTests              # Kern bereitstellen (siehe Fallstricke)
mvn -Ppaket clean package            # eigenständiges Windows-Paket
mvn -Pdebug -pl gkv-ui javafx:run    # mit Debug-Anschluss auf Port 5005
mvn checkstyle:check                 # 10 Warnungen, nicht blockierend
```

Die beiden Werkzeuge, die keine Tests sind:

```bash
# Die Oberfläche in PNG zeichnen, ohne ein Fenster zu öffnen
mvn -q -pl gkv-ui exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=de.gkvtransmitter.presentation.Vorschau \
    -Dexec.args="target/vorschau"

# Die Anwendung über eine Befehlsfolge bedienen (Abschnitt H)
mvn -q -pl gkv-ui exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=de.gkvtransmitter.presentation.Bedienung \
    -Dexec.args="gkv-ui/src/test/resources/ablaeufe/durchlauf-mit-bildern.txt target/durchlauf"
```

Abgeleitete Unterlagen neu erzeugen — **beide sind abgeleitet und nicht von
Hand zu bearbeiten:**

```bash
cd Information
java -jar C:/Tools/plantuml/plantuml.jar -tpng -charset UTF-8 "*.puml"
pandoc GKVTransmitter_Dokumentation.md -o GKVTransmitter_Dokumentation.docx --toc --toc-depth=2
```

In VS Code: `F5` → „Starten mit Testdaten (eigene Datenbank)".
