package de.gkvtransmitter.hibernate.sqllite;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.LockMode;
import org.hibernate.SessionFactory;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.DtaCounter;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.repository.DataRepository;

/**
 * SQLite-Umsetzung von {@link DataRepository} auf Basis von Hibernate.
 *
 * <p>Der Lebenszyklus liegt jetzt beim Aufrufer: {@link #open(DatabaseSettings)}
 * baut die Verbindung auf, {@link #close()} gibt sie frei. Die frueher
 * vorhandene Methode {@code getInstance()} war kein Singleton - sie war eine
 * Instanzmethode, die aus einer bereits erzeugten Instanz heraus eine
 * <em>zweite</em> anlegte und zurueckgab, so dass jeder Aufruf von
 * {@code new HibernateSqllite().getInstance()} zwei Objekte erzeugte.</p>
 */
public class HibernateSqllite implements DataRepository, AutoCloseable {

    private final SessionFactory sessionFactory;
    private final TransactionRunner runner;

    /** Serialisiert die Vergabe der Datenaustauschreferenz, siehe unten. */
    private final Lock referenzSperre = new ReentrantLock();

    private HibernateSqllite(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
        this.runner = new TransactionRunner(sessionFactory);
    }

    /** Oeffnet das Repository gegen die angegebene Datenbank. */
    public static HibernateSqllite open(DatabaseSettings settings) {
        return new HibernateSqllite(SessionFactoryProvider.create(settings));
    }

    /** Oeffnet das Repository gegen die per Umgebung konfigurierte Datenbank. */
    public static HibernateSqllite open() {
        return open(DatabaseSettings.fromEnvironment());
    }

    @Override
    public void close() {
        sessionFactory.close();
    }

    // --- Patient ---------------------------------------------------------

    @Override
    public void savePatient(Patient patient) {
        runner.writeVoid("Patient speichern", session -> session.merge(patient));
    }

    @Override
    public List<Patient> getAllPatients() {
        return runner.read("Patienten laden",
                session -> session.createQuery("FROM Patient", Patient.class).getResultList());
    }

    @Override
    public Patient getPatientById(int id) {
        return runner.read("Patient " + id + " laden", session -> session.get(Patient.class, id));
    }

    @Override
    public void deletePatient(Patient patient) {
        runner.writeVoid("Patient loeschen", session -> session.remove(session.merge(patient)));
    }

    // --- ServiceProvider -------------------------------------------------

    @Override
    public void saveServiceProvider(ServiceProvider serviceProvider) {
        runner.writeVoid("Leistungserbringer speichern", session -> session.merge(serviceProvider));
    }

    @Override
    public List<ServiceProvider> getAllServiceProviders() {
        return runner.read("Leistungserbringer laden",
                session -> session.createQuery("FROM ServiceProvider", ServiceProvider.class).getResultList());
    }

    @Override
    public void deleteServiceProvider(ServiceProvider serviceProvider) {
        runner.writeVoid("Leistungserbringer loeschen", session -> session.remove(session.merge(serviceProvider)));
    }

    // --- PersonGroup -----------------------------------------------------

    @Override
    public void savePersonGroup(PersonGroup personGroup) {
        runner.writeVoid("Gruppe speichern", session -> session.merge(personGroup));
    }

    @Override
    public List<PersonGroup> getAllPersonGroups() {
        return runner.read("Gruppen laden",
                session -> session.createQuery("FROM PersonGroup", PersonGroup.class).getResultList());
    }

    @Override
    public void deletePersonGroup(PersonGroup personGroup) {
        runner.writeVoid("Gruppe loeschen", session -> session.remove(session.merge(personGroup)));
    }

    // --- Blueprint -------------------------------------------------------

    @Override
    public void saveBlueprint(Blueprint blueprint) {
        runner.writeVoid("Blaupause speichern", session -> session.merge(blueprint));
    }

    @Override
    public List<Blueprint> getAllBlueprints() {
        return runner.read("Blaupausen laden",
                session -> session.createQuery("FROM Blueprint", Blueprint.class).getResultList());
    }

    // --- DTA-Zaehler -----------------------------------------------------

    /**
     * Liefert die naechste Datenaustauschreferenz und zaehlt sie weiter.
     *
     * <p>Die Referenz muss je Sender eindeutig sein; eine doppelt vergebene
     * Nummer fuehrt zur Abweisung der gesamten Lieferung durch die Kasse.
     * Zuvor war die Vergabe ein ungesichertes Lesen-Aendern-Schreiben: zwei
     * gleichzeitige Abrechnungslaeufe konnten denselben Wert erhalten.</p>
     *
     * <p>Die Sperre liegt bewusst in der Anwendung und nicht in der Datenbank.
     * Ein Datenbank-Sperrhinweis reicht bei SQLite nicht aus: eine Transaktion,
     * die erst liest und dann schreibt, muss ihre Sperre nachtraeglich
     * aufwerten. Kollidiert das mit einer zweiten Transaktion, meldet SQLite
     * sofort {@code SQLITE_BUSY}, statt zu warten - der {@code busy_timeout}
     * greift hier absichtlich nicht, weil sonst eine Verklemmung entstuende.
     * Mit der vorgelagerten Sperre kommt es gar nicht erst zu dieser
     * Kollision.</p>
     *
     * <p>Das schuetzt innerhalb einer laufenden Anwendung. Mehrere Prozesse auf
     * derselben Datenbankdatei sind nicht vorgesehen - der Zugriff auf die
     * Datei ist ohnehin auf einen Schreiber beschraenkt.</p>
     */
    @Override
    public long nextDtaInterchangeReference() {
        referenzSperre.lock();
        try {
            return runner.write("Datenaustauschreferenz vergeben", session -> {
                DtaCounter counter = session.get(DtaCounter.class, 1L, LockMode.PESSIMISTIC_WRITE);
                if (counter == null) {
                    counter = new DtaCounter();
                    counter.setId(1L);
                    counter.setNextValue(1L);
                }
                long aktuell = counter.getNextValue() != null ? counter.getNextValue() : 1L;
                counter.setNextValue(aktuell + 1L);
                session.merge(counter);
                return aktuell;
            });
        } finally {
            referenzSperre.unlock();
        }
    }
}
