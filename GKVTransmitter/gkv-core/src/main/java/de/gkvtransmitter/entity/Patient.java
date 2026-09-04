package de.gkvtransmitter.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "Patient")
public class Patient extends Person {

    public Patient() {
        super();
    }

    

    public Patient(String firstname, String lastname, String street, String country, String housenumber, int plz,
            int ik, int kassenIk, LocalDate birthDate) {
        super(firstname, lastname, street, country, housenumber, plz, ik, kassenIk, birthDate);
    }

}
