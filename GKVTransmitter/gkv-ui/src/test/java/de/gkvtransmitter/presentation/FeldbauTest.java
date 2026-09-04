package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;

/**
 * Prueft den Feldbau: die Regeln je Feld und die Erklaerungen darunter.
 *
 * <p>Die Regeln lassen sich ohne Bedienelement pruefen, die Erklaerungen nicht -
 * deshalb beides getrennt.</p>
 */
@DisplayName("Feldbau")
class FeldbauTest {

    /** Ein IK mit richtiger Pruefziffer. */
    private static final String GUELTIGES_IK = "108310400";

    private Feldbau feldbau;
    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        texte = new AppMessages("/messages/ui-messages.json");
        feldbau = new Feldbau(new JavaFxUiFactory(), texte);
    }

    @Nested
    @DisplayName("Institutionskennzeichen")
    class Kennzeichen {

        @Test
        @DisplayName("Ein IK mit falscher Pruefziffer wird beanstandet")
        void falschePruefziffer() {
            assertEquals(Optional.of(texte.get("msg.invalidIk")),
                    feldbau.pruefe("kassenIk", beschreibung("kassenIk"), "108310401"));
        }

        @Test
        @DisplayName("Ein gueltiges IK geht durch")
        void gueltig() {
            assertEquals(Optional.empty(),
                    feldbau.pruefe("kassenIk", beschreibung("kassenIk"), GUELTIGES_IK));
        }

        @Test
        @DisplayName("Auch das eigene IK des Dienstleisters wird geprueft")
        void eigenesIk() {
            assertTrue(feldbau.pruefe("ik", beschreibung("ik"), "123456789").isPresent(),
                    "Ein falsches eigenes IK laesst die Kasse die Lieferung ebenso abweisen");
            assertEquals(Optional.empty(), feldbau.pruefe("ik", beschreibung("ik"), GUELTIGES_IK));
        }

        @Test
        @DisplayName("Ein zu kurzes IK wird beanstandet")
        void zuKurz() {
            assertTrue(feldbau.pruefe("ik", beschreibung("ik"), "1083104").isPresent());
        }

        @Test
        @DisplayName("Ein leeres Feld wird nicht beanstandet")
        void leerIstStill() {
            assertEquals(Optional.empty(), feldbau.pruefe("ik", beschreibung("ik"), ""));
            assertEquals(Optional.empty(), feldbau.pruefe("ik", beschreibung("ik"), "   "));
        }
    }

    @Nested
    @DisplayName("Weitere Regeln")
    class WeitereRegeln {

        @Test
        @DisplayName("Eine vierstellige Postleitzahl wird beanstandet")
        void postleitzahl() {
            assertEquals(Optional.of(texte.get("msg.invalidPlz")),
                    feldbau.pruefe("plz", beschreibung("plz"), "1234"));
            assertEquals(Optional.empty(), feldbau.pruefe("plz", beschreibung("plz"), "12345"));
        }

        @Test
        @DisplayName("Ein zu langer Text wird beanstandet, statt still abgeschnitten zu werden")
        void zuLang() {
            String zuLang = "x".repeat(101);

            Optional<String> befund = feldbau.pruefe("firstname", beschreibung("firstname"), zuLang);

            assertTrue(befund.isPresent(), "Die Hoechstlaenge von 100 muss auffallen");
            assertTrue(befund.get().contains("100"), befund.get());
        }

        @Test
        @DisplayName("Ein Text innerhalb der Grenze geht durch")
        void langeGenug() {
            assertEquals(Optional.empty(),
                    feldbau.pruefe("firstname", beschreibung("firstname"), "x".repeat(100)));
        }
    }

    @Nested
    @DisplayName("Erklaerungen")
    class Erklaerungen {

        @Test
        @DisplayName("Unter dem Kassen-IK steht, wem es gehoert und woher man es nimmt")
        void kassenIk() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                String hinweis = erklaerung("kassenIk");

                assertTrue(hinweis.contains("Krankenkasse"), hinweis);
                assertTrue(hinweis.contains("Versichertenkarte"), hinweis);
            });
        }

        @Test
        @DisplayName("Unter einem Textfeld steht die Hoechstlaenge")
        void hoechstlaenge() {
            JavaFxLaufzeit.aufFxFaden(() -> assertTrue(erklaerung("firstname").contains("100"),
                    erklaerung("firstname")));
        }

        @Test
        @DisplayName("Die Beanstandung nimmt keinen Platz weg, solange es keine gibt")
        void beanstandungOhnePlatz() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Label zeile = beanstandungszeile(feldbau.erzeugeFeld("plz", beschreibung("plz")));

                assertFalse(zeile.isVisible());
                assertFalse(zeile.isManaged(), "Sonst staenden die Felder weiter auseinander als noetig");
            });
        }

        @Test
        @DisplayName("Eine falsche Zahl wird sofort beanstandet und das Feld gekennzeichnet")
        void beanstandungErscheint() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("kassenIk", beschreibung("kassenIk"));
                @SuppressWarnings("unchecked")
                Spinner<Integer> zaehler = (Spinner<Integer>) feldbau.bedienelement(feld);

                zaehler.getValueFactory().setValue(108310401);

                Label zeile = beanstandungszeile(feld);
                assertTrue(zeile.isVisible(), "Die Beanstandung muss zu sehen sein");
                assertEquals(texte.get("msg.invalidIk"), zeile.getText());
                assertTrue(zaehler.getStyleClass().contains(Feldbau.STIL_FEHLERHAFT));
            });
        }

        @Test
        @DisplayName("Wird die Zahl berichtigt, verschwindet die Beanstandung wieder")
        void beanstandungVerschwindet() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("kassenIk", beschreibung("kassenIk"));
                @SuppressWarnings("unchecked")
                Spinner<Integer> zaehler = (Spinner<Integer>) feldbau.bedienelement(feld);
                zaehler.getValueFactory().setValue(108310401);

                zaehler.getValueFactory().setValue(Integer.parseInt(GUELTIGES_IK));

                assertFalse(beanstandungszeile(feld).isVisible());
                assertFalse(zaehler.getStyleClass().contains(Feldbau.STIL_FEHLERHAFT));
            });
        }
    }

    // --- Hilfsmittel ------------------------------------------------------

    private static TagList beschreibung(String feldname) {
        return TagConfigLoader.loadTagConfig("/tags/person-tags.json").get(feldname);
    }

    private String erklaerung(String feldname) {
        return zeilenMitStil(feldbau.erzeugeFeld(feldname, beschreibung(feldname)), Feldbau.STIL_HINWEIS)
                .stream().findFirst().map(Label::getText)
                .orElseThrow(() -> new AssertionError("Keine Erklaerung unter " + feldname));
    }

    private static Label beanstandungszeile(Node feld) {
        return zeilenMitStil(feld, Feldbau.STIL_FEHLER).stream().findFirst()
                .orElseThrow(() -> new AssertionError("Keine Zeile fuer die Beanstandung"));
    }

    private static List<Label> zeilenMitStil(Node feld, String stilklasse) {
        return ((VBox) feld).getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(zeile -> zeile.getStyleClass().contains(stilklasse))
                .toList();
    }
}
