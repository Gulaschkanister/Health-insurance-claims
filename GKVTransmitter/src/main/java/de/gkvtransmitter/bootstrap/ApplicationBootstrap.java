package de.gkvtransmitter.bootstrap;

import de.gkvtransmitter.definition.GlobalDefinitions;
import de.gkvtransmitter.model.Invoice;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.parser.json.JsonParserFactory;
import de.gkvtransmitter.presentation.FactoryManager;

public class ApplicationBootstrap {

    private final GlobalDefinitions globalDefinitions;
    private final FactoryManager factoryManager;

    public ApplicationBootstrap(GlobalDefinitions globalDefinitions, FactoryManager factoryManager) {
        this.globalDefinitions = globalDefinitions;
        this.factoryManager = factoryManager;
    }

    public void initialize() throws RuntimeException {
        try {
            // Liest die Profil- und Segmentdefinitionen aus den JSON-Resourcen.
            JsonParserFactory jsonParserFactory = new JsonParserFactory();

            // Optional fuer spaetere Aufrufe ueber den FactoryManager hinterlegen.
            factoryManager.addFactory(jsonParserFactory);

            // Profile enthalten Strukturdefinitionen (nicht die eigentlichen
            // Rechnungsinstanzen) und werden separat gehalten.
            for (Invoice profile : jsonParserFactory.parseProfiles()) {
                globalDefinitions.registerProfile(profile.getMessageType(), profile);
            }

            // Invoice-Templates werden aus resources/invoices geladen und in der
            // Template-Map gesammelt.
            for (DtaMessage invoice : jsonParserFactory.parseInvoices()) {
                globalDefinitions.registerInvoiceTemplate(invoice);
            }
        } catch (Exception e) {
            System.err.println("❌ Bootstrap-Initialisierung fehlgeschlagen: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Ursache: " + e.getCause().getMessage());
            }
            throw new RuntimeException("Fehler beim Laden der Profile und Invoices", e);
        }
    }
}
