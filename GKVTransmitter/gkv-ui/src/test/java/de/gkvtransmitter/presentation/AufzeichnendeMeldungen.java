package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.List;

import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Sammelt die Meldungen, statt sie zu zeigen, und gibt vorgegebene Antworten.
 *
 * <p>Damit laesst sich pruefen, <em>was</em> eine Maske meldet und wie sie auf
 * Zustimmung oder Ablehnung reagiert, ohne dass etwas auf dem Bildschirm
 * erscheinen muesste, worauf niemand klicken koennte.</p>
 */
class AufzeichnendeMeldungen implements Meldungen {

    /** Art einer aufgezeichneten Meldung. */
    enum Art { ERFOLG, HINWEIS, FEHLER, PRUEFBERICHT }

    /** Eine aufgezeichnete Meldung. */
    record Meldung(Art art, String text) {
    }

    private final List<Meldung> meldungen = new ArrayList<>();
    private final List<String> rueckfragen = new ArrayList<>();

    private ValidationReport letzterBericht;

    /** Wie eine Rueckfrage beantwortet wird. Standard: abgelehnt. */
    private boolean zustimmung;

    /** Laesst kuenftige Rueckfragen bejahen. */
    AufzeichnendeMeldungen stimmtZu() {
        this.zustimmung = true;
        return this;
    }

    @Override
    public void erfolg(String text) {
        meldungen.add(new Meldung(Art.ERFOLG, text));
    }

    @Override
    public void hinweis(String text) {
        meldungen.add(new Meldung(Art.HINWEIS, text));
    }

    @Override
    public void fehler(String text) {
        meldungen.add(new Meldung(Art.FEHLER, text));
    }

    @Override
    public void pruefbericht(ValidationReport bericht) {
        this.letzterBericht = bericht;
        meldungen.add(new Meldung(Art.PRUEFBERICHT, bericht.kurzfassung()));
    }

    @Override
    public void frageNach(String frage, String bejahenBeschriftung, Runnable wennBejaht) {
        rueckfragen.add(frage);
        if (zustimmung) {
            wennBejaht.run();
        }
    }

    List<Meldung> alle() {
        return List.copyOf(meldungen);
    }

    /**
     * Raeumt die Ecke, wie es die Anwendung beim Bereichswechsel tut.
     *
     * <p>Gehoert nicht zur Schnittstelle {@code Meldungen} - dort waere es
     * fehl am Platz, weil keine Maske ihre eigenen Meldungen loescht. Im
     * Betrieb macht das {@code Benachrichtigungen.leeren}, gerufen vom Rahmen.
     * Hier steht es, damit {@link AufzeichnenderRahmen#beiWechsel} dasselbe
     * nachbilden kann.</p>
     */
    void raeume() {
        meldungen.clear();
        letzterBericht = null;
    }

    /** Die Texte der gestellten Rueckfragen, in der Reihenfolge des Auftretens. */
    List<String> gestellteRueckfragen() {
        return List.copyOf(rueckfragen);
    }

    /** Der zuletzt gezeigte Pruefbericht, oder {@code null}. */
    ValidationReport letzterPruefbericht() {
        return letzterBericht;
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
