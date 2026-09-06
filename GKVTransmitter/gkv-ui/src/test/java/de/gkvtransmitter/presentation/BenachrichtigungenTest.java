package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.presentation.Benachrichtigungen.Art;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Prueft die Meldungsecke.
 *
 * <p>Die Anzeigedauer ist in den Tests hoch gesetzt: geprueft wird, was in der
 * Ecke steht, nicht wie schnell es wieder verschwindet. Eine Pruefung gegen
 * die Uhr waere von der Auslastung des Rechners abhaengig und damit
 * unzuverlaessig.</p>
 */
@DisplayName("Meldungsecke")
class BenachrichtigungenTest {

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @Test
    @DisplayName("Eine Meldung erscheint mit ihrem Text")
    void meldungErscheint() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Benachrichtigungen ecke = neueEcke();
            ecke.zeige(Art.ERFOLG, "Anna Muster gespeichert");

            assertEquals(List.of("Anna Muster gespeichert"), ecke.offene());
            assertTrue(enthaeltText(ecke, "Anna Muster gespeichert"),
                    "Der Text muss auch im Knotenbaum stehen, nicht nur in der Liste");
        });
    }

    @Test
    @DisplayName("Mehrere Meldungen stehen untereinander, aelteste zuerst")
    void mehrereMeldungen() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Benachrichtigungen ecke = neueEcke();
            ecke.zeige(Art.ERFOLG, "eins");
            ecke.zeige(Art.HINWEIS, "zwei");

            assertEquals(List.of("eins", "zwei"), ecke.offene());
        });
    }

    @Test
    @DisplayName("Ab der fuenften Meldung weicht die aelteste")
    void hoechstzahl() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Benachrichtigungen ecke = neueEcke();
            for (int i = 1; i <= 6; i++) {
                ecke.zeige(Art.HINWEIS, "Meldung " + i);
            }

            assertEquals(List.of("Meldung 3", "Meldung 4", "Meldung 5", "Meldung 6"), ecke.offene(),
                    "Die Ecke darf nicht den halben Bildschirm zustellen");
        });
    }

    @Test
    @DisplayName("Eine bleibende Meldung laesst sich einzeln wegnehmen")
    void einzelnWegnehmen() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Benachrichtigungen ecke = neueEcke();
            ecke.zeige(Art.FEHLER, "eins");
            javafx.scene.Node zweite = ecke.zeigeBleibend(Art.FEHLER, "Fehler", "zwei", null);

            ecke.entferne(zweite);

            assertEquals(List.of("eins"), ecke.offene());
        });
    }

    @Test
    @DisplayName("Leeren nimmt alles weg")
    void leeren() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Benachrichtigungen ecke = neueEcke();
            ecke.zeige(Art.FEHLER, "eins");
            ecke.zeige(Art.FEHLER, "zwei");

            ecke.leeren();

            assertTrue(ecke.offene().isEmpty());
        });
    }

    @Test
    @DisplayName("Die Ecke faengt keine Klicks ab, wo keine Meldung steht")
    void durchlaessig() {
        JavaFxLaufzeit.aufFxFaden(() ->
                assertFalse(neueEcke().bereich().isPickOnBounds(),
                        "Sonst waere die Maske unter der Ecke nicht mehr bedienbar"));
    }

    private static Benachrichtigungen neueEcke() {
        return new Benachrichtigungen(Duration.minutes(5));
    }

    private static boolean enthaeltText(Benachrichtigungen ecke, String gesucht) {
        return ecke.bereich().lookupAll(".meldung-text").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(beschriftung -> gesucht.equals(beschriftung.getText()));
    }
}
