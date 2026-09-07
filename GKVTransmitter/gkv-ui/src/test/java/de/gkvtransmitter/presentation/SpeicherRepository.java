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
    private final List<Patient> gespeichertePatienten = new ArrayList<>();

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

    /**
     * Die Teilnehmer, die tatsaechlich zum Speichern uebergeben wurden.
     *
     * <p>Nicht dasselbe wie {@link #getAllPatients()}: wer einen vorhandenen
     * Teilnehmer bearbeitet, uebergibt ihn erneut, und dann stuende er dort
     * zweimal. Hier steht, was der Speicherweg wirklich erreicht hat.</p>
     */
    List<Patient> gespeichertePatienten() {
        return List.copyOf(gespeichertePatienten);
    }

    @Override
    public void savePatient(Patient patient) {
        if (fehlerBeimSpeichern != null) {
            throw fehlerBeimSpeichern;
        }
        gespeichertePatienten.add(patient);
        if (!patienten.contains(patient)) {
            patienten.add(patient);
        }
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
        if (fehlerBeimSpeichern != null) {
            throw fehlerBeimSpeichern;
        }
        blaupausen.remove(blueprint);
        blaupausen.add(blueprint);
    }

    @Override
    public void deleteBlueprint(Blueprint blueprint) {
        blaupausen.remove(blueprint);
    }

    @Override
    public List<Blueprint> getAllBlueprints() {
        return List.copyOf(blaupausen);
    }

    @Override
    public long nextDtaInterchangeReference() {
        return naechsteReferenz++;
    }

    /** Die Einstellungen, wie sie in der Tabelle {@code einstellung} laegen. */
    private final java.util.Map<String, String> einstellungen = new java.util.LinkedHashMap<>();

    /** Ob das Speichern einer Einstellung fehlschlagen soll. */
    private boolean einstellungSchreibenScheitert;

    /**
     * Laesst jedes weitere Speichern einer Einstellung fehlschlagen.
     *
     * <p>Ein Ersatz fuer den Fall, den man an einer echten Datenbank nur mit
     * Muehe herstellt - gesperrte Datei, volle Platte. Was geprueft werden
     * soll, ist nicht der Grund, sondern die Reaktion: die Wahl gilt trotzdem,
     * und die Meldungsecke sagt, dass sie die Sitzung nicht ueberlebt.</p>
     */
    void lassSpeichernScheitern() {
        einstellungSchreibenScheitert = true;
    }

    @Override
    public java.util.Map<String, String> ladeEinstellungen() {
        return java.util.Map.copyOf(einstellungen);
    }

    @Override
    public void speichereEinstellung(String schluessel, String wert) {
        if (einstellungSchreibenScheitert) {
            throw new IllegalStateException("Speichern nicht moeglich");
        }
        if (wert == null || wert.isBlank()) {
            einstellungen.remove(schluessel);
        } else {
            einstellungen.put(schluessel, wert.trim());
        }
    }
}
