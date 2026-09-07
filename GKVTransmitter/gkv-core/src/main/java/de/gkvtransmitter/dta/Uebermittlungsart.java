package de.gkvtransmitter.dta;

import java.util.Locale;

/**
 * Wofuer sich eine erzeugte Datei ausgibt: Test, Erprobung oder Echtbetrieb.
 *
 * <p>Die Technische Anlage (Anlage 1 zu den Richtlinien nach § 302 SGB V,
 * Abschnitt 5.4) sieht dafuer die letzte Stelle des {@code UNB}-Segments vor:
 * <b>0 = Testdatei, 1 = Erprobungsdatei, 2 = Echtdatei</b>. Die Angabe
 * entscheidet, was die Datenannahmestelle mit der Lieferung macht.</p>
 *
 * <p><b>Bis zum 07.09.2026 stand dort fest eine 1.</b> Jede Datei bezeichnete
 * sich als Erprobungsdatei - zum damaligen Stand des Programms richtig, aber
 * niemand wusste es, und es liess sich nicht aendern. Beim ersten Echtversand
 * waere das die stillste denkbare Zurueckweisung gewesen: die Datei ist formal
 * fehlerfrei, wird gelesen, verarbeitet - und nicht bezahlt, weil sie sich
 * selbst als Erprobung ausweist.</p>
 *
 * <p>Die Reihenfolge ist auch die des Verfahrens: erst Test (Abschnitt 10 der
 * Technischen Anlage), dann die vorgeschriebene Erprobung zwischen Absender und
 * Empfaenger (Abschnitt 2), erst danach Echtbetrieb.</p>
 */
public enum Uebermittlungsart {

    /** Testdatei: zum Ausprobieren, ohne jede Wirkung. */
    TEST("0"),

    /**
     * Erprobungsdatei.
     *
     * <p>Die vorgeschriebene Stufe vor dem Echtbetrieb: "Vor der erstmaligen
     * Durchfuehrung oder vor Aenderung des Datenaustauschverfahrens ist die
     * ordnungsgemaesse Verarbeitung zwischen Absender und Empfaenger zu
     * erproben."</p>
     */
    ERPROBUNG("1"),

    /** Echtdatei: eine Forderung, die bezahlt werden soll. */
    ECHT("2");

    private final String kennzeichen;

    Uebermittlungsart(String kennzeichen) {
        this.kennzeichen = kennzeichen;
    }

    /** Die Ziffer, die im UNB steht. */
    public String kennzeichen() {
        return kennzeichen;
    }

    /**
     * Liest die Art aus einer Einstellung.
     *
     * <p><b>Was sich nicht zuordnen laesst, wird zur Erprobung</b> - nicht zum
     * Echtbetrieb. Ein Vertipper in einer Einstellungsdatei darf nicht dazu
     * fuehren, dass echte Forderungen hinausgehen; andersherum kostet er
     * hoechstens einen zweiten Versand.</p>
     */
    public static Uebermittlungsart aus(String wert) {
        if (wert == null) {
            return ERPROBUNG;
        }
        for (Uebermittlungsart art : values()) {
            if (art.name().equalsIgnoreCase(wert.trim())) {
                return art;
            }
        }
        return ERPROBUNG;
    }

    /** Der Wert, wie er in der Einstellungsdatei steht. */
    public String alsEinstellung() {
        return name().toLowerCase(Locale.GERMAN);
    }
}
