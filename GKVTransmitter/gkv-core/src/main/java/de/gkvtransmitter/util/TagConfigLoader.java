package de.gkvtransmitter.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.util.modifiers.DecimalPlaceModifier;
import de.gkvtransmitter.util.modifiers.MaxLengthModifier;
import de.gkvtransmitter.util.modifiers.NoDecimalPlaceModifier;
import de.gkvtransmitter.util.modifiers.NumberModifier;
import de.gkvtransmitter.util.modifiers.SpecialCharModifier;
import de.gkvtransmitter.util.modifiers.TextModifier;

public class TagConfigLoader {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Loads tag configuration from a JSON resource file and returns a Map of field names to TagLists.
     * 
     * @param resourcePath the classpath resource path (e.g., "/tags/person-tags.json")
     * @return Map<String, TagList> with loaded configurations
     */
    public static Map<String, TagList> loadTagConfig(String resourcePath) {
        Map<String, TagList> tagMap = new java.util.HashMap<>();
        try (InputStream inputStream = TagConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            JsonNode root = mapper.readTree(inputStream);
            root.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldConfig = entry.getValue();
                TagList tagList = parseTagConfig(fieldConfig);
                tagMap.put(fieldName, tagList);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tag configuration from " + resourcePath, e);
        }
        return tagMap;
    }

    private static TagList parseTagConfig(JsonNode fieldConfig) {
        String inputOptionStr = fieldConfig.get("inputOption").asText();
        InputOption inputOption = InputOption.valueOf(inputOptionStr);

        List<ModifierInstance> modifiers = new ArrayList<>();
        JsonNode modifiersNode = fieldConfig.get("modifiers");
        if (modifiersNode != null && modifiersNode.isArray()) {
            modifiersNode.forEach(modNode -> {
                ModifierInstance mod = parseModifier(modNode);
                if (mod != null) {
                    modifiers.add(mod);
                }
            });
        }

        return new TagList(inputOption, modifiers);
    }

    private static ModifierInstance parseModifier(JsonNode modNode) {
        String type = modNode.get("type").asText();
        String value = modNode.has("value") ? modNode.get("value").asText() : null;

        return switch (type) {
            case "TEXT" -> (ModifierInstance) new TextModifier();
            case "NUMBER" -> (ModifierInstance) new NumberModifier();
            case "SPECIAL_CHAR" -> (ModifierInstance) new SpecialCharModifier();
            case "NO_DECIMAL_PLACE" -> (ModifierInstance) new NoDecimalPlaceModifier();
            case "MAX_LENGTH" -> {
                if (value == null) throw new IllegalArgumentException("MAX_LENGTH requires a value");
                yield (ModifierInstance) new MaxLengthModifier(Integer.parseInt(value));
            }
            case "DECIMAL_PLACE" -> {
                if (value == null) throw new IllegalArgumentException("DECIMAL_PLACE requires a value");
                yield (ModifierInstance) new DecimalPlaceModifier(Integer.parseInt(value));
            }
            default -> throw new IllegalArgumentException("Unknown modifier type: " + type);
        };
    }
}
