package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ein ganzer Monat, in einem Zug.
 *
 * <p>Der Test, der bisher fehlte. Jeder andere Test in diesem Verzeichnis
 * prueft <em>eine</em> Maske fuer sich, mit einem Ersatzspeicher darunter und
 * einem Ersatzrahmen darum. Dass die vier Masken zusammenpassen - dass eine in
 * der Personenmaske angelegte Frau in der Gruppenmaske ankommt, dass die dort
 * gebildete Gruppe in der Abrechnungsmaske auftaucht, dass die Blaupause dabei
 * gefunden wird -, prueft keiner von ihnen.</p>
 *
 * <p>Hier laeuft die <b>echte</b> Anwendung: echter {@code Controller}, echte
 * SQLite-Datenbank unter {@code @TempDir}, echte Masken, echte Meldungsecke.
 * Bedient wird sie ueber {@link Bedienung} - dasselbe Werkzeug, mit dem sich
 * ein Ablauf auch von Hand durchspielen laesst.</p>
 *
 * <p><b>Was dieser Test nicht ist:</b> ein Ersatz fuer die Maskentests. Er
 * sagt, dass es irgendwo klemmt, nicht wo. Wenn er rot wird und kein anderer,
 * liegt der Fehler zwischen zwei Masken - und genau dort hat dieses Projekt
 * schon zweimal einen gehabt: der Einzelbetrag, der zwischen JSON-Datei und
 * suchender Klasse verlorenging, und die Bearbeitung, die nicht pruefte.</p>
 */
@DisplayName("Ein Monat von Anfang bis Ende")
class AblaufTest {

    /** Der Ablauf als Befehlsfolge; auch von Hand aufrufbar, siehe Bedienung. */
    private static final String ABLAUF = "/ablaeufe/ein-monat.txt";

    /**
     * Kein {@code @TempDir}.
     *
     * <p>Die Anwendung haelt ihre SQLite-Verbindung offen, solange sie laeuft -
     * und sie laeuft ueber das Testende hinaus, weil die JavaFX-Laufzeit fuer
     * alle Tests <em>einmal</em> hochgefahren wird. JUnit versucht sein
     * {@code @TempDir} danach zu loeschen, Windows laesst die offene Datei
     * nicht los, und der Test wird rot, obwohl der Ablauf durchgelaufen ist.
     * Deshalb ein Verzeichnis unter {@code target}, das {@code mvn clean}
     * wegraeumt.</p>
     */
    private static final Path VERZEICHNIS = Path.of("target", "ablauf").toAbsolutePath();

    @Test
    @DisplayName("Teilnehmerin anlegen, Gruppe bilden, Blaupause anlegen, abrechnen")
    void einMonat() throws Exception {
        // Ohne Testdaten: der Ablauf legt seine Daten selbst an, und genau das
        // ist der Punkt. Mit vorgefertigten Bestaenden bliebe offen, ob das
        // Anlegen ueber die Oberflaeche wirklich funktioniert.
        Bedienung bedienung = Bedienung.aufbauen(VERZEICHNIS, false);

        bedienung.fuehreAus(zeilen());

        List<String> protokoll = bedienung.protokoll();
        assertFalse(protokoll.isEmpty(), "Der Ablauf hat nichts getan");
        assertTrue(protokoll.stream().anyMatch(zeile -> zeile.startsWith("klick abrechnung-start")),
                "Der Ablauf ist nicht bis zur Abrechnung gekommen:\n"
                        + String.join("\n", protokoll));
    }

    /**
     * Die Wartungsseiten, an einem Betrieb mit ablaufendem Zertifikat.
     *
     * <p>Block G ist die Bedingung, unter der der eigene Uebermittlungsweg
     * ueberhaupt sinnvoll ist - und seine drei Seiten haengen an Daten aus
     * drei Quellen: den Betriebsdaten, dem Stand der Unterlagen und dem
     * Dienstleisterprofil. Ob die zusammenkommen, sagt kein Maskentest.</p>
     */
    @Test
    @DisplayName("Betriebsdaten erfassen, Wartung und Unterlagenstand ansehen")
    void wartung() throws Exception {
        Bedienung bedienung = Bedienung.aufbauen(Path.of("target", "wartung").toAbsolutePath(), false);

        bedienung.fuehreAus(zeilen("/ablaeufe/wartung.txt"));

        assertTrue(bedienung.protokoll().stream()
                        .anyMatch(zeile -> zeile.startsWith("oeffne Registrierungsblatt")),
                "Der Ablauf ist nicht bis zum Registrierungsblatt gekommen:\n"
                        + String.join("\n", bedienung.protokoll()));
    }

    private static List<String> zeilen() throws Exception {
        return zeilen(ABLAUF);
    }

    private static List<String> zeilen(String ablauf) throws Exception {
        try (var quelle = AblaufTest.class.getResourceAsStream(ablauf)) {
            if (quelle == null) {
                throw new AssertionError("Kein Ablauf unter " + ablauf);
            }
            return new String(quelle.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines().toList();
        }
    }
}
