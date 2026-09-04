package de.gkvtransmitter.presentation.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.validator.ValidationReport;

/**
 * Prueft den Wortlaut des Pruefberichts.
 *
 * <p>Ohne JavaFX: die Aufbereitung liegt genau deshalb ausserhalb der
 * Dialogklasse.</p>
 */
@DisplayName("Text des Pruefberichts")
class PruefberichttextTest {

    @Test
    @DisplayName("Fehler und Warnungen stehen unter getrennten Ueberschriften")
    void getrennteUeberschriften() {
        String text = Pruefberichttext.formatiere(ValidationReport.builder()
                .error("UNZ_ANZAHL", "UNZ", "Die Zahl der Nachrichten stimmt nicht.")
                .warning("BETRAG", "", "Der Betrag ist auffaellig hoch.")
                .build());

        assertTrue(text.contains("Zu beheben:"), text);
        assertTrue(text.contains("Hinweise:"), text);
        assertTrue(text.indexOf("Zu beheben:") < text.indexOf("Hinweise:"),
                "Was den Versand aufgehalten hat, gehoert nach oben");
    }

    @Test
    @DisplayName("Ohne Warnungen fehlt die Ueberschrift fuer Hinweise")
    void keineLeereUeberschrift() {
        String text = Pruefberichttext.formatiere(ValidationReport.builder()
                .error("UNZ_ANZAHL", "UNZ", "Die Zahl der Nachrichten stimmt nicht.")
                .build());

        assertFalse(text.contains("Hinweise:"), text);
    }

    @Test
    @DisplayName("Die Fundstelle steht hinter dem Befund, wenn es eine gibt")
    void fundstelle() {
        String text = Pruefberichttext.formatiere(ValidationReport.builder()
                .error("UNZ_ANZAHL", "UNZ (Zeile 18)", "Die Zahl der Nachrichten stimmt nicht.")
                .error("IK", "", "Das IK ist ungueltig.")
                .build());

        assertTrue(text.contains("[UNZ (Zeile 18)]"), text);
        assertTrue(text.contains("Das IK ist ungueltig." + System.lineSeparator()),
                "Ohne Fundstelle darf keine leere Klammer stehen: " + text);
    }

    @Test
    @DisplayName("Der Bericht sagt zuerst, dass nichts versendet wurde")
    void versandUnterblieben() {
        String text = Pruefberichttext.formatiere(ValidationReport.builder()
                .error("UNZ_ANZAHL", "UNZ", "Die Zahl der Nachrichten stimmt nicht.")
                .build());

        assertTrue(text.startsWith("Die Abrechnung wurde nicht versendet."), text);
    }
}
