package de.gkvtransmitter.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.dta.DtaFactory;
import de.gkvtransmitter.wartung.Unterlage;
import de.gkvtransmitter.wartung.Unterlagenstand;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Auf welchem Stand die Anwendung steht.
 *
 * <p><b>Der Grund steht im Projekt selbst:</b> Die verbindlichen Anlagen unter
 * {@code Information/} waren am 07.09.2026 vom Juli - zwei Monate ueberholt,
 * und es war niemandem aufgefallen. Veralten faellt nicht auf, solange nichts
 * es zeigt.</p>
 *
 * <p>Oben steht, womit die Anwendung <em>tatsaechlich sendet</em>, darunter,
 * welche Fassungen vorliegen und ab wann sie gelten. Beides nebeneinander,
 * weil erst der Vergleich etwas aussagt.</p>
 */
public class UnterlagenMaske {

    /** Kennung der Zeile mit der Version, mit der gesendet wird. */
    public static final String ID_SENDEVERSION = "unterlagen-sendeversion";
    /** Kennung der Zeile mit dem naechsten Termin. */
    public static final String ID_TERMIN = "unterlagen-termin";
    /** Kennung der Liste. */
    public static final String ID_LISTE = "unterlagen-liste";

    private static final DateTimeFormatter TAG = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Unterlagenstand stand;
    private final LocalDate heute;

    public UnterlagenMaske(UiFactory bausteine, AppMessages texte, Unterlagenstand stand, LocalDate heute) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.stand = Objects.requireNonNull(stand, "stand must not be null");
        this.heute = Objects.requireNonNull(heute, "heute must not be null");
    }

    /** Die Uebersicht. */
    public Region maske() {
        VBox wurzel = new VBox(16);
        wurzel.getStyleClass().add("maske");
        wurzel.getChildren().addAll(kopf(), sendeversion(), termin(), liste());
        if (stand.quelle() != null) {
            Label quelle = bausteine.createLabel(
                    String.format(texte.get("documents.source"), stand.quelle()));
            quelle.getStyleClass().add("feld-hinweis");
            quelle.setWrapText(true);
            wurzel.getChildren().add(quelle);
        }
        return wurzel;
    }

    private Region kopf() {
        Label titel = bausteine.createLabel(texte.get("documents.title"));
        titel.getStyleClass().add("masken-titel");
        Label erklaerung = bausteine.createLabel(texte.get("documents.subtitle"));
        erklaerung.getStyleClass().add("feld-hinweis");
        erklaerung.setWrapText(true);
        return new VBox(4, titel, erklaerung);
    }

    /**
     * Womit die Anwendung sendet.
     *
     * <p>Aus {@link DtaFactory#NACHRICHTENVERSION} und nicht aus der Liste:
     * Was hier steht, soll die Wahrheit ueber den Quelltext sagen und nicht
     * ueber eine gepflegte Datei. Gingen die beiden auseinander, waere genau
     * das der Befund.</p>
     */
    private Region sendeversion() {
        Label zeile = bausteine.createLabel(String.format(texte.get("documents.sending"),
                DtaFactory.NACHRICHTENVERSION, DtaFactory.ZEICHENSATZ));
        zeile.setId(ID_SENDEVERSION);
        zeile.getStyleClass().add("feld-beschriftung");
        zeile.setWrapText(true);
        return zeile;
    }

    /**
     * Der naechste Termin - oder die Meldung, dass keiner ansteht.
     *
     * <p>Abgelaufene Fassungen zuerst: Das ist der Fall, den niemand bemerkt,
     * weil die Anwendung weiter sendet und erst die Kasse zurueckweist.</p>
     */
    private Region termin() {
        List<Unterlage> abgelaufen = stand.abgelaufene(heute);
        String text;
        String stil;
        if (!abgelaufen.isEmpty()) {
            Unterlage erste = abgelaufen.get(0);
            text = String.format(texte.get("documents.expired"),
                    erste.titel(), erste.version(), erste.gueltigBis().format(TAG));
            stil = "feld-fehler";
        } else {
            text = stand.naechsterTermin(heute)
                    .map(naechste -> String.format(texte.get("documents.due"),
                            naechste.titel(), naechste.version(), naechste.anzuwendenAb().format(TAG)))
                    .orElse(texte.get("documents.noDue"));
            stil = stand.naechsterTermin(heute).isPresent() ? "feld-beschriftung" : "feld-hinweis";
        }
        Label zeile = bausteine.createLabel(text);
        zeile.setId(ID_TERMIN);
        zeile.getStyleClass().add(stil);
        zeile.setWrapText(true);
        return zeile;
    }

    private Region liste() {
        VBox liste = new VBox(8);
        liste.setId(ID_LISTE);
        if (stand.alle().isEmpty()) {
            Label leer = bausteine.createLabel(texte.get("documents.empty"));
            leer.getStyleClass().add("feld-hinweis");
            liste.getChildren().add(leer);
            return liste;
        }
        for (Unterlage unterlage : stand.alle()) {
            liste.getChildren().add(zeile(unterlage));
        }
        return liste;
    }

    private Region zeile(Unterlage unterlage) {
        Label kopfzeile = bausteine.createLabel(
                unterlage.titel() + "  ·  " + texte.get("documents.version") + " " + unterlage.version());
        kopfzeile.getStyleClass().add("feld-beschriftung");
        kopfzeile.setWrapText(true);

        Label unterzeile = bausteine.createLabel(beschreibung(unterlage));
        unterzeile.getStyleClass().add("feld-hinweis");
        unterzeile.setWrapText(true);

        VBox texteZeile = new VBox(2, kopfzeile, unterzeile);
        HBox.setHgrow(texteZeile, Priority.ALWAYS);

        Label zustand = bausteine.createLabel(zustandVon(unterlage));
        zustand.getStyleClass().add(unterlage.abgelaufen(heute) ? "feld-fehler" : "feld-hinweis");

        HBox zeile = new HBox(8, texteZeile, zustand);
        zeile.getStyleClass().add("karte");
        return zeile;
    }

    private String beschreibung(Unterlage unterlage) {
        StringBuilder text = new StringBuilder();
        if (unterlage.stand() != null) {
            text.append(texte.get("documents.asOf")).append(' ')
                    .append(unterlage.stand().format(TAG)).append("  ·  ");
        }
        if (unterlage.anzuwendenAb() != null) {
            text.append(texte.get("documents.from")).append(' ')
                    .append(unterlage.anzuwendenAb().format(TAG));
        }
        if (unterlage.gueltigBis() != null) {
            text.append("  ·  ").append(texte.get("documents.until")).append(' ')
                    .append(unterlage.gueltigBis().format(TAG));
        }
        if (unterlage.datei() != null) {
            text.append(System.lineSeparator()).append(unterlage.datei());
        }
        if (unterlage.bemerkung() != null) {
            text.append(System.lineSeparator()).append(unterlage.bemerkung());
        }
        return text.toString();
    }

    private String zustandVon(Unterlage unterlage) {
        if (unterlage.abgelaufen(heute)) {
            return texte.get("documents.stateExpired");
        }
        if (unterlage.stehtBevor(heute)) {
            return texte.get("documents.stateUpcoming");
        }
        return texte.get("documents.stateCurrent");
    }
}
