package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.Person;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

/**
 * Prueft die Gruppenmaske.
 *
 * <p>Eine Gruppe ist die Voraussetzung jeder Abrechnung; was hier falsch
 * zusammengestellt wird, faellt erst bei der Kasse auf. Geprueft wird deshalb
 * vor allem, was gespeichert wird - nicht, wie es aussieht.</p>
 */
@DisplayName("Gruppenmaske")
class GruppenMaskeTest {

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
        @DisplayName("Ein leerer Gruppenname wird abgewiesen und nichts gespeichert")
        void leererName() {
            datenbank.mitPatient(patient(1, "Anna"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                speichern(formular).fire();

                assertEquals(texte.get("msg.groupNameRequired"), dialoge.einzige().text());
                assertTrue(datenbank.gespeicherteGruppen().isEmpty());
                assertEquals(0, rahmen.wieOftGeleert(), "Die Eingaben duerfen nicht verlorengehen");
            });
        }

        @Test
        @DisplayName("Ein Name aus Leerzeichen zaehlt als leer")
        void nurLeerzeichen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("   ");
                speichern(formular).fire();

                assertEquals(texte.get("msg.groupNameRequired"), dialoge.einzige().text());
                assertTrue(datenbank.gespeicherteGruppen().isEmpty());
            });
        }

        @Test
        @DisplayName("Nur angehakte Personen kommen in die Gruppe")
        void nurAngehakte() {
            datenbank.mitPatient(patient(1, "Anna")).mitPatient(patient(2, "Bernd"))
                    .mitDienstleister(dienstleister(9, "Max"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 2);
                anhaken(formular, GruppenMaske.ID_DIENSTLEISTER + 9);
                speichern(formular).fire();

                PersonGroup gespeichert = einzigeGespeicherte();
                assertEquals("Montagsgruppe", gespeichert.getName());
                assertEquals(List.of(2), kennungen(gespeichert.getPatients()));
                assertEquals(List.of(9), kennungen(gespeichert.getServiceProviders()));
            });
        }

        @Test
        @DisplayName("Der Name wird ohne umgebende Leerzeichen gespeichert")
        void nameGetrimmt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("  Montagsgruppe  ");
                speichern(formular).fire();

                assertEquals("Montagsgruppe", einzigeGespeicherte().getName());
            });
        }

        @Test
        @DisplayName("Nach dem Speichern wird der Bereich geraeumt")
        void bereichGeraeumt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                speichern(formular).fire();

                assertEquals(1, rahmen.wieOftGeleert());
                assertEquals(texte.get("msg.groupCreated"), dialoge.einzige().text());
            });
        }

        @Test
        @DisplayName("Abbrechen raeumt den Bereich, ohne zu speichern")
        void abbrechen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                ((Button) formular.lookup("#" + GruppenMaske.ID_ABBRECHEN)).fire();

                assertEquals(1, rahmen.wieOftGeleert());
                assertTrue(datenbank.gespeicherteGruppen().isEmpty());
                assertTrue(dialoge.leer());
            });
        }

        @Test
        @DisplayName("Ohne Personen in der Datenbank steht ein Hinweis statt einer leeren Liste")
        void keinePersonen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();

                assertNull(formular.lookup("#" + GruppenMaske.ID_TEILNEHMER + "1"));
                assertNotNull(namensfeld(formular), "Das Formular muss trotzdem bedienbar sein");
            });
        }

        @Test
        @DisplayName("Ein Fehlschlag beim Speichern wird gemeldet und der Bereich bleibt stehen")
        void speichernScheitert() {
            datenbank.scheitertBeimSpeichern(new IllegalStateException("Datenbank gesperrt"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                speichern(formular).fire();

                AufzeichnendeDialoge.Meldung meldung = dialoge.einzige();
                assertEquals(AufzeichnendeDialoge.Art.FEHLER, meldung.art());
                assertTrue(meldung.text().contains("Datenbank gesperrt"), meldung.text());
                assertEquals(0, rahmen.wieOftGeleert());
            });
        }
    }

    @Nested
    @DisplayName("Bearbeiten")
    class Bearbeiten {

        @Test
        @DisplayName("Ohne Gruppen meldet die Maske das, statt ein leeres Formular zu zeigen")
        void keineGruppen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().bearbeiten();

                assertEquals(texte.get("msg.noGroups"), dialoge.einzige().text());
                assertNull(rahmen.inhalt());
            });
        }

        @Test
        @DisplayName("Die bisherigen Mitglieder sind vorausgewaehlt")
        void mitgliederVorausgewaehlt() {
            Patient anna = patient(1, "Anna");
            Patient bernd = patient(2, "Bernd");
            PersonGroup vorhanden = new PersonGroup("Montagsgruppe");
            vorhanden.setPatients(new LinkedHashSet<>(List.of(bernd)));
            datenbank.mitPatient(anna).mitPatient(bernd).mitGruppe(vorhanden);
            dialoge.waehltEintrag(0);

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().bearbeiten();

                Region formular = rahmen.inhalt();
                assertNotNull(formular, "Das Formular muss gezeigt werden");
                assertEquals("Montagsgruppe", namensfeld(formular).getText());
                assertFalse(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 1).isSelected());
                assertTrue(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 2).isSelected());
            });
        }

        @Test
        @DisplayName("Ein Abbruch der Auswahl zeigt kein Formular")
        void auswahlAbgebrochen() {
            datenbank.mitGruppe(new PersonGroup("Montagsgruppe"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().bearbeiten();

                assertNull(rahmen.inhalt());
                assertTrue(dialoge.leer());
            });
        }

        @Test
        @DisplayName("Das Bearbeiten aendert die vorhandene Gruppe, statt eine zweite anzulegen")
        void aendertVorhandene() {
            Patient anna = patient(1, "Anna");
            PersonGroup vorhanden = new PersonGroup("Alt");
            datenbank.mitPatient(anna).mitGruppe(vorhanden);
            dialoge.waehltEintrag(0);

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().bearbeiten();
                Region formular = rahmen.inhalt();
                namensfeld(formular).setText("Neu");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                speichern(formular).fire();

                assertEquals(List.of(vorhanden), datenbank.gespeicherteGruppen());
                assertEquals("Neu", vorhanden.getName());
                assertEquals(1, datenbank.getAllPersonGroups().size());
                assertEquals(texte.get("msg.groupUpdated"), dialoge.einzige().text());
            });
        }
    }

    @Nested
    @DisplayName("Loeschen")
    class Loeschen {

        @Test
        @DisplayName("Ohne Zustimmung bleibt die Gruppe bestehen")
        void ohneZustimmung() {
            datenbank.mitGruppe(new PersonGroup("Montagsgruppe"));
            dialoge.waehltEintrag(0);

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().loeschen();

                assertEquals(1, dialoge.gestellteRueckfragen().size(), "Es muss nachgefragt werden");
                assertEquals(1, datenbank.getAllPersonGroups().size());
                assertTrue(dialoge.leer(), "Ohne Loeschung gibt es nichts zu melden");
            });
        }

        @Test
        @DisplayName("Nach Zustimmung wird geloescht und das gemeldet")
        void mitZustimmung() {
            datenbank.mitGruppe(new PersonGroup("Montagsgruppe"));
            dialoge.waehltEintrag(0).stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().loeschen();

                assertTrue(datenbank.getAllPersonGroups().isEmpty());
                assertEquals(texte.get("msg.groupDeleted"), dialoge.einzige().text());
            });
        }

        @Test
        @DisplayName("Die Rueckfrage nennt die Gruppe beim Namen")
        void rueckfrageNenntGruppe() {
            datenbank.mitGruppe(new PersonGroup("Montagsgruppe"));
            dialoge.waehltEintrag(0);

            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().loeschen();

                assertTrue(dialoge.gestellteRueckfragen().get(0).contains("Montagsgruppe"),
                        dialoge.gestellteRueckfragen().toString());
            });
        }

        @Test
        @DisplayName("Ohne Gruppen wird gar nicht erst nachgefragt")
        void keineGruppen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                maske().loeschen();

                assertEquals(texte.get("msg.noGroups"), dialoge.einzige().text());
                assertTrue(dialoge.gestellteRueckfragen().isEmpty());
            });
        }
    }

    // --- Aufbau und Bedienung -------------------------------------------

    private GruppenMaske maske() {
        return new GruppenMaske(new JavaFxUiFactory(), texte, dialoge, datenbank, rahmen);
    }

    private Region neuesFormular() {
        return maske().formular(null);
    }

    private TextField namensfeld(Region formular) {
        return (TextField) formular.lookup("#" + GruppenMaske.ID_NAME);
    }

    private Button speichern(Region formular) {
        return (Button) formular.lookup("#" + GruppenMaske.ID_SPEICHERN);
    }

    private CheckBox kaestchen(Region formular, String kennung) {
        CheckBox kaestchen = (CheckBox) formular.lookup("#" + kennung);
        assertNotNull(kaestchen, "Kein Auswahlkaestchen mit der Kennung " + kennung);
        return kaestchen;
    }

    private void anhaken(Region formular, String kennung) {
        kaestchen(formular, kennung).setSelected(true);
    }

    private PersonGroup einzigeGespeicherte() {
        List<PersonGroup> gespeichert = datenbank.gespeicherteGruppen();
        assertEquals(1, gespeichert.size(), "Erwartet war genau ein Speichervorgang");
        return gespeichert.get(0);
    }

    private static List<Integer> kennungen(Set<? extends Person> personen) {
        return personen.stream().map(Person::getId).toList();
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
