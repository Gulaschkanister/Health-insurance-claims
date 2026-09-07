package de.gkvtransmitter.dta;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Der logische Dateiname, der im UNB als Anwendungsreferenz steht.
 *
 * <p>Er ist nicht frei waehlbar. Anhang 1 zur Anlage 1, Abschnitt 4.2 gibt
 * genau elf Stellen vor:</p>
 *
 * <pre>
 * Stellen  1 - 2   "SL" fuer Sonstige Leistungserbringer
 * Stellen  3 - 8   Stellen 3 bis 8 des Absender-IK
 * Stelle   9       "S" Selbstabrechner, "A" Abrechnungsstelle
 * Stellen 10 - 11  Nummer des Abrechnungsmonats
 * </pre>
 *
 * <p><b>Bis zum 07.09.2026 stand dort {@code HEB} plus Datum plus laufende
 * Nummer</b> - eine Erfindung, die die richtige Laenge hatte und sonst nichts.
 * Sie war der Belegnummer nachempfunden, und die darf tatsaechlich frei
 * gebildet werden; die Anwendungsreferenz nicht.</p>
 *
 * <p>Derselbe Name gehoert nach Abschnitt 4.2 auch in das Feld "Dateiname" der
 * Auftragsdatei, und beide muessen uebereinstimmen. Solange es die
 * Auftragsdatei nicht gibt, faellt eine Abweichung nirgends auf - das ist ein
 * Grund mehr, den Namen jetzt richtig zu bilden und nicht spaeter an zwei
 * Stellen zugleich zu berichtigen.</p>
 */
public final class LogischerDateiname {

    /** Absenderklassifikation: Sonstige Leistungserbringer. */
    private static final String KLASSIFIKATION = "SL";

    /** Neuntes Zeichen, wenn der Leistungserbringer selbst abrechnet. */
    public static final char SELBSTABRECHNER = 'S';

    /** Neuntes Zeichen, wenn eine Abrechnungsstelle absendet. */
    public static final char ABRECHNUNGSSTELLE = 'A';

    /** Vorgeschriebene Laenge. */
    public static final int LAENGE = 11;

    private LogischerDateiname() {
    }

    /**
     * Bildet den Namen.
     *
     * @param absenderIk    IK der absendenden Stelle; die Stellen 3 bis 8
     *                      gehen ein - Regionalschluessel und Seriennummer
     * @param selbstAbrechnend ob der Leistungserbringer selbst absendet
     * @param abrechnungsmonat der Monat, fuer den abgerechnet wird
     */
    public static String bilde(String absenderIk, boolean selbstAbrechnend, LocalDate abrechnungsmonat) {
        return KLASSIFIKATION
                + kernDesIk(absenderIk)
                + (selbstAbrechnend ? SELBSTABRECHNER : ABRECHNUNGSSTELLE)
                + String.format(Locale.ROOT, "%02d", abrechnungsmonat.getMonthValue());
    }

    /**
     * Die Stellen 3 bis 8 des IK.
     *
     * <p>Ist das IK kuerzer als vorgesehen oder fehlt es, wird auf Nullen
     * aufgefuellt statt abgebrochen. Ein zu kurzer Name waere schlimmer als
     * einer mit Nullen: er verschoebe im UNB nichts, aber die Datei liefe in
     * Pruefstufe 2 auf eine Feldlaenge auf, und die Meldung nennte dann das
     * Feld und nicht die fehlende Stammangabe.</p>
     */
    private static String kernDesIk(String absenderIk) {
        String ziffern = absenderIk == null ? "" : absenderIk.replaceAll("\\D", "");
        String kern = ziffern.length() >= 8 ? ziffern.substring(2, 8)
                : (ziffern.length() > 2 ? ziffern.substring(2) : "");
        return (kern + "000000").substring(0, 6);
    }
}
