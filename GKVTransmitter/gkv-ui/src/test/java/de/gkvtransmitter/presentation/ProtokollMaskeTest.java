package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Protokolleintrag;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Die Uebersicht ueber das Uebermittlungsprotokoll.
 *
 * <p>Zwei Dinge muss sie leisten: zeigen, <em>dass</em> etwas uebermittelt
 * wurde (Pflicht nach Anlage 1, Abschnitt 3 Absatz 2), und zeigen, <em>worauf
 * noch Geld aussteht</em> - denn bis zur Bezahlung ist die Sicherungskopie
 * vorzuhalten (Absatz 4).</p>
 */
class ProtokollMaskeTest {

    private SpeicherRepository datenbank;
    private AufzeichnendeMeldungen meldungen;
    private AppMessages texte;
    private AufzeichnenderRahmen rahmen;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        meldungen = new AufzeichnendeMeldungen();
        texte = new AppMessages("/messages/ui-messages.json");
        rahmen = new AufzeichnenderRahmen();
    }

    private Region maskeAufbauen() {
        return new ProtokollMaske(new JavaFxUiFactory(), texte, meldungen, datenbank, rahmen).liste();
    }

    private static Protokolleintrag eintrag(long id, String dateiname) {
        Protokolleintrag eintrag = new Protokolleintrag();
        eintrag.setId(id);
        eintrag.setDateiname(dateiname);
        eintrag.setErstelltAm(OffsetDateTime.now());
        eintrag.setLaufendeNummer(id);
        eintrag.setEmpfaengerIk("104940005");
        eintrag.setGroesseBytes(512);
        return eintrag;
    }

    @Test
    @DisplayName("Ohne Uebermittlung sagt die Maske, dass nichts vorliegt")
    void leeresProtokoll() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            assertNotNull(maske.lookup("#" + ProtokollMaske.ID_LISTE));
            assertEquals(texte.get("log.allPaid"),
                    ((Label) maske.lookup("#" + ProtokollMaske.ID_STAND)).getText());
        });
    }

    @Test
    @DisplayName("Offene Zahlungen werden gezaehlt")
    void zaehltOffeneZahlungen() {
        datenbank.mitProtokolleintrag(eintrag(1, "erste.dta"))
                .mitProtokolleintrag(eintrag(2, "zweite.dta"));

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            assertEquals(String.format(texte.get("log.open"), 2),
                    ((Label) maske.lookup("#" + ProtokollMaske.ID_STAND)).getText());
        });
    }

    /**
     * Eine Eins vor einer Mehrzahl ist im Projekt schon einmal aufgefallen -
     * die Erfolgsmeldung der Abrechnung sagte „1 DTA-Batches erzeugt".
     */
    @Test
    @DisplayName("Bei genau einer offenen Lieferung steht die Einzahl da")
    void einzahlBeiEiner() {
        datenbank.mitProtokolleintrag(eintrag(1, "erste.dta"));

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            assertEquals(texte.get("log.openOne"),
                    ((Label) maske.lookup("#" + ProtokollMaske.ID_STAND)).getText());
        });
    }

    /**
     * Erst der Vermerk gibt die Sicherungskopie frei - Anlage 1, Abschnitt 3
     * Absatz 4.
     */
    @Test
    @DisplayName("Eine Lieferung laesst sich als bezahlt vermerken")
    void vermerktBezahlung() {
        datenbank.mitProtokolleintrag(eintrag(1, "erste.dta"));

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();
            ((Button) maske.lookup("#" + ProtokollMaske.ID_BEZAHLT + "1")).fire();

            assertNotNull(datenbank.ladeProtokoll().get(0).getBezahltAm());
            assertEquals(AufzeichnendeMeldungen.Art.ERFOLG, meldungen.einzige().art());
        });
    }

    @Test
    @DisplayName("Eine bezahlte Lieferung bietet die Schaltflaeche nicht mehr an")
    void bezahlteOhneSchaltflaeche() {
        Protokolleintrag bezahlt = eintrag(1, "erste.dta");
        bezahlt.setBezahltAm(OffsetDateTime.now());
        datenbank.mitProtokolleintrag(bezahlt);

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            assertNull(maske.lookup("#" + ProtokollMaske.ID_BEZAHLT + "1"));
            assertEquals(texte.get("log.allPaid"),
                    ((Label) maske.lookup("#" + ProtokollMaske.ID_STAND)).getText());
        });
    }

    @Test
    @DisplayName("Eine fehlerhafte Uebermittlung nennt ihren Fehlerstatus")
    void zeigtFehlerstatus() {
        Protokolleintrag fehlerhaft = eintrag(1, "erste.dta");
        fehlerhaft.setFehlerfrei(false);
        fehlerhaft.setFehlerstatus("Zielverzeichnis nicht beschreibbar");
        datenbank.mitProtokolleintrag(fehlerhaft);

        JavaFxLaufzeit.aufFxFaden(() -> {
            Region maske = maskeAufbauen();

            boolean gefunden = maske.lookupAll(".feld-hinweis").stream()
                    .filter(Label.class::isInstance)
                    .map(knoten -> ((Label) knoten).getText())
                    .anyMatch(text -> text.contains("Zielverzeichnis nicht beschreibbar"));
            assertTrue(gefunden, "Der Fehlerstatus ist ein Pflichtinhalt und gehoert auf den Schirm");
        });
    }
}
