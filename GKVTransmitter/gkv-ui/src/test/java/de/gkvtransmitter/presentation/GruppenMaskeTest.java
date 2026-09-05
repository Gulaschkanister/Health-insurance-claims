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
import javafx.scene.control.Label;
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
    private AufzeichnendeMeldungen meldungen;
    private AufzeichnenderRahmen rahmen;
    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        meldungen = new AufzeichnendeMeldungen();
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

                assertEquals(texte.get("msg.groupNameRequired"), meldungen.einzige().text());
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

                assertEquals(texte.get("msg.groupNameRequired"), meldungen.einzige().text());
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
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("  Montagsgruppe  ");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                anhaken(formular, GruppenMaske.ID_DIENSTLEISTER + 9);
                speichern(formular).fire();

                assertEquals("Montagsgruppe", einzigeGespeicherte().getName());
            });
        }

        @Test
        @DisplayName("Nach dem Speichern wird der Bereich geraeumt")
        void bereichGeraeumt() {
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                anhaken(formular, GruppenMaske.ID_DIENSTLEISTER + 9);
                speichern(formular).fire();

                assertEquals(1, rahmen.wieOftGeleert());
                assertEquals(String.format(texte.get("msg.groupCreated"), "Montagsgruppe"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Eine Gruppe ohne Teilnehmer oder Dienstleister wird abgewiesen")
        void unvollstaendigeGruppe() {
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                // Dienstleister absichtlich nicht angehakt.
                speichern(formular).fire();

                assertTrue(datenbank.gespeicherteGruppen().isEmpty(),
                        "Ohne Dienstleister fehlt der Abrechnung das Absender-IK");
                assertTrue(meldungen.einzige().text().contains(texte.get("msg.groupNeedsProvider")),
                        meldungen.einzige().text());
                assertEquals(0, rahmen.wieOftGeleert(), "Die Eingaben duerfen nicht verlorengehen");
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
                assertTrue(meldungen.leer());
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
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"))
                    .scheitertBeimSpeichern(new IllegalStateException("Datenbank gesperrt"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                anhaken(formular, GruppenMaske.ID_DIENSTLEISTER + 9);
                speichern(formular).fire();

                AufzeichnendeMeldungen.Meldung meldung = meldungen.einzige();
                assertEquals(AufzeichnendeMeldungen.Art.FEHLER, meldung.art());
                assertTrue(meldung.text().contains("Datenbank gesperrt"), meldung.text());
                assertEquals(0, rahmen.wieOftGeleert());
            });
        }
    }


    @Nested
    @DisplayName("Uebersicht")
    class Uebersicht {

        @Test
        @DisplayName("Ohne Gruppen steht ein Hinweis in der Liste, kein leeres Formular")
        void keineGruppen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = maske().liste();

                assertNull(bearbeitenKnopf(liste, 1), "Ohne Gruppen darf keine Zeile entstehen");
                assertTrue(meldungen.leer(), "Der Hinweis gehoert in die Liste, nicht in eine Meldung");
            });
        }

        @Test
        @DisplayName("Die Liste nennt Name und Zahl der Mitglieder")
        void mitgliederzahl() {
            PersonGroup gruppe = new PersonGroup("Montagsgruppe");
            gruppe.setId(7);
            gruppe.setPatients(new LinkedHashSet<>(List.of(patient(1, "Anna"), patient(2, "Bernd"))));
            gruppe.setServiceProviders(new LinkedHashSet<>(List.of(dienstleister(9, "Max"))));
            datenbank.mitGruppe(gruppe);

            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> zeile = zeilentexte(maske().liste());

                assertTrue(zeile.contains("Montagsgruppe"), zeile.toString());
                assertTrue(zeile.contains("2"), "Zahl der Teilnehmer fehlt: " + zeile);
                assertTrue(zeile.contains("1"), "Zahl der Dienstleister fehlt: " + zeile);
            });
        }

        @Test
        @DisplayName("Das Suchfeld blendet aus, was nicht passt")
        void suche() {
            datenbank.mitGruppe(gruppe(1, "Montagsgruppe")).mitGruppe(gruppe(2, "Freitagsgruppe"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = maske().liste();
                assertNotNull(bearbeitenKnopf(liste, 1));
                assertNotNull(bearbeitenKnopf(liste, 2));

                suchfeld(liste).setText("freitag");

                assertNull(bearbeitenKnopf(liste, 1), "Die Montagsgruppe passt nicht zur Suche");
                assertNotNull(bearbeitenKnopf(liste, 2));
            });
        }

        @Test
        @DisplayName("Die Suche unterscheidet nicht zwischen gross und klein")
        void sucheOhneRuecksichtAufSchreibweise() {
            datenbank.mitGruppe(gruppe(1, "Montagsgruppe"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = maske().liste();
                suchfeld(liste).setText("MONTAG");

                assertNotNull(bearbeitenKnopf(liste, 1));
            });
        }
    }

    @Nested
    @DisplayName("Bearbeiten")
    class Bearbeiten {

        @Test
        @DisplayName("Die bisherigen Mitglieder sind vorausgewaehlt")
        void mitgliederVorausgewaehlt() {
            Patient anna = patient(1, "Anna");
            Patient bernd = patient(2, "Bernd");
            PersonGroup vorhanden = gruppe(7, "Montagsgruppe");
            vorhanden.setPatients(new LinkedHashSet<>(List.of(bernd)));
            datenbank.mitPatient(anna).mitPatient(bernd).mitGruppe(vorhanden);

            JavaFxLaufzeit.aufFxFaden(() -> {
                bearbeitenKnopf(maske().liste(), 7).fire();

                Region formular = rahmen.inhalt();
                assertNotNull(formular, "Das Formular muss gezeigt werden");
                assertEquals("Montagsgruppe", namensfeld(formular).getText());
                assertFalse(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 1).isSelected());
                assertTrue(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 2).isSelected());
            });
        }

        @Test
        @DisplayName("Das Bearbeiten aendert die vorhandene Gruppe, statt eine zweite anzulegen")
        void aendertVorhandene() {
            Patient anna = patient(1, "Anna");
            PersonGroup vorhanden = gruppe(7, "Alt");
            datenbank.mitPatient(anna).mitDienstleister(dienstleister(9, "Max")).mitGruppe(vorhanden);

            JavaFxLaufzeit.aufFxFaden(() -> {
                bearbeitenKnopf(maske().liste(), 7).fire();
                Region formular = rahmen.inhalt();
                namensfeld(formular).setText("Neu");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                anhaken(formular, GruppenMaske.ID_DIENSTLEISTER + 9);
                speichern(formular).fire();

                assertEquals(List.of(vorhanden), datenbank.gespeicherteGruppen());
                assertEquals("Neu", vorhanden.getName());
                assertEquals(1, datenbank.getAllPersonGroups().size());
                assertEquals(String.format(texte.get("msg.groupUpdated"), "Neu"), meldungen.einzige().text());
            });
        }
    }

    /**
     * Die Suche in den Mitgliederlisten.
     *
     * <p>Der heikle Punkt ist nicht das Filtern, sondern was mit einem Haken
     * geschieht, den die Suche gerade ausblendet. Wuerde er verlorengehen,
     * entstuende eine Gruppe, in der jemand fehlt, den man angehakt hat - und
     * das faellt erst bei der Abrechnung auf, wenn ueberhaupt.</p>
     */
    @Nested
    @DisplayName("Mitglieder suchen")
    class MitgliederSuchen {

        @Test
        @DisplayName("Das Suchfeld blendet aus, wer nicht passt")
        void blendetAus() {
            datenbank.mitPatient(patient(1, "Anna")).mitPatient(patient(2, "Bernd"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                suchfeldFuer(formular, GruppenMaske.ID_TEILNEHMER_SUCHE).setText("bernd");

                assertFalse(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 1).isVisible(),
                        "Anna passt nicht zur Suche");
                assertTrue(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 2).isVisible());
            });
        }

        @Test
        @DisplayName("Ausgeblendete Kaestchen belegen keinen Platz mehr")
        void nimmtDenPlatzMit() {
            datenbank.mitPatient(patient(1, "Anna")).mitPatient(patient(2, "Bernd"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                suchfeldFuer(formular, GruppenMaske.ID_TEILNEHMER_SUCHE).setText("bernd");

                assertFalse(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 1).isManaged(),
                        "Ohne setManaged(false) bliebe die Luecke stehen und die Liste"
                                + " waere nach der Suche so lang wie vorher");
            });
        }

        @Test
        @DisplayName("Ein Haken ueberlebt es, wenn die Suche ihn ausblendet")
        void hakenUeberlebtDieSuche() {
            datenbank.mitPatient(patient(1, "Anna")).mitPatient(patient(2, "Bernd"))
                    .mitDienstleister(dienstleister(9, "Max"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                namensfeld(formular).setText("Montagsgruppe");
                TextField suche = suchfeldFuer(formular, GruppenMaske.ID_TEILNEHMER_SUCHE);

                suche.setText("anna");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                suche.setText("bernd");
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 2);
                anhaken(formular, GruppenMaske.ID_DIENSTLEISTER + 9);
                speichern(formular).fire();

                assertEquals(List.of(1, 2), kennungen(einzigeGespeicherte().getPatients()),
                        "Anna wurde angehakt und dann ausgeblendet - sie gehoert trotzdem dazu");
            });
        }

        @Test
        @DisplayName("Der Zaehler nennt Angehakte und Gesamtzahl")
        void zaehlerNenntBeides() {
            datenbank.mitPatient(patient(1, "Anna")).mitPatient(patient(2, "Bernd"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                assertEquals(String.format(texte.get("label.selectedCount"), 0, 2),
                        zaehler(formular, GruppenMaske.ID_TEILNEHMER_ZAEHLER).getText());

                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);

                assertEquals(String.format(texte.get("label.selectedCount"), 1, 2),
                        zaehler(formular, GruppenMaske.ID_TEILNEHMER_ZAEHLER).getText(),
                        "Der Zaehler muss dem Haken folgen, sonst sagt er nichts aus");
            });
        }

        @Test
        @DisplayName("Der Zaehler zaehlt auch mit, was die Suche gerade verbirgt")
        void zaehlerZaehltVerborgeneMit() {
            datenbank.mitPatient(patient(1, "Anna")).mitPatient(patient(2, "Bernd"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                anhaken(formular, GruppenMaske.ID_TEILNEHMER + 1);
                suchfeldFuer(formular, GruppenMaske.ID_TEILNEHMER_SUCHE).setText("bernd");

                assertEquals(String.format(texte.get("label.selectedCount"), 1, 2),
                        zaehler(formular, GruppenMaske.ID_TEILNEHMER_ZAEHLER).getText(),
                        "Genau dafuer steht der Zaehler da: die Auswahl ist groesser als das Sichtbare");
            });
        }

        @Test
        @DisplayName("Findet die Suche nichts, steht das da - statt einer leeren Flaeche")
        void hinweisOhneTreffer() {
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                Label hinweis = zaehler(formular, GruppenMaske.ID_TEILNEHMER_LEER);
                assertFalse(hinweis.isVisible(), "Ohne Suchbegriff gibt es nichts zu melden");

                suchfeldFuer(formular, GruppenMaske.ID_TEILNEHMER_SUCHE).setText("zzz");

                assertTrue(hinweis.isVisible());
                assertEquals(String.format(texte.get("msg.noMatch"), "zzz"), hinweis.getText());
            });
        }

        @Test
        @DisplayName("Die Suche der Teilnehmer laesst die Dienstleister in Ruhe")
        void beideListenSindUnabhaengig() {
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                suchfeldFuer(formular, GruppenMaske.ID_TEILNEHMER_SUCHE).setText("zzz");

                assertFalse(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 1).isVisible());
                assertTrue(kaestchen(formular, GruppenMaske.ID_DIENSTLEISTER + 9).isVisible(),
                        "Ein Suchbegriff darf nur die Liste treffen, ueber der er steht");
            });
        }

        @Test
        @DisplayName("Die Suche unterscheidet nicht zwischen gross und klein")
        void ohneRuecksichtAufSchreibweise() {
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();
                suchfeldFuer(formular, GruppenMaske.ID_TEILNEHMER_SUCHE).setText("ANNA");

                assertTrue(kaestchen(formular, GruppenMaske.ID_TEILNEHMER + 1).isVisible());
            });
        }

        @Test
        @DisplayName("Ohne Personen gibt es kein Suchfeld, sondern den Hinweis")
        void ohnePersonenKeinSuchfeld() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = neuesFormular();

                assertNull(formular.lookup("#" + GruppenMaske.ID_TEILNEHMER_SUCHE),
                        "Ein Suchfeld ueber einer leeren Liste waere nur Zierde");
                assertTrue(zeilentexte(formular).contains(texte.get("msg.noPatients")));
            });
        }
    }

    @Nested
    @DisplayName("Loeschen")
    class Loeschen {

        @Test
        @DisplayName("Ohne Zustimmung bleibt die Gruppe bestehen")
        void ohneZustimmung() {
            datenbank.mitGruppe(gruppe(7, "Montagsgruppe"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().liste(), 7).fire();

                assertEquals(1, meldungen.gestellteRueckfragen().size(), "Es muss nachgefragt werden");
                assertEquals(1, datenbank.getAllPersonGroups().size());
                assertTrue(meldungen.leer(), "Ohne Loeschung gibt es nichts zu melden");
            });
        }

        @Test
        @DisplayName("Nach Zustimmung wird geloescht und das gemeldet")
        void mitZustimmung() {
            datenbank.mitGruppe(gruppe(7, "Montagsgruppe"));
            meldungen.stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().liste(), 7).fire();

                assertTrue(datenbank.getAllPersonGroups().isEmpty());
                assertEquals(String.format(texte.get("msg.groupDeleted"), "Montagsgruppe"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Die Rueckfrage nennt die Gruppe beim Namen")
        void rueckfrageNenntGruppe() {
            datenbank.mitGruppe(gruppe(7, "Montagsgruppe"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().liste(), 7).fire();

                assertTrue(meldungen.gestellteRueckfragen().get(0).contains("Montagsgruppe"),
                        meldungen.gestellteRueckfragen().toString());
            });
        }

        @Test
        @DisplayName("Nach dem Loeschen wird die Liste neu aufgebaut")
        void listeNeuAufgebaut() {
            datenbank.mitGruppe(gruppe(7, "Montagsgruppe"));
            meldungen.stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().liste(), 7).fire();

                assertEquals(1, rahmen.wieOftGeleert(),
                        "Sonst stuende die geloeschte Gruppe noch in der Liste");
            });
        }
    }

    // --- Aufbau und Bedienung -------------------------------------------

    private GruppenMaske maske() {
        return new GruppenMaske(new JavaFxUiFactory(), texte, meldungen, datenbank, rahmen);
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

    private TextField suchfeldFuer(Region formular, String kennung) {
        TextField feld = (TextField) formular.lookup("#" + kennung);
        assertNotNull(feld, "Kein Suchfeld mit der Kennung " + kennung);
        return feld;
    }

    private Label zaehler(Region formular, String kennung) {
        Label beschriftung = (Label) formular.lookup("#" + kennung);
        assertNotNull(beschriftung, "Keine Beschriftung mit der Kennung " + kennung);
        return beschriftung;
    }

    private Button bearbeitenKnopf(Region liste, int gruppenId) {
        return zeilenknopf(liste, gruppenId, GruppenMaske.AKTION_BEARBEITEN);
    }

    private Button loeschenKnopf(Region liste, int gruppenId) {
        Button knopf = zeilenknopf(liste, gruppenId, GruppenMaske.AKTION_LOESCHEN);
        assertNotNull(knopf, "Keine Loeschen-Schaltflaeche fuer Gruppe " + gruppenId);
        return knopf;
    }

    private Button zeilenknopf(Region liste, int gruppenId, String aktion) {
        return (Button) liste.lookup(
                "#" + GruppenMaske.KENNUNG + Listenbau.ZEILE + gruppenId + "-" + aktion);
    }

    private TextField suchfeld(Region liste) {
        TextField feld = (TextField) liste.lookup("#" + GruppenMaske.KENNUNG + Listenbau.SUCHE);
        assertNotNull(feld, "Kein Suchfeld in der Liste");
        return feld;
    }

    /** Alle Beschriftungstexte der Liste, um Zeileninhalte zu pruefen. */
    private List<String> zeilentexte(Region liste) {
        return liste.lookupAll(".label").stream()
                .filter(Label.class::isInstance)
                .map(knoten -> ((Label) knoten).getText())
                .toList();
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

    private static PersonGroup gruppe(int id, String name) {
        PersonGroup gruppe = new PersonGroup(name);
        gruppe.setId(id);
        return gruppe;
    }

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
