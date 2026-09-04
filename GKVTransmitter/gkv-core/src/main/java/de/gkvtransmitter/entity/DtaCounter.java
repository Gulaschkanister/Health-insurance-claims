package de.gkvtransmitter.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dta_counter")
public class DtaCounter {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private Long nextValue = 1L;

    public DtaCounter() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNextValue() {
        return nextValue;
    }

    public void setNextValue(Long nextValue) {
        this.nextValue = nextValue;
    }
}