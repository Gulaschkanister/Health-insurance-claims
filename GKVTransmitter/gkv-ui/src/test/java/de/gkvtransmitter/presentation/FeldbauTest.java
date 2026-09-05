package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
            assertTrue(feldbau.pruefe("kassenIk", beschreibung("kassenIk"), "108310401")
                    .orElseThrow().startsWith(texte.get("msg.invalidIk")));
        }

        /**
         * Der Ausweg aus der Sackgasse.
         *
         * <p>Die alte Meldung sagte nur, dass die Pruefziffer nicht stimmt.
         * Wer neun plausible Ziffern eingetippt hatte, wusste danach nicht,
         * wie er zu einer gueltigen Zahl kaeme - es gibt dafuer keinen Weg
         * ausser Rechnen. Die richtige Ziffer steht deshalb in der
         * Beanstandung.</p>
         */
        @Test
        @DisplayName("Die Beanstandung nennt die Pruefziffer, die passen wuerde")
        void nenntDieRichtigePruefziffer() {
            String befund = feldbau.pruefe("kassenIk", beschreibung("kassenIk"), "123456789")
                    .orElseThrow();

            assertTrue(befund.contains("123456780"), befund);
        }

        /**
         * Ein nicht ausgefuelltes Feld ist kein Kennzeichen.
         *
         * <p>Lauter Nullen bestehen die Pruefziffer - 0 mod 10 ist 0 -, und
         * ein {@code int} ohne Zuweisung ist 0. {@code istGueltig(0)} lieferte
         * deshalb {@code true}: eine Person ganz ohne IK kam durch jede
         * Pruefung bis zur Kasse.</p>
         */
        @Test
        @DisplayName("Lauter Nullen sind kein gueltiges IK")
        void nullenSindKeinKennzeichen() {
            assertTrue(feldbau.pruefe("ik", beschreibung("ik"), "000000000").isPresent());
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
            assertEquals(Optional.of(texte.get("msg.invalidIkLength")),
                    feldbau.pruefe("ik", beschreibung("ik"), "1083104"));
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

    /**
     * Was aus hinterlegten Vorschlaegen wird.
     *
     * <p>Simons Einwand: "Wenn es fuer bestimmte Abrechnungscodes nur eine
     * Auswahl gibt, waere es sinnlos, ein Dropdown dafuer zu nutzen." Er hat
     * recht - und die Mechanik war zusaetzlich verkehrt herum eingesetzt: das
     * einzige Feld mit hinterlegten Werten hatte genau einen, und der
     * Umsatzsteuersatz, wo drei Vorschlaege helfen, hatte gar keine.</p>
     */
    @Nested
    @DisplayName("Vorschlaege")
    class Vorschlaege {

        @Test
        @DisplayName("Ein einziger Vorschlag wird ein ausgefuelltes Textfeld, kein Aufklappmenue")
        void einVorschlagIstKeineAuswahl() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node bedienelement = feldbau.bedienelement(
                        feldbau.erzeugeFeld("Abrechnungscode", null, "", List.of("61")));

                assertInstanceOf(TextField.class, bedienelement,
                        "Ein Menue mit einer Zeile ist Bedienlast ohne Nutzen");
                assertEquals("61", ((TextField) bedienelement).getText());
            });
        }

        @Test
        @DisplayName("Mehrere Vorschlaege werden ein Auswahlfeld, das beschreibbar bleibt")
        void mehrereVorschlaege() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node bedienelement = feldbau.bedienelement(
                        feldbau.erzeugeFeld("Umsatzsteuersatz", null, "", List.of("19", "7", "0")));

                assertInstanceOf(ComboBox.class, bedienelement);
                ComboBox<?> auswahl = (ComboBox<?>) bedienelement;
                assertEquals(List.of("19", "7", "0"), auswahl.getItems());
                assertTrue(auswahl.isEditable(),
                        "Ein Vorschlag darf nichts ausschliessen - es kann ein anderer Satz gelten");
            });
        }
    }

    /**
     * Das Info-Zeichen an den Feldern.
     *
     * <p>Simons Vorschlag gegen lange Formulare, mit dem Einwand dagegen:
     * was hinter einem Zeichen liegt, liest niemand. Umgesetzt als
     * Mittelweg - eingeklappt ist die Regel, ausgeklappt die Ausnahme, und
     * zwar dort, wo ein falscher Wert die Lieferung kostet.</p>
     */
    @Nested
    @DisplayName("Info-Zeichen")
    class Infozeichen {

        @Test
        @DisplayName("Am IK bleibt die Erklaerung stehen, ohne Zeichen zum Aufklappen")
        void wichtigesBleibtOffen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("kassenIk", beschreibung("kassenIk"));

                assertTrue(hinweiszeile(feld).isVisible(),
                        "Ein IK kann man sich nicht ausdenken - das muss dastehen");
                assertNull(infozeichen(feld, "kassenIk"),
                        "Ein Zeichen ohne Wirkung waere nur ein Punkt mehr");
            });
        }

        @Test
        @DisplayName("Am Vornamen liegt die Erklaerung hinter dem Zeichen")
        void uebrigesLiegtDahinter() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("firstname", beschreibung("firstname"));

                assertFalse(hinweiszeile(feld).isVisible());
                assertFalse(hinweiszeile(feld).isManaged(),
                        "Sonst bliebe die Zeile leer stehen und das Formular waere gleich lang");
                assertNotNull(infozeichen(feld, "firstname"));
            });
        }

        @Test
        @DisplayName("Ein Klick klappt die Erklaerung auf und wieder zu")
        void zeichenKlapptAufUndZu() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("firstname", beschreibung("firstname"));
                Button zeichen = infozeichen(feld, "firstname");

                zeichen.fire();
                assertTrue(hinweiszeile(feld).isVisible());
                assertTrue(hinweiszeile(feld).isManaged());

                zeichen.fire();
                assertFalse(hinweiszeile(feld).isVisible());
            });
        }

        @Test
        @DisplayName("Der Text haengt auch als Kurzhinweis am Zeichen")
        void zeichenTraegtDenText() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Button zeichen = infozeichen(
                        feldbau.erzeugeFeld("firstname", beschreibung("firstname")), "firstname");

                assertNotNull(zeichen.getTooltip());
                assertEquals(hinweiszeile(feldbau.erzeugeFeld("firstname", beschreibung("firstname")))
                        .getText(), zeichen.getTooltip().getText());
            });
        }

        /**
         * Ein Zeichen, hinter dem nichts liegt, ist eine Falle. Dieselbe
         * Regel gilt fuer das Zeichen in der Statuszeile.
         */
        @Test
        @DisplayName("Ohne Erklaerung gibt es kein Zeichen")
        void keinZeichenOhneErklaerung() {
            JavaFxLaufzeit.aufFxFaden(() -> assertNull(
                    infozeichen(feldbau.erzeugeFeld("ohneHilfe", null), "ohneHilfe")));
        }

        @Test
        @DisplayName("Das Bedienelement bleibt auffindbar, auch neben dem Zeichen")
        void bedienelementBleibtErreichbar() {
            JavaFxLaufzeit.aufFxFaden(() -> assertInstanceOf(TextField.class, feldbau.bedienelement(
                    feldbau.erzeugeFeld("firstname", beschreibung("firstname")))));
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

        /**
         * Beanstandet wird beim <em>Verlassen</em> des Feldes.
         *
         * <p>Nicht beim Tippen: wer eine neunstellige Zahl eingibt, haette
         * sonst nach jeder Ziffer eine rote Zeile unter sich, weil acht von
         * neun Zwischenstaenden falsch sind. Erst wenn etwas beanstandet
         * <em>wurde</em>, laeuft die Pruefung beim Tippen mit - damit die
         * Meldung verschwindet, sobald es stimmt.</p>
         *
         * <p>Bis zum 05.09.2026 war das IK-Feld ein Zaehler, und dessen
         * {@code valueProperty} loeste sofort aus. Seit die Zahlenfelder
         * Textfelder sind (Simons Einwand gegen die Pfeilchen), gilt hier
         * dieselbe Regel wie fuer jedes andere Textfeld.</p>
         */
        @Test
        @DisplayName("Waehrend des Tippens wird noch nicht beanstandet")
        void stillWaehrendDesTippens() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("kassenIk", beschreibung("kassenIk"));

                ((TextField) feldbau.bedienelement(feld)).setText("10831040");

                assertFalse(beanstandungszeile(feld).isVisible(),
                        "Acht von neun Zwischenstaenden einer IK-Eingabe sind falsch");
            });
        }

        @Test
        @DisplayName("Auf Abruf erscheint die Beanstandung und das Feld wird gekennzeichnet")
        void beanstandungErscheint() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("kassenIk", beschreibung("kassenIk"));
                TextField eingabe = (TextField) feldbau.bedienelement(feld);
                eingabe.setText("108310401");

                Optional<String> befund = feldbau.beanstandung(feld);

                assertTrue(befund.orElseThrow().startsWith(texte.get("msg.invalidIk")), befund.orElseThrow());
                Label zeile = beanstandungszeile(feld);
                assertTrue(zeile.isVisible(), "Die Beanstandung muss auch zu sehen sein");
                assertEquals(befund.orElseThrow(), zeile.getText());
                assertTrue(eingabe.getStyleClass().contains(Feldbau.STIL_FEHLERHAFT));
            });
        }

        @Test
        @DisplayName("Wird das IK berichtigt, verschwindet die Beanstandung noch beim Tippen")
        void beanstandungVerschwindet() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node feld = feldbau.erzeugeFeld("kassenIk", beschreibung("kassenIk"));
                TextField eingabe = (TextField) feldbau.bedienelement(feld);
                eingabe.setText("108310401");
                feldbau.beanstandung(feld);
                assertTrue(beanstandungszeile(feld).isVisible());

                eingabe.setText(GUELTIGES_IK);

                assertFalse(beanstandungszeile(feld).isVisible(),
                        "Was einmal beanstandet wurde, wird beim Tippen nachgeprueft");
                assertFalse(eingabe.getStyleClass().contains(Feldbau.STIL_FEHLERHAFT));
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

    private static Label hinweiszeile(Node feld) {
        return zeilenMitStil(feld, Feldbau.STIL_HINWEIS).stream().findFirst()
                .orElseThrow(() -> new AssertionError("Keine Zeile fuer die Erklaerung"));
    }

    /** Das Info-Zeichen eines Feldes, oder {@code null}, wenn es keines gibt. */
    private static Button infozeichen(Node feld, String feldname) {
        return (Button) feld.lookup("#" + Feldbau.ID_INFO + feldname);
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
