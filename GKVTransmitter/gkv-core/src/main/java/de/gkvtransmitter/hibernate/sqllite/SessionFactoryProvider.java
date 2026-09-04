package de.gkvtransmitter.hibernate.sqllite;

import java.util.Objects;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.DtaCounter;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;

/**
 * Baut die Hibernate-{@link SessionFactory} auf.
 *
 * <p>Die Vorgaengerfassung war ein statischer Initialisierer, der die
 * {@code hibernate.cfg.xml} eins zu eins uebernommen hat. Damit liess sich weder
 * die Datenbankdatei wechseln noch eine Testdatenbank verwenden, und ein Fehler
 * beim Laden hat die Klasse dauerhaft unbrauchbar gemacht
 * ({@code ExceptionInInitializerError} beim ersten Zugriff, danach
 * {@code NoClassDefFoundError}).</p>
 *
 * <p>Jetzt liefert die Klasse eine {@link SessionFactory} zu uebergebenen
 * {@link DatabaseSettings}. Die entity-Klassen werden explizit registriert,
 * damit ein vergessener Eintrag beim Uebersetzen und nicht erst zur Laufzeit
 * auffaellt.</p>
 */
public final class SessionFactoryProvider {

    private SessionFactoryProvider() {
    }

    /**
     * Erzeugt eine neue {@link SessionFactory}. Der Aufrufer ist fuer das
     * Schliessen verantwortlich.
     */
    public static SessionFactory create(DatabaseSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");

        // Muss vor dem Verbindungsaufbau geschehen: SQLite legt eine Datenbank
        // nur in einem bereits vorhandenen Verzeichnis an.
        settings.sicherstelleVerzeichnis();

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure("hibernate.cfg.xml")
                .applySetting(AvailableSettings.URL, settings.getJdbcUrl())
                .applySetting(AvailableSettings.HBM2DDL_AUTO, settings.getSchemaMode())
                .build();
        try {
            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(Patient.class)
                    .addAnnotatedClass(ServiceProvider.class)
                    .addAnnotatedClass(PersonGroup.class)
                    .addAnnotatedClass(Blueprint.class)
                    .addAnnotatedClass(DtaCounter.class)
                    .getMetadataBuilder()
                    .build();
            return metadata.getSessionFactoryBuilder().build();
        } catch (RuntimeException e) {
            // Ohne das Freigeben bleibt bei einem Fehlschlag ein Registry-Objekt
            // samt Verbindungspool zurueck.
            StandardServiceRegistryBuilder.destroy(registry);
            throw new IllegalStateException(
                    "SessionFactory konnte nicht aufgebaut werden fuer " + settings.getBeschreibung(), e);
        }
    }
}
