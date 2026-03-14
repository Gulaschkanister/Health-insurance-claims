# 1-Seiten-Checkliste fuer diese Abrechnung (SGS H)

## A) Pflichtdaten vor dem Export

- IK Leistungserbringer vorhanden
- IK Kostentraeger vorhanden
- IK Krankenkasse vorhanden
- Rechnungsnummer vorhanden (Format Sammel:Einzel, z. B. 00000000:0)
- Rechnungsdatum gesetzt (JJJJMMTT)
- Rechnungsart gesetzt
- Belegnummer vorhanden (z. B. HEB2403001)

## B) Versicherte Person

- Entweder:
  - Versichertennummer und Versichertenstatus in INV gepflegt
- Oder (wenn Status fehlt):
  - NAD mit kompletter Anschrift:
  - Nachname, Vorname, Geburtsdatum
  - Strasse/Hausnummer
  - PLZ
  - Ort

## C) ENF (Leistungszeile) fuer SGS H

Muster:

ENF+01+61:00000+306050601+08,00+15000,00+20240301+0,00'

Pruefpunkte:

- Feld 2 Identifikationsnummer: numerisch (z. B. 01)
- Feld 3 Leistungserbringergruppe = Abrechnungscode:Tarifkennzeichen
  - Abrechnungscode fuer SGS H = 61
  - Tarifkennzeichen 5-stellig (vertraglich gueltig)
- Feld 4 Abrechnungspositionsnummer gueltig fuer den Bereich
- Feld 5 Anzahl/Menge mit 2 Nachkommastellen (z. B. 08,00)
- Feld 6 Einzelbetrag mit 2 Nachkommastellen
- Feld 7 Leistungsdatum im Format JJJJMMTT
- Feld 8 Zuzahlung mit 2 Nachkommastellen (z. B. 0,00)

## D) Fallsumme und Gesamtsumme

- BES je Fall korrekt berechnet
  - Formel: ENF.Anzahl/Menge x ENF.Einzelbetrag (+ ggf. MWS)
- GES in SLGA konsistent mit Fallsummen
  - GES 00 vorhanden
  - GES 99 vorhanden (wenn vom Pruefer gefordert)

## E) Segmentzaehler

- UNT Segmentanzahl ist 6-stellig und korrekt gezaehlt
- UNZ Nachrichtenanzahl ist 6-stellig und entspricht Anzahl UNH

## F) Schneller Endcheck (5 Punkte)

- Keine Feldverschiebung in ENF (Doppelpunkt nur im Kompositfeld)
- Alle Geld-/Mengenfelder haben 2 Nachkommastellen
- Schluesselwerte aus Anlage 3 passen zu Sammelgruppenschluessel
- NAD ist vollstaendig, falls INV-Status nicht geliefert wird
- BES/GES/UNT/UNZ sind rechnerisch und formal konsistent

## G) Werte aus deinem aktuellen Beispiel

- Abrechnungscode: 61
- Tarifkennzeichen: 00000
- Positionsnummer: 306050601
- Menge: 08,00
- Einzelbetrag: 15000,00
- BES: 120000,00
