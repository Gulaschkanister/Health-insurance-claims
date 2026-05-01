package de.gkvtransmitter.hibernate.sqllite;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.util.HibernateUtil;

public class HibernateSqllite {
    private final SessionFactory sf = HibernateUtil.getSessionFactory();
    private HibernateSqllite hbsqli;

    public HibernateSqllite getInstance() {
        if (this.hbsqli == null) {
            this.hbsqli = new HibernateSqllite();
        }
        return hbsqli;
    }
    
    /**
     * Save or update a Patient in the database.
     */
    public void savePatient(Patient patient) {
        Transaction transaction = null;
        try (Session session = sf.openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(patient);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving patient", e);
        }
    }

    /**
     * Save or update a ServiceProvider in the database.
     */
    public void saveServiceProvider(ServiceProvider serviceProvider) {
        Transaction transaction = null;
        try (Session session = sf.openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(serviceProvider);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving service provider", e);
        }
    }
    
    /**
     * Load all patients from the database.
     */
    public List<Patient> getAllPatients() {
        try (Session session = sf.openSession()) {
            return session.createQuery("FROM Patient", Patient.class).list();
        } catch (Exception e) {
            throw new RuntimeException("Error loading patients", e);
        }
    }
    
    /**
     * Load a patient by ID.
     */
    public Patient getPatientById(int id) {
        try (Session session = sf.openSession()) {
            return session.get(Patient.class, id);
        } catch (Exception e) {
            throw new RuntimeException("Error loading patient with id: " + id, e);
        }
    }
    
    /**
     * Delete a patient from the database.
     */
    public void deletePatient(Patient patient) {
        Transaction transaction = null;
        try (Session session = sf.openSession()) {
            transaction = session.beginTransaction();
            Patient managed = (Patient) session.merge(patient);
            session.delete(managed);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting patient", e);
        }
    }

}
