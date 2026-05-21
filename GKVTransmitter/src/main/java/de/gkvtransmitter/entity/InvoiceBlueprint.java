package de.gkvtransmitter.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "InvoiceBlueprint")
@Getter
@Setter
public class InvoiceBlueprint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String invoiceTemplateName;

    @Column(nullable = false, length = 8000)
    private String fieldPayload;

    @Column(nullable = false)
    private String selectedPatientIds;

    public InvoiceBlueprint() {
    }

    public InvoiceBlueprint(String name, String invoiceTemplateName, String fieldPayload, String selectedPatientIds) {
        this.name = name;
        this.invoiceTemplateName = invoiceTemplateName;
        this.fieldPayload = fieldPayload;
        this.selectedPatientIds = selectedPatientIds;
    }
}
