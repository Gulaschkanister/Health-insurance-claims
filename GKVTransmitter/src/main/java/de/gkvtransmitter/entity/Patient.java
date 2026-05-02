package de.gkvtransmitter.entity;

import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "Patient")
public class Patient extends Person {

    public Patient() {
        super();
    }

    public Patient(String firstname, String lastname, String street, String country, String housenumber, int plz,
            int ik, LocalDate birthDate) {
        super(firstname, lastname, street, country, housenumber, plz, ik, birthDate);
    }

}
