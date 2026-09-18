package de.gkvtransmitter.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ein Eintrag im Uebermittlungsprotokoll.
 *
 * <p><b>Das ist keine Bequemlichkeit, sondern eine Pflicht.</b> Anlage 1,
 * Abschnitt 3 Absatz 2: „Ueber den Datenaustausch ist eine Dokumentation zu
 * fuehren. Die Dokumentation ist mindestens 2 Jahre aufzubewahren. Dabei sind
 * alle Schritte von der Initiierung bis ggf. zur Quittierung der Uebernahme
 * sowie der Weiterverarbeitung zu dokumentieren."</p>
 *
 * <p>Die Mindestinhalte zaehlt Anhang 1, Abschnitt 4.5 auf. Jedem davon
 * entspricht hier ein Feld:</p>
 *
 * <table border="1">
 *   <caption>Anhang 1, Abschnitt 4.5</caption>
 *   <tr><th>Pflichtinhalt</th><th>Feld</th></tr>
 *   <tr><td>Inhalt der Datenlieferung (Physikalischer Dateiname)</td><td>{@link #dateiname}</td></tr>
 *   <tr><td>Erstellungsdatum der Datei</td><td>{@link #erstelltAm}</td></tr>
 *   <tr><td>Lfd. Nummer der Datenuebermittlung</td><td>{@link #laufendeNummer}</td></tr>
 *   <tr><td>Eindeutige Bezeichnung der Kommunikationspartner</td>
 *       <td>{@link #absenderIk}, {@link #empfaengerIk}</td></tr>
 *   <tr><td>Beginn und Ende der Datenuebermittlung</td><td>{@link #beginn}, {@link #ende}</td></tr>
 *   <tr><td>Dateigroesse</td><td>{@link #groesseBytes}</td></tr>
 *   <tr><td>Verarbeitungshinweise</td><td>{@link #hinweise}</td></tr>
 *   <tr><td>Senden/Empfangen</td><td>{@link #richtung}</td></tr>
 *   <tr><td>fehlerfrei/fehlerhaft</td><td>{@link #fehlerfrei}</td></tr>
 *   <tr><td>wenn fehlerhaft: Fehlerstatus</td><td>{@link #fehlerstatus}</td></tr>
 * </table>
 *
 * <p>Dazu kommt ein Feld, das nicht aus Abschnitt 4.5 stammt, sondern aus
 * Absatz 4 desselben Abschnitts 3: <b>{@link #bezahltAm}</b>. Dort steht, eine
 * Sicherungskopie sei „bis zur Bezahlung vorzuhalten". Ohne einen Zeitpunkt,
 * ab dem das erfuellt ist, weiss niemand, wann die Kopien unter
 * {@code staging/} weg duerfen - und niemand sieht, worauf noch Geld
 * aussteht.</p>
 */
@Entity
@Table(name = "uebermittlungsprotokoll")
public class Protokolleintrag {

    /** Richtung der Uebermittlung, Anhang 1 Abschnitt 4.5. */
    public enum Richtung {
        /** Vom Betrieb an die Annahmestelle. */
        SENDEN,
        /** Rueckmeldung der Kasse. */
        EMPFANGEN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Der Name, unter dem die Datei uebertragen wurde. */
    @Column(name = "dateiname", nullable = false)
    private String dateiname;

    @Column(name = "erstellt_am", nullable = false)
    private OffsetDateTime erstelltAm;

    /** Die Datenaustauschreferenz aus dem UNB - je Absender eindeutig. */
    @Column(name = "laufende_nummer")
    private long laufendeNummer;

    @Column(name = "absender_ik")
    private String absenderIk;

    @Column(name = "empfaenger_ik")
    private String empfaengerIk;

    @Column(name = "beginn")
    private OffsetDateTime beginn;

    @Column(name = "ende")
    private OffsetDateTime ende;

    @Column(name = "groesse_bytes")
    private long groesseBytes;

    @Column(name = "hinweise")
    private String hinweise;

    @Column(name = "richtung", nullable = false)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Richtung richtung = Richtung.SENDEN;

    @Column(name = "fehlerfrei")
    private boolean fehlerfrei = true;

    @Column(name = "fehlerstatus")
    private String fehlerstatus;

    /** Wo die Sicherungskopie liegt, solange sie vorzuhalten ist. */
    @Column(name = "sicherungskopie")
    private String sicherungskopie;

    /**
     * Die Rechnungsnummer aus dem {@code REC}.
     *
     * <p>Nicht aus Anhang 1 Abschnitt 4.5, sondern aus Abschnitt 7.3: Eine
     * Korrekturrechnung verweist ueber {@code URI} auf Rechnungsnummer,
     * Rechnungsdatum und Belegnummer der Ursprungsrechnung. <b>Ohne sie im
     * Protokoll liesse sich keine versendete Rechnung mehr stornieren.</b></p>
     */
    @Column(name = "rechnungsnummer")
    private long rechnungsnummer;

    /** Die Belegnummer aus dem {@code INV} - eindeutig innerhalb der Rechnung. */
    @Column(name = "belegnummer")
    private int belegnummer;

    /**
     * Das Rechnungsdatum aus dem {@code REC}.
     *
     * <p>Nicht dasselbe wie {@link #erstelltAm}: Das ist der Zeitpunkt der
     * Uebermittlung, dies das Datum der Rechnung. Eine Korrektur verweist auf
     * dieses.</p>
     */
    @Column(name = "rechnungsdatum")
    private java.time.LocalDate rechnungsdatum;

    /**
     * Wofuer diese Lieferung stand, Anlage 3 Abschnitt 8.1.7.
     *
     * <p>Gebraucht, weil sich Korrekturen ketten lassen: Auf eine
     * Nachforderung ({@code 02}) ist wieder eine Korrekturrechnung moeglich
     * (Anlage 1, Abschnitt 7.2). Ohne das Kennzeichen liesse sich nicht sagen,
     * worauf ein Bezug zulaessig ist.</p>
     */
    @Column(name = "verarbeitungskennzeichen")
    private String verarbeitungskennzeichen = "01";

    /** Wann bezahlt wurde; {@code null}, solange die Zahlung aussteht. */
    @Column(name = "bezahlt_am")
    private OffsetDateTime bezahltAm;

    public Protokolleintrag() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDateiname() {
        return dateiname;
    }

    public void setDateiname(String dateiname) {
        this.dateiname = dateiname;
    }

    public OffsetDateTime getErstelltAm() {
        return erstelltAm;
    }

    public void setErstelltAm(OffsetDateTime erstelltAm) {
        this.erstelltAm = erstelltAm;
    }

    public long getLaufendeNummer() {
        return laufendeNummer;
    }

    public void setLaufendeNummer(long laufendeNummer) {
        this.laufendeNummer = laufendeNummer;
    }

    public String getAbsenderIk() {
        return absenderIk;
    }

    public void setAbsenderIk(String absenderIk) {
        this.absenderIk = absenderIk;
    }

    public String getEmpfaengerIk() {
        return empfaengerIk;
    }

    public void setEmpfaengerIk(String empfaengerIk) {
        this.empfaengerIk = empfaengerIk;
    }

    public OffsetDateTime getBeginn() {
        return beginn;
    }

    public void setBeginn(OffsetDateTime beginn) {
        this.beginn = beginn;
    }

    public OffsetDateTime getEnde() {
        return ende;
    }

    public void setEnde(OffsetDateTime ende) {
        this.ende = ende;
    }

    public long getGroesseBytes() {
        return groesseBytes;
    }

    public void setGroesseBytes(long groesseBytes) {
        this.groesseBytes = groesseBytes;
    }

    public String getHinweise() {
        return hinweise;
    }

    public void setHinweise(String hinweise) {
        this.hinweise = hinweise;
    }

    public Richtung getRichtung() {
        return richtung;
    }

    public void setRichtung(Richtung richtung) {
        this.richtung = richtung;
    }

    public boolean istFehlerfrei() {
        return fehlerfrei;
    }

    public void setFehlerfrei(boolean fehlerfrei) {
        this.fehlerfrei = fehlerfrei;
    }

    public String getFehlerstatus() {
        return fehlerstatus;
    }

    public void setFehlerstatus(String fehlerstatus) {
        this.fehlerstatus = fehlerstatus;
    }

    public String getSicherungskopie() {
        return sicherungskopie;
    }

    public void setSicherungskopie(String sicherungskopie) {
        this.sicherungskopie = sicherungskopie;
    }

    public long getRechnungsnummer() {
        return rechnungsnummer;
    }

    public void setRechnungsnummer(long rechnungsnummer) {
        this.rechnungsnummer = rechnungsnummer;
    }

    public int getBelegnummer() {
        return belegnummer;
    }

    public void setBelegnummer(int belegnummer) {
        this.belegnummer = belegnummer;
    }

    public java.time.LocalDate getRechnungsdatum() {
        return rechnungsdatum;
    }

    public void setRechnungsdatum(java.time.LocalDate rechnungsdatum) {
        this.rechnungsdatum = rechnungsdatum;
    }

    public String getVerarbeitungskennzeichen() {
        return verarbeitungskennzeichen;
    }

    public void setVerarbeitungskennzeichen(String verarbeitungskennzeichen) {
        this.verarbeitungskennzeichen = verarbeitungskennzeichen;
    }

    /**
     * Ob sich auf diese Lieferung eine Korrektur beziehen darf.
     *
     * <p>Anlage 1, Abschnitt 7.2: Voraussetzung ist der <b>erfolgreiche
     * Dateneingang</b> der Ursprungsrechnung - sie muss "die Pruefstufen 1-3
     * bei der Datenannahme fehlerfrei durchlaufen" haben. Was hier als
     * fehlerhaft vermerkt ist, wurde nie angenommen und ist deshalb kein Fall
     * fuer das Korrekturverfahren, sondern fuer eine erneute
     * Erstabrechnung.</p>
     */
    public boolean korrigierbar() {
        return fehlerfrei && richtung == Richtung.SENDEN && rechnungsnummer > 0
                && rechnungsdatum != null;
    }

    public OffsetDateTime getBezahltAm() {
        return bezahltAm;
    }

    public void setBezahltAm(OffsetDateTime bezahltAm) {
        this.bezahltAm = bezahltAm;
    }

    /**
     * Ob die Sicherungskopie noch vorzuhalten ist.
     *
     * <p>Anlage 1, Abschnitt 3 Absatz 4: bis zur Bezahlung. Eine fehlerhafte
     * Uebermittlung wartet ebenfalls - sie ist erst recht nicht bezahlt.</p>
     */
    public boolean wartetAufZahlung() {
        return richtung == Richtung.SENDEN && bezahltAm == null;
    }
}
