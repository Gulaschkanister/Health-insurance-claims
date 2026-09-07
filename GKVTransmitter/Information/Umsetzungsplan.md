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
5. **Wie wird der Alltag leichter** — von der Kursstunde bis zum Zahlungseingang?

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
| **Hängt an** | 1.3, 1.7 |

Zu jeder Nutzdatendatei gehört eine unverschlüsselte Auftragsdatei mit den Transportangaben (GGT Anlage 2). Prüfstufe 1 prüft ausdrücklich, ob die Dateien **paarweise** ankommen.

Der physikalische Dateiname ist vorgeschrieben (Anhang 1, Abschnitt 4.3): `E` oder `T`, dann `SOL`, dann `0`, dann eine dreistellige Transfernummer — `ESOL0001`, `TSOL0001`. Der logische Dateiname im UNB und in der Auftragsdatei müssen **übereinstimmen**.

**Achtung:** auch eine Erprobungsdatei trägt den physikalischen Namen einer **Test**datei. Testindikator `1` und `TSOL` gehören zusammen.

**Fertig, wenn:** zu jeder Nutzdatei eine Auftragsdatei entsteht, beide denselben logischen Dateinamen tragen und der physikalische Name zur eingestellten Übermittlungsart passt.


### 1.7 Betriebsdaten — die eigenen Angaben an einem Ort

| | |
|---|---|
| **Weg** | A, B, C |
| **Größe** | M |
| **Hängt an** | nichts — **1.6 und 2.1 hängen daran** |

Simons Vorschlag vom 07.09.2026: *„wie wär's noch, wenn man noch seine Daten/Konto für die Registrierung auch irgendwie hinzufügen kann?"*

**Die Anwendung weiß heute nicht, wer sie betreibt.** Der Absender einer Datei stammt aus dem Dienstleister, der je Abrechnung ausgewählt wird — als wäre die absendende Stelle eine Eigenschaft des einzelnen Vorgangs. Sie ist es nicht: sie ist eine Eigenschaft des Betriebs, sie ändert sich fast nie, und sie wird bei **jeder** Registrierung wieder gebraucht.

#### Was hineingehört

| Angabe | Wofür |
|---|---|
| Name der Praxis, Anschrift | Zertifikatsantrag, Anmeldung bei der Annahmestelle |
| **Eigenes IK** | Absender im `UNB`, `FKT`, Stellen 3–8 des logischen Dateinamens |
| **Rolle: Selbstabrechner oder Abrechnungsstelle** | neunte Stelle des logischen Dateinamens (`S` oder `A`) |
| Steuernummer, Umsatzsteuerpflicht | `UST`-Segment |
| **Bankverbindung (IBAN, BIC)** | Anmeldung bei den Kassen — **nicht** für die Datei, siehe unten |
| Ansprechpartner, Telefon, E-Mail | Anmeldung als Kommunikationspartner; die Annahmestelle braucht jemanden, den sie erreichen kann |
| Zertifikat: ausgestellt am, gültig bis | Ablaufwarnung, siehe Wartungsplan |

#### Die Bankverbindung steht in keiner Nachricht

**Nachgeschlagen, nicht angenommen:** Anlage 1 kennt kein Feld für IBAN, Konto oder Geldinstitut — in keinem der dreizehn Segmente. Die Kasse zahlt auf das Konto, das zum Institutionskennzeichen hinterlegt ist. Hinterlegt wird es bei der **Anmeldung**, nicht bei der Abrechnung.

Das entwertet den Vorschlag nicht, es schärft ihn: **die Maske ist kein Nachrichtenlieferant, sondern ein Aktenordner.** Und genau darin liegt ihr Nutzen — ARGE·IK, ITSG-Trust-Center und jede Datenannahmestelle fragen im Kern dieselben Angaben ab. Heute liegen sie auf Papier, in einer alten E-Mail oder im Kopf.

Zwei Angaben sind die Ausnahme und gehen sehr wohl in die Nachricht: **das eigene IK und die Rolle.** Ohne sie ist der logische Dateiname nicht bildbar — er wird heute aus dem Dienstleister-IK gebildet und nimmt `Selbstabrechner` als gesetzt an.

#### Was das nebenbei geraderückt

Der Absender wandert von der einzelnen Abrechnung an den Betrieb. Für eine allein arbeitende Hebamme ist das dasselbe IK und fällt nicht auf; sobald eine zweite Person abrechnet oder eine Abrechnungsstelle einspringt, ist es der Unterschied zwischen richtiger und falscher Datei.

#### Wo die Daten liegen

In der Datenbank, eine Zeile, eigene Entität — nicht in den Einstellungen. Einstellungen sind Wahlmöglichkeiten, Betriebsdaten sind Stammdaten mit Struktur und Prüfregeln (das IK gegen die Prüfziffer, wie überall im Programm).

> **Die IBAN ist die erste wirklich schützenswerte Angabe außerhalb der Patientendaten**, und die Datenbank ist unverschlüsselt. Das verschiebt die Festplattenverschlüsselung aus Teil 5 nach vorn: sie sollte stehen, bevor hier eine Kontoverbindung eingetragen wird.
>
> **Keine Zugangsdaten und keine privaten Schlüssel hier.** Die gehören in den Windows-Anmeldeinformationsspeicher. Ein Ablaufdatum ist unbedenklich, ein Schlüssel nicht.

#### Fertig, wenn

- die Betriebsdaten in einer eigenen Maske erfasst und geändert werden können,
- das eigene IK gegen die Prüfziffer geprüft wird,
- der logische Dateiname Rolle und IK **von dort** nimmt und nicht mehr aus dem Dienstleister,
- fehlende Pflichtangaben den Abrechnungslauf mit einer verständlichen Meldung anhalten, statt eine Datei mit Lücken zu erzeugen.

#### Naheliegende Erweiterung

Ein **Registrierungsblatt** zum Ausdrucken oder als PDF: alle Angaben auf einer Seite, in der Form, die die Anmeldung verlangt. Damit wird aus dem Aktenordner ein Formular, das man nur noch unterschreiben muss. Größe **S**, sobald die Daten einmal stehen — und es ist der Punkt, an dem die Maske sich das erste Mal auszahlt.

## Phase 2 — Beschaffung und Verschlüsselung

Hier fällt die Entscheidung: **selbst übertragen oder nicht.** Wer sie verneint, hört nach Phase 1 auf und gibt die geprüfte Datei weiter.

### 2.1 Beschaffen

| Was | Wo | Kosten |
|---|---|---|
| Betriebsstätten-IK | ARGE·IK | gebührenfrei |
| Zertifikat | ITSG Trust Center | 79 € zzgl. USt., dann 49 € jährlich |
| Anmeldung als Kommunikationspartner | bei der Datenannahmestelle | keine |

Das Zertifikat gilt **für alle Kassen**, nicht je Kasse; die öffentlichen Schlüssel aller Annahmestellen kommen als Schlüsselliste kostenlos mit. Gültigkeit ein Jahr, Ausstellung in drei bis vier Arbeitstagen.

**Alle drei Anträge fragen im Kern dieselben Angaben ab.** Wenn 1.7 steht, sind sie an einer Stelle und müssen nicht dreimal zusammengesucht werden.

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
1.3 Kostenträgerdatei ──┐
1.7 Betriebsdaten ──────┴── 1.6 Auftragsdatei ── 2.2 Verschlüsselung ── 2.3 Versandweg
         │                                                │
         └── 2.1 Beschaffen (IK, Zertifikat, Anmeldung) ───┘
1.4 Verarbeitungskennzeichen (unabhängig)

                       Phase 3 Test ── Phase 4 Erprobung ── Phase 5 Echt
```

**Der kritische Pfad läuft über 1.3 und 1.7.** Die Kostenträgerdatei sagt, an wen geliefert wird; die Betriebsdaten sagen, wer liefert. Fehlt eines von beiden, ist die Datei falsch adressiert oder falsch unterschrieben.

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
2. **Ablaufwarnung für das Zertifikat** — die Anwendung kennt das Datum aus den Betriebsdaten (1.7) und kann rechtzeitig erinnern. Größe **S**, Wirkung groß: das ist der einzige Wartungspunkt, dessen Versäumnis den Betrieb anhält.
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

1. **Festplattenverschlüsselung auf dem Arbeitsgerät** — deckt den häufigsten realen Fall ab (Verlust, Diebstahl) und kostet keine Zeile Quelltext. **Zuerst** — und spätestens, bevor mit 1.7 eine Kontoverbindung in die unverschlüsselte Datenbank kommt.
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


# Teil 6 — Den Arbeitsablauf verbessern

Die Teile 1 bis 5 machen die Abrechnung **richtig**. Dieser Teil macht sie **erträglich**. Er beschreibt den Weg von der Kursstunde bis zum Zahlungseingang und die Stellen, an denen heute abgetippt wird, was schon jemand aufgeschrieben hat.

## Wie der Ablauf heute aussieht

```
Kursstunde ──> Anwesenheitsliste auf Papier ──> abends abtippen
                                                      │
                                        „Termine für alle: 8"
                                                      │
                                          Abrechnung ──> Datei
                                                      │
                                       Zahlungseingang? irgendwann
```

Drei Bruchstellen: **die Liste wird zweimal geführt** (auf Papier und im Programm), **die Terminzahl ist eine getippte Zahl** statt einer gezählten, und **niemand merkt, wenn eine Zahlung ausbleibt.**

## 6.1 Der Kurs als eigenes Ding

| | |
|---|---|
| **Größe** | M |
| **Hängt an** | nichts — **6.2 bis 6.4 hängen daran** |

Heute kennt das Programm Blaupausen, Gruppen und eine getippte Terminzahl. Was fehlt, ist der **Kurs**: ein Anfang, eine Reihe von Terminen, eine Teilnehmerliste.

Damit wird die Terminzahl vom **Eingabefeld zum Ergebnis**. Sie ist dann keine Behauptung mehr, sondern die Zahl der Termine, an denen jemand da war — und sie ist belegbar, wenn die Kasse fragt.

**Fertig, wenn:** ein Kurs mit Terminen angelegt werden kann, Teilnehmerinnen ihm zugeordnet sind und der Abrechnungslauf die Terminzahl von dort nimmt statt aus einem Feld.

## 6.2 Anwesenheit unterschreiben — auf dem Telefon oder Tablet

| | |
|---|---|
| **Größe** | L |
| **Hängt an** | 6.1 |

Simons Vorschlag vom 07.09.2026. **Er ist besser begründet, als er zunächst aussieht** — das Verfahren sieht so etwas ausdrücklich vor.

#### Wofür es eine Fundstelle gibt

Anhang 3, Abschnitt 8.2 kennt bei „Art der Datenlieferung" den Schlüssel **30 = Vollelektronische Abrechnung einschließlich elektronischem Leistungsnachweis**. Der elektronische Leistungsnachweis ist also kein Behelf, sondern eine vorgesehene Stufe — die höchste.

Anhang 4c regelt, was dafür gilt, und enthält den entscheidenden Satz: *„Sofern Dokumente bereits in digitaler Form vorliegen, gilt diese Verfahrensdokumentation ebenfalls, es erübrigen sich die Verfahrensschritte und Maßnahmen die Papierbelege betreffen."*

**Im Klartext: eine von Anfang an digitale Unterschrift muss nicht erst gedruckt und wieder eingescannt werden.** Sie ist unmittelbar zulässig — wenn die Bedingungen erfüllt sind.

#### Was das kostet, außer Programmierung

| Bedingung | Woher |
|---|---|
| **Verfahrensdokumentation** samt ausgefülltem Formular „Verfahrensbeschreibung Imageverfahren" | Anhang 4c, liegt als Formular vor |
| **Integritätssicherung** — ab der Unterschrift unveränderbar | Anhang 4c, Abschnitt 4.4; angelehnt an BSI TR-03138 (Ersetzendes Scannen) |
| **Aufbewahrung und geregelte Löschung** | Anhang 4c, Abschnitte 5 und 6 |
| **Kontrollrecht der Kassen** — sie dürfen einsehen | Anhang 4c, Abschnitt 7 |

Das ist der eigentliche Aufwand. **Die Unterschrift einzusammeln ist der einfache Teil; sie beweiskräftig zu halten, ist der schwierige.** Wer das Formular nicht ausfüllt, hat eine hübsche App und keinen gültigen Leistungsnachweis.

#### Wie es gebaut gehört, ohne alles zu zerstören

Ein Telefon darf **nicht** in die Datenbank schreiben. SQLite lässt einen Schreiber zu, die Datenbank liegt auf dem Rechner der Praxis, und eine zweite Datenhaltung auf dem Telefon wäre genau das, was am 07.09.2026 schon einmal verworfen wurde: eine zweite Ablage mit eigenen Fehlerfällen.

**Der Weg, der ohne all das auskommt:** die Anwendung öffnet für die Dauer des Kurses eine Seite im eigenen Netz. Das Telefon ruft sie im Browser auf, die Unterschrift entsteht dort und geht direkt in dieselbe Datenbank.

```
Rechner der Praxis (die Anwendung)
   │  öffnet für die Kursstunde eine Seite im WLAN
   ▼
Telefon/Tablet im Browser ── Unterschrift ──> dieselbe Datenbank
```

**Keine App im Store, keine Cloud, keine zweite Datenhaltung, keine Synchronisierung.** Die Daten verlassen die Praxis nicht. Und es funktioniert ohne Internet — im Kursraum ein nicht zu unterschätzender Vorzug.

Der Preis: der Rechner muss laufen und im selben Netz sein. Für einen Kurs im eigenen Raum ist das gegeben; für einen Hausbesuch nicht — dafür bliebe die Erfassung im Nachhinein.

#### Datenschutz

Eine Unterschrift ist ein personenbezogenes Datum. Sie gehört nicht in eine Cloud und nicht auf ein Telefon, das der Kurs nur ausleiht. Beim hier vorgeschlagenen Weg liegt sie nie irgendwo anders als in der Datenbank der Praxis — das ist der zweite Grund für diesen Zuschnitt. Auch hier gilt: **die Festplattenverschlüsselung sollte vorher stehen.**

**Fertig, wenn:** eine Teilnehmerin auf dem Telefon unterschreibt, die Unterschrift dem Termin und der Person zugeordnet in der Datenbank liegt, danach nicht mehr änderbar ist, und ein Kursnachweis daraus erzeugt werden kann.

## 6.3 Die Teilnehmerin trägt sich selbst ein

| | |
|---|---|
| **Größe** | M |
| **Hängt an** | 6.2 (dieselbe Seite im eigenen Netz) |

Zum Kursbeginn ruft jede Teilnehmerin dieselbe Seite auf und trägt Name, Anschrift, Krankenkasse und **Versichertennummer** selbst ein — von ihrer Karte abgelesen.

**Das ist die fehleranfälligste Stelle des ganzen Verfahrens.** Eine falsch abgetippte Versichertennummer führt zur Zurückweisung, und sie fällt bei keiner Formprüfung auf, weil sie formal richtig aussieht. Wer sie selbst einträgt, hat die Karte in der Hand.

**Fertig, wenn:** eine Teilnehmerin sich ohne Zutun der Hebamme vollständig erfassen kann und die Angaben denselben Prüfregeln unterliegen wie bei der Eingabe am Rechner.

## 6.4 Zahlungseingang abgleichen

| | |
|---|---|
| **Größe** | M |
| **Hängt an** | 1.5 |

Das Übermittlungsprotokoll aus 1.5 kennt den Zustand „bezahlt" — heute müsste ihn jemand von Hand setzen, und deshalb wird es niemand tun.

**Zu bauen:** Einlesen eines Kontoauszugs (CAMT.053 oder MT940, beides bekommt man bei jeder Bank) und Zuordnung über Betrag und Verwendungszweck. Was sich nicht sicher zuordnen lässt, bleibt offen und wird gezeigt — **nie automatisch als bezahlt gelten lassen.**

Der Nutzen ist doppelt: die Sicherungskopie darf weg (Pflicht aus Anlage 1, Abschnitt 3 Absatz 4 erfüllt), und **eine ausbleibende Zahlung fällt auf.** Heute fällt sie nicht auf.

**Fertig, wenn:** nach dem Einlesen eines Auszugs erkennbar ist, welche Forderung bezahlt ist, welche offen und welche überfällig.

## 6.5 „Was ansteht" — eine Übersicht

| | |
|---|---|
| **Größe** | S bis M |
| **Hängt an** | 1.5, 6.4 |

Die günstigste Verbesserung im ganzen Plan. Eine Seite, die zeigt:

- Kurse, die zu Ende sind und noch nicht abgerechnet wurden
- Forderungen ohne Zahlungseingang, älter als acht Wochen
- Zurückweisungen, auf die noch niemand geantwortet hat
- **Zertifikat läuft in weniger als einem Monat ab**
- neues Quartal — Kostenträgerdatei prüfen

**Damit wird aus dem Wartungsplan in Teil 2 etwas, das von selbst erinnert**, statt ein Dokument zu sein, in das jemand hineinsehen müsste. Ein Wartungsplan, an den niemand denkt, ist kein Wartungsplan.

## 6.6 Kursvorlagen und Serientermine

| | |
|---|---|
| **Größe** | S |
| **Hängt an** | 6.1 |

Ein Kurs entsteht aus Blaupause, Anfangsdatum und Rhythmus („zehn Termine, dienstags, 19 Uhr"); die Termine werden erzeugt. Ausfälle und Nachholtermine lassen sich einzeln ändern.

Kleine Arbeit, spürbarer Unterschied: das ist die Tätigkeit, die sonst mehrmals im Jahr von Hand anfällt.

## 6.7 Belege ablegen

| | |
|---|---|
| **Größe** | M |
| **Hängt an** | 6.1 |

Solange Papier im Spiel ist — Verordnungen, Bescheinigungen, unterschriebene Listen aus der Zeit vor 6.2 — brauchen sie einen Platz beim Kurs oder bei der Teilnehmerin.

**Nur sinnvoll zusammen mit der Verfahrensdokumentation aus Anhang 4c.** Ein Scan ohne dieses Verfahren ersetzt das Original nicht, und das Papier muss trotzdem aufgehoben werden — dann hat man beides statt keines.

## 6.8 Verlauf je Teilnehmerin

| | |
|---|---|
| **Größe** | S |
| **Hängt an** | 6.1 |

Eine Seite je Person: welche Kurse, welche Termine, welche Abrechnungen, was zurückkam, was bezahlt ist. Heute liegen diese Angaben in vier verschiedenen Masken.

Das ist die Seite, die man aufschlägt, wenn jemand anruft und fragt.

## 6.9 Alles wieder herausbekommen

| | |
|---|---|
| **Größe** | S |
| **Hängt an** | nichts |

Ein vollständiger Export aller Daten in ein offenes Format. Zwei Gründe, und der zweite ist der wichtigere: der Steuerberater fragt danach — und **niemand sollte in einem Programm festsitzen**, auch nicht in diesem.

## Reihenfolge

```
6.1 Kurs ──┬── 6.2 Unterschrift ── 6.3 Selbsteintrag
           ├── 6.6 Serientermine
           ├── 6.7 Belege
           └── 6.8 Verlauf
1.5 Protokoll ── 6.4 Zahlungsabgleich ── 6.5 „Was ansteht"
6.9 Export (unabhängig)
```

**Zuerst 6.1**, weil ohne den Kurs als eigenes Ding nichts davon trägt. **Dann 6.5**, weil es das billigste Stück mit der größten Wirkung auf den Alltag ist. **Dann 6.2 und 6.3** — dort liegt die eigentliche Arbeitserleichterung, und dort liegt auch der größte Aufwand.

## Was bewusst nicht dazugehört

- **Keine Cloud und kein Konto bei uns.** Die Daten liegen bei der Praxis. Alles, was das Telefon tut, tut es im eigenen Netz.
- **Keine dauerhaften Daten auf dem Telefon.** Es zeigt eine Seite, es speichert nichts.
- **Keine Terminerinnerung an Teilnehmerinnen.** Das ist Kursverwaltung, nicht Abrechnung — und es gibt gute Programme dafür.
- **Keine App in einem Store.** Sie brächte Veröffentlichung, Pflege und zwei Betriebssysteme mit sich, für einen Vorteil, den eine Seite im Browser auch liefert.

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
| Keine Bankverbindung in der Nutzdatendatei | Anlage 1, Abschnitt 5.5 — kein Segment und kein Feld dafür |
| Vollelektronische Abrechnung mit elektronischem Leistungsnachweis (Schlüssel 30) | Anhang 3, Abschnitt 8.2 |
| Digitale Belege, Integritätssicherung, Aufbewahrung, Kontrollrecht | Anhang 4c; angelehnt an BSI TR-03138 |
| Auftragsdatei | GGT Anlage 2 |
| KIM ersetzt KKS, Auftragsdatei, SECON | GGT Anlage 20, Abschnitt 1 |
| Zertifikat: Preis, Gültigkeit, je IK | ITSG Trust Center |
| IK gebührenfrei | ARGE·IK |

Alle Dokumente liegen unter `Information/` oder sind dort verlinkt. **Wer eine Angabe ändert, ändert auch die Fundstelle** — oder stellt fest, dass er keine hat.
