package de.gkvtransmitter.repository;

/**
 * Eine fortlaufende Nummer, die nur einmal vergeben werden darf.
 *
 * <p>Zwei davon gibt es, und sie beantworten verschiedene Fragen:</p>
 *
 * <ul>
 *   <li>Die <b>Datenaustauschreferenz</b> im {@code UNB} unterscheidet
 *       Uebermittlungen desselben Absenders. "Datenaustauschreferenz doppelt
 *       vergeben" ist ein dokumentierter Abweisungsgrund.</li>
 *   <li>Die <b>Rechnungsnummer</b> im {@code REC} unterscheidet Rechnungen.
 *       Auf sie und das Rechnungsdatum verweist eine spaetere Korrektur
 *       (Anlage 1, Abschnitt 5.5.3, Segment {@code URI}) - ohne sie laesst
 *       sich keine Rechnung stornieren.</li>
 * </ul>
 *
 * <p>Die Kennung ist der Schluessel der Zeile in {@code dta_counter} und
 * <b>darf sich nie aendern</b>: Eine verschobene Kennung faengt wieder bei 1
 * an, und damit waeren Nummern doppelt vergeben, die es schon gab.</p>
 */
public enum Nummernkreis {

    /** Die Datenaustauschreferenz im {@code UNB}; seit je Zeile 1. */
    DATENAUSTAUSCHREFERENZ(1L),

    /** Die Rechnungsnummer im {@code REC}. */
    RECHNUNGSNUMMER(2L);

    private final long kennung;

    Nummernkreis(long kennung) {
        this.kennung = kennung;
    }

    /** Der Schluessel der Zeile in {@code dta_counter}. */
    public long kennung() {
        return kennung;
    }
}
