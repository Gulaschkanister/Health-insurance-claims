package de.gkvtransmitter.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Wer die Leistung erbringt.
 *
 * <p><b>Nicht zu verwechseln mit {@link Betriebsdaten}.</b> Die beschreiben,
 * wer die Anwendung betreibt und die Datei absendet - ein einziger Datensatz.
 * Diese Klasse beschreibt, wer am Kurs steht. Bei einer allein arbeitenden
 * Hebamme ist das dieselbe Person, in einer Praxis mit zwei Hebammen nicht.</p>
 *
 * <p>Was ueber die Anschrift hinausgeht, steht hier, weil es sich <b>fast nie
 * aendert</b>: Ohne diese Angaben waere bei jedem Abrechnungslauf erneut zu
 * tippen oder zu waehlen, was seit dem ersten Kurs feststeht.</p>
 */
@Entity
@Table(name = "ServiceProvider")
public class ServiceProvider extends Person {

    /**
     * Der Abrechnungscode nach Anlage 3, Abschnitt 8.1.5.
     *
     * <p><b>Er gehoert zum Beruf, nicht zur Kursart</b> - {@code 50} ist
     * Hebamme/Entbindungspfleger, {@code 61} Leistungserbringer von
     * Rehabilitationssport. Bis zum 17.09.2026 stand er in der Blaupause, also
     * bei der Leistung: Wer zwei Kursarten anbot, pflegte denselben Code
     * zweimal, und wer ihn in einer der beiden vergass, schickte eine
     * Lieferung, die in Pruefstufe 3 zurueckkommt.</p>
     *
     * <p>Aus ihm ergibt sich zugleich der Leistungsbereich im {@code UNB},
     * siehe {@link de.gkvtransmitter.dta.Leistungsbereich}.</p>
     */
    @Column(name = "abrechnungscode")
    private String abrechnungscode;

    public ServiceProvider() {
        super();
    }

    public ServiceProvider(String firstname, String lastname, String street, String country, String housenumber,
            int plz,
            int ik, int kassenIk, LocalDate birthDate) {
        super(firstname, lastname, street, country, housenumber, plz, ik, kassenIk, birthDate);
    }

    public String getAbrechnungscode() {
        return abrechnungscode;
    }

    public void setAbrechnungscode(String abrechnungscode) {
        this.abrechnungscode = abrechnungscode;
    }

    /**
     * Ob ein Abrechnungscode hinterlegt ist.
     *
     * <p>Leer ist der Normalfall fuer jeden Dienstleister, der vor dem
     * 17.09.2026 angelegt wurde. Dann gilt weiter, was in der Blaupause
     * steht - siehe {@code DtaDispatchService}.</p>
     */
    public boolean hatAbrechnungscode() {
        return abrechnungscode != null && !abrechnungscode.isBlank();
    }
}
