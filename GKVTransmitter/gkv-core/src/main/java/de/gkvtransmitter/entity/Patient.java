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



    public Patient(String firstname, String lastname, Anschrift anschrift,
            int ik, int kassenIk, LocalDate birthDate) {
        super(firstname, lastname, anschrift, ik, kassenIk, birthDate);
    }

}
