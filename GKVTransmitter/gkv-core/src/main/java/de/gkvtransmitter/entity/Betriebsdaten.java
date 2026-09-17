package de.gkvtransmitter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Wer die Anwendung betreibt - die absendende Stelle.
 *
 * <p><b>Warum es diese Tabelle gibt.</b> Der Absender einer Datei stammte
 * bisher aus dem Dienstleister, der je Abrechnung ausgewaehlt wird, als waere
 * die absendende Stelle eine Eigenschaft des einzelnen Vorgangs. Sie ist es
 * nicht: Sie gehoert zum Betrieb, sie aendert sich fast nie, und sie wird bei
 * <b>jeder</b> Registrierung wieder gebraucht - bei der ARGE·IK, beim
 * ITSG-Trust-Center und bei jeder Datenannahmestelle.</p>
 *
 * <p>Fuer eine allein arbeitende Hebamme ist das dasselbe IK und faellt nicht
 * auf. Sobald eine zweite Person abrechnet oder eine Abrechnungsstelle
 * einspringt, ist es der Unterschied zwischen richtiger und falscher Datei.</p>
 *
 * <p><b>Die meisten Felder hier stehen in keiner Nachricht.</b> Anlage 1 kennt
 * kein Feld fuer Steuernummer oder Ansprechpartner. Die Maske ist insofern
 * kein Nachrichtenlieferant, sondern ein <em>Aktenordner</em>: Die Anmeldungen
 * fragen im Kern dieselben Angaben ab, und die liegen heute auf Papier, in
 * einer alten E-Mail oder im Kopf. Zwei Angaben sind die Ausnahme und gehen
 * sehr wohl in die Datei - das {@link #ik} und die {@link #selbstabrechner
 * Rolle}: ohne sie ist der logische Dateiname nicht bildbar.</p>
 *
 * <p><b>Keine Bankverbindung.</b> Sie war vorgesehen und ist bewusst nicht
 * hier: Anlage 1 kennt kein Feld dafuer, die Kasse zahlt auf das Konto, das
 * zum Institutionskennzeichen hinterlegt ist, und hinterlegt wird es bei der
 * Anmeldung. Als erste wirklich schuetzenswerte Angabe ausserhalb der
 * Patientendaten gehoert sie ausserdem nicht in eine unverschluesselte
 * Datenbank - die Festplattenverschluesselung sollte stehen, bevor hier eine
 * Kontoverbindung eingetragen wird. Dasselbe gilt erst recht fuer
 * Zugangsdaten und private Schluessel: die gehoeren in den
 * Anmeldeinformationsspeicher, nicht hierher. Ein Ablaufdatum ist
 * unbedenklich, ein Schluessel nicht.</p>
 */
@Entity
@Table(name = "betriebsdaten")
public class Betriebsdaten {

    /** Es gibt genau einen Betrieb je Ablage, wie beim {@link DtaCounter}. */
    @Id
    private Long id = 1L;

    @Column(name = "praxisname")
    private String praxisname;

    @Column(name = "strasse")
    private String strasse;

    @Column(name = "hausnummer")
    private String hausnummer;

    @Column(name = "plz")
    private String plz;

    @Column(name = "ort")
    private String ort;

    /** Das eigene Institutionskennzeichen; Absender im UNB. */
    @Column(name = "ik")
    private String ik;

    /**
     * Ob der Leistungserbringer selbst abrechnet.
     *
     * <p>Neunte Stelle des logischen Dateinamens: {@code S} oder {@code A}.
     * Vorbelegt mit {@code true} - wer eine Abrechnungsstelle beauftragt,
     * betreibt diese Anwendung in aller Regel nicht selbst.</p>
     */
    @Column(name = "selbstabrechner")
    private boolean selbstabrechner = true;

    @Column(name = "steuernummer")
    private String steuernummer;

    @Column(name = "ansprechpartner")
    private String ansprechpartner;

    @Column(name = "telefon")
    private String telefon;

    @Column(name = "email")
    private String email;

    /** Ablaufdatum des Zertifikats, {@code yyyy-MM-dd}, fuer die Ablaufwarnung. */
    @Column(name = "zertifikat_bis")
    private String zertifikatBis;

    public Betriebsdaten() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPraxisname() {
        return praxisname;
    }

    public void setPraxisname(String praxisname) {
        this.praxisname = praxisname;
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    public String getHausnummer() {
        return hausnummer;
    }

    public void setHausnummer(String hausnummer) {
        this.hausnummer = hausnummer;
    }

    public String getPlz() {
        return plz;
    }

    public void setPlz(String plz) {
        this.plz = plz;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getIk() {
        return ik;
    }

    public void setIk(String ik) {
        this.ik = ik;
    }

    public boolean istSelbstabrechner() {
        return selbstabrechner;
    }

    public void setSelbstabrechner(boolean selbstabrechner) {
        this.selbstabrechner = selbstabrechner;
    }

    public String getSteuernummer() {
        return steuernummer;
    }

    public void setSteuernummer(String steuernummer) {
        this.steuernummer = steuernummer;
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

    public String getZertifikatBis() {
        return zertifikatBis;
    }

    public void setZertifikatBis(String zertifikatBis) {
        this.zertifikatBis = zertifikatBis;
    }

    /**
     * Ob die Angaben reichen, um eine Datei zu adressieren.
     *
     * <p>Alles Weitere ist fuer die Anmeldungen da und haelt keinen Versand
     * auf.</p>
     */
    public boolean sindVersandtauglich() {
        return ik != null && !ik.isBlank();
    }
}
