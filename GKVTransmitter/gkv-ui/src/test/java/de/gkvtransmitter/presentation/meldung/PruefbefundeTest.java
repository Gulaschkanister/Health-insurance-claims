package de.gkvtransmitter.presentation.meldung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Prueft die Reihenfolge und Benennung der Beanstandungen.
 *
 * <p>Ohne JavaFX: die Aufbereitung liegt genau deshalb ausserhalb der
 * Meldungsklasse.</p>
 */
@DisplayName("Aufbereitung des Pruefberichts")
class PruefbefundeTest {

    /**
     * Die Texte kommen seit dem 18.09.2026 aus {@code ui-messages.json}.
     *
     * <p>Damit ist {@code Pruefbefunde} kein Sammelbecken statischer Methoden
     * mehr - dieser Bereich war der letzte, der eine eigene Sammlung deutscher
     * Woerter im Quelltext mitfuehrte.</p>
     */
    private final AppMessages texte = new AppMessages("/messages/ui-messages.json");

    private final Pruefbefunde befunde = new Pruefbefunde(texte);

    @Test
    @DisplayName("Fehler stehen vor den Hinweisen")
    void fehlerZuerst() {
        List<Pruefbefunde.Zeile> zeilen = befunde.zeilen(ValidationReport.builder()
                .warning("BETRAG", "", "Der Betrag ist auffaellig hoch.")
                .error("UNZ_ANZAHL", "UNZ", "Die Zahl der Nachrichten stimmt nicht.")
                .build());

        assertEquals(2, zeilen.size());
        assertTrue(zeilen.get(0).istFehler(), "Was den Versand aufgehalten hat, gehoert nach oben");
        assertEquals("Die Zahl der Nachrichten stimmt nicht.", zeilen.get(0).text());
        assertFalse(zeilen.get(1).istFehler());
    }

    @Test
    @DisplayName("Die Fundstelle wird mitgefuehrt, auch wenn es keine gibt")
    void fundstelle() {
        List<Pruefbefunde.Zeile> zeilen = befunde.zeilen(ValidationReport.builder()
                .error("UNZ_ANZAHL", "UNZ (Zeile 18)", "Die Zahl stimmt nicht.")
                .error("IK", "", "Das IK ist ungueltig.")
                .build());

        assertEquals("UNZ (Zeile 18)", zeilen.get(0).ort());
        assertEquals("", zeilen.get(1).ort(), "Ohne Fundstelle darf nichts erfunden werden");
    }

    @Test
    @DisplayName("Ein Bericht ohne Beanstandungen liefert keine Zeilen")
    void ohneBefund() {
        assertTrue(befunde.zeilen(ValidationReport.leer()).isEmpty());
        assertEquals("Keine Beanstandungen", befunde.kurzfassung(ValidationReport.leer()));
    }

    @Test
    @DisplayName("Die Kurzfassung nennt Fehler und Hinweise getrennt")
    void kurzfassung() {
        String kurz = befunde.kurzfassung(ValidationReport.builder()
                .error("A", "", "eins")
                .error("B", "", "zwei")
                .warning("C", "", "drei")
                .build());

        assertEquals("2 zu beheben, 1 Hinweis", kurz);
    }

    @Test
    @DisplayName("Mehrere Hinweise werden in der Mehrzahl genannt")
    void mehrzahl() {
        String kurz = befunde.kurzfassung(ValidationReport.builder()
                .warning("C", "", "eins")
                .warning("D", "", "zwei")
                .build());

        assertEquals("2 Hinweise", kurz);
    }

    /**
     * Ein Trockenlauf versendet nichts - und darf das nicht behaupten.
     *
     * <p><b>Genau das stand dort.</b> Der Bericht des Trockenlaufs trug die
     * Einleitung „Die Abrechnung wurde versendet."; versendet wurde nichts.
     * Aufgefallen ist es am Bildschirmfoto des Ablaufs, nicht in einem Test -
     * deshalb steht jetzt einer hier.</p>
     */
    @Nested
    @DisplayName("Die Einleitung")
    class DieEinleitung {

        private final ValidationReport hinweise = ValidationReport.builder()
                .warning("W", "UNB", "Ein Hinweis").build();
        private final ValidationReport fehler = ValidationReport.builder()
                .error("E", "ENF", "Ein Fehler").build();

        @Test
        @DisplayName("behauptet beim Trockenlauf keinen Versand")
        void trockenlaufBehauptetKeinenVersand() {
            String text = befunde.einleitung(hinweise, Pruefbefunde.Lage.TROCKENLAUF);

            assertFalse(text.contains("wurde versendet"), text);
            assertTrue(text.contains("Durchgespielt"), text);
        }

        @Test
        @DisplayName("sagt beim Trockenlauf auch im Fehlerfall, dass ohnehin nichts hinausging")
        void trockenlaufMitFehler() {
            assertTrue(befunde.einleitung(fehler, Pruefbefunde.Lage.TROCKENLAUF)
                    .contains("ohnehin nichts"));
        }

        @Test
        @DisplayName("bleibt beim Versand, wie sie war")
        void versandUnveraendert() {
            assertEquals("Die Abrechnung wurde versendet.", befunde.einleitung(hinweise));
            assertEquals("Die Abrechnung wurde nicht versendet.", befunde.einleitung(fehler));
        }
    }
}
