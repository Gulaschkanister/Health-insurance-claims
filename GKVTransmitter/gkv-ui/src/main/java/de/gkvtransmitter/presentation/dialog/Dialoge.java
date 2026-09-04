package de.gkvtransmitter.presentation.dialog;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import de.gkvtransmitter.validator.ValidationReport;

/**
 * Alles, was die Masken mit dem Anwender wechseln: melden, auswaehlen lassen,
 * nachfragen.
 *
 * <p>Das liegt bewusst hinter einer Schnittstelle. Ein
 * {@code Alert.showAndWait()} haelt den aufrufenden Faden an, bis jemand den
 * Dialog schliesst - in einem Test kaeme niemand. Ohne diese Naht liesse sich
 * kein einziger Ablauf pruefen, der im Fehlerfall etwas anzeigt oder vor dem
 * Loeschen nachfragt, und das sind die Ablaeufe, auf die es ankommt.</p>
 *
 * <p>Im Betrieb steht dahinter {@link JavaFxDialoge}, im Test ein
 * aufzeichnender Ersatz, der die Meldungen sammelt und vorgegebene Antworten
 * gibt.</p>
 */
public interface Dialoge {

    /** Meldet etwas Gelungenes oder eine Voraussetzung, die noch fehlt. */
    void zeigeInfo(String titel, String nachricht);

    /** Meldet einen Fehlschlag. */
    void zeigeFehler(String titel, String nachricht);

    /**
     * Laesst einen Eintrag aus einer Liste auswaehlen.
     *
     * @param anzeige wie ein Eintrag benannt wird
     * @return der gewaehlte Eintrag, oder leer bei Abbruch
     */
    <T> Optional<T> waehleAus(String titel, String text, List<T> eintraege, Function<T, String> anzeige);

    /**
     * Stellt eine Rueckfrage, die zu bejahen ist.
     *
     * @return ob zugestimmt wurde; bei Abbruch {@code false}
     */
    boolean bestaetige(String titel, String kopfzeile, String text);

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
