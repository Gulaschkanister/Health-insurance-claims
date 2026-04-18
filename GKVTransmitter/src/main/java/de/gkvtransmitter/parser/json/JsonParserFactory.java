package de.gkvtransmitter.parser.json;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.definition.InvoiceType;
import de.gkvtransmitter.domain.DtaMessage;
import de.gkvtransmitter.domain.SegmentInfo;
import de.gkvtransmitter.domain.ValueFieldEntry;
import de.gkvtransmitter.domain.invoice.Invoice;
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
    private Map<InvoiceType, Invoice> profileByTypeCache;
    private final Map<String, SegmentDefinition> segmentDefinitionCache = new LinkedHashMap<>();

    @Override
    public Invoice parse() {
        return parseInvoice();
    }

    @Override
    public Object create() {
        return parseInvoice();
    }

    public Invoice parseInvoice() {
        return parseProfiles().stream().findFirst().orElse(new Invoice(Map.of(), InvoiceType.SLLA));
    }

    public List<Invoice> parseProfiles() {
        return PROFILE_FILES.stream().map(this::parseProfile).toList();
    }

    public List<DtaMessage> parseInvoices() {
        ensureProfileCache();
        return INVOICE_FILES.stream().map(this::parseInvoiceResource).toList();
    }

    public List<SegmentDefinition> parseSegments() {
        List<SegmentDefinition> definitions = new ArrayList<>();
        for (Invoice invoice : parseProfiles()) {
            definitions.addAll(invoice.getSegments().values());
        }
        return definitions;
    }

    private Invoice parseProfile(String profileResourcePath) throws IllegalArgumentException {
        JsonNode profileRoot = readResourceTree(profileResourcePath);
        JsonNode segmentsNode = profileRoot.path("segments");

        Map<String, SegmentDefinition> segments = new LinkedHashMap<>();
        if (segmentsNode.isArray()) {
            for (JsonNode segmentNode : segmentsNode) {
                String segmentName = segmentNode.path("name").asText();
                boolean repeatable = segmentNode.path("repeatable").asBoolean(false);
                segments.put(segmentName, parseSegmentFromResource(segmentName, repeatable));
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
        boolean hasMessageType = false;
        
        if (segmentsNode.isArray()) {
            for (JsonNode segmentNode : segmentsNode) {
                int position = segmentNode.path("position").asInt(-1);
                String segmentType = segmentNode.path("segmentType").asText();
                
                if (!segmentType.isBlank()) {
                    InvoiceType messageType = null;
                    String groupTag = segmentNode.path("groupTag").asText("").trim();
                    Map<String, String> valueFields = readValueFields(segmentNode.path("values"));
                    
                    // Sammle Nachrichtentypen (ignoriere Header/Footer wie UNB/UNZ)
                    // UNB/UNZ haben nachrichtentyp=null, da sie technische Hülle sind
                    if (!segmentNode.path("nachrichtentyp").isNull()) {
                        String messageTypeRaw = segmentNode.path("nachrichtentyp").asText();
                        if (!messageTypeRaw.isBlank()) {
                            try {
                                messageType = InvoiceType.valueOf(messageTypeRaw.toUpperCase(Locale.ROOT));
                                hasMessageType = true;
                            } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException("Unbekannter Nachrichtentyp '" + messageTypeRaw + 
                                    "' bei Segment " + segmentType + " in " + invoiceResourcePath, e);
                            }
                        }
                    }

                    valueFields = ensureTemplateValueFields(messageType, segmentType, valueFields);
                        Map<String, String> valueFieldJavaTypes = buildValueFieldJavaTypes(messageType, segmentType,
                            valueFields);
                        Map<String, ValueFieldEntry> typedValueFields = buildTypedValueFields(valueFields,
                            valueFieldJavaTypes);
                    
                    // Erstelle SegmentInfo mit Position, Typ und MessageType
                        segments.add(new SegmentInfo(position, segmentType, messageType, groupTag, typedValueFields));
                }
            }
        }
        
        if (!hasMessageType) {
            throw new IllegalArgumentException("Invoice " + invoiceResourcePath + 
                " hat keine gültigen Nachrichtentypen in den Segmenten");
        }

        String sourceName = extractFileName(invoiceResourcePath);
        String invoicerName = resolveInvoicerName(invoiceRoot, sourceName);
        String schemaVersion = invoiceRoot.path("schemaVersion").asText();
        String version = invoiceRoot.path("version").asText();
        
        return new DtaMessage(sourceName, invoicerName, schemaVersion, version, segments);
    }

    private SegmentDefinition parseSegmentFromResource(String segmentName, boolean repeatable) {
        String resourcePath = "segments/" + segmentName.toLowerCase(Locale.ROOT) + ".json";
        JsonNode segmentRoot = readResourceTree(resourcePath);

        Map<Integer, FieldDefinition> fieldDefinitions = new LinkedHashMap<>();
        JsonNode fieldsNode = segmentRoot.path("fields");
        if (fieldsNode.isArray()) {
            for (JsonNode fieldNode : fieldsNode) {
                FieldDefinition fieldDefinition = toFieldDefinition(fieldNode);
                fieldDefinitions.put(fieldDefinition.getPosition(), fieldDefinition);
            }
        }

        return new SegmentDefinition(fieldDefinitions, segmentName, repeatable);
    }

    private void ensureProfileCache() {
        if (profileByTypeCache != null) {
            return;
        }

        profileByTypeCache = new LinkedHashMap<>();
        for (Invoice profile : parseProfiles()) {
            profileByTypeCache.put(profile.getMessageType(), profile);
        }
    }

    private Map<String, String> ensureTemplateValueFields(InvoiceType messageType, String segmentType,
            Map<String, String> existingValues) {
        SegmentDefinition definition = resolveSegmentDefinition(messageType, segmentType);
        if (definition == null) {
            return existingValues;
        }

        Map<String, String> normalizedExistingValues = new LinkedHashMap<>();
        existingValues.forEach((key, value) -> normalizedExistingValues.put(toFormFieldKey(key), value));

        Map<String, String> filledValues = new LinkedHashMap<>();
        List<FieldDefinition> orderedFields = definition.getFieldDefinitions().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        for (FieldDefinition field : orderedFields) {
            String key = toFormFieldKey(field.getName());
            if (filledValues.containsKey(key)) {
                key = key + "_" + field.getPosition();
            }

            if (existingValues.containsKey(field.getName())) {
                filledValues.put(key, existingValues.get(field.getName()));
            } else if (existingValues.containsKey(key)) {
                filledValues.put(key, existingValues.get(key));
            } else if (normalizedExistingValues.containsKey(toFormFieldKey(field.getName()))) {
                filledValues.put(key, normalizedExistingValues.get(toFormFieldKey(field.getName())));
            } else {
                filledValues.put(key, "");
            }
        }

        normalizedExistingValues.forEach(filledValues::putIfAbsent);
        return filledValues;
    }

    private String toFormFieldKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }

        String trimmed = rawKey.trim();
        int separatorIndex = trimmed.indexOf(" - ");
        if (separatorIndex > 0 && separatorIndex + 3 < trimmed.length()) {
            return trimmed.substring(separatorIndex + 3).trim();
        }

        return trimmed;
    }

    private SegmentDefinition resolveSegmentDefinition(InvoiceType messageType, String segmentType) {
        if (segmentDefinitionCache.containsKey(segmentType)) {
            return segmentDefinitionCache.get(segmentType);
        }

        ensureProfileCache();
        if (messageType != null) {
            Invoice profile = profileByTypeCache.get(messageType);
            if (profile != null) {
                SegmentDefinition definition = profile.getSegments().get(segmentType);
                if (definition != null) {
                    segmentDefinitionCache.put(segmentType, definition);
                    return definition;
                }
            }
        }

        SegmentDefinition resourceDefinition = parseSegmentFromResource(segmentType, false);
        segmentDefinitionCache.put(segmentType, resourceDefinition);
        return resourceDefinition;
    }

    private Map<String, String> readValueFields(JsonNode valuesNode) {
        if (valuesNode == null || !valuesNode.isObject()) {
            return new LinkedHashMap<>();
        }

        Map<String, String> valueFields = new LinkedHashMap<>();
        valuesNode.fields().forEachRemaining(entry -> {
            JsonNode valueNode = entry.getValue();
            valueFields.put(entry.getKey(), valueNode == null || valueNode.isNull() ? "" : valueNode.asText());
        });
        return valueFields;
    }

    private Map<String, String> buildValueFieldJavaTypes(InvoiceType messageType, String segmentType,
            Map<String, String> valueFields) {
        Map<String, String> valueFieldJavaTypes = new LinkedHashMap<>();
        SegmentDefinition definition = resolveSegmentDefinition(messageType, segmentType);
        if (definition == null) {
            valueFields.keySet().forEach(key -> valueFieldJavaTypes.put(key, "String"));
            return valueFieldJavaTypes;
        }

        Map<String, String> keyToJavaType = new LinkedHashMap<>();
        List<FieldDefinition> orderedFields = definition.getFieldDefinitions().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        for (FieldDefinition field : orderedFields) {
            String baseKey = toFormFieldKey(field.getName());
            String key = baseKey;
            if (keyToJavaType.containsKey(key)) {
                key = key + "_" + field.getPosition();
            }
            keyToJavaType.put(key, mapFieldTypeToJavaType(field.getType()));
        }

        valueFields.keySet().forEach(key -> valueFieldJavaTypes.put(key, keyToJavaType.getOrDefault(key, "String")));
        return valueFieldJavaTypes;
    }

    private Map<String, ValueFieldEntry> buildTypedValueFields(Map<String, String> rawValueFields,
            Map<String, String> valueFieldJavaTypes) {
        Map<String, ValueFieldEntry> typedValueFields = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawValueFields.entrySet()) {
            String key = entry.getKey();
            String rawValue = entry.getValue();
            String javaType = valueFieldJavaTypes.getOrDefault(key, "String");
            Object typedValue = parseValueByJavaType(rawValue, javaType);
            typedValueFields.put(key, new ValueFieldEntry(typedValue, javaType));
        }
        return typedValueFields;
    }

    private Object parseValueByJavaType(String rawValue, String javaType) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return switch (javaType) {
                case "Integer" -> Integer.valueOf(rawValue);
                case "LocalDate" -> parseLocalDate(rawValue);
                default -> rawValue;
            };
        } catch (RuntimeException ex) {
            // Keep original user/resource value if typed conversion fails.
            return rawValue;
        }
    }

    private LocalDate parseLocalDate(String rawValue) {
        try {
            return LocalDate.parse(rawValue, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(rawValue);
        }
    }

    private String mapFieldTypeToJavaType(FieldType fieldType) {
        if (fieldType == null) {
            return "String";
        }

        return switch (fieldType) {
            case NUMBER -> "Integer";
            case DATE -> "LocalDate";
            case STRING -> "String";
        };
    }

    private FieldDefinition toFieldDefinition(JsonNode fieldNode) {
        int position = fieldNode.path("position").asInt(-1);
        FieldType fieldType = mapFieldType(fieldNode.path("type").asText());
        boolean mandatory = fieldNode.path("mandatory").asBoolean(false);
        int maxLength = fieldNode.path("maxLength").asInt(0);
        String name = fieldNode.path("name").asText();
        return new FieldDefinition(position, fieldType, mandatory, maxLength, name);
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
