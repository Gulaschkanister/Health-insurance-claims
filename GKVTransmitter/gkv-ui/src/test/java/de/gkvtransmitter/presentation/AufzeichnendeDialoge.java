package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import de.gkvtransmitter.presentation.dialog.Dialoge;

/**
 * Sammelt die Meldungen, statt sie zu zeigen, und gibt vorgegebene Antworten.
 *
 * <p>Damit laesst sich pruefen, <em>was</em> eine Maske meldet und wie sie auf
 * Zustimmung oder Ablehnung reagiert, ohne dass ein Fenster aufginge, auf das
 * niemand klicken koennte.</p>
 */
class AufzeichnendeDialoge implements Dialoge {

    /** Art einer Meldung. */
    enum Art { INFO, FEHLER }

    /** Eine aufgezeichnete Meldung. */
    record Meldung(Art art, String titel, String text) {
    }

    private final List<Meldung> meldungen = new ArrayList<>();
    private final List<String> rueckfragen = new ArrayList<>();

    /** Was bei einer Auswahl herauskommt. Standard: Abbruch. */
    private Function<List<?>, Object> auswahl = eintraege -> null;

    /** Wie eine Rueckfrage beantwortet wird. Standard: abgelehnt. */
    private boolean zustimmung;

    /** Laesst kuenftige Auswahlen den Eintrag an dieser Stelle liefern. */
    AufzeichnendeDialoge waehltEintrag(int stelle) {
        this.auswahl = eintraege -> eintraege.get(stelle);
        return this;
    }

    /** Laesst kuenftige Rueckfragen bejahen. */
    AufzeichnendeDialoge stimmtZu() {
        this.zustimmung = true;
        return this;
    }

    @Override
    public void zeigeInfo(String titel, String nachricht) {
        meldungen.add(new Meldung(Art.INFO, titel, nachricht));
    }

    @Override
    public void zeigeFehler(String titel, String nachricht) {
        meldungen.add(new Meldung(Art.FEHLER, titel, nachricht));
    }

    @Override
    public <T> Optional<T> waehleAus(String titel, String text, List<T> eintraege,
            Function<T, String> anzeige) {
        if (eintraege == null || eintraege.isEmpty()) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        T gewaehlt = (T) auswahl.apply(eintraege);
        return Optional.ofNullable(gewaehlt);
    }

    @Override
    public boolean bestaetige(String titel, String kopfzeile, String text) {
        rueckfragen.add(text);
        return zustimmung;
    }

    List<Meldung> alle() {
        return List.copyOf(meldungen);
    }

    /** Die Texte der gestellten Rueckfragen, in der Reihenfolge des Auftretens. */
    List<String> gestellteRueckfragen() {
        return List.copyOf(rueckfragen);
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
