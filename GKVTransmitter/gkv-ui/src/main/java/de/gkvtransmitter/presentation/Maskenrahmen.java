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

    /** Raeumt den Bereich, etwa nach Speichern oder Abbrechen. */
    void leeren();
}
