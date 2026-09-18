package de.gkvtransmitter.presentation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.gkvtransmitter.dispatch.Versandergebnis;
import de.gkvtransmitter.dta.Verarbeitungskennzeichen;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.Protokolleintrag;

/**
 * Was die Abrechnungsmaske zum Korrigieren braucht.
 *
 * <p>Wie {@link Abrechnungslauf} eine Signatur statt eines Dienstes. Der
 * Unterschied sind die beiden letzten Werte: <b>wofuer</b> die Rechnung steht
 * und <b>worauf</b> sie sich bezieht - beides verlangt Anlage 1, Abschnitt 7.3
 * zwingend, sobald das Verarbeitungskennzeichen nicht {@code 01} ist.</p>
 */
@FunctionalInterface
public interface Korrekturlauf {

    /**
     * Erzeugt und versendet die Abrechnungen als Korrektur.
     *
     * @throws IllegalArgumentException wenn sich auf die Ursprungslieferung
     *         keine Korrektur beziehen laesst - Anlage 1, Abschnitt 7.2
     */
    Versandergebnis starte(List<Patient> teilnehmer, PersonGroup gruppe, Blueprint blaupause,
            Map<Integer, Integer> termine, Path zielordner,
            Verarbeitungskennzeichen art, Protokolleintrag bezug);
}
