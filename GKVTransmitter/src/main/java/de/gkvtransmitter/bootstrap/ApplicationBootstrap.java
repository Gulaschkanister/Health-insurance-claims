package de.gkvtransmitter.bootstrap;

import de.gkvtransmitter.definition.GlobalDefinitions;
import de.gkvtransmitter.domain.DtaMessage;
import de.gkvtransmitter.domain.invoice.Invoice;
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

            // Echte Rechnungen werden aus resources/invoices geladen und in der
            // Invoice-Liste gesammelt.
            for (DtaMessage invoice : jsonParserFactory.parseInvoices()) {
                globalDefinitions.registerInvoice(invoice);
            }
        } catch (Exception e) {
            System.err.println("❌ Bootstrap-Initialisierung fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Fehler beim Laden der Profile und Invoices", e);
        }
    }
}