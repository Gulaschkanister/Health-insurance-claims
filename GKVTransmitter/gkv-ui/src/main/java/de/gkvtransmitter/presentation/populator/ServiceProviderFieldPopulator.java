package de.gkvtransmitter.presentation.populator;

import java.time.LocalDate;

import de.gkvtransmitter.entity.ServiceProvider;

/**
 * Konkrete Implementierung für ServiceProvider-Entities.
 *
 * Behandelt die Populierung und das Extrahieren von ServiceProvider-Daten.
 */
public class ServiceProviderFieldPopulator extends EntityFieldPopulator<ServiceProvider> {

    @Override
    protected String getFieldValue(String fieldName, ServiceProvider serviceProvider) {
        return switch (fieldName) {
            case "firstname" -> serviceProvider.getFirstname();
            case "lastname" -> serviceProvider.getLastname();
            case "street" -> serviceProvider.getStreet();
            case "country" -> serviceProvider.getCountry();
            case "housenumber" -> serviceProvider.getHousenumber();
            case "plz" -> String.valueOf(serviceProvider.getPlz());
            case "ik" -> String.valueOf(serviceProvider.getIk());
            case "kassenIk" -> String.valueOf(serviceProvider.getKassenIk());
            default -> "";
        };
    }

    /**
     * Übernimmt einen Feldwert — <b>auch einen geleerten</b>.
     *
     * <p>Siehe {@link PatientFieldPopulator#setEntityFieldValue}: der
     * vorangestellte {@code isBlank}-Ausstieg übersprang ein geleertes Feld
     * still und umging damit die Prüfung vor dem Speichern.</p>
     */
    @Override
    protected void setEntityFieldValue(String fieldName, ServiceProvider serviceProvider, String value) {
        String text = value == null ? "" : value.trim();

        switch (fieldName) {
            case "firstname" -> serviceProvider.setFirstname(text);
            case "lastname" -> serviceProvider.setLastname(text);
            case "street" -> serviceProvider.setStreet(text);
            case "country" -> serviceProvider.setCountry(text);
            case "housenumber" -> serviceProvider.setHousenumber(text);
            case "plz" -> serviceProvider.setPlz(zahl(text, serviceProvider.getPlz()));
            case "ik" -> serviceProvider.setIk(zahl(text, serviceProvider.getIk()));
            case "kassenIk" -> serviceProvider.setKassenIk(zahl(text, serviceProvider.getKassenIk()));
            case "birthDate" -> serviceProvider.setBirthDate(datum(text));
            default -> { }
        }
    }

    @Override
    public String getDisplayName(ServiceProvider serviceProvider) {
        return getPlainName(serviceProvider) + " (ID: " + serviceProvider.getId() + ")";
    }

    @Override
    public String getPlainName(ServiceProvider serviceProvider) {
        return (serviceProvider.getFirstname() + " " + serviceProvider.getLastname()).strip();
    }

    @Override
    public Object getId(ServiceProvider serviceProvider) {
        return serviceProvider.getId();
    }

    @Override
    protected LocalDate getDateFieldValue(String fieldName, ServiceProvider serviceProvider) {
        if (isDateField(fieldName)) {
            try {
                return serviceProvider.getBirthDate();
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
