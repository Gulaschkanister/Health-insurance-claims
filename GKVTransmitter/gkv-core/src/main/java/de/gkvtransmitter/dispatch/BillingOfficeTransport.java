package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.nio.file.Path;

public interface BillingOfficeTransport {

    BillingOfficeTransportType getType();

    Path send(Path sourceFile, BillingOfficeEndpoint endpoint) throws IOException;

    BillingOfficeEndpointCheck probe(BillingOfficeEndpoint endpoint);
}