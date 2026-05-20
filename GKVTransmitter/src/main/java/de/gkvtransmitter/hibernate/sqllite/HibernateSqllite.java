package de.gkvtransmitter.hibernate.sqllite;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.Person;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.util.HibernateUtil;

public class HibernateSqllite {

    private static HibernateSqllite instance;

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    private HibernateSqllite() {
    }

    public static synchronized HibernateSqllite getInstance() {
        if (instance == null) {
            instance = new HibernateSqllite();
        }
        return instance;
    }

    // ── Generic ────────────────────────────────────────────────────────────

    /**
     * Speichert oder aktualisiert eine Person-Entität (Patient oder ServiceProvider).
     */
    public void save(Person person) {
        Transaction transaction = null;
        try (Session session = sf.openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(person);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving person", e);
        }
    }

    /**
     * Löscht eine Person-Entität (Patient oder ServiceProvider).
     */
    public void delete(Person person) {
        Transaction transaction = null;
        try (Session session = sf.openSession()) {
            transaction = session.beginTransaction();
            Person managed = (Person) session.merge(person);
            session.delete(managed);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting person", e);
        }
    }

    // ── Patient ────────────────────────────────────────────────────────────

    /**
     * Speichert oder aktualisiert einen Patienten.
     */
    public void savePatient(Patient patient) {
        save(patient);
    }

    /**
     * Löscht einen Patienten.
     */
    public void deletePatient(Patient patient) {
        delete(patient);
    }

    /**
     * Gibt alle Patienten zurück.
     */
    public List<Patient> getAllPatients() {
        try (Session session = sf.openSession()) {
            return session.createQuery("FROM Patient", Patient.class).list();
        } catch (Exception e) {
            throw new RuntimeException("Error loading patients", e);
        }
    }

    /**
     * Gibt einen Patienten anhand der ID zurück.
     */
    public Patient getPatientById(int id) {
        try (Session session = sf.openSession()) {
            return session.get(Patient.class, id);
        } catch (Exception e) {
            throw new RuntimeException("Error loading patient with id: " + id, e);
        }
    }

    // ── ServiceProvider ────────────────────────────────────────────────────

    /**
     * Speichert oder aktualisiert einen Leistungserbringer.
     */
    public void saveServiceProvider(ServiceProvider serviceProvider) {
        save(serviceProvider);
    }

    /**
     * Löscht einen Leistungserbringer.
     */
    public void deleteServiceProvider(ServiceProvider serviceProvider) {
        delete(serviceProvider);
    }

    /**
     * Gibt alle Leistungserbringer zurück.
     */
    public List<ServiceProvider> getAllServiceProviders() {
        try (Session session = sf.openSession()) {
            return session.createQuery("FROM ServiceProvider", ServiceProvider.class).list();
        } catch (Exception e) {
            throw new RuntimeException("Error loading service providers", e);
        }
    }

    /**
     * Gibt einen Leistungserbringer anhand der ID zurück.
     */
    public ServiceProvider getServiceProviderById(int id) {
        try (Session session = sf.openSession()) {
            return session.get(ServiceProvider.class, id);
        } catch (Exception e) {
            throw new RuntimeException("Error loading service provider with id: " + id, e);
        }
    }

}
