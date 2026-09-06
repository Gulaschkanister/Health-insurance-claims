package de.gkvtransmitter.presentation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.gkvtransmitter.dispatch.DispatchBatch;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;

/**
 * Was die Abrechnungsmaske vom Fachdienst braucht: einen Lauf anstossen.
 *
 * <p>Die Maske haengt damit an einer Signatur statt an
 * {@code AbrechnungService}. Der Dienst ist {@code final} und erzeugt in
 * seinem Standardkonstruktor die gesamte Versandkette; im Test liesse er sich
 * weder ersetzen noch gefahrlos aufrufen. Die Oberflaeche uebergibt einfach
 * {@code abrechnungService::createAndDispatch}, der Test eine Lambda.</p>
 */
@FunctionalInterface
public interface Abrechnungslauf {

    /**
     * Erzeugt und versendet die Abrechnungen.
     *
     * @throws de.gkvtransmitter.dispatch.DtaValidierungsException wenn die
     *         Pruefung nicht bestanden wurde; dann wurde nichts versendet
     */
    List<DispatchBatch> starte(List<Patient> teilnehmer, PersonGroup gruppe, Blueprint blaupause,
            Map<Integer, Integer> termine, Path zielordner);
}
