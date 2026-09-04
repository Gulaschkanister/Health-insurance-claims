package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.List;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.repository.DataRepository;

/**
 * Ein Datenbestand im Arbeitsspeicher.
 *
 * <p>Die Masken lesen nur; eine echte SQLite-Datei brauchte es dafuer nicht.
 * Fuer die Persistenz selbst gibt es eigene Tests in {@code gkv-core}, die
 * bewusst gegen eine Datei laufen.</p>
 */
class SpeicherRepository implements DataRepository {

    private final List<Patient> patienten = new ArrayList<>();
    private final List<ServiceProvider> dienstleister = new ArrayList<>();
    private final List<PersonGroup> gruppen = new ArrayList<>();
    private final List<Blueprint> blaupausen = new ArrayList<>();

    private final List<PersonGroup> gespeicherteGruppen = new ArrayList<>();

    private long naechsteReferenz = 1;

    /** Wird beim Speichern geworfen, wenn gesetzt. Fuer den Fehlerpfad. */
    private RuntimeException fehlerBeimSpeichern;

    SpeicherRepository mitGruppe(PersonGroup gruppe) {
        gruppen.add(gruppe);
        return this;
    }

    SpeicherRepository mitBlaupause(Blueprint blaupause) {
        blaupausen.add(blaupause);
        return this;
    }

    SpeicherRepository mitPatient(Patient patient) {
        patienten.add(patient);
        return this;
    }

    SpeicherRepository mitDienstleister(ServiceProvider dienstleister) {
        this.dienstleister.add(dienstleister);
        return this;
    }

    SpeicherRepository scheitertBeimSpeichern(RuntimeException fehler) {
        this.fehlerBeimSpeichern = fehler;
        return this;
    }

    /** Die Gruppen, die tatsaechlich zum Speichern uebergeben wurden. */
    List<PersonGroup> gespeicherteGruppen() {
        return List.copyOf(gespeicherteGruppen);
    }

    @Override
    public void savePatient(Patient patient) {
        if (fehlerBeimSpeichern != null) {
            throw fehlerBeimSpeichern;
        }
        patienten.add(patient);
    }

    @Override
    public void saveServiceProvider(ServiceProvider serviceProvider) {
        if (fehlerBeimSpeichern != null) {
            throw fehlerBeimSpeichern;
        }
        dienstleister.add(serviceProvider);
    }

    @Override
    public List<Patient> getAllPatients() {
        return List.copyOf(patienten);
    }

    @Override
    public List<ServiceProvider> getAllServiceProviders() {
        return List.copyOf(dienstleister);
    }

    @Override
    public List<PersonGroup> getAllPersonGroups() {
        return List.copyOf(gruppen);
    }

    @Override
    public Patient getPatientById(int id) {
        return patienten.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    @Override
    public void deletePatient(Patient patient) {
        patienten.remove(patient);
    }

    @Override
    public void deleteServiceProvider(ServiceProvider serviceProvider) {
        dienstleister.remove(serviceProvider);
    }

    @Override
    public void savePersonGroup(PersonGroup personGroup) {
        if (fehlerBeimSpeichern != null) {
            throw fehlerBeimSpeichern;
        }
        gespeicherteGruppen.add(personGroup);
        if (!gruppen.contains(personGroup)) {
            gruppen.add(personGroup);
        }
    }

    @Override
    public void deletePersonGroup(PersonGroup personGroup) {
        gruppen.remove(personGroup);
    }

    @Override
    public void saveBlueprint(Blueprint blueprint) {
        blaupausen.add(blueprint);
    }

    @Override
    public List<Blueprint> getAllBlueprints() {
        return List.copyOf(blaupausen);
    }

    @Override
    public long nextDtaInterchangeReference() {
        return naechsteReferenz++;
    }
}
