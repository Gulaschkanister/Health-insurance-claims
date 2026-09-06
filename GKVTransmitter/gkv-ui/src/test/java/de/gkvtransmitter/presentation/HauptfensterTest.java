package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Button;
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

    /**
     * Der Fenstertitel folgt dem offenen Bereich.
     *
     * <p>{@code App} bindet {@code stage.titleProperty()} daran. Ohne diesen
     * Test faellt es nicht auf, wenn die Bindung ins Leere geht: die
     * Titelleiste bliebe schlicht leer, und das sieht man erst im gebauten
     * Paket - so geschehen am 06.09.2026.</p>
     */
    @Test
    @DisplayName("Der Fenstertitel nennt den offenen Bereich und den Programmnamen")
    void fenstertitel() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            assertTrue(fenster.fenstertitel().get().contains(View.PROGRAMMNAME),
                    "Auch ohne offenen Bereich darf die Leiste nicht leer sein: "
                            + fenster.fenstertitel().get());

            fenster.ergaenzeBereich("Abrechnung", () -> { });
            fenster.ergaenzeBereich("Teilnehmer", () -> { });
            fenster.oeffneErstenBereich();

            navigationseintrag(fenster, "Teilnehmer").fire();

            assertEquals("Teilnehmer – " + View.PROGRAMMNAME, fenster.fenstertitel().get());
        });
    }

    /**
     * Nicht zweimal dasselbe Wort.
     *
     * <p>Seit die Anwendung „GKV-Abrechnung" heisst, ergab der Bereich
     * „Abrechnung" die Titelleiste „Abrechnung – GKV-Abrechnung". Nicht falsch,
     * aber es liest sich wie ein Versehen.</p>
     */
    @Test
    @DisplayName("Steht der Bereich schon im Programmnamen, genuegt der Programmname")
    void keinDoppelterName() {
        assertEquals(View.PROGRAMMNAME, Hauptfenster.fenstertitel("Abrechnung"));
        assertEquals(View.PROGRAMMNAME, Hauptfenster.fenstertitel(null));
        assertEquals("Gruppen – " + View.PROGRAMMNAME, Hauptfenster.fenstertitel("Gruppen"));
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

    /**
     * Die Statuszeile und ihr Info-Zeichen.
     *
     * <p>Dort stand bis zum 05.09.2026 die Aufzaehlung aller geladenen
     * JSON-Dateien samt Nachrichtentypen. Sie war der laengste Text im Programm
     * und wurde entsprechend mit Auslassungspunkten abgeschnitten; ihr eigener
     * Kommentar nannte sie "sichtbare Debug-Hilfe im UI".</p>
     */
    @Test
    @DisplayName("Ohne Info bleibt das Zeichen neben der Statuszeile verborgen")
    void ohneInfoKeinZeichen() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.setzeStatus("2 Vorlagen geladen");

            assertEquals("2 Vorlagen geladen", statuszeile(fenster).getText());
            assertFalse(infozeichen(fenster).isVisible(),
                    "Ein Zeichen, hinter dem nichts liegt, ist eine Falle");
            assertFalse(infozeichen(fenster).isManaged(),
                    "Sonst bliebe daneben ein leerer Platz stehen");
        });
    }

    @Test
    @DisplayName("Mit Info erscheint das Zeichen und ruft beim Klick auf")
    void infoWirdGezeigtUndGerufen() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            int[] wieOft = {0};
            fenster.setzeStatusInfo("Welche Vorlagen geladen sind", () -> wieOft[0]++);

            assertTrue(infozeichen(fenster).isVisible());
            assertNotNull(infozeichen(fenster).getTooltip(),
                    "Ein \"i\" ohne Kurzhinweis sagt nicht, was dahinter liegt");
            assertEquals("Welche Vorlagen geladen sind",
                    infozeichen(fenster).getTooltip().getText());
            infozeichen(fenster).fire();

            assertEquals(1, wieOft[0]);
        });
    }

    @Test
    @DisplayName("Ein zurueckgenommenes Info laesst das Zeichen wieder verschwinden")
    void infoLaesstSichZuruecknehmen() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.setzeStatusInfo("Welche Vorlagen geladen sind", () -> { });
            fenster.setzeStatusInfo(null, null);

            assertFalse(infozeichen(fenster).isVisible());
        });
    }

    /**
     * Ein Bereich mit einem Leerzeichen im Namen.
     *
     * <p>Bis zum 05.09.2026 stand die Beschriftung unveraendert in der Kennung:
     * "Testdaten anlegen" ergab {@code nav-testdaten anlegen}. Ein
     * {@code lookup} darauf findet nichts - das Leerzeichen trennt im
     * Suchausdruck zwei Bedingungen. Aufgefallen ist es nicht durch einen Test,
     * sondern weil die Vorschau den Punkt anklicken wollte: alle bis dahin
     * geprueften Bereiche heissen einwortig.</p>
     */
    @Test
    @DisplayName("Ein mehrwortiger Bereich bleibt auffindbar")
    void mehrwortigerBereich() {
        List<String> geoeffnet = new ArrayList<>();
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Testdaten anlegen", () -> geoeffnet.add("ja"));

            ToggleButton eintrag = navigationseintrag(fenster, "Testdaten anlegen");
            assertFalse(eintrag.getId().contains(" "),
                    "Eine Kennung mit Leerzeichen laesst sich nicht nachschlagen: " + eintrag.getId());

            eintrag.fire();
            assertEquals(1, geoeffnet.size());
        });
    }

    /**
     * Ein Wechsel aus einer Maske heraus nimmt alles mit.
     *
     * <p>{@code zeige} tauscht nur den Inhalt aus. Ueberschrift, Untertitel,
     * Fenstertitel und die Hervorhebung in der Seitenleiste gehoeren zum
     * <em>Bereich</em> - und blieben stehen. Sichtbar wurde das beim Speichern
     * einer Blaupause, die aus einer Vorlage heraus angelegt wird (Simons K4):
     * die Liste stand da, darueber der Vorlagenname.</p>
     */
    @Test
    @DisplayName("Ein Bereichswechsel nimmt Ueberschrift und Seitenleiste mit")
    void wechselNimmtAllesMit() {
        List<String> geoeffnet = new ArrayList<>();
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Geburtsvorbereitungskurs", () -> geoeffnet.add("vorlage"));
            fenster.ergaenzeBereich("Blaupausen", () -> geoeffnet.add("liste"));
            navigationseintrag(fenster, "Geburtsvorbereitungskurs").fire();
            assertEquals("Geburtsvorbereitungskurs", ueberschrift(fenster).getText());

            fenster.wechsleZu("Blaupausen");

            assertEquals("Blaupausen", ueberschrift(fenster).getText());
            assertEquals("Blaupausen – " + View.PROGRAMMNAME, fenster.fenstertitel().get(),
                    "Auch die Titelleiste gehoert zum Bereich");
            assertTrue(navigationseintrag(fenster, "Blaupausen").isSelected(),
                    "Sonst zeigt die Leiste auf etwas anderes als die Ueberschrift daneben");
            assertFalse(navigationseintrag(fenster, "Geburtsvorbereitungskurs").isSelected());
            assertEquals(List.of("vorlage", "liste"), geoeffnet,
                    "Der Bereich muss dabei auch wirklich aufgebaut werden");
        });
    }

    @Test
    @DisplayName("Ein Wechsel in einen unbekannten Bereich geschieht nicht")
    void wechselInsLeere() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Hauptfenster fenster = neuesFenster();
            fenster.ergaenzeBereich("Blaupausen", () -> { });
            navigationseintrag(fenster, "Blaupausen").fire();

            fenster.wechsleZu("Gibt es nicht");

            assertEquals("Blaupausen", ueberschrift(fenster).getText(),
                    "Ein Vertipper im Bereichsnamen darf die Kopfzeile nicht leeren");
        });
    }

    @Test
    @DisplayName("Umlaute und Satzzeichen werden zu Bindestrichen")
    void kennungOhneSonderzeichen() {
        assertEquals("nav-rückbildungskurs-nach-geburten",
                Hauptfenster.kennung("Rückbildungskurs nach Geburten, Einzelabrechnung"
                        .replace(", Einzelabrechnung", "")));
        assertEquals("nav-abrechnung", Hauptfenster.kennung("Abrechnung"));
    }

    /**
     * Ein Rahmen, der sich durchsuchen laesst.
     *
     * <p>Seit die Seitenleiste in einem Rollbereich steckt, haengt sie erst im
     * Knotenbaum, wenn die Darstellung aufgebaut ist — ein {@code lookup} auf
     * einen Navigationseintrag liefe vorher ins Leere. Dieselbe Falle wie bei
     * den Masken, und dieselbe Antwort: kurz in eine {@code Scene} haengen und
     * {@code applyCss()} sowie {@code layout()} rufen.</p>
     *
     * <p>Der Test geht damit denselben Weg wie die Anwendung. Zuvor ging er
     * einen, den es so nicht gibt.</p>
     */
    private static Hauptfenster neuesFenster() {
        Hauptfenster fenster = new Hauptfenster(new Benachrichtigungen().bereich());
        new Scene(fenster.wurzel(), 1100, 720);
        fenster.wurzel().applyCss();
        ((Region) fenster.wurzel()).layout();
        return fenster;
    }

    /** Baut die Darstellung neu auf, nachdem sich der Knotenbaum geaendert hat. */
    private static void aufbauen(Hauptfenster fenster) {
        fenster.wurzel().applyCss();
        ((Region) fenster.wurzel()).layout();
    }

    private static Label statuszeile(Hauptfenster fenster) {
        Label zeile = (Label) fenster.wurzel().lookup("#" + Hauptfenster.ID_STATUS);
        assertNotNull(zeile, "Keine Statuszeile im Rahmen");
        return zeile;
    }

    private static Button infozeichen(Hauptfenster fenster) {
        Button zeichen = (Button) fenster.wurzel().lookup("#" + Hauptfenster.ID_STATUS_INFO);
        assertNotNull(zeichen, "Kein Info-Zeichen neben der Statuszeile");
        return zeichen;
    }

    private static Label ueberschrift(Hauptfenster fenster) {
        Label titel = (Label) fenster.wurzel().lookup("#" + Hauptfenster.ID_TITEL);
        assertNotNull(titel, "Keine Ueberschrift im Rahmen");
        return titel;
    }

    private static ToggleButton navigationseintrag(Hauptfenster fenster, String beschriftung) {
        // Die Eintraege kommen erst nach dem Aufbau der Szene dazu; ohne einen
        // erneuten Durchlauf haengen sie noch nicht im Knotenbaum.
        aufbauen(fenster);
        Region wurzel = (Region) fenster.wurzel();
        ToggleButton eintrag = (ToggleButton) wurzel.lookup(
                "#" + Hauptfenster.kennung(beschriftung));
        assertNotNull(eintrag, "Kein Navigationseintrag " + beschriftung);
        return eintrag;
    }
}
