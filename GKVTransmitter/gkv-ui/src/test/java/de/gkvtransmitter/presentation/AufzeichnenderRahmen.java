package de.gkvtransmitter.presentation;

import javafx.scene.layout.Region;

/** Merkt sich, was zuletzt gezeigt wurde, statt es darzustellen. */
class AufzeichnenderRahmen implements Maskenrahmen {

    private Region inhalt;
    private int geleert;

    @Override
    public void zeige(Region inhalt) {
        this.inhalt = inhalt;
    }

    @Override
    public void leeren() {
        this.inhalt = null;
        this.geleert++;
    }

    /** Was gerade zu sehen waere, oder {@code null} nach dem Leeren. */
    Region inhalt() {
        return inhalt;
    }

    /** Wie oft der Bereich geraeumt wurde. */
    int wieOftGeleert() {
        return geleert;
    }
}
