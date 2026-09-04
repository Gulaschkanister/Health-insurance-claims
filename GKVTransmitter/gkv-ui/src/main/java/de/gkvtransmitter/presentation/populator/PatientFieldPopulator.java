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

    @Override
    protected void setEntityFieldValue(String fieldName, Patient patient, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        switch (fieldName) {
            case "firstname" -> patient.setFirstname(value);
            case "lastname" -> patient.setLastname(value);
            case "street" -> patient.setStreet(value);
            case "country" -> patient.setCountry(value);
            case "housenumber" -> patient.setHousenumber(value);
            case "plz" -> {
                try {
                    patient.setPlz(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "ik" -> {
                try {
                    patient.setIk(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "kassenIk" -> {
                try {
                    patient.setKassenIk(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "birthDate" -> {
                try {
                    LocalDate ld = LocalDate.parse(value);
                    patient.setBirthDate(ld);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public String getDisplayName(Patient patient) {
        return patient.getFirstname() + " " + patient.getLastname() + " (ID: " + patient.getId() + ")";
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
