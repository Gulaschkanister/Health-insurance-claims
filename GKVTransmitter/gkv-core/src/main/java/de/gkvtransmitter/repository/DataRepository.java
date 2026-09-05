package de.gkvtransmitter.repository;

import java.util.List;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;

public interface DataRepository {

    void savePatient(Patient patient);

    void saveServiceProvider(ServiceProvider serviceProvider);

    List<Patient> getAllPatients();

    List<ServiceProvider> getAllServiceProviders();

    List<PersonGroup> getAllPersonGroups();

    Patient getPatientById(int id);

    void deletePatient(Patient patient);

    void deleteServiceProvider(ServiceProvider serviceProvider);

    void savePersonGroup(PersonGroup personGroup);

    void deletePersonGroup(PersonGroup personGroup);

    void saveBlueprint(Blueprint blueprint);

    /**
     * Loescht eine Blaupause.
     *
     * <p>Fehlte bis zum 05.09.2026 als einzige der gefuehrten Entitaeten. Eine
     * einmal angelegte Blaupause liess sich weder ansehen noch berichtigen
     * noch entfernen - sie blieb als Name im Auswahlfeld der Abrechnung
     * stehen, einen Klick vom Versand entfernt.</p>
     */
    void deleteBlueprint(Blueprint blueprint);

    List<Blueprint> getAllBlueprints();

    long nextDtaInterchangeReference();
}
