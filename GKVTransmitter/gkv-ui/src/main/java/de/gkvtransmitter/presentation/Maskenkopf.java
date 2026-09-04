package de.gkvtransmitter.presentation;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Der immer gleiche Kopf einer Uebersicht: "Neu" rechts, darunter die Liste.
 *
 * <p>Steht hier und nicht in jeder Maske, damit die Bereiche nicht auseinander
 * driften - eine Uebersicht soll in allen Bereichen gleich aussehen und gleich
 * zu bedienen sein.</p>
 */
final class Maskenkopf {

    private Maskenkopf() {
    }

    /**
     * <p>Ohne eigene Ueberschrift: den Namen des Bereichs traegt bereits die
     * Kopfzeile des Rahmens. Stuende er hier noch einmal, staende er zweimal
     * untereinander.</p>
     */
    static Region mitListe(UiFactory bausteine, String neuBeschriftung,
            String neuKennung, Runnable neu, Region liste) {
        Button neuerEintrag = bausteine.createButton("+ " + neuBeschriftung);
        neuerEintrag.setId(neuKennung);
        neuerEintrag.getStyleClass().add("schaltflaeche-haupt");
        neuerEintrag.setOnAction(ereignis -> neu.run());

        Region abstand = new Region();
        HBox.setHgrow(abstand, Priority.ALWAYS);
        // Die Schaltflaeche steht links, nicht rechts: rechts oben erscheinen
        // die Meldungen, und die wuerden sie verdecken.
        HBox kopf = new HBox(12, neuerEintrag, abstand);
        kopf.setAlignment(Pos.CENTER_LEFT);

        VBox wurzel = new VBox(16, kopf, liste);
        wurzel.getStyleClass().add("maske");
        return wurzel;
    }
}
