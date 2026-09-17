package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

/**
 * Das Registrierungsblatt auf dem Bildschirm.
 *
 * <p>Der Punkt, an dem sich die Betriebsdaten zum ersten Mal auszahlen: Was
 * dort und im Dienstleisterprofil einmal erfasst wurde, steht hier
 * zusammengestellt statt in vier Masken verteilt.</p>
 */
@DisplayName("Registrierungsblatt")
class RegistrierungsblattMaskeTest {

    @TempDir
    Path ablage;

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
        return new RegistrierungsblattMaske(new JavaFxUiFactory(), texte, meldungen, datenbank,
                () -> ablage).maske();
    }

    private void mitVollstaendigenDaten() {
        Betriebsdaten betrieb = new Betriebsdaten();
        betrieb.setPraxisname("Hebammenpraxis Bremen");
        betrieb.setIk("261914007");
        betrieb.setSteuernummer("60/123/45678");
        betrieb.setAnsprechpartner("Maria Hebamme");
        betrieb.setTelefon("0421 123456");
        betrieb.setEmail("praxis@example.de");
        betrieb.setZertifikatBis("2027-03-31");
        datenbank.speichereBetriebsdaten(betrieb);

        ServiceProvider person = new ServiceProvider("Maria", "Hebamme", "Kurweg", "DE", "3",
                28203, 261914007, 101560000, LocalDate.of(1980, 1, 9));
        person.setId(1);
        person.setAbrechnungscode("50");
        person.setSteuernummer("60/123/45678");
        datenbank.saveServiceProvider(person);
    }

    @Test
    @DisplayName("zeigt die erfassten Angaben")
    void zeigtDieAngaben() {
        mitVollstaendigenDaten();

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            String blatt = ((TextArea) maske.lookup("#" + RegistrierungsblattMaske.ID_BLATT)).getText();
            assertTrue(blatt.contains("Hebammenpraxis Bremen"), blatt);
            assertTrue(blatt.contains("50 (Leistungsbereich F)"), blatt);
        });
    }

    /**
     * Die Zeile mit den Luecken steht <b>ueber</b> dem Blatt.
     *
     * <p>Der Abschnitt "4 Was noch fehlt" ist die letzte Zeile eines langen
     * Textes. Wer das Blatt ablegt, ohne zu blaettern, sieht ihn nie.</p>
     */
    @Test
    @DisplayName("nennt die Zahl der fehlenden Angaben ueber dem Blatt")
    void nenntFehlendeAngaben() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            String stand = ((Label) maske.lookup("#" + RegistrierungsblattMaske.ID_LUECKEN)).getText();
            assertFalse(stand.equals(texte.get("registration.complete")),
                    "Ohne jede Angabe darf dort nicht \"vollstaendig\" stehen");
        });
    }

    @Test
    @DisplayName("meldet bei vollstaendigen Angaben, dass nichts fehlt")
    void meldetVollstaendigkeit() {
        mitVollstaendigenDaten();

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            assertEquals(texte.get("registration.complete"),
                    ((Label) maske.lookup("#" + RegistrierungsblattMaske.ID_LUECKEN)).getText());
        });
    }

    @Test
    @DisplayName("legt das Blatt als Datei ab und sagt wo")
    void legtDasBlattAb() throws Exception {
        mitVollstaendigenDaten();

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();
            ((Button) maske.lookup("#" + RegistrierungsblattMaske.ID_ABLEGEN)).fire();

            assertEquals(AufzeichnendeMeldungen.Art.ERFOLG, meldungen.einzige().art());
            // Mit dem Pfad: Ein "Gespeichert" ohne Ort laesst jemanden suchen.
            assertTrue(meldungen.einzige().text().contains(RegistrierungsblattMaske.DATEINAME),
                    meldungen.einzige().text());
        });

        Path datei = ablage.resolve(RegistrierungsblattMaske.DATEINAME);
        assertTrue(Files.exists(datei), "Die Datei gehoert nach " + datei);
        assertTrue(Files.readString(datei).contains("Hebammenpraxis Bremen"));
    }
}
