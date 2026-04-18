package de.gkvtransmitter.definition;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.gkvtransmitter.domain.DtaMessage;
import de.gkvtransmitter.domain.invoice.Invoice;

public final class GlobalDefinitions {
    private static final GlobalDefinitions INSTANCE = new GlobalDefinitions();

    private final Map<InvoiceType, Invoice> profileCollection;
    private final Map<String, DtaMessage> invoiceTemplateCollection;

    private GlobalDefinitions() {
        this.profileCollection = new EnumMap<>(InvoiceType.class);
        this.invoiceTemplateCollection = new LinkedHashMap<>();
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

    public Map<String, DtaMessage> getInvoiceTemplateCollection() {
        return Collections.unmodifiableMap(invoiceTemplateCollection);
    }

    public void registerInvoiceTemplate(DtaMessage invoiceTemplate) {
        String key = invoiceTemplate.getInvoicerName() == null ? "" : invoiceTemplate.getInvoicerName().trim();
        if (key.isBlank()) {
            key = invoiceTemplate.getSourceName();
        }
        invoiceTemplateCollection.put(key, invoiceTemplate);
    }

    public List<DtaMessage> findInvoicesByInvoicerName(String searchTerm) throws IllegalArgumentException {

        String normalizedSearchTerm = normalize(searchTerm);
        if (normalizedSearchTerm.isEmpty()) {
            throw new IllegalArgumentException("Invoice search term cannot be empty or whitespace only.");
        }

        return invoiceTemplateCollection.values().stream()
                .filter(invoice -> normalize(invoice.getInvoicerName()).contains(normalizedSearchTerm))
                .toList();
    }

    private String normalize(String value) throws IllegalArgumentException{
        if (value == null) {
            throw new IllegalArgumentException("Invoice search term cannot be null.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}