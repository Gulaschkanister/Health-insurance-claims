package de.gkvtransmitter.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.gkvtransmitter.domain.DtaMessage;
import de.gkvtransmitter.domain.Invoice;

public final class GlobalDefinitions {
    private static final GlobalDefinitions INSTANCE = new GlobalDefinitions();

    private final Map<InvoiceType, Invoice> profileCollection;
    private final List<DtaMessage> invoiceCollection;

    private GlobalDefinitions() {
        this.profileCollection = new EnumMap<>(InvoiceType.class);
        this.invoiceCollection = new ArrayList<>();
    }

    public static GlobalDefinitions getInstance() {
        return INSTANCE;
    }

    public Map<InvoiceType, Invoice> getProfileCollection() {
        return Collections.unmodifiableMap(profileCollection);
    }

    public void registerProfile(InvoiceType type, Invoice profile) {
        profileCollection.put(type, profile);
    }

    public List<DtaMessage> getInvoiceCollection() {
        return Collections.unmodifiableList(invoiceCollection);
    }

    public void registerInvoice(DtaMessage invoice) {
        invoiceCollection.add(invoice);
    }

    public List<DtaMessage> findInvoicesByInvoicerName(String searchTerm) {
        String normalizedSearchTerm = normalize(searchTerm);
        if (normalizedSearchTerm.isEmpty()) {
            return List.of();
        }

        return invoiceCollection.stream()
                .filter(invoice -> normalize(invoice.getInvoicerName()).contains(normalizedSearchTerm))
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}