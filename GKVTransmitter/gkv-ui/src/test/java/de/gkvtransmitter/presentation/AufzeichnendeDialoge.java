package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.List;

import de.gkvtransmitter.presentation.dialog.Dialoge;

/**
 * Sammelt die Meldungen, statt sie zu zeigen.
 *
 * <p>Damit laesst sich pruefen, <em>was</em> eine Maske meldet, ohne dass ein
 * Fenster aufginge, auf das niemand klicken koennte.</p>
 */
class AufzeichnendeDialoge implements Dialoge {

    /** Art einer Meldung. */
    enum Art { INFO, FEHLER }

    /** Eine aufgezeichnete Meldung. */
    record Meldung(Art art, String titel, String text) {
    }

    private final List<Meldung> meldungen = new ArrayList<>();

    @Override
    public void zeigeInfo(String titel, String nachricht) {
        meldungen.add(new Meldung(Art.INFO, titel, nachricht));
    }

    @Override
    public void zeigeFehler(String titel, String nachricht) {
        meldungen.add(new Meldung(Art.FEHLER, titel, nachricht));
    }

    List<Meldung> alle() {
        return List.copyOf(meldungen);
    }

    /** Die einzige Meldung. Schlaegt fehl, wenn es keine oder mehrere gibt. */
    Meldung einzige() {
        if (meldungen.size() != 1) {
            throw new AssertionError("Erwartet war genau eine Meldung, aufgezeichnet: " + meldungen);
        }
        return meldungen.get(0);
    }

    boolean leer() {
        return meldungen.isEmpty();
    }
}
