package de.gkvtransmitter.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "Patient")
public class ServiceProvider extends Person {

    public ServiceProvider(String firstname, String lastname, String street, String country, String housenumber,
            int plz,
            int ik) {
        super(firstname, lastname, street, country, housenumber, plz, ik);
    }

}