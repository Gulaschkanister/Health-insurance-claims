package de.gkvtransmitter.hibernate.sqllite;

/**
 * Zeigt an, dass ein Datenbankzugriff fehlgeschlagen ist.
 *
 * <p>Ersetzt die zuvor geworfenen nackten {@link RuntimeException}-Instanzen:
 * die liessen sich von aufrufendem Code nicht von Programmierfehlern
 * unterscheiden, weshalb die Oberflaeche jeden Fehler gleich behandeln musste.</p>
 */
public class PersistenceOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenceOperationException(String operation, Throwable cause) {
        super("Datenbankzugriff fehlgeschlagen: " + operation, cause);
    }
}
