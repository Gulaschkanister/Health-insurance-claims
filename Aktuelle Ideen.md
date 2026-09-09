# Aktuelle Ideen — GKV-Abrechnung

Stand: 9. September 2026

Ablage für alles, was zu **diesem** Projekt gehört und noch nicht umgesetzt ist.
Ideen zu anderen Vorhaben liegen auf dem Desktop in `Aktuelle Ideen.md`.

---

## Wo was steht

| Frage | Dokument |
|---|---|
| Wie kommt eine Lieferung zur Kasse? | `GKVTransmitter_Dokumentation.md`, Kapitel „Der Weg zur Kasse" |
| Was ist in welcher Reihenfolge zu bauen? | `Information/Umsetzungsplan.md`, Teil 1 |
| Was ist danach dauerhaft zu tun? | `Umsetzungsplan.md`, Teil 2 (Wartung) |
| Was passiert bei einer Zurückweisung? | `Umsetzungsplan.md`, Teil 3 |
| Wie wird gearbeitet? | `Umsetzungsplan.md`, Teil 4 · `Information/spezifikationen/VORLAGE.md` |
| Wie wird der Alltag leichter? | `Umsetzungsplan.md`, Teil 6 |
| Wo steht das Projekt gerade? | `Information/Naechste_Schritte.md` |
| Schulische Aufarbeitung | `Information/Schulprojekt.md` |

**Der Umsetzungsplan ist die Wahrheit.** Was hier steht, ist das, was dort noch
keinen Platz hat.

---

## Die drei Dinge, die heute schon stören

Aus dem Umsetzungsplan herausgehoben, weil sie unabhängig von jeder
Grundsatzentscheidung gelten:

1. **Warnungen erreichen den Bildschirm nicht.** `generateAndRoute` sammelt den
   Prüfbericht und wirft ihn bei fehlerfreiem Lauf weg. Betrifft alle Warnungen,
   nicht nur die neue Positionsnummer-Regel. *(Umsetzungsplan 1.1)*
2. **Das Programm kennt nur `FKT+01`.** Auf eine Zurückweisung lässt sich damit
   nicht antworten — dieselbe Rechnung erneut zu senden sieht für die Kasse aus
   wie eine Doppelabrechnung. *(1.4)*
3. **Die Abrechnungspositionsnummer ist neunstellig** und damit für Hebammenhilfe
   falsch. Bewusst nicht erfunden; `PositionsnummerRegel` warnt. *(D.2)*

---

## Lose Ideen, noch in keinem Plan

- **Kostenträgerdatei als Prüfquelle, nicht nur als Adressbuch.** Sie enthält
  auch, welche Kassen welche Abrechnungscodes annehmen. Damit ließe sich vorab
  prüfen, ob eine Kasse Hebammenhilfe überhaupt digital entgegennimmt.
- **Ein „Trockenlauf" gegen die simulierte Gegenstelle als Menüpunkt.**
  `SimulierterKassenTransport` existiert, ist aber nur über Tests erreichbar.
  Ein Knopf „einmal durchspielen, ohne zu senden" wäre die beste Übung vor der
  Erprobung.
- **Die Belegnummer ist frei gebildet** (`HEB` + Jahr/Monat + laufende Nummer).
  Das ist zulässig, aber es gibt keine Garantie gegen Doppelvergabe über
  Jahresgrenzen. Vor dem Echtbetrieb ansehen.
- **Umsatzsteuer steht fest auf 19.** Für Hebammenleistungen ist die Frage, ob
  überhaupt Umsatzsteuer anfällt — § 4 Nr. 14 UStG. Ungeprüft, und es steht in
  jeder erzeugten Datei.
- **Mehrere Leistungserbringerinnen in einer Installation.** Heute ist der
  Absender der je Abrechnung gewählte Dienstleister. Mit Betriebsdaten (1.7)
  wird das eine Betriebseigenschaft — aber eine Praxis mit zwei Hebammen
  bräuchte zwei Betriebsdatensätze.
- **Export für den Steuerberater.** Alles, was erzeugt und bezahlt wurde, in
  einem offenen Format. Fällt fast als Nebenprodukt aus dem Übermittlungs-
  protokoll (1.5) ab.

---

## Ausdrücklich verworfen

| Idee | Grund |
|---|---|
| Einstellungen in einer eigenen Datei | konnte nichts, was die Datenbank nicht auch kann — 07.09.2026 |
| `java.util.prefs` für Einstellungen | landet in der Registry, entzieht sich Umzug und Sicherung |
| Eigene Datenaggregation statt Kostenträgerdatei | die amtliche Datei ist frei und vierteljährlich aktuell |
| Automatische Abrechnung ohne Anstoß | erst soll der Weg von Hand verlässlich sein |
| Weboberfläche | die Daten liegen beim Benutzer, und das ist ein Vorzug |

---

## Was jemand entscheiden muss, nicht ich

- **Selbst übermitteln oder über eine Abrechnungsstelle?** Bei drei Kursen im
  Jahr rechnet sich der eigene Weg nicht — die Pflege kostet mehr als die
  Gebühr. Siehe „Was der eigene Weg wirklich kostet" in der Dokumentation.
- **Vier Branches auf `origin`** (`dev`, `restart`, `refactor`, `ai`) warten auf
  ein Wort, bevor sie gelöscht werden.
- **98 Testdateien** liegen noch im echten Ausgangsordner unter
  `%LOCALAPPDATA%\GKV-Abrechnung\dta_output`. Entstanden vor der Umlenkung auf
  `gkv.home`, seither kommen keine dazu. Löschen ist Simons Entscheidung.
