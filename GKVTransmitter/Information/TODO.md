---
title: "GKV-Abrechnung — Arbeitsliste"
subtitle: "Alles, was ohne zusätzliche Elemente umgesetzt werden kann"
lang: de
toc-title: "Inhaltsverzeichnis"
---

# Wozu diese Liste

Der `Umsetzungsplan.md` beschreibt den ganzen Weg in den Echtbetrieb,
einschließlich dessen, was auf Zertifikate, Verträge und die Zustimmung
Dritter wartet. **Diese Liste ist der Ausschnitt daraus, der heute
abgearbeitet werden kann** — ohne eine Beschaffung, ohne eine fremde Zusage,
ohne eine Unterlage, die nicht schon unter `Information/` liegt.

Sie ist keine zweite Planung. Jeder Punkt verweist auf seine Nummer im
Umsetzungsplan; was dort ausführlicher steht, wird hier nicht wiederholt.

## Was „ohne zusätzliche Elemente" heißt

| Zulässig | Nicht zulässig |
|---|---|
| Die Anlagen und Anhänge unter `Information/` (Anlage 1, Anlage 3, Anhänge 1–4c, GGT 2, 7, 20) | Unterlagen, die erst beschafft werden müssten |
| Vorhandene Bibliotheken: Hibernate, Jackson, JavaFX, JUnit | Eine neue Abhängigkeit im `pom.xml` |
| Die simulierte Kassengegenstelle als Gegenüber | Eine echte Datenannahmestelle |
| Selbst erdachte Prüfdaten | Echte Zertifikate, IK, Zugangsdaten |
| Ein Netzabruf auf Knopfdruck mit Bordmitteln (`java.net.http`, seit Java 11) | Ein Dienst, der im Hintergrund oder unaufgefordert sendet |

**Wo diese Grenze verläuft, ist bei jedem Punkt vermerkt.** Zwei Stellen liegen
knapp daneben und stehen deshalb am Ende unter „Ausdrücklich nicht in dieser
Liste".

---

# Block A — Der Prüfbericht erreicht die Anwenderin

Der wichtigste Block, weil ohne ihn jede weitere Prüfregel ins Leere warnt.

## A1. Warnungen sichtbar machen — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Umsetzungsplan** | 1.1 |
| **Größe** | M (ein bis drei Tage) |
| **Hängt an** | nichts |
| **Stand** | **umgesetzt** — `Versandergebnis` trägt Lieferungen und Bericht; `Bildschirmmeldungen` färbt nur noch rot, wenn wirklich ein Fehler vorlag |

`DtaDispatchService.generateAndRoute` sammelt den Prüfbericht und **wirft ihn
bei fehlerfreiem Lauf weg**; die Oberfläche zeigt ihn nur im `catch`-Zweig für
`DtaValidierungsException`. Warnungen und Hinweise erreichen den Bildschirm
also nie — obwohl die Abstufung in `ValidationSeverity` genau dafür gebaut ist.

**Zu bauen:** `generateAndRoute` liefert statt `List<DispatchBatch>` ein
Ergebnis mit Lieferungen **und** Bericht. Das zieht nach sich:
`AbrechnungService.createAndDispatch`, die Schnittstelle `Abrechnungslauf`,
`AbrechnungsMaske` und rund fünfzehn Teststellen.

**Nicht** über eine zweite Prüfung in der Maske lösen — die zöge
`referenzen.naechste()` ein zweites Mal und verbrennte
Datenaustauschreferenzen.

**Fertig, wenn:** ein Lauf mit einer Warnung erfolgreich durchläuft *und* die
Warnung in der Meldungsecke steht; ein Lauf ohne Beanstandung zeigt außer der
Erfolgsmeldung nichts.

## A2. Beanstandungen erklären statt nur benennen

| | |
|---|---|
| **Umsetzungsplan** | 1.2 |
| **Größe** | M |
| **Hängt an** | A1 |

Jede Beanstandung bekommt eine **Fundstelle** (Anlage, Abschnitt) und eine
**Handlungsanweisung**. Die Fundstellen stehen in den vorhandenen PDF unter
`Information/` — es ist Nachschlagen, keine Beschaffung.

**Fertig, wenn:** kein Fehlercode ohne Fundstelle existiert, durchgesetzt von
einem Test, der über alle Regeln läuft.

## A3. Positionsnummer gegen Anlage 3 § 8.2.6 prüfen — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Umsetzungsplan** | neu — Teil von D.2 / B4, siehe unten |
| **Größe** | S bis M |
| **Hängt an** | A1 (sinnvoll erst, wenn Warnungen ankommen) |
| **Stand** | **umgesetzt** als Warnung: `POSITION_HEBAMME_ZUSCHLAG` und `POSITION_HEBAMME_VIERSTELLIG`. Die Abstufungsfrage unten bleibt offen und ist deine |

**Das ist der Punkt, der bisher als blockiert galt und es nur halb ist.**
`Anlage_3_TP5_V22_20260521.pdf` liegt vor und beschreibt in § 8.2.6 den
**Aufbau** der Hebammen-Positionsnummer genau:

| Stelle | Bedeutung |
|---|---|
| 1 | Kategorie (Schwangerschaft, Geburt, Wochenbett, **Kurse**) |
| 2+3 | laufende Nummer |
| 4 | Zuschlag nach § 3 Anlage 1.1 zum Vertrag § 134a SGB V (`0` ohne, `1` mit) |
| 5 | Leistungsart (aufsuchend, nicht aufsuchend, Videobetreuung) |

Dazu: **fünf Stellen**, vier nur bei Betriebskostenpauschalen für von Hebammen
geleitete Einrichtungen — und die Übergangsregel *„bis Leistungsdatum
31.10.2025 gilt das alte, ausschließlich vierstellige Verzeichnis"*.

**Was die Regel heute schon tut** (nachgesehen, nicht angenommen): Sie prüft
für Abrechnungscode `50` die **Länge** auf vier bis fünf Stellen, nennt die
Fundstelle und meldet bewusst eine **Warnung**, keinen Fehler — mit
dokumentierter Begründung: Eine richtige Nummer ließe sich ohne das Verzeichnis
nicht hinschreiben, ein Fehler wäre „eine Sperre ohne Ausgang".

**Neu zu bauen sind die zwei Prüfungen, die heute fehlen:**

1. **Aufbau der fünften Stelle.** Die vierte Stelle ist der Zuschlag und darf
   nur `0` oder `1` sein. Eine fünfstellige Nummer mit einer `7` an vierter
   Stelle ist ohne Verzeichnis als falsch erkennbar.
2. **Die Übergangsregel am Leistungsdatum.** Vier Stellen gelten nur bis
   Leistungsdatum **31.10.2025** beziehungsweise für Betriebskostenpauschalen;
   danach sind fünf vorgesehen. Das Leistungsdatum steht im `ENF` und wird
   heute gar nicht herangezogen — vier Stellen gehen derzeit immer durch.

**Was das nicht leistet:** ob eine konkrete Nummer im bundeseinheitlichen
Verzeichnis *existiert*, bleibt ungeprüft. Das entscheidet die Kasse in
Prüfstufe 4.

> **Eine Entscheidung gehört dazu, und sie ist nicht meine:** Soll eine Nummer
> mit offensichtlich falscher **Länge** (die Vorbelegung führt neun Stellen)
> weiterhin nur warnen? Dafür spricht die vorhandene Begründung; dagegen, dass
> die Länge auch ohne Verzeichnis eindeutig falsch ist und die Lieferung
> sicher zurückgewiesen wird. **Aufbau und Datum als Warnung zu ergänzen ist
> unstrittig** — die Abstufung entscheidest du.

**Fertig, wenn:** eine fünfstellige Nummer mit unzulässiger vierter Stelle
beanstandet wird, eine vierstellige Nummer abhängig vom Leistungsdatum
beurteilt wird, und jede Meldung ihre Fundstelle nennt.

---

# Block B — Wer liefert, an wen

Der kritische Pfad des Umsetzungsplans. Beide Punkte hängen an nichts.

## B1. Betriebsdaten — die eigenen Angaben an einem Ort — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Umsetzungsplan** | 1.7 |
| **Größe** | M |
| **Hängt an** | nichts — **B3 hängt daran** |
| **Stand** | **umgesetzt**: Entität `Betriebsdaten`, Maske unter „Stammdaten", IK gegen die Prüfziffer. `UNB` und logischer Dateiname nehmen IK und Rolle von dort — mit Rückfall auf den Dienstleister und sichtbarem Hinweis, siehe Abweichung unten |

> **Eine Abweichung von „Fertig, wenn", bewusst:** Fehlende Betriebsdaten
> **halten den Lauf nicht an**, sondern melden `BETRIEBSDATEN_FEHLEN` als
> Hinweis, und es gilt weiter das IK des Leistungserbringers. Eine Sperre
> hätte jeden Lauf angehalten, **bevor die Maske überhaupt einmal geöffnet
> werden konnte** — dieselbe „Sperre ohne Ausgang", die schon bei der
> Positionsnummer gegen einen Fehler sprach. Für den häufigsten Fall, eine
> Hebamme, die für sich selbst abrechnet, ist die Rückfallebene richtig.
> **Ob daraus später ein harter Stopp wird, ist deine Entscheidung** — seit A1
> wird der Hinweis wenigstens gesehen.

> **Keine Bankverbindung angelegt**, anders als unten beschrieben. Der
> Umsetzungsplan sagt, die Festplattenverschlüsselung solle stehen, **bevor**
> hier eine Kontoverbindung eingetragen wird — ein Feld anzulegen, das dazu
> einlädt, wäre der falsche Weg herum. Ein Hinweistext in der Maske sagt, warum
> es fehlt.

Die Anwendung weiß heute nicht, wer sie betreibt: Der Absender stammt aus dem
je Abrechnung gewählten Dienstleister. Eine eigene Entität mit Praxisname,
Anschrift, **eigenem IK**, **Rolle** (Selbstabrechner `S` / Abrechnungsstelle
`A`), Steuernummer, Ansprechpartner, Zertifikatsdaten.

**Grenze:** Die Maske wird gebaut, das IK gegen die Prüfziffer geprüft — was
eingetragen wird, ist Sache des Betriebs. **Die IBAN bleibt zunächst
ungenutzt**: sie wäre die erste schützenswerte Angabe außerhalb der
Patientendaten, und die Datenbank ist unverschlüsselt. Feld anlegen, aber im
Formular mit Hinweis versehen.

**Fertig, wenn:** Betriebsdaten erfasst und geändert werden können, das IK
gegen die Prüfziffer geprüft wird, der logische Dateiname Rolle und IK **von
dort** nimmt statt aus dem Dienstleister, und fehlende Pflichtangaben den
Abrechnungslauf mit verständlicher Meldung anhalten.

## B2. Kostenträgerdatei einlesen — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Umsetzungsplan** | 1.3 |
| **Größe** | M |
| **Hängt an** | nichts |
| **Stand** | **umgesetzt**: `Kostentraegerdatei` löst Kassen-IK + Abrechnungscode auf die Annahmestelle **mit** Entschlüsselungsbefugnis auf, samt Übertragungsweg aus dem `DFU`. Eine fehlende Zuordnung meldet sich als `ANNAHMESTELLE_UNBEKANNT` |

> **Noch nicht verdrahtet:** Der Leser steht und ist geprüft, aber
> `billing-office-endpoints.json` bleibt vorerst die Quelle für den Versand.
> Das Umstellen braucht die **echte** Kostenträgerdatei — ein freier Download,
> aber eben ein zusätzliches Element, und danach gehört sie in den
> Wartungskalender (G3, vierteljährlich). Bis dahin sagt die Warnung
> wenigstens, wenn eine Kasse kein Ziel hat.

> **Zwei Festlegungen aus der Beschreibung, nicht geraten:** Ein **leeres**
> Abrechnungscode-Feld gilt *nicht* als „für alles" — Fußnote 4 nennt den Code
> für Teilprojekt 5 zwingend. Und der **Sonderschlüssel 99** wird nicht
> stillschweigend benutzt: Er gilt für *nicht aufgeführte* Gruppen.

Heute wird `billing-office-endpoints.json` von Hand gepflegt — mit 23 einzelnen
Kassen und damit dem **falschen Empfänger**: zu liefern ist an die
Datenannahmestelle, nicht an die Kasse.

Das Format steht in `Anhang_3_Kostentraegerdatei_V10_20260414.pdf`:

```
IDK   IK der Versichertenkarte
 └─ VKG+01+<IK>            → Kostenträger
      └─ VKG+03+<IK>+…+50  → Datenannahmestelle MIT Entschlüsselungsbefugnis
           │                 (VKG+02 wäre ein Netzbetreiber OHNE)
           └─ DFU           → Adresse: 070 E-Mail, 016 FTAM, 080 KIM
```

**Grenze:** Der Leser wird gegen selbst gebaute Prüfdaten nach dieser
Beschreibung entwickelt. Die echte, vierteljährlich erneuerte Datei ist frei
verfügbar, aber ein Download — sie gehört in den Wartungsplan, nicht in diese
Liste. `DtaDocument` trägt das Segmentformat bereits.

**Fertig, wenn:** zu einem Kassen-IK und Abrechnungscode `50` die richtige
Annahmestelle samt Adresse ermittelt wird und eine fehlende Zuordnung sich
**meldet**, statt still auf einen Sammelordner auszuweichen.

## B3. Auftragsdatei und physikalischer Dateiname — ⛔ zurückgestellt (17.09.2026)

| | |
|---|---|
| **Umsetzungsplan** | 1.6 |
| **Stand** | **nicht machbar ohne zusätzliches Element** — dieser Eintrag war in der ersten Fassung dieser Liste falsch eingeordnet |

**Was beim Nachlesen herauskam.** `GGT_Anlage_2_Auftragsdatei.pdf` liegt vor,
reicht aber nicht: Der Auftragssatz ist ein Satz **fester Länge** (348 Byte,
Version 01), und zwei seiner **Mussfelder** lassen sich hier nicht füllen.

| Mussfeld | Woher der Wert käme | Steht er zur Verfügung? |
|---|---|---|
| `VERFAHREN_KENNUNG` (Stellen 20–24) | „Anlage 4 zu den Gemeinsamen Grundsätzen Technik" — so verweist Anlage 2 selbst | **nein**, unter `Information/` liegen nur GGT 2, 7 und 20 |
| `EMPFÄNGER_NUTZER` (Stellen 63–77) | „die Datenannahmestelle mit Entschlüsselungsbefugnis **gemäß Kostenträgerdatei**" | Leser steht (B2), die **echte Datei** fehlt |
| `ABSENDER_EIGNER` (Stellen 33–47) | eigenes IK | ja, seit B1 |

Der **physikalische Dateiname** dagegen ist vollständig beschrieben (Anhang 1,
Abschnitt 4.3): `E`/`T` + `SOL` + `0` + dreistellige Transfernummer. Er ist
aber **nur in der Auftragsdatei** anzugeben — ohne sie hätte eine Klasse dafür
keinen Abnehmer und wäre toter Quelltext.

> **Deshalb bleibt B3 ungebaut, statt halb gebaut.** Eine Auftragsdatei mit
> erfundener Verfahrenskennung wäre genau das, was der Umsetzungsplan verbietet:
> *„Was keine Fundstelle hat, ist eine Vermutung."* Prüfstufe 1 prüft das Paar
> aus Nutzdaten- und Auftragsdatei — eine falsche Kennung fiele dort auf.

**Was es bräuchte:** GGT Anlage 4 herunterladen (frei verfügbar, wie die
übrigen) und die echte Kostenträgerdatei dazu. Dann ist B3 eine Größe **M**.

---

# Block C — Was nach dem Versand bleibt

## C1. Übermittlungsprotokoll und Sicherungskopie — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Umsetzungsplan** | 1.5 |
| **Größe** | M |
| **Hängt an** | A1 |
| **Stand** | **umgesetzt**: Entität `Protokolleintrag` mit einem Feld je Pflichtinhalt, geschrieben beim Versand, Übersicht unter „Übermittlungen" mit Vermerk „Bezahlt" |

Die zehn Mindestinhalte aus **Anhang 1, Abschnitt 4.5** sind einzeln
abgebildet — physikalischer Dateiname, Erstellungsdatum, laufende Nummer,
Kommunikationspartner, Beginn und Ende, Dateigröße, Verarbeitungshinweise,
Senden/Empfangen, fehlerfrei/fehlerhaft und im Fehlerfall der Fehlerstatus.
**Auch ein Fehlschlag wird protokolliert**: Ein Protokoll, das nur die
gelungenen Fälle kennt, belegt nichts.

Dazu der Zustand **bezahlt** aus Abschnitt 3 Absatz 4 („Eine Sicherungskopie
der Daten ist durch den Absender bis zur Bezahlung vorzuhalten"). Ohne einen
Zeitpunkt dafür wüsste niemand, wann die Kopien unter `staging/` weg dürfen —
und niemand sähe, worauf noch Geld aussteht.

> **Damit ist auch die andere Hälfte von C2 nicht mehr blockiert**, jedenfalls
> nicht mehr am Protokoll: Was versendet wurde, steht jetzt fest. Es fehlen
> weiterhin echte Rechnungs- und Belegnummern (D.4 der Übergabe), ohne die ein
> `URI`-Segment auf `00000000` zeigen würde.

Anlage 1, Abschnitt 3 Absatz 2 verlangt eine Dokumentation des Datenaustauschs,
**mindestens zwei Jahre aufzubewahren**; die Mindestinhalte nennt Anhang 1,
Abschnitt 4.5: physikalischer Dateiname, Erstellungsdatum, laufende Nummer,
Kommunikationspartner, Beginn und Ende der Übermittlung, Dateigröße,
Verarbeitungshinweise, Richtung, fehlerfrei oder fehlerhaft, im Fehlerfall der
Fehlerstatus.

**Fast ein Nebenprodukt** — die Angaben entstehen beim Versand ohnehin, sie
werden nur nirgends festgehalten. Dazu der Zustand „bezahlt", ab dem die
Sicherungskopien unter `staging/` weg dürfen (Absatz 4).

**Fertig, wenn:** jede erzeugte und jede zugestellte Datei einen
Protokolleintrag hat und eine Übersicht zeigt, was noch auf Zahlung wartet.

## C2. Verarbeitungskennzeichen: Korrektur und Nachforderung — 🟡 zur Hälfte (17.09.2026)

| | |
|---|---|
| **Umsetzungsplan** | 1.4 |
| **Größe** | S |
| **Hängt an** | nichts |
| **Stand** | **Prüfung gebaut**: `VerarbeitungskennzeichenRegel` hält den Wert gegen Anlage 3 § 8.1.7 und Anlage 1 § 7.3. **Das Erzeugen einer Korrekturrechnung fehlt** — Grund unten |

**Was die Regel prüft** (alles nachgelesen, nicht angenommen):

| Befund | Fundstelle |
|---|---|
| `FKT_KENNZEICHEN_UNBEKANNT` — nur `01`, `02`, `03`, `04`, `10` | Anlage 3, Abschnitt 8.1.7 |
| `FKT_KENNZEICHEN_UNEINHEITLICH` — „Innerhalb einer Datei dürfen nicht verschiedene Verarbeitungskennzeichen genutzt werden" | Anlage 1, Abschnitt 7.3 |
| `URI_FEHLT` — bei Kennzeichen ≠ `01` sind die Ursprungsangaben zu übermitteln | Anlage 1, Abschnitt 7.3 |

> **Warum das Erzeugen noch nicht geht.** Eine Korrekturrechnung braucht das
> `URI`-Segment mit **Leistungserbringer-IK, Sammel- und
> Einzelrechnungsnummer, Rechnungsdatum und Belegnummer der
> Ursprungsrechnung**. Das Programm hält davon nichts fest: `DtaFactory`
> schreibt `REC+00000000:0` als Konstante, und **was versendet wurde, wird
> nirgends protokolliert**. Eine Korrektur, die auf `00000000` verweist, wäre
> keine.
>
> **Es hängt also an C1** (Übermittlungsprotokoll) und an der offenen Frage D.4
> der Übergabe (echte Rechnungs- und Belegnummern). Die Regel steht schon
> heute — sie greift in dem Moment, in dem jemand ein anderes Kennzeichen
> setzt, und sie prüft auch eine fremde Datei.

Das Programm kennt nur `FKT+01`. Anlage 3, § 8.1.7 (vorhanden, nachgesehen)
führt fünf Werte:

| Wert | Bedeutung |
|---|---|
| `01` | Abrechnung ohne Besonderheiten |
| `02` | Nachforderung |
| `03` | Zuzahlungsforderung |
| `04` | Korrekturrechnung |
| `10` | Wiederaufnahme |

Ohne sie gibt es **keinen Weg, auf eine Zurückweisung zu antworten** — dieselbe
Rechnung erneut zu senden sieht für die Kasse wie eine Doppelabrechnung aus.
Wann welcher Wert zulässig ist, regelt Anlage 1, Abschnitt 7.1 — **zu lesen,
nicht zu raten**; beide Anlagen liegen vor.

**Fertig, wenn:** eine zurückgewiesene Abrechnung als Korrekturrechnung neu
erzeugt werden kann und die Prüfung den Wert gegen Abschnitt 7.1 hält.

---

# Block D — Kleineres, das jetzt geht

Jeder Punkt hier ist **S**, sofern nichts anderes steht.

| | Was | Warum jetzt | Hängt an |
|---|---|---|---|
| **D1** | **Trockenlauf als Menüpunkt.** `SimulierterKassenTransport` existiert, ist aber nur über Tests erreichbar. Ein Knopf „einmal durchspielen, ohne zu senden". | Die beste Übung vor der Erprobung — und sie kostet nichts, weil das Gegenstück schon gebaut ist. | — |
| **D2** | **Zertifikats-Ablaufwarnung.** Die Anwendung kennt das Datum aus B1 und erinnert rechtzeitig. | Einer der drei Punkte, die die Wartungslast klein halten. | B1 |
| **D3** ✅ | ~~**Versionsangabe sichtbar machen.**~~ **Erledigt mit G1.** Der Nachrichtentyp trägt sie bereits (`SLGA:21:0:0`); wer sieht, womit er sendet, merkt einen Wechsel. | Ebenfalls Wartungslast. Anlage 3 **V22 gilt ab 01.02.2027** — das Projekt steht auf V21. | — |
| **D4** | **Belegnummer gegen Doppelvergabe sichern.** Heute frei gebildet (`HEB` + Jahr/Monat + laufende Nummer), ohne Garantie über Jahresgrenzen. | Vor dem Echtbetrieb ohnehin fällig, und die Zählerlogik aus `DtaCounter` liegt vor. | — |
| **D5** ✅ | ~~**Umsatzsteuersatz einstellbar** statt fest `19` im `UST`-Segment.~~ **Erledigt mit F3 — und die Frage war falsch gestellt:** das `UST`-Segment trägt keinen Satz, sondern Steuernummer und Befreiungskennzeichen (Anlage 1, Abschnitt 5.5.2). | Ob für Hebammenleistungen überhaupt Umsatzsteuer anfällt (§ 4 Nr. 14 UStG), ist eine Frage an einen Menschen — **dass der Wert einstellbar sein muss, ist keine.** | B1 oder Einstellungen |
| **D6** | **Checkstyle: die zehn übrigen Warnungen.** Nachgemessen am 17.09.2026: fünf in `gkv-core`, fünf in `gkv-ui`. **ParameterNumber** in `FieldDefinition`, `Person`, `Patient`, `ServiceProvider` (je 9) und `EditFormController` (12); **CyclomaticComplexity** in `BetragskonsistenzRegel` (11), `Feldbau` (13 und 12) sowie beiden `FieldPopulator` (je 11). Größe **M**. | Eine Person hat mehr Eigenschaften, als ein Konstruktor tragen sollte — Builder oder ein `record` für die Anschrift. Eingriff in die Entitäten, also bewusst und nicht nebenbei. | — |

---

# Block E — Dokumentation

| | Was | Anmerkung |
|---|---|---|
| **E1** | **Teil 12 der Projektdokumentation** (Nachrichtenerzeugung) schreiben. | Teile 01–11 stehen. Bauplan und Stilregeln in `Schulprojekt.md`. |
| **E2** | **Kostenrechnung auf die richtige Kurszahl umstellen.** Das Kapitel „Was der eigene Weg wirklich kostet" rechnet mit **drei Kursen im Jahr**. Tatsächlich sind es rund **vierzig Kurstermine im Jahr** (ein Kurs über mehrere Wochen, etwa vierzig Wochen Kursbetrieb). Das Kapitel ist entsprechend neu zu rechnen — **und der Schluss ändert sich weniger, als die Zahl vermuten lässt**, siehe Kasten unten. | Der Einzelbetrag je Teilnehmerin und Termin steht in der Hebammenhilfe-Vergütungsvereinbarung und liegt **nicht** vor; die Rechnung bleibt bis dahin eine Formel mit einer Unbekannten. |
| **E3** | **`00-Gesamtdokumentation` neu erzeugen** — sie steht noch auf dem Stand bis Teil 08. | `dokumentation/erzeugen.sh` braucht Pandoc **und** LibreOffice; letzteres fehlt in der WSL. **Unter Windows laufen lassen.** |

> ## Was bei vierzig Kursterminen herauskommt
>
> `Jahresumsatz = 40 Termine × Teilnehmerinnen × Einzelbetrag`, die Gebühr
> einer Abrechnungsstelle sind **3 %**, das Zertifikat kostet **79 € einmalig
> und 49 € jährlich** (zzgl. USt).
>
> | Teiln. | €/Termin | Umsatz/Jahr | Gebühr 3 % | Ersparnis nach Zertifikat |
> |---:|---:|---:|---:|---:|
> | 8 | 10 | 3.200 € | 96 € | **47 €** |
> | 10 | 15 | 6.000 € | 180 € | **131 €** |
> | 12 | 20 | 9.600 € | 288 € | **239 €** |
>
> **Gleichstand ab rund 1.630 € Jahresumsatz** (im ersten Jahr rund 2.630 €).
> Bei vierzig Terminen wird das in jedem Fall überschritten — der eigene Weg
> ist also billiger, **aber um fünfzig bis zweihundertvierzig Euro im Jahr.**
>
> Das entscheidet die Frage nicht, sondern verschiebt sie: Eine Ersparnis in
> dieser Größe ist ein bis drei Arbeitsstunden wert. **Kostet die Pflege mehr
> als das, ist der eigene Weg trotz niedrigerer Gebühr teurer.** Deshalb ist
> Block G keine Bequemlichkeit, sondern die Bedingung, unter der die Rechnung
> überhaupt aufgeht.

---

# Block F — Das Dienstleisterprofil trägt mehr

Heute steht am `ServiceProvider` wenig mehr als Name, Anschrift und IK.
Alles Übrige wird bei **jeder** Abrechnung erneut gewählt oder getippt,
obwohl es sich fast nie ändert. Jeder Punkt hier spart Eingaben, ohne dass
eine Angabe von außen dazukommt.

**Abgrenzung zu B1:** Die **Betriebsdaten** beschreiben, *wer die Anwendung
betreibt* (ein Datensatz, Absender der Datei). Das **Dienstleisterprofil**
beschreibt *wer die Leistung erbringt* — bei einer allein arbeitenden Hebamme
dieselbe Person, in einer Praxis mit zwei Hebammen nicht.

| | Was | Was es spart | Größe |
|---|---|---|---|
| **F1** ✅ | **Abrechnungscode je Dienstleister** (`50` Hebamme, `61` Rehabilitationssport) statt je Blaupause. | Der Code gehört zum Beruf, nicht zur Kursart. Nebenbei wird der **Leistungsbereich** im `UNB` richtig abgeleitet, ohne dass eine Blaupause ihn tragen muss. | S |
| **F2** ✅ | **Standardwerte für den Abrechnungslauf:** übliche Blaupause, übliche Terminzahl, übliche Gruppengröße. Die Abrechnungsmaske füllt damit vor. | Bei vierzig Kursterminen im Jahr ist das der Unterschied zwischen „vier Felder je Lauf" und „bestätigen". | S |
| **F3** ✅ | **Umsatzsteuerpflicht als Kennzeichen am Dienstleister.** | Hängt an der Person, nicht am Kurs (§ 4 Nr. 14 UStG). Speist `D5` und das `UST`-Segment. **Die Rechtsfrage bleibt extern, das Feld nicht.** | S |
| **F4** ✅ | **Ansprechpartner, Telefon, E-Mail, Steuernummer.** | Wird bei jeder Anmeldung wieder gebraucht. Zusammen mit B1 ergibt das den „Aktenordner", aus dem sich die Anträge ausfüllen lassen. | S |
| **F5** ✅ | **Registrierungsblatt drucken** — alle Angaben aus B1 und F4 auf einer Seite, in der Form, die ARGE·IK, Trust Center und Annahmestelle abfragen. | Aus dem Aktenordner wird ein Formular, das nur noch zu unterschreiben ist. Der Punkt, an dem sich B1 zum ersten Mal auszahlt. | S–M |
| **F6** ✅ | **Gültigkeitszeitraum am Dienstleister** (tätig seit/bis) und ein Kennzeichen „aktiv". | Ausgeschiedene Personen verschwinden aus den Auswahllisten, ohne dass ihre alten Abrechnungen ihren Bezug verlieren. | S |

> **Nicht hierher gehört das Tarifkennzeichen.** Es ist der plausibelste
> Kandidat für dieses Profil — aber solange der Vertrag nicht gelesen ist,
> wäre das Anlegen des Feldes eine Vorwegnahme der Antwort. Siehe B5 unter
> „Ausdrücklich nicht in dieser Liste". **Wenn** die Antwort „hängt am
> Dienstleister" lautet, ist F1 die Stelle, an der es dazukommt.

**Fertig, wenn:** ein Abrechnungslauf für einen eingerichteten Dienstleister
ohne eine einzige Eingabe außer der Terminzahl startbar ist und die erzeugte
Datei dieselbe ist wie bei vollständiger Handeingabe.

## F1. Abrechnungscode je Dienstleister — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Größe** | S |
| **Stand** | **umgesetzt**: Feld am `ServiceProvider`, Vorrang vor der Blaupause, Hinweis bei Abweichung |

Der Code stand in der Blaupause, also bei der **Leistung**. Nach Anlage 3,
Abschnitt 8.1.5 hängt er am **Beruf**: `50` ist Hebamme/Entbindungspfleger,
`61` Leistungserbringer von Rehabilitationssport. Wer zwei Kursarten anbot,
pflegte denselben Wert zweimal — und aus ihm leitet sich der Leistungsbereich
im `UNB` ab, in dem das Projekt am 07.09.2026 schon einmal falsch lag (`H`
statt `F`).

Trägt der Dienstleister einen Code, gewinnt seiner. **Ein leeres Profilfeld
überschreibt nichts** — sonst hätte das Einführen des Feldes jeden bestehenden
Dienstleister auf einen leeren Code gesetzt.

> **Der Vorrang meldet sich.** Weichen Profil und Blaupause voneinander ab,
> steht das als Hinweis (`ABRECHNUNGSCODE_AUS_PROFIL`) im Prüfbericht. Ein
> stiller Wechsel des Abrechnungscodes ist genau der Fall, der in Prüfstufe 3
> als Zurückweisung zurückkommt: Die Datei ist in sich stimmig und nennt
> trotzdem den falschen Beruf.

**Nebenbei zwei Dinge, die vorher nicht gingen:**

- Das Personenformular kann jetzt **Felder je Rolle** zeigen
  (`Personenfelder`, `EntityFieldPopulator.zusatzfelder()`). Teilnehmer und
  Dienstleister teilten sich bis dahin nicht nur das Formular, sondern
  zwangsläufig auch jedes Feld.
- Das Formular zum **Bearbeiten** beschriftet die Felder jetzt wie das zum
  Anlegen. Dort stand `kassenIk` und `birthDate`, wo dieselben Felder beim
  Anlegen „IK der Krankenkasse" und „Geburtsdatum" heißen.

## F3 + F4. Steuerangaben und Erreichbarkeit — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Größe** | S (beide zusammen) |
| **Stand** | **umgesetzt**: Steuernummer, Befreiungskennzeichen, Ansprechpartner, Telefon, E-Mail am Dienstleister — und ein `UST`-Segment, das der Anlage entspricht |

Beide Punkte gehören zusammen, weil sie sich die **Steuernummer** teilen. Beim
Nachschlagen dafür kam ein Befund heraus, der größer ist als der TODO-Eintrag:

> ### Das `UST`-Segment trägt keinen Umsatzsteuersatz
>
> Die Anwendung schrieb `UST+19'`. Nach **Anlage 1, Abschnitt 5.5.2** —
> gleichlautend in Version 21 und Version 22 — besteht das Segment aus:
>
> | Feld | Stellen | Art | Inhalt |
> |---|---|---|---|
> | Umsatzsteuer-Kennzeichen | 3 | M | `UST` |
> | Steuernummer / USt-IdNr. | ..20 | M | „Steuernummer gemäß § 14 Abs. 1a UStG oder Umsatzsteuer-Identifikationsnummer" |
> | Kennung UST-Befreiung | 1 | K | „`J`" wenn befreit gem. § 4 UStG |
>
> **Die 19 landete damit im Feld der Steuernummer.** Sie stammt aus
> `Information/Valide.DTA`, das an derselben Stelle `UST+19` führt — dieselbe
> Referenzdatei, die dem Projekt schon den Abrechnungscode 61 und den
> Leistungsbereich H eingetragen hat. **Sie ist in sich stimmig und in der
> Sache dennoch nicht maßgeblich.**
>
> Richtig ist jetzt `UST+60/123/45678+J'`. Ohne Steuernummer bleibt das
> Segment **ganz weg**: Sie ist innerhalb des Segments Pflicht, das Segment
> selbst ist konditional (Art `K`, Wiederholungsfaktor 0–1). Weglassen ist
> erlaubt, erfinden nicht — dieselbe Entscheidung wie beim fehlenden
> Geburtsdatum im `NAD`. Dass sie fehlt, meldet `STEUERNUMMER_FEHLT`.

**Damit ist D5 erledigt, aber anders als dort angenommen.** D5 wollte den
Umsatzsteuer*satz* einstellbar machen. Einen Satz gibt es an dieser Stelle
nicht; einstellbar sein müssen Steuernummer und Befreiungskennzeichen, und
beide hängen an der Person. Das Feld `Umsatzsteuersatz` ist aus
`Leistungsparameter`, `ust.json` und dem Blaupausenformular entfernt — es war
nach dieser Korrektur ein Feld, das sich ausfüllen ließ und die Nachricht nie
erreicht hätte. Genau der Fehler, den `BlaupausenfelderTest` seit dem
05.09.2026 abfängt.

**Zwei Nebenbefunde:**

- **`InputOption.BOOLEAN` wurde nie gebaut.** Der Wert stand seit jeher im
  Enum; `Feldbau` fiel damit auf den Vorgabezweig und machte ein Textfeld
  daraus, in das sich „vielleicht" tippen ließ. Aufgefallen beim ersten Feld,
  das es braucht. Auch `Bedienung` kannte keine Kästchen — das erste
  Ja-Nein-Feld wäre aus keinem Ablauf heraus zu setzen gewesen.
- **`ServiceProviderFieldPopulator`** ist von zwei `switch` auf zwei Tabellen
  umgestellt. Mit vierzehn Feldern meldete Checkstyle Komplexität 15 und 17
  gegen eine Obergrenze von 10. Eine Tabelle wächst in der Breite statt in der
  Tiefe. **Das ist zugleich das ausgearbeitete Beispiel für D6**, wo dasselbe
  für `PatientFieldPopulator` noch aussteht.

## F5. Registrierungsblatt — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Größe** | S–M |
| **Stand** | **umgesetzt**: Menüpunkt „Registrierungsblatt", Anzeige und Ablage als Textdatei |

Alles, was bei der Anmeldung zum Datenaustausch zur Hand sein muss, auf einem
Blatt: Absender (B1), Leistungserbringer (F1/F3/F4), das Verfahren, mit dem die
Anwendung sendet — **und was noch fehlt.**

### Es ist kein Antrag, und das ist eine Korrektur der Aufgabe

Die Aufgabe sagte „in der Form, die ARGE·IK, Trust Center und Annahmestelle
abfragen". **Diese Formen liegen dem Projekt nicht vor**, und Anlage 1 gibt
keine vor. Abschnitt 2 Absatz 1 verlangt etwas anderes:

> „Die Einzelheiten zur Durchführung der Datenübermittlung sind rechtzeitig vor
> der erstmaligen Durchführung oder Änderung des Datenaustauschverfahrens
> zwischen dem Absender und dem Empfänger der Daten abzustimmen."

Das Blatt ist deshalb eine **Zusammenstellung für diese Abstimmung**, kein
Formular. Ein Blatt, das sich als amtliches ausgäbe, wäre schlimmer als keines
— dieselbe Regel wie bei der Positionsnummer: *was keine Fundstelle hat, ist
eine Vermutung.*

### Drei Entscheidungen

- **Abgelegt statt gedruckt.** Ein Druckauftrag setzt einen eingerichteten
  Drucker voraus und ließe sich weder unter WSL noch im CI-Läufer prüfen. Eine
  Textdatei lässt sich öffnen, ausdrucken, anhängen — und ein Test kann sie
  lesen.
- **Fehlende Werte werden zur Linie, nicht zur Leerstelle.** Wer das Blatt vor
  sich hat, soll sehen, *dass* dort etwas hingehört. Ein weggelassenes Feld
  fällt niemandem auf.
- **Die Zahl der Lücken steht über dem Blatt.** Abschnitt 4 ist die letzte
  Zeile eines langen Textes; wer ablegt, ohne zu blättern, sieht ihn nie.

### Nebenbei: `DtaFactory.NACHRICHTENVERSION`

Die Version stand zweimal als Zeichenkette im Quelltext, einmal für `SLGA` und
einmal für `SLLA`. Als Konstante ist sie ablesbar — das Blatt nennt sie, und
**wer sieht, womit er sendet, merkt einen Wechsel.** Damit ist zugleich die
Grundlage für **D3** und **G1** gelegt.

> **Block F ist damit vollständig.**

## F2. Standardwerte für den Abrechnungslauf — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Größe** | S |
| **Stand** | **umgesetzt**: übliche Blaupause, übliche Terminzahl, übliche Gruppengröße am Dienstleister |

Drei Felder am Profil, die die Abrechnungsmaske ausliest, sobald eine Gruppe
gewählt ist — erst sie nennt den Dienstleister:

| Feld | Wirkung |
|---|---|
| **Übliche Blaupause** | wird vorgewählt, **solange nichts gewählt ist** |
| **Übliche Terminzahl** | steht in jedem Zähler, auch in dem der Werkzeugleiste |
| **Übliche Gruppengröße** | wird *nicht* vorbelegt, sondern gegengeprüft |

> **Eine getroffene Wahl wird nicht überschrieben.** Wer die Gruppe korrigiert,
> hat seine Blaupause nicht zur Disposition gestellt — sonst verlöre er sie bei
> jedem Wechsel erneut. Passt der hinterlegte Name auf keine vorhandene
> Blaupause, wird nichts vorgewählt; die Maske verlangt ohnehin eine bewusste
> Wahl, bevor etwas losgeht.

### Die Gruppengröße belegt nichts vor, sie prüft gegen

Ein Vorbelegen wäre sinnlos — wer teilnimmt, steht in der Gruppe. Der Nutzen
liegt woanders: **Ein vergessener Haken ist eine nicht gestellte Forderung, und
die fällt niemandem auf.** Die Datei ist gültig, die Kasse zahlt, was dasteht.
Weicht die Zahl der Angehakten von der üblichen ab, steht das deshalb als
Nachsatz genau an der Zeile, an der man vor dem Versand ohnehin abliest, ob die
Zahlen stimmen — und schweigt, wenn sie passt. Ein Hinweis bei jedem Lauf wird
nicht mehr gelesen.

### Nachgeprüft am Ablauf, nicht nur im Test

`ein-monat.txt` setzt weder `abrechnung-blaupause` noch
`abrechnung-termine-alle`. Die erzeugte Datei ist dieselbe wie bei
vollständiger Handeingabe:

```
UNB+UNOC:3+108310400+102137985+20260917:1508+00001+F+SL831040S09+1'
UST+60/123/45678+J'
ENF+01+50:00000+306050601+8,00+13,25+20260917+0,00'
```

Der Leistungsbereich `F` aus dem Abrechnungscode 50 (F1), das `UST` aus
Steuernummer und Befreiung (F3), die acht Termine aus der üblichen Terminzahl
(F2) — **drei Blockpunkte in einer Zeile nachweisbar.**

**Damit ist Block F vollständig** — siehe F5 oben.

## F6. Tätigkeitszeitraum — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Größe** | S |
| **Stand** | **umgesetzt**: `tätig seit` / `tätig bis` am Dienstleister, „aktiv" daraus abgeleitet |

Ausgeschiedene Personen verschwinden aus den **Auswahllisten**. Nur dort: Ihre
Gruppen, Abrechnungen und Protokolleinträge behalten den Bezug auf sie — ein
Protokolleintrag, dessen Leistungserbringer verschwindet, wäre kein Protokoll
mehr.

> **Wer schon in einer Gruppe steht, bleibt dort wählbar.** Ohne diese
> Ausnahme würde das Bearbeiten einer alten Gruppe den ausgeschiedenen
> Dienstleister still herauswerfen: Er stünde in keiner Liste, könnte also
> nicht angehakt sein, und beim Speichern wäre er weg — mit einer Gruppe, die
> sich danach nicht mehr abrechnen lässt.

### Das Kennzeichen „aktiv" ist kein Feld geworden

Die Aufgabe nannte den Zeitraum **und** ein Kennzeichen. Als Feld gebaut, hielt
es nicht eine Minute: Ein Kästchen im Formular beginnt ungehakt, das Feld stand
auf `true` — **jeder neu angelegte Dienstleister wäre sofort inaktiv gewesen.**
Der Ablauf `ein-monat.txt` blieb an Zeile 59 stehen, `klick
gruppe-dienstleister-1`: In der Auswahl stand niemand mehr.

Zwei Quellen für dieselbe Tatsache waren ohnehin eine zu viel. „Nicht mehr
anbieten" heißt „tätig bis heute", und das ist ein Datum, das jeder hat. Der
Status wird jetzt abgeleitet (`istAktivAm`) und in der Dienstleisterübersicht
als eigene Spalte angezeigt — sichtbar bleiben sie dort, sonst ließe sich ein
falsch gesetztes Enddatum nicht mehr zurücknehmen.

**Ein leeres Feld heißt jeweils das Unschädliche:** kein Beginn „schon immer",
kein Ende „weiterhin tätig". Sonst wäre mit dem Einbau des Zeitraums der ganze
Bestand aus den Listen verschwunden.

---

# Block G — Wartung, die sich selbst meldet

**Der Grund steht im Projekt selbst:** Die verbindlichen Anlagen unter
`Information/` waren am 07.09.2026 vom Juli — zwei Monate überholt, und es war
niemandem aufgefallen. So sieht die Wartungslast in der Praxis aus: nicht als
Aufwand, den man einplant, sondern als **Veralten, das niemand bemerkt**.

Dieser Block ist die Antwort darauf. Er ist der Grund, warum der eigene
Übermittlungsweg überhaupt tragbar ist — ohne ihn ist er jedes Jahr Handarbeit.

## G1. Die Anwendung weiß, auf welchem Stand sie steht — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Größe** | S |
| **Hängt an** | nichts |
| **Stand** | **umgesetzt**: Menüpunkt „Stand der Unterlagen", gespeist aus `unterlagen/unterlagen.json` |

Oben steht, womit die Anwendung **tatsächlich sendet** (aus
`DtaFactory.NACHRICHTENVERSION`, nicht aus der gepflegten Liste — gingen die
beiden auseinander, wäre genau das der Befund), darunter jede Unterlage mit
Version, Stand, Anwendungsdatum und Zustand.

### Die Deckblätter, nachgetragen am 17.09.2026

| Unterlage | Version | Stand | anzuwenden ab | gültig bis |
|---|---:|---|---|---|
| Anlage 1 | 21 | 15.01.2026 | 01.10.2025 | **30.04.2027** |
| Anlage 1 | 22 | 21.05.2026 | **01.02.2027** | — |
| Anlage 3 | 22 | 21.05.2026 | **01.02.2027** | — |
| Anhang 1 | — | 31.08.2017 | 01.09.2017 | — |
| Anhang 2 | — | 10.11.2003 | 10.11.2003 | — |
| Anhang 3 (Kostenträgerdatei) | 10 | 14.04.2026 | **01.02.2027** | — |

**Zwei Befunde fallen dabei sofort an:**

1. Der erwartete: Version 22 gilt ab **01.02.2027**, und — das stand bisher
   nirgends — **Version 21 verliert am 30.04.2027 ihre Gültigkeit.** Das
   Deckblatt der Version 22 sagt es. Drei Monate Überlappung, danach ein
   harter Stichtag.
2. Der unerwartete: **Eine Anlage 3 in Version 21 liegt dem Projekt gar nicht
   vor.** Die Schlüsselverzeichnisse, gegen die `Leistungsbereich`,
   `PositionsnummerRegel` und `Kostentraegerdatei` prüfen, stammen also aus der
   künftigen Fassung, während gesendet wird nach Version 21. Das ist
   vermutlich unschädlich — aber es war eine Annahme und ist jetzt eine
   notierte.

**Eine fehlende oder kaputte Liste hält nichts auf.** Der Stand ist eine
Auskunft, keine Voraussetzung; sonst könnte eine unlesbare JSON-Datei die
Abrechnung anhalten.

**Damit ist auch D3 erledigt** (Versionsangabe sichtbar machen).

Eine Übersicht im Programm: welche Anlage, welche Version, welcher Stand,
welches Anwendungsdatum — gespeist aus einer gepflegten Liste zu den PDF unter
`Information/`. Dazu die Version, mit der die Anwendung **tatsächlich sendet**
(`SLGA:21:0:0`, heute `V21` in den Segmentbeschreibungen).

**Fertig, wenn:** eine Seite zeigt, welche Fassung gilt, welche vorliegt und ab
wann eine neue anzuwenden ist. — ✅

## G2. Auf neue Fassungen prüfen — ✅ erledigt am 17.09.2026

| | |
|---|---|
| **Größe** | M |
| **Hängt an** | G1 |
| **Stand** | **umgesetzt**: Knopf „Auf neue Unterlagen prüfen" auf derselben Seite, `java.net.http.HttpClient`, keine neue Bibliothek |

Alle drei Festlegungen sind so umgesetzt, wie sie unten stehen. Dazu kommt
eine vierte, die sich beim Bauen ergab: **der Abruf läuft auf einem eigenen
Faden.** Er hat zwei Zeitgrenzen von je zehn Sekunden; auf dem Zeichenfaden
wäre die Oberfläche so lange eingefroren, und das ist von einem Absturz nicht
zu unterscheiden.

### Der erste Lauf gegen die echte Seite hat die Umsetzung widerlegt

Die Prüfung war fertig, sieben Tests grün — und meldete gegen
`gkv-datenaustausch.de` **achtzehn neuere Fassungen**, darunter
`Anhang_03_Anlage_1_TP5_20120912.pdf`. Vierzehn Jahre alt.

Zwei Dinge waren angenommen und beide falsch:

| Annahme | Wirklichkeit |
|---|---|
| Die Seite listet die aktuellen Fassungen | Sie ist ein **Archiv** und führt jede Fassung seit 2008, von `Anlage_1_TP5_V7_20110610.pdf` an |
| Die Dateinamen dort sind unsere | `Anhang_3_Kostentraegerdatei_V10_20260414.pdf` heißt dort `Anhang_03_Anlage_1_TP5_V10_20260414.pdf` |

Verglichen wird jetzt **je Unterlage** statt gegen „alles Hinterlegte", über
einen neuen Eintrag `quellname` in `unterlagen.json` — den Namensanfang, unter
dem dieselbe Unterlage an der Quelle steht. **Ohne `quellname` wird eine
Unterlage nicht geprüft**, statt falsch verglichen zu werden; `Anhang 2` führt
kein Datum im Dateinamen und ist deshalb ausgenommen.

Unbekannte Namen werden übergangen, nicht gemeldet: Auf der Seite stehen
achtundneunzig Dateien, die meisten gehen diese Anwendung nichts an.

> **Nachgeprüft am 17.09.2026 gegen die echte Seite:** erreichbar, 98 Dateien
> erkannt, **nichts Neueres.** Das Projekt hält von allem, was es verfolgt, den
> jüngsten Stand.

### „Nichts erkannt" ist nicht „alles aktuell"

Der dritte Zustand neben *aktuell* und *neuer*: Die Seite war erreichbar, aber
kein Dateiname passte auf das Muster. Wer die beiden gleich behandelt, **meldet
Aktualität, weil eine Webseite umgebaut wurde** — und der Unterschied
entscheidet darüber, ob jemand nachsieht.

Die Anlagen stehen frei auf `gkv-datenaustausch.de`. Ein Abgleich „gibt es
dort etwas Neueres als das, was hier liegt?" ist machbar — **ohne neue
Bibliothek**, `java.net.http.HttpClient` gehört seit Java 11 zum
Sprachumfang.

**Drei Festlegungen gehören dazu, und sie sind bewusst zu treffen:**

1. **Auf Knopfdruck, nicht im Hintergrund.** Eine Anwendung, die
   Patientendaten hält, ruft nicht unaufgefordert im Netz an. Ein Menüpunkt
   „Auf neue Unterlagen prüfen" — mehr nicht.
2. **Ein Fehlschlag ist kein Fehler.** Kein Netz, Seite umgebaut, Zeitüberlauf:
   Das meldet sich als Hinweis und hält nichts auf. Sonst steht die Abrechnung
   still, weil eine Webseite sich geändert hat.
3. **Prüfen, nicht herunterladen.** Die Anwendung meldet „es gibt eine neuere
   Fassung, Stand X" — das Holen und Ablegen bleibt Handarbeit, weil danach
   ohnehin gelesen werden muss, was sich geändert hat.

**Fertig, wenn:** der Knopf bei unverändertem Stand „alles aktuell" meldet, bei
einer neueren Fassung deren Datum nennt, und bei fehlendem Netz einen Hinweis
gibt, der nichts blockiert. — ✅

## G3. Wartungskalender in der Anwendung

| | |
|---|---|
| **Größe** | S |
| **Hängt an** | G1, B1 (Zertifikatsdatum), B2 (Kostenträgerdatei) |

Eine Seite, die zeigt, **was wann fällig ist** — gespeist aus dem, was das
Programm ohnehin weiß:

| Was | Rhythmus | Woher das Datum kommt |
|---|---|---|
| Zertifikat erneuern | jährlich, Antrag 1–2 Wochen vorher | Betriebsdaten (B1), siehe D2 |
| Kostenträgerdatei | vierteljährlich | Dateidatum der eingelesenen Datei (B2) |
| Anlage 1 / Anlage 3 | bei neuer Fassung, 3 Monate Übergangsfrist | G1 und G2 |
| Positionsnummernverzeichnis | bei Vertragsänderung | nur als Merkposten — kein Datum bekannt |

**Fertig, wenn:** die Seite beim Start eine Fälligkeit anzeigt, sobald eine
innerhalb der nächsten sechs Wochen ansteht, und sonst schweigt.

> **Damit schrumpft die Wartung auf eine Aufgabe im Jahr** — das Zertifikat
> erneuern. Alles andere meldet sich selbst. Genau das ist die Bedingung,
> unter der der eigene Übermittlungsweg überhaupt sinnvoll ist.

---

# Ausdrücklich nicht in dieser Liste

| Was | Warum nicht |
|---|---|
| **Verschlüsselung** (Umsetzungsplan 2.2) | Zwei zusätzliche Elemente: die Beschreibung **GGT Anlage 16** liegt nicht unter `Information/`, und es bräuchte eine neue Krypto-Bibliothek im `pom.xml`. |
| **Versandweg E-Mail** (2.3) | Hängt an der Verschlüsselung. `GGT_Anlage_7_EMail.pdf` liegt zwar vor — ohne 2.2 ginge die Datei aber unverschlüsselt hinaus. |
| **IK, Zertifikat, Anmeldung** (2.1) | Formulare, keine Software. Gebührenfrei bis 79 € zzgl. USt., drei bis vier Arbeitstage. B1 legt die Angaben dafür an einer Stelle ab. |
| **Positionsnummern-Katalog** | Bundeseinheitliches Verzeichnis der Hebammenhilfe-Vergütungsvereinbarung — liegt nicht vor. Der **Aufbau** ist prüfbar, siehe A3. |
| **Tarifkennzeichen** (B5) | Steht im Vertrag mit der Kasse. Vorher nicht zu bauen, weil zwei der drei denkbaren Antworten einen falschen Umbau bedeuteten. |
| **Vollständiges Storno** | Der Weg dahin beginnt mit C2; was darüber hinausgeht, braucht die Rechnungsnummern-Frage (D.4 der Übergabe). |
| **Automatische Abrechnung** | Zurückgestellt — deine Entscheidung, nicht vergessen. |
| **Vier Branches, 98 Testdateien** | Deine Entscheidung, kein Bauauftrag. |
| **Datenbankverschlüsselung** | Betriebssystemsache (BitLocker), nicht Programmsache — aber Voraussetzung dafür, dass die IBAN aus B1 wirklich benutzt wird. |

---

# Reihenfolge

```
A1 Warnungen sichtbar ──┬── A2 Beanstandungen erklären
                        ├── A3 Positionsnummer-Aufbau
                        └── C1 Übermittlungsprotokoll

B1 Betriebsdaten ──┬── B3 Auftragsdatei   ── D2 Zertifikatswarnung ──┐
B2 Kostenträger ───┘                       ── D5 Umsatzsteuer        │
       │                                                             │
       └────────────────── F1…F6 Dienstleisterprofil ── F5 Blatt     │
                                                                     │
G1 Versionsstand ──┬── G2 Auf Neues prüfen                           │
                   └── G3 Wartungskalender ◄─────────────────────────┘

C2 Verarbeitungskennzeichen    (unabhängig)
D1 Trockenlauf · D3 Version · D4 Belegnummer · D6 Checkstyle    (unabhängig)
```

**Anfangen mit A1.** Nicht weil es das größte ist, sondern weil A2, A3 und C1
daran hängen und weil es heute ein echter Fehler ist: Eine Warnung, die
niemand sieht, ist keine Warnung.

**Danach B1**, weil daran der halbe Rest hängt — B3, F1 bis F6, D2 und über
das Zertifikatsdatum auch G3.

**G1 lässt sich jederzeit dazwischenschieben**, es hängt an nichts und
beantwortet eine Frage, die sonst niemand stellt: auf welchem Stand stehen wir
eigentlich.

Größenordnung des Ganzen: **etwa sechzehn bis vierzig Arbeitstage** — Block A
bis E rund elf bis dreißig, Block F etwa drei bis sechs, Block G etwa zwei bis
vier. Danach ist das Programm technisch vollständig, meldet seine Wartung
selbst und wartet nur noch auf Zertifikat, IK und die Zulassung der Kasse.
