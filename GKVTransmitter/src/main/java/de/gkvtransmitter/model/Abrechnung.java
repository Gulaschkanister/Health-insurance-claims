package de.gkvtransmitter.model;

import java.time.LocalDateTime;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;

public class Abrechnung {
    private final Patient patient;
    private final ServiceProvider provider;
    private final Blueprint blueprint;
    private final int appointments;
    private final LocalDateTime createdAt;

    public Abrechnung(Patient patient, ServiceProvider provider, Blueprint blueprint, int appointments) {
        this.patient = patient;
        this.provider = provider;
        this.blueprint = blueprint;
        this.appointments = appointments;
        this.createdAt = LocalDateTime.now();
    }

    public Patient getPatient() { return patient; }
    public ServiceProvider getProvider() { return provider; }
    public Blueprint getBlueprint() { return blueprint; }
    public int getAppointments() { return appointments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
