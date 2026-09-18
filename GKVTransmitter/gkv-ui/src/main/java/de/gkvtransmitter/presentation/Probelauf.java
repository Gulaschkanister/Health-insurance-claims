package de.gkvtransmitter.presentation;

import java.util.List;
import java.util.Map;

import de.gkvtransmitter.dispatch.Trockenlauf;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;

/**
 * Was die Abrechnungsmaske zum Durchspielen braucht.
 *
 * <p>Wie {@link Abrechnungslauf} eine Signatur statt eines Dienstes: Die Maske
 * uebergibt {@code abrechnungService::probelauf}, der Test eine Lambda.
 * Auffaellig ist, was <b>fehlt</b> - kein Zielordner. Ein Trockenlauf sucht
 * sich seinen eigenen und hinterlaesst nichts, wo etwas hingehoert.</p>
 */
@FunctionalInterface
public interface Probelauf {

    /**
     * Spielt die Abrechnungen durch, ohne zu senden.
     *
     * <p>Wirft nicht: Eine nicht bestandene Pruefung ist hier kein Fehlschlag,
     * sondern das Ergebnis.</p>
     */
    Trockenlauf.Ergebnis spiele(List<Patient> teilnehmer, PersonGroup gruppe, Blueprint blaupause,
            Map<Integer, Integer> termine);
}
