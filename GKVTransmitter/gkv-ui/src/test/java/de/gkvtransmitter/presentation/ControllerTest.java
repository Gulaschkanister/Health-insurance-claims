package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.dta.Leistungsparameter;

/**
 * Prueft, was beim Programmstart geladen wird.
 *
 * <p>Der {@code Controller} liest Profile und Vorlagen und oeffnet die
 * Datenbank. <b>Schlaegt er fehl, startet die Anwendung nicht</b> - und der
 * Fehler faellt erst auf dem Rechner auf, auf dem sie starten soll. Bis zum
 * 05.09.2026 gab es dafuer keinen Test.</p>
 *
 * <p>Geprueft wird nicht, dass er "irgendetwas" laedt, sondern dass er das
 * laedt, worauf sich die Oberflaeche verlaesst: beide Kursvorlagen mit ihren
 * Segmenten, und in den Segmenten jedes Feld, das
 * {@link Leistungsparameter#BLAUPAUSENFELDER} spaeter darin sucht. Ohne diese
 * Kette lautet jede Rechnung auf die Vorbelegung - genau der Fehler, der bis
 * zum 05.09.2026 jede Leistung mit 15.000,00 EUR je Termin abrechnete.</p>
 */
@DisplayName("Controller")
class ControllerTest {

    /**
     * Eine eigene Datenbank, damit der Test nicht die des Benutzers oeffnet.
     *
     * <p>Nicht {@code @TempDir}: die Verbindung bleibt offen, solange die
     * Anwendung laeuft, und Windows laesst eine offene Datei nicht loeschen -
     * JUnit machte daraus einen roten Test. Unter {@code target} raeumt
     * {@code mvn clean} auf.</p>
     */
    private static void eigeneDatenbank() {
        System.setProperty("gkv.db.path",
                Path.of("target", "controller", "controller.db").toString());
    }

    @Test
    @DisplayName("laedt beide Kursvorlagen mit ihren Segmenten")
    void laedtDieVorlagen() {
        eigeneDatenbank();

        Controller controller = new Controller();

        var vorlagen = controller.getGlobalDefinitions().getInvoiceTemplateCollection();
        assertFalse(vorlagen.isEmpty(), "Ohne Vorlage laesst sich keine Blaupause anlegen");
        assertTrue(vorlagen.keySet().stream().anyMatch(name -> name.contains("Geburtsvorbereitung")),
                vorlagen.keySet().toString());
        assertTrue(vorlagen.keySet().stream().anyMatch(name -> name.contains("Rückbildung")),
                vorlagen.keySet().toString());
        vorlagen.forEach((name, nachricht) -> assertFalse(nachricht.getSegments().isEmpty(),
                "Die Vorlage \"" + name + "\" hat keine Segmente"));
    }

    /**
     * Die Kette, die schon einmal gerissen ist.
     *
     * <p>{@code Leistungsparameter} sucht seine Werte in der Blaupause ueber
     * <em>Namen</em>, und die Blaupause bekommt ihre Felder aus den Segmenten,
     * die hier geladen werden. Passt ein Name nicht, faellt nichts aus -
     * {@code Leistungsparameter} nimmt still die Vorbelegung.</p>
     *
     * <p>{@code BlaupausenfelderTest} prueft dieselbe Kette im Kern. Hier steht
     * sie noch einmal am geladenen Zustand: dort gegen die Dateien, hier gegen
     * das, was der Programmstart daraus gemacht hat.</p>
     */
    @Test
    @DisplayName("laedt jedes Feld, das die Abrechnung spaeter in der Blaupause sucht")
    void laedtDieGesuchtenFelder() {
        eigeneDatenbank();

        Controller controller = new Controller();

        var vorhandeneNamen = controller.getGlobalDefinitions().getInvoiceTemplateCollection()
                .values().stream()
                .flatMap(nachricht -> nachricht.getSegments().stream())
                .flatMap(segment -> segment.getValueFields().keySet().stream())
                .toList();
        for (String gesucht : Leistungsparameter.BLAUPAUSENFELDER) {
            assertTrue(vorhandeneNamen.contains(gesucht),
                    "\"" + gesucht + "\" wird gesucht, aber von keiner Vorlage angeboten."
                            + " Die Abrechnung fiele still auf die Vorbelegung zurueck.");
        }
    }

    @Test
    @DisplayName("oeffnet die Datenbank, ueber die die Masken arbeiten")
    void oeffnetDieDatenbank() {
        eigeneDatenbank();

        Controller controller = new Controller();

        assertNotNull(controller.getDatabase());
        assertNotNull(controller.getDatabase().getAllPatients(),
                "Eine Abfrage muss gehen, nicht nur das Oeffnen");
        assertNotNull(controller.getFactoryManager());
    }
}
