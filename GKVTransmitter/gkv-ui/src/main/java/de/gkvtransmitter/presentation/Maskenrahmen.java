package de.gkvtransmitter.presentation;

import javafx.scene.layout.Region;

/**
 * Der Platz, an dem eine Maske erscheint.
 *
 * <p>Die Masken sollen nicht wissen, dass sie in der Mitte eines
 * {@code BorderPane} sitzen und in ein {@code ScrollPane} gehuellt werden.
 * Sie sagen nur, was zu sehen sein soll - und im Test zeichnet ein Ersatz auf,
 * was das war.</p>
 */
public interface Maskenrahmen {

    /** Zeigt den Inhalt an und ersetzt dabei, was vorher zu sehen war. */
    void zeige(Region inhalt);

    /**
     * Wechselt in einen Bereich der Seitenleiste, mit allem, was dazugehoert.
     *
     * <p>Der Unterschied zu {@link #zeige}: das tauscht nur den Inhalt aus.
     * Ueberschrift, Untertitel, Fenstertitel und die Hervorhebung in der
     * Seitenleiste gehoeren zum <em>Bereich</em> und bleiben dabei stehen.</p>
     *
     * <p>Aufgefallen ist der Unterschied an einer Stelle, an der er sichtbar
     * wurde: wer aus einer Vorlage heraus eine Blaupause speichert, landet in
     * der Blaupausenliste - und darueber stand weiterhin der Vorlagenname
     * ("Geburtsvorbereitungskurs"), weil das der Bereich war, aus dem er kam.
     * Simons Rueckmeldung K4.</p>
     *
     * <p><b>Erst wechseln, dann melden.</b> Ein echter Bereichswechsel raeumt
     * die Meldungsecke - wer erst meldet, loescht seine eigene
     * Erfolgsmeldung.</p>
     *
     * @param bereich die Beschriftung, unter der der Bereich aufgenommen wurde
     */
    void wechsleZu(String bereich);

    /** Raeumt den Bereich, etwa nach Speichern oder Abbrechen. */
    void leeren();
}
