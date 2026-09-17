package de.gkvtransmitter.wartung;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Etwas, das zu tun ist - mit oder ohne bekanntes Datum.
 *
 * <p><b>Der Fall ohne Datum ist der wichtigere.</b> Ein Kalender, der nur
 * zeigt, was er weiss, sieht vollstaendig aus und ist es nicht. Was faellig
 * wird, ohne dass die Anwendung den Termin kennt, gehoert genauso auf die
 * Seite - mit dem Grund, warum kein Datum dasteht.</p>
 *
 * @param was       worum es geht
 * @param rhythmus  wie oft, in Worten
 * @param faelligAm wann, oder {@code null}
 * @param woher     woher das Datum kommt oder warum keines dasteht
 */
public record Faelligkeit(String was, String rhythmus, LocalDate faelligAm, String woher) {

    /** Ob ein Datum bekannt ist. */
    public boolean hatDatum() {
        return faelligAm != null;
    }

    /** Tage bis zur Faelligkeit; negativ, wenn sie vorbei ist. */
    public long tageBis(LocalDate heute) {
        return faelligAm == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(heute, faelligAm);
    }

    /**
     * Ob sie innerhalb der naechsten Wochen ansteht - oder schon vorbei ist.
     *
     * <p>Vorbei zaehlt mit: Ein ueberschrittener Termin ist dringender als
     * einer, der bevorsteht, und verschwindet sonst still von der Seite.</p>
     */
    public boolean stehtAn(LocalDate heute, int wochen) {
        return hatDatum() && tageBis(heute) <= (long) wochen * 7;
    }

    /** Ob der Termin verstrichen ist. */
    public boolean ueberfaellig(LocalDate heute) {
        return hatDatum() && faelligAm.isBefore(heute);
    }
}
