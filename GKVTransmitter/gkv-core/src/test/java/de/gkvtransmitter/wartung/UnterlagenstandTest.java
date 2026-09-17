package de.gkvtransmitter.wartung;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Der Stand der verbindlichen Unterlagen.
 *
 * <p>Diese Tests gaebe es nicht, wenn das Veralten aufgefallen waere: Die
 * Anlagen unter {@code Information/} waren am 07.09.2026 vom Juli - zwei
 * Monate ueberholt, und niemand hatte es bemerkt.</p>
 *
 * <p>Geprueft wird deshalb <b>an der mitgelieferten Liste</b> und nicht an
 * einer erfundenen: Wenn dort etwas Falsches steht, soll es hier auffallen.</p>
 */
@DisplayName("Der Stand der Unterlagen")
class UnterlagenstandTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 9, 17);

    private final Unterlagenstand stand = Unterlagenstand.lade();

    @Test
    @DisplayName("ist nicht leer - sonst zeigt die Maske nichts")
    void istNichtLeer() {
        assertFalse(stand.alle().isEmpty(), "Ohne Liste weiss niemand, worauf die Anwendung steht");
    }

    /**
     * Der Befund, den G1 sofort erbringt.
     *
     * <p>Anlage 1 und Anlage 3 in der Version 22 liegen vor und sind <b>ab dem
     * 01.02.2027 anzuwenden</b>. Das ist kein Fehler, solange das Datum nicht
     * erreicht ist - aber es ist ein Termin, den heute niemand kennt.</p>
     */
    @Test
    @DisplayName("kennt den naechsten Termin")
    void kenntDenNaechstenTermin() {
        Unterlage naechste = stand.naechsterTermin(HEUTE).orElseThrow();

        assertEquals(LocalDate.of(2027, 2, 1), naechste.anzuwendenAb(),
                "Version 22 ist ab dem 01.02.2027 anzuwenden");
    }

    /**
     * Die Fassung, nach der gesendet wird, gilt heute.
     *
     * <p>Anlage 1 Version 21: anzuwenden ab 01.10.2025, Gueltigkeit bis zum
     * 30.04.2027. Der Tag steht auf dem Deckblatt der Version 22 - "Version 21
     * verliert Gueltigkeit: 30.04.2027".</p>
     */
    @Test
    @DisplayName("weist Version 21 heute als geltend aus")
    void version21GiltHeute() {
        Unterlage v21 = stand.alle().stream()
                .filter(unterlage -> "anlage-1-v21".equals(unterlage.kennung()))
                .findFirst().orElseThrow();

        assertTrue(v21.giltAm(HEUTE));
        assertFalse(v21.giltAm(LocalDate.of(2027, 5, 1)), "Ab dem 01.05.2027 nicht mehr");
        assertTrue(v21.abgelaufen(LocalDate.of(2027, 5, 1)));
    }

    @Test
    @DisplayName("meldet heute keine abgelaufene Fassung")
    void heuteNichtsAbgelaufen() {
        assertTrue(stand.abgelaufene(HEUTE).isEmpty(), "" + stand.abgelaufene(HEUTE));
    }

    /**
     * Am 01.05.2027 faellt Version 21 heraus.
     *
     * <p>Genau der Fall, den niemand bemerkt: Die Anwendung sendet weiter, die
     * Kasse weist zurueck, und die Ursache steht auf einem Deckblatt.</p>
     */
    @Test
    @DisplayName("meldet Version 21 nach dem 30.04.2027 als abgelaufen")
    void nachDemStichtag() {
        List<Unterlage> abgelaufen = stand.abgelaufene(LocalDate.of(2027, 5, 1));

        assertFalse(abgelaufen.isEmpty(), "Der Stichtag steht auf dem Deckblatt der Version 22");
        assertTrue(abgelaufen.stream().anyMatch(u -> "anlage-1-v21".equals(u.kennung())));
    }

    /**
     * Eine fehlende Liste haelt nichts auf.
     *
     * <p>Der Stand ist eine Auskunft, keine Voraussetzung. Waere er es, koennte
     * eine kaputte JSON-Datei die Abrechnung aufhalten.</p>
     */
    @Test
    @DisplayName("kommt ohne Datei aus, statt den Start zu verhindern")
    void ohneDatei() {
        Unterlagenstand leer = Unterlagenstand.lade("/gibt/es/nicht.json");

        assertTrue(leer.alle().isEmpty());
        assertTrue(leer.naechsterTermin(HEUTE).isEmpty());
    }
}
