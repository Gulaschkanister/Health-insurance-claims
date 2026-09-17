package de.gkvtransmitter.repository;

import java.util.List;
import java.util.Map;

import de.gkvtransmitter.entity.Betriebsdaten;
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

    /**
     * Die Angaben zur absendenden Stelle, oder {@code null}, solange keine
     * erfasst sind.
     *
     * <p>Es gibt hoechstens eine Zeile - siehe {@link Betriebsdaten}.</p>
     */
    Betriebsdaten ladeBetriebsdaten();

    /** Legt die Betriebsdaten an oder aendert die vorhandenen. */
    void speichereBetriebsdaten(Betriebsdaten betriebsdaten);

    /**
     * Alle gespeicherten Einstellungen, Schluessel auf Wert.
     *
     * <p>Alles auf einmal und nicht einzeln abgefragt: es sind eine Handvoll
     * Zeilen, und ein Zugriff je Einstellung waere ein Datenbankaufruf mitten
     * im Aufbau einer Maske.</p>
     */
    Map<String, String> ladeEinstellungen();

    /**
     * Speichert eine Einstellung. Ein leerer Wert loescht sie, damit wieder die
     * Vorgabe gilt.
     */
    void speichereEinstellung(String schluessel, String wert);
}
