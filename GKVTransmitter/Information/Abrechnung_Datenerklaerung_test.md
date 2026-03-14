# Datenbeschreibung fuer diese konkrete Abrechnung

Diese Erklaerung bezieht sich nur auf die Datei test.DTA in ihrem aktuellen Stand.

## 1) Kopf und Nachrichtenstruktur

- UNB+UNOC:3+123456780+987654324+20260308:1200+00001+H+HEB26030801+1'
  - Bedeutung: Interchange-Kopf (technische Huelle fuer die Uebertragung).
  - Woher kommen die Werte:
    - 123456780: Absender-IK (dein IK).
    - 987654324: Empfaenger-IK (Annahmestelle/Kasse).
    - 20260308:1200: Erstellzeitpunkt der Datei.
    - 00001: Austauschreferenz (pro Datei eindeutig).
    - H / HEB26030801 / 1: technische Kennzeichen gemaess Spezifikation/Verfahren.

- UNH+00001+SLGA:21:0:0'
  - Bedeutung: Beginn Nachricht 1 vom Typ SLGA (Summen-/Begleitdaten).
  - Woher: Nachrichtszaehler 00001 fortlaufend je UNB.

- UNH+00002+SLLA:21:0:0'
  - Bedeutung: Beginn Nachricht 2 vom Typ SLLA (Falldaten/Leistungsdaten).
  - Woher: Nachrichtszaehler 00002 fortlaufend je UNB.

- UNT+000008+00001' und UNT+000008+00002'
  - Bedeutung: Segmentanzahl je Nachricht + zugehoerige UNH-Referenz.
  - Woher: tatsaechlich gezaehlte Segmente zwischen UNH und UNT (inklusive UNH und UNT).

- UNZ+000002+00001'
  - Bedeutung: Abschluss Interchange mit Anzahl enthaltener Nachrichten.
  - Woher: Anzahl UNH in der Datei = 2; Referenz 00001 muss zu UNB passen.

## 2) Absender/Kassenbezug (in SLGA und SLLA gleich)

- FKT+01++123456780+987654324+987654324+123456780'
  - 01: Verarbeitungskennzeichen = normale Abrechnung.
  - IK Leistungserbringer: 123456780.
  - IK Kostentraeger: 987654324.
  - IK Kasse von Karte/VO: 987654324.
  - IK Rechnungssteller: 123456780.
  - Woher: IK-Stammdaten Vertrag/Abrechnungsstelle.

- REC+00000000:0+20260307+1'
  - Rechnungsnummer (Sammel:Einzel) = 00000000:0.
  - Rechnungsdatum = 20260307.
  - Rechnungsart = 1.
  - Woher: internes Rechnungswesen + Schluessel 8.1.4.

## 3) SLGA-Summen (Gesamtrechnung)

- UST+19'
  - Bedeutung: Summenstatus-Block vorhanden.
  - Woher: entsprechend TA-Aufbau fuer SLGA.

- GES+00+120000,00+120000,00'
  - 00 = Gesamtsumme aller Status.
  - Feld 3 = Rechnungsbetrag Einzelrechnung(en) gesamt.
  - Feld 4 = Bruttobetrag gesamt.
  - Woher: Summe aus den Fall-/Betragssummen der SLLA-Faelle.

- GES+99+120000,00+120000,00'
  - 99 = nicht zuzuordnende Status (von deinem Pruefer gefordert).
  - In dieser Datei identisch mit Gesamtbetrag.
  - Woher: Schluessel 8.1.6 + Pruefvorgaben Annahmestelle.

- NAM+MUSTERMANN+MARIA+DR'
  - Bedeutung: Name Ansprechpartner/Leistungserbringerbezug im Kopf.
  - Woher: Stammdaten.

## 4) SLLA-Falldaten (genau ein Fall in dieser Datei)

- INV+012345678901++1+HEB2403001'
  - Feld 2: Versichertennummer = 012345678901.
  - Feld 3 leer: Versichertenstatus nicht geliefert.
  - Feld 4: Beleginformation = 1.
  - Feld 5: Belegnummer = HEB2403001.
  - Woher: Verordnung/Urbeleg.

- NAD+MUSTERMANN+ANNA+19900101+MUSTERSTRASSE 1+12345+MUSTERSTADT'
  - Bedeutung: Name/Adresse Versicherte.
  - Warum vollstaendig noetig: Da INV-Status leer ist, muss Anschrift vollstaendig sein.
  - Woher: Verordnung bzw. Versichertenstammdaten.

## 5) Leistungszeile (ENF) fuer diesen Fall

- ENF+01+61:00000+306050601+08,00+15000,00+20240301+0,00'
  - Feld 2 (Identifikationsnummer): 01.
  - Feld 3 (Leistungserbringergruppe): 61:00000
    - 61 = Abrechnungscode fuer SGS H (Rehabilitationssport) laut Anlage 3, 8.1.5.1.
    - 00000 = Tarifkennzeichen (5-stellig).
  - Feld 4: Abrechnungspositionsnummer = 306050601.
  - Feld 5: Anzahl/Menge = 08,00 (mit 2 Nachkommastellen).
  - Feld 6: Einzelbetrag = 15000,00.
  - Feld 7: Datum Leistung = 20240301.
  - Feld 8: Zuzahlung je Position = 0,00.
  - Woher:
    - Abrechnungscode/Tarifkennzeichen: Vertrag + Anlage 3 Schluessel 8.1.5.1/8.1.5.2.
    - Positionsnummer: Anlage 3 Abschnitt 8.2.x passend zum Leistungsbereich.
    - Menge, Preis, Datum: aus Leistungserfassung.

## 6) Fallsumme

- BES+120000,00'
  - Bedeutung: Bruttosumme dieses einen Abrechnungsfalls.
  - Plausibilitaet in dieser Datei:
    - ENF Menge 08,00 x Einzelbetrag 15000,00 = 120000,00.
    - Zuzahlung je Position 0,00.
  - Woher: rechnerisch aus ENF (plus ggf. MWS wenn vorhanden).

## 7) Was du fuer genau diese Abrechnung brauchst (Checkliste)

- IK-Daten: Leistungserbringer, Kostentraeger, Kasse, ggf. Rechnungssteller.
- Rechnungsdaten: Rechnungsnummer, Rechnungsdatum, Rechnungsart.
- Fallbezug: Belegnummer und Versicherungsdaten.
- Wenn Versichertenstatus nicht geliefert wird: NAD mit kompletter Anschrift.
- Leistungsdaten: korrekter Abrechnungscode/Tarifkennzeichen, gueltige Positionsnummer, Menge (2 Nachkommastellen), Preis, Leistungsdatum.
- Summen: BES je Fall und GES in SLGA konsistent.

## 8) Kurzformel fuer diese Datei

- BES = ENF.Menge x ENF.Einzelbetrag = 08,00 x 15000,00 = 120000,00
- GES(00) = Summe aller BES = 120000,00
- GES(99) = in dieser Datei ebenfalls 120000,00
