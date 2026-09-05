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

    /**
     * Holt einen Text und weicht auf einen zweiten Schluessel aus, wenn es den
     * ersten nicht gibt.
     *
     * <p>Fuer zusammengesetzte Schluessel wie {@code msg.patientUpdated}: sie
     * entstehen aus einem Typnamen und muessen nicht fuer jeden Typ hinterlegt
     * sein. Ohne diesen Ausweg stuende bei einem fehlenden Schluessel der
     * Schluessel selbst in der Oberflaeche - {@link #get(String)} gibt ihn
     * zurueck, damit eine Luecke beim Entwickeln auffaellt, aber im Betrieb
     * waere das eine Zumutung.</p>
     */
    public String get(String key, String ersatzSchluessel) {
        return messages.containsKey(key) ? messages.get(key) : get(ersatzSchluessel);
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
