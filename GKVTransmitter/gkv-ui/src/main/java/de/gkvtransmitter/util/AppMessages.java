package de.gkvtransmitter.util;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AppMessages {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Map<String, String> messages;

    public AppMessages(String resourcePath) {
        this.messages = loadMessages(resourcePath);
    }

    public String get(String key) {
        return messages.getOrDefault(key, key);
    }

    private Map<String, String> loadMessages(String resourcePath) {
        try (InputStream inputStream = AppMessages.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return Collections.emptyMap();
            }
            return MAPPER.readValue(inputStream, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to load messages from " + resourcePath, e);
        }
    }
}
