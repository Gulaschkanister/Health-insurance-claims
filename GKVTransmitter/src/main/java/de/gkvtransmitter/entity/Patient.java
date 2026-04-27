package de.gkvtransmitter.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="Patient")
public class Patient {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;
    
    public Patient(){
        
    }
}
