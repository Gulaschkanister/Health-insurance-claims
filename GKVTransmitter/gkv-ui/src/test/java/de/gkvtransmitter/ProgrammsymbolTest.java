package de.gkvtransmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.presentation.JavaFxLaufzeit;
import javafx.scene.image.Image;

/**
 * Prueft das Programmsymbol.
 *
 * <p>Ein Symbol laesst sich nicht auf Schoenheit pruefen, wohl aber auf
 * Vorhandensein - und genau daran fehlte es: bis zum 05.09.2026 gab es gar
 * keines. Diese Tests halten fest, dass die Dateien mitkommen und lesbar sind.
 * Eine Ressource, die beim Verpacken herausfaellt, faellt sonst erst dem auf,
 * der das fertige Programm startet.</p>
 */
@DisplayName("Programmsymbol")
class ProgrammsymbolTest {

    @BeforeAll
    static void laufzeitHochfahren() {
        // Image gehoert zu JavaFX und braucht die Laufzeit.
        JavaFxLaufzeit.starten();
    }

    @Nested
    @DisplayName("Die Bilddateien")
    class DieBilddateien {

        @Test
        @DisplayName("sind fuer jede angekuendigte Groesse vorhanden und lesbar")
        void alleGroessenVorhanden() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                for (int groesse : Programmsymbol.GROESSEN) {
                    Image bild = Programmsymbol.lade(groesse);
                    assertNotNull(bild, "Es fehlt symbol-" + groesse + ".png");
                    assertFalse(bild.isError(), "symbol-" + groesse + ".png ist unlesbar");
                }
            });
        }

        @Test
        @DisplayName("haben die Kantenlaenge, die ihr Name verspricht")
        void kantenlaengeStimmt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                for (int groesse : Programmsymbol.GROESSEN) {
                    Image bild = Programmsymbol.lade(groesse);
                    assertEquals(groesse, (int) bild.getWidth(), "Breite von symbol-" + groesse);
                    assertEquals(groesse, (int) bild.getHeight(), "Hoehe von symbol-" + groesse);
                }
            });
        }

        @Test
        @DisplayName("enthalten eine kleine Groesse - die sieht man am haeufigsten")
        void enthaltenEineKleineGroesse() {
            boolean klein = false;
            for (int groesse : Programmsymbol.GROESSEN) {
                klein = klein || groesse <= 16;
            }
            assertTrue(klein, "Ohne 16 Pixel skaliert die Taskleiste selbst, und zwar schlecht");
        }
    }

    @Nested
    @DisplayName("Was die Anwendung ans Fenster haengt")
    class AmFenster {

        @Test
        @DisplayName("ist die vollstaendige Reihe, damit das System selbst waehlen kann")
        void alleWerdenGeliefert() {
            JavaFxLaufzeit.aufFxFaden(() ->
                    assertEquals(Programmsymbol.GROESSEN.length, Programmsymbol.alle().size()));
        }
    }

    @Nested
    @DisplayName("Die Datei fuer das Auslieferungspaket")
    class FuerDasPaket {

        /**
         * Prueft den Kopf der ICO-Datei.
         *
         * <p>{@code jpackage} bekommt sie ueber {@code --icon} in
         * {@code gkv-ui/pom.xml}. Faellt sie weg oder wird sie unbrauchbar,
         * merkt man das erst am fertigen Paket - und dafuer muss erst ein
         * vollstaendiger Paketlauf durch.</p>
         */
        @Test
        @DisplayName("ist vorhanden und traegt einen gueltigen ICO-Kopf")
        void icoVorhanden() throws Exception {
            try (InputStream quelle = getClass().getResourceAsStream("/symbol/symbol.ico")) {
                assertNotNull(quelle, "symbol.ico fehlt; jpackage bekaeme kein Symbol");
                byte[] kopf = quelle.readNBytes(6);

                assertEquals(6, kopf.length);
                assertEquals(0, kopf[0] + kopf[1], "Die ersten beiden Bytes sind reserviert und null");
                assertEquals(1, kopf[2], "Typ 1 kennzeichnet ein Symbol, nicht einen Mauszeiger");
                assertEquals(Programmsymbol.GROESSEN.length, kopf[4],
                        "Die ICO muss dieselben Groessen fuehren wie die PNG-Reihe");
            }
        }
    }
}
