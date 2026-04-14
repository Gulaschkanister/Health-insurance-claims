package de.gkvtransmitter.presentation;

import java.util.HashSet;
import java.util.Set;

import de.gkvtransmitter.factory.Factory;

public class FactoryManager {
    private final Set<Factory> factories = new HashSet<>();

    public void addFactory(Factory factory) {
        factories.add(factory);
    }

    public Factory getSpecificFactory(String type) {
        return factories.stream()
                .filter(factory -> factory.getClass().getSimpleName().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }
}