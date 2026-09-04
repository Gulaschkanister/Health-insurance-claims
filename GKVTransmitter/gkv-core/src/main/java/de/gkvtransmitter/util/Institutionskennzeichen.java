package de.gkvtransmitter.util;

/**
 * Pruefung eines Institutionskennzeichens (IK).
 *
 * <p>Das IK identifiziert Krankenkassen und Leistungserbringer im
 * Abrechnungsverkehr und ist neunstellig aufgebaut:</p>
 *
 * <pre>
 *   1 0 8 3 1 0 4 0 0
 *   | |         | | |
 *   | |         | | +-- Stelle 9: Pruefziffer
 *   | +---------+------ Stellen 3-8: Regionalbereich und Seriennummer
 *   +------------------ Stellen 1-2: Klassifikation
 * </pre>
 *
 * <p>Die Pruefziffer entsteht aus den Stellen 3 bis 8: diese werden abwechselnd
 * mit 2 und 1 gewichtet (beginnend mit 2 auf Stelle 3), von jedem Produkt wird
 * die Quersumme gebildet, und die letzte Ziffer der Summe ist die Pruefziffer.</p>
 *
 * <p>Der Nutzen liegt im Versand: ein IK mit falscher Pruefziffer laesst die
 * Kasse die gesamte Lieferung abweisen. Der Fehler faellt so schon vor dem
 * Verschicken auf und nicht erst ueber ein Fehlerprotokoll Tage spaeter.</p>
 */
public final class Institutionskennzeichen {

    /** Ein IK hat genau neun Stellen. */
    public static final int LAENGE = 9;

    private static final int ERSTE_PRUEFSTELLE = 3;
    private static final int LETZTE_PRUEFSTELLE = 8;

    private Institutionskennzeichen() {
    }

    /**
     * Prueft ein IK auf Laenge und Pruefziffer.
     *
     * @param ik das Kennzeichen als Text, ohne Trennzeichen
     * @return {@code true}, wenn Laenge und Pruefziffer stimmen
     */
    public static boolean istGueltig(String ik) {
        if (ik == null) {
            return false;
        }
        String bereinigt = ik.trim();
        if (bereinigt.length() != LAENGE || !bereinigt.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int erwartet = berechnePruefziffer(bereinigt);
        int vorhanden = Character.getNumericValue(bereinigt.charAt(LAENGE - 1));
        return erwartet == vorhanden;
    }

    /** Prueft ein IK in Zahlenform. Fuehrende Nullen werden ergaenzt. */
    public static boolean istGueltig(long ik) {
        if (ik < 0) {
            return false;
        }
        return istGueltig(String.format("%09d", ik));
    }

    /**
     * Berechnet die Pruefziffer zu den Stellen 3 bis 8.
     *
     * @param ik ein mindestens achtstelliger Zifferntext
     * @return die erwartete Pruefziffer 0 bis 9
     */
    public static int berechnePruefziffer(String ik) {
        if (ik == null || ik.length() < LETZTE_PRUEFSTELLE) {
            throw new IllegalArgumentException(
                    "Fuer die Pruefziffer werden mindestens " + LETZTE_PRUEFSTELLE + " Stellen benoetigt");
        }
        int summe = 0;
        for (int stelle = ERSTE_PRUEFSTELLE; stelle <= LETZTE_PRUEFSTELLE; stelle++) {
            int ziffer = Character.getNumericValue(ik.charAt(stelle - 1));
            // Stelle 3 wird mit 2 gewichtet, danach abwechselnd 1 und 2.
            int gewicht = (stelle - ERSTE_PRUEFSTELLE) % 2 == 0 ? 2 : 1;
            int produkt = ziffer * gewicht;
            summe += quersumme(produkt);
        }
        return summe % 10;
    }

    private static int quersumme(int wert) {
        return wert / 10 + wert % 10;
    }
}
