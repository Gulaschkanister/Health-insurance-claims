package de.gkvtransmitter.application;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.gkvtransmitter.dispatch.DispatchBatch;
import de.gkvtransmitter.dispatch.DtaDispatchService;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;

/** Coordinates creation and dispatch of patient-specific settlements. */
public final class AbrechnungService {

    private final DtaDispatchService dispatchService;

    public AbrechnungService() {
        this(new DtaDispatchService());
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
