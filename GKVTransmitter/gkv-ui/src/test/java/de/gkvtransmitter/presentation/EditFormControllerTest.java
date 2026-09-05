package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.presentation.controller.EditFormController;
import de.gkvtransmitter.presentation.populator.EntityFieldPopulator;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Prueft den {@link EditFormController}.
 *
 * <p>Die Klasse traegt das gesamte Bearbeiten von Personen und war bis zum
 * 05.09.2026 die groesste ungeprueft gebliebene Stelle der Oberflaeche - die
 * Personenmaske prueft nur, dass das Formular erscheint, nicht was darin
 * geschieht. Gerade dort steht aber der Knopf, der Aenderungen an Stammdaten
 * in die Datenbank schreibt.</p>
 *
 * <p>Der Zugang fuehrt ueber {@code onFormReady}: der Controller reicht dort
 * den Behaelter heraus, in dem Felder und Schaltflaechen haengen. Ein
 * {@code lookup} auf das zurueckgegebene {@code ScrollPane} ginge ins Leere,
 * solange dessen Inhalt noch nicht im Knotenbaum haengt.</p>
 */
@DisplayName("Bearbeitungsformular")
class EditFormControllerTest {

    /** Ein Eintrag, an dem sich der Controller ohne Datenbank ausprobieren laesst. */
    private static final class Eintrag {
        private final Map<String, String> werte = new LinkedHashMap<>();
        private final String name;

        Eintrag(String name) {
            this.name = name;
        }
    }

    /** Legt die Werte des Eintrags in den Feldern ab und wieder zurueck. */
    private static final class EintragFelder extends EntityFieldPopulator<Eintrag> {

        @Override
        protected String getFieldValue(String feldname, Eintrag eintrag) {
            return eintrag.werte.get(feldname);
        }

        @Override
        protected void setEntityFieldValue(String feldname, Eintrag eintrag, String wert) {
            eintrag.werte.put(feldname, wert);
        }

        @Override
        public String getDisplayName(Eintrag eintrag) {
            return eintrag.name;
        }

        @Override
        public Object getId(Eintrag eintrag) {
            return eintrag.name;
        }
    }

    private AufzeichnendeMeldungen meldungen;
    private AppMessages texte;
    private List<Eintrag> bestand;
    private List<Eintrag> gespeichert;
    private List<Eintrag> geloescht;
    private int wieOftFormularFertig;
    private int wieOftGeaendert;
    private RuntimeException speicherfehler;
    private VBox behaelter;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        meldungen = new AufzeichnendeMeldungen();
        texte = new AppMessages("/messages/ui-messages.json");
        bestand = new ArrayList<>();
        gespeichert = new ArrayList<>();
        geloescht = new ArrayList<>();
        wieOftFormularFertig = 0;
        wieOftGeaendert = 0;
        speicherfehler = null;
        behaelter = null;
    }

    @Nested
    @DisplayName("Ohne einen einzigen Eintrag")
    class OhneEintraege {

        @Test
        @DisplayName("erscheint der Hinweis statt eines leeren Formulars")
        void hinweisStattFormular() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                ScrollPane ansicht = steuerung(false).buildEditForm();
                aufbauen(ansicht);

                assertTrue(beschriftungen(ansicht).contains(texte.get("msg.noPatients")),
                        "Vorhanden: " + beschriftungen(ansicht));
                assertEquals(0, wieOftFormularFertig, "Es gibt nichts zu bearbeiten");
            });
        }
    }

    @Nested
    @DisplayName("Bei genau einem Eintrag")
    class BeiEinemEintrag {

        @Test
        @DisplayName("wird ohne Umweg ueber ein Auswahlfeld geoeffnet")
        void ohneAuswahlfeld() {
            bestand.add(new Eintrag("Anna Muster"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                ScrollPane ansicht = steuerung(false).buildEditForm();
                aufbauen(ansicht);

                assertEquals(1, wieOftFormularFertig, "Das Formular muss von selbst erscheinen");
                assertTrue(ansicht.lookupAll(".combo-box").isEmpty(),
                        "Bei einem einzigen Eintrag gibt es nichts auszuwaehlen");
            });
        }

        @Test
        @DisplayName("nennt die Ueberschrift den Eintrag beim Namen")
        void ueberschriftMitNamen() {
            bestand.add(new Eintrag("Anna Muster"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();

                assertTrue(beschriftungen(behaelter).contains(
                                texte.get("title.patient.edit") + ": Anna Muster"),
                        "Vorhanden: " + beschriftungen(behaelter));
            });
        }

        @Test
        @DisplayName("stehen die bisherigen Werte schon in den Feldern")
        void werteVorbelegt() {
            Eintrag anna = new Eintrag("Anna Muster");
            anna.werte.put("firstname", "Anna");
            bestand.add(anna);

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();

                assertEquals("Anna", feld("firstname").getText());
            });
        }
    }

    @Nested
    @DisplayName("Beim Speichern")
    class BeimSpeichern {

        @Test
        @DisplayName("landen die geaenderten Werte im Eintrag")
        void werteUebernommen() {
            Eintrag anna = new Eintrag("Anna Muster");
            anna.werte.put("firstname", "Anna");
            bestand.add(anna);

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();
                feld("firstname").setText("Annika");
                knopf(texte.get("button.update")).fire();

                assertEquals(List.of(anna), gespeichert);
                assertEquals("Annika", anna.werte.get("firstname"));
                assertEquals(texte.get("msg.saved"), meldungen.einzige().text());
                assertEquals(1, wieOftGeaendert, "Die Uebersicht muss neu geladen werden");
            });
        }

        @Test
        @DisplayName("wird das Formular danach geraeumt")
        void formularGeraeumt() {
            bestand.add(new Eintrag("Anna Muster"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();
                knopf(texte.get("button.update")).fire();

                assertTrue(behaelter.getChildren().isEmpty());
            });
        }

        @Test
        @DisplayName("bleiben die Eingaben stehen, wenn das Speichern abgelehnt wird")
        void eingabenBleibenBeiAblehnung() {
            bestand.add(new Eintrag("Anna Muster"));
            speicherfehler = new IllegalArgumentException("Nicht gespeichert: das IK stimmt nicht");

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();
                feld("firstname").setText("Annika");
                knopf(texte.get("button.update")).fire();

                assertEquals(AufzeichnendeMeldungen.Art.FEHLER, meldungen.einzige().art());
                assertEquals("Nicht gespeichert: das IK stimmt nicht", meldungen.einzige().text());
                assertFalse(behaelter.getChildren().isEmpty(),
                        "Wer nachbessern soll, muss noch sehen, was er eingegeben hat");
                assertEquals(0, wieOftGeaendert);
                assertTrue(gespeichert.isEmpty());
            });
        }

        @Test
        @DisplayName("meldet eine unlesbare Zahl als Hinweis, nicht als Fehler")
        void unlesbareZahl() {
            bestand.add(new Eintrag("Anna Muster"));
            speicherfehler = new NumberFormatException("For input string: abc");

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();
                knopf(texte.get("button.update")).fire();

                assertEquals(AufzeichnendeMeldungen.Art.HINWEIS, meldungen.einzige().art());
                assertEquals(texte.get("msg.invalidNumbers"), meldungen.einzige().text());
            });
        }
    }

    @Nested
    @DisplayName("Beim Abbrechen")
    class BeimAbbrechen {

        @Test
        @DisplayName("wird nichts gespeichert und nichts gemeldet")
        void nichtsGeschieht() {
            bestand.add(new Eintrag("Anna Muster"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();
                feld("firstname").setText("Annika");
                knopf(texte.get("button.cancel")).fire();

                assertTrue(gespeichert.isEmpty());
                assertTrue(meldungen.leer());
                assertTrue(behaelter.getChildren().isEmpty());
            });
        }
    }

    @Nested
    @DisplayName("Beim Loeschen")
    class BeimLoeschen {

        @Test
        @DisplayName("gibt es die Schaltflaeche nur, wenn sie gewuenscht ist")
        void nurWennGewuenscht() {
            bestand.add(new Eintrag("Anna Muster"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(false).buildEditForm();

                assertTrue(sucheKnopf(texte.get("button.delete")).isEmpty(),
                        "Die Personenmaske loescht ueber die Uebersicht, nicht ueber das Formular");
            });
        }

        @Test
        @DisplayName("wird zuerst zurueckgefragt, und zwar mit dem Namen")
        void fragtZurueck() {
            bestand.add(new Eintrag("Anna Muster"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(true).buildEditForm();
                knopf(texte.get("button.delete")).fire();

                assertEquals(List.of(String.format(texte.get("msg.deleteConfirmBody"), "Anna Muster")),
                        meldungen.gestellteRueckfragen());
                assertTrue(geloescht.isEmpty(), "Ohne Zustimmung wird nichts geloescht");
            });
        }

        @Test
        @DisplayName("wird nach der Zustimmung geloescht")
        void loeschtNachZustimmung() {
            Eintrag anna = new Eintrag("Anna Muster");
            bestand.add(anna);
            meldungen.stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                steuerung(true).buildEditForm();
                knopf(texte.get("button.delete")).fire();

                assertEquals(List.of(anna), geloescht);
                assertEquals(texte.get("msg.deleted"), meldungen.einzige().text());
                assertEquals(1, wieOftGeaendert);
                assertTrue(behaelter.getChildren().isEmpty());
            });
        }
    }

    @Nested
    @DisplayName("Bei mehreren Eintraegen")
    class BeiMehrerenEintraegen {

        @Test
        @DisplayName("stehen alle im Auswahlfeld, und keiner ist vorab geoeffnet")
        void alleZurAuswahl() {
            bestand.add(new Eintrag("Anna Muster"));
            bestand.add(new Eintrag("Bernd Muster"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                ScrollPane ansicht = steuerung(false).buildEditForm();
                aufbauen(ansicht);

                assertEquals(List.of("Anna Muster", "Bernd Muster"), auswahlfeld(ansicht).getItems());
                assertEquals(0, wieOftFormularFertig,
                        "Solange nichts gewaehlt ist, darf auch nichts bearbeitet werden");
            });
        }

        @Test
        @DisplayName("oeffnet die Auswahl den gewaehlten Eintrag")
        void auswahlOeffnetFormular() {
            bestand.add(new Eintrag("Anna Muster"));
            Eintrag bernd = new Eintrag("Bernd Muster");
            bernd.werte.put("firstname", "Bernd");
            bestand.add(bernd);

            JavaFxLaufzeit.aufFxFaden(() -> {
                ScrollPane ansicht = steuerung(false).buildEditForm();
                aufbauen(ansicht);

                // setValue loest das Ereignis bereits selbst aus; ein
                // zusaetzliches fireEvent baute das Formular ein zweites Mal
                // auf. Das ist harmlos, aber es zeigt, dass der Controller
                // jede Auswahl neu aufbaut - auch die schon gewaehlte.
                auswahlfeld(ansicht).setValue("Bernd Muster");

                assertEquals(1, wieOftFormularFertig);
                assertEquals("Bernd", feld("firstname").getText());
            });
        }
    }

    // --- Aufbau ----------------------------------------------------------

    private EditFormController<Eintrag> steuerung(boolean mitLoeschen) {
        return new EditFormController<>(
                new JavaFxUiFactory(), texte, meldungen, new EintragFelder(),
                () -> bestand,
                eintrag -> {
                    if (speicherfehler != null) {
                        throw speicherfehler;
                    }
                    gespeichert.add(eintrag);
                },
                geloescht::add,
                mitLoeschen,
                feldname -> {
                    TextField feld = new TextField();
                    feld.setId(feldname);
                    return feld;
                },
                "Patient",
                fertig -> {
                    behaelter = fertig;
                    wieOftFormularFertig++;
                },
                erneut -> wieOftGeaendert++);
    }

    /**
     * Baut die Darstellung so weit auf, dass ein {@code lookup} greift.
     *
     * <p>Ein {@code ScrollPane} haengt seinen Inhalt erst dann in den
     * Knotenbaum, wenn seine Darstellung erzeugt ist - und das geschieht erst
     * in einer Szene. Ohne diesen Schritt lieferte jeder {@code lookup}
     * {@code null}, und der Test bestuende aus lauter Nichtfunden. Genau
     * deshalb liefert jede Maske ihren nackten Bereich statt eines
     * {@code ScrollPane}; hier geht es nicht anders, weil der Controller ein
     * {@code ScrollPane} zurueckgibt.</p>
     */
    private static void aufbauen(ScrollPane ansicht) {
        new Scene(ansicht);
        ansicht.applyCss();
        ansicht.layout();
    }

    private TextField feld(String feldname) {
        assertNotNull(behaelter, "Es wurde kein Formular aufgebaut");
        TextField feld = (TextField) behaelter.lookup("#" + feldname);
        assertNotNull(feld, "Kein Eingabefeld " + feldname);
        return feld;
    }

    private Button knopf(String beschriftung) {
        List<Button> treffer = sucheKnopf(beschriftung);
        assertEquals(1, treffer.size(), "Erwartet war genau eine Schaltflaeche " + beschriftung);
        return treffer.get(0);
    }

    private List<Button> sucheKnopf(String beschriftung) {
        assertNotNull(behaelter, "Es wurde kein Formular aufgebaut");
        return behaelter.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(knopf -> beschriftung.equals(knopf.getText()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> auswahlfeld(ScrollPane ansicht) {
        Node knoten = ansicht.lookup(".combo-box");
        assertNotNull(knoten, "Kein Auswahlfeld gefunden");
        return (ComboBox<String>) knoten;
    }

    private static List<String> beschriftungen(Node knoten) {
        return knoten.lookupAll(".label").stream()
                .filter(Label.class::isInstance)
                .map(gefunden -> ((Label) gefunden).getText())
                .toList();
    }
}
