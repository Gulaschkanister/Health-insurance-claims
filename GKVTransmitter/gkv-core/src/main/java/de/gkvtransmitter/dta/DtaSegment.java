package de.gkvtransmitter.dta;

import java.util.List;
import java.util.Objects;

/**
 * Ein einzelnes Segment einer DTA-Nachricht.
 *
 * <p>Ein Segment beginnt mit einem Bezeichner (etwa {@code UNB}, {@code REC},
 * {@code ENF}), danach folgen die Datenelemente. Getrennt wird mit {@code +},
 * abgeschlossen mit {@code '}. Ein Datenelement kann selbst noch mit
 * {@code :} unterteilt sein.</p>
 *
 * @param tag          Segmentbezeichner, etwa {@code UNB}
 * @param elements     Datenelemente ohne den Bezeichner, in Reihenfolge
 * @param raw          die urspruengliche Zeile einschliesslich Abschlusszeichen
 * @param zeilennummer 1-basierte Position in der Nachricht, fuer Fehlermeldungen
 */
public record DtaSegment(String tag, List<String> elements, String raw, int zeilennummer) {

    public DtaSegment {
        Objects.requireNonNull(tag, "tag must not be null");
        Objects.requireNonNull(elements, "elements must not be null");
        elements = List.copyOf(elements);
    }

    /**
     * Liefert das Datenelement an der angegebenen Position oder einen leeren
     * Text, wenn es das Element nicht gibt.
     *
     * <p>Bewusst kein {@code null} und keine Ausnahme: Validierungsregeln
     * pruefen gerade auf fehlende Elemente und sollen dabei nicht selbst
     * abbrechen.</p>
     *
     * @param position 0-basiert, gezaehlt ohne den Segmentbezeichner
     */
    public String element(int position) {
        if (position < 0 || position >= elements.size()) {
            return "";
        }
        return elements.get(position);
    }

    /**
     * Liefert eine Komponente innerhalb eines Datenelements.
     *
     * <p>Im Element {@code SLGA:21:0:0} ist {@code SLGA} die Komponente 0 und
     * {@code 21} die Komponente 1.</p>
     */
    public String komponente(int position, int komponente) {
        String[] teile = element(position).split(":", -1);
        if (komponente < 0 || komponente >= teile.length) {
            return "";
        }
        return teile[komponente];
    }

    /** Anzahl der Datenelemente ohne den Segmentbezeichner. */
    public int elementAnzahl() {
        return elements.size();
    }

    /** Ortsangabe fuer Fehlermeldungen, etwa {@code UNB (Zeile 1)}. */
    public String ort() {
        return tag + " (Zeile " + zeilennummer + ")";
    }
}
