package de.gkvtransmitter.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.wartung.Faelligkeit;
import de.gkvtransmitter.wartung.Unterlagenstand;
import de.gkvtransmitter.wartung.Wartungskalender;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Was wann faellig ist.
 *
 * <p><b>Damit schrumpft die Wartung auf eine Aufgabe im Jahr:</b> das
 * Zertifikat erneuern. Alles andere meldet sich selbst - und das ist die
 * Bedingung, unter der der eigene Uebermittlungsweg ueberhaupt sinnvoll ist.
 * Die Ersparnis gegenueber einer Abrechnungsstelle liegt bei vierzig
 * Kursterminen im Jahr zwischen fuenfzig und zweihundertvierzig Euro; kostet
 * die Pflege mehr als ein bis drei Stunden, ist der eigene Weg teurer.</p>
 *
 * <p>Die Zeile oben <b>schweigt</b>, solange nichts innerhalb von sechs Wochen
 * ansteht. Eine Meldung, die bei jedem Start erscheint, wird nach der dritten
 * Woche nicht mehr gelesen.</p>
 */
public class WartungsMaske {

    /** Kennung der Zeile, die sagt, ob etwas ansteht. */
    public static final String ID_STAND = "wartung-stand";
    /** Kennung der Liste der Faelligkeiten. */
    public static final String ID_LISTE = "wartung-liste";

    private static final DateTimeFormatter TAG = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final DataRepository datenbank;
    private final LocalDate heute;

    public WartungsMaske(UiFactory bausteine, AppMessages texte, DataRepository datenbank, LocalDate heute) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
        this.heute = Objects.requireNonNull(heute, "heute must not be null");
    }

    /** Der Kalender zum Zeitpunkt des Aufrufs. */
    public Wartungskalender kalender() {
        return new Wartungskalender(datenbank.ladeBetriebsdaten(), Unterlagenstand.lade());
    }

    /** Die Seite. */
    public Region maske() {
        Wartungskalender kalender = kalender();

        Label titel = bausteine.createLabel(texte.get("maintenance.title"));
        titel.getStyleClass().add("masken-titel");
        Label erklaerung = bausteine.createLabel(texte.get("maintenance.subtitle"));
        erklaerung.getStyleClass().add("feld-hinweis");
        erklaerung.setWrapText(true);

        VBox wurzel = new VBox(16);
        wurzel.getStyleClass().add("maske");
        wurzel.getChildren().addAll(new VBox(4, titel, erklaerung), stand(kalender), liste(kalender));
        return wurzel;
    }

    private Region stand(Wartungskalender kalender) {
        List<Faelligkeit> anstehend = kalender.anstehende(heute);
        String text;
        if (anstehend.isEmpty()) {
            text = String.format(texte.get("maintenance.quiet"), Wartungskalender.VORWARNUNG_WOCHEN);
        } else if (anstehend.size() == 1) {
            // Einzahl und Mehrzahl getrennt - "1 Faelligkeiten stehen an" ist
            // im Projekt schon dreimal vorgekommen.
            text = String.format(texte.get("maintenance.dueOne"), anstehend.get(0).was());
        } else {
            text = String.format(texte.get("maintenance.due"), anstehend.size());
        }
        Label zeile = bausteine.createLabel(text);
        zeile.setId(ID_STAND);
        zeile.getStyleClass().add(anstehend.isEmpty() ? "feld-hinweis" : "feld-beschriftung");
        zeile.setWrapText(true);
        return zeile;
    }

    private Region liste(Wartungskalender kalender) {
        VBox liste = new VBox(8);
        liste.setId(ID_LISTE);
        for (Faelligkeit faelligkeit : kalender.alle(heute)) {
            liste.getChildren().add(zeile(faelligkeit));
        }
        return liste;
    }

    private Region zeile(Faelligkeit faelligkeit) {
        Label kopfzeile = bausteine.createLabel(faelligkeit.was());
        kopfzeile.getStyleClass().add("feld-beschriftung");
        kopfzeile.setWrapText(true);

        Label unterzeile = bausteine.createLabel(faelligkeit.rhythmus() + "  ·  " + faelligkeit.woher());
        unterzeile.getStyleClass().add("feld-hinweis");
        unterzeile.setWrapText(true);

        VBox texteZeile = new VBox(2, kopfzeile, unterzeile);
        HBox.setHgrow(texteZeile, Priority.ALWAYS);

        Label termin = bausteine.createLabel(termin(faelligkeit));
        termin.getStyleClass().add(faelligkeit.ueberfaellig(heute) ? "feld-fehler" : "feld-hinweis");
        // Nicht abschneiden. Im ersten Bild stand rechts "kein Dat..." - die
        // linke Spalte waechst mit dem Text und nahm sich den Platz. Ein
        // abgeschnittener Termin ist schlimmer als eine schmalere Zeile.
        termin.setMinWidth(Region.USE_PREF_SIZE);

        HBox zeile = new HBox(8, texteZeile, termin);
        zeile.getStyleClass().add("karte");
        return zeile;
    }

    /**
     * Der Termin - und wie weit er weg ist.
     *
     * <p>Ein Datum allein beantwortet die Frage nicht, die jemand hat:
     * "muss ich jetzt etwas tun?" Deshalb steht daneben, in wie vielen Tagen
     * es soweit ist.</p>
     */
    private String termin(Faelligkeit faelligkeit) {
        if (!faelligkeit.hatDatum()) {
            return texte.get("maintenance.noDate");
        }
        String datum = faelligkeit.faelligAm().format(TAG);
        long tage = faelligkeit.tageBis(heute);
        if (tage < 0) {
            return String.format(texte.get("maintenance.overdue"), datum, -tage);
        }
        if (tage == 0) {
            return String.format(texte.get("maintenance.today"), datum);
        }
        return String.format(texte.get("maintenance.inDays"), datum, tage);
    }
}
