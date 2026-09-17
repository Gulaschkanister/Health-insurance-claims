package de.gkvtransmitter.dispatch;

import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.validator.ValidationReport;

/**
 * Was bei einem Abrechnungslauf herauskam: die Lieferungen <b>und</b> der
 * Pruefbericht.
 *
 * <p><b>Warum es diese Klasse gibt.</b> Zuvor lieferte
 * {@link DtaDispatchService#generateAndRoute} nur {@code List<DispatchBatch>}.
 * Der Bericht entstand dabei ohnehin, wurde bei fehlerfreiem Lauf aber
 * weggeworfen - die Oberflaeche bekam ihn nur im Ausnahmefall ueber
 * {@link DtaValidierungsException} zu sehen. <b>Warnungen und Hinweise
 * erreichten den Bildschirm damit nie</b>, obwohl die Abstufung in
 * {@code ValidationSeverity} genau dafuer gedacht ist: nur ein Fehler haelt den
 * Versand auf, eine Warnung soll gesehen, aber nicht befolgt werden muessen.</p>
 *
 * <p>Eine Warnung, die niemand sieht, ist keine Warnung. Deshalb reicht der
 * Dienst jetzt beides heraus.</p>
 *
 * @param lieferungen die zugestellten Dateien, nach Kasse gebuendelt
 * @param bericht     alle Befunde des Laufs. Enthaelt <b>keine Fehler</b> -
 *                    ein Fehler haette den Lauf mit einer
 *                    {@link DtaValidierungsException} beendet, und dann gibt es
 *                    kein Ergebnis, sondern eine Ausnahme.
 */
public record Versandergebnis(List<DispatchBatch> lieferungen, ValidationReport bericht) {

    public Versandergebnis {
        lieferungen = List.copyOf(Objects.requireNonNull(lieferungen, "lieferungen must not be null"));
        Objects.requireNonNull(bericht, "bericht must not be null");
    }

    /** Ob es ueberhaupt etwas zu melden gibt. */
    public boolean hatBefunde() {
        return !bericht.istLeer();
    }
}
