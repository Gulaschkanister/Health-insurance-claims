package de.gkvtransmitter.presentation;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import de.gkvtransmitter.dta.Uebermittlungsart;
import de.gkvtransmitter.einstellung.Einstellung;
import de.gkvtransmitter.einstellung.Einstellungen;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Was sich einstellen laesst - und wo es liegt.
 *
 * <p>Die erste Maske, die nichts mit einer Abrechnung zu tun hat. Sie schreibt
 * in die Tabelle {@code einstellung} der Datenbank - erst war es eine eigene
 * Datei daneben, bis Simons Einwand sie ueberfluessig machte: <em>"da kann es
 * auch einfach in der DB gespeichert werden, muessen ja nicht extra Dateien
 * herzaubern."</em></p>
 *
 * <p><b>Jede Aenderung wirkt sofort und ist sofort gespeichert.</b> Kein
 * "Speichern"-Knopf: es gibt hier nichts, was man zusammenhaengend eingibt und
 * am Ende abschickt, und ein Umschalter, der erst nach einem zweiten Klick
 * gilt, ist eine Falle. Scheitert das Schreiben, sagt die Meldungsecke das -
 * die Wahl gilt dann fuer diese Sitzung und nicht darueber hinaus.</p>
 *
 * <p><b>Warum die Uebermittlungsart hier steht</b>, obwohl sie gefaehrlich
 * aussieht: sie stand bis zum 07.09.2026 fest im Quelltext, und genau das war
 * das Problem - jede Datei bezeichnete sich als Erprobungsdatei, ohne dass es
 * jemand sehen konnte. Eine Angabe, die ueber Bezahlung oder Nichtbezahlung
 * entscheidet, gehoert dorthin, wo man sie liest.</p>
 */
public class EinstellungenMaske {

    /** Kennung des Auswahlfelds fuer die Darstellung. */
    public static final String ID_DARSTELLUNG = "einstellung-darstellung";
    /** Kennung des Auswahlfelds fuer die Uebermittlungsart. */
    public static final String ID_UEBERMITTLUNG = "einstellung-uebermittlung";
    /** Kennung der Zeile, die den Ablageort nennt. */
    public static final String ID_ORT = "einstellung-ort";

    /** Wert der Einstellung {@code darstellung} fuer die dunkle Fassung. */
    public static final String DUNKEL = "dunkel";
    /** Wert der Einstellung {@code darstellung} fuer die helle Fassung. */
    public static final String HELL = "hell";

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final Einstellungen einstellungen;
    private final Consumer<String> darstellungAnwenden;

    /**
     * @param darstellungAnwenden was mit der gewaehlten Darstellung geschieht -
     *        die Maske kennt die Szene nicht
     */
    public EinstellungenMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
            Einstellungen einstellungen, Consumer<String> darstellungAnwenden) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.einstellungen = Objects.requireNonNull(einstellungen, "einstellungen must not be null");
        this.darstellungAnwenden =
                Objects.requireNonNull(darstellungAnwenden, "darstellungAnwenden must not be null");
    }

    /** Die Maske. */
    public Region maske() {
        VBox wurzel = new VBox(16);
        wurzel.getStyleClass().add("maske");

        wurzel.getChildren().add(darstellung());
        wurzel.getChildren().add(uebermittlung());
        wurzel.getChildren().add(ablageort());
        return wurzel;
    }

    private Region darstellung() {
        ComboBox<String> auswahl = auswahlfeld(ID_DARSTELLUNG,
                List.of(texte.get("settings.appearance.light"), texte.get("settings.appearance.dark")));
        auswahl.getSelectionModel().select(
                DUNKEL.equals(einstellungen.get(Einstellung.DARSTELLUNG)) ? 1 : 0);

        // Auf den Wert hoeren und nicht auf setOnAction: das feuert nur bei
        // einer Auswahl durch die Bedienung und haengt an einer aufgebauten
        // Darstellung. Ein Test - und jeder andere Weg, der den Wert setzt -
        // liefe daran vorbei, ohne dass etwas fehlschlaegt.
        auswahl.valueProperty().addListener((wo, vorher, nachher) -> {
            String wert = auswahl.getSelectionModel().getSelectedIndex() == 1 ? DUNKEL : HELL;
            // Erst anwenden, dann speichern: die Wirkung ist das, was jemand
            // sehen will, und sie darf nicht davon abhaengen, ob eine Datei
            // beschreibbar ist.
            darstellungAnwenden.accept(wert);
            melde(einstellungen.setze(Einstellung.DARSTELLUNG, wert));
        });

        return karte(texte.get("settings.appearance"), texte.get("settings.appearance.help"), auswahl);
    }

    private Region uebermittlung() {
        List<Uebermittlungsart> arten = List.of(
                Uebermittlungsart.TEST, Uebermittlungsart.ERPROBUNG, Uebermittlungsart.ECHT);
        ComboBox<String> auswahl = auswahlfeld(ID_UEBERMITTLUNG, List.of(
                texte.get("settings.transfer.test"),
                texte.get("settings.transfer.trial"),
                texte.get("settings.transfer.live")));
        Uebermittlungsart gewaehlt =
                Uebermittlungsart.aus(einstellungen.get(Einstellung.UEBERMITTLUNGSART));
        auswahl.getSelectionModel().select(arten.indexOf(gewaehlt));

        auswahl.valueProperty().addListener((wo, vorher, nachher) -> {
            int nummer = Math.max(0, auswahl.getSelectionModel().getSelectedIndex());
            Uebermittlungsart art = arten.get(nummer);
            melde(einstellungen.setze(Einstellung.UEBERMITTLUNGSART, art.alsEinstellung()));
            if (art == Uebermittlungsart.ECHT) {
                // Ein Hinweis und keine Rueckfrage: das Umstellen selbst
                // verschickt nichts. Wer hier steht, soll aber wissen, dass die
                // naechste Abrechnung als Forderung gilt.
                meldungen.hinweis(texte.get("settings.transfer.liveWarning"));
            }
        });

        return karte(texte.get("settings.transfer"), texte.get("settings.transfer.help"), auswahl);
    }

    /**
     * Wo die Einstellungen liegen.
     *
     * <p>Steht hier, weil es die einzige Auskunft ist, die im Ernstfall
     * hilft: wer sichern oder umziehen will, muss wissen, wo die Datenbank
     * liegt - und sie liegt nicht dort, wo das Programm liegt.</p>
     */
    private Region ablageort() {
        Label ort = bausteine.createLabel(einstellungen.ort());
        ort.setId(ID_ORT);
        ort.getStyleClass().add("feld-hinweis");
        ort.setWrapText(true);
        return karte(texte.get("settings.location"), texte.get("settings.location.help"), ort);
    }

    private ComboBox<String> auswahlfeld(String kennung, List<String> werte) {
        ComboBox<String> auswahl = new ComboBox<>();
        auswahl.setId(kennung);
        auswahl.getItems().addAll(werte);
        auswahl.setMaxWidth(Double.MAX_VALUE);
        return auswahl;
    }

    private Region karte(String titel, String erklaerung, javafx.scene.Node bedienelement) {
        Label beschriftung = bausteine.createLabel(titel);
        beschriftung.getStyleClass().add("feld-beschriftung");
        Label hinweis = bausteine.createLabel(erklaerung);
        hinweis.getStyleClass().add("feld-hinweis");
        hinweis.setWrapText(true);

        VBox karte = new VBox(8, beschriftung, bedienelement, hinweis);
        karte.getStyleClass().add("karte");
        return karte;
    }

    /** Sagt Bescheid, wenn die Wahl die Sitzung nicht ueberlebt. */
    private void melde(boolean gespeichert) {
        if (!gespeichert) {
            meldungen.fehler(texte.get("settings.notSaved"));
        }
    }
}
