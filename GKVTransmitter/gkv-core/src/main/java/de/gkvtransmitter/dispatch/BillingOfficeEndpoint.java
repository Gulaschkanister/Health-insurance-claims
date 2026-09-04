package de.gkvtransmitter.dispatch;

import java.nio.file.Path;

public record BillingOfficeEndpoint(
        int kassenIk,
        String name,
        BillingOfficeTransportType transportType,
        Path destinationDirectory,
        boolean enabled) {

    public BillingOfficeEndpoint {
        if (transportType == null) {
            transportType = BillingOfficeTransportType.FILE;
        }
        if (destinationDirectory == null) {
            throw new IllegalArgumentException("destinationDirectory must not be null");
        }
    }

    public static BillingOfficeEndpoint fileEndpoint(int kassenIk, String name, Path destinationDirectory) {
        return new BillingOfficeEndpoint(kassenIk, name, BillingOfficeTransportType.FILE, destinationDirectory, true);
    }
}