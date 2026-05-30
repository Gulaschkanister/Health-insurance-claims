# Annotierte Ideenskizze

Originaltext und darunter kompakte Kommentare / Fragen zur Umsetzung.

---

Vision
Erstellen von abrechnungen für z.B einen Geburtsvorbereitungskurs und andere Kurse

Kommentar: Gute, konkrete Domain; Ziel sollte als User-Story formuliert werden (z.B. "Als Abrechner möchte ich..."). Das hilft bei Priorisierung und Tests.

---

Schritt 1
Lesen/Erstellen der Segment Struktur warscheinlich besser als json, um spätere Änderungen durch den GKVSpitzenverband übernehmen zu können.

Kommentar: Formatentscheidung kritisch:
- Empfehlung: JSON mit einer klaren Schema-/Version-Nummer (JSON Schema) oder YAML (besser lesbar) — beides maschinenlesbar.
- Falls externe Instanz Vorgaben in XML liefert, halte Import/Export asynchron (Converter), statt internes Format zu wechseln.
- Wichtig: Trenne Strukturdefinition (Metadaten) von Instanzdaten und Validierungsregeln.

Frage: Gibt es konkrete externe Referenzformate vom GKV-Spitzenverband, die unterstützt werden müssen?

---

Schritt 2
Erstellen, Bearbeiten, Löschen von Personen
Perosnen Gruppen
Dienstleister und Teilnehmer

Kommentar:
- Kernmodell: `Person` (Basis) + Rollen/Entitäten (Dienstleister, Teilnehmer, Kontakt) als einfache Typ-Attribute.
- CRUD über generische Repository-/Service-Schnittstellen, nicht viele spezielle DAOs.
- Nutze eindeutige IDs und Änderungs-Logs (Audit) von Anfang an.

---

Schritt 3
Personen Gruppen zuordnen, bearbeiten löschen
Da Gruppen häufiger stattfinden können hierüber die Blaupause für Gruppen nutzbar sein. Oder auch Inhalte wie häufigmal die Termine stattfinden einstellen.

Kommentar:
- `GroupTemplate` (Blaupause) vs. `GroupInstance` (konkreter Terminverlauf).
- Template definiert erwartete Termine, Regeln, Standardteilnehmer-Rollen.
- Bei Teilnehmer-Teilnahme: speichere tatsächliche Anwesenheit pro Termin (nicht nur Zahl), das vereinfacht Reporting.

---

Schritt 4
Abrechnungsvorlage erstellen
Hier werden Alle Felder gezeigt, welche nicht Personen bezogen oder Intern sind, die ein Benutzer nicht ausfüllen muss.

Kommentar:
- `InvoiceTemplate` ähnlich zu GroupTemplate: enthält Mappings zu Segmentdefinitionen und Feldmetadaten (Typ, Pflicht, Default, Validierungsregeln).
- UI: Template-Editor sollte Schema-getrieben sein (Felder aus Metadaten automatisch rendern), damit neue Felder ohne Code auskommen.

---

Schritt 5
Speichern der Abrechnungsvorlage
Speichern in der Datenbank, möglichs so, dass es später auch für unterschiedliche möglich ist

Kommentar:
- Versioniere Vorlagen (Templates) und ermögliche Aktiv-/Inaktiv-Status.
- Denk an Migrationspfad wenn Template-Definitionen sich ändern (record which template version was used for generated DTA/PDF).

---

Schritt 6
Erstellung einer Abrechnung
Hier Soll es Checkboxen geben mit einer Suchfunktion, um Personen zu finden die Teilgenommen haben ebenso auch für den Dienstleister. So soll die Auswahl vereinfacht werden. Ebenso soll s die möglichkeit geben Gruppen einzufügen. Nachdem eine gruppe ausgewählt wurde wird diese angezeigt und per default die erwarteten Termine dort notiert als Zahl bspw. 5.
Das kann dann dort angepasst werden um zu sagen, diese personen waren nur so und so viel da.

Kommentar:
- UI-Flow klar: Auswahl (Template, Zeitraum) → Auswahl Teilnehmer/Dienstleister → Zuordnung/Anwesenheit editieren → Review → DTA-Generierung.
- Suche/Filter: Indexpfade (Name, Gruppe, ID) und Autocomplete.
- Businessregel: Berechne Default-Anwesenheit aus `GroupInstance` und markiere Abweichungen explizit.
- Validierung: Vor Erzeugung DTA alle Regeln gegen Template-Version prüfen.

---

Schritt 7
Speichern der Abrechnung im System als PDF mit verweisen und Logs. Um später auch genau das nachweisen zu können.

Kommentar:
- Speichere sowohl die Daten (JSON) und das erzeugte PDF sowie Metadaten (Template-Version, Ersteller, Zeitstempel, Prüfstatus, Hash).
- Logs: Aktionen (Erzeugt, Geprüft, Versendet) mit User-IDs, Timestamps und Ergebnis-Codes.

---

Schritt 8
Versenden der DTA an die erforderlichen Schnittstellen. Bearbeiten der Antworden oder speichern der Rückmeldungen(Aktuell nicht nicht klar was dort ankommt oder wie man Infos bekommt).

Kommentar:
- Schnittstellen: Trenne Erzeugung von Versand. Erstelle ein „Outbound“-Modul, das DTA-Dateien queued, retried und Status verarbeitet.
- Rückmeldungen: Speichere empfangene ACK/NACK oder Reports in einer Inbox; lege Regeln fest (z.B. manuelle Nachbearbeitung bei Fehlern).
- Falls unbekannt: Fang mit einer einfachen „Speichere alle Rückmeldungen als Dateien in DB/FS“-Strategie an.

---

Schritt 9
Multi Login einfügen, damit die Bearbeitung für mehrere Benutzer möglich ist

Kommentar:
- Rolle/Permission-Modell (RBAC) von Anfang an: Admin, Bearbeiter, Prüfer, Lesender Zugriff.
- Session-Handling, Audit und Locking bei gleichzeitiger Bearbeitung (optimistic locking reicht oft).

---

Schritt 10
Sicherheit für die Datenlager erstellen.

Kommentar:
- Verschlüsselung ruhender Daten (voll DB oder Feldverschlüsselung für sensible Felder) und TLS für Transport.
- Datenschutz (Pseudonymisierung) und minimaler Zugriff: Prinzip der geringsten Privilegien.
- Backup/Restore-Strategie dokumentieren.

---

Schritt 11
Dokumentation und Testfälle für alles erstellen und veröffentlichen. Mit glück wen erfahrenes drüberschauen lassen. Sonst könnte man das auch anderen personen anbieten.

Kommentar:
- Tests: Unit-Tests für Services, Integrationstests für DTA-Generierung und End-to-End für UI-Flow.
- Dokumentation: API-Docs, Betriebsanleitung (Backup, Migration), Entwickler-README.

---

Abschließender Kommentar

Datenstruktur aktuell ist die nicht so schlecht, jedoch wird viel interne Elemente auch mit der json vermischt. Ebenso auch die validierung. Daher möchte ich das nochmal etwas geregelter machen, dass klar ist was was ist und keine unnötige komplexität hinein kommt.

Kommentar: Deine Einschätzung ist korrekt. Konkretes Vorgehen:
1. Trenne folgende Schichten klar: Metadaten/Definitionen (Profile/Segment-Schema), Instanzdaten (Person, Invoice), Validierungsregeln, Persistenz/Transport.
2. Mache Definitionsdateien so generisch wie möglich (Schema + UI-Hints + Mapping zu DTA-Feldern).
3. Baue einen kleinen Importer/Converter für externe Formate (GKV), damit Kernmodell stabil bleibt.
4. Iterative Migration: für bestehende JSONs Adapter schreiben, nicht sofort alles migrieren.


---

Nächste Schritte (Vorschlag)
- Priorisieren: Welche Abrechnungstypen sind zuerst wichtig? (z.B. Geburtsvorbereitungskurs)
- Minimaler Daten-Schema-Vorschlag für `Person`, `GroupTemplate`, `GroupInstance`, `InvoiceTemplate`, `InvoiceInstance` erstellen.
- Prototyp: UI-getriebener Template-Editor (schemagetrieben) minimal implementieren.

Wenn du willst, kann ich jetzt sofort ein erstes schlankes JSON-Schema für `InvoiceTemplate` und `Person` vorschlagen und als Datei in das Repository legen.
