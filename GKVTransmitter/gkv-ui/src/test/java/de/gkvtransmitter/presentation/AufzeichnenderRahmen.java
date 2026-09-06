package de.gkvtransmitter.presentation;

import javafx.scene.layout.Region;

/** Merkt sich, was zuletzt gezeigt wurde, statt es darzustellen. */
class AufzeichnenderRahmen implements Maskenrahmen {

    private Region inhalt;
    private int geleert;
    private String bereich;
    private Runnable beimWechsel = () -> { };

    /**
     * Was ein Bereichswechsel ausloest.
     *
     * <p>Im echten Rahmen haengt daran das Raeumen der Meldungsecke
     * ({@code View}: {@code hauptfenster.beiBereichswechsel(benachrichtigungen::leeren)}).
     * <b>Ohne diese Nachbildung waere jeder Test ueber die Reihenfolge von
     * Wechsel und Meldung wertlos</b> - er ginge in beiden Reihenfolgen durch,
     * weil hier nie etwas geloescht wuerde.</p>
     */
    AufzeichnenderRahmen beiWechsel(Runnable was) {
        this.beimWechsel = was == null ? () -> { } : was;
        return this;
    }

    @Override
    public void zeige(Region inhalt) {
        this.inhalt = inhalt;
    }

    @Override
    public void wechsleZu(String bereich) {
        this.bereich = bereich;
        // Der echte Rahmen baut den Bereich neu auf; was dabei entsteht, weiss
        // nur er. Hier zaehlt, dass gewechselt wurde und wohin.
        this.inhalt = null;
        beimWechsel.run();
    }

    /** In welchen Bereich zuletzt gewechselt wurde, oder {@code null}. */
    String bereich() {
        return bereich;
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
