package de.gkvtransmitter.wartung;

import java.time.LocalDate;

/**
 * Eine verbindliche Unterlage mit ihrem Stand.
 *
 * <p>Die Angaben stammen vom Deckblatt der jeweiligen PDF: Version, Stand,
 * "Anzuwenden ab" und - wo genannt - der Tag, an dem die Vorgaengerfassung
 * ihre Gueltigkeit verliert.</p>
 *
 * @param kennung      eindeutiger Name, etwa {@code anlage-1-v21}
 * @param titel        wie die Unterlage heisst
 * @param datei        Dateiname unter {@code Information/}
 * @param version      Versionsnummer, oder {@code —} wenn keine gefuehrt wird
 * @param stand        Stand der Fassung
 * @param anzuwendenAb ab wann sie anzuwenden ist
 * @param gueltigBis   bis wann sie gilt, oder {@code null}
 * @param bemerkung    was dazu zu wissen ist, oder {@code null}
 * @param quellname    Namensanfang derselben Unterlage auf der Bezugsquelle,
 *                     oder {@code null} - dann wird sie nicht geprueft
 */
public record Unterlage(String kennung, String titel, String datei, String version,
        LocalDate stand, LocalDate anzuwendenAb, LocalDate gueltigBis, String bemerkung,
        String quellname) {

    /**
     * Ob sich diese Unterlage auf der Bezugsquelle wiederfinden laesst.
     *
     * <p>Die Namen dort weichen von den unseren ab:
     * {@code Anhang_3_Kostentraegerdatei_V10_20260414.pdf} heisst auf
     * {@code gkv-datenaustausch.de} {@code Anhang_03_Anlage_1_TP5_V10_...}.
     * Ohne diesen Namen bleibt die Unterlage von der Pruefung ausgenommen -
     * <b>lieber gar nicht geprueft als falsch verglichen.</b></p>
     */
    public boolean istPruefbar() {
        return quellname != null && !quellname.isBlank();
    }

    /** Ob diese Fassung an einem Tag bereits anzuwenden ist. */
    public boolean giltAm(LocalDate tag) {
        boolean begonnen = anzuwendenAb == null || !tag.isBefore(anzuwendenAb);
        boolean nochNichtAbgelaufen = gueltigBis == null || !tag.isAfter(gueltigBis);
        return begonnen && nochNichtAbgelaufen;
    }

    /** Ob sie erst in der Zukunft anzuwenden ist. */
    public boolean stehtBevor(LocalDate tag) {
        return anzuwendenAb != null && tag.isBefore(anzuwendenAb);
    }

    /**
     * Ob ihre Gueltigkeit bereits abgelaufen ist.
     *
     * <p>Das ist der Fall, den niemand bemerkt: Die Anwendung sendet weiter,
     * die Kasse weist zurueck, und die Ursache steht auf einem Deckblatt.</p>
     */
    public boolean abgelaufen(LocalDate tag) {
        return gueltigBis != null && tag.isAfter(gueltigBis);
    }
}
