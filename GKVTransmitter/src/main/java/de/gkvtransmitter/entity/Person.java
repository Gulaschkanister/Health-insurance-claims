package de.gkvtransmitter.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Getter;

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
