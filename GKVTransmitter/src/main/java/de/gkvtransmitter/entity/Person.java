package de.gkvtransmitter.entity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import de.gkvtransmitter.converter.EncryptedLocalDateConverter;
import de.gkvtransmitter.converter.EncryptedStringConverter;
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

    @Convert(converter = EncryptedStringConverter.class)
    private String firstname;
    @Convert(converter = EncryptedStringConverter.class)
    private String lastname;
    @Convert(converter = EncryptedStringConverter.class)
    private String street;
    @Convert(converter = EncryptedStringConverter.class)
    private String country;
    @Convert(converter = EncryptedStringConverter.class)
    private String housenumber;
    private int plz;
    private int ik;
    @Convert(converter = EncryptedLocalDateConverter.class)
    private LocalDate birthDate;

    @Transient
    private Map<String, TagList> tags = new HashMap<>();

    protected Person() {
        this.tags = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
    }

    public Person(String firstname, String lastname, String street, String country, String housenumber, int plz,
            int ik, LocalDate birthDate) {
        this();
        this.firstname = firstname;
        this.lastname = lastname;
        this.street = street;
        this.country = country;
        this.housenumber = housenumber;
        this.plz = plz;
        this.ik = ik;
        this.birthDate = birthDate;
    }

    public TagList getTagList(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return tags.get(name);
    }
}
