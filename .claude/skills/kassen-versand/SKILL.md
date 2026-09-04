---
name: kassen-versand
description: Versand von DTA-Lieferungen an Krankenkassen, Transportwege, Endpunktverwaltung und Auswertung der Kassenrückmeldungen. Verwenden beim Anlegen eines neuen Transports (Datei, SMTP, HTTP), beim Ändern der Endpunktliste, bei Fragen zu Kassen-IK, Zielverzeichnissen, Antwortprotokollen, angenommen/abgelehnt/Syntaxfehler, oder beim Erproben des Ablaufs ohne echte Gegenstelle.
---

# Versand an die Krankenkassen

Alles unter `gkv-core/src/main/java/de/gkvtransmitter/dispatch/`.

## Der Ablauf

```
AbrechnungService.createAndDispatch(...)
 └── DtaDispatchService.generateAndRoute(abrechnungen, outDir)
      ├── 1. erzeugen        DtaFactory je Abrechnung
      ├── 2. prüfen          DtaValidationService über alle
      │                      ein Fehler ⇒ DtaValidierungsException, nichts geht raus
      └── 3. zustellen       je Abrechnung:
             outDir/staging/<kassenIk>/patient_<id>_<zeit>.dta
             └── BillingOfficeTransport.send(datei, endpunkt)
                  └── Zielverzeichnis des Endpunkts
```

**Erst alles prüfen, dann zustellen.** Würde je Abrechnung geprüft und sofort
zugestellt, wäre bei einem Fehler in der Mitte eines Laufs bereits ein Teil bei
der Kasse und müsste dort einzeln storniert werden. Wer den Ablauf umbaut, muss
das erhalten.

Die Zwischenablage unter `staging/` ist beabsichtigt: die Datei existiert dort
unverändert weiter, auch wenn der Transport sie verschiebt oder umbenennt.

## Endpunkte

Konfiguriert in `gkv-core/src/main/resources/billing-office-endpoints.json`,
verwaltet von `BillingOfficeEndpointRegistry`. Aktuell sind 23 Kassen
hinterlegt.

```json
{
  "fallbackRoot": "dta_output/outbox",
  "targets": [
    {
      "kassenIk": 108310400,
      "name": "AOK Bayern",
      "transportType": "FILE",
      "destinationDirectory": "dta_output/outbox/aok-bayern",
      "enabled": true
    }
  ]
}
```

Aufgelöst wird über die Kassen-IK der versicherten Person
(`patient.getKassenIk()`). Ist keine Kasse hinterlegt, greift `fallbackRoot` —
die Lieferung geht also nicht verloren, landet aber im Sammelordner. Wer eine
Kasse ergänzt, trägt sie hier ein; Code ist dafür nicht zu ändern.

`enabled: false` blendet einen Endpunkt aus, ohne den Eintrag zu verlieren.

## Einen neuen Transport schreiben

`BillingOfficeTransport` hat drei Methoden:

```java
public interface BillingOfficeTransport {
    BillingOfficeTransportType getType();
    Path send(Path sourceFile, BillingOfficeEndpoint endpoint) throws IOException;
    BillingOfficeEndpointCheck probe(BillingOfficeEndpoint endpoint);
}
```

`probe` prüft die Erreichbarkeit, ohne etwas zu senden — die Oberfläche ruft
das über `DtaDispatchService.checkConfiguredEndpoints()` auf. Ein neuer
Transport muss `probe` sinnvoll umsetzen, sonst lässt sich eine Fehlkonfiguration
erst beim Echtversand feststellen.

`send` gibt den Pfad der **zugestellten** Datei zurück, nicht den der Quelle.

Für einen Transport mit eigenem Typ ist `BillingOfficeTransportType` zu
erweitern und in `BillingOfficeEndpointDefinition` zu berücksichtigen.

## Erproben ohne echte Gegenstelle

`SimulierteKassenGegenstelle` nimmt eine Lieferung an, prüft sie mit demselben
Regelwerk wie die Anwendung und schreibt ein Antwortprotokoll in der Form einer
Kassenrückmeldung:

```java
SimulierteKassenGegenstelle kasse = new SimulierteKassenGegenstelle("AOK Testkasse");
BillingOfficeResponse antwort = kasse.empfange(dtaInhalt);
```

Sie unterscheidet dabei wie eine echte Kasse:

- **Syntaxfehler**, wenn ausschließlich Struktur beanstandet wird (Codes mit
  `SYNTAX_`, `RAHMEN_`, `UNT_`, `UNZ_`) — die Datei muss technisch neu erzeugt
  werden
- **Zurückweisung** bei inhaltlichen Beanstandungen — die Abrechnung muss
  fachlich korrigiert werden

`SimulierterKassenTransport` **umschließt** einen vorhandenen Transport, statt
ihn zu ersetzen:

```java
SimulierterKassenTransport transport = new SimulierterKassenTransport();
DtaDispatchService service = new DtaDispatchService(registry, transport);
service.generateAndRoute(abrechnungen, outDir);

BillingOfficeResponse antwort = transport.letzteAntwort(108310400);
```

Neben jeder zugestellten Datei liegt danach ein `*.antwort.txt`. Der Ablauf
lässt sich damit bis zur Rückmeldung durchspielen, ohne Zugangsdaten oder
Zertifikate und ohne am Versandweg etwas zu ändern.

**Das ersetzt keinen Echtversand** und keine Prüfung durch eine reale Kasse.

## Rückmeldungen auswerten

`BillingOfficeResponseParser` ordnet eine Antwort einer Bewertung zu.

| Einstufung | Bedeutung | Was zu tun ist |
|---|---|---|
| `ACCEPTED` | angenommen | nichts |
| `REJECTED` | fachlich zurückgewiesen | Abrechnung korrigieren |
| `SYNTAX_ERROR` | Formfehler | Datei neu erzeugen |
| `TECHNICAL_ERROR` | Übertragungsproblem | erneut senden |
| `UNKNOWN` | nicht einzuordnen | **ein Mensch muss draufschauen** |

Zwei Eigenschaften sind wesentlich und dürfen beim Erweitern nicht verloren
gehen:

**Die Reihenfolge geht vom Spezifischen zum Allgemeinen.** „Technischer Fehler"
muss vor dem allgemeinen „fehler" stehen, sonst gilt jeder technische Fehler
als fachliche Ablehnung — und die beiden verlangen unterschiedliche Reaktionen.
Genau dieser Fehler steckte in der früheren Auswertung.

**Die Suchbegriffe sind an Wortgrenzen gebunden** (`\b…\b`). Ohne das trifft
`ok` in „Protokoll", und eine Ablehnung wird als Annahme gelesen. Zusammengesetzte
Wörter brauchen deshalb einen eigenen Eintrag: `fehlerprotokoll` wird von
`fehler` nicht erfasst.

**Eine nicht einzuordnende Antwort gilt nie als Annahme.** Lieber `UNKNOWN` und
eine Nachfrage als eine stillschweigend verlorene Abrechnung.

Beim Ergänzen von Begriffen: Kleinschreibung, und bei Umlauten beide
Schreibweisen aufnehmen (`zurückgewiesen` und `zurueckgewiesen`), weil
Kassenprotokolle in beiden Formen vorkommen.

## Fehlerbehandlung

| Ausnahme | Wann |
|---|---|
| `DtaValidierungsException` | Prüfung nicht bestanden — trägt den vollständigen Bericht, **nichts wurde versendet** |
| `DispatchException` | Zustellung fehlgeschlagen (Ein-/Ausgabe) |

In der Oberfläche wird `DtaValidierungsException` gesondert gefangen und über
`zeigePruefbericht` mit allen Beanstandungen angezeigt — nicht als generische
Fehlermeldung.

## Für einen echten Versandweg

Wird künftig ein realer Weg umgesetzt (etwa signierte und verschlüsselte
Übermittlung), gehört das hinter `BillingOfficeTransport`. Dazu werden
zusätzlich benötigt: Zertifikate einer anerkannten Stelle, Zugangsdaten und ein
eigenes Betriebsstätten-IK. Der übrige Ablauf — Erzeugung, Prüfung, Routing,
Auswertung — bleibt unverändert.
