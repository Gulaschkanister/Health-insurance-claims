package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.presentation.populator.PatientFieldPopulator;
import de.gkvtransmitter.presentation.populator.ServiceProviderFieldPopulator;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Prueft die Personenmaske.
 *
 * <p>Teilnehmer und Dienstleister teilen sich das Formular. Die Tests achten
 * deshalb vor allem darauf, dass die Rolle sich durchhaelt: dass ein
 * Dienstleister auch als solcher gespeichert und gemeldet wird.</p>
 */
@DisplayName("Personenmaske")
class PersonenMaskeTest {

    private static final int KASSEN_IK = 108310400;

    private SpeicherRepository datenbank;
    private AufzeichnendeDialoge dialoge;
    private AufzeichnenderRahmen rahmen;
    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        dialoge = new AufzeichnendeDialoge();
        rahmen = new AufzeichnenderRahmen();
        texte = new AppMessages("/messages/ui-messages.json");
    }

    @Nested
    @DisplayName("Anlegen")
    class Anlegen {

        @Test
        @DisplayName("Ein Teilnehmer wird mit den eingegebenen Werten angelegt")
        void teilnehmerAngelegt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(1, datenbank.getAllPatients().size());
                Patient angelegt = datenbank.getAllPatients().get(0);
                assertEquals("Anna", angelegt.getFirstname());
                assertEquals("Muster", angelegt.getLastname());
                assertEquals("Musterweg", angelegt.getStreet());
                assertEquals(12345, angelegt.getPlz());
                assertEquals(KASSEN_IK, angelegt.getKassenIk());
                assertTrue(datenbank.getAllServiceProviders().isEmpty(),
                        "Es darf kein Dienstleister entstanden sein");
            });
        }

        @Test
        @DisplayName("Ein Dienstleister wird als Dienstleister gespeichert und auch so gemeldet")
        void dienstleisterAngelegt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(true);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(1, datenbank.getAllServiceProviders().size());
                ServiceProvider angelegt = datenbank.getAllServiceProviders().get(0);
                assertEquals("Anna", angelegt.getFirstname());
                assertTrue(datenbank.getAllPatients().isEmpty(), "Es darf kein Teilnehmer entstanden sein");
                assertEquals(texte.get("msg.selfCreated"), dialoge.einzige().text());
            });
        }

        @Test
        @DisplayName("Ein Teilnehmer wird als Teilnehmer gemeldet")
        void teilnehmerGemeldet() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(texte.get("msg.patientCreated"), dialoge.einzige().text());
            });
        }

        @Test
        @DisplayName("Ein eingetragenes Geburtsdatum wird uebernommen")
        void geburtsdatum() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                kalender(formular, "birthDate").setValue(LocalDate.of(1980, 5, 17));
                speichern(formular).fire();

                assertEquals(LocalDate.of(1980, 5, 17), datenbank.getAllPatients().get(0).getBirthDate());
            });
        }

        @Test
        @DisplayName("Nach dem Speichern wird der Bereich geraeumt")
        void bereichGeraeumt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(1, rahmen.wieOftGeleert());
            });
        }

        @Test
        @DisplayName("Abbrechen raeumt den Bereich, ohne zu speichern")
        void abbrechen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                ((Button) formular.lookup("#" + PersonenMaske.ID_ABBRECHEN)).fire();

                assertEquals(1, rahmen.wieOftGeleert());
                assertTrue(datenbank.getAllPatients().isEmpty());
                assertTrue(dialoge.leer());
            });
        }

        @Test
        @DisplayName("Ein Fehlschlag beim Speichern wird gemeldet und der Bereich bleibt stehen")
        void speichernScheitert() {
            datenbank.scheitertBeimSpeichern(new IllegalStateException("Datenbank gesperrt"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                AufzeichnendeDialoge.Meldung meldung = dialoge.einzige();
                assertEquals(AufzeichnendeDialoge.Art.FEHLER, meldung.art());
                assertTrue(meldung.text().contains("Datenbank gesperrt"), meldung.text());
                assertEquals(0, rahmen.wieOftGeleert());
            });
        }

        @Test
        @DisplayName("Das Formular bringt fuer jedes beschriebene Feld ein Eingabeelement mit")
        void alleFelder() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);

                for (String feld : new String[] {"firstname", "lastname", "street", "country",
                        "housenumber", "plz", "ik", "kassenIk", "birthDate"}) {
                    assertNotNull(formular.lookup("#" + PersonenMaske.ID_FELD + feld),
                            "Kein Eingabefeld fuer " + feld);
                }
            });
        }
    }

    @Nested
    @DisplayName("Loeschen")
    class Loeschen {

        @Test
        @DisplayName("Ohne Teilnehmer wird gar nicht erst nachgefragt")
        void keineTeilnehmer() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().teilnehmerLoeschen();

                assertEquals(texte.get("msg.noPatients"), dialoge.einzige().text());
                assertTrue(dialoge.gestellteRueckfragen().isEmpty());
            });
        }

        @Test
        @DisplayName("Ohne Zustimmung bleibt der Teilnehmer bestehen")
        void ohneZustimmung() {
            datenbank.mitPatient(patient(1, "Anna"));
            dialoge.waehltEintrag(0);

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().teilnehmerLoeschen();

                assertEquals(1, dialoge.gestellteRueckfragen().size(), "Es muss nachgefragt werden");
                assertEquals(1, datenbank.getAllPatients().size());
                assertTrue(dialoge.leer());
            });
        }

        @Test
        @DisplayName("Nach Zustimmung wird der Teilnehmer geloescht und das gemeldet")
        void mitZustimmung() {
            datenbank.mitPatient(patient(1, "Anna"));
            dialoge.waehltEintrag(0).stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().teilnehmerLoeschen();

                assertTrue(datenbank.getAllPatients().isEmpty());
                assertEquals(texte.get("msg.patientDeleted"), dialoge.einzige().text());
            });
        }

        @Test
        @DisplayName("Der Dienstleister wird eigenstaendig geloescht und eigenstaendig gemeldet")
        void dienstleisterGeloescht() {
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"));
            dialoge.waehltEintrag(0).stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().dienstleisterLoeschen();

                assertTrue(datenbank.getAllServiceProviders().isEmpty());
                assertEquals(1, datenbank.getAllPatients().size(), "Der Teilnehmer bleibt unberuehrt");
                assertEquals(texte.get("msg.selfDeleted"), dialoge.einzige().text());
            });
        }
    }

    @Nested
    @DisplayName("Bearbeiten")
    class Bearbeiten {

        @Test
        @DisplayName("Ohne Teilnehmer meldet die Maske das, statt ein leeres Formular zu zeigen")
        void keineTeilnehmer() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().teilnehmerBearbeiten();

                assertEquals(texte.get("msg.noPatients"), dialoge.einzige().text());
                assertNull(rahmen.inhalt());
            });
        }

        @Test
        @DisplayName("Ein Abbruch der Auswahl zeigt kein Formular")
        void auswahlAbgebrochen() {
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().teilnehmerBearbeiten();

                assertNull(rahmen.inhalt());
                assertTrue(dialoge.leer());
            });
        }

        @Test
        @DisplayName("Der gewaehlte Teilnehmer wird zum Bearbeiten gezeigt")
        void formularGezeigt() {
            datenbank.mitPatient(patient(1, "Anna"));
            dialoge.waehltEintrag(0);

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().teilnehmerBearbeiten();

                assertNotNull(rahmen.inhalt(), "Das Bearbeitungsformular muss gezeigt werden");
            });
        }
    }

    // --- Aufbau und Bedienung -------------------------------------------

    private PersonenMaske maske() {
        UiFactory bausteine = new JavaFxUiFactory();
        return new PersonenMaske(bausteine, texte, dialoge, datenbank, rahmen,
                new Feldbau(bausteine, texte), new PatientFieldPopulator(),
                new ServiceProviderFieldPopulator());
    }

    private Region formular(boolean alsDienstleister) {
        return maske().formular("Ueberschrift", alsDienstleister);
    }

    /** Traegt einen vollstaendigen, gueltigen Satz Werte ein. */
    private void fuelleAus(Region formular) {
        text(formular, "firstname").setText("Anna");
        text(formular, "lastname").setText("Muster");
        text(formular, "street").setText("Musterweg");
        text(formular, "country").setText("DE");
        text(formular, "housenumber").setText("1");
        zahl(formular, "plz", 12345);
        zahl(formular, "ik", 101);
        zahl(formular, "kassenIk", KASSEN_IK);
    }

    private TextField text(Region formular, String feldname) {
        return (TextField) feld(formular, feldname);
    }

    private DatePicker kalender(Region formular, String feldname) {
        return (DatePicker) feld(formular, feldname);
    }

    /**
     * Setzt einen Zahlenwert.
     *
     * <p>Zahlenfelder tragen eine Beschriftung fuer die Beanstandung unter
     * sich; das Bedienelement ist deshalb das erste Kind des Feldes.</p>
     */
    private void zahl(Region formular, String feldname, int wert) {
        VBox umhuellung = (VBox) feld(formular, feldname);
        @SuppressWarnings("unchecked")
        Spinner<Integer> zaehler = (Spinner<Integer>) umhuellung.getChildren().get(0);
        zaehler.getValueFactory().setValue(wert);
    }

    private Node feld(Region formular, String feldname) {
        Node feld = formular.lookup("#" + PersonenMaske.ID_FELD + feldname);
        assertNotNull(feld, "Kein Eingabefeld fuer " + feldname);
        return feld;
    }

    private Button speichern(Region formular) {
        return (Button) formular.lookup("#" + PersonenMaske.ID_SPEICHERN);
    }

    // --- Testdaten -------------------------------------------------------

    private static Patient patient(int id, String vorname) {
        Patient person = new Patient(vorname, "Muster", "Musterweg", "DE", "1", 12345, 101, KASSEN_IK, null);
        person.setId(id);
        return person;
    }

    private static ServiceProvider dienstleister(int id, String vorname) {
        ServiceProvider person =
                new ServiceProvider(vorname, "Muster", "Musterstr.", "DE", "1", 12345, 1001, KASSEN_IK, null);
        person.setId(id);
        return person;
    }
}
