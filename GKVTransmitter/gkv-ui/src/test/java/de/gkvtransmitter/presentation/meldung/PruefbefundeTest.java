package de.gkvtransmitter.presentation.meldung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.validator.ValidationReport;

/**
 * Prueft die Reihenfolge und Benennung der Beanstandungen.
 *
 * <p>Ohne JavaFX: die Aufbereitung liegt genau deshalb ausserhalb der
 * Meldungsklasse.</p>
 */
@DisplayName("Aufbereitung des Pruefberichts")
class PruefbefundeTest {

    @Test
    @DisplayName("Fehler stehen vor den Hinweisen")
    void fehlerZuerst() {
        List<Pruefbefunde.Zeile> zeilen = Pruefbefunde.zeilen(ValidationReport.builder()
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
        List<Pruefbefunde.Zeile> zeilen = Pruefbefunde.zeilen(ValidationReport.builder()
                .error("UNZ_ANZAHL", "UNZ (Zeile 18)", "Die Zahl stimmt nicht.")
                .error("IK", "", "Das IK ist ungueltig.")
                .build());

        assertEquals("UNZ (Zeile 18)", zeilen.get(0).ort());
        assertEquals("", zeilen.get(1).ort(), "Ohne Fundstelle darf nichts erfunden werden");
    }

    @Test
    @DisplayName("Ein Bericht ohne Beanstandungen liefert keine Zeilen")
    void ohneBefund() {
        assertTrue(Pruefbefunde.zeilen(ValidationReport.leer()).isEmpty());
        assertEquals("Keine Beanstandungen", Pruefbefunde.kurzfassung(ValidationReport.leer()));
    }

    @Test
    @DisplayName("Die Kurzfassung nennt Fehler und Hinweise getrennt")
    void kurzfassung() {
        String kurz = Pruefbefunde.kurzfassung(ValidationReport.builder()
                .error("A", "", "eins")
                .error("B", "", "zwei")
                .warning("C", "", "drei")
                .build());

        assertEquals("2 zu beheben, 1 Hinweis", kurz);
    }

    @Test
    @DisplayName("Mehrere Hinweise werden in der Mehrzahl genannt")
    void mehrzahl() {
        String kurz = Pruefbefunde.kurzfassung(ValidationReport.builder()
                .warning("C", "", "eins")
                .warning("D", "", "zwei")
                .build());

        assertEquals("2 Hinweise", kurz);
    }
}
