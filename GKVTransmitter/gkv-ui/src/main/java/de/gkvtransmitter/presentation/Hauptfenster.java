package de.gkvtransmitter.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Der Rahmen der Anwendung: Seitenleiste links, Maske rechts, Meldungen oben
 * rechts, Statuszeile unten.
 *
 * <p>Tritt an die Stelle der Menueleiste. Ein Menue zeigt nach dem Loslassen
 * keine Spur mehr davon, wo man ist; die Seitenleiste laesst den gewaehlten
 * Bereich stehen. Nebenbei verschwindet damit ein Kunstgriff, der noetig war,
 * um "Abrechnung" mit einem Klick zu oeffnen: das Menue feuerte seinen einzigen
 * Eintrag beim Aufklappen selbst ab und schloss sich sofort wieder.</p>
 *
 * <p>Der Rahmen ist zugleich der {@link Maskenrahmen}. {@link #leeren()} zeigt
 * nicht etwa nichts, sondern oeffnet den gewaehlten Bereich erneut - nach dem
 * Speichern eines Teilnehmers landet man so wieder in der Teilnehmerliste und
 * nicht vor einer leeren Flaeche.</p>
 */
public class Hauptfenster implements Maskenrahmen {

    /** Kennung der Ueberschrift ueber der Maske. */
    public static final String ID_TITEL = "kopf-titel";
    /** Vorsatz der Kennung eines Navigationseintrags, gefolgt von seinem Namen. */
    public static final String ID_NAVIGATION = "nav-";

    private final BorderPane geruest = new BorderPane();
    private final VBox seitenleiste = new VBox();
    private final ToggleGroup navigationsgruppe = new ToggleGroup();
    private final ScrollPane inhaltsbereich = new ScrollPane();
    private final Label ueberschrift = new Label();
    private final Label statuszeile = new Label();
    private final StackPane wurzel;

    /** Die Bereiche in der Reihenfolge ihrer Aufnahme. */
    private final Map<String, Runnable> bereiche = new LinkedHashMap<>();

    private String offenerBereich;

    /**
     * @param meldungsecke der Knoten, der ueber die Maske gelegt wird
     */
    public Hauptfenster(Region meldungsecke) {
        Objects.requireNonNull(meldungsecke, "meldungsecke must not be null");

        seitenleiste.getStyleClass().add("seitenleiste");

        ueberschrift.setId(ID_TITEL);
        ueberschrift.getStyleClass().add("kopf-titel");
        VBox kopfzeile = new VBox(ueberschrift);
        kopfzeile.getStyleClass().add("kopfzeile");

        inhaltsbereich.getStyleClass().add("inhalt");
        inhaltsbereich.setFitToWidth(true);

        statuszeile.getStyleClass().add("statuszeile");
        statuszeile.setMaxWidth(Double.MAX_VALUE);

        // Die Seitenleiste reicht ueber die volle Hoehe; Kopfzeile und
        // Statuszeile gehoeren nur zur Maske daneben. Laege die Kopfzeile im
        // aeusseren Rahmen oben, schoebe sie die Seitenleiste nach unten und
        // der Programmname stuende nicht mehr in der Ecke.
        BorderPane inhaltsspalte = new BorderPane();
        inhaltsspalte.setTop(kopfzeile);
        inhaltsspalte.setCenter(inhaltsbereich);
        inhaltsspalte.setBottom(statuszeile);

        geruest.setLeft(seitenleiste);
        geruest.setCenter(inhaltsspalte);

        wurzel = new StackPane(geruest, meldungsecke);
        StackPane.setAlignment(meldungsecke, Pos.TOP_RIGHT);
    }

    /** Setzt Name und Untertitel oben in der Seitenleiste. */
    public void setzeMarke(String name, String zusatz) {
        Label marke = new Label(name);
        marke.getStyleClass().add("marke");
        Label untertitel = new Label(zusatz);
        untertitel.getStyleClass().add("marke-zusatz");
        untertitel.setWrapText(true);
        seitenleiste.getChildren().addAll(0, java.util.List.of(marke, untertitel));
    }

    /** Ueberschrift ueber einer Gruppe von Bereichen. */
    public void ergaenzeAbschnitt(String beschriftung) {
        Label ueberschriftDesAbschnitts = new Label(beschriftung.toUpperCase(java.util.Locale.GERMAN));
        ueberschriftDesAbschnitts.getStyleClass().add("nav-gruppe");
        seitenleiste.getChildren().add(ueberschriftDesAbschnitts);
    }

    /**
     * Ergaenzt einen Bereich in der Seitenleiste.
     *
     * @param oeffnen was geschieht, wenn er gewaehlt wird
     */
    public void ergaenzeBereich(String beschriftung, Runnable oeffnen) {
        bereiche.put(beschriftung, oeffnen);

        ToggleButton eintrag = new ToggleButton(beschriftung);
        eintrag.setId(ID_NAVIGATION + beschriftung.toLowerCase(java.util.Locale.GERMAN));
        eintrag.getStyleClass().add("nav-eintrag");
        eintrag.setMaxWidth(Double.MAX_VALUE);
        eintrag.setToggleGroup(navigationsgruppe);
        // Ein erneuter Klick auf den offenen Bereich soll ihn neu laden und
        // nicht die Auswahl aufheben - sonst stuende man ohne Bereich da.
        eintrag.setOnAction(ereignis -> {
            eintrag.setSelected(true);
            oeffne(beschriftung);
        });
        seitenleiste.getChildren().add(eintrag);
    }

    /** Oeffnet den ersten aufgenommenen Bereich. Fuer den Programmstart. */
    public void oeffneErstenBereich() {
        bereiche.keySet().stream().findFirst().ifPresent(erster -> {
            navigationsgruppe.getToggles().stream()
                    .filter(ToggleButton.class::isInstance)
                    .map(ToggleButton.class::cast)
                    .filter(eintrag -> erster.equals(eintrag.getText()))
                    .findFirst()
                    .ifPresent(eintrag -> eintrag.setSelected(true));
            oeffne(erster);
        });
    }

    private void oeffne(String beschriftung) {
        Runnable oeffnen = bereiche.get(beschriftung);
        if (oeffnen == null) {
            return;
        }
        offenerBereich = beschriftung;
        ueberschrift.setText(beschriftung);
        oeffnen.run();
    }

    public void setzeStatus(String text) {
        statuszeile.setText(text == null ? "" : text);
    }

    /** Der Knoten, der in die Szene gehaengt wird. */
    public Parent wurzel() {
        return wurzel;
    }

    @Override
    public void zeige(Region inhalt) {
        inhaltsbereich.setContent(inhalt);
    }

    /**
     * Was gerade zu sehen ist, oder {@code null}.
     *
     * <p>Nicht ueber den Knotenbaum zu erreichen: der Inhalt eines
     * {@code ScrollPane} haengt erst darin, wenn die Darstellung aufgebaut ist.
     * Genau deshalb liefern die Masken ihren nackten Bereich.</p>
     */
    public Node gezeigterInhalt() {
        return inhaltsbereich.getContent();
    }

    /**
     * Kehrt zum offenen Bereich zurueck.
     *
     * <p>Zuvor hiess Leeren woertlich: nach dem Speichern einer Gruppe blieb
     * eine leere Flaeche zurueck, und man musste sich selbst wieder
     * hinnavigieren.</p>
     */
    @Override
    public void leeren() {
        if (offenerBereich != null) {
            oeffne(offenerBereich);
            return;
        }
        inhaltsbereich.setContent(null);
    }
}
