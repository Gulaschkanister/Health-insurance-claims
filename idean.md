Vision
Erstellen von abrechnungen für z.B einen Geburtsvorbereitungskurs und andere Kurse

Schritt 1
Lesen/Erstellen der Segment Struktur warscheinlich besser als json, um spätere Änderungen durch den GKVSpitzenverband übernehmen zu können.

Schritt 2
Erstellen, Bearbeiten, Löschen von Personen
Perosnen Gruppen
Dienstleister und Teilnehmer

Schritt 3
Personen Gruppen zuordnen, bearbeiten löschen
Da Gruppen häufiger stattfinden können hierüber die Blaupause für Gruppen nutzbar sein. Oder auch Inhalte wie häufigmal die Termine stattfinden einstellen.

Schritt 4
Abrechnungsvorlage erstellen
Hier werden Alle Felder gezeigt, welche nicht Personen bezogen oder Intern sind, die ein Benutzer nicht ausfüllen muss.

Diese Abrchnungsvorlage soll später in der erstellung der Abrechnung verwendet werden um festzulegen, was abregechnet werden soll.

Schritt 5
Speichern der Abrechnungsvorlage
Speichern in der Datenbank, möglichs so, dass es später auch für unterschiedliche möglich ist

Schritt 6
Erstellung einer Abrechnung
Hier Soll es Checkboxen geben it einer Suchfunktion, um Personen zu finden die Teilgenommen haben ebenso auch für den Dienstleister. So soll die Auswahl vereinfacht werden. Ebenso soll s die möglichkeit geben Gruppen einzufügen. Nachdem eine gruppe ausgewählt wurde wird diese angezeigt und per default die erwarteten Termine dort notiert als Zahl bspw. 5. 
Das kann dann dort angepasst werden um zu sagen, diese personen waren nur so und so viel da.

Natürlich gibt es dann die Möglichkeit in einem Fenster zu prüfen ob alle Angaben stimmen. Wenn das der Fall ist wird eine DTA valide Datei erstellt basierend auf der validen Vorlage die ich schon gemacht habe.

Schritt 7
Speichern der Abrechnung im System als PDF mit verweisen und Logs. Um später auch genau das nachweisen zu können.

Schritt 8
Versenden der DTA an die erforderlichen Schnittstellen. Bearbeiten der Antworden oder speichern der Rückmeldungen(Aktuell nicht nicht klar was dort ankommt oder wie man Infos bekommt).

Schritt 9
Multi Login einfügen, damit die Bearbeitung für mehrere Benutzer möglich ist

Schritt 10
Sicherheit für die Datenlager erstellen.

Schritt 11
Dokumentation und Testfälle für alles erstellen und veröffentlichen. Mit glück wen erfahrenes drüberschauen lassen. Sonst könnte man das auch anderen personen anbieten.


Datenstruktur aktuell ist die nicht so schlecht, jedoch wird viel interne Elemente auch mit der json vermischt. Ebenso auch die validierung. Daher möchte ich das nochmal etwas geregelter machen, dass klar ist was was ist und keine unnötige komplexität hinein kommt.