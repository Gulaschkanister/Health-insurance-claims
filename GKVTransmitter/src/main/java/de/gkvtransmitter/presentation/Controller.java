package de.gkvtransmitter.presentation;

import de.gkvtransmitter.bootstrap.ApplicationBootstrap;
import de.gkvtransmitter.definition.GlobalDefinitions;

public class Controller {
    private final GlobalDefinitions globalDefinitions;
    private final FactoryManager factoryManager;
    private final ApplicationBootstrap bootstrap;

    public Controller() {
        // Singleton mit globalen, zur Laufzeit geladenen Strukturdefinitionen.
        this.globalDefinitions = GlobalDefinitions.getInstance();
        // Registry fuer Fabriken (u.a. JSON-Parser-Factory).
        this.factoryManager = new FactoryManager();
        // Fuehrt Initialladungen aus und registriert die Profile in GlobalDefinitions.
        this.bootstrap = new ApplicationBootstrap(globalDefinitions, factoryManager);
        initialize();
    }

    private void initialize() {
        // Von hier aus werden die JSON-Profile gelesen und gespeichert.
        bootstrap.initialize();
    }

    public GlobalDefinitions getGlobalDefinitions() {
        return globalDefinitions;
    }

    public FactoryManager getFactoryManager() {
        return factoryManager;
    }
}