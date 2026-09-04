package de.gkvtransmitter.entity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String firstname;
    private String lastname;
    private String street;
    private String country;
    private String housenumber;
    private int plz;
    private int ik;
    private int kassenIk;
    private LocalDate birthDate;

    @Transient
    private Map<String, TagList> tags = new HashMap<>();

    protected Person() {
        this.tags = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
    }

   

    public Person(String firstname, String lastname, String street, String country, String housenumber, int plz,
            int ik, int kassenIk, LocalDate birthDate) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.street = street;
        this.country = country;
        this.housenumber = housenumber;
        this.plz = plz;
        this.ik = ik;
        this.kassenIk = kassenIk;
        this.birthDate = birthDate;
        // Load tag configuration from JSON
        this.tags = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
    }

    public TagList getTagList(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return tags.get(name);
    }
}
