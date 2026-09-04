package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ein Transport, der die Lieferung zustellt und zusaetzlich eine Antwort
 * erzeugt.
 *
 * <p>Umschliesst einen vorhandenen Transport, statt ihn zu ersetzen: die
 * Zustellung selbst bleibt unveraendert, danach nimmt eine
 * {@link SimulierteKassenGegenstelle} die Datei entgegen und legt ein
 * Antwortprotokoll daneben. Dadurch laesst sich der vollstaendige Ablauf bis
 * zur Rueckmeldung durchspielen, ohne dass am Versandweg etwas geaendert werden
 * muss.</p>
 *
 * <p>Gedacht fuer Erprobung und Abnahme. Fuer den Echtbetrieb wird stattdessen
 * der jeweilige Transport ohne diese Huelle verwendet.</p>
 */
public final class SimulierterKassenTransport implements BillingOfficeTransport {

    private final BillingOfficeTransport delegat;
    private final SimulierteKassenGegenstelle gegenstelle;

    /** Die letzte Antwort je Kassen-IK, damit der Aufrufer sie auswerten kann. */
    private final Map<Integer, BillingOfficeResponse> letzteAntworten = new ConcurrentHashMap<>();

    public SimulierterKassenTransport() {
        this(new FileBillingOfficeTransport(), new SimulierteKassenGegenstelle("Test-Kasse"));
    }

    public SimulierterKassenTransport(BillingOfficeTransport delegat, SimulierteKassenGegenstelle gegenstelle) {
        this.delegat = Objects.requireNonNull(delegat, "delegat must not be null");
        this.gegenstelle = Objects.requireNonNull(gegenstelle, "gegenstelle must not be null");
    }

    @Override
    public BillingOfficeTransportType getType() {
        return delegat.getType();
    }

    @Override
    public Path send(Path sourceFile, BillingOfficeEndpoint endpoint) throws IOException {
        Path zugestellt = delegat.send(sourceFile, endpoint);

        Path protokoll = gegenstelle.empfangeDatei(zugestellt);
        String antworttext = java.nio.file.Files.readString(protokoll);
        letzteAntworten.put(endpoint.kassenIk(), new BillingOfficeResponseParser().parse(antworttext));

        return zugestellt;
    }

    @Override
    public BillingOfficeEndpointCheck probe(BillingOfficeEndpoint endpoint) {
        return delegat.probe(endpoint);
    }

    /** Die zuletzt erhaltene Rueckmeldung der angegebenen Kasse. */
    public BillingOfficeResponse letzteAntwort(int kassenIk) {
        return letzteAntworten.get(kassenIk);
    }

    /** Alle bisher erhaltenen Rueckmeldungen, nach Kassen-IK. */
    public Map<Integer, BillingOfficeResponse> alleAntworten() {
        return Map.copyOf(letzteAntworten);
    }
}
