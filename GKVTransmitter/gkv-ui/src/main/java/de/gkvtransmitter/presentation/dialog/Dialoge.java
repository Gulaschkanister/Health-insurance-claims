package de.gkvtransmitter.presentation.dialog;

import de.gkvtransmitter.validator.ValidationReport;

/**
 * Alles, was die Masken an den Anwender melden.
 *
 * <p>Die Meldungen liegen bewusst hinter einer Schnittstelle. Ein
 * {@code Alert.showAndWait()} haelt den aufrufenden Faden an, bis jemand den
 * Dialog schliesst - in einem Test kaeme niemand. Ohne diese Naht liesse sich
 * kein einziger Ablauf pruefen, der im Fehlerfall etwas anzeigt, und das sind
 * die Ablaeufe, auf die es ankommt.</p>
 *
 * <p>Im Betrieb steht dahinter {@link JavaFxDialoge}, im Test ein
 * aufzeichnender Ersatz, der die Meldungen sammelt statt sie zu zeigen.</p>
 */
public interface Dialoge {

    /** Meldet etwas Gelungenes oder eine Voraussetzung, die noch fehlt. */
    void zeigeInfo(String titel, String nachricht);

    /** Meldet einen Fehlschlag. */
    void zeigeFehler(String titel, String nachricht);

    /**
     * Zeigt einen Pruefbericht mit allen Beanstandungen.
     *
     * <p>Die Aufbereitung liegt in {@link Pruefberichttext} und damit
     * ausserhalb jeder Oberflaechentechnik - so laesst sich der Wortlaut ohne
     * laufendes JavaFX pruefen.</p>
     */
    default void zeigePruefbericht(ValidationReport bericht) {
        zeigeFehler(Pruefberichttext.TITEL, Pruefberichttext.formatiere(bericht));
    }
}
