package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Prueft den Rahmen: Seitenleiste, Ueberschrift und den Platz der Masken.
 */
@DisplayName("Hauptfenster")
class HauptfensterTest {

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @Test
    @DisplayName("Der erste Bereich wird beim Start geoeffnet und ist ausgewaehlt")
    void ersterBereich() {
        List<String> geoeffnet = new ArrayList<>();
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Abrechnung", () -> geoeffnet.add("Abrechnung"));
            fenster.ergaenzeBereich("Teilnehmer", () -> geoeffnet.add("Teilnehmer"));

            fenster.oeffneErstenBereich();

            assertEquals(List.of("Abrechnung"), geoeffnet);
            assertEquals("Abrechnung", ueberschrift(fenster).getText());
            assertTrue(navigationseintrag(fenster, "Abrechnung").isSelected());
        });
    }

    @Test
    @DisplayName("Ein Klick auf einen Bereich oeffnet ihn und setzt die Ueberschrift")
    void bereichWechseln() {
        List<String> geoeffnet = new ArrayList<>();
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Abrechnung", () -> geoeffnet.add("Abrechnung"));
            fenster.ergaenzeBereich("Teilnehmer", () -> geoeffnet.add("Teilnehmer"));
            fenster.oeffneErstenBereich();
            geoeffnet.clear();

            navigationseintrag(fenster, "Teilnehmer").fire();

            assertEquals(List.of("Teilnehmer"), geoeffnet);
            assertEquals("Teilnehmer", ueberschrift(fenster).getText());
        });
    }

    @Test
    @DisplayName("Nur ein Eintrag ist gleichzeitig ausgewaehlt")
    void nurEinerAusgewaehlt() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Abrechnung", () -> { });
            fenster.ergaenzeBereich("Teilnehmer", () -> { });
            fenster.oeffneErstenBereich();

            navigationseintrag(fenster, "Teilnehmer").fire();

            assertTrue(navigationseintrag(fenster, "Teilnehmer").isSelected());
            assertTrue(!navigationseintrag(fenster, "Abrechnung").isSelected(),
                    "Sonst waere nicht zu erkennen, wo man ist");
        });
    }

    @Test
    @DisplayName("Ein erneuter Klick auf den offenen Bereich laedt ihn neu, statt ihn abzuwaehlen")
    void erneuterKlick() {
        List<String> geoeffnet = new ArrayList<>();
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Teilnehmer", () -> geoeffnet.add("Teilnehmer"));
            fenster.oeffneErstenBereich();

            navigationseintrag(fenster, "Teilnehmer").fire();

            assertEquals(2, geoeffnet.size());
            assertTrue(navigationseintrag(fenster, "Teilnehmer").isSelected());
        });
    }

    @Test
    @DisplayName("Leeren kehrt zum offenen Bereich zurueck, statt eine leere Flaeche zu zeigen")
    void leerenKehrtZurueck() {
        List<String> geoeffnet = new ArrayList<>();
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Teilnehmer", () -> {
                geoeffnet.add("Teilnehmer");
                fenster.zeige(new VBox());
            });
            fenster.oeffneErstenBereich();
            fenster.zeige(new VBox());

            fenster.leeren();

            assertEquals(2, geoeffnet.size(), "Der Bereich muss erneut aufgebaut werden");
        });
    }

    @Test
    @DisplayName("Ohne offenen Bereich raeumt Leeren die Flaeche")
    void leerenOhneBereich() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            VBox inhalt = new VBox();
            fenster.zeige(inhalt);

            fenster.leeren();

            assertNull(fenster.gezeigterInhalt());
        });
    }

    @Test
    @DisplayName("Was gezeigt wird, steht danach im Inhaltsbereich")
    void inhaltGesetzt() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            VBox inhalt = new VBox();

            fenster.zeige(inhalt);

            assertSame(inhalt, fenster.gezeigterInhalt());
        });
    }

    private static Hauptfenster neuesFenster() {
        return new Hauptfenster(new Benachrichtigungen().bereich());
    }

    private static Label ueberschrift(Hauptfenster fenster) {
        Label titel = (Label) fenster.wurzel().lookup("#" + Hauptfenster.ID_TITEL);
        assertNotNull(titel, "Keine Ueberschrift im Rahmen");
        return titel;
    }

    private static ToggleButton navigationseintrag(Hauptfenster fenster, String beschriftung) {
        Region wurzel = (Region) fenster.wurzel();
        ToggleButton eintrag = (ToggleButton) wurzel.lookup(
                "#" + Hauptfenster.ID_NAVIGATION + beschriftung.toLowerCase(java.util.Locale.GERMAN));
        assertNotNull(eintrag, "Kein Navigationseintrag " + beschriftung);
        return eintrag;
    }
}
