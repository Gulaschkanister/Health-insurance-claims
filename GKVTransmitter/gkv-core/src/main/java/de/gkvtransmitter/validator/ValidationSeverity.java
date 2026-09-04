package de.gkvtransmitter.validator;

/**
 * Gewicht eines Pruefbefunds.
 *
 * <p>Die Abstufung entscheidet, ob eine Lieferung den Versand erreicht: nur
 * {@link #ERROR} haelt sie auf. Damit lassen sich Regeln ergaenzen, die auf
 * Auffaelligkeiten hinweisen, ohne die Abrechnung zu blockieren.</p>
 */
public enum ValidationSeverity {

    /** Reine Information, kein Handlungsbedarf. */
    INFO,

    /** Auffaellig, aber nicht regelwidrig - der Versand laeuft weiter. */
    WARNING,

    /** Verstoss gegen eine verbindliche Vorgabe - der Versand wird angehalten. */
    ERROR
}
