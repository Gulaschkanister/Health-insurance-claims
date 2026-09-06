package de.gkvtransmitter.presentation;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Der immer gleiche Kopf einer Uebersicht: "Neu" links, darunter die Liste.
 *
 * <p>Steht hier und nicht in jeder Maske, damit die Bereiche nicht auseinander
 * driften - eine Uebersicht soll in allen Bereichen gleich aussehen und gleich
 * zu bedienen sein.</p>
 *
 * <p>Hier stand bis zum 06.09.2026 "rechts", und zwar eine Zeile ueber dem
 * Kommentar, der genau diese Angabe schon berichtigt hatte. Wer eine
 * Begruendung nachzieht, muss die Kurzfassung darueber mitnehmen -
 * <b>eine veraltete Zusammenfassung ist genauso irrefuehrend wie eine
 * veraltete Begruendung, und sie wird oefter gelesen.</b></p>
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
        // Die Schaltflaeche steht links, nicht rechts. Hier stand als Grund,
        // rechts oben erschienen die Meldungen und wuerden sie verdecken -
        // das stimmt seit dem 06.09.2026 nicht mehr, die Meldungsecke liegt
        // jetzt unten rechts. Der Platz bleibt trotzdem links: dort beginnt
        // die Leserichtung, und darunter beginnt auch die Liste.
        HBox kopf = new HBox(12, neuerEintrag, abstand);
        kopf.setAlignment(Pos.CENTER_LEFT);

        VBox wurzel = new VBox(16, kopf, liste);
        wurzel.getStyleClass().add("maske");
        return wurzel;
    }
}
