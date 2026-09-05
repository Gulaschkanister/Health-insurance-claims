package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Baut eine durchsuchbare Liste mit Schaltflaechen je Zeile.
 *
 * <p>Sie loest die Auswahl per Dialogfenster ab. Wer einen Teilnehmer
 * bearbeiten wollte, bekam bisher eine Auswahlbox mit Namen darin - man musste
 * wissen, wen man sucht, und sah nichts weiter. Hier stehen alle Eintraege mit
 * ihren Werten, und "Bearbeiten" steht in der Zeile, um die es geht.</p>
 *
 * <p>Bewusst ein {@code GridPane} und keine {@code TableView}. Eine TableView
 * erzeugt ihre Zeilen erst beim Zeichnen und nur die sichtbaren; im Test waere
 * keine davon auffindbar. Bei den hier zu erwartenden Mengen - Teilnehmer
 * eines Kurses, Gruppen einer Praxis - wiegt die Pruefbarkeit schwerer als die
 * Ersparnis.</p>
 *
 * @param <T> was in der Liste steht
 */
public class Listenbau<T> {

    /** Nachsatz der Kennung des Suchfelds. */
    public static final String SUCHE = "-suche";
    /** Nachsatz der Kennung einer Zeile, vor der Kennnummer des Eintrags. */
    public static final String ZEILE = "-zeile-";

    private record Aktion<T>(String beschriftung, String kennung, String stilklasse, Consumer<T> tun) {
    }

    private final UiFactory bausteine;
    private final String kennungsvorsatz;
    private final List<Aktion<T>> aktionen = new ArrayList<>();

    private List<String> ueberschriften = List.of();
    private Function<T, List<String>> zellen = eintrag -> List.of();
    private Function<T, String> kennnummer = eintrag -> "";
    private Function<T, String> suchtext;
    private String hinweisWennLeer = "";
    private String suchhinweis = "Suchen";

    /**
     * @param kennungsvorsatz Vorsatz aller Kennungen dieser Liste, etwa
     *        {@code "teilnehmer"}; daraus wird {@code teilnehmer-suche} und
     *        {@code teilnehmer-zeile-7}
     */
    public Listenbau(UiFactory bausteine, String kennungsvorsatz) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.kennungsvorsatz = Objects.requireNonNull(kennungsvorsatz, "kennungsvorsatz must not be null");
    }

    /** Die Spaltenueberschriften. */
    public Listenbau<T> spalten(String... ueberschriften) {
        this.ueberschriften = List.of(ueberschriften);
        return this;
    }

    /** Was in den Spalten einer Zeile steht, in derselben Reihenfolge. */
    public Listenbau<T> zellen(Function<T, List<String>> zellen) {
        this.zellen = Objects.requireNonNull(zellen, "zellen must not be null");
        return this;
    }

    /** Woraus die Kennung einer Zeile gebildet wird. */
    public Listenbau<T> kennnummer(Function<T, String> kennnummer) {
        this.kennnummer = Objects.requireNonNull(kennnummer, "kennnummer must not be null");
        return this;
    }

    /** Wonach das Suchfeld filtert. Ohne Angabe gibt es keines. */
    public Listenbau<T> durchsuchbar(String suchhinweis, Function<T, String> suchtext) {
        this.suchhinweis = suchhinweis;
        this.suchtext = Objects.requireNonNull(suchtext, "suchtext must not be null");
        return this;
    }

    /** Was steht, wenn es nichts zu zeigen gibt. */
    public Listenbau<T> hinweisWennLeer(String hinweis) {
        this.hinweisWennLeer = hinweis;
        return this;
    }

    /**
     * Ergaenzt eine Schaltflaeche je Zeile.
     *
     * @param kennung Nachsatz der Kennung, etwa {@code "bearbeiten"}
     */
    public Listenbau<T> aktion(String beschriftung, String kennung, String stilklasse, Consumer<T> tun) {
        aktionen.add(new Aktion<>(beschriftung, kennung, stilklasse, tun));
        return this;
    }

    /** Baut die Liste. */
    public Region baue(List<T> eintraege) {
        List<T> bestand = eintraege == null ? List.of() : eintraege;

        VBox wurzel = new VBox(12);
        GridPane gitter = neuesGitter();

        if (suchtext != null && !bestand.isEmpty()) {
            TextField suche = bausteine.createTextField();
            suche.setId(kennungsvorsatz + SUCHE);
            suche.setPromptText(suchhinweis);
            suche.setPrefWidth(280);
            suche.textProperty().addListener((wert, alt, neu) -> fuelle(gitter, gefiltert(bestand, neu)));
            wurzel.getChildren().add(suche);
        }

        fuelle(gitter, bestand);
        wurzel.getChildren().add(gitter);
        return wurzel;
    }

    private List<T> gefiltert(List<T> bestand, String suchbegriff) {
        if (suchbegriff == null || suchbegriff.isBlank()) {
            return bestand;
        }
        String gesucht = suchbegriff.toLowerCase(Locale.GERMAN);
        return bestand.stream()
                .filter(eintrag -> suchtext.apply(eintrag).toLowerCase(Locale.GERMAN).contains(gesucht))
                .toList();
    }

    private void fuelle(GridPane gitter, List<T> bestand) {
        gitter.getChildren().clear();
        if (bestand.isEmpty()) {
            Label hinweis = bausteine.createLabel(hinweisWennLeer);
            hinweis.getStyleClass().add("zeile-still");
            gitter.add(hinweis, 0, 0);
            return;
        }

        for (int spalte = 0; spalte < ueberschriften.size(); spalte++) {
            Label ueberschrift = bausteine.createLabel(ueberschriften.get(spalte));
            ueberschrift.getStyleClass().add("tabellenkopf");
            gitter.add(ueberschrift, spalte, 0);
        }

        int zeile = 1;
        for (T eintrag : bestand) {
            List<String> werte = zellen.apply(eintrag);
            for (int spalte = 0; spalte < werte.size(); spalte++) {
                gitter.add(bausteine.createLabel(werte.get(spalte)), spalte, zeile);
            }
            if (!aktionen.isEmpty()) {
                gitter.add(schaltflaechen(eintrag), Math.max(werte.size(), ueberschriften.size()), zeile);
            }
            zeile++;
        }
    }

    /**
     * Die Schaltflaechen einer Zeile.
     *
     * <p>Sie duerfen <b>nicht schrumpfen</b>. Reichte der Platz nicht, kuerzte
     * JavaFX ihre Beschriftung mit Auslassungspunkten, und in der
     * Blaupausenliste stand tatsaechlich "Bearbei..." neben "Lösc...". Ein
     * gekuerzter Name ist unschoen; eine gekuerzte Schaltflaeche, die
     * unwiderruflich loescht, ist ein Bedienfehler in Wartestellung.</p>
     */
    private HBox schaltflaechen(T eintrag) {
        HBox reihe = new HBox(6);
        reihe.setAlignment(Pos.CENTER_LEFT);
        for (Aktion<T> aktion : aktionen) {
            Button schaltflaeche = bausteine.createButton(aktion.beschriftung());
            schaltflaeche.setId(kennungsvorsatz + ZEILE + kennnummer.apply(eintrag) + "-" + aktion.kennung());
            schaltflaeche.getStyleClass().add(aktion.stilklasse());
            schaltflaeche.setMinWidth(Region.USE_PREF_SIZE);
            schaltflaeche.setOnAction(ereignis -> aktion.tun().accept(eintrag));
            reihe.getChildren().add(schaltflaeche);
        }
        reihe.setMinWidth(Region.USE_PREF_SIZE);
        return reihe;
    }

    /**
     * Das Gitter der Liste.
     *
     * <p>Der freie Platz geht an die erste Spalte - dort stehen die Namen, und
     * die sollen lesbar bleiben. Die uebrigen Spalten behalten ihre
     * bevorzugte Breite; bis zum 05.09.2026 hatten sie <b>gar keine</b>
     * Vorgabe, und wenn die Summe aller Spalten breiter wurde als die Liste,
     * schrumpften sie alle zugleich - bis hin zu den Schaltflaechen.</p>
     */
    private GridPane neuesGitter() {
        GridPane gitter = new GridPane();
        gitter.setHgap(16);
        gitter.setVgap(8);

        ColumnConstraints erste = new ColumnConstraints();
        erste.setHgrow(Priority.ALWAYS);
        erste.setMinWidth(160);
        gitter.getColumnConstraints().add(erste);

        // Fuer jede weitere Spalte eine Vorgabe, die das Schrumpfen verbietet.
        // Ohne sie nimmt GridPane den Platz dort weg, wo gerade etwas steht.
        int weitere = Math.max(ueberschriften.size(), 1) + (aktionen.isEmpty() ? 0 : 1);
        for (int spalte = 1; spalte < weitere; spalte++) {
            ColumnConstraints vorgabe = new ColumnConstraints();
            vorgabe.setHgrow(Priority.NEVER);
            vorgabe.setMinWidth(Region.USE_PREF_SIZE);
            gitter.getColumnConstraints().add(vorgabe);
        }
        return gitter;
    }
}
