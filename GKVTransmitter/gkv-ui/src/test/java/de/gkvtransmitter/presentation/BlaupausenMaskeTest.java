package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.parser.json.JsonParserFactory;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

/**
 * Prueft die Blaupausenmaske.
 *
 * <p>Eine Blaupause traegt den Preis, mit dem gegenueber der Kasse abgerechnet
 * wird. Bis zum 05.09.2026 liess sie sich anlegen und danach nie wieder
 * ansehen - ein Vertipper im Betrag war weder zu erkennen noch zu berichtigen.
 * Geprueft wird deshalb vor allem, dass gespeicherte Werte wieder erscheinen
 * und dass sich eine Blaupause entfernen laesst.</p>
 */
@DisplayName("Blaupausenmaske")
class BlaupausenMaskeTest {

    private static final String VORLAGE = "Geburtsvorbereitungskurs, Einzelabrechnung";
    private static final String FELD_BETRAG = "Durchschnittlicher Einzelbetrag";

    private SpeicherRepository datenbank;
    private AufzeichnendeMeldungen meldungen;
    private AufzeichnenderRahmen rahmen;
    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        meldungen = new AufzeichnendeMeldungen();
        // Wie in der Anwendung: ein Bereichswechsel raeumt die Meldungsecke.
        // Ohne diese Verdrahtung ginge der Test ueber die Reihenfolge in beiden
        // Reihenfolgen durch und bewiese nichts.
        rahmen = new AufzeichnenderRahmen().beiWechsel(meldungen::raeume);
        texte = new AppMessages("/messages/ui-messages.json");
    }

    @Nested
    @DisplayName("Die Uebersicht")
    class Uebersicht {

        @Test
        @DisplayName("nennt Namen und Preis je Termin")
        void nenntNamenUndPreis() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                datenbank.mitBlaupause(blaupause(7L, "Kurs Montag", "\"" + FELD_BETRAG + "\":\"12,50\""));

                String text = beschriftungen(maske().liste());

                assertTrue(text.contains("Kurs Montag"), text);
                assertTrue(text.contains("12,50"),
                        "Der Preis ist die Angabe, wegen der man nachschlaegt: " + text);
            });
        }

        @Test
        @DisplayName("sagt es, wenn eine Blaupause keinen Preis fuehrt")
        void nenntFehlendenPreis() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                datenbank.mitBlaupause(blaupause(8L, "Ohne Preis", ""));

                assertTrue(beschriftungen(maske().liste()).contains(texte.get("label.notSet")));
            });
        }

        @Test
        @DisplayName("erklaert bei leerer Liste, wozu eine Blaupause dient")
        void erklaertLeereListe() {
            JavaFxLaufzeit.aufFxFaden(() ->
                    assertTrue(beschriftungen(maske().liste()).contains(texte.get("msg.noBlueprints"))));
        }
    }

    @Nested
    @DisplayName("Bearbeiten")
    class Bearbeiten {

        @Test
        @DisplayName("zeigt die gespeicherten Werte wieder an")
        void zeigtGespeicherteWerte() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Blueprint vorhanden = blaupause(3L, "Kurs A", "\"" + FELD_BETRAG + "\":\"12,50\"");

                Region formular = maske().formular(VORLAGE, vorhanden);

                assertEquals("Kurs A", namensfeld(formular).getText());
                assertEquals("12,50", wertVon(formular, FELD_BETRAG),
                        "Ohne das waere jedes Bearbeiten ein Neuanlegen");
            });
        }

        @Test
        @DisplayName("legt beim Speichern keine zweite Blaupause an")
        void ersetztStattAnzulegen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Blueprint vorhanden = blaupause(3L, "Kurs A", "\"" + FELD_BETRAG + "\":\"12,50\"");
                datenbank.mitBlaupause(vorhanden);

                Region formular = maske().formular(VORLAGE, vorhanden);
                setzeWert(formular, FELD_BETRAG, "9,90");
                speichernKnopf(formular).fire();

                assertEquals(1, datenbank.getAllBlueprints().size());
                assertTrue(datenbank.getAllBlueprints().get(0).getPayload().contains("9,90"),
                        datenbank.getAllBlueprints().get(0).getPayload());
            });
        }

        /**
         * Nach dem Speichern steht der richtige Bereich in der Kopfzeile.
         *
         * <p>Simon (K4): "Wenn ich eine Vorlage gespeichert habe werde ich
         * automatisch zur Blaupausen Seite weitergeleitet, jedoch ist der Titel
         * dann nicht der Menuepunkt Blaupause sondern der Name der Vorlage."</p>
         *
         * <p>Die Ursache war ein {@code zeige(liste())}: das tauscht nur den
         * Inhalt aus. Ueberschrift, Untertitel, Fenstertitel und die
         * Hervorhebung in der Seitenleiste gehoeren zum <em>Bereich</em>, und
         * der war weiterhin die Vorlage, aus der man hereingekommen ist.</p>
         */
        @Test
        @DisplayName("wechselt nach dem Speichern in den Bereich Blaupausen")
        void wechseltInDenBereich() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);
                namensfeld(formular).setText("Kurs A");
                setzeWert(formular, FELD_BETRAG, "12,50");

                speichernKnopf(formular).fire();

                assertEquals(texte.get("menu.blueprints"), rahmen.bereich(),
                        "Sonst steht ueber der Liste weiter der Name der Vorlage");
            });
        }

        /**
         * Die Erfolgsmeldung ueberlebt den Bereichswechsel.
         *
         * <p>Die Reihenfolge ist nicht beliebig: ein echter Bereichswechsel
         * raeumt die Meldungsecke. Wer erst meldet und dann wechselt, loescht
         * seine eigene Meldung - und der Benutzer sieht nach dem Speichern
         * nichts.</p>
         */
        @Test
        @DisplayName("meldet den Erfolg nach dem Wechsel, nicht davor")
        void meldetNachDemWechsel() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);
                namensfeld(formular).setText("Kurs A");
                setzeWert(formular, FELD_BETRAG, "12,50");

                speichernKnopf(formular).fire();

                assertTrue(meldungen.einzige().text().contains("Kurs A"),
                        "Nach dem Speichern muss dastehen, was gespeichert wurde");
            });
        }

        @Test
        @DisplayName("verlangt einen Namen")
        void verlangtNamen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);
                namensfeld(formular).setText("   ");

                speichernKnopf(formular).fire();

                assertEquals(texte.get("msg.blueprintNameRequired"), meldungen.einzige().text());
                assertTrue(datenbank.getAllBlueprints().isEmpty());
            });
        }
    }

    @Nested
    @DisplayName("Loeschen")
    class Loeschen {

        @Test
        @DisplayName("geschieht erst nach Rueckfrage")
        void erstNachRueckfrage() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                datenbank.mitBlaupause(blaupause(5L, "Kurs A", ""));

                loeschenKnopf(maske().liste(), 5).fire();

                assertEquals(1, datenbank.getAllBlueprints().size(),
                        "Ohne Zustimmung darf nichts verschwinden");
            });
        }

        @Test
        @DisplayName("entfernt die Blaupause nach Zustimmung")
        void entferntNachZustimmung() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                meldungen.stimmtZu();
                datenbank.mitBlaupause(blaupause(5L, "Kurs A", ""));

                loeschenKnopf(maske().liste(), 5).fire();

                assertTrue(datenbank.getAllBlueprints().isEmpty());
            });
        }
    }

    @Nested
    @DisplayName("Die Eingabefelder")
    class Eingabefelder {

        @Test
        @DisplayName("erklaeren jeweils, was hineingehoert")
        void erklaerenSich() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);
                Node feld = feld(formular, FELD_BETRAG);

                assertNotNull(feld);
                assertTrue(hinweisVon(feld).contains("Preis je Termin"),
                        "Ohne Erklaerung steht dort nur ein Name: " + hinweisVon(feld));
            });
        }

        @Test
        @DisplayName("geben der neunstelligen Positionsnummer keine Zaehlerpfeile")
        void keineZaehlerpfeileBeiLangenZahlen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);

                Node bedienelement = bedienelement(formular, "Abrechnungspositionsnummer");
                assertFalse(bedienelement instanceof Spinner,
                        "Eine neunstellige Nummer klickt niemand hoch");
                assertTrue(bedienelement instanceof TextField, bedienelement.getClass().getName());
            });
        }

        @Test
        @DisplayName("geben dem Betrag ein Textfeld statt eines Zaehlers")
        void keinZaehlerBeimBetrag() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);

                assertFalse(bedienelement(formular, FELD_BETRAG) instanceof Spinner,
                        "Ein vorbelegter Zaehler liefert bei nicht bestaetigter Eingabe die alte Zahl");
            });
        }

        @Test
        @DisplayName("bieten kein leeres Auswahlfeld fuer das Tarifkennzeichen")
        void keinLeeresAuswahlfeld() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);

                Node bedienelement = bedienelement(formular, "Tarifkennzeichen");
                assertFalse(bedienelement instanceof ComboBox,
                        "Ein Auswahlfeld ohne hinterlegte Werte laesst sich aufklappen und ist leer");
            });
        }

        /**
         * Simons Einwand, umgesetzt.
         *
         * <p>{@code codes/abrechnungscodes.json} enthaelt genau einen Eintrag.
         * Bis zum 05.09.2026 wurde daraus ein Aufklappmenue mit einer Zeile -
         * man klappte es auf, um zu erfahren, dass es nichts zu waehlen gibt.
         * Jetzt steht der Wert schon im Feld.</p>
         *
         * <p>Ob der Code ueberhaupt ins Formular gehoert, ist damit
         * <em>nicht</em> entschieden: waere er je Vorlage fest, koennte er ganz
         * verschwinden. Das haengt daran, ob ein kuenftiger Leistungsbereich
         * einen anderen Code braucht, und steht in Anlage 3.</p>
         */
        @Test
        @DisplayName("fuellen den Abrechnungscode vor, statt ein Menue mit einer Zeile anzubieten")
        void einVorschlagStattAufklappmenue() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);

                Node bedienelement = bedienelement(formular, "Abrechnungscode");
                assertFalse(bedienelement instanceof ComboBox,
                        "Ein Menue mit einer einzigen Zeile ist Bedienlast ohne Nutzen");
                assertEquals("61", ((TextField) bedienelement).getText());
            });
        }

        @Test
        @DisplayName("fragen nichts ab, was die Anwendung selbst setzt")
        void fragenNichtsUeberfluessiges() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = maske().formular(VORLAGE, null);

                for (String selbstGesetzt : List.of("Summe Gesamtbetrag", "Gesamtbetrag",
                        "Anzahl/Menge", "Leistungsdatum", "Rechnungsart")) {
                    assertNull(feld(formular, selbstGesetzt),
                            "\"" + selbstGesetzt + "\" wird erfragt, aber verworfen");
                }
            });
        }
    }

    // --- Aufbau und Bedienung -------------------------------------------

    private BlaupausenMaske maske() {
        return new BlaupausenMaske(new JavaFxUiFactory(), texte, meldungen, datenbank, rahmen,
                new Feldbau(new JavaFxUiFactory(), texte), new EchteVorlagen());
    }

    /**
     * Liefert die wirklichen Vorlagen aus den JSON-Definitionen.
     *
     * <p>Bewusst keine erfundenen Felder: der Fehler, um den es hier geht, lag
     * gerade in diesen Dateien. Ein Testdoppel mit ausgedachten Feldern haette
     * ihn nie gezeigt.</p>
     */
    private static final class EchteVorlagen implements BlaupausenMaske.Vorlagen {
        private final Map<String, DtaMessage> vorlagen;

        EchteVorlagen() {
            this.vorlagen = new java.util.LinkedHashMap<>();
            for (DtaMessage nachricht : new JsonParserFactory().parseInvoices()) {
                vorlagen.put(nachricht.getInvoicerName(), nachricht);
            }
        }

        @Override
        public Map<String, DtaMessage> alle() {
            return vorlagen;
        }

        @Override
        public List<String> auswahlFuer(String feldname) {
            return "Abrechnungscode".equals(feldname) ? List.of("61") : List.of();
        }
    }

    private static Blueprint blaupause(long id, String name, String felder) {
        Blueprint blaupause = new Blueprint(name, VORLAGE,
                "{\"template\":\"" + VORLAGE + "\",\"fields\":{" + felder + "}}", OffsetDateTime.now());
        blaupause.setId(id);
        return blaupause;
    }

    private TextField namensfeld(Region formular) {
        return (TextField) formular.lookup("#" + BlaupausenMaske.ID_NAME);
    }

    private Button speichernKnopf(Region formular) {
        return (Button) formular.lookup("#" + BlaupausenMaske.ID_SPEICHERN);
    }

    private Button loeschenKnopf(Region liste, long id) {
        return (Button) liste.lookup("#" + BlaupausenMaske.KENNUNG + Listenbau.ZEILE + id
                + "-" + BlaupausenMaske.AKTION_LOESCHEN);
    }

    /** Das Feld hinter einer Beschriftung, oder {@code null}. */
    private Node feld(Region formular, String feldname) {
        for (Node knoten : formular.lookupAll(".label")) {
            if (knoten instanceof Label beschriftung && feldname.equals(beschriftung.getText())
                    && beschriftung.getParent() instanceof javafx.scene.layout.VBox huelle
                    && huelle.getChildren().size() > 1) {
                return huelle.getChildren().get(1);
            }
        }
        return null;
    }

    private Node bedienelement(Region formular, String feldname) {
        Node feld = feld(formular, feldname);
        assertNotNull(feld, "Feld nicht gefunden: " + feldname);
        return Feldbau.bedienelement(feld);
    }

    private String wertVon(Region formular, String feldname) {
        return new Feldbau(new JavaFxUiFactory(), texte).textVon(feld(formular, feldname));
    }

    private void setzeWert(Region formular, String feldname, String wert) {
        Node bedienelement = bedienelement(formular, feldname);
        if (bedienelement instanceof javafx.scene.control.TextInputControl eingabe) {
            eingabe.setText(wert);
        } else if (bedienelement instanceof ComboBox<?> auswahl) {
            auswahl.getEditor().setText(wert);
        }
    }

    /** Die Erklaerung unter einem Feld. */
    private String hinweisVon(Node feld) {
        if (feld instanceof javafx.scene.layout.VBox huelle) {
            for (Node kind : huelle.getChildren()) {
                if (kind instanceof Label beschriftung
                        && beschriftung.getStyleClass().contains(Feldbau.STIL_HINWEIS)) {
                    return beschriftung.getText();
                }
            }
        }
        return "";
    }

    /** Alle sichtbaren Beschriftungen eines Bereichs, aneinandergereiht. */
    private static String beschriftungen(Region bereich) {
        StringBuilder text = new StringBuilder();
        for (Node knoten : bereich.lookupAll(".label")) {
            if (knoten instanceof Label beschriftung && beschriftung.getText() != null) {
                text.append(beschriftung.getText()).append(" | ");
            }
        }
        return text.toString();
    }
}
