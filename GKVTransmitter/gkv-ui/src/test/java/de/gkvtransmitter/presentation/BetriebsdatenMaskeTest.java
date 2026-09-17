package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

/**
 * Die Maske fuer die Angaben zum eigenen Betrieb.
 *
 * <p>Zwei der Angaben gehen in jede erzeugte Datei - das eigene IK und die
 * Rolle. Deshalb wird das IK hier gegen die Pruefziffer gehalten und nicht
 * erst beim Versand: dort naennte die Meldung ein Feld im UNB statt die
 * fehlende Stammangabe.</p>
 */
class BetriebsdatenMaskeTest {

    private SpeicherRepository datenbank;
    private AufzeichnendeMeldungen meldungen;
    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        meldungen = new AufzeichnendeMeldungen();
        texte = new AppMessages("/messages/ui-messages.json");
    }

    private Region maskeAufbauen() {
        return new BetriebsdatenMaske(new JavaFxUiFactory(), texte, meldungen, datenbank).maske();
    }

    private static void setze(Region maske, String kennung, String wert) {
        ((TextField) maske.lookup("#" + kennung)).setText(wert);
    }

    private static String textVon(Region maske, String kennung) {
        return ((TextField) maske.lookup("#" + kennung)).getText();
    }

    private static void speichern(Region maske) {
        ((Button) maske.lookup("#" + BetriebsdatenMaske.ID_SPEICHERN)).fire();
    }

    @Nested
    @DisplayName("Erfassen")
    class Erfassen {

        @Test
        @DisplayName("Gueltige Angaben werden gespeichert")
        void speichertAngaben() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                setze(maske, BetriebsdatenMaske.ID_NAME, "Hebammenpraxis Muster");
                setze(maske, BetriebsdatenMaske.ID_IK, "108310400");
                setze(maske, BetriebsdatenMaske.ID_ORT, "Bremen");
                setze(maske, BetriebsdatenMaske.ID_ANSPRECHPARTNER, "Maria Muster");

                speichern(maske);

                Betriebsdaten gespeichert = datenbank.ladeBetriebsdaten();
                assertNotNull(gespeichert, "Die Betriebsdaten muessen angelegt worden sein");
                assertEquals("Hebammenpraxis Muster", gespeichert.getPraxisname());
                assertEquals("108310400", gespeichert.getIk());
                assertEquals("Bremen", gespeichert.getOrt());
                assertEquals(AufzeichnendeMeldungen.Art.ERFOLG, meldungen.einzige().art());
            });
        }

        /**
         * Ein falsches IK faellt hier auf und nicht erst beim Versand.
         *
         * <p>{@code 108310401} ist {@code 108310400} mit geaenderter letzter
         * Stelle - rechnerisch also genau um die Pruefziffer daneben.</p>
         */
        @Test
        @DisplayName("Ein IK mit falscher Pruefziffer wird abgewiesen")
        void weistFalschesIkAb() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                setze(maske, BetriebsdatenMaske.ID_NAME, "Hebammenpraxis Muster");
                setze(maske, BetriebsdatenMaske.ID_IK, "108310401");

                speichern(maske);

                assertNull(datenbank.ladeBetriebsdaten(),
                        "Nichts darf gespeichert werden, solange das IK nicht stimmt");
                assertEquals(AufzeichnendeMeldungen.Art.FEHLER, meldungen.einzige().art());
            });
        }

        /**
         * Ohne IK laesst sich trotzdem speichern.
         *
         * <p>Wer die Maske oeffnet, um den Ansprechpartner nachzutragen, soll
         * nicht am IK haengenbleiben - das fehlende IK meldet der
         * Versanddienst als Hinweis, und zwar dann, wenn es darauf ankommt.</p>
         */
        @Test
        @DisplayName("Ohne IK laesst sich speichern")
        void speichertAuchOhneIk() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                setze(maske, BetriebsdatenMaske.ID_ANSPRECHPARTNER, "Maria Muster");

                speichern(maske);

                assertNotNull(datenbank.ladeBetriebsdaten());
                assertEquals("Maria Muster", datenbank.ladeBetriebsdaten().getAnsprechpartner());
            });
        }

        @Test
        @DisplayName("Die Rolle bestimmt die neunte Stelle des Dateinamens")
        void merktSichDieRolle() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                setze(maske, BetriebsdatenMaske.ID_IK, "108310400");
                @SuppressWarnings("unchecked")
                ComboBox<String> rolle =
                        (ComboBox<String>) maske.lookup("#" + BetriebsdatenMaske.ID_ROLLE);
                rolle.getSelectionModel().select(1);

                speichern(maske);

                assertFalse(datenbank.ladeBetriebsdaten().istSelbstabrechner(),
                        "Bei einer Abrechnungsstelle gehoert ein A in den Dateinamen");
            });
        }
    }

    @Nested
    @DisplayName("Wiedersehen")
    class Wiedersehen {

        @Test
        @DisplayName("Erfasste Angaben stehen beim naechsten Oeffnen wieder da")
        void zeigtVorhandenes() {
            Betriebsdaten vorhanden = new Betriebsdaten();
            vorhanden.setPraxisname("Praxis am Deich");
            vorhanden.setIk("104940005");
            vorhanden.setSelbstabrechner(false);
            datenbank.mitBetriebsdaten(vorhanden);

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();

                assertEquals("Praxis am Deich", textVon(maske, BetriebsdatenMaske.ID_NAME));
                assertEquals("104940005", textVon(maske, BetriebsdatenMaske.ID_IK));
                @SuppressWarnings("unchecked")
                ComboBox<String> rolle =
                        (ComboBox<String>) maske.lookup("#" + BetriebsdatenMaske.ID_ROLLE);
                assertEquals(1, rolle.getSelectionModel().getSelectedIndex());
            });
        }

        @Test
        @DisplayName("Jedes gefuehrte Feld ist auch in der Maske zu finden")
        void alleFelderVorhanden() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region maske = maskeAufbauen();
                for (String kennung : BetriebsdatenMaske.felder()) {
                    assertNotNull(maske.lookup("#" + kennung), "Feld fehlt in der Maske: " + kennung);
                }
                assertTrue(maske.lookup("#" + BetriebsdatenMaske.ID_SPEICHERN) instanceof Button);
            });
        }
    }
}
