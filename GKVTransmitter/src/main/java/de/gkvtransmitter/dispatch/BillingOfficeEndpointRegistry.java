package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BillingOfficeEndpointRegistry {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_RESOURCE = "/billing-office-endpoints.json";

    private final Map<Integer, BillingOfficeEndpoint> endpoints;
    private final Path fallbackRoot;

    public BillingOfficeEndpointRegistry(Map<Integer, BillingOfficeEndpoint> endpoints, Path fallbackRoot) {
        this.endpoints = Collections.unmodifiableMap(new LinkedHashMap<>(endpoints));
        this.fallbackRoot = fallbackRoot;
    }

    public static BillingOfficeEndpointRegistry loadDefault() {
        try (InputStream inputStream = BillingOfficeEndpointRegistry.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (inputStream == null) {
                return new BillingOfficeEndpointRegistry(Map.of(), Path.of("dta_output", "outbox"));
            }
            RegistryConfig config = MAPPER.readValue(inputStream, RegistryConfig.class);
            Map<Integer, BillingOfficeEndpoint> loaded = new LinkedHashMap<>();
            for (BillingOfficeEndpointDefinition definition : config.targets()) {
                if (definition.enabled()) {
                    loaded.put(definition.kassenIk(), definition.toEndpoint());
                }
            }
            Path fallback = config.fallbackRoot() != null ? Path.of(config.fallbackRoot()) : Path.of("dta_output", "outbox");
            return new BillingOfficeEndpointRegistry(loaded, fallback);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load billing office endpoints", e);
        }
    }

    public BillingOfficeEndpoint resolve(int kassenIk, Path fallbackDestinationRoot) {
        BillingOfficeEndpoint endpoint = endpoints.get(kassenIk);
        if (endpoint != null) {
            return endpoint;
        }
        Path destinationRoot = fallbackDestinationRoot != null ? fallbackDestinationRoot : fallbackRoot;
        return BillingOfficeEndpoint.fileEndpoint(kassenIk, "Kasse " + kassenIk, destinationRoot.resolve(String.valueOf(kassenIk)));
    }

    public Map<Integer, BillingOfficeEndpoint> getEndpoints() {
        return endpoints;
    }

    public Path getFallbackRoot() {
        return fallbackRoot;
    }

    public record RegistryConfig(String fallbackRoot, List<BillingOfficeEndpointDefinition> targets) {
        @JsonCreator
        public RegistryConfig(
                @JsonProperty("fallbackRoot") String fallbackRoot,
                @JsonProperty("targets") List<BillingOfficeEndpointDefinition> targets) {
            this.fallbackRoot = fallbackRoot;
            this.targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }

    public record BillingOfficeEndpointDefinition(
            @JsonProperty("kassenIk") int kassenIk,
            @JsonProperty("name") String name,
            @JsonProperty("transportType") BillingOfficeTransportType transportType,
            @JsonProperty("destinationDirectory") String destinationDirectory,
            @JsonProperty("enabled") boolean enabled) {

        @JsonCreator
        public BillingOfficeEndpointDefinition {
        }

        public BillingOfficeEndpoint toEndpoint() {
            return new BillingOfficeEndpoint(kassenIk, name, transportType, Path.of(destinationDirectory), enabled);
        }
    }
}