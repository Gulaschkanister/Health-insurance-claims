package de.gkvtransmitter.validator;

import de.gkvtransmitter.dta.DtaDocument;

/**
 * Eine einzelne, austauschbare Pruefregel fuer eine DTA-Nachricht.
 *
 * <p>Zuvor war das ein leeres Interface ohne Methode. Jetzt beschreibt es den
 * Vertrag, gegen den sich neue Regeln ergaenzen lassen, ohne bestehende zu
 * beruehren: eine Regel bekommt die eingelesene Nachricht und traegt ihre
 * Befunde in den Bericht ein.</p>
 *
 * <p>Eine Regel darf nicht abbrechen, wenn sie etwas nicht findet - fehlende
 * Segmente sind selbst ein Befund. Sie meldet ihn und laesst die uebrigen
 * Regeln weiterlaufen.</p>
 */
public interface ValidationRule {

    /** Sprechender Name der Regel, erscheint in Protokollen. */
    String getName();

    /**
     * Prueft die Nachricht und traegt gefundene Beanstandungen ein.
     *
     * @param document die eingelesene Nachricht
     * @param bericht  Sammelstelle fuer Befunde
     */
    void pruefe(DtaDocument document, ValidationReport.Builder bericht);
}
