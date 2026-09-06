package de.gkvtransmitter.dta;

import java.time.OffsetDateTime;

import de.gkvtransmitter.entity.Blueprint;

/**
 * Eine Blaupause, mit der sich abrechnen laesst.
 *
 * <p>Bis zum 06.09.2026 stand in den Tests dafuer {@code "{}"} - eine Blaupause
 * ohne jede Angabe. Das ging, weil {@code Leistungsparameter.VORBELEGUNG} den
 * Einzelbetrag mit 15.000,00 fuellte; die Tests rechneten also mit einem Wert,
 * den niemand eingetragen hatte, und pruefen konnte das keiner von ihnen.</p>
 *
 * <p>Seit die Vorbelegung null ist und {@code LeistungspositionRegel} eine
 * Null-Position zurueckweist, muss der Preis dastehen, wo abgerechnet wird.
 * <b>Das ist der eigentliche Gewinn:</b> die Tests bekommen ihre Zahl jetzt von
 * derselben Stelle wie die Anwendung - aus der Blaupause.</p>
 *
 * <p>Der Betrag ist derselbe wie zuvor, damit die erwarteten Summen in den
 * Tests unveraendert bleiben. Fachlich ist er zu hoch fuer einen Kurs; er
 * stammt aus {@code Information/Valide.DTA}.</p>
 */
public final class Testblaupause {

    /** Der Feldname, unter dem {@link Leistungsparameter} den Preis sucht. */
    private static final String FELD_EINZELBETRAG = "Durchschnittlicher Einzelbetrag";

    /** Nutzlast mit Preis, in der Form, die {@code BlaupausenMaske} schreibt. */
    public static final String MIT_PREIS =
            "{\"fields\":{\"" + FELD_EINZELBETRAG + "\":\"15000,00\"}}";

    private Testblaupause() {
    }

    /** Eine Blaupause mit Preis. */
    public static Blueprint mitPreis() {
        return new Blueprint("Test", "test-template", MIT_PREIS, OffsetDateTime.now());
    }

    /** Eine Blaupause ohne Preis - damit darf nichts hinausgehen. */
    public static Blueprint ohnePreis() {
        return new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
    }
}
