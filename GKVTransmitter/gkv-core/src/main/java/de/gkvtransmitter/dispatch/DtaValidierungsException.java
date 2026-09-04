package de.gkvtransmitter.dispatch;

import de.gkvtransmitter.validator.ValidationReport;

/**
 * Zeigt an, dass eine Lieferung die Pruefung nicht bestanden hat und deshalb
 * nicht versendet wurde.
 *
 * <p>Traegt den vollstaendigen Pruefbericht mit, damit die Oberflaeche alle
 * Beanstandungen auf einmal anzeigen kann und nicht nur die erste.</p>
 */
public class DtaValidierungsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ValidationReport bericht;

    public DtaValidierungsException(ValidationReport bericht) {
        super("Die Abrechnung wurde nicht versendet: " + bericht.kurzfassung()
                + System.lineSeparator() + bericht.alsText());
        this.bericht = bericht;
    }

    public ValidationReport getBericht() {
        return bericht;
    }
}
