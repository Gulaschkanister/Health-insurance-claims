package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.wartung.Wartungskalender;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Der Wartungskalender auf dem Bildschirm.
 *
 * <p>Die Zeile oben muss <b>schweigen</b>, solange nichts ansteht - eine
 * Meldung bei jedem Start wird nach der dritten Woche nicht mehr gelesen, und
 * dann meldet sich auch das Zertifikat vergeblich.</p>
 */
@DisplayName("Wartung")
class WartungsMaskeTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 9, 17);

    private SpeicherRepository datenbank;
    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        texte = new AppMessages("/messages/ui-messages.json");
    }

    private void mitZertifikatBis(String datum) {
        Betriebsdaten betrieb = new Betriebsdaten();
        betrieb.setZertifikatBis(datum);
        datenbank.speichereBetriebsdaten(betrieb);
    }

    private Region maskeAufbauen() {
        return new WartungsMaske(new JavaFxUiFactory(), texte, datenbank, HEUTE).maske();
    }

    private String stand(Region maske) {
        Label zeile = (Label) maske.lookup("#" + WartungsMaske.ID_STAND);
        assertNotNull(zeile, "Keine Standzeile");
        return zeile.getText();
    }

    @Test
    @DisplayName("schweigt, solange nichts ansteht")
    void schweigt() {
        mitZertifikatBis("2027-06-30");

        JavaFxLaufzeit.aufFxFaden(() -> assertEquals(
                String.format(texte.get("maintenance.quiet"), Wartungskalender.VORWARNUNG_WOCHEN),
                stand(maskeAufbauen())));
    }

    /**
     * Bei genau einer Faelligkeit steht die Einzahl da - und wovon die Rede
     * ist.
     *
     * <p>Eine Eins vor einer Mehrzahl ist im Projekt schon dreimal
     * vorgekommen: „1 DTA-Batches erzeugt", „1 Termine insgesamt",
     * „1 Lieferungen warten auf Zahlung".</p>
     */
    @Test
    @DisplayName("nennt bei einer Faelligkeit deren Namen, in der Einzahl")
    void eineFaelligkeit() {
        mitZertifikatBis("2026-10-15");

        JavaFxLaufzeit.aufFxFaden(() -> {
            String stand = stand(maskeAufbauen());

            assertTrue(stand.contains("Zertifikat"), stand);
            assertTrue(stand.startsWith("Eine"), "Einzahl: " + stand);
        });
    }

    @Test
    @DisplayName("zeigt, in wie vielen Tagen ein Termin ansteht")
    void nenntDieTage() {
        mitZertifikatBis("2026-10-15");

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region liste = (Region) maskeAufbauen().lookup("#" + WartungsMaske.ID_LISTE);

            assertNotNull(liste);
            assertTrue(alleTexte(liste).contains("in 28 Tagen"), alleTexte(liste));
        });
    }

    /**
     * Was kein Datum hat, steht mit dem Grund da.
     *
     * <p>Ein Kalender, der nur zeigt, was er weiss, sieht vollstaendig aus und
     * ist es nicht.</p>
     */
    @Test
    @DisplayName("fuehrt auch auf, wofuer kein Datum bekannt ist")
    void auchOhneDatum() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            String texteDerListe =
                    alleTexte((Region) maskeAufbauen().lookup("#" + WartungsMaske.ID_LISTE));

            assertTrue(texteDerListe.contains("Kostentraegerdatei"), texteDerListe);
            assertTrue(texteDerListe.contains(texte.get("maintenance.noDate")), texteDerListe);
        });
    }

    private static String alleTexte(Region bereich) {
        StringBuilder text = new StringBuilder();
        sammle(bereich, text);
        return text.toString();
    }

    private static void sammle(javafx.scene.Node knoten, StringBuilder text) {
        if (knoten instanceof Label label) {
            text.append(label.getText()).append(System.lineSeparator());
        }
        if (knoten instanceof javafx.scene.Parent eltern) {
            eltern.getChildrenUnmodifiable().forEach(kind -> sammle(kind, text));
        }
    }
}
