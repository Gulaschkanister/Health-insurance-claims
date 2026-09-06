package de.gkvtransmitter.hibernate.sqllite;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Kapselt Sitzungs- und Transaktionsbehandlung.
 *
 * <p>Zuvor stand in jeder der zwoelf Repository-Methoden derselbe Block aus
 * {@code openSession}, {@code beginTransaction}, {@code commit} und
 * {@code rollback}. Das war nicht nur Wiederholung: der Rollback lief in einem
 * {@code catch (Exception)}, so dass ein Fehler <em>waehrend</em> des Rollbacks
 * die eigentliche Ursache verdeckt hat.</p>
 *
 * <p>Hier passiert das an genau einer Stelle, und ein fehlgeschlagener Rollback
 * wird der urspruenglichen Ausnahme als {@code suppressed} beigelegt, statt sie
 * zu ersetzen.</p>
 */
public final class TransactionRunner {

    private final SessionFactory sessionFactory;

    public TransactionRunner(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    /** Fuehrt eine nur lesende Arbeit ohne eigene Transaktion aus. */
    public <T> T read(String beschreibung, Function<Session, T> arbeit) {
        try (Session session = sessionFactory.openSession()) {
            return arbeit.apply(session);
        } catch (RuntimeException e) {
            throw new PersistenceOperationException(beschreibung, e);
        }
    }

    /** Fuehrt eine schreibende Arbeit in einer Transaktion aus. */
    public <T> T write(String beschreibung, Function<Session, T> arbeit) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaktion = session.beginTransaction();
            try {
                T ergebnis = arbeit.apply(session);
                transaktion.commit();
                return ergebnis;
            } catch (RuntimeException e) {
                rollbackStill(transaktion, e);
                throw new PersistenceOperationException(beschreibung, e);
            }
        }
    }

    /** Schreibende Arbeit ohne Rueckgabewert. */
    public void writeVoid(String beschreibung, Consumer<Session> arbeit) {
        write(beschreibung, session -> {
            arbeit.accept(session);
            return null;
        });
    }

    /**
     * Setzt die Transaktion zurueck, ohne dabei die Ursache zu verlieren: ein
     * Fehler beim Rollback wird der Originalausnahme angehaengt.
     */
    private static void rollbackStill(Transaction transaktion, RuntimeException ursache) {
        if (transaktion == null || !transaktion.isActive()) {
            return;
        }
        try {
            transaktion.rollback();
        } catch (RuntimeException rollbackFehler) {
            ursache.addSuppressed(rollbackFehler);
        }
    }
}
