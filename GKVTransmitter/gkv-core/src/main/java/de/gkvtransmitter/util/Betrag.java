package de.gkvtransmitter.util;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Liest Geldbetraege in deutscher wie in englischer Schreibweise.
 *
 * <p>Bis zum 05.09.2026 gab es dafuer zwei Stellen mit unterschiedlichem
 * Ergebnis: {@code Leistungsparameter} las {@code 12,50} anstandslos,
 * {@code FieldValidator} wies es zurueck, weil er den Wert unbesehen an
 * {@code new BigDecimal(...)} weiterreichte. Die Wirkung war ein Formular, das
 * unter dem Feld "mit Komma und zwei Nachkommastellen" verlangte und die
 * Eingabe dann beanstandete. Wer stattdessen einen Punkt setzte, kam durch -
 * und derselbe Wert wurde spaeter wieder als deutsches Format gelesen.</p>
 *
 * <p>Jetzt gibt es nur noch diese eine Auslegung.</p>
 */
public final class Betrag {

    private Betrag() {
    }

    /**
     * Liest einen Betrag.
     *
     * <p>Der Punkt ist mehrdeutig: in {@code 15.000,00} trennt er Tausender, in
     * {@code 1234.56} die Nachkommastellen. Entschieden wird am Komma - ist
     * eines vorhanden, ist es das Dezimaltrennzeichen und Punkte trennen
     * Tausender; fehlt es, ist der Punkt das Dezimaltrennzeichen. Ein Wert wie
     * {@code 15.000} ohne Komma wird damit als 15,0 gelesen. Das ist bewusst
     * so: die andere Auslegung wuerde {@code 1234.56} um den Faktor 100
     * verfaelschen, was auf einer Rechnung schwerer wiegt.</p>
     *
     * @param wert die Eingabe, auch {@code null} oder leer
     * @return der Betrag, oder leer wenn sich keiner lesen laesst
     */
    public static Optional<BigDecimal> lese(String wert) {
        if (wert == null || wert.isBlank()) {
            return Optional.empty();
        }
        String ohneLeerzeichen = wert.trim().replace(" ", "");
        String normalisiert = ohneLeerzeichen.indexOf(',') >= 0
                ? ohneLeerzeichen.replace(".", "").replace(',', '.')
                : ohneLeerzeichen;
        try {
            return Optional.of(new BigDecimal(normalisiert));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
