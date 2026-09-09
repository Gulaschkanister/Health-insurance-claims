---
title: "Teil 04 — Machbarkeit und Grenzen"
subtitle: "GKV-Abrechnung — Projektdokumentation"
lang: de
---

## Ausgangslage

Die Anforderungsliste aus Teil 03 enthält einen Punkt, der sich von allen anderen
unterscheidet: **F6 — die Nachricht an den zuständigen Empfänger zustellen.**

Alle übrigen Anforderungen lassen sich durch Programmieren erfüllen. Diese nicht
allein.

## Ziel dieses Teils

Herausfinden, **was von der Vision durch eigene Arbeit erreichbar ist und was an
Dritten hängt** — bevor Aufwand in etwas fließt, das am Ende an einer Zulassung
scheitert.

Und, ebenso wichtig: eine Antwort darauf, wie man weiterarbeitet, wenn ein Teil
des Ziels vorerst verschlossen bleibt.

## Überlegungen

### Der Irrtum, der bis hierhin unentdeckt blieb

Das Zielbild aus Teil 01 sagt: *Datei → Krankenkasse*. Das ist falsch.

Empfänger einer Lieferung ist nicht die einzelne Kasse, sondern eine
**Datenannahmestelle mit Entschlüsselungsbefugnis** — je Kassenart eine. Ihr
Kennzeichen steht im Nachrichtenkopf als Empfänger, nicht das der Kasse.

Das ist kein Detail an einer Stelle, sondern wirkt an dreien:

| Betroffen | Wie |
|---|---|
| **Der Empfänger** | Nicht die Kasse, sondern die für sie zuständige Annahmestelle |
| **Die Anzahl der Dateien** | Nicht eine je Kasse, sondern **eine je Annahmestelle und Kassenart** — das ändert die Bündelung, also genau das, was das Modell „Lieferung" nennt |
| **Die Zuordnung** | Welche Annahmestelle für welche Kasse zuständig ist, steht in einem eigenen, regelmäßig erneuerten Verzeichnis |

Das bisherige Vorgehen — eine handgepflegte Liste mit einem Eintrag je Kasse —
liefert damit **an den falschen Adressaten**. Dass das bisher nicht auffiel,
liegt daran, dass nichts davon je bei einer echten Stelle ankam.

### Was ein Verzeichnis besser kann als eine Liste

Die Zuordnung Kasse → Annahmestelle ließe sich abschreiben. Dagegen spricht:

| Handgepflegte Liste | Amtliches Verzeichnis |
|---|---|
| veraltet still | wird regelmäßig neu herausgegeben |
| ein Eintrag je Kasse, Dutzende Zeilen | eine Datei, maschinell lesbar |
| ein Tippfehler fällt erst bei der Zurückweisung auf | die Kennzeichen tragen eine Prüfziffer |

Besonders unangenehm ist die erste Zeile. **Eine Quelle, die still altert, ist
die schlechteste Art von Quelle** — sie liefert weiter Antworten, nur falsche.

### Was sich nicht programmieren lässt

Der Reihe nach, mit der jeweiligen Abhängigkeit:

| Voraussetzung | Wovon sie abhängt |
|---|---|
| Ein eigenes Betriebsstätten-Kennzeichen | einer vergebenden Stelle. Gebührenfrei, aber nicht selbst zu erzeugen |
| Ein Zertifikat für Signatur und Verschlüsselung | einer anerkannten Vergabestelle. Kostet, gilt ein Jahr, wird **je Betriebsstätte** ausgestellt und nicht je Kasse |
| Anmeldung als Kommunikationspartner | der jeweiligen Annahmestelle |
| Ein bestandenes Erprobungsverfahren | ebenfalls der Annahmestelle — sie prüft die eingereichten Dateien, bevor sie den Echtbetrieb freigibt |

Die dritte und vierte Zeile sind die eigentliche Hürde. Sie kosten kein Geld,
sondern **Zeit und die Mitwirkung einer Gegenstelle** — und beides lässt sich
nicht beschleunigen.

### Ein Zwischenstand, mit dem sich arbeiten lässt

Damit stellt sich die Frage: Wie geht es weiter, wenn der letzte Schritt vorerst
nicht gegangen werden kann?

Drei Möglichkeiten standen zur Wahl:

| Weg | Was dafür spricht | Was dagegen |
|---|---|---|
| **Warten**, bis alle Zulassungen vorliegen | Danach ist alles echt | Solange steht das Projekt. Und die Zulassung setzt ein funktionierendes Programm voraus — das wäre ein Kreis |
| **Nur bis zur Datei bauen** und den Versand offenlassen | ehrlich | Der ganze Rückweg — Rückmeldung, Einordnung, Fehlerbehandlung — bliebe ungebaut und ungeprüft |
| **Den Versand vollständig bauen, aber gegen eine nachgestellte Gegenstelle** | Der gesamte Ablauf inklusive Antwort ist durchspielbar | Es beweist nicht, dass eine echte Stelle die Datei annimmt |

Der dritte Weg wurde gewählt, **unter einer Bedingung**: Die nachgestellte
Gegenstelle darf nicht wohlwollend sein. Sie prüft mit demselben Regelwerk wie
das Programm selbst und antwortet in derselben Form wie eine echte Kasse — mit
Annahme, fachlicher Zurückweisung oder Syntaxfehler.

Eine Attrappe, die immer „in Ordnung" sagt, wäre wertlos gewesen. **Eine Prüfung,
die nie fehlschlägt, beweist nichts.**

## Entscheidung

**Die Vision bleibt unverändert. Erreichbar ist sie heute bis einschließlich der
fertigen, geprüften Datei.** Der Weg zur echten Annahmestelle wird gebaut, aber
gegen eine nachgestellte Gegenstelle erprobt.

Dazu drei Festlegungen:

| Festlegung | Grund |
|---|---|
| Der Versandweg liegt **hinter einer Schnittstelle** | Ein echter Weg tritt später an die Stelle des heutigen, ohne dass die Fachlichkeit sich ändert |
| Die Zuordnung zur Annahmestelle wird als **eigene Aufgabe** geführt, nicht nebenbei erledigt | Sie ist der Unterschied zwischen richtigem und falschem Empfänger |
| Was heute nicht geht, steht **ausdrücklich in der Dokumentation** | Ein unbenanntes Loch wird für einen Boden gehalten |

### Warum nicht der einfachere Weg

Man hätte den Versand als erledigt darstellen können — die Datei landet ja an
einem Ziel, und die nachgestellte Kasse antwortet.

Dagegen spricht der Zweck des Ganzen: **Wer sich auf eine Zustellung verlässt,
die nie bei einer echten Stelle ankam, entdeckt den Irrtum erst mit der ersten
echten Abrechnung.** Genau diese Art von spätem, teurem Fehler soll das Programm
verhindern. Sie in seiner eigenen Beschreibung zuzulassen, wäre widersinnig.

## Ergebnis

Eine Abgrenzung, die für alle folgenden Teile gilt:

| Anforderung | Stand |
|---|---|
| F1–F5 — Stammdaten, Erzeugung, Prüfung, Tor | durch eigene Arbeit erreichbar |
| **F6 — Zustellung** | **gebaut, aber nur gegen eine nachgestellte Gegenstelle erprobt** |
| F7 — Rückmeldung einordnen | gebaut; die Antworten stammen aus der Nachstellung |
| F8, F9 — Zähler, Betriebsdaten | erreichbar |

Und eine Liste dessen, was für den Echtbetrieb noch von außen kommen muss:
eigenes Kennzeichen, Zertifikat, Anmeldung bei der Annahmestelle, bestandene
Erprobung.

**Der wichtigste Ertrag dieses Teils ist aber kein Ergebnis, sondern eine
Korrektur:** Der Empfänger im Zielbild war falsch. Das aufzudecken hat nichts
gekostet außer Nachlesen — und wäre es unentdeckt geblieben, wäre jede einzelne
Lieferung an die falsche Stelle gegangen.

## Offen geblieben

- **Das Verzeichnis der Annahmestellen auszuwerten.** Es ist im selben Format
  aufgebaut wie die Abrechnungsnachricht selbst, kann also mit demselben Leser
  verarbeitet werden. Umgesetzt ist es nicht.
- **Ob der Ausweichordner bleiben darf.** Findet sich zu einer Kasse kein
  Empfänger, landet die Lieferung heute in einem Sammelordner statt in einer
  Meldung. Das verdeckt genau den Fehler, um den es hier ging — siehe Teil 19.
- **Wie lange eine Erprobung dauert.** Unbekannt, und nicht durch Arbeit zu
  verkürzen.
