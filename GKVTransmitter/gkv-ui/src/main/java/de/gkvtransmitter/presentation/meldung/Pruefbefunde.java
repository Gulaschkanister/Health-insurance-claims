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

    /** Ueberschrift der Meldung. */
    public static final String TITEL = "Prüfung nicht bestanden";

    /** Was unter der Ueberschrift steht, bevor die Liste beginnt. */
    public static final String EINLEITUNG = "Die Abrechnung wurde nicht versendet.";

    /**
     * Eine Zeile der Liste.
     *
     * @param istFehler ob der Befund den Versand aufgehalten hat
     * @param text      die Beanstandung in ganzen Saetzen
     * @param ort       die Fundstelle, oder leer
     */
    public record Zeile(boolean istFehler, String text, String ort) {
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
            zeilen.add(new Zeile(true, befund.text(), befund.ort()));
        }
        for (ValidationMessage befund : bericht.getWarnings()) {
            zeilen.add(new Zeile(false, befund.text(), befund.ort()));
        }
        return List.copyOf(zeilen);
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
