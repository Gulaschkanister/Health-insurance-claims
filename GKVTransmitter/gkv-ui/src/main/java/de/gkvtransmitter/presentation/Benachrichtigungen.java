package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Die Meldungsecke oben rechts.
 *
 * <p>Tritt an die Stelle der bisherigen {@code Alert}-Fenster. Ein Fenster
 * legte sich vor die Anwendung und musste weggeklickt werden, ehe es
 * weiterging - fuer die Mitteilung "Teilnehmer erfolgreich erstellt" ist das
 * zu viel Aufhebens. Hier steht die Meldung am Rand, unterbricht nichts und
 * geht von selbst wieder.</p>
 *
 * <p>Nicht alles verschwindet: Fehler und Rueckfragen bleiben stehen, bis
 * jemand sie zur Kenntnis genommen hat. Was schiefgegangen ist, darf sich
 * nicht davonstehlen, waehrend man gerade woanders hinsieht.</p>
 */
public class Benachrichtigungen {

    /** Gewicht einer Meldung. Bestimmt Farbe und ob sie von selbst vergeht. */
    public enum Art {
        /** Etwas hat geklappt. Vergeht. */
        ERFOLG("Erledigt", "meldung-erfolg", true),
        /** Etwas fehlt noch oder ist zu beachten. Vergeht. */
        HINWEIS("Hinweis", "meldung-hinweis", true),
        /** Etwas ist schiefgegangen. Bleibt stehen. */
        FEHLER("Fehler", "meldung-fehler", false);

        private final String titel;
        private final String stilklasse;
        private final boolean vergeht;

        Art(String titel, String stilklasse, boolean vergeht) {
            this.titel = titel;
            this.stilklasse = stilklasse;
            this.vergeht = vergeht;
        }
    }

    /** Wie lange eine vergehende Meldung stehen bleibt. */
    public static final Duration STANDARDDAUER = Duration.seconds(6);

    /**
     * Mehr Meldungen zeigt die Ecke nicht gleichzeitig; die aelteste weicht.
     * Ohne diese Grenze koennte ein Durchlauf mit vielen Beanstandungen den
     * halben Bildschirm zustellen.
     */
    private static final int HOECHSTZAHL = 4;

    private static final Duration UEBERBLENDUNG = Duration.millis(220);

    private final VBox ecke = new VBox(10);
    private final Duration anzeigedauer;

    /** Die Texte der sichtbaren Meldungen, in derselben Reihenfolge wie die Karten. */
    private final List<String> offeneTexte = new ArrayList<>();

    public Benachrichtigungen() {
        this(STANDARDDAUER);
    }

    public Benachrichtigungen(Duration anzeigedauer) {
        this.anzeigedauer = Objects.requireNonNull(anzeigedauer, "anzeigedauer must not be null");
        ecke.getStyleClass().add("meldungsecke");
        ecke.setAlignment(Pos.TOP_RIGHT);
        // Die Ecke liegt ueber der Maske. Ohne das hier fienge sie auch dort
        // Klicks ab, wo gar keine Meldung steht, und die Maske waere in ihrem
        // oberen rechten Viertel nicht mehr bedienbar.
        ecke.setPickOnBounds(false);
    }

    /** Der Knoten, der ueber die Maske gelegt wird. */
    public Region bereich() {
        return ecke;
    }

    /** Zeigt eine einzeilige Meldung. */
    public void zeige(Art art, String text) {
        haenge(art, baueKarte(art, art.titel, new Label[] {fliesstext(text)}), art.vergeht, text);
    }

    /**
     * Zeigt eine Meldung, die stehen bleibt, bis jemand sie schliesst - mit
     * beliebigem Inhalt darunter, etwa einer Liste von Beanstandungen oder
     * zwei Schaltflaechen fuer eine Rueckfrage.
     *
     * @return die Karte, damit der Aufrufer sie wieder wegnehmen kann - etwa
     *         sobald die Rueckfrage beantwortet ist
     */
    public Node zeigeBleibend(Art art, String titel, String text, Node zusatz) {
        List<Node> inhalt = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            inhalt.add(fliesstext(text));
        }
        if (zusatz != null) {
            inhalt.add(zusatz);
        }
        Region karte = baueKarte(art, titel, inhalt.toArray(Node[]::new));
        haenge(art, karte, false, text);
        return karte;
    }

    /** Nimmt alle Meldungen weg. */
    public void leeren() {
        ecke.getChildren().clear();
        offeneTexte.clear();
    }

    /** Die Texte der gerade sichtbaren Meldungen, aelteste zuerst. */
    public List<String> offene() {
        return List.copyOf(offeneTexte);
    }

    private void haenge(Art art, Region karte, boolean vergeht, String text) {
        while (ecke.getChildren().size() >= HOECHSTZAHL) {
            entferne(ecke.getChildren().get(0));
        }
        karte.setUserData(text);
        ecke.getChildren().add(karte);
        offeneTexte.add(text == null ? "" : text);

        FadeTransition auf = new FadeTransition(UEBERBLENDUNG, karte);
        auf.setFromValue(0);
        auf.setToValue(1);

        if (!vergeht) {
            auf.play();
            return;
        }

        PauseTransition warten = new PauseTransition(anzeigedauer);
        FadeTransition zu = new FadeTransition(UEBERBLENDUNG, karte);
        zu.setFromValue(1);
        zu.setToValue(0);

        SequentialTransition ablauf = new SequentialTransition(auf, warten, zu);
        ablauf.setOnFinished(ereignis -> entferne(karte));
        ablauf.play();
    }

    /** Nimmt eine einzelne Meldung weg. */
    public void entferne(Node karte) {
        int stelle = ecke.getChildren().indexOf(karte);
        if (stelle < 0) {
            return;
        }
        ecke.getChildren().remove(stelle);
        if (stelle < offeneTexte.size()) {
            offeneTexte.remove(stelle);
        }
    }

    private Region baueKarte(Art art, String titel, Node... inhalt) {
        Label ueberschrift = new Label(titel);
        ueberschrift.getStyleClass().add("meldung-titel");

        VBox spalte = new VBox(6, ueberschrift);
        spalte.getChildren().addAll(inhalt);
        HBox.setHgrow(spalte, Priority.ALWAYS);

        Button schliessen = new Button("×");
        schliessen.getStyleClass().add("meldung-schliessen");
        schliessen.setFocusTraversable(false);

        HBox karte = new HBox(8, spalte, schliessen);
        karte.getStyleClass().addAll("meldung", art.stilklasse);
        karte.setAlignment(Pos.TOP_LEFT);
        schliessen.setOnAction(ereignis -> entferne(karte));
        return karte;
    }

    private Label fliesstext(String text) {
        Label beschriftung = new Label(text);
        beschriftung.getStyleClass().add("meldung-text");
        beschriftung.setWrapText(true);
        beschriftung.setMaxWidth(320);
        return beschriftung;
    }
}
