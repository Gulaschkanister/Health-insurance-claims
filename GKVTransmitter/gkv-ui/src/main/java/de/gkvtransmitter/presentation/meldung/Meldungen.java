package de.gkvtransmitter.presentation.meldung;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import de.gkvtransmitter.validator.ValidationReport;

/**
 * Alles, was die Masken dem Anwender mitteilen oder von ihm erfragen.
 *
 * <p>Nichts davon oeffnet mehr ein Fenster. Meldungen erscheinen in der Ecke
 * oben rechts und gehen von selbst wieder; nur was eine Entscheidung verlangt
 * oder schiefgegangen ist, bleibt stehen.</p>
 *
 * <p>Alle Methoden geben nichts zurueck. Das ist der wesentliche Unterschied
 * zum frueheren Zuschnitt: {@code Optional<T> waehleAus(...)} musste den
 * aufrufenden Faden anhalten, bis jemand geklickt hatte - und genau dieses
 * Anhalten ist das Fenster. Wer eine Antwort braucht, uebergibt hier, was
 * damit geschehen soll.</p>
 *
 * <p>Im Betrieb steht dahinter {@link Bildschirmmeldungen}, im Test ein
 * aufzeichnender Ersatz.</p>
 */
public interface Meldungen {

    /** Etwas hat geklappt. Vergeht von selbst. */
    void erfolg(String text);

    /** Etwas fehlt noch oder ist zu beachten. Vergeht von selbst. */
    void hinweis(String text);

    /** Etwas ist schiefgegangen. Bleibt stehen, bis es weggeklickt wird. */
    void fehler(String text);

    /**
     * Zeigt alle Beanstandungen einer nicht bestandenen Pruefung.
     *
     * <p>Nicht als Fliesstext, sondern als Liste: wer eine Abrechnung
     * berichtigt, arbeitet sie Punkt fuer Punkt ab.</p>
     */
    void pruefbericht(ValidationReport bericht);

    /**
     * Stellt eine Rueckfrage.
     *
     * @param bejahenBeschriftung was auf der bestaetigenden Schaltflaeche steht,
     *        etwa "Loeschen" - nicht "Ja". Wer nur "Ja" liest, weiss nicht mehr,
     *        wozu.
     * @param wennBejaht laeuft nur bei Zustimmung; bei Ablehnung geschieht nichts
     */
    void frageNach(String frage, String bejahenBeschriftung, Runnable wennBejaht);

    /**
     * Laesst einen Eintrag aus einer Liste waehlen.
     *
     * @param wennGewaehlt laeuft mit dem gewaehlten Eintrag; bei Abbruch nicht
     */
    <T> void waehleAus(String titel, String text, List<T> eintraege,
            Function<T, String> anzeige, Consumer<T> wennGewaehlt);
}
