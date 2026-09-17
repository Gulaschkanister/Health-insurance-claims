package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.dta.DtaFactory;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.wartung.Unterlagenstand;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Der Stand der Unterlagen auf dem Bildschirm.
 *
 * <p>Die Anlagen unter {@code Information/} waren am 07.09.2026 zwei Monate
 * ueberholt, und es war niemandem aufgefallen. Diese Maske ist die Antwort
 * darauf: Veralten faellt nicht auf, solange nichts es zeigt.</p>
 */
@DisplayName("Stand der Unterlagen")
class UnterlagenMaskeTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 9, 17);

    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        texte = new AppMessages("/messages/ui-messages.json");
    }

    private Region maskeAm(LocalDate tag) {
        return new UnterlagenMaske(new JavaFxUiFactory(), texte, Unterlagenstand.lade(), tag).maske();
    }

    private String zeile(Region maske, String kennung) {
        Label label = (Label) maske.lookup("#" + kennung);
        assertNotNull(label, "Keine Zeile mit der Kennung " + kennung);
        return label.getText();
    }

    /**
     * Womit die Anwendung sendet, kommt aus dem Quelltext.
     *
     * <p>Und nicht aus der gepflegten Liste: Was hier steht, soll die Wahrheit
     * ueber den Quelltext sagen. Gingen die beiden auseinander, waere genau
     * das der Befund.</p>
     */
    @Test
    @DisplayName("nennt die Version, mit der gesendet wird")
    void nenntDieSendeversion() {
        JavaFxLaufzeit.aufFxFaden(() -> assertTrue(
                zeile(maskeAm(HEUTE), UnterlagenMaske.ID_SENDEVERSION)
                        .contains(DtaFactory.NACHRICHTENVERSION),
                zeile(maskeAm(HEUTE), UnterlagenMaske.ID_SENDEVERSION)));
    }

    /**
     * Der Termin, den heute niemand kennt.
     *
     * <p>Anlage 1 und Anlage 3 in der Version 22 liegen vor und sind ab dem
     * 01.02.2027 anzuwenden.</p>
     */
    @Test
    @DisplayName("nennt den naechsten Termin mit Datum")
    void nenntDenNaechstenTermin() {
        JavaFxLaufzeit.aufFxFaden(() -> assertTrue(
                zeile(maskeAm(HEUTE), UnterlagenMaske.ID_TERMIN).contains("01.02.2027"),
                zeile(maskeAm(HEUTE), UnterlagenMaske.ID_TERMIN)));
    }

    /**
     * Eine abgelaufene Fassung geht allem anderen vor.
     *
     * <p>Das ist der Fall, den niemand bemerkt: Die Anwendung sendet weiter,
     * die Kasse weist zurueck, und die Ursache steht auf einem Deckblatt.</p>
     */
    @Test
    @DisplayName("meldet nach dem 30.04.2027 zuerst die abgelaufene Fassung")
    void meldetAbgelaufenes() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            String termin = zeile(maskeAm(LocalDate.of(2027, 5, 1)), UnterlagenMaske.ID_TERMIN);

            assertTrue(termin.contains("30.04.2027"), termin);
            assertTrue(termin.contains("Gültigkeit"), termin);
        });
    }

    @Test
    @DisplayName("listet jede hinterlegte Unterlage")
    void listetAlle() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Region liste = (Region) maskeAm(HEUTE).lookup("#" + UnterlagenMaske.ID_LISTE);

            assertNotNull(liste);
            assertEquals(Unterlagenstand.lade().alle().size(),
                    ((javafx.scene.layout.VBox) liste).getChildren().size(),
                    "Jede Unterlage gehoert auf den Schirm");
        });
    }
}
