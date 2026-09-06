package de.gkvtransmitter.presentation.populator;

import java.time.LocalDate;

import de.gkvtransmitter.entity.Patient;

/**
 * Konkrete Implementierung für Patient-Entities.
 *
 * Behandelt die Populierung und das Extrahieren von Patientendaten.
 */
public class PatientFieldPopulator extends EntityFieldPopulator<Patient> {

    @Override
    protected String getFieldValue(String fieldName, Patient patient) {
        return switch (fieldName) {
            case "firstname" -> patient.getFirstname();
            case "lastname" -> patient.getLastname();
            case "street" -> patient.getStreet();
            case "country" -> patient.getCountry();
            case "housenumber" -> patient.getHousenumber();
            case "plz" -> String.valueOf(patient.getPlz());
            case "ik" -> String.valueOf(patient.getIk());
            case "kassenIk" -> String.valueOf(patient.getKassenIk());
            default -> "";
        };
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
    @Override
    protected void setEntityFieldValue(String fieldName, Patient patient, String value) {
        String text = value == null ? "" : value.trim();

        switch (fieldName) {
            case "firstname" -> patient.setFirstname(text);
            case "lastname" -> patient.setLastname(text);
            case "street" -> patient.setStreet(text);
            case "country" -> patient.setCountry(text);
            case "housenumber" -> patient.setHousenumber(text);
            case "plz" -> patient.setPlz(zahl(text, patient.getPlz()));
            case "ik" -> patient.setIk(zahl(text, patient.getIk()));
            case "kassenIk" -> patient.setKassenIk(zahl(text, patient.getKassenIk()));
            case "birthDate" -> patient.setBirthDate(datum(text));
            default -> { }
        }
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
