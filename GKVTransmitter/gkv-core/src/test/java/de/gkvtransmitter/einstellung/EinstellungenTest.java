package de.gkvtransmitter.einstellung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.hibernate.sqllite.DatabaseSettings;
import de.gkvtransmitter.hibernate.sqllite.HibernateSqllite;
import de.gkvtransmitter.repository.DataRepository;

/**
 * Prueft die Einstellungen.
 *
 * <p>Sie sind die erste Stelle, an der die Anwendung sich etwas merkt, das
 * keine Fachangabe ist. Seit dem 07.09.2026 liegen sie in der Tabelle
 * {@code einstellung} der Datenbank und nicht mehr in einer eigenen Datei
 * daneben; die Tests laufen deshalb gegen eine echte SQLite-Datei, wie alle
 * Persistenztests des Projekts.</p>
 *
 * <p>Wichtiger als das Speichern selbst ist, <b>was geschieht, wenn es
 * schiefgeht</b>: ein Lesefehler darf den Start nicht aufhalten, und ein
 * gescheitertes Schreiben darf die getroffene Wahl nicht mitnehmen.</p>
 */
@DisplayName("Einstellungen")
class EinstellungenTest {

    @TempDir
    Path ordner;

    private HibernateSqllite datenbank;

    @BeforeEach
    void oeffne() {
        datenbank = HibernateSqllite.open(DatabaseSettings.forFile(ordner.resolve("test.db")));
    }

    @AfterEach
    void schliesse() {
        if (datenbank != null) {
            datenbank.close();
        }
    }

    @Nested
    @DisplayName("Lesen")
    class Lesen {

        @Test
        @DisplayName("Ohne gespeicherten Wert gilt die Vorgabe")
        void ohneWert() {
            Einstellungen einstellungen = Einstellungen.aus(datenbank);

            assertEquals("hell", einstellungen.get(Einstellung.DARSTELLUNG));
            assertEquals("erprobung", einstellungen.get(Einstellung.UEBERMITTLUNGSART));
        }

        @Test
        @DisplayName("Ein leer gespeicherter Wert zaehlt als nicht gesetzt")
        void leererWert() {
            datenbank.speichereEinstellung(Einstellung.DARSTELLUNG.schluessel(), "   ");

            assertEquals("hell", Einstellungen.aus(datenbank).get(Einstellung.DARSTELLUNG));
        }

        @Test
        @DisplayName("Ein Lesefehler haelt nichts auf - es gelten die Vorgaben")
        void lesefehler() {
            Einstellungen einstellungen = Einstellungen.aus(new UnlesbaresRepository());

            assertEquals("hell", einstellungen.get(Einstellung.DARSTELLUNG));
        }
    }

    @Nested
    @DisplayName("Schreiben")
    class Schreiben {

        @Test
        @DisplayName("Ein gesetzter Wert ueberlebt das Schliessen der Datenbank")
        void ueberlebtNeustart() {
            assertTrue(Einstellungen.aus(datenbank).setze(Einstellung.DARSTELLUNG, "dunkel"));
            datenbank.close();

            datenbank = HibernateSqllite.open(DatabaseSettings.forFile(ordner.resolve("test.db")));
            assertEquals("dunkel", Einstellungen.aus(datenbank).get(Einstellung.DARSTELLUNG),
                    "Eine Einstellung, die den Neustart nicht ueberlebt, ist keine");
        }

        @Test
        @DisplayName("Ein leerer Wert loescht die Zeile und stellt die Vorgabe wieder her")
        void leerLoescht() {
            Einstellungen einstellungen = Einstellungen.aus(datenbank);
            einstellungen.setze(Einstellung.DARSTELLUNG, "dunkel");

            einstellungen.setze(Einstellung.DARSTELLUNG, "");

            assertEquals("hell", Einstellungen.aus(datenbank).get(Einstellung.DARSTELLUNG));
            assertFalse(datenbank.ladeEinstellungen().containsKey(Einstellung.DARSTELLUNG.schluessel()),
                    "Eine geleerte Einstellung soll keine Zeile hinterlassen: sonst gaebe es zwei "
                            + "Zustaende mit derselben Bedeutung");
        }

        @Test
        @DisplayName("Zwei Einstellungen stehen nebeneinander, ohne sich zu ueberschreiben")
        void nebeneinander() {
            Einstellungen einstellungen = Einstellungen.aus(datenbank);
            einstellungen.setze(Einstellung.DARSTELLUNG, "dunkel");
            einstellungen.setze(Einstellung.UEBERMITTLUNGSART, "echt");

            Einstellungen erneut = Einstellungen.aus(datenbank);
            assertEquals("dunkel", erneut.get(Einstellung.DARSTELLUNG));
            assertEquals("echt", erneut.get(Einstellung.UEBERMITTLUNGSART));
        }

        @Test
        @DisplayName("Scheitert das Speichern, gilt der Wert trotzdem - aber nur fuer diese Sitzung")
        void schreibenScheitert() {
            Einstellungen einstellungen = Einstellungen.aus(new UnschreibbaresRepository());

            assertFalse(einstellungen.setze(Einstellung.DARSTELLUNG, "dunkel"),
                    "Ein gescheitertes Speichern muss sich melden");
            assertEquals("dunkel", einstellungen.get(Einstellung.DARSTELLUNG),
                    "Wer die dunkle Fassung waehlt, soll sie sehen - auch wenn sie nicht bleibt");
        }
    }

    @Nested
    @DisplayName("Ablageort")
    class Ablageort {

        @Test
        @DisplayName("Wird durchgereicht, damit die Maske ihn nennen kann")
        void ortWirdGereicht() {
            assertEquals("irgendwo/database.db",
                    Einstellungen.aus(datenbank, "irgendwo/database.db").ort());
        }

        @Test
        @DisplayName("Ohne Angabe bleibt er leer und wird nicht erfunden")
        void ohneOrt() {
            assertEquals("", Einstellungen.aus(datenbank).ort());
        }
    }

    /** Eine Datenbank, aus der sich nichts lesen laesst. */
    private static class UnlesbaresRepository extends NutzlosesRepository {
        @Override
        public Map<String, String> ladeEinstellungen() {
            throw new IllegalStateException("Tabelle nicht lesbar");
        }
    }

    /** Eine Datenbank, in die sich nichts schreiben laesst. */
    private static class UnschreibbaresRepository extends NutzlosesRepository {
        @Override
        public void speichereEinstellung(String schluessel, String wert) {
            throw new IllegalStateException("Tabelle nicht schreibbar");
        }
    }

    /**
     * Alles, was {@link DataRepository} sonst noch verlangt.
     *
     * <p>Die beiden Faelle oben brauchen genau eine Methode, die anders
     * reagiert als sonst. Der Rest steht hier, damit er dort nicht vom
     * Wesentlichen ablenkt.</p>
     */
    private static class NutzlosesRepository implements DataRepository {
        @Override
        public void savePatient(Patient patient) {
        }

        @Override
        public void saveServiceProvider(ServiceProvider serviceProvider) {
        }

        @Override
        public List<Patient> getAllPatients() {
            return List.of();
        }

        @Override
        public List<ServiceProvider> getAllServiceProviders() {
            return List.of();
        }

        @Override
        public List<PersonGroup> getAllPersonGroups() {
            return List.of();
        }

        @Override
        public Patient getPatientById(int id) {
            return null;
        }

        @Override
        public void deletePatient(Patient patient) {
        }

        @Override
        public void deleteServiceProvider(ServiceProvider serviceProvider) {
        }

        @Override
        public void savePersonGroup(PersonGroup personGroup) {
        }

        @Override
        public void deletePersonGroup(PersonGroup personGroup) {
        }

        @Override
        public void saveBlueprint(Blueprint blueprint) {
        }

        @Override
        public void deleteBlueprint(Blueprint blueprint) {
        }

        @Override
        public List<Blueprint> getAllBlueprints() {
            return List.of();
        }

        @Override
        public long nextDtaInterchangeReference() {
            return 1;
        }

        @Override
        public Map<String, String> ladeEinstellungen() {
            return new LinkedHashMap<>();
        }

        @Override
        public void speichereEinstellung(String schluessel, String wert) {
        }
    }
}
