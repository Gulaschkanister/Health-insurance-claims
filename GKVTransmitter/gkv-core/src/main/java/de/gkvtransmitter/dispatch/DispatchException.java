package de.gkvtransmitter.dispatch;

/**
 * Zeigt an, dass eine Lieferung nicht zugestellt werden konnte.
 *
 * <p>Ersetzt die zuvor geworfene nackte {@link RuntimeException}: die liess
 * sich vom aufrufenden Code nicht von einem Programmierfehler unterscheiden.</p>
 */
public class DispatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
