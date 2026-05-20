package de.gkvtransmitter.presentation.populator;

import java.time.LocalDate;

import de.gkvtransmitter.entity.Person;

/**
 * Generische Populierung für alle {@link Person}-Subtypen (Patient, ServiceProvider).
 *
 * Ersetzt die identische Duplikation in {@link PatientFieldPopulator} und
 * {@link ServiceProviderFieldPopulator} durch eine einzelne, wiederverwendbare
 * Implementierung über den gemeinsamen Basistyp {@code Person}.
 *
 * @param <T> Konkreter Person-Subtyp
 */
public class PersonFieldPopulator<T extends Person> extends EntityFieldPopulator<T> {

    @Override
    protected String getFieldValue(String fieldName, T person) {
        return switch (fieldName) {
            case "firstname" -> person.getFirstname();
            case "lastname" -> person.getLastname();
            case "street" -> person.getStreet();
            case "country" -> person.getCountry();
            case "housenumber" -> person.getHousenumber();
            case "plz" -> String.valueOf(person.getPlz());
            case "ik" -> String.valueOf(person.getIk());
            default -> "";
        };
    }

    @Override
    protected void setEntityFieldValue(String fieldName, T person, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        switch (fieldName) {
            case "firstname" -> person.setFirstname(value);
            case "lastname" -> person.setLastname(value);
            case "street" -> person.setStreet(value);
            case "country" -> person.setCountry(value);
            case "housenumber" -> person.setHousenumber(value);
            case "plz" -> {
                try {
                    person.setPlz(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "ik" -> {
                try {
                    person.setIk(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "birthDate" -> {
                try {
                    person.setBirthDate(LocalDate.parse(value));
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public String getDisplayName(T person) {
        return person.getFirstname() + " " + person.getLastname() + " (ID: " + person.getId() + ")";
    }

    @Override
    public Object getId(T person) {
        return person.getId();
    }

    @Override
    protected LocalDate getDateFieldValue(String fieldName, T person) {
        if (isDateField(fieldName)) {
            return person.getBirthDate();
        }
        return null;
    }
}
