package de.gkvtransmitter.dta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import de.gkvtransmitter.entity.Protokolleintrag;

/**
 * Worauf sich eine Korrektur bezieht - das {@code URI}-Segment.
 *
 * <p>Anlage 1, Abschnitt 5.5.3.1. Es steht in der Leistungsaufstellung
 * <b>zwischen {@code INV} und {@code NAD}</b> (Abschnitt 7.3) und traegt:</p>
 *
 * <ol>
 *   <li>das <b>urspruengliche IK des Leistungserbringers</b> aus dem
 *       {@code FKT} der Ursprungsrechnung, 9 Stellen, Pflicht;</li>
 *   <li>die <b>urspruengliche Rechnungsnummer</b> als Gruppe aus Sammel- und
 *       Einzelnummer, wie im {@code REC};</li>
 *   <li>das <b>urspruengliche Rechnungsdatum</b>, {@code JJJJMMTT};</li>
 *   <li>die <b>urspruengliche Belegnummer</b> aus dem {@code INV}.</li>
 * </ol>
 *
 * <p><b>Alle vier kommen aus dem Uebermittlungsprotokoll</b> (C1 und D4).
 * Vorher hielt das Programm nichts davon fest - eine Korrektur haette auf
 * {@code 00000000} verwiesen, und das ist keine.</p>
 *
 * <p><b>Voraussetzung ist ein erfolgreicher Dateneingang</b> der
 * Ursprungsrechnung: Anlage 1, Abschnitt 7.2 - "dass die Ursprungsrechnung
 * [...] die Pruefstufen 1-3 bei der Datenannahme fehlerfrei durchlaufen hat".
 * Eine <em>zurueckgewiesene</em> Rechnung ist deshalb <b>kein</b> Fall fuer
 * das Korrekturverfahren; sie wird als Erstabrechnung erneut gesendet.</p>
 *
 * @param leistungserbringerIk das IK aus dem {@code FKT} der Ursprungsrechnung
 * @param rechnungsnummer      die Sammel-Rechnungsnummer der Ursprungsrechnung
 * @param rechnungsdatum       ihr Rechnungsdatum
 * @param belegnummer          ihre Belegnummer
 */
public record Korrekturbezug(String leistungserbringerIk, long rechnungsnummer,
        LocalDate rechnungsdatum, int belegnummer) {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Der Bezug auf einen Eintrag des Uebermittlungsprotokolls.
     *
     * @return der Bezug, oder {@code null}, wenn der Eintrag die noetigen
     *         Angaben nicht fuehrt - etwa weil er vor D4 entstanden ist
     */
    public static Korrekturbezug ausProtokoll(Protokolleintrag eintrag) {
        if (eintrag == null || eintrag.getRechnungsnummer() <= 0
                || eintrag.getRechnungsdatum() == null) {
            return null;
        }
        return new Korrekturbezug(eintrag.getAbsenderIk(), eintrag.getRechnungsnummer(),
                eintrag.getRechnungsdatum(), eintrag.getBelegnummer());
    }

    /** Das Segment, wie es in der Nachricht steht. */
    public String alsSegment() {
        Objects.requireNonNull(rechnungsdatum, "rechnungsdatum must not be null");
        return String.format("URI+%s+%08d:0+%s+%d'",
                leistungserbringerIk == null ? "" : leistungserbringerIk.trim(),
                rechnungsnummer, rechnungsdatum.format(DATUM), belegnummer);
    }

    /** Wie der Bezug auf dem Bildschirm heisst. */
    public String beschreibung() {
        return "Rechnung Nr. " + String.format("%08d", rechnungsnummer)
                + " vom " + rechnungsdatum.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
