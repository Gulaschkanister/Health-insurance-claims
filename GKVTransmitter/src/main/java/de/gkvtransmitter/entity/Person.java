package de.gkvtransmitter.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Getter;

/**
 * TODO: Entity-FieldDefinition-Architektur
 * 
 * Ziel: Zentrale Verwaltung aller UI-Metadaten (InputType, Label, Validation)
 * für alle Entities (Person, Patient, ServiceProvider) über eine dedizierte
 * FieldDefinition-Klasse/Registry statt in separaten Annotations.
 * 
 * Struktur:
 * 1. EntityFieldRegistry - hält alle FieldDefinitions für jede Entity
 *    - Kann aus JSON (src/main/resources/entities/) oder Code konfiguriert werden
 * 2. FieldDefinition - zentral für DTA-Segments UND Entities
 *    - Unterschied zu DTA: keine DB-Speicherung
 * 3. Form-Generator nutzt die gleiche Logik für beide
 * 
 * Vorteil: Single Source of Truth für alle InputField-Typen und deren Handling
 */
@Getter
@MappedSuperclass
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String firstname;
    private String lastname;
    private String street;
    private String country;
    private String housenumber;
    private int plz;
    private int ik;

    public Person(String firstname, String lastname, String street, String country, String housenumber, int plz,
            int ik) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.street = street;
        this.country = country;
        this.housenumber = housenumber;
        this.plz = plz;
        this.ik = ik;
    }
}
