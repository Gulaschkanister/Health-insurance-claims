package de.gkvtransmitter.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "Patient")
public class Patient extends Person {

    public Patient() {
        super();
    }

    public Patient(String firstname, String lastname, String street, String country, String housenumber, int plz,
            int ik) {
        super(firstname, lastname, street, country, housenumber, plz, ik);
    }

}
