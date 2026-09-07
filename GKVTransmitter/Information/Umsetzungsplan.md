---
title: "GKV-Abrechnung — Umsetzungsplan"
subtitle: "Weg in den Echtbetrieb, Wartung, Fehlerbehebung und die Vorbereitung auf spezifikationsgetriebenes Arbeiten"
date: "7. September 2026"
lang: de
toc-title: "Inhaltsverzeichnis"
---

# Wozu dieser Plan

Das Programm erzeugt und prüft heute vollständige DTA-Lieferungen. Was fehlt, ist alles, was danach kommt: der Weg zur Kasse, der Umgang mit dem, was zurückkommt, und die Pflege, die ein Verfahren verlangt, das sich jedes Jahr bewegt.

Dieser Plan beantwortet vier Fragen:

1. **Was ist in welcher Reihenfolge zu bauen** — und was hängt woran?
2. **Was ist danach dauerhaft zu tun**, damit es nicht unbemerkt veraltet?
3. **Was geschieht, wenn eine Nachricht beanstandet wird** — und wie erfährt die Anwenderin, was genau falsch ist?
4. **Wie wird gearbeitet**, damit ein Mensch und ein Agent an denselben Vorgaben arbeiten können?

> **Ein Grundsatz zieht sich durch alles Folgende.** Jede Angabe in einer Nachricht muss auf eine Fundstelle zeigen — Anlage, Abschnitt. Wo das versäumt wurde, sind Werte erfunden worden: der Leistungsbereich `H`, der logische Dateiname `HEB260907…`, eine neunstellige Positionsnummer. Alle drei sahen plausibel aus, alle drei waren falsch, keiner ist aufgefallen. **Was keine Fundstelle hat, ist eine Vermutung.**

# Teil 1 — Der Weg in den Echtbetrieb

## Die Gliederung folgt den Abhängigkeiten

Nicht dem Wunsch und nicht dem Aufwand. Entscheidend ist, was **von außen** gebraucht wird:

```
Phase 1   braucht nichts und niemanden          ← sofort machbar
Phase 2   braucht IK, Zertifikat, Anmeldung     ← Beschaffung nötig
Phase 3   braucht eine Datenannahmestelle       ← Abstimmung nötig
Phase 4   braucht deren Freigabe                ← Wartezeit
Phase 5   Echtbetrieb
```

**Phase 1 lohnt sich unabhängig von der Grundsatzentscheidung.** Ob am Ende selbst übertragen oder über eine Abrechnungsstelle abgerechnet wird — die Datei muss in beiden Fällen richtig sein, und die Rückmeldungen müssen in beiden Fällen ankommen. Die Entscheidung fällt erst am Übergang zu Phase 2.

In den Tabellen bedeutet die Spalte *Weg*: **A** = selbst übertragen, **B** = über eine Abrechnungsstelle, **C** = Datei von Hand weitergeben. Größenordnung: **S** unter einem Tag, **M** ein bis drei Tage, **L** mehr.

## Phase 0 — Was schon steht

| | Stand |
|---|---|
| Erfassung, Blaupausen, Gruppen, Abrechnungslauf | vollständig |
| DTA-Erzeugung (SLGA + SLLA) | vollständig |
| Prüfung mit acht Regeln als Tor vor dem Versand | vollständig |
| Fortlaufende, dauerhafte Datenaustauschreferenz | vollständig |
| Leistungsbereich und logischer Dateiname nach Vorgabe | seit 07.09.2026 |
| Übermittlungsart einstellbar (Test / Erprobung / Echt) | seit 07.09.2026 |
| Simulierte Kassengegenstelle zum Durchspielen | vollständig |

## Phase 1 — Ohne Außenkontakt

### 1.1 Warnungen sichtbar machen

| | |
|---|---|
| **Weg** | A, B, C |
| **Größe** | M |
| **Hängt an** | nichts |

**Das Wichtigste zuerst, weil ohne es der ganze Teil 3 Theorie bleibt.** `DtaDispatchService.generateAndRoute` sammelt den Prüfbericht und wirft ihn bei fehlerfreiem Lauf weg; die Oberfläche zeigt ihn nur im Ausnahmefall. Warnungen und Hinweise erreichen den Bildschirm nie. Die Dokumentation behauptete jahrelang das Gegenteil.

**Zu bauen:** `generateAndRoute` liefert statt `List<DispatchBatch>` ein Ergebnis mit Lieferungen **und** Bericht. Das zieht `AbrechnungService.createAndDispatch`, die Schnittstelle `Abrechnungslauf` und `AbrechnungsMaske` nach sich, dazu rund fünfzehn Teststellen.

**Nicht** über eine zweite Prüfung in der Maske lösen: die zöge `referenzen.naechste()` ein zweites Mal und verbrennte Datenaustauschreferenzen.

**Fertig, wenn:** ein Lauf mit einer Warnung erfolgreich durchläuft **und** die Warnung in der Meldungsecke steht; ein Lauf ohne Beanstandung zeigt keine Meldung außer der Erfolgsmeldung.

### 1.2 Beanstandungen erklären statt nur benennen

| | |
|---|---|
| **Weg** | A, B, C |
| **Größe** | M |
| **Hängt an** | 1.1 |

Siehe Teil 3. Jede der 45 Beanstandungen bekommt eine **Fundstelle** und eine **Handlungsanweisung**.

**Fertig, wenn:** kein Code ohne Fundstelle existiert — durchgesetzt durch einen Test, der über alle Regeln läuft.

### 1.3 Kostenträgerdatei einlesen

| | |
|---|---|
| **Weg** | A (und Wartung für alle) |
| **Größe** | M |
| **Hängt an** | nichts |

Die Zuordnung Kasse → Datenannahmestelle steht in einer öffentlichen, vierteljährlich erneuerten EDIFACT-Datei. Heute wird sie in `billing-office-endpoints.json` von Hand gepflegt — mit 23 einzelnen Kassen und damit **dem falschen Empfänger**.

**Zu bauen:** ein Leser für das KOTR-Format (der vorhandene `DtaDocument` trägt es), der zu einem Kassen-IK die Datenannahmestelle mit Entschlüsselungsbefugnis liefert:

```
IDK   IK der Versichertenkarte
 └─ VKG+01+<IK>            → Kostenträger
      └─ VKG+03+<IK>+…+50  → Datenannahmestelle MIT Entschlüsselungsbefugnis
           │                 (VKG+02 wäre ein Netzbetreiber OHNE)
           └─ DFU           → Adresse: 070 E-Mail, 016 FTAM, 080 KIM
```

**Fertig, wenn:** aus einer echten Kostenträgerdatei zu einem gegebenen Kassen-IK und Abrechnungscode 50 die richtige Annahmestelle samt Adresse ermittelt wird; eine fehlende Zuordnung meldet sich, statt still auf einen Sammelordner auszuweichen.

### 1.4 Verarbeitungskennzeichen: Korrektur und Nachforderung

| | |
|---|---|
| **Weg** | A, B, C |
| **Größe** | S |
| **Hängt an** | nichts |

Das Programm kann heute nur `FKT+01` — „Abrechnung ohne Besonderheiten". Anlage 3, Abschnitt 8.1.7 kennt außerdem **02 Nachforderung, 03 Zuzahlungsforderung, 04 Korrekturrechnung, 10 Wiederaufnahme**. Ohne diese Werte gibt es **keinen Weg, auf eine Zurückweisung zu antworten** — man kann dieselbe Rechnung nur noch einmal als Erstabrechnung schicken, was die Kasse als Doppelabrechnung sieht.

**Vorher zu klären:** Anlage 1, Abschnitt 7.1 regelt, welcher Wert wann zulässig ist. Diese Tabelle ist zu lesen, nicht zu raten.

**Fertig, wenn:** eine zurückgewiesene Abrechnung als Korrekturrechnung neu erzeugt werden kann und die Prüfung den Wert gegen Abschnitt 7.1 hält.

### 1.5 Übermittlungsprotokoll

| | |
|---|---|
| **Weg** | A (Pflicht), B und C (nützlich) |
| **Größe** | M |
| **Hängt an** | 1.1 |

Anlage 1, Abschnitt 3 Absatz 2 verlangt eine Dokumentation des Datenaustauschs, **mindestens zwei Jahre aufzubewahren**. Anhang 1, Abschnitt 4.5 nennt die Mindestinhalte: physikalischer Dateiname, Erstellungsdatum, laufende Nummer, Kommunikationspartner, Beginn und Ende der Übermittlung, Dateigröße, Verarbeitungshinweise, Richtung, fehlerfrei oder fehlerhaft, im Fehlerfall der Fehlerstatus.

**Das ist fast ein Nebenprodukt** — die Angaben entstehen ohnehin beim Versand, sie werden nur nirgends festgehalten. Eine Tabelle in der Datenbank, eine Übersicht in der Oberfläche.

Dazu gehört die zweite Pflicht aus Absatz 4: eine **Sicherungskopie bis zur Bezahlung**. Die Dateien liegen bereits unter `staging/`; es fehlt der Zustand „bezahlt" und damit der Zeitpunkt, ab dem sie weg dürfen.

**Fertig, wenn:** jede erzeugte und jede zugestellte Datei einen Protokolleintrag hat und die Übersicht zeigt, was noch auf Zahlung wartet.

### 1.6 Auftragsdatei und physikalischer Dateiname

| | |
|---|---|
| **Weg** | A |
| **Größe** | M |
| **Hängt an** | 1.3 |

Zu jeder Nutzdatendatei gehört eine unverschlüsselte Auftragsdatei mit den Transportangaben (GGT Anlage 2). Prüfstufe 1 prüft ausdrücklich, ob die Dateien **paarweise** ankommen.

Der physikalische Dateiname ist vorgeschrieben (Anhang 1, Abschnitt 4.3): `E` oder `T`, dann `SOL`, dann `0`, dann eine dreistellige Transfernummer — `ESOL0001`, `TSOL0001`. Der logische Dateiname im UNB und in der Auftragsdatei müssen **übereinstimmen**.

**Achtung:** auch eine Erprobungsdatei trägt den physikalischen Namen einer **Test**datei. Testindikator `1` und `TSOL` gehören zusammen.

**Fertig, wenn:** zu jeder Nutzdatei eine Auftragsdatei entsteht, beide denselben logischen Dateinamen tragen und der physikalische Name zur eingestellten Übermittlungsart passt.

## Phase 2 — Beschaffung und Verschlüsselung

Hier fällt die Entscheidung: **selbst übertragen oder nicht.** Wer sie verneint, hört nach Phase 1 auf und gibt die geprüfte Datei weiter.

### 2.1 Beschaffen

| Was | Wo | Kosten |
|---|---|---|
| Betriebsstätten-IK | ARGE·IK | gebührenfrei |
| Zertifikat | ITSG Trust Center | 79 € zzgl. USt., dann 49 € jährlich |
| Anmeldung als Kommunikationspartner | bei der Datenannahmestelle | keine |

Das Zertifikat gilt **für alle Kassen**, nicht je Kasse; die öffentlichen Schlüssel aller Annahmestellen kommen als Schlüsselliste kostenlos mit. Gültigkeit ein Jahr, Ausstellung in drei bis vier Arbeitstagen.

### 2.2 Verschlüsselung

| | |
|---|---|
| **Weg** | A |
| **Größe** | L |
| **Hängt an** | 2.1, 1.6 |

Nach der Security-Schnittstelle (GGT Anlage 16), mit dem öffentlichen Schlüssel der Datenannahmestelle **mit Entschlüsselungsbefugnis** — nicht dem eines zwischengeschalteten Netzbetreibers.

**Der private Schlüssel gehört nicht in die Datenbank** und nicht in eine Datei daneben. Er gehört in den Windows-Anmeldeinformationsspeicher oder einen Schlüsselbund.

**Fertig, wenn:** eine erzeugte Datei sich mit dem Schlüssel der Annahmestelle verschlüsseln und im Test mit dem passenden privaten Schlüssel wieder entschlüsseln lässt.

### 2.3 Versandweg

| | |
|---|---|
| **Weg** | A |
| **Größe** | M |
| **Hängt an** | 2.2 |

Eine weitere Umsetzung von `BillingOfficeTransport` — E-Mail (DFÜ-Schlüssel `070`), weil das heute der Weg ist, den die Annahmestellen für Hebammenhilfe veröffentlichen. `probe` muss die Erreichbarkeit prüfen können, sonst fällt eine Fehlkonfiguration erst im Echtbetrieb auf.

**Der übrige Ablauf bleibt unverändert** — Erzeugung, Prüfung, Zuordnung, Auswertung liegen hinter derselben Schnittstelle.

## Phase 3 — Testverfahren

| | |
|---|---|
| **Weg** | A |
| **Größe** | S Bauaufwand, offene Wartezeit |

Einstellung `uebermittlungsart = test`, Testindikator `0`, physikalischer Name `TSOL…`. Geprüft werden die **Prüfstufen 1 bis 3** bei der Datenannahmestelle. Es fließt kein Geld, und es ist ausdrücklich auch für selbst entwickelte Software vorgesehen.

**Vorher abzustimmen:** Beginn des Tests, zu verwendende IK, Umfang. Anhang 2, Abschnitt 5.

**Fertig, wenn:** die Testdatei die Prüfstufen 1 bis 3 fehlerlos durchläuft und die Rückmeldung das bestätigt.

## Phase 4 — Erprobung

| | |
|---|---|
| **Weg** | A |
| **Größe** | keine Bauarbeit, Wartezeit |

Einstellung `uebermittlungsart = erprobung`, Testindikator `1`, physikalischer Name weiterhin `TSOL…`. Geprüft wird die **vollständige Verarbeitung**. Die Phase endet nicht durch Zeitablauf, sondern **mit der Zulassung zum Echtverfahren durch die Kasse**.

Vor der ersten Übermittlung sind die Einzelheiten mit dem Empfänger abzustimmen — das ist vorgeschrieben (Anlage 1, Abschnitt 2), kein Entgegenkommen.

## Phase 5 — Echtbetrieb

Einstellung `uebermittlungsart = echt`, Testindikator `2`, physikalischer Name `ESOL…`. Ab hier gilt jede Lieferung als Forderung.

**Voraussetzungen, die keine Software erfüllt:** die Zulassung aus Phase 4, das Übermittlungsprotokoll aus 1.5, und eine Sicherungskopie bis zur Bezahlung.

## Reihenfolge auf einen Blick

```
1.1 Warnungen sichtbar ──┬── 1.2 Beanstandungen erklären
                         └── 1.5 Übermittlungsprotokoll
1.3 Kostenträgerdatei ────── 1.6 Auftragsdatei ── 2.2 Verschlüsselung ── 2.3 Versandweg
1.4 Verarbeitungskennzeichen (unabhängig)              │
                                                 2.1 Beschaffen
                                                       │
                                    Phase 3 Test ── Phase 4 Erprobung ── Phase 5 Echt
```

**Der kritische Pfad läuft über 1.3.** Wer zuerst die Kostenträgerdatei liest, hat den richtigen Empfänger — und ohne den ist alles Weitere an die falsche Adresse gerichtet.

# Teil 2 — Wartungsplan

## Warum es diesen Teil gibt

Das Verfahren steht nicht still, und **Veralten fällt nicht auf.** Der Beleg liegt im Projekt selbst: die verbindlichen Anlagen unter `Information/` waren am 07.09.2026 vom Juli und seit Monaten überholt, ohne dass es jemandem aufgefallen wäre.

Bei einer Abrechnungsstelle ist diese Pflege im Preis enthalten. Wer selbst überträgt, übernimmt sie.

## Der Kalender

| Wann | Was | Dauer | Wenn versäumt |
|---|---|---|---|
| **Quartalsanfang** | Kostenträgerdatei der genutzten Kassenarten laden | 10 min | Lieferung an eine unzuständige Stelle |
| **Jährlich, 1 Monat vor Ablauf** | Zertifikat erneuern (49 €) | 30 min + 3–4 Tage Bearbeitung | **Datenaustausch steht still** |
| **Jährlich** | Anlage 1 und 3 auf neue Version prüfen | 1 h | Datei wird nach der Übergangsfrist abgewiesen |
| **Bei Vertragsänderung** | Positionsnummern und Vergütungssätze | 1 h | falsche Beträge, Rückforderung |
| **Nach jedem Versand** | Rückmeldung auswerten, offene Zahlungen prüfen | 5 min | unbemerkt nicht bezahlte Abrechnungen |

Übergangsfristen beim Versionswechsel betragen in der Regel **drei Monate**. Das ist der Puffer — nicht mehr.

## Was die Wartung klein hält

Drei Bausteine verwandeln wiederkehrende Handarbeit in einmalige Arbeit:

1. **Kostenträgerdatei einlesen** (1.3) — aus vier Pflegeterminen im Jahr wird ein Dateiaustausch.
2. **Ablaufwarnung für das Zertifikat** — die Anwendung kennt das Datum und kann rechtzeitig erinnern. Größe **S**, Wirkung groß: das ist der einzige Wartungspunkt, dessen Versäumnis den Betrieb anhält.
3. **Versionsangabe sichtbar machen** — der Nachrichtentyp trägt sie bereits (`SLGA:21:0:0`). Wer sieht, mit welcher Version er sendet, bemerkt einen Wechsel.

Mit diesen dreien bleibt **eine** Aufgabe im Jahr: das Zertifikat erneuern.

## Prüfliste für den Quartalswechsel

- [ ] Kostenträgerdateien der genutzten Kassenarten geladen
- [ ] Zuordnung Kasse → Annahmestelle unverändert? Sonst: was hat sich geändert?
- [ ] Läuft das Zertifikat in den nächsten drei Monaten ab?
- [ ] Neue Version von Anlage 1 oder 3 veröffentlicht?
- [ ] Offene Forderungen ohne Zahlungseingang älter als acht Wochen?
- [ ] `mvn clean test` grün, Paket baut

# Teil 3 — Fehlerbehebung und Rückmeldungen

## Wo geprüft wird

```
bei uns          Prüfstufen 1–3 vorweggenommen, acht Regeln    ← hält den Versand auf
Annahmestelle    Prüfstufe 1  Datei, Dateipaar, Anmeldung
                 Prüfstufe 2  Segmentfolge, Feldart, -typ, -länge
                 Prüfstufe 3  Inhalte gegen die Schlüsselverzeichnisse
Krankenkasse     Prüfstufe 4  vertrags-, versicherungs-, leistungsrechtlich
```

**Alles, was wir vorher finden, kostet keine Woche Postlauf.** Deshalb ist die eigene Prüfung kein Beiwerk — der Absender hat sicherzustellen, dass nur geprüfte Datensätze übermittelt werden (Anlage 1, Abschnitt 3 Absatz 3).

Prüfstufe 4 kann kein Programm vorwegnehmen. Sie braucht den Vertrag.

## Was eine gute Beanstandung enthält

Heute trägt ein Befund drei Angaben: **Code**, **Ort** (`UNB (Zeile 1)`) und einen **Text**. Das ist mehr als üblich und trotzdem zu wenig. Es fehlen zwei:

| Angabe | Heute | Beispiel |
|---|---|---|
| **Wo** | ✔ | `ENF (Zeile 15)` |
| **Was** | ✔ | `POSITION_HEBAMME_LAENGE` |
| **Warum** | im Text vermischt | „Für Hebammenhilfe sind vier oder fünf Stellen vorgesehen" |
| **Fundstelle** | **fehlt** | Anlage 3, Abschnitt 8.2.6 |
| **Wie zu beheben** | **fehlt** | „Positionsnummer in der Blaupause ‚Geburtsvorbereitung' berichtigen" |

**Die Fundstelle ist das Wichtigste.** Sie hilft nicht nur beim Nachschlagen — sie ist die Prüfung, ob die Regel überhaupt Grundlage hat. Genau dieser Zwang hätte die drei erfundenen Werte verhindert.

**Die Handlungsanweisung ist das Zweitwichtigste.** „Die Menge ist 0,00" sagt, was ist. „Trage bei Anna Berger die Zahl der wahrgenommenen Termine ein" sagt, was zu tun ist — und nur das Zweite bringt jemanden weiter, der die Segmentstruktur nicht kennt.

**Umzusetzen** als zwei zusätzliche Felder in `ValidationMessage`. Ein Test läuft über alle Regeln und schlägt fehl, sobald ein Code ohne Fundstelle auskommt.

## Was zurückkommt, und was es bedeutet

`BillingOfficeResponseParser` ordnet eine Kassenantwort ein. Die Einstufung entscheidet über die Reaktion — sie zu verwechseln kostet Wochen:

| Einstufung | Bedeutung | Reaktion | Verarbeitungskennzeichen |
|---|---|---|---|
| `ACCEPTED` | angenommen | auf Zahlung warten | – |
| `SYNTAX_ERROR` | technisch falsch aufgebaut | Datei **neu erzeugen**, Inhalt bleibt | `01` |
| `REJECTED` | lesbar, fachlich beanstandet | Abrechnung **korrigieren** | `04` Korrekturrechnung |
| `TECHNICAL_ERROR` | Übertragungsproblem | **erneut senden**, nichts ändern | unverändert |
| `UNKNOWN` | nicht einzuordnen | **ein Mensch muss draufschauen** | – |

Zwei Eigenschaften dürfen beim Erweitern nicht verloren gehen: die Reihenfolge geht vom Spezifischen zum Allgemeinen (sonst gilt jeder technische Fehler als fachliche Ablehnung), und die Suchbegriffe sind an Wortgrenzen gebunden (sonst trifft `ok` in „Protokoll"). **Eine nicht einzuordnende Antwort gilt nie als Annahme.**

## Der Fehlerkreislauf

```
Versand ──> Rückmeldung ──> Einstufung
                               │
     ┌─────────────────────────┼──────────────────────┬────────────────┐
     ▼                         ▼                      ▼                ▼
 ACCEPTED                SYNTAX_ERROR            REJECTED       TECHNICAL_ERROR
     │                         │                      │                │
 auf Zahlung           Datei neu erzeugen      Daten berichtigen   erneut senden
     │                         │                      │                │
 bezahlt?                      └──────────┬───────────┘                │
     │                                    ▼                            │
     │                          Korrekturrechnung (FKT 04)             │
     │                                    │                            │
     └──────────> Sicherungskopie darf weg └────────> erneuter Versand <┘
```

**Heute fehlt der rechte Ast vollständig.** Das Programm kennt nur `FKT+01`. Auf eine Zurückweisung lässt sich damit nicht antworten — dieselbe Rechnung noch einmal zu schicken sieht für die Kasse aus wie eine Doppelabrechnung. Das ist Arbeitspaket 1.4, und es ist klein.

## Was dafür zu bauen ist

| | Größe | Zweck |
|---|---|---|
| Fundstelle und Handlungsanweisung je Befund | M | die Anwenderin weiß, was zu tun ist |
| Verarbeitungskennzeichen 02/03/04/10 | S | überhaupt auf eine Zurückweisung antworten können |
| Rückmeldungsjournal | M | welche Lieferung wurde wann wie beantwortet |
| Wiedervorlage für offene Forderungen | S | eine Abrechnung darf nicht still verschwinden |
| Zustand „bezahlt" | S | Ende der Aufbewahrungspflicht für die Sicherungskopie |

# Teil 4 — Vorbereitung auf spezifikationsgetriebenes Arbeiten

## Warum gerade hier

Dieses Projekt ist ein besonders guter und ein besonders gefährlicher Fall für agentische Entwicklung.

**Gut**, weil die Fachlichkeit vollständig schriftlich vorliegt: die Technische Anlage, die Anhänge, die Schlüsselverzeichnisse. Es gibt für fast jede Frage eine Fundstelle.

**Gefährlich**, weil eine erfundene Regel nicht auffällt. Ein falscher Leistungsbereich erzeugt eine Datei, die sich übersetzt, prüft, versendet — und erst Wochen später von der Kasse zurückkommt. **Der Übersetzer fängt hier nichts, die Tests fangen nur, was jemand als Erwartung hingeschrieben hat.** Genau so sind `H`, `HEB260907…` und die neunstellige Positionsnummer entstanden: aus einer Beispieldatei übernommen, nie gegen die Vorgabe gehalten.

## Was schon spezifikationsartig ist

| Vorhandenes | Was es leistet |
|---|---|
| Fünf Skills unter `.claude/skills/` | Fachwissen, Architekturregeln, wo was hingehört |
| `segments/*.json` | Feldarten, Längen, Erklärungen der DTA-Segmente |
| `codes/*.json` | Schlüsselwerte mit Herkunft |
| `Information/Valide.DTA` | ausführbare Messlatte: muss unbeanstandet durchlaufen |
| Testkonventionen | deutsche `@DisplayName` in ganzen Sätzen — lesbar als Anforderung |
| `Naechste_Schritte.md` | Merksätze und Fallstricke als Erfahrungsspeicher |

Das ist eine gute Grundlage. Es fehlt die Verbindung zwischen **Vorgabe** und **Code**.

## Was fehlt: die Fundstelle als Pflichtangabe

**Der eine Baustein, der am meisten trägt.** Jede Regel, jeder Schlüsselwert, jedes Format zeigt auf Anlage und Abschnitt — maschinenlesbar, nicht nur im Fließtext eines Kommentars.

```json
{
  "code": "50",
  "sammelgruppe": "F",
  "fundstelle": { "dokument": "Anlage 3", "abschnitt": "8.1.5", "version": "22" }
}
```

Damit wird zweierlei möglich, das heute nicht geht: **eine Änderung der Vorgabe lässt sich auf die betroffenen Stellen abbilden** — bei einer neuen Anlage 3 zeigt eine Abfrage, was zu prüfen ist. Und **eine Regel ohne Fundstelle fällt auf**, statt sich als Vermutung zu tarnen.

## Der Arbeitszyklus

```
Spezifikation ──> Plan ──> Umsetzung ──> Nachweis ──> Übergabe
     │              │           │            │            │
  Was und        Reihen-     Code +      Test grün,   Merksatz,
  woher          folge       Tests       Gegenprobe   falls gelernt
     │
  Fundstelle ist Pflicht
```

**Der Nachweis ist mehr als „Tests grün".** Zu jeder Verhaltensänderung gehört eine **Gegenprobe**: ein Test, der ohne die Änderung fehlschlägt. Ein Test, der in beiden Fassungen besteht, prüft nichts — dieser Fall ist in diesem Projekt bereits vorgekommen.

## Vorlage für eine Spezifikation

Abzulegen unter `Information/spezifikationen/<kurzname>.md`, eine je Arbeitspaket:

```markdown
# <Kurzname>

## Ziel
Ein Satz. Was kann jemand danach, was vorher nicht ging?

## Fundstelle
Anlage 1, Abschnitt 5.4 — wörtliches Zitat der maßgeblichen Stelle.
Ohne Fundstelle keine Spezifikation.

## Beispiel
Eingabe  → Ausgabe, konkret und nachrechenbar.

## Abnahmekriterien
Als Testnamen formuliert, deutsch, ganze Sätze:
- "Der Leistungsbereich folgt dem Abrechnungscode und nicht einem Festwert"
- "Ein unbekannter Abrechnungscode fällt auf J und nicht auf einen fremden Bereich"

## Nicht-Ziele
Was ausdrücklich nicht dazugehört — verhindert Ausufern.

## Gegenprobe
Welcher Test schlägt fehl, wenn die Änderung fehlt?
```

## Was ein Agent hier wissen muss

Diese Punkte kosten sonst jedes Mal Zeit; sie stehen ausführlich in `Naechste_Schritte.md`:

- **`mvn install -DskipTests` nach jeder Änderung an `gkv-core`** — sonst läuft `gkv-ui` gegen ein altes Jar.
- **`mvn clean test` vor dem Urteil** — inkrementelle Läufe können grün lügen.
- **`gkv.home` in Werkzeugen setzen**, nicht nur `gkv.db.path`. Sonst schreibt ein Testlauf in den echten Ausgangsordner — das ist einmal mit 98 Dateien passiert.
- **Kein `setStyle` im Quelltext**, keine Dialogfenster, nichts Neues in `View`.
- **IK in Testdaten brauchen eine gültige Prüfziffer.**
- **`Valide.DTA` muss unbeanstandet durchlaufen** — jeder Einzeltest verletzt danach genau eine Vorgabe.

## Wann fertig fertig ist

Eine Aufgabe gilt als erledigt, wenn:

1. `mvn clean test` grün ist — vollständig, nicht inkrementell,
2. eine **Gegenprobe** zeigt, dass der neue Test die Änderung wirklich prüft,
3. jede neue Regel eine **Fundstelle** nennt,
4. Checkstyle nicht mehr Warnungen meldet als vorher,
5. die Dokumentation die Änderung trägt — und bei Oberflächenarbeit die **Vorschau** angesehen wurde, in beiden Fassungen.

Punkt 5 ist nicht Formsache: der Dunkelmodus hatte zwei Fehler, die kein Test gefunden hat und das erste Bild sofort zeigte.

# Teil 5 — Zukunftspläne

## Als Nächstes, wenn der Echtbetrieb läuft

| Vorhaben | Warum | Größe |
|---|---|---|
| **Stornierung und Nachberechnung** | sobald real abgerechnet wird, unvermeidlich | M |
| **Sammelrechnung** | mehrere Einzelrechnungen unter einem Kostenträger-IK | M |
| **Echte Rechnungsnummern** | heute steht fest `REC+00000000:0` | S |
| **Positionsnummernverzeichnis** | macht aus der Formprüfung eine fachliche | M |

## Datensicherheit

Nach Wirkung geordnet, nicht nach Aufwand:

1. **Festplattenverschlüsselung auf dem Arbeitsgerät** — deckt den häufigsten realen Fall ab (Verlust, Diebstahl) und kostet keine Zeile Quelltext. **Zuerst.**
2. **Sicherung und Aufbewahrungsfristen** — was wann gelöscht werden darf und muss.
3. **Datenbankverschlüsselung** — der kleinere Gewinn, solange Punkt 1 fehlt.
4. **Anmeldung und Zugriffsprotokoll** — erst sinnvoll, wenn mehr als eine Person arbeitet.

Sobald echte Zugangsdaten dazukommen: **nicht in die Datenbank**, sondern in den Anmeldeinformationsspeicher des Betriebssystems.

## Weiter draußen

**KIM statt E-Mail.** Wird KIM verwendet, entfallen Krankenkassenkommunikationssystem, Auftragsdatei und SECON — drei der aufwendigsten Bausteine (GGT Anlage 20). Heute veröffentlicht **keine** Annahmestelle für Sonstige Leistungserbringer eine KIM-Adresse; der DFÜ-Schlüssel `080` ist aber vorgesehen. **Beim Quartalswechsel lohnt der Blick in die Kostenträgerdatei.** Sobald dort eine `080` auftaucht, ändert sich die Rechnung: die Bausteine 1.6 und 2.2 wären dann nicht mehr nötig, dafür ein TI-Anschluss mit SMC-B und laufenden Kosten.

**Weitere Leistungsbereiche.** `Leistungsbereich` kennt bereits alle Sammelgruppenschlüssel aus Anlage 3; es fehlen nur Einträge in `codes/abrechnungscodes.json`. Der Schritt ist klein, sobald jemand ihn braucht.

**Automatische Abrechnung** (monatlich, ohne Anstoß) — zurückgestellt. Erst soll der Weg von Hand verlässlich sein. Ein automatischer Lauf, der Forderungen an Krankenkassen stellt, ohne dass jemand hinsieht, ist die falsche Reihenfolge.

## Was bewusst nicht geplant ist

- **Mehrbenutzerbetrieb.** SQLite lässt einen Schreiber zu; das reicht für eine Praxis und ist eine bewusste Grenze.
- **Privatabrechnung.** Ein anderes Verfahren mit eigenen Regeln.
- **Eine Weboberfläche.** Die Daten liegen beim Benutzer, und das ist ein Vorzug, kein Mangel.

# Anhang — Woher die Angaben stammen

| Aussage | Fundstelle |
|---|---|
| Empfänger ist die Datenannahmestelle je Kassenart | Anlage 1, Abschnitte 3 und 5.3.1 |
| Testindikator 0 / 1 / 2 | Anlage 1, Abschnitt 5.4 |
| Leistungsbereich = Sammelgruppenschlüssel | Anlage 1, Abschnitt 5.4; Anlage 3, Abschnitt 8.1.14 |
| Abrechnungscode 50 = Hebamme | Anlage 3, Abschnitt 8.1.5 |
| Positionsnummer Hebammenhilfe 4–5 Stellen | Anlage 3, Abschnitt 8.2.6 |
| Verarbeitungskennzeichen 01/02/03/04/10 | Anlage 3, Abschnitt 8.1.7; Anwendbarkeit in Anlage 1, Abschnitt 7.1 |
| Logischer Dateiname | Anhang 1, Abschnitt 4.2 |
| Physikalischer Dateiname | Anhang 1, Abschnitt 4.3 |
| Dokumentation, zwei Jahre | Anlage 1, Abschnitt 3 Abs. 2; Anhang 1, Abschnitt 4.5 |
| Sicherungskopie bis zur Bezahlung | Anlage 1, Abschnitt 3 Abs. 4 |
| Erprobung vorgeschrieben | Anlage 1, Abschnitt 2 |
| Test- und Erprobungsverfahren | Anhang 2, Abschnitte 3 bis 6 |
| Aufbau der Kostenträgerdatei | Anhang 3, Abschnitte 5 und 8 |
| Auftragsdatei | GGT Anlage 2 |
| KIM ersetzt KKS, Auftragsdatei, SECON | GGT Anlage 20, Abschnitt 1 |
| Zertifikat: Preis, Gültigkeit, je IK | ITSG Trust Center |
| IK gebührenfrei | ARGE·IK |

Alle Dokumente liegen unter `Information/` oder sind dort verlinkt. **Wer eine Angabe ändert, ändert auch die Fundstelle** — oder stellt fest, dass er keine hat.
