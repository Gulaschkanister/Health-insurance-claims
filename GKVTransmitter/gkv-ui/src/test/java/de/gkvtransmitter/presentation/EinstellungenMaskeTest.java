package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.einstellung.Einstellung;
import de.gkvtransmitter.einstellung.Einstellungen;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Prueft die Einstellungsseite.
 *
 * <p>Sie ist die erste Maske, die etwas ausserhalb der Fachdaten aendert. Zwei
 * Dinge muessen deshalb stimmen: <b>die Wahl wirkt sofort</b> - eine
 * Einstellung, die erst nach einem Neustart greift, sieht aus wie ein kaputter
 * Schalter -, und <b>sie ueberlebt den Neustart</b>, sonst waere die ganze
 * Seite eine Vortaeuschung.</p>
 */
@DisplayName("Einstellungen (Maske)")
class EinstellungenMaskeTest {

    @TempDir
    private Path ordner;

    private AppMessages texte;
    private AufzeichnendeMeldungen meldungen;
    private Einstellungen einstellungen;
    private List<String> angewandt;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        texte = new AppMessages("/messages/ui-messages.json");
        meldungen = new AufzeichnendeMeldungen();
        einstellungen = Einstellungen.laden(ordner.resolve(Einstellungen.DATEINAME));
        angewandt = new ArrayList<>();
    }

    @Test
    @DisplayName("Zeigt beim Oeffnen, was gerade gilt")
    void zeigtDenStand() {
        einstellungen.setze(Einstellung.DARSTELLUNG, EinstellungenMaske.DUNKEL);

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maske();

            assertEquals(texte.get("settings.appearance.dark"),
                    auswahl(maske, EinstellungenMaske.ID_DARSTELLUNG).getValue(),
                    "Eine Seite, die den eigenen Stand nicht zeigt, laedt zum Doppelklick ein");
        });
    }

    @Test
    @DisplayName("Die Umschaltung wirkt sofort")
    void wirktSofort() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            ComboBox<String> auswahl = auswahl(maske(), EinstellungenMaske.ID_DARSTELLUNG);

            auswahl.getSelectionModel().select(texte.get("settings.appearance.dark"));

            assertEquals(List.of(EinstellungenMaske.DUNKEL), angewandt,
                    "Eine Einstellung, die erst beim Neustart greift, sieht aus wie ein Fehler");
        });
    }

    @Test
    @DisplayName("Die Wahl steht danach in der Datei")
    void ueberlebtDenNeustart() {
        JavaFxLaufzeit.aufFxFaden(() ->
                auswahl(maske(), EinstellungenMaske.ID_DARSTELLUNG)
                        .getSelectionModel().select(texte.get("settings.appearance.dark")));

        assertEquals(EinstellungenMaske.DUNKEL,
                Einstellungen.laden(ordner.resolve(Einstellungen.DATEINAME)).get(Einstellung.DARSTELLUNG));
    }

    @Test
    @DisplayName("Zurueck auf hell geht ebenso")
    void wiederZurueck() {
        einstellungen.setze(Einstellung.DARSTELLUNG, EinstellungenMaske.DUNKEL);

        JavaFxLaufzeit.aufFxFaden(() ->
                auswahl(maske(), EinstellungenMaske.ID_DARSTELLUNG)
                        .getSelectionModel().select(texte.get("settings.appearance.light")));

        assertEquals(EinstellungenMaske.HELL,
                Einstellungen.laden(ordner.resolve(Einstellungen.DATEINAME)).get(Einstellung.DARSTELLUNG));
        assertEquals(List.of(EinstellungenMaske.HELL), angewandt);
    }

    @Test
    @DisplayName("Die Maske nennt den Ablageort der Datei")
    void nenntDenOrt() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Label ort = (Label) maske().lookup("#" + EinstellungenMaske.ID_ORT);

            assertNotNull(ort, "Wer sichern oder mitnehmen will, muss wissen, wo die Datei liegt");
            assertTrue(ort.getText().endsWith(Einstellungen.DATEINAME), ort.getText());
        });
    }

    /**
     * Die Angabe, die ueber Bezahlung entscheidet.
     *
     * <p>Sie stand bis zum 07.09.2026 fest im Quelltext: jede Datei bezeichnete
     * sich als Erprobungsdatei, und niemand konnte das sehen. Eine Angabe
     * dieser Tragweite gehoert dorthin, wo man sie liest.</p>
     */
    @Nested
    @DisplayName("Uebermittlungsart")
    class Uebermittlung {

        @Test
        @DisplayName("Vorgabe ist die Erprobung, nicht der Echtbetrieb")
        void vorgabeErprobung() {
            JavaFxLaufzeit.aufFxFaden(() ->
                    assertEquals(texte.get("settings.transfer.trial"),
                            auswahl(maske(), EinstellungenMaske.ID_UEBERMITTLUNG).getValue()));
        }

        @Test
        @DisplayName("Die Wahl steht danach in der Datei")
        void wirdGespeichert() {
            JavaFxLaufzeit.aufFxFaden(() ->
                    auswahl(maske(), EinstellungenMaske.ID_UEBERMITTLUNG)
                            .getSelectionModel().select(texte.get("settings.transfer.live")));

            assertEquals("echt", Einstellungen.laden(ordner.resolve(Einstellungen.DATEINAME))
                    .get(Einstellung.UEBERMITTLUNGSART));
        }

        @Test
        @DisplayName("Der Wechsel in den Echtbetrieb wird angesagt")
        void echtbetriebWirdAngesagt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                auswahl(maske(), EinstellungenMaske.ID_UEBERMITTLUNG)
                        .getSelectionModel().select(texte.get("settings.transfer.live"));

                assertEquals(texte.get("settings.transfer.liveWarning"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Der Wechsel auf Erprobung sagt nichts an")
        void erprobungOhneMeldung() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                auswahl(maske(), EinstellungenMaske.ID_UEBERMITTLUNG)
                        .getSelectionModel().select(texte.get("settings.transfer.test"));

                assertTrue(meldungen.leer(), "Nur der Echtbetrieb ist eine Ansage wert");
            });
        }
    }

    /**
     * Wenn sich nichts speichern laesst.
     *
     * <p>Nachgestellt ueber einen Elternpfad, der eine Datei ist. Die Wahl muss
     * trotzdem wirken - sonst laesst sich die Anwendung nicht mehr bedienen,
     * weil ein Ordner klemmt - und die Meldungsecke muss sagen, dass sie die
     * Sitzung nicht ueberlebt.</p>
     */
    @Test
    @DisplayName("Laesst sich nichts speichern, wirkt die Wahl trotzdem und wird gemeldet")
    void nichtSpeicherbar() throws IOException {
        Path keinOrdner = ordner.resolve("eine-datei");
        Files.writeString(keinOrdner, "kein Ordner");
        einstellungen = Einstellungen.laden(keinOrdner.resolve(Einstellungen.DATEINAME));

        JavaFxLaufzeit.aufFxFaden(() -> {
            auswahl(maske(), EinstellungenMaske.ID_DARSTELLUNG)
                    .getSelectionModel().select(texte.get("settings.appearance.dark"));

            assertEquals(List.of(EinstellungenMaske.DUNKEL), angewandt,
                    "Die Wirkung darf nicht davon abhaengen, ob eine Datei beschreibbar ist");
            assertEquals(texte.get("settings.notSaved"), meldungen.einzige().text());
            assertFalse(meldungen.leer());
        });
    }

    private Region maske() {
        return new EinstellungenMaske(new JavaFxUiFactory(), texte, meldungen, einstellungen,
                angewandt::add).maske();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> auswahl(Region maske, String kennung) {
        ComboBox<String> feld = (ComboBox<String>) maske.lookup("#" + kennung);
        assertNotNull(feld, "Kein Auswahlfeld " + kennung);
        return feld;
    }
}
