package de.gkvtransmitter.dta;

import java.util.Set;

/**
 * Wofuer eine Rechnung steht, Anlage 3 Abschnitt 8.1.7.
 *
 * <p>Das Programm erzeugte bis zum 18.09.2026 ausschliesslich {@code 01} -
 * und <b>ohne die uebrigen gibt es keinen Weg, eine bereits angenommene
 * Rechnung zu berichtigen</b>: Dieselbe Rechnung noch einmal als
 * Erstabrechnung zu schicken sieht fuer die Kasse wie eine Doppelabrechnung
 * aus.</p>
 *
 * <h2>Fuer Hebammen sind nur zwei davon zulaessig</h2>
 *
 * <p>Anlage 1, Abschnitt 7.1 fuehrt die Leistungsbereiche einzeln auf:</p>
 *
 * <table border="1">
 *   <caption>Anlage 1, Abschnitt 7.1</caption>
 *   <tr><th>Leistungsbereich</th><th>verpflichtend ab</th><th>zulaessig</th></tr>
 *   <tr><td>Heilmittel</td><td>01.07.2020</td><td>02, 03, 04</td></tr>
 *   <tr><td>Heilmittel (Physiotherapie)</td><td>01.10.2025</td><td>02, 03, 04, 10</td></tr>
 *   <tr><td><b>Hebammen</b></td><td><b>01.10.2025</b></td><td><b>02, 04</b></td></tr>
 * </table>
 *
 * <p>Zuzahlungsforderung ({@code 03}) und Wiederaufnahme ({@code 10}) gehoeren
 * also nicht hierher, so naheliegend sie aussehen. Das Korrekturverfahren ist
 * fuer Hebammen seit dem <b>01.10.2025 verpflichtend</b> - es ist keine
 * Erweiterung, sondern eine Luecke.</p>
 */
public enum Verarbeitungskennzeichen {

    /** Abrechnung ohne Besonderheiten - die Erstrechnung. */
    ERSTABRECHNUNG("01", "Abrechnung ohne Besonderheiten"),

    /** Nachforderung zu einer bereits angenommenen Rechnung. */
    NACHFORDERUNG("02", "Nachforderung"),

    /** Zuzahlungsforderung - fuer Hebammen nicht zulaessig. */
    ZUZAHLUNGSFORDERUNG("03", "Zuzahlungsforderung"),

    /** Korrekturrechnung zu einer bereits angenommenen Rechnung. */
    KORREKTURRECHNUNG("04", "Korrekturrechnung"),

    /** Wiederaufnahme - fuer Hebammen nicht zulaessig. */
    WIEDERAUFNAHME("10", "Wiederaufnahme");

    /** Was Hebammen senden duerfen, Anlage 1 Abschnitt 7.1. */
    private static final Set<String> HEBAMMEN = Set.of("01", "02", "04");

    private final String schluessel;
    private final String bedeutung;

    Verarbeitungskennzeichen(String schluessel, String bedeutung) {
        this.schluessel = schluessel;
        this.bedeutung = bedeutung;
    }

    /** Der zweistellige Wert, wie er im {@code FKT} steht. */
    public String schluessel() {
        return schluessel;
    }

    /** Wofuer der Wert steht - fuer Meldungen, die etwas sagen. */
    public String bedeutung() {
        return bedeutung;
    }

    /**
     * Ob dieses Kennzeichen ein {@code URI}-Segment verlangt.
     *
     * <p>Anlage 1, Abschnitt 7.3: "URI darf nur angegeben werden bei
     * Uebermittlung einer Rechnung mit Verarbeitungskennzeichen ungleich
     * '01'". Umgekehrt ist es dort Pflicht, sonst kann die Kasse die Korrektur
     * nicht zuordnen.</p>
     */
    public boolean brauchtUrsprungsangaben() {
        return this != ERSTABRECHNUNG;
    }

    /** Ob eine Hebamme dieses Kennzeichen senden darf, Anlage 1 Abschnitt 7.1. */
    public boolean zulaessigFuerHebammen() {
        return HEBAMMEN.contains(schluessel);
    }

    /** Ob ein Abrechnungscode Hebammenhilfe meint, Anlage 3 Abschnitt 8.1.5. */
    public static boolean istHebamme(String abrechnungscode) {
        return "50".equals(abrechnungscode == null ? null : abrechnungscode.trim());
    }

    /** Das Kennzeichen zu einem Schluessel, oder {@code null}. */
    public static Verarbeitungskennzeichen zuSchluessel(String schluessel) {
        for (Verarbeitungskennzeichen kennzeichen : values()) {
            if (kennzeichen.schluessel.equals(schluessel == null ? null : schluessel.trim())) {
                return kennzeichen;
            }
        }
        return null;
    }
}
