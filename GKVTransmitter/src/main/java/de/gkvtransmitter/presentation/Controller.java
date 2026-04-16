package de.gkvtransmitter.presentation;

import de.gkvtransmitter.bootstrap.ApplicationBootstrap;
import de.gkvtransmitter.definition.GlobalDefinitions;

public class Controller {
    private final GlobalDefinitions globalDefinitions;
    private final FactoryManager factoryManager;
    private final ApplicationBootstrap bootstrap;

    public Controller() {
        try {
            // Singleton mit globalen, zur Laufzeit geladenen Strukturdefinitionen.
            globalDefinitions = GlobalDefinitions.getInstance();
            // Registry fuer Fabriken (u.a. JSON-Parser-Factory).
            factoryManager = new FactoryManager();
            // Fuehrt Initialladungen aus und registriert die Profile in GlobalDefinitions.
            bootstrap = new ApplicationBootstrap(globalDefinitions, factoryManager);
            initialize();
        } catch (IllegalArgumentException e) {
            System.err.println("Controller konnte nicht initialisiert werden: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("App-Initialisierung fehlgeschlagen", e);
        } catch (RuntimeException e) {
            System.err.println("Controller konnte nicht initialisiert werden: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("App-Initialisierung fehlgeschlagen", e);
        }
    }

    private void initialize() throws RuntimeException {
        // Von hier aus werden die JSON-Profile gelesen und gespeichert.
        try {
            bootstrap.initialize();
        } catch (Exception e) {
            System.err.println("❌ Bootstrap-Initialisierung fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("JSON-Profile konnten nicht geladen werden", e);
        }
    }

    public GlobalDefinitions getGlobalDefinitions() {
        return globalDefinitions;
    }

    public FactoryManager getFactoryManager() {
        return factoryManager;
    }
}