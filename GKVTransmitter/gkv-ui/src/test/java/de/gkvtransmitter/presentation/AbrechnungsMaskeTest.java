package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import de.gkvtransmitter.presentation.meldung.Pruefbefunde;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.validator.ValidationReport;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
    private AufzeichnendeMeldungen meldungen;
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
        meldungen = new AufzeichnendeMeldungen();
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

                assertEquals(texte.get("msg.noBlueprints"), meldungen.einzige().text());
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

                assertEquals(texte.get("msg.selectGroupRequired"), meldungen.einzige().text());
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

                assertEquals(texte.get("msg.noParticipantsSelected"), meldungen.einzige().text());
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
                assertTrue(meldungen.leer(), "Der Hinweis gehoert in die Maske, nicht in einen Dialog");
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

                assertEquals(AufzeichnendeMeldungen.Art.PRUEFBERICHT, meldungen.einzige().art());
                assertSame(bericht, meldungen.letzterPruefbericht(),
                        "Der Bericht muss vollstaendig weitergereicht werden, nicht als Text");
                assertEquals(List.of(
                                "Die Zahl der Nachrichten stimmt nicht.",
                                "Das IK der Kasse ist ungueltig.",
                                "Der Rechnungsbetrag ist auffaellig hoch."),
                        Pruefbefunde.zeilen(meldungen.letzterPruefbericht()).stream()
                                .map(Pruefbefunde.Zeile::text).toList(),
                        "Erst die Fehler, dann die Hinweise");
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

                AufzeichnendeMeldungen.Meldung meldung = meldungen.einzige();
                assertEquals(AufzeichnendeMeldungen.Art.FEHLER, meldung.art());
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

                AufzeichnendeMeldungen.Meldung meldung = meldungen.einzige();
                assertEquals(AufzeichnendeMeldungen.Art.ERFOLG, meldung.art());
                assertTrue(meldung.text().contains(String.valueOf(KASSEN_IK)), meldung.text());
                assertTrue(meldung.text().contains(datei.toString()), meldung.text());
            });
        }
    }

    @Nested
    @DisplayName("Monat fuer Monat derselbe Kurs")
    class Monatsbetrieb {

        @Test
        @DisplayName("Alle auswaehlen hakt jeden Teilnehmer an")
        void alleAuswaehlen() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Kurs", patient(1, "Anna"), patient(2, "Bernd"), patient(3, "Clara")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);

                knopf(maske, AbrechnungsMaske.ID_ALLE).fire();
                start(maske).fire();

                assertEquals(List.of(1, 2, 3),
                        laeufe.get(0).teilnehmer().stream().map(Patient::getId).toList());
            });
        }

        @Test
        @DisplayName("Keinen nimmt alle Haken wieder weg")
        void keinenAuswaehlen() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Kurs", patient(1, "Anna"), patient(2, "Bernd")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                knopf(maske, AbrechnungsMaske.ID_ALLE).fire();

                knopf(maske, AbrechnungsMaske.ID_KEINEN).fire();
                start(maske).fire();

                assertTrue(laeufe.isEmpty(), "Ohne Haken darf nichts abgerechnet werden");
                assertEquals(texte.get("msg.noParticipantsSelected"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Ist niemand angehakt, gilt die Terminzahl fuer alle")
        void termineFuerAlleOhneAuswahl() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Kurs", patient(1, "Anna"), patient(2, "Bernd")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);

                termineFuerAlleSetzen(maske, 4);
                knopf(maske, AbrechnungsMaske.ID_ALLE).fire();
                start(maske).fire();

                assertEquals(Map.of(1, 4, 2, 4), laeufe.get(0).termine());
            });
        }

        @Test
        @DisplayName("Sind Teilnehmer angehakt, gilt die Terminzahl nur fuer diese")
        void termineNurFuerAngehakte() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Kurs", patient(1, "Anna"), patient(2, "Bernd")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                teilnehmerAnhaken(maske, 2);

                termineFuerAlleSetzen(maske, 6);
                start(maske).fire();

                assertEquals(Map.of(2, 6), laeufe.get(0).termine());
                assertEquals(1, terminZaehler(maske, 1).getValue(),
                        "Der nicht angehakte Teilnehmer bleibt unberuehrt");
            });
        }

        @Test
        @DisplayName("Die Zusammenfassung nennt Auswahl und Summe der Termine")
        void zeigtZusammenfassung() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Kurs", patient(1, "Anna"), patient(2, "Bernd"), patient(3, "Clara")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);

                assertEquals(String.format(texte.get("msg.selectionSummary"), 0, 3, 0),
                        zusammenfassung(maske));

                teilnehmerAnhaken(maske, 1);
                termineSetzen(maske, 1, 4);
                teilnehmerAnhaken(maske, 2);
                termineSetzen(maske, 2, 3);

                assertEquals(String.format(texte.get("msg.selectionSummary"), 2, 3, 7),
                        zusammenfassung(maske),
                        "Man soll vor dem Absenden sehen, was abgerechnet wird");
            });
        }

        @Test
        @DisplayName("Der Dienstleister wird genannt, aber nicht zur Auswahl gestellt")
        void dienstleisterOhneAuswahl() {
            datenbank.mitBlaupause(blaupause()).mitGruppe(gruppe("Kurs", patient(1, "Anna")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);

                Label zeile = (Label) maske.lookup("#" + AbrechnungsMaske.ID_DIENSTLEISTER);
                assertNotNull(zeile, "Wer die Leistung erbracht hat, gehoert in die Maske");
                assertTrue(zeile.getText().contains("Max Muster"), zeile.getText());
                assertNull(maske.lookup("#" + AbrechnungsMaske.ID_DIENSTLEISTER + "-9"),
                        "Ein Kaestchen ohne Wirkung waere irrefuehrend");
            });
        }

        @Test
        @DisplayName("Ein Gruppenwechsel setzt Auswahl und Zusammenfassung zurueck")
        void gruppenwechselSetztZurueck() {
            datenbank.mitBlaupause(blaupause())
                    .mitGruppe(gruppe("Erste", patient(1, "Anna"), patient(2, "Bernd")))
                    .mitGruppe(gruppe("Zweite", patient(3, "Clara")));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                gruppeWaehlen(maske, 0);
                knopf(maske, AbrechnungsMaske.ID_ALLE).fire();

                gruppeWaehlen(maske, 1);

                assertEquals(String.format(texte.get("msg.selectionSummary"), 0, 1, 0),
                        zusammenfassung(maske));
            });
        }
    }

    // --- Aufbau und Bedienung -------------------------------------------

    private Region maskeAufbauen() {
        AbrechnungsMaske maske = new AbrechnungsMaske(new JavaFxUiFactory(), texte, meldungen, datenbank,
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

    private Button knopf(Region maske, String kennung) {
        Button schaltflaeche = (Button) maske.lookup("#" + kennung);
        assertNotNull(schaltflaeche, "Keine Schaltflaeche mit der Kennung " + kennung);
        return schaltflaeche;
    }

    /** Traegt die Terminzahl in die Werkzeugleiste ein und uebernimmt sie. */
    @SuppressWarnings("unchecked")
    private void termineFuerAlleSetzen(Region maske, int anzahl) {
        Spinner<Integer> fuerAlle =
                (Spinner<Integer>) maske.lookup("#" + AbrechnungsMaske.ID_TERMINE_ALLE);
        assertNotNull(fuerAlle, "Kein Zaehler fuer die Terminzahl aller Teilnehmer");
        fuerAlle.getValueFactory().setValue(anzahl);
        knopf(maske, AbrechnungsMaske.ID_TERMINE_SETZEN).fire();
    }

    @SuppressWarnings("unchecked")
    private Spinner<Integer> terminZaehler(Region maske, int personId) {
        return (Spinner<Integer>) maske.lookup("#" + AbrechnungsMaske.ID_TERMINE + personId);
    }

    private String zusammenfassung(Region maske) {
        Label zeile = (Label) maske.lookup("#" + AbrechnungsMaske.ID_ZUSAMMENFASSUNG);
        assertNotNull(zeile, "Keine Zusammenfassung unter der Liste");
        return zeile.getText();
    }
}
