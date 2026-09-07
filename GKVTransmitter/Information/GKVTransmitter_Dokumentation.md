---
title: "GKV-Abrechnung"
subtitle: "Abrechnung mit gesetzlichen Krankenkassen — Dokumentation"
lang: de-DE
toc-title: "Inhaltsverzeichnis"
toc: true
toc-depth: 2
---

# Worum es geht

Die GKV-Abrechnung unterstützt die Abrechnung erbrachter Leistungen mit gesetzlichen Krankenkassen. Leistungen für gesetzlich Versicherte werden nicht mit den Versicherten abgerechnet, sondern mit deren Krankenkasse — und zwar als strukturierte Datenlieferung im DTA-Format, nicht als Rechnung auf Papier.

Das Programm erfasst die dafür nötigen Angaben, erzeugt daraus die Lieferung, prüft sie gegen ein Regelwerk und übergibt sie an die zuständige Kasse.

Der fachliche Zuschnitt entspricht der Tätigkeit einer Hebamme: die Belegnummern tragen das Präfix `HEB`, abgedeckt ist der Leistungsbereich SGS H mit dem Abrechnungscode `61`.

# Betrieb

## Systemvoraussetzungen

Ein Windows-Rechner. **Mehr nicht** — insbesondere kein installiertes Java, kein Datenbankdienst, kein Server und keine Administratorrechte.

Die ausgelieferte Fassung bringt ihre eigene Java-Laufzeit mit. Der Ordner wird kopiert, `GKV-Abrechnung.exe` per Doppelklick gestartet.

Eine Internetverbindung wird ausschließlich für den Versand an die Krankenkassen benötigt. Stammdatenpflege, Abrechnungserstellung, DTA-Erzeugung und Prüfung funktionieren ohne Netz.

## Wo die Daten liegen

Alle Daten liegen im Profil des angemeldeten Benutzers:

| Betriebssystem | Ort |
|---|---|
| Windows | `%LOCALAPPDATA%GKV-Abrechnung` |
| macOS | `~/Library/Application Support/GKV-Abrechnung` |
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
GKV-Abrechnung.exe
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
| Leistungsposition | Einzelbetrag und Menge einer Leistungszeile stehen nicht auf null |
| Positionsnummer | Länge der Abrechnungspositionsnummer passt zum Abrechnungscode |
| Betragskonsistenz | Leistungspositionen gegen Fallsumme, Fallsumme gegen Gesamtsumme |

## Fehler und Hinweise

Nur **Fehler** halten den Versand auf. **Hinweise und Warnungen** verhindern nichts.

> **Sie werden derzeit auch nicht angezeigt.** Der Prüfbericht erreicht die Oberfläche nur über die Ausnahme, die bei einem Fehler geworfen wird; ein Bericht ohne Fehler wird verworfen. Eine Warnung, die niemand sieht, ist keine Warnung — das ist die dringendste offene Lücke, siehe `Naechste_Schritte.md`, Abschnitt D.

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

> **Für den echten Betrieb ist diese Zuordnung zu eng.** Empfänger einer Lieferung ist nicht die einzelne Krankenkasse, sondern die **Datenannahmestelle mit Entschlüsselungsbefugnis** ihrer Kassenart. Siehe „Der Weg zur Kasse".

## Art der erzeugten Dateien

Die letzte Stelle des `UNB`-Segments sagt, wofür sich eine Datei ausgibt:

| Wert | Bedeutung | Folge |
|---|---|---|
| `0` | Testdatei | wird angenommen, aber nicht verarbeitet |
| `1` | Erprobungsdatei | wird verarbeitet und geprüft, aber **nicht bezahlt** |
| `2` | Echtdatei | gilt als Forderung |

Einstellbar unter „Einstellungen"; die Vorgabe ist **Erprobung**. Das Programm wechselt nicht von selbst in den Echtbetrieb — auch dann nicht, wenn die Einstellungsdatei fehlt oder unlesbar ist.

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

# Der Weg zur Kasse

Dieses Kapitel beschreibt, wie eine Lieferung tatsächlich zur Krankenkasse gelangt, was davon das Programm leistet und was dafür noch fehlt.

Grundlage sind die Unterlagen unter `Information/`. Sie liegen **vollständig** vor; bis zum 07.09.2026 fehlten zwei Anhänge, und ihr Fehlen war der Grund, warum drei Angaben im Programm frei erfunden waren. Sie waren nie schwer zu bekommen — sie stehen frei zum Herunterladen auf [gkv-datenaustausch.de](https://www.gkv-datenaustausch.de/leistungserbringer/sonstige_leistungserbringer/sonstige_leistungserbringer.jsp), dem Portal des GKV-Spitzenverbandes.

| Unterlage | Datei | Regelt |
|---|---|---|
| Anlage 1, Version 21 | `Anlage_1_TP5_V21_20260115.pdf` | Aufbau der Nutzdaten — **heute gültig** |
| Anlage 1, Version 22 | `Anlage_1_TP5_V22_20260521.pdf` | dasselbe, anzuwenden ab 01.02.2027 |
| Anlage 3, Version 22 | `Anlage_3_TP5_V22_20260521.pdf` | Schlüsselverzeichnisse |
| Anhang 1 zur Anlage 1 | `Anhang_1_Datenuebermittlung_20170831.pdf` | Übermittlung, Dateinamen, Dokumentation |
| Anhang 2 zur Anlage 1 | `Anhang_2_Pruefverfahren.pdf` | Prüf- und Erprobungsverfahren |
| Anhang 3 zur Anlage 1 | `Anhang_3_Kostentraegerdatei_V10_20260414.pdf` | Aufbau der Kostenträgerdatei |
| GGT Anlage 2 | `GGT_Anlage_2_Auftragsdatei.pdf` | Auftragsdatei |
| GGT Anlage 7 | `GGT_Anlage_7_EMail.pdf` | Übertragung per E-Mail |
| GGT Anlage 20 | `GGT_Anlage_20_KIM.pdf` | Übertragung über KIM in der Telematikinfrastruktur |

> Die Security-Schnittstelle (SECON, GGT Anlage 16) ist mit 5 MB nicht mitgelieferter Bestandteil des Projekts. Sie steht unter [Technische Standards](https://www.gkv-datenaustausch.de/technische_standards_1/technische_standards.jsp) und wird erst gebraucht, wenn tatsächlich selbst verschlüsselt wird.

## Der vorgesehene Weg

```
Leistungserbringerin (eigenes IK)
      │  Nutzdatendatei (UNB … UNZ) + Auftragsdatei
      │  verschlüsselt mit dem öffentlichen Schlüssel der Annahmestelle
      ▼
Datenannahmestelle mit Entschlüsselungsbefugnis   ← je Kassenart eine
      │  Prüfstufe 1  Datei, Dateipaar, Anmeldung des Absenders
      │  Prüfstufe 2  Segmentfolge, Feldart, Feldtyp, Feldlänge
      │  Prüfstufe 3  Inhalte gegen die Schlüsselverzeichnisse
      ▼
Krankenkasse
      │  Prüfstufe 4  vertrags-, versicherungs- und leistungsrechtlich
      ▼
Bezahlung  oder  Zurückweisung
```

Drei Punkte daran werden regelmäßig unterschätzt:

**Empfänger ist die Datenannahmestelle, nicht die Kasse.** Für jede Datenannahmestelle mit Entschlüsselungsbefugnis ist je Kassenart **eine** Nutzdatendatei zu erstellen (Anlage 1, Abschnitt 3 und 5.3.1). Ihr IK steht im `UNB` als Empfänger.

**Zu jeder Nutzdatendatei gehört eine Auftragsdatei.** Sie geht unverschlüsselt voraus und trägt die Transportangaben; ohne sie scheitert die Lieferung bereits in Prüfstufe 1, die ausdrücklich prüft, „ob die Dateien paarweise übermittelt" wurden.

**Die Erprobung ist vorgeschrieben, nicht optional.** Vor der erstmaligen Durchführung und vor jeder Änderung des Verfahrens sind die Einzelheiten mit dem Empfänger abzustimmen und die ordnungsgemäße Verarbeitung zu erproben (Anlage 1, Abschnitt 2).

## Wer der Empfänger ist — und wie man ihn findet

Die Zuordnung steht in der **Kostenträgerdatei**, die jeder Kassenartenverband vierteljährlich veröffentlicht ([Kostenträgerdateien SLE](https://www.gkv-datenaustausch.de/leistungserbringer/sonstige_leistungserbringer/kostentraegerdateien_sle/kostentraegerdateien.jsp)). Sie ist selbst eine EDIFACT-Datei mit der Endung `.KE0`, `.KE1` und so fort — im selben Segmentstil wie unsere Nutzdaten, das Programm könnte sie mit dem vorhandenen Leser verarbeiten.

Der Weg von der Kasse auf der Versichertenkarte zur Annahmestelle geht über drei Segmente:

```
IDK   IK der Versichertenkarte
 └─ VKG+01+<IK>            → Verweis auf den Kostenträger
      └─ VKG+03+<IK>+…+50  → Datenannahmestelle MIT Entschlüsselungsbefugnis
           │                 (VKG+02 wäre ein Netzbetreiber OHNE)
           └─ DFU           → die Adresse: 070 E-Mail, 016 FTAM, 080 KIM
```

Das `VKG`-Segment trägt an vorletzter Stelle den **Abrechnungscode**. Für Hebammenhilfe ist das die **50**; nur die Verweise mit dieser Ausprägung gelten für uns.

Ein Beispiel aus den Dateien vom Sommer 2026, nachgeschlagen und nicht angenommen: sowohl der AOK-Bundesverband als auch der Verband der Ersatzkassen führen für den Abrechnungscode 50 dieselbe Stelle — die **IQVIA Health System Services GmbH, IK 661430035**, erreichbar über `edi302@iqvia-hss.de` und über FTAM. Aus 23 Kassen im heutigen `billing-office-endpoints.json` wird damit eine sehr viel kürzere Liste.

> Der Befund gilt für die Dateien vom Juli und Oktober 2026 und für die dort geführten Kassenarten. Er ersetzt keine eigene Abfrage: die Dateien ändern sich vierteljährlich, und für BKK, IKK, Knappschaft und die landwirtschaftlichen Kassen ist er nicht geprüft.

## Wie die Datei übertragen wird

Nach Anhang 1, Abschnitt 4.1 ist **die Datenfernübertragung der Regelfall**; Datenträger sind nur zulässig, wenn eine Übertragung technisch oder wirtschaftlich nicht möglich ist. Die Kosten trägt der Absender.

Zugelassen sind:

| Schlüssel | Weg | Bemerkung |
|---|---|---|
| `070` | E-Mail über das Internet | heute der verbreitete Weg |
| `016` | FTAM über TCP/IP | für größere Mengen |
| `080` | KIM-Mail in der Telematikinfrastruktur | vorgesehen, aber noch von keiner Annahmestelle für Sonstige Leistungserbringer veröffentlicht |

**KIM ist der Weg, der die meiste Arbeit erspart** — und deshalb der, den man im Blick behalten sollte. Wird KIM verwendet, gelten nach GGT Anlage 20 die Vorgaben zu **Krankenkassenkommunikationssystem, Auftragsdatei und SECON ausdrücklich nicht**; sie werden durch Komponenten der Telematikinfrastruktur ersetzt. Damit entfielen auf einen Schlag drei der aufwendigsten Bausteine. Voraussetzung sind ein TI-Anschluss und eine SMC-B; die Adresse der Kasse wird über das Verzeichnisdienst-Attribut `domainID` = Haupt-IK gesucht.

Solange keine Annahmestelle eine KIM-Adresse führt, bleibt es bei E-Mail mit verschlüsseltem Anhang.

## Die Dateinamen sind vorgeschrieben

Beides stand bis zum 07.09.2026 falsch im Programm, und beides fällt in Prüfstufe 1 oder 2 auf.

**Logischer Dateiname** — elf Stellen, im `UNB` als Anwendungsreferenz und im Feld „Dateiname" der Auftragsdatei, dort identisch (Anhang 1, Abschnitt 4.2):

```
SL  191400  S  09
│   │       │  └─ Nummer des Abrechnungsmonats
│   │       └──── S = Selbstabrechner, A = Abrechnungsstelle
│   └──────────── Stellen 3 bis 8 des Absender-IK
└──────────────── „SL" für Sonstige Leistungserbringer
```

**Physikalischer Dateiname** — acht Stellen, in der Auftragsdatei (Abschnitt 4.3): `E` für Echtdaten oder `T` für Testdaten, dann `SOL`, dann `0`, dann eine dreistellige Transfernummer. Also `ESOL0001`, `TSOL0001`.

## Test und Erprobung

Anhang 2 unterscheidet zwei Stufen, und der Unterschied ist praktisch bedeutsam:

| | Testverfahren | Erprobungsverfahren |
|---|---|---|
| Wann | vor der Erprobung, bei Versionswechseln | vor dem Echtbetrieb |
| Testindikator im `UNB` | `0` | `1` |
| Physikalischer Name | `TSOL…` | `TSOL…` |
| Geprüft wird | Prüfstufen 1 bis 3 | vollständige Verarbeitung |
| Zahlung | nein | nein |
| Endet | mit der Rückmeldung | mit der **Zulassung zum Echtverfahren durch die Kasse** |

Zweierlei ist daran wichtig. Erstens: **man fragt nicht um einen Gefallen.** Die Spitzenverbände bieten das Prüfverfahren ausdrücklich auch Leistungserbringern an, „die ihre Abrechnungssoftware selbst entwickelt haben". Es ist ein vorgesehener Weg, kein Entgegenkommen.

Zweitens: **auch die Erprobungsdatei trägt den physikalischen Namen einer Testdatei.** Testindikator `1` und `TSOL` gehören zusammen — eine Erprobungsdatei mit `ESOL` wäre in sich widersprüchlich.

## Was das Programm davon leistet

| Schritt | Stand |
|---|---|
| Nutzdaten erzeugen (SLGA + SLLA) | vollständig |
| Prüfstufen 1 bis 3 vorab selbst durchlaufen | weitgehend, acht Regeln |
| Datenaustauschreferenz fortlaufend und dauerhaft vergeben | vollständig |
| Art der Datei (Test / Erprobung / Echt) | einstellbar |
| Leistungsbereich im `UNB` aus dem Abrechnungscode | **seit 07.09.2026** |
| Logischer Dateiname nach Anhang 1 | **seit 07.09.2026** |
| Physikalischer Dateiname | fehlt — gehört zur Auftragsdatei |
| Auftragsdatei | **fehlt** |
| Verschlüsselung | **fehlt** |
| Zuordnung zur Datenannahmestelle statt zur Kasse | **fehlt** |
| Übertragung selbst | dateibasiert, kein realer Weg |

Die eigene Prüfung vor dem Versand ist kein Beiwerk: der Absender hat sicherzustellen, dass **nur geprüfte Datensätze übermittelt werden** (Anlage 1, Abschnitt 3 Absatz 3). Genau dafür ist die Prüfstufe im Programm das Tor vor dem Versand.

## Drei mögliche Wege

**A — Selbst übermitteln.** Eigenes IK, eigenes Zertifikat, eigener Transportweg. Volle Kontrolle, keine laufenden Kosten je Rechnung. Zu bauen sind Auftragsdatei, Verschlüsselung, Empfängerermittlung aus der Kostenträgerdatei und der Versandweg; dazu kommt eine Erprobung je Annahmestelle.

**B — Über eine Abrechnungsstelle.** Ein Dienstleister übernimmt Übermittlung und häufig auch das Inkasso. Die Technische Anlage sieht dafür die **Rechnungsart 2** vor: der Dienstleister sendet, Zahlung geht an das IK der Leistungserbringerin. Der Einstieg ist deutlich einfacher, es entstehen Kosten je Rechnung, und eine Abhängigkeit.

**C — Erzeugen und prüfen, Übergabe als Datei.** Der heutige Stand: das Programm erstellt die geprüfte Datei, die Übermittlung geschieht auf anderem Weg.

**Empfehlung: C, dann B, dann A — mit einer Einschränkung gegenüber der Einschätzung vom Vortag.** Weg A ist näher, als er aussah. Was ihn versperrt hat, war nicht die Schwierigkeit, sondern die Unkenntnis: die fehlenden Anhänge waren frei verfügbar, der Empfänger steht in einer öffentlichen Datei, und das Zertifikat kostet **79 € zuzüglich Umsatzsteuer** für den Erstantrag, danach **49 €** je Folgeantrag über die Online-Schnittstelle. Es gilt **ein Jahr** und wird in drei bis vier Arbeitstagen ausgestellt; beantragt wird es beim [ITSG Trust Center für sonstige Leistungserbringer](https://www.itsg.de/produkte/trust-center/zertifikat-beantragen/).

> **Der Betrag fällt einmal an, nicht je Kasse.** Das Zertifikat wird auf das **eigene IK** ausgestellt und gilt für den Datenaustausch mit **allen** Datenannahmestellen. Zum Verschlüsseln wird der öffentliche Schlüssel des jeweiligen Empfängers gebraucht — den liefert das Trust Center als Schlüsselliste (`annahme-sha256.key`) zusammen mit dem Zertifikat mit, ohne weitere Kosten. Es gibt also weder eine Gebühr je Kasse noch eine je Lieferung.

Trotzdem bleibt die Reihenfolge. Nicht wegen der Kosten, sondern wegen der Reihenfolge des Lernens: Weg B liefert echte Beanstandungen echter Kassen, und genau die sind die Vorbereitung auf die Erprobung, die Weg A verlangt. Und wegen der laufenden Pflege — siehe den nächsten Abschnitt, der die Empfehlung erst vollständig macht.

**Der Umbau bleibt in jedem Fall überschaubar**, weil der Transport hinter einer einzigen Schnittstelle liegt: `BillingOfficeTransport`. Erzeugung, Prüfung, Zuordnung und Auswertung der Rückmeldungen bleiben unverändert.

## Was der eigene Weg wirklich kostet

Die Gebühr ist der kleinere Teil. **Wer selbst überträgt, pflegt auch selbst** — und das Verfahren steht nicht still. Simons Einwand am 07.09.2026: *„jedoch auch Wartung und Co, das fließt auch mit ein, da jedes Jahr sich was ändern könnte."*

Was sich tatsächlich bewegt, und wie oft:

| Was | Rhythmus | Passiert was, wenn man es verpasst |
|---|---|---|
| **Kostenträgerdatei** | **vierteljährlich** | Lieferung geht an eine Stelle, die nicht mehr zuständig ist |
| **Zertifikat** | **jährlich**, Antrag 1–2 Wochen vorher | Datenaustausch steht still, bis das neue da ist |
| **Technische Anlage 1** | neue Version etwa jährlich, 3 Monate Übergangsfrist | Datei wird nach Ablauf abgewiesen |
| **Anlage 3 (Schlüssel)** | mehrmals jährlich | ungültige Schlüsselausprägung, Prüfstufe 3 |
| **Positionsnummernverzeichnis** | bei Vertragsänderung | zuletzt zum 01.11.2025: vier Stellen wurden fünf |
| **Vergütungsvereinbarung** | bei Anpassung | falsche Beträge, Rückforderung |

**Der Beleg liegt im Projekt selbst.** Die verbindlichen Anlagen unter `Information/` waren am 07.09.2026 vom Juli — nach zwei Monaten überholt, und es war niemandem aufgefallen. Genau so sieht die Wartungslast in der Praxis aus: nicht als Aufwand, den man einplant, sondern als Veralten, das niemand bemerkt.

Bei einer Abrechnungsstelle ist das **im Preis enthalten** — sie muss dem Verfahren folgen, nicht die Leistungserbringerin. Das ist der eigentliche Gegenwert der drei Prozent, nicht der Briefversand.

**Was die Wartung klein hält**, und deshalb vor dem Echtbetrieb gebaut gehört:

1. **Kostenträgerdatei einlesen statt Endpunkte pflegen** — aus vier Pflegeterminen im Jahr wird ein Dateiaustausch.
2. **Ablaufwarnung für das Zertifikat** — die Anwendung kennt das Datum, sie kann rechtzeitig daran erinnern.
3. **Versionsangabe in der Nachricht sichtbar machen** — der Nachrichtentyp trägt sie bereits (`SLGA:21:0:0`); wer sieht, mit welcher Version er sendet, merkt einen Wechsel.

Ohne diese drei ist der eigene Weg jedes Jahr Handarbeit. Mit ihnen bleibt eine Aufgabe im Jahr: das Zertifikat erneuern.

## Was noch zu beschaffen ist

Die Liste ist kürzer geworden. Keiner dieser Punkte lässt sich programmieren:

- ein eigenes **Betriebsstätten-IK** (bei der ARGE·IK)
- ein **Zertifikat** des ITSG Trust Centers, siehe oben
- die **Anmeldung als Kommunikationspartner** bei der Datenannahmestelle — Prüfstufe 1 prüft sie ab
- die gültigen **Abrechnungspositionsnummern** aus dem bundeseinheitlichen Positionsnummernverzeichnis der Hebammenhilfe-Vergütungsvereinbarung und das **Tarifkennzeichen** aus dem Vertrag

Der letzte Punkt ist der einzige, der heute schon stört: die Vorbelegung führt eine neunstellige Positionsnummer, für Hebammenhilfe sind nach Anlage 3, Abschnitt 8.2.6 aber vier oder fünf Stellen vorgesehen. `PositionsnummerRegel` warnt bei jedem Lauf, hält ihn aber nicht auf — eine Sperre ohne Ausgang wäre schlimmer, solange das Verzeichnis fehlt.

## Pflichten neben der Übermittlung

Die Technische Anlage verlangt zweierlei, das nicht in der Datei steht:

- Über den Datenaustausch ist eine **Dokumentation** zu führen und **mindestens zwei Jahre** aufzubewahren. Anhang 1, Abschnitt 4.5 nennt die Mindestinhalte: physikalischer Dateiname, Erstellungsdatum, laufende Nummer, Kommunikationspartner, Beginn und Ende der Übermittlung, Dateigröße, Verarbeitungshinweise, Senden oder Empfangen, fehlerfrei oder fehlerhaft und im Fehlerfall der Fehlerstatus.
- Eine **Sicherungskopie** der Daten ist bis zur Bezahlung vorzuhalten, damit sich eine verlorene oder zurückgewiesene Lieferung rekonstruieren lässt.

Beides ist im Programm bisher nicht abgebildet und gehört zum Weg in den Echtbetrieb. Die geforderte Dokumentation ist dabei fast ein Nebenprodukt: die Angaben entstehen ohnehin beim Versand, sie werden nur nirgends festgehalten.
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

Die Oberfläche ist in Masken geschnitten: eine je Aufgabe. `AbrechnungsMaske` stellt eine Abrechnung zusammen, `GruppenMaske` pflegt die Gruppen, `PersonenMaske` die Teilnehmer und Dienstleister, `Feldbau` erzeugt und prüft die Eingabefelder. `Hauptfenster` trägt Seitenleiste, Kopfzeile und Statuszeile und weist jeder Maske ihren Platz zu.

Jede Maske bekommt von außen, was sie braucht — Daten, Meldewege, den Platz, an dem sie erscheint. Deshalb lässt sich jede einzeln prüfen, ohne Datenbank und ohne dass etwas auf dem Bildschirm erscheinen müsste.

**Im laufenden Betrieb öffnet die Anwendung kein Fenster.** Meldungen erscheinen in der Ecke oben rechts und gehen nach wenigen Sekunden von selbst; was schiefgegangen ist und was eine Antwort verlangt, bleibt stehen, bis es zur Kenntnis genommen wurde. Auch die Rückfrage vor dem Löschen und der Prüfbericht einer abgewiesenen Abrechnung erscheinen dort — Letzterer als Liste, die sich Punkt für Punkt abarbeiten lässt.

Unter jedem Eingabefeld steht, was hineingehört, und darunter Platz für eine Beanstandung. Das **Institutionskennzeichen wird schon hier gegen seine Prüfziffer geprüft**: ein falsches IK lässt die Kasse sonst die gesamte Lieferung abweisen, und das fiele erst Tage später auf.

`DataRepository` ist der Vertrag, `HibernateSqllite` die Umsetzung. Die Oberfläche kennt nur den Vertrag — die Datenhaltung wäre austauschbar, ohne die Oberfläche zu berühren.

## Erweiterungspunkte

An fünf Stellen ist ein Vertrag bewusst von seiner Umsetzung getrennt, damit sich das Programm erweitern lässt, ohne Bestehendes anzufassen:

| Vertrag | Heutige Umsetzung | Erweiterbar um |
|---|---|---|
| `DataRepository` | `HibernateSqllite` | eine andere Datenhaltung |
| `BillingOfficeTransport` | Datei-Zustellung, simulierte Kasse | einen echten Übermittlungsweg |
| `ValidationRule` | sieben mitgelieferte Regeln | weitere fachliche Prüfungen |
| `Meldungen` | die Meldungsecke im Fenster | eine andere Art zu melden, etwa ein Protokoll |
| `Maskenrahmen` | die Mitte des Hauptfensters | eine andere Anordnung der Masken |

# Konfiguration

## Einstellungen in der Anwendung

Was sich im Bereich „Einstellungen" wählen lässt, steht in der Tabelle `einstellung` der Datenbank — also im Ablageort der Daten und **nicht im Programmordner**. Wer die Datenbank sichert oder auf einen anderen Rechner mitnimmt, nimmt die Einstellungen mit.

Einen Tag lang war es eine eigene Datei `einstellungen.json` neben der Datenbank. Sie hat sich nicht gehalten: sie lag im selben Ordner, wurde von derselben Sicherung erfasst und folgte demselben `gkv.home` — sie konnte nichts, was die Datenbank nicht auch kann, brachte aber einen zweiten Ablageweg mit eigenem Lese-, Schreib- und Nebendateifehler mit.

| Schlüssel | Werte | Vorgabe | Bedeutung |
|---|---|---|---|
| `darstellung` | `hell`, `dunkel` | `hell` | helle oder dunkle Oberfläche |
| `uebermittlungsart` | `test`, `erprobung`, `echt` | `erprobung` | wofür sich erzeugte Dateien ausgeben |

Jede Änderung wirkt sofort und wird sofort geschrieben. Ist die Datei beschädigt oder nicht lesbar, gelten die Vorgaben und die Anwendung startet trotzdem — eine Farbwahl ist nichts, wofür ein Programm nicht aufgehen darf.

**Zugangsdaten und Schlüssel gehören nicht in diese Datei.** Sie liegt im Klartext im Benutzerprofil. Sobald ein echter Übermittlungsweg Zugangsdaten braucht, gehören diese in den Anmeldeinformationsspeicher des Betriebssystems.

## Beim Start

Alle folgenden Angaben sind optional; ohne Angabe gelten die Vorgabewerte.

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

Ergebnis ist `gkv-ui/target/paket/GKV-Abrechnung/` — ein Ordner mit Startprogramm und mitgelieferter Java-Laufzeit, rund 166 MB.

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
- `Abrechnung_Datenerklaerung.md` — Felderklärung anhand der Referenznachricht
- `codes/` — die Codelisten des Verfahrens
- `Valide.DTA` — eine gültige Referenznachricht, die im Projekt als Messlatte dient

# Grenzen

Was das Programm heute **nicht** leistet:

- Der Versand ist dateibasiert. Ein signierter und verschlüsselter Übermittlungsweg an reale Annahmestellen ist nicht umgesetzt. Dafür wären Zertifikate einer anerkannten Stelle, Zugangsdaten und ein eigenes Betriebsstätten-IK erforderlich. Siehe „Der Weg zur Kasse".
- Die **Auftragsdatei**, die zu jeder Nutzdatendatei gehört, wird nicht erzeugt.
- Die Zuordnung erfolgt zur einzelnen Krankenkasse; vorgesehen ist die Zuordnung zur **Datenannahmestelle je Kassenart**.
- Die vorgeschriebene **Dokumentation des Datenaustauschs** (zwei Jahre) und die **Sicherungskopie bis zur Bezahlung** sind nicht abgebildet.
- Die Daten liegen **unverschlüsselt** im Benutzerprofil. Es gibt keine Anmeldung und kein Protokoll darüber, wer was geändert hat.
- Positionsnummern, Tarifkennzeichen und Abrechnungscodes werden auf Form, nicht aber auf fachliche Zulässigkeit geprüft. Das setzt den jeweiligen Vertrag und Anlage 3 voraus.
- Es gibt keine Stornierung und keine Nachberechnung.
- Der Umsatzsteuersatz ist mit 19 fest hinterlegt.
- Abgedeckt ist der Leistungsbereich SGS H mit Abrechnungscode `61`.
- Die Anwendung ist auf einen Benutzer und einen Rechner ausgelegt. Ein gemeinsamer Zugriff mehrerer Arbeitsplätze auf dieselbe Datenbank ist nicht vorgesehen.
