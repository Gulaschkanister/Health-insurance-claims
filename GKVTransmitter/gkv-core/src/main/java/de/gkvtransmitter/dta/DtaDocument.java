package de.gkvtransmitter.dta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Eine eingelesene DTA-Nachricht als Folge von Segmenten.
 *
 * <p>Bis hierher gab es nur die erzeugte Zeichenkette aus der
 * {@link DtaFactory}. Damit liess sich nicht pruefen, was tatsaechlich
 * verschickt wird - jede Pruefung waere auf Textsuche im fertigen Ergebnis
 * hinausgelaufen. Diese Klasse liest die Nachricht zurueck, so dass die
 * Validierung gegen die Struktur arbeitet und nicht gegen Zeichenketten.</p>
 *
 * <p>Der Weg ueber das Wiedereinlesen ist Absicht: geprueft wird damit genau
 * das Ergebnis, das die Kasse erhaelt, unabhaengig davon, wie es entstanden
 * ist. Eine Datei aus fremder Quelle laesst sich auf demselben Weg pruefen.</p>
 */
public final class DtaDocument {

    /** Schliesst ein Segment ab. */
    public static final char SEGMENT_ENDE = '\'';
    /** Trennt Datenelemente innerhalb eines Segments. */
    public static final char ELEMENT_TRENNER = '+';

    private final List<DtaSegment> segments;
    private final String rohtext;

    private DtaDocument(List<DtaSegment> segments, String rohtext) {
        this.segments = List.copyOf(segments);
        this.rohtext = rohtext;
    }

    /**
     * Liest eine DTA-Nachricht ein.
     *
     * <p>Leerzeilen werden uebergangen - die {@link DtaFactory} setzt zwischen
     * den Nachrichten eine Trennzeile. Ein Segment ohne Abschlusszeichen wird
     * trotzdem aufgenommen; das Fehlen faellt in der Validierung auf und nicht
     * schon beim Einlesen, damit die Meldung aussagekraeftig bleibt.</p>
     */
    public static DtaDocument parse(String inhalt) {
        Objects.requireNonNull(inhalt, "inhalt must not be null");

        List<DtaSegment> gefunden = new ArrayList<>();
        int zeilennummer = 0;

        for (String zeile : inhalt.split("\\R")) {
            String bereinigt = zeile.trim();
            zeilennummer++;
            if (bereinigt.isEmpty()) {
                continue;
            }
            gefunden.add(leseSegment(bereinigt, zeilennummer));
        }
        return new DtaDocument(gefunden, inhalt);
    }

    private static DtaSegment leseSegment(String zeile, int zeilennummer) {
        String ohneAbschluss = zeile.endsWith(String.valueOf(SEGMENT_ENDE))
                ? zeile.substring(0, zeile.length() - 1)
                : zeile;

        // -1 erhaelt leere Elemente am Ende. "FKT+01++123" hat ein leeres
        // zweites Element, und genau das muss die Validierung sehen koennen.
        String[] teile = ohneAbschluss.split("\\" + ELEMENT_TRENNER, -1);
        String tag = teile.length > 0 ? teile[0] : "";
        List<String> elemente = teile.length > 1
                ? Arrays.asList(teile).subList(1, teile.length)
                : List.of();

        return new DtaSegment(tag, elemente, zeile, zeilennummer);
    }

    public List<DtaSegment> getSegments() {
        return segments;
    }

    public String getRohtext() {
        return rohtext;
    }

    /** Alle Segmente mit dem angegebenen Bezeichner, in Reihenfolge. */
    public List<DtaSegment> mitTag(String tag) {
        return segments.stream().filter(s -> s.tag().equals(tag)).toList();
    }

    /** Das erste Segment mit dem angegebenen Bezeichner. */
    public Optional<DtaSegment> erstesMitTag(String tag) {
        return segments.stream().filter(s -> s.tag().equals(tag)).findFirst();
    }

    /** Anzahl der Segmente mit dem angegebenen Bezeichner. */
    public int anzahlMitTag(String tag) {
        return (int) segments.stream().filter(s -> s.tag().equals(tag)).count();
    }

    public boolean istLeer() {
        return segments.isEmpty();
    }
}
