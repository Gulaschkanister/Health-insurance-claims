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

    @Override
    protected void setEntityFieldValue(String fieldName, ServiceProvider serviceProvider, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        switch (fieldName) {
            case "firstname" -> serviceProvider.setFirstname(value);
            case "lastname" -> serviceProvider.setLastname(value);
            case "street" -> serviceProvider.setStreet(value);
            case "country" -> serviceProvider.setCountry(value);
            case "housenumber" -> serviceProvider.setHousenumber(value);
            case "plz" -> {
                try {
                    serviceProvider.setPlz(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "ik" -> {
                try {
                    serviceProvider.setIk(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "kassenIk" -> {
                try {
                    serviceProvider.setKassenIk(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "birthDate" -> {
                try {
                    LocalDate ld = LocalDate.parse(value);
                    serviceProvider.setBirthDate(ld);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public String getDisplayName(ServiceProvider serviceProvider) {
        return serviceProvider.getFirstname() + " " + serviceProvider.getLastname() + " (ID: " + serviceProvider.getId() + ")";
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
