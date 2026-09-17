package de.gkvtransmitter.entity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Seit wann und bis wann jemand Leistungen erbringt.
 *
 * <p>Die Frage "wird diese Person noch angeboten" wird <b>abgeleitet</b> und
 * nicht gespeichert. Ein eigenes Feld {@code aktiv} stand hier kurz und war die
 * falsche Antwort: Ein Kaestchen im Formular beginnt ungehakt, das Feld stand
 * auf {@code true}, und jeder neu angelegte Dienstleister waere sofort inaktiv
 * gewesen. Zwei Quellen fuer dieselbe Tatsache waren ohnehin eine zu viel.</p>
 */
@DisplayName("Der Taetigkeitszeitraum eines Dienstleisters")
class TaetigkeitszeitraumTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 9, 17);

    private static ServiceProvider erbringer() {
        return new ServiceProvider("Maria", "Hebamme", "Kurweg", "DE", "3",
                28203, 261914007, 101560000, LocalDate.of(1980, 1, 9));
    }

    /**
     * Ohne Angaben taetig - das ist der Bestand.
     *
     * <p>Jeder Dienstleister, der vor dem 17.09.2026 angelegt wurde, fuehrt
     * diese Felder nicht. Faende die Auswahl ihn nicht mehr, waere mit dem
     * Einbau des Zeitraums der ganze Bestand verschwunden.</p>
     */
    @Test
    @DisplayName("Ohne Angaben ist jemand taetig")
    void ohneAngabenTaetig() {
        assertTrue(erbringer().istAktivAm(HEUTE));
    }

    @Test
    @DisplayName("Vor dem Beginn nicht")
    void vorDemBeginn() {
        ServiceProvider erbringer = erbringer();
        erbringer.setTaetigSeit(HEUTE.plusDays(1));

        assertFalse(erbringer.istAktivAm(HEUTE));
    }

    @Test
    @DisplayName("Am Tag des Beginns schon")
    void amTagDesBeginns() {
        ServiceProvider erbringer = erbringer();
        erbringer.setTaetigSeit(HEUTE);

        assertTrue(erbringer.istAktivAm(HEUTE), "Der Beginn zaehlt mit");
    }

    /**
     * Der letzte Tag zaehlt mit.
     *
     * <p>"Taetig bis 17.09." heisst einschliesslich des 17., wie jeder das
     * liest. Ein Vertrag, der am Letzten endet, endet am Letzten.</p>
     */
    @Test
    @DisplayName("Am letzten Tag noch, am naechsten nicht mehr")
    void letzterTagZaehltMit() {
        ServiceProvider erbringer = erbringer();
        erbringer.setTaetigBis(HEUTE);

        assertTrue(erbringer.istAktivAm(HEUTE));
        assertFalse(erbringer.istAktivAm(HEUTE.plusDays(1)));
    }

    @Test
    @DisplayName("Innerhalb des Zeitraums taetig, ausserhalb nicht")
    void innerhalbUndAusserhalb() {
        ServiceProvider erbringer = erbringer();
        erbringer.setTaetigSeit(LocalDate.of(2020, 1, 1));
        erbringer.setTaetigBis(LocalDate.of(2026, 12, 31));

        assertTrue(erbringer.istAktivAm(HEUTE));
        assertFalse(erbringer.istAktivAm(LocalDate.of(2019, 12, 31)));
        assertFalse(erbringer.istAktivAm(LocalDate.of(2027, 1, 1)));
    }
}
