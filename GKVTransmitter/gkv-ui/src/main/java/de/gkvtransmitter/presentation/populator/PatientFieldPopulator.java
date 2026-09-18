package de.gkvtransmitter.presentation.populator;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import de.gkvtransmitter.entity.Patient;

/**
 * Konkrete Implementierung für Patient-Entities.
 *
 * Behandelt die Populierung und das Extrahieren von Patientendaten.
 */
public class PatientFieldPopulator extends EntityFieldPopulator<Patient> {

    /**
     * Woher der Wert eines Feldes kommt.
     *
     * <p>Eine Tabelle statt einer Verzweigung - dieselbe Umstellung wie bei
     * {@link ServiceProviderFieldPopulator} und aus demselben Grund: Ein
     * {@code switch} ueber Feldnamen waechst in die Tiefe, eine Tabelle in die
     * Breite. Checkstyle mass hier zyklomatische Komplexitaet 11 gegen eine
     * Obergrenze von 10.</p>
     */
    private static final Map<String, Function<Patient, String>> LESER = Map.ofEntries(
            Map.entry("firstname", Patient::getFirstname),
            Map.entry("lastname", Patient::getLastname),
            Map.entry("street", Patient::getStreet),
            Map.entry("country", Patient::getCountry),
            Map.entry("housenumber", Patient::getHousenumber),
            Map.entry("plz", person -> String.valueOf(person.getPlz())),
            Map.entry("ik", person -> String.valueOf(person.getIk())),
            Map.entry("kassenIk", person -> String.valueOf(person.getKassenIk())));

    @Override
    protected String getFieldValue(String fieldName, Patient patient) {
        return LESER.getOrDefault(fieldName, person -> "").apply(patient);
    }

    /**
     * Übernimmt einen Feldwert — <b>auch einen geleerten</b>.
     *
     * <p>Hier stand bis zum 06.09.2026 ein {@code if (value.isBlank()) return;}
     * ganz vorn. Ein geleertes Feld wurde damit still übergangen, und die Maske
     * meldete trotzdem Erfolg: eine falsch eingetragene Straße ließ sich
     * ändern, aber nicht entfernen.</p>
     *
     * <p>Schlimmer war die zweite Wirkung. {@code PersonenMaske} prüft vor dem
     * Speichern an der fertigen Person, ob Vorname, Nachname, Geburtsdatum und
     * beide IK stimmen. Wer eines davon leerte, umging diese Prüfung — sie sah
     * den alten Wert und ließ durch, was auf dem Bildschirm längst leer war.
     * <b>Der Leerwert muss ankommen, damit das Tor greift.</b></p>
     *
     * <p>Zahlen werden dabei zu 0 und das Datum zu {@code null} — beides sind
     * die Werte, die {@link de.gkvtransmitter.util.Institutionskennzeichen}
     * beziehungsweise die Prüfung als „fehlt" erkennt.</p>
     */
    /** Wohin der Wert eines Feldes geht; siehe {@link #setEntityFieldValue}. */
    private static final Map<String, BiConsumer<Patient, String>> SCHREIBER = Map.ofEntries(
            Map.entry("firstname", Patient::setFirstname),
            Map.entry("lastname", Patient::setLastname),
            Map.entry("street", Patient::setStreet),
            Map.entry("country", Patient::setCountry),
            Map.entry("housenumber", Patient::setHousenumber),
            Map.entry("plz", (person, text) -> person.setPlz(zahl(text, person.getPlz()))),
            Map.entry("ik", (person, text) -> person.setIk(zahl(text, person.getIk()))),
            Map.entry("kassenIk", (person, text) -> person.setKassenIk(zahl(text, person.getKassenIk()))),
            Map.entry("birthDate", (person, text) -> person.setBirthDate(datum(text))));

    @Override
    protected void setEntityFieldValue(String fieldName, Patient patient, String value) {
        SCHREIBER.getOrDefault(fieldName, (person, text) -> { })
                .accept(patient, value == null ? "" : value.trim());
    }

    @Override
    public String getDisplayName(Patient patient) {
        return getPlainName(patient) + " (ID: " + patient.getId() + ")";
    }

    @Override
    public String getPlainName(Patient patient) {
        return (patient.getFirstname() + " " + patient.getLastname()).strip();
    }

    @Override
    public Object getId(Patient patient) {
        return patient.getId();
    }

    @Override
    protected LocalDate getDateFieldValue(String fieldName, Patient patient) {
        if (isDateField(fieldName)) {
            try {
                return patient.getBirthDate();
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
