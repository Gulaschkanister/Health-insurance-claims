package de.gkvtransmitter.presentation;

import java.util.HashSet;
import java.util.Set;

import de.gkvtransmitter.factory.Factory;

/**
 * Zentrale Registry fuer Factory-Instanzen.
 *
 * Erlaubt das Hinterlegen und spaetere Aufloesen von Fabriken anhand ihres
 * Klassennamens.
 */
public class FactoryManager {

    private final Set<Factory> factories = new HashSet<>();

    /**
     * Registriert eine neue Factory in der Registry.
     */
    public void addFactory(Factory factory) {
        factories.add(factory);
    }

    /**
     * Sucht eine Factory ueber den (einfachen) Klassennamen.
     */
    public Factory getSpecificFactory(String type) {
        return factories.stream()
                .filter(factory -> factory.getClass().getSimpleName().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }
}
