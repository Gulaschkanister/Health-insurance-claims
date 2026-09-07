package de.gkvtransmitter.dta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueft den Leistungsbereich im UNB.
 *
 * <p>Das sechste Element des UNB ist kein Freitext, sondern der
 * Sammelgruppenschluessel aus Anlage 3, Abschnitt 8.1.14. Bis zum 07.09.2026
 * stand dort fest ein {@code H} - der Bereich Rehabilitationssport, uebernommen
 * aus der Beispieldatei und fuer eine Hebamme falsch.</p>
 */
@DisplayName("Leistungsbereich")
class LeistungsbereichTest {

    @Test
    @DisplayName("Der Abrechnungscode 50 gehoert zum Bereich F (Hebammen)")
    void hebamme() {
        assertEquals("F", Leistungsbereich.zuAbrechnungscode("50"));
    }

    @Test
    @DisplayName("Der Abrechnungscode 61 gehoert zum Bereich H - das war der alte Festwert")
    void rehabilitationssport() {
        assertEquals("H", Leistungsbereich.zuAbrechnungscode("61"),
                "Der alte Festwert war nicht zufaellig, sondern der Bereich der Beispieldatei");
    }

    @Test
    @DisplayName("Heilmittel fallen zusammen in den Bereich B")
    void heilmittel() {
        assertEquals("B", Leistungsbereich.zuAbrechnungscode("22"));
        assertEquals("B", Leistungsbereich.zuAbrechnungscode("71"));
    }

    @Test
    @DisplayName("Buchstabenschluessel der ausserklinischen Intensivpflege werden gelesen")
    void buchstabenschluessel() {
        assertEquals("R", Leistungsbereich.zuAbrechnungscode("A1"));
        assertEquals("R", Leistungsbereich.zuAbrechnungscode("a1"),
                "Ein kleingeschriebener Schluessel ist derselbe Schluessel");
    }

    @Test
    @DisplayName("Ein unbekannter Code faellt auf J und nicht auf einen fremden Bereich")
    void unbekannt() {
        assertEquals(Leistungsbereich.UNBEKANNT, Leistungsbereich.zuAbrechnungscode("99"));
        assertEquals(Leistungsbereich.UNBEKANNT, Leistungsbereich.zuAbrechnungscode(null));
        assertEquals(Leistungsbereich.UNBEKANNT, Leistungsbereich.zuAbrechnungscode(""));
    }
}
