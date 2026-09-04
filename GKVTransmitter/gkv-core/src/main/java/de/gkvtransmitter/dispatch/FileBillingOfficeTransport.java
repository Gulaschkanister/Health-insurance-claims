package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileBillingOfficeTransport implements BillingOfficeTransport {

    @Override
    public BillingOfficeTransportType getType() {
        return BillingOfficeTransportType.FILE;
    }

    @Override
    public Path send(Path sourceFile, BillingOfficeEndpoint endpoint) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile must not be null");
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        if (!endpoint.enabled()) {
            throw new IllegalStateException("Billing office endpoint is disabled for kassenIk=" + endpoint.kassenIk());
        }
        if (endpoint.transportType() != BillingOfficeTransportType.FILE) {
            throw new IllegalArgumentException("Unsupported endpoint transport type: " + endpoint.transportType());
        }

        Files.createDirectories(endpoint.destinationDirectory());
        Path destination = endpoint.destinationDirectory().resolve(sourceFile.getFileName());
        return Files.copy(sourceFile, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public BillingOfficeEndpointCheck probe(BillingOfficeEndpoint endpoint) {
        if (endpoint == null) {
            return new BillingOfficeEndpointCheck(-1, "unbekannt", null, false, "Endpoint ist null");
        }
        if (!endpoint.enabled()) {
            return new BillingOfficeEndpointCheck(endpoint.kassenIk(), endpoint.name(), endpoint.destinationDirectory(), false, "Endpoint ist deaktiviert");
        }
        if (endpoint.transportType() != BillingOfficeTransportType.FILE) {
            return new BillingOfficeEndpointCheck(endpoint.kassenIk(), endpoint.name(), endpoint.destinationDirectory(), false, "Transporttyp wird von File-Transport nicht unterstützt");
        }

        try {
            Files.createDirectories(endpoint.destinationDirectory());
            boolean writable = Files.isWritable(endpoint.destinationDirectory());
            return new BillingOfficeEndpointCheck(
                    endpoint.kassenIk(),
                    endpoint.name(),
                    endpoint.destinationDirectory(),
                    writable,
                    writable ? "Zielverzeichnis ist erreichbar" : "Zielverzeichnis ist nicht beschreibbar");
        } catch (IOException e) {
            return new BillingOfficeEndpointCheck(endpoint.kassenIk(), endpoint.name(), endpoint.destinationDirectory(), false, e.getMessage());
        }
    }
}