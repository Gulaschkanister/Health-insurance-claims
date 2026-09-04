package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.dispatch.DispatchBatch;
import de.gkvtransmitter.dispatch.DtaValidierungsException;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.validator.ValidationReport;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.Region;

/**
 * Prueft die Abrechnungsmaske - die erste Oberflaechenpruefung des Projekts.
 *
 * <p>Der Blickwinkel ist der der Anwenderin: Die Maske wird aufgebaut,
 * Bedienelemente werden gesetzt, die Schaltflaeche wird ausgeloest, und
 * geprueft wird, was gemeldet und was an den Fachdienst uebergeben wurde.
 * Kein Test greift auf Interna der Maske zu.</p>
 */
@DisplayName("Abrechnungsmaske")
class AbrechnungsMaskeTest {

    /** Gueltige Pruefziffer, sonst weist die Validierung zu Recht ab. */
    private static final int KASSEN_IK = 108310400;

    @TempDir
    Path zielordner;

    private SpeicherRepository datenbank;
    private AufzeichnendeDialoge dialoge;
    private AppMessages texte;
    private final List<Lauf> laeufe = new ArrayList<>();

    /** Was der Fachdienst auf einen Lauf hin tut. Standard: nichts zu melden. */
    private Function<Lauf, List<DispatchBatch>> antwort = lauf -> List.of();

    /** Ein aufgezeichneter Aufruf des Fachdienstes. */
    private record Lauf(List<Patient> teilnehmer, PersonGroup gruppe, Blueprint blaupause,
            Map<Integer, Integer> termine, Path ordner) {
    }

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        dialoge = new AufzeichnendeDialoge();
        texte = new AppMessages("/messages/ui-messages.json");
        laeufe.clear();
        antwort = lauf -> List.of();
    }

    @Nested
    @DisplayName("Voraussetzungen vor dem Versand")
    class Voraussetzungen {

        @Test
        @DisplayName("Ohne Blaupause meldet die Maske das und rechnet nicht ab")
        void ohneBlaupause() {
            datenbank.mitGruppe(gruppe("Gruppe", patient(1, "Anna")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                teilnehmerAnhaken(maske, 1);
                start(maske).fire();

                assertEquals(texte.get("msg.noBlueprints"), dialoge.einzige().text());
                assertTrue(laeufe.isEmpty(), "Es darf nichts abgerechnet worden sein");
            });
        }

        @Test
        @DisplayName("Ohne gewaehlte Gruppe fordert die Maske zur Auswahl auf")
        void ohneGruppe() {
            datenbank.mitBlaupause(blaupause()).mitGruppe(gruppe("Gruppe", patient(1, "Anna")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                start(maske).fire();

                assertEquals(texte.get("msg.selectGroupRequired"), dialoge.einzige().text());
                assertTrue(laeufe.isEmpty(), "Es darf nichts abgerechnet worden sein");
            });
        }

        @Test
        @DisplayName("Ohne angehakten Teilnehmer wird nichts versendet")
        void ohneTeilnehmer() {
            datenbank.mitBlaupause(blaupause()).mitGruppe(gruppe("Gruppe", patient(1, "Anna")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                start(maske).fire();

                assertEquals(texte.get("msg.noParticipantsSelected"), dialoge.einzige().text());
                assertTrue(laeufe.isEmpty(), "Es darf nichts abgerechnet worden sein");
            });
        }

        @Test
        @DisplayName("Eine Gruppe ohne Mitglieder zeigt einen Hinweis statt einer leeren Tabelle")
        void leereGruppe() {
            PersonGroup leer = new PersonGroup("Leere Gruppe");
            leer.setServiceProviders(new LinkedHashSet<>());
            datenbank.mitBlaupause(blaupause()).mitGruppe(leer);
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);

                assertNull(maske.lookup("#" + AbrechnungsMaske.ID_TEILNEHMER + "1"),
                        "Ohne Mitglieder darf kein Auswahlkaestchen entstehen");
                assertTrue(dialoge.leer(), "Der Hinweis gehoert in die Maske, nicht in einen Dialog");
            });
        }
    }

    @Nested
    @DisplayName("Zusammenstellen der Teilnehmer")
    class Zusammenstellen {

        @Test
        @DisplayName("Ein Wechsel der Gruppe baut die Teilnehmerliste neu auf")
        void gruppenwechsel() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Erste", patient(1, "Anna")))
                    .mitGruppe(gruppe("Zweite", patient(2, "Bernd")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();

                gruppeWaehlen(maske, 0);
                assertNotNull(maske.lookup("#" + AbrechnungsMaske.ID_TEILNEHMER + "1"));
                assertNull(maske.lookup("#" + AbrechnungsMaske.ID_TEILNEHMER + "2"));

                gruppeWaehlen(maske, 1);
                assertNull(maske.lookup("#" + AbrechnungsMaske.ID_TEILNEHMER + "1"),
                        "Der Teilnehmer der vorigen Gruppe muss verschwinden");
                assertNotNull(maske.lookup("#" + AbrechnungsMaske.ID_TEILNEHMER + "2"));
            });
        }

        @Test
        @DisplayName("Nur angehakte Teilnehmer werden abgerechnet, mit der eingestellten Terminzahl")
        void nurAngehakte() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Gruppe", patient(1, "Anna"), patient(2, "Bernd"), patient(3, "Clara")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                teilnehmerAnhaken(maske, 1);
                teilnehmerAnhaken(maske, 3);
                termineSetzen(maske, 1, 4);
                termineSetzen(maske, 3, 7);
                start(maske).fire();

                assertEquals(1, laeufe.size());
                Lauf lauf = laeufe.get(0);
                assertEquals(List.of(1, 3), lauf.teilnehmer().stream().map(Patient::getId).toList());
                assertEquals(Map.of(1, 4, 3, 7), lauf.termine());
                assertEquals(zielordner, lauf.ordner());
            });
        }

        @Test
        @DisplayName("Ein Teilnehmer ohne Anhaken taucht auch nicht in den Terminen auf")
        void nichtAngehakteOhneTermine() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Gruppe", patient(1, "Anna"), patient(2, "Bernd")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                teilnehmerAnhaken(maske, 2);
                start(maske).fire();

                assertEquals(Map.of(2, 1), laeufe.get(0).termine(),
                        "Voreingestellt ist ein Termin je Teilnehmer");
            });
        }
    }

    @Nested
    @DisplayName("Rueckmeldung des Fachdienstes")
    class Rueckmeldung {

        @Test
        @DisplayName("Eine nicht bestandene Pruefung zeigt alle Beanstandungen und versendet nichts")
        void pruefungNichtBestanden() {
            ValidationReport bericht = ValidationReport.builder()
                    .error("UNZ_ANZAHL", "UNZ (Zeile 18)", "Die Zahl der Nachrichten stimmt nicht.")
                    .error("IK_PRUEFZIFFER", "NAD", "Das IK der Kasse ist ungueltig.")
                    .warning("BETRAG", "", "Der Rechnungsbetrag ist auffaellig hoch.")
                    .build();
            antwort = lauf -> {
                throw new DtaValidierungsException(bericht);
            };
            datenbank.mitBlaupause(blaupause()).mitGruppe(gruppe("Gruppe", patient(1, "Anna")));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                teilnehmerAnhaken(maske, 1);
                start(maske).fire();

                AufzeichnendeDialoge.Meldung meldung = dialoge.einzige();
                assertEquals(AufzeichnendeDialoge.Art.FEHLER, meldung.art());
                assertTrue(meldung.text().contains("Die Zahl der Nachrichten stimmt nicht."),
                        "Der erste Fehler fehlt: " + meldung.text());
                assertTrue(meldung.text().contains("Das IK der Kasse ist ungueltig."),
                        "Auch der zweite Fehler muss zu sehen sein: " + meldung.text());
                assertTrue(meldung.text().contains("Der Rechnungsbetrag ist auffaellig hoch."),
                        "Die Warnung fehlt: " + meldung.text());
            });
        }

        @Test
        @DisplayName("Ein Fehlschlag beim Versand wird als Fehler gemeldet")
        void versandFehlgeschlagen() {
            antwort = lauf -> {
                throw new IllegalStateException("Zielordner nicht beschreibbar");
            };
            datenbank.mitBlaupause(blaupause()).mitGruppe(gruppe("Gruppe", patient(1, "Anna")));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                teilnehmerAnhaken(maske, 1);
                start(maske).fire();

                AufzeichnendeDialoge.Meldung meldung = dialoge.einzige();
                assertEquals(AufzeichnendeDialoge.Art.FEHLER, meldung.art());
                assertTrue(meldung.text().contains("Zielordner nicht beschreibbar"), meldung.text());
            });
        }

        @Test
        @DisplayName("Nach einem gelungenen Lauf nennt die Meldung Kasse und erzeugte Dateien")
        void gelungenerLauf() {
            Path datei = zielordner.resolve("108310400_1.DTA");
            antwort = lauf -> List.of(new DispatchBatch(KASSEN_IK, List.of(datei)));
            datenbank.mitBlaupause(blaupause()).mitGruppe(gruppe("Gruppe", patient(1, "Anna")));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                teilnehmerAnhaken(maske, 1);
                start(maske).fire();

                AufzeichnendeDialoge.Meldung meldung = dialoge.einzige();
                assertEquals(AufzeichnendeDialoge.Art.INFO, meldung.art());
                assertTrue(meldung.text().contains(String.valueOf(KASSEN_IK)), meldung.text());
                assertTrue(meldung.text().contains(datei.toString()), meldung.text());
            });
        }
    }

    // --- Aufbau und Bedienung -------------------------------------------

    private Region maskeAufbauen() {
        AbrechnungsMaske maske = new AbrechnungsMaske(new JavaFxUiFactory(), texte, dialoge, datenbank,
                (teilnehmer, gruppe, blaupause, termine, ordner) -> {
                    Lauf lauf = new Lauf(List.copyOf(teilnehmer), gruppe, blaupause,
                            Map.copyOf(termine), ordner);
                    laeufe.add(lauf);
                    return antwort.apply(lauf);
                },
                () -> zielordner);
        return maske.erzeuge();
    }

    @SuppressWarnings("unchecked")
    private void gruppeWaehlen(Region maske, int stelle) {
        ComboBox<PersonGroup> auswahl =
                (ComboBox<PersonGroup>) maske.lookup("#" + AbrechnungsMaske.ID_GRUPPE);
        auswahl.setValue(auswahl.getItems().get(stelle));
    }

    private void teilnehmerAnhaken(Region maske, int personId) {
        CheckBox kaestchen = (CheckBox) maske.lookup("#" + AbrechnungsMaske.ID_TEILNEHMER + personId);
        assertNotNull(kaestchen, "Kein Auswahlkaestchen fuer Teilnehmer " + personId);
        kaestchen.setSelected(true);
    }

    @SuppressWarnings("unchecked")
    private void termineSetzen(Region maske, int personId, int anzahl) {
        Spinner<Integer> zaehler =
                (Spinner<Integer>) maske.lookup("#" + AbrechnungsMaske.ID_TERMINE + personId);
        assertNotNull(zaehler, "Kein Terminzaehler fuer Teilnehmer " + personId);
        zaehler.getValueFactory().setValue(anzahl);
    }

    private Button start(Region maske) {
        return (Button) maske.lookup("#" + AbrechnungsMaske.ID_START);
    }

    // --- Testdaten -------------------------------------------------------

    private static Patient patient(int id, String vorname) {
        Patient person = new Patient(vorname, "Muster", "Musterweg", "DE", "1", 12345, 101, KASSEN_IK, null);
        person.setId(id);
        return person;
    }

    private static PersonGroup gruppe(String name, Patient... teilnehmer) {
        PersonGroup gruppe = new PersonGroup(name);
        gruppe.setPatients(new LinkedHashSet<>(List.of(teilnehmer)));
        ServiceProvider dienstleister =
                new ServiceProvider("Max", "Muster", "Musterstr.", "DE", "1", 12345, 1001, KASSEN_IK, null);
        dienstleister.setId(99);
        gruppe.setServiceProviders(new LinkedHashSet<>(List.of(dienstleister)));
        return gruppe;
    }

    private static Blueprint blaupause() {
        return new Blueprint("Testblaupause", "test-template", "{\"fields\":{}}", OffsetDateTime.now());
    }
}
