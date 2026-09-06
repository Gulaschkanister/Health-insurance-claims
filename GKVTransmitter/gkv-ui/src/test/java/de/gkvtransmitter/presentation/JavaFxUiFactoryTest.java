package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Spinner;

/**
 * Prueft die Fabrik - genauer: die eine Entscheidung, die sie trifft.
 *
 * <p>Die Uebergabe fuehrte sie lange als „reine Fabrik ohne eigene
 * Entscheidungen, am ehesten verzichtbar". Das galt bis zum 05.09.2026. Seither
 * steckt hier die <b>Uebernahme beim Fokusverlust</b>, und genau die hatte
 * einen Fehler mit Folgen: „Termine fuer alle: 8" und dann „Setzen" ergab acht
 * Zeilen mit <em>einem</em> Termin. Auf dem Bildschirm stand die ganze Zeit die
 * 8, und die Rechnung an die Kasse lautete auf ein Achtel.</p>
 *
 * <p>Kein Test fand das, weil alle den Wert ueber
 * {@code getValueFactory().setValue(...)} setzten und damit am Editor vorbei -
 * also gerade nicht den Weg gingen, den jemand am Bildschirm geht. Diese Tests
 * tippen in den Editor.</p>
 */
@DisplayName("JavaFxUiFactory")
class JavaFxUiFactoryTest {

    private static final JavaFxUiFactory FABRIK = new JavaFxUiFactory();

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @Nested
    @DisplayName("Ein Zaehler")
    class Zaehler {

        @Test
        @DisplayName("uebernimmt getippten Text, sobald er den Fokus verliert")
        void uebernimmtBeimVerlassen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Spinner<Integer> zaehler = FABRIK.createSpinner(Integer.class);
                // Ohne Fenster wechselt kein Fokus. Der Weg dorthin ist
                // derselbe, den der Horcher nimmt: uebernimm liest den Editor
                // und setzt den Wert.
                zaehler.getEditor().setText("8");
                assertEquals(0, zaehler.getValue(),
                        "Vor der Uebernahme steht noch der alte Wert - das war der Fehler");

                JavaFxUiFactory.uebernimm(zaehler);

                assertEquals(8, zaehler.getValue());
            });
        }

        @Test
        @DisplayName("faellt bei unlesbarem Text auf seinen letzten gueltigen Wert zurueck")
        void unlesbarerText() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Spinner<Integer> zaehler = FABRIK.createSpinner(Integer.class);
                zaehler.getValueFactory().setValue(3);

                zaehler.getEditor().setText("acht");
                JavaFxUiFactory.uebernimm(zaehler);

                assertEquals(3, zaehler.getValue(),
                        "Ein Vertipper darf keine Ausnahme werfen und keine Null ergeben");
                assertEquals("3", zaehler.getEditor().getText(),
                        "Und im Feld muss stehen, womit wirklich gerechnet wird");
            });
        }

        @Test
        @DisplayName("ist beschreibbar - sonst gaebe es das Problem gar nicht")
        void istBeschreibbar() {
            JavaFxLaufzeit.aufFxFaden(() ->
                    assertTrue(FABRIK.createSpinner(Integer.class).isEditable()));
        }

        @Test
        @DisplayName("gibt es auch fuer Betraege, mit Schritten von einem Cent")
        void fuerBetraege() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Spinner<BigDecimal> zaehler = FABRIK.createSpinner(BigDecimal.class);

                assertEquals(BigDecimal.ZERO, zaehler.getValue());
                zaehler.increment(1);
                assertEquals(new BigDecimal("0.01"), zaehler.getValue());
                zaehler.decrement(2);
                assertEquals(new BigDecimal("-0.01"), zaehler.getValue());
            });
        }

        @Test
        @DisplayName("gibt es nicht fuer einen Typ, den die Fabrik nicht kennt")
        void unbekannterTyp() {
            assertThrows(IllegalArgumentException.class,
                    () -> FABRIK.createSpinner(String.class),
                    "Lieber ein Fehler beim Bauen als ein Feld, das nichts tut");
        }
    }
}
