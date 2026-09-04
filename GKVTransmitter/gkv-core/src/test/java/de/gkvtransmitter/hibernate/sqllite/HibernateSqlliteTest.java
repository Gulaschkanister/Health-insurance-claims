package de.gkvtransmitter.hibernate.sqllite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;

/**
 * Prueft die SQLite-Persistenz gegen eine echte Datenbankdatei.
 *
 * <p>Diese Tests haben zwei Aufgaben. Zum einen decken sie den Umstieg auf
 * Hibernate 6 und {@code jakarta.persistence} ab, der sonst erst beim ersten
 * Programmstart aufgefallen waere. Zum anderen halten sie fest, dass gespeicherte
 * Daten einen Neustart ueberleben - genau das war mit
 * {@code hbm2ddl.auto=create} nicht der Fall.</p>
 */
class HibernateSqlliteTest {

    @TempDir
    Path tempDir;

    private Path dbFile;
    private HibernateSqllite repository;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("test.db");
        repository = HibernateSqllite.open(DatabaseSettings.forFile(dbFile));
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            repository.close();
        }
    }

    private static Patient patient(String vorname, String nachname) {
        return new Patient(vorname, nachname, "Musterstrasse", "DE", "1",
                12345, 101, 108310400, LocalDate.of(1990, 1, 1));
    }

    private static ServiceProvider provider(String vorname, String nachname) {
        return new ServiceProvider(vorname, nachname, "Praxisweg", "DE", "2",
                54321, 260326822, 108310400, LocalDate.of(1980, 5, 5));
    }

    // --- Grundlegendes Speichern und Laden -------------------------------

    @Test
    @DisplayName("Ein gespeicherter Patient laesst sich wieder laden")
    void speichertUndLaedtPatient() {
        repository.savePatient(patient("Anna", "Albers"));

        List<Patient> geladen = repository.getAllPatients();

        assertEquals(1, geladen.size());
        assertEquals("Anna", geladen.get(0).getFirstname());
        assertEquals("Albers", geladen.get(0).getLastname());
        assertEquals(108310400, geladen.get(0).getKassenIk());
    }

    @Test
    @DisplayName("Der Primaerschluessel wird von SQLite vergeben")
    void vergibtIdAutomatisch() {
        repository.savePatient(patient("Bernd", "Bauer"));

        Patient geladen = repository.getAllPatients().get(0);

        assertTrue(geladen.getId() > 0, "SQLite muss eine ID vergeben haben, war: " + geladen.getId());
        assertNotNull(repository.getPatientById(geladen.getId()));
    }

    @Test
    @DisplayName("Das Geburtsdatum bleibt als LocalDate erhalten")
    void haeltGeburtsdatum() {
        repository.savePatient(patient("Clara", "Conrad"));

        assertEquals(LocalDate.of(1990, 1, 1), repository.getAllPatients().get(0).getBirthDate());
    }

    @Test
    @DisplayName("Ein geloeschter Patient ist nicht mehr auffindbar")
    void loeschtPatient() {
        repository.savePatient(patient("Dora", "Deux"));
        Patient gespeichert = repository.getAllPatients().get(0);

        repository.deletePatient(gespeichert);

        assertTrue(repository.getAllPatients().isEmpty());
        assertNull(repository.getPatientById(gespeichert.getId()));
    }

    @Test
    @DisplayName("Leistungserbringer werden getrennt von Patienten verwaltet")
    void trenntPatientUndLeistungserbringer() {
        repository.savePatient(patient("Emil", "Ernst"));
        repository.saveServiceProvider(provider("Frida", "Fuchs"));

        assertEquals(1, repository.getAllPatients().size());
        assertEquals(1, repository.getAllServiceProviders().size());
        assertEquals("Frida", repository.getAllServiceProviders().get(0).getFirstname());
    }

    @Test
    @DisplayName("Nach dem Speichern traegt das uebergebene Objekt seine ID")
    void schreibtIdInDasUebergebeneObjekt() {
        Patient neu = patient("Nora", "Neu");
        assertEquals(0, neu.getId(), "vor dem Speichern hat der Patient keine ID");

        repository.savePatient(neu);

        assertTrue(neu.getId() > 0,
                "Das Speichern muss die vergebene ID in das uebergebene Objekt zurueckschreiben, "
                        + "sonst laesst es sich danach nicht weiterverwenden. War: " + neu.getId());
    }

    @Test
    @DisplayName("Frisch gespeicherte Objekte lassen sich unmittelbar zu einer Gruppe verbinden")
    void verbindetFrischGespeicherteObjekte() {
        // Genau der Ablauf beim Anlegen von Testdaten: erst die Personen
        // speichern, dann dieselben Objekte in eine Gruppe stecken. Ohne
        // zurueckgeschriebene ID haetten sie alle die ID 0 und die Gruppe
        // liesse sich nicht speichern.
        Patient p1 = patient("Otto", "Ohne");
        Patient p2 = patient("Paula", "Peters");
        ServiceProvider sp = provider("Quirin", "Quast");
        repository.savePatient(p1);
        repository.savePatient(p2);
        repository.saveServiceProvider(sp);

        PersonGroup gruppe = new PersonGroup("Frisch verbunden");
        gruppe.setPatients(new LinkedHashSet<>(List.of(p1, p2)));
        gruppe.setServiceProviders(new LinkedHashSet<>(List.of(sp)));
        repository.savePersonGroup(gruppe);

        PersonGroup geladen = repository.getAllPersonGroups().get(0);
        assertEquals(2, geladen.getPatients().size());
        assertEquals(1, geladen.getServiceProviders().size());
    }

    @Test
    @DisplayName("Eine Gruppe haelt ihre Patienten und Leistungserbringer")
    void speichertGruppeMitMitgliedern() {
        Patient p = patient("Gustav", "Gross");
        ServiceProvider sp = provider("Hanna", "Haas");
        repository.savePatient(p);
        repository.saveServiceProvider(sp);

        PersonGroup gruppe = new PersonGroup("Rueckbildungskurs");
        gruppe.setPatients(new LinkedHashSet<>(repository.getAllPatients()));
        gruppe.setServiceProviders(new LinkedHashSet<>(repository.getAllServiceProviders()));
        repository.savePersonGroup(gruppe);

        List<PersonGroup> gruppen = repository.getAllPersonGroups();
        assertEquals(1, gruppen.size());
        assertEquals("Rueckbildungskurs", gruppen.get(0).getName());
        assertEquals(1, gruppen.get(0).getPatients().size());
        assertEquals(1, gruppen.get(0).getServiceProviders().size());
    }

    @Test
    @DisplayName("Eine geloeschte Gruppe nimmt ihre Mitglieder nicht mit")
    void loeschtGruppeOhneMitglieder() {
        repository.savePatient(patient("Ida", "Iber"));
        PersonGroup gruppe = new PersonGroup("Temporaer");
        gruppe.setPatients(new LinkedHashSet<>(repository.getAllPatients()));
        repository.savePersonGroup(gruppe);

        repository.deletePersonGroup(repository.getAllPersonGroups().get(0));

        assertTrue(repository.getAllPersonGroups().isEmpty());
        assertEquals(1, repository.getAllPatients().size(),
                "Das Loeschen der Gruppe darf die Patienten nicht mitloeschen");
    }

    @Test
    @DisplayName("Blaupausen werden mit Zeitstempel gespeichert")
    void speichertBlaupause() {
        OffsetDateTime erstellt = OffsetDateTime.now();
        repository.saveBlueprint(new Blueprint("Kurs A", "antenatal_class_single", "{}", erstellt));

        List<Blueprint> geladen = repository.getAllBlueprints();

        assertEquals(1, geladen.size());
        assertEquals("Kurs A", geladen.get(0).getName());
        assertEquals("antenatal_class_single", geladen.get(0).getTemplateName());
        assertNotNull(geladen.get(0).getCreatedAt());
    }

    // --- Regressionstest zum Datenverlust --------------------------------

    @Test
    @DisplayName("Daten ueberleben einen Neustart der Anwendung")
    void datenUeberlebenNeustart() {
        repository.savePatient(patient("Jonas", "Jung"));
        repository.saveBlueprint(new Blueprint("Bestand", "tpl", "{}", OffsetDateTime.now()));
        repository.close();

        // Zweiter Programmstart gegen dieselbe Datei.
        repository = HibernateSqllite.open(DatabaseSettings.forFile(dbFile));

        assertEquals(1, repository.getAllPatients().size(),
                "Mit hbm2ddl.auto=create war die Tabelle nach dem Neustart leer");
        assertEquals("Jonas", repository.getAllPatients().get(0).getFirstname());
        assertEquals(1, repository.getAllBlueprints().size());
    }

    @Test
    @DisplayName("Die Datenbankdatei liegt am konfigurierten Ort")
    void legtDatenbankAmKonfiguriertenOrtAn() {
        repository.savePatient(patient("Karl", "Kunz"));

        assertTrue(Files.exists(dbFile), "erwartete Datenbankdatei unter " + dbFile);
    }

    @Test
    @DisplayName("Ein noch nicht vorhandenes Datenverzeichnis wird angelegt")
    void legtFehlendesVerzeichnisAn() {
        // SQLite legt eine Datenbank nur in einem bereits vorhandenen
        // Verzeichnis an. Fehlte es, meldete Hibernate lediglich "Cannot get a
        // connection as the driver manager is not properly initialized" - was
        // den eigentlichen Grund nicht erkennen liess. Betroffen war jeder
        // erste Start gegen ein frisches Datenverzeichnis.
        Path nochNichtVorhanden = tempDir.resolve("neu").resolve("tiefer").resolve("gkv.db");
        assertTrue(Files.notExists(nochNichtVorhanden.getParent()));

        try (HibernateSqllite frisch =
                HibernateSqllite.open(DatabaseSettings.forFile(nochNichtVorhanden))) {
            frisch.savePatient(patient("Lena", "Lang"));

            assertEquals(1, frisch.getAllPatients().size());
        }
        assertTrue(Files.exists(nochNichtVorhanden), "erwartete Datenbankdatei unter " + nochNichtVorhanden);
    }

    // --- Datenaustauschreferenz ------------------------------------------

    @Test
    @DisplayName("Die Datenaustauschreferenz zaehlt luecken- und wiederholungsfrei hoch")
    void zaehltReferenzHoch() {
        long erste = repository.nextDtaInterchangeReference();
        long zweite = repository.nextDtaInterchangeReference();
        long dritte = repository.nextDtaInterchangeReference();

        assertEquals(1L, erste);
        assertEquals(2L, zweite);
        assertEquals(3L, dritte);
    }

    @Test
    @DisplayName("Die Datenaustauschreferenz zaehlt ueber einen Neustart hinweg weiter")
    void zaehltReferenzNachNeustartWeiter() {
        repository.nextDtaInterchangeReference();
        repository.nextDtaInterchangeReference();
        repository.close();

        repository = HibernateSqllite.open(DatabaseSettings.forFile(dbFile));

        assertEquals(3L, repository.nextDtaInterchangeReference(),
                "Eine doppelt vergebene Referenz fuehrt zur Abweisung durch die Kasse");
    }

    @Test
    @DisplayName("Gleichzeitige Zugriffe erhalten niemals dieselbe Referenz")
    void vergibtReferenzNiemalsDoppelt() throws Exception {
        int anzahl = 20;
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Long>> aufgaben = IntStream.range(0, anzahl)
                    .mapToObj(i -> (Callable<Long>) () -> repository.nextDtaInterchangeReference())
                    .collect(Collectors.toList());

            List<Future<Long>> ergebnisse = pool.invokeAll(aufgaben, 60, TimeUnit.SECONDS);

            Set<Long> vergeben = new HashSet<>();
            for (Future<Long> future : ergebnisse) {
                vergeben.add(future.get());
            }
            assertEquals(anzahl, vergeben.size(),
                    "Jede Referenz darf nur einmal vergeben werden, erhalten: " + vergeben);
        } finally {
            pool.shutdownNow();
        }
    }
}
