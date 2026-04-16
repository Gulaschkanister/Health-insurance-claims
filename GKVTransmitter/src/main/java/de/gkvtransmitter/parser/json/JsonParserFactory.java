package de.gkvtransmitter.parser.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.definition.InvoiceType;
import de.gkvtransmitter.domain.DtaMessage;
import de.gkvtransmitter.domain.SegmentInfo;
import de.gkvtransmitter.domain.invoice.Invoice;
import de.gkvtransmitter.domain.segment.Segment;
import de.gkvtransmitter.domain.segment.SegmentDefinition;
import de.gkvtransmitter.domain.segment.field.FieldDefinition;
import de.gkvtransmitter.domain.segment.field.FieldType;
import de.gkvtransmitter.factory.Factory;
import de.gkvtransmitter.parser.ParserFactory;

public class JsonParserFactory implements ParserFactory<Invoice>, Factory {
    private static final List<String> PROFILE_FILES = List.of(
            "profiles/slla-profile.json",
            "profiles/slga-profile.json");

    private static final List<String> INVOICE_FILES = List.of(
            "invoices/antenatal_class_single.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Invoice parse() {
        return parseInvoice();
    }

    @Override
    public Object create() {
        return parseInvoice();
    }

    public Invoice parseInvoice() {
        return parseProfiles().stream().findFirst().orElse(new Invoice(new ArrayList<>(), InvoiceType.SLLA));
    }

    public List<Invoice> parseProfiles() {
        return PROFILE_FILES.stream().map(this::parseProfile).toList();
    }

    public List<DtaMessage> parseInvoices() {
        return INVOICE_FILES.stream().map(this::parseInvoiceResource).toList();
    }

    public List<SegmentDefinition> parseSegments() {
        List<SegmentDefinition> definitions = new ArrayList<>();
        for (Invoice invoice : parseProfiles()) {
            for (Segment segment : invoice.getSegments()) {
                definitions.add(segment.getDefinition());
            }
        }
        return definitions;
    }

    private Invoice parseProfile(String profileResourcePath) throws IllegalArgumentException {
        JsonNode profileRoot = readResourceTree(profileResourcePath);
        JsonNode segmentsNode = profileRoot.path("segments");

        List<Segment> segments = new ArrayList<>();
        if (segmentsNode.isArray()) {
            for (JsonNode segmentNode : segmentsNode) {
                String segmentName = segmentNode.path("name").asText();
                boolean repeatable = segmentNode.path("repeatable").asBoolean(false);
                segments.add(parseSegmentFromResource(segmentName, repeatable));
            }
        }

        String invoiceTypeRaw = profileRoot.path("nachrichtentyp").asText();
        if (invoiceTypeRaw.isBlank()) {
            throw new IllegalArgumentException("Profile " + profileResourcePath + " hat keinen gültigen Nachrichtentyp");
        }
        
        try {
            InvoiceType invoiceType = InvoiceType.valueOf(invoiceTypeRaw.toUpperCase(Locale.ROOT));
            return new Invoice(segments, invoiceType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unbekannter Nachrichtentyp '" + invoiceTypeRaw + "' in " + profileResourcePath, e);
        }
    }

    private DtaMessage parseInvoiceResource(String invoiceResourcePath) throws IllegalArgumentException {
        JsonNode invoiceRoot = readResourceTree(invoiceResourcePath);
        JsonNode segmentsNode = invoiceRoot.path("segments");

        List<SegmentInfo> segments = new ArrayList<>();
        Set<InvoiceType> messageTypes = new java.util.HashSet<>();
        
        if (segmentsNode.isArray()) {
            for (JsonNode segmentNode : segmentsNode) {
                int position = segmentNode.path("position").asInt(-1);
                String segmentType = segmentNode.path("segmentType").asText();
                
                if (!segmentType.isBlank()) {
                    InvoiceType messageType = null;
                    
                    // Sammle Nachrichtentypen (ignoriere Header/Footer wie UNB/UNZ)
                    // UNB/UNZ haben nachrichtentyp=null, da sie technische Hülle sind
                    if (!segmentNode.path("nachrichtentyp").isNull()) {
                        String messageTypeRaw = segmentNode.path("nachrichtentyp").asText();
                        if (!messageTypeRaw.isBlank()) {
                            try {
                                messageType = InvoiceType.valueOf(messageTypeRaw.toUpperCase(Locale.ROOT));
                                messageTypes.add(messageType);
                            } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException("Unbekannter Nachrichtentyp '" + messageTypeRaw + 
                                    "' bei Segment " + segmentType + " in " + invoiceResourcePath, e);
                            }
                        }
                    }
                    
                    // Erstelle SegmentInfo mit Position, Typ und MessageType
                    segments.add(new SegmentInfo(position, segmentType, messageType));
                }
            }
        }
        
        if (messageTypes.isEmpty()) {
            throw new IllegalArgumentException("Invoice " + invoiceResourcePath + 
                " hat keine gültigen Nachrichtentypen in den Segmenten");
        }

        String sourceName = extractFileName(invoiceResourcePath);
        String invoicerName = resolveInvoicerName(invoiceRoot, sourceName);
        String schemaVersion = invoiceRoot.path("schemaVersion").asText();
        String version = invoiceRoot.path("version").asText();
        
        return new DtaMessage(sourceName, invoicerName, schemaVersion, version, messageTypes, segments);
    }

    private Segment parseSegmentFromResource(String segmentName, boolean repeatable) {
        String resourcePath = "segments/" + segmentName.toLowerCase(Locale.ROOT) + ".json";
        JsonNode segmentRoot = readResourceTree(resourcePath);

        List<FieldDefinition> fieldDefinitions = new ArrayList<>();
        JsonNode fieldsNode = segmentRoot.path("fields");
        if (fieldsNode.isArray()) {
            for (JsonNode fieldNode : fieldsNode) {
                fieldDefinitions.add(toFieldDefinition(fieldNode));
            }
        }

        SegmentDefinition definition = new SegmentDefinition(fieldDefinitions, segmentName, repeatable);
        return new Segment(new ArrayList<>(), definition);
    }

    private FieldDefinition toFieldDefinition(JsonNode fieldNode) {
        FieldType fieldType = mapFieldType(fieldNode.path("type").asText());
        boolean mandatory = fieldNode.path("mandatory").asBoolean(false);
        int maxLength = fieldNode.path("maxLength").asInt(0);
        String name = fieldNode.path("name").asText();
        return new FieldDefinition(fieldType, mandatory, maxLength, name);
    }

    private FieldType mapFieldType(String rawType) {
        String normalized = rawType.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("D")) {
            return FieldType.DATE;
        }
        if (normalized.startsWith("N")) {
            return FieldType.NUMBER;
        }
        return FieldType.STRING;
    }

    private String extractFileName(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        if (lastSlash < 0) {
            return resourcePath;
        }
        return resourcePath.substring(lastSlash + 1);
    }

    private String resolveInvoicerName(JsonNode invoiceRoot, String sourceName) {
        String invoicerName = invoiceRoot.path("invoicerName").asText("").trim();
        if (!invoicerName.isBlank()) {
            return invoicerName;
        }

        // Fallback to a stable, searchable value when the JSON does not yet
        // expose a dedicated invoicerName field.
        return sourceName;
    }

    private JsonNode readResourceTree(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            return objectMapper.readTree(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not parse resource: " + resourcePath, e);
        }
    }
}
