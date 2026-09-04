package de.gkvtransmitter.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.validator.rules.BetragskonsistenzRegel;
import de.gkvtransmitter.validator.rules.InstitutionskennzeichenRegel;
import de.gkvtransmitter.validator.rules.NachrichtenAbschlussRegel;
import de.gkvtransmitter.validator.rules.NachrichtenRahmenRegel;
import de.gkvtransmitter.validator.rules.SegmentSyntaxRegel;
import de.gkvtransmitter.validator.rules.VersichertenangabenRegel;

/**
 * Die Pruefstelle im Ablauf: hier wird eine DTA-Nachricht geprueft, bevor sie
 * die Kasse erreicht.
 *
 * <p>Der Dienst haelt eine Liste von {@link ValidationRule}n und laesst jede
 * ueber die Nachricht laufen. Eine Regel, die selbst abstuerzt, darf die
 * Pruefung nicht beenden - sonst haetten die uebrigen Regeln keine Gelegenheit
 * mehr, ihre Befunde zu melden. Ein solcher Absturz wird deshalb als eigener
 * Fehlerbefund aufgenommen.</p>
 *
 * <p>Neue Regeln kommen ueber {@link #mitZusaetzlicherRegel(ValidationRule)}
 * dazu, ohne dass bestehende angefasst werden muessen.</p>
 */
public final class DtaValidationService {

    private final List<ValidationRule> regeln;

    public DtaValidationService(List<ValidationRule> regeln) {
        this.regeln = List.copyOf(Objects.requireNonNull(regeln, "regeln must not be null"));
    }

    /**
     * Erzeugt den Dienst mit den mitgelieferten Regeln.
     *
     * <p>Die Reihenfolge ist bewusst gewaehlt: erst die formale Struktur, dann
     * der Rahmen, dann die Inhalte. So steht bei einer kaputten Datei die
     * grundlegende Ursache oben im Bericht.</p>
     */
    public static DtaValidationService standard() {
        return new DtaValidationService(List.of(
                new SegmentSyntaxRegel(),
                new NachrichtenRahmenRegel(),
                new NachrichtenAbschlussRegel(),
                new InstitutionskennzeichenRegel(),
                new VersichertenangabenRegel(),
                new BetragskonsistenzRegel()));
    }

    /** Liefert einen Dienst mit einer zusaetzlichen Regel. */
    public DtaValidationService mitZusaetzlicherRegel(ValidationRule regel) {
        Objects.requireNonNull(regel, "regel must not be null");
        List<ValidationRule> erweitert = new ArrayList<>(regeln);
        erweitert.add(regel);
        return new DtaValidationService(erweitert);
    }

    public List<ValidationRule> getRegeln() {
        return regeln;
    }

    /** Prueft eine DTA-Nachricht im Rohformat. */
    public ValidationReport pruefe(String dtaInhalt) {
        Objects.requireNonNull(dtaInhalt, "dtaInhalt must not be null");
        return pruefe(DtaDocument.parse(dtaInhalt));
    }

    /** Prueft eine bereits eingelesene DTA-Nachricht. */
    public ValidationReport pruefe(DtaDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        ValidationReport.Builder bericht = ValidationReport.builder();

        for (ValidationRule regel : regeln) {
            try {
                regel.pruefe(document, bericht);
            } catch (RuntimeException e) {
                // Eine fehlerhafte Regel darf die uebrigen nicht mitreissen.
                bericht.error("REGEL_ABGEBROCHEN", regel.getName(),
                        "Die Regel '%s' konnte nicht ausgefuehrt werden: %s"
                                .formatted(regel.getName(), e.getMessage()));
            }
        }
        return bericht.build();
    }
}
