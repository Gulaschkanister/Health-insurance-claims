package de.gkvtransmitter.entity;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    @Transient
    private Map<String, TagList> tags = new HashMap<>();
    // TODO: Transient Tag Klasse Pro Feld hinterlegen zur einfacheren erstellung
    // von Feldern

    protected Person() {
        this.tags = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
    }

    public Person(String firstname, String lastname, String street, String country, String housenumber, int plz,
            int ik) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.street = street;
        this.country = country;
        this.housenumber = housenumber;
        this.plz = plz;
        this.ik = ik;
        // Load tag configuration from JSON
        this.tags = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
    }

    public TagList getTagList(String name) {
        if (name == null || name.isBlank()) return null;
        return tags.get(name);
    }
}
