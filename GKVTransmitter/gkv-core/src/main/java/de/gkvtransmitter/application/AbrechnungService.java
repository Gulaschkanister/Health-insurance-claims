package de.gkvtransmitter.application;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.gkvtransmitter.dispatch.BillingOfficeEndpointRegistry;
import de.gkvtransmitter.dispatch.DispatchBatch;
import de.gkvtransmitter.dispatch.DtaDispatchService;
import de.gkvtransmitter.dispatch.FileBillingOfficeTransport;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.hibernate.sqllite.HibernateSqllite;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.validator.DtaValidationService;

/** Coordinates creation and dispatch of patient-specific settlements. */
public final class AbrechnungService {

    private final DtaDispatchService dispatchService;

    /**
     * Der Dienst, wie ihn die Anwendung benutzt.
     *
     * <p>Die Datenaustauschreferenzen kommen aus der Datenbank und nicht aus
     * einem Zaehler im Arbeitsspeicher. <b>Bis zum 06.09.2026 war es
     * umgekehrt</b>, und damit trug die zweite Abrechnung eines Monats
     * dieselben Referenzen wie die erste — „Datenaustauschreferenz doppelt
     * vergeben" ist ein dokumentierter Abweisungsgrund. Die gesperrte,
     * gepruefte Methode dafuer gab es die ganze Zeit; sie hatte nur keinen
     * Aufrufer.</p>
     *
     * <p>Die Datenbank wird hier geoeffnet und <b>nicht wieder geschlossen</b>
     * — sie lebt so lange wie die Anwendung. Wer den Dienst kurzlebig braucht,
     * nimmt den anderen Konstruktor.</p>
     */
    public AbrechnungService() {
        this(HibernateSqllite.open());
    }

    /**
     * Der Dienst mit einer bereits geoeffneten Datenbank.
     *
     * <p><b>Diesen Konstruktor nimmt die Anwendung</b>, damit sie nicht eine
     * zweite Verbindung auf dieselbe SQLite-Datei oeffnet. SQLite laesst nur
     * einen Schreiber zu, und die Vergabe der Referenz liest erst und schreibt
     * dann — genau der Fall, in dem {@code busy_timeout} absichtlich nicht
     * greift. Mit einer Verbindung stellt sich die Frage gar nicht.</p>
     */
    public AbrechnungService(DataRepository datenbank) {
        this(new DtaDispatchService(BillingOfficeEndpointRegistry.loadDefault(),
                new FileBillingOfficeTransport(), DtaValidationService.standard(),
                Objects.requireNonNull(datenbank, "datenbank must not be null")
                        ::nextDtaInterchangeReference));
    }

    public AbrechnungService(DtaDispatchService dispatchService) {
        this.dispatchService = Objects.requireNonNull(dispatchService, "dispatchService must not be null");
    }

    public List<DispatchBatch> createAndDispatch(List<Patient> selectedPatients,
            PersonGroup group, Blueprint blueprint, Map<Integer, Integer> appointments, Path outputDirectory) {
        Objects.requireNonNull(selectedPatients, "selectedPatients must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        Objects.requireNonNull(appointments, "appointments must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        if (selectedPatients.isEmpty()) {
            throw new IllegalArgumentException("At least one patient must be selected");
        }
        ServiceProvider provider = findProvider(group);
        List<Abrechnung> abrechnungen = new ArrayList<>();
        for (Patient patient : selectedPatients) {
            if (patient == null) {
                throw new IllegalArgumentException("Selected patients must not contain null");
            }
            int appointmentCount = appointments.getOrDefault(patient.getId(), 0);
            if (appointmentCount < 0) {
                throw new IllegalArgumentException("Appointment count must not be negative");
            }
            abrechnungen.add(new Abrechnung(patient, provider, blueprint, appointmentCount));
        }
        return dispatchService.generateAndRoute(abrechnungen, outputDirectory);
    }

    private ServiceProvider findProvider(PersonGroup group) {
        if (group.getServiceProviders() == null || group.getServiceProviders().isEmpty()) {
            throw new IllegalArgumentException("The selected group needs at least one service provider");
        }
        return group.getServiceProviders().iterator().next();
    }
}
