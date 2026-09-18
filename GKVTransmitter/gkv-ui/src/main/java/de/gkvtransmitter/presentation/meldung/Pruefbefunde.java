package de.gkvtransmitter.presentation.meldung;

import java.util.ArrayList;
import java.util.List;

import de.gkvtransmitter.validator.ValidationMessage;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Bringt einen Pruefbericht in die Reihenfolge, in der man ihn abarbeitet.
 *
 * <p>Liegt bewusst ausserhalb jeder Oberflaechentechnik: welche Beanstandung
 * zuerst kommt und wie sie heisst, laesst sich so ohne laufendes JavaFX
 * pruefen. Wie die Zeilen dann aussehen, entscheidet
 * {@link Bildschirmmeldungen}.</p>
 */
public final class Pruefbefunde {

    /**
     * Wozu der Bericht gehoert.
     *
     * <p><b>Ohne diese Unterscheidung log die Meldung.</b> Der Trockenlauf
     * zeigte seinen Bericht mit der Einleitung „Die Abrechnung wurde
     * versendet." - und versendet wurde nichts. Aufgefallen ist das nicht in
     * einem Test, sondern am Bildschirmfoto des Ablaufs.</p>
     */
    public enum Lage {
        /** Der Bericht erklaert, was mit einer echten Lieferung geschehen ist. */
        VERSAND,
        /** Der Bericht gehoert zu einem Trockenlauf - es ging nichts hinaus. */
        TROCKENLAUF
    }

    /**
     * Ueberschrift der Meldung.
     *
     * <p>Ein Bericht kann auf zwei Weisen zustande kommen: als Grund, warum
     * nichts hinausging - oder als Anmerkung zu etwas, das hinausgegangen ist.
     * Beides mit „Prüfung nicht bestanden" zu ueberschreiben waere im zweiten
     * Fall schlicht falsch.</p>
     */
    public static String titel(ValidationReport bericht) {
        return bericht.hatFehler() ? "Prüfung nicht bestanden" : "Hinweise zur Abrechnung";
    }

    /** Was unter der Ueberschrift steht, bevor die Liste beginnt. */
    public static String einleitung(ValidationReport bericht) {
        return einleitung(bericht, Lage.VERSAND);
    }

    /**
     * Dasselbe, aber mit Blick darauf, ob ueberhaupt etwas hinausging.
     *
     * <p>Beim Trockenlauf ist die Frage „versendet oder nicht" gar nicht
     * gestellt - er versendet nie. Gesagt werden muss stattdessen, <b>was
     * geschehen waere</b>.</p>
     */
    public static String einleitung(ValidationReport bericht, Lage lage) {
        if (lage == Lage.TROCKENLAUF) {
            return bericht.hatFehler()
                    ? "So wäre nichts hinausgegangen — versendet wurde ohnehin nichts."
                    : "Durchgespielt, nicht versendet.";
        }
        return bericht.hatFehler()
                ? "Die Abrechnung wurde nicht versendet."
                : "Die Abrechnung wurde versendet.";
    }

    /**
     * Eine Zeile der Liste.
     *
     * <p><b>{@code fundstelle} und {@code handlung} kamen am 18.09.2026 dazu.</b>
     * Eine Beanstandung, die nur sagt, <em>dass</em> etwas nicht stimmt, ist
     * die halbe Auskunft - "Die Abrechnungspositionsnummer hat 9 Stellen"
     * laesst offen, wo das steht und was zu tun ist. Beides steht jetzt im
     * {@code Befundkatalog}.</p>
     *
     * @param istFehler  ob der Befund den Versand aufgehalten hat
     * @param text       die Beanstandung in ganzen Saetzen
     * @param ort        die Stelle in der Datei, oder leer
     * @param fundstelle Anlage und Abschnitt, oder leer
     * @param handlung   was zu tun ist, oder leer
     */
    public record Zeile(boolean istFehler, String text, String ort, String fundstelle,
            String handlung) {

        /** Eine Zeile ohne Katalogangaben - fuer Tests und fremde Berichte. */
        public Zeile(boolean istFehler, String text, String ort) {
            this(istFehler, text, ort, "", "");
        }
    }

    private Pruefbefunde() {
    }

    /**
     * Die Befunde, Fehler zuerst.
     *
     * <p>Fehler und Warnungen bedeuten Verschiedenes: Fehler haben den Versand
     * aufgehalten und muessen behoben werden, Warnungen sind Hinweise. Ohne die
     * Trennung waere aus der Liste nicht ersichtlich, was zu tun ist.</p>
     */
    public static List<Zeile> zeilen(ValidationReport bericht) {
        List<Zeile> zeilen = new ArrayList<>();
        for (ValidationMessage befund : bericht.getErrors()) {
            zeilen.add(zeile(true, befund));
        }
        for (ValidationMessage befund : bericht.getWarnings()) {
            zeilen.add(zeile(false, befund));
        }
        return List.copyOf(zeilen);
    }

    /** Eine Zeile samt dem, was der Befundkatalog dazu weiss. */
    private static Zeile zeile(boolean istFehler, ValidationMessage befund) {
        return new Zeile(istFehler, befund.text(), befund.ort(),
                befund.fundstelle(), befund.handlung());
    }

    /** Eine Zeile fuer die Statusleiste, etwa "2 zu beheben, 1 Hinweis". */
    public static String kurzfassung(ValidationReport bericht) {
        int fehler = bericht.getErrors().size();
        int hinweise = bericht.getWarnings().size();
        if (fehler == 0 && hinweise == 0) {
            return "Keine Beanstandungen";
        }
        List<String> teile = new ArrayList<>();
        if (fehler > 0) {
            teile.add(fehler + " zu beheben");
        }
        if (hinweise > 0) {
            teile.add(hinweise + (hinweise == 1 ? " Hinweis" : " Hinweise"));
        }
        return String.join(", ", teile);
    }
}
