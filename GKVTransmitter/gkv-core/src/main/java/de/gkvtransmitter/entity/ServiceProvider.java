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

    /**
     * Steuernummer oder Umsatzsteuer-Identifikationsnummer.
     *
     * <p>Das erste Datenelement des {@code UST}-Segments, Pflicht <em>innerhalb
     * des Segments</em>: "Steuernummer gemaess § 14 Abs. 1a UStG oder
     * Umsatzsteuer-Identifikationsnummer" (Anlage 1, Abschnitt 5.5.2, hoechstens
     * 20 Stellen). Das Segment selbst ist konditional und darf je Nachricht
     * einmal vorkommen - ohne Steuernummer bleibt es also weg, statt eine
     * erfundene zu tragen.</p>
     */
    @Column(name = "steuernummer", length = 20)
    private String steuernummer;

    /**
     * Ob die Leistung von der Umsatzsteuer befreit ist.
     *
     * <p>Das dritte Datenelement des {@code UST}: <b>"J" wenn befreit gem. § 4
     * UStG</b>. Fuer Hebammenhilfe kommt § 4 Nr. 14 UStG in Betracht - ob er
     * greift, ist eine Frage an einen Menschen, aber sie haengt an der Person
     * und nicht am Kurs. Deshalb steht das Kennzeichen hier und nicht in der
     * Blaupause.</p>
     */
    @Column(name = "umsatzsteuerbefreit")
    private boolean umsatzsteuerbefreit;

    /** Wer Rueckfragen beantwortet - fuer Antraege und Anmeldungen. */
    @Column(name = "ansprechpartner")
    private String ansprechpartner;

    @Column(name = "telefon")
    private String telefon;

    @Column(name = "email")
    private String email;

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

    public String getSteuernummer() {
        return steuernummer;
    }

    public void setSteuernummer(String steuernummer) {
        this.steuernummer = steuernummer;
    }

    public boolean istUmsatzsteuerbefreit() {
        return umsatzsteuerbefreit;
    }

    public void setUmsatzsteuerbefreit(boolean umsatzsteuerbefreit) {
        this.umsatzsteuerbefreit = umsatzsteuerbefreit;
    }

    public String getAnsprechpartner() {
        return ansprechpartner;
    }

    public void setAnsprechpartner(String ansprechpartner) {
        this.ansprechpartner = ansprechpartner;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Ob sich ein {@code UST}-Segment bilden laesst.
     *
     * <p>Ohne Steuernummer nicht: Sie ist innerhalb des Segments Pflicht, und
     * das Segment ist konditional. Weglassen ist erlaubt, erfinden nicht.</p>
     */
    public boolean hatSteuerangaben() {
        return steuernummer != null && !steuernummer.isBlank();
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
