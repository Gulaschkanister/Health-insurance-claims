package de.gkvtransmitter.dta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueft den logischen Dateinamen.
 *
 * <p>Anhang 1 zur Anlage 1, Abschnitt 4.2 gibt elf Stellen vor:
 * {@code SL} + Stellen 3 bis 8 des Absender-IK + {@code S} oder {@code A} +
 * Abrechnungsmonat. Bis zum 07.09.2026 stand dort {@code HEB} plus Datum plus
 * laufende Nummer - richtig lang und sonst frei erfunden.</p>
 */
@DisplayName("Logischer Dateiname")
class LogischerDateinameTest {

    private static final LocalDate MAI = LocalDate.of(2026, 5, 30);

    @Test
    @DisplayName("Setzt sich aus Klassifikation, IK-Kern, Rolle und Monat zusammen")
    void aufbau() {
        assertEquals("SL191400S05", LogischerDateiname.bilde("261914007", true, MAI));
    }

    @Test
    @DisplayName("Eine Abrechnungsstelle setzt an neunter Stelle ein A")
    void abrechnungsstelle() {
        assertEquals("SL191400A05", LogischerDateiname.bilde("261914007", false, MAI));
    }

    @Test
    @DisplayName("Ist immer elf Stellen lang")
    void laenge() {
        assertEquals(LogischerDateiname.LAENGE,
                LogischerDateiname.bilde("108310400", true, LocalDate.of(2026, 12, 1)).length());
        assertEquals(LogischerDateiname.LAENGE,
                LogischerDateiname.bilde(null, true, MAI).length(),
                "Auch ohne IK darf der Name nicht kuerzer werden - sonst meldet die Kasse"
                        + " eine Feldlaenge statt der fehlenden Stammangabe");
    }

    @Test
    @DisplayName("Der Monat steht zweistellig")
    void monatZweistellig() {
        assertEquals("SL831040S01", LogischerDateiname.bilde("108310400", true, LocalDate.of(2026, 1, 9)));
    }

    @Test
    @DisplayName("Ein zu kurzes IK wird aufgefuellt, nicht abgeschnitten")
    void zuKurzesIk() {
        assertEquals("SL345000S05", LogischerDateiname.bilde("12345", true, MAI));
    }
}
