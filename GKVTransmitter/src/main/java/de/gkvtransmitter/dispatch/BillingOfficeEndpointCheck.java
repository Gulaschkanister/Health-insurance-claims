package de.gkvtransmitter.dispatch;

import java.nio.file.Path;

public record BillingOfficeEndpointCheck(
        int kassenIk,
        String name,
        Path destinationDirectory,
        boolean reachable,
        String message) {
}