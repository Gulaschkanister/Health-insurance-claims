package de.gkvtransmitter.dta;

/**
 * Die Nummern, unter denen eine Rechnung wiederzufinden ist.
 *
 * <p><b>Beide standen fest im Quelltext.</b> Das {@code REC} trug
 * {@code 00000000:0}, die Belegnummer im {@code INV} war
 * {@code "HEB" + Jahr/Monat + Kennnummer der Teilnehmerin} - frei gebildet,
 * ohne Zusicherung ueber Jahresgrenzen und ohne Bezug zu irgendetwas
 * Vergebenem.</p>
 *
 * <h2>Was die Anlage verlangt</h2>
 *
 * <p>Anlage 1, Abschnitt 5.5.2:</p>
 *
 * <ul>
 *   <li><b>Sammel-Rechnungsnummer</b> im {@code REC}, {@code ..14} AN,
 *       Pflicht. Die Einzel-Rechnungsnummer daneben wird "auf 0 gesetzt",
 *       solange keine Abrechnungsstelle mit Inkassovollmacht absendet -
 *       daher das {@code :0}, das schon dastand und richtig war.</li>
 *   <li><b>Belegnummer</b> im {@code INV}, {@code ..10} AN, Pflicht:
 *       "Eindeutige Nummer innerhalb der Gesamtrechnung".</li>
 * </ul>
 *
 * <p><b>Die Belegnummer muss also nicht ueber Jahre eindeutig sein</b>,
 * sondern innerhalb <em>einer</em> Rechnung - das ist weniger, als die
 * Aufgabenliste annahm. Eindeutig ueber die Zeit ist die
 * <b>Rechnungsnummer</b>, und auf sie verweist eine spaetere Korrektur.</p>
 *
 * @param rechnungsnummer aus dem Nummernkreis der Datenbank
 * @param belegnummer     laufend innerhalb dieser Rechnung, beginnend bei 1
 */
public record Rechnungsangaben(long rechnungsnummer, int belegnummer) {

    /**
     * Was gilt, wenn niemand eine Nummer vergeben hat.
     *
     * <p>Rechnungsnummer {@code 0} - also genau die {@code 00000000}, die
     * vorher fest dastand. <b>Sie ist kein Ersatzwert, sondern eine Sperre:</b>
     * {@code RechnungsangabenRegel} weist sie als Fehler zurueck, und die
     * Pruefung vor dem Versand haelt den Lauf an. Dieselbe Entscheidung wie
     * beim Einzelbetrag, der auf {@code 0,00} faellt statt auf einen
     * plausiblen Betrag.</p>
     *
     * <p>Gebraucht wird sie von den kurzen {@code buildDtaFor}-Ueberladungen,
     * die es fuer Tests gibt. Der Versandweg gibt immer echte Nummern mit.</p>
     */
    public static final Rechnungsangaben OHNE = new Rechnungsangaben(0L, 1);

    /**
     * Das Feld, wie es im {@code REC} steht: Sammel- und Einzelnummer.
     *
     * <p>Achtstellig mit fuehrenden Nullen - {@code ..14} laesst mehr zu, aber
     * eine feste Breite liest sich in einer Datei besser, und achtstellig
     * reicht fuer hunderttausend Jahre Kursbetrieb.</p>
     */
    public String rechnungsnummerFeld() {
        return String.format("%08d:0", rechnungsnummer);
    }

    /** Die Belegnummer, wie sie im {@code INV} steht. */
    public String belegnummerFeld() {
        return String.valueOf(belegnummer);
    }

    /** Dieselbe Rechnung, naechster Beleg. */
    public Rechnungsangaben naechsterBeleg() {
        return new Rechnungsangaben(rechnungsnummer, belegnummer + 1);
    }
}
