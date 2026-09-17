package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import de.gkvtransmitter.anmeldung.Registrierungsblatt;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Das Registrierungsblatt: alle Angaben fuer die Anmeldung auf einem Blatt.
 *
 * <p>Der Punkt, an dem sich die Betriebsdaten zum ersten Mal auszahlen. Was
 * dort und im Dienstleisterprofil einmal erfasst wurde, steht hier
 * zusammengestellt - statt in vier Masken verteilt, aus denen es jemand von
 * Hand abschreibt.</p>
 *
 * <p><b>Es ist kein Antrag.</b> Anlage 1, Abschnitt 2 Absatz 1 gibt kein
 * Formular vor, sondern verlangt eine Abstimmung zwischen Absender und
 * Empfaenger; die Formulare der ARGE·IK, eines Trust Centers und der
 * Annahmestelle liegen dem Projekt nicht vor. Ein Blatt, das sich als
 * amtliches Formular ausgaebe, waere schlimmer als keines - siehe
 * {@link Registrierungsblatt}.</p>
 *
 * <p><b>Abgelegt statt gedruckt.</b> Ein Druckauftrag aus der Anwendung heraus
 * setzt einen eingerichteten Drucker voraus und laesst sich weder unter WSL
 * noch im CI-Laeufer pruefen. Eine Textdatei laesst sich oeffnen, ausdrucken,
 * anhaengen und weiterreichen - und der Test kann sie lesen.</p>
 */
public class RegistrierungsblattMaske {

    /** Kennung des Textfeldes mit dem Blatt. */
    public static final String ID_BLATT = "registrierung-blatt";
    /** Kennung der Zeile, die die Luecken zaehlt. */
    public static final String ID_LUECKEN = "registrierung-luecken";
    /** Kennung der Schaltflaeche, die das Blatt ablegt. */
    public static final String ID_ABLEGEN = "registrierung-ablegen";

    /** Name der abgelegten Datei. */
    static final String DATEINAME = "Registrierungsblatt.txt";

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final DataRepository datenbank;
    private final Supplier<Path> ablageordner;

    public RegistrierungsblattMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
            DataRepository datenbank, Supplier<Path> ablageordner) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
        this.ablageordner = Objects.requireNonNull(ablageordner, "ablageordner must not be null");
    }

    /** Die Maske. */
    public Region maske() {
        Registrierungsblatt blatt = blatt();

        Label titel = bausteine.createLabel(texte.get("registration.title"));
        titel.getStyleClass().add("masken-titel");
        Label erklaerung = bausteine.createLabel(texte.get("registration.subtitle"));
        erklaerung.getStyleClass().add("feld-hinweis");
        erklaerung.setWrapText(true);

        TextArea anzeige = new TextArea(blatt.alsText());
        anzeige.setId(ID_BLATT);
        // Nur zu lesen: Was hier steht, wird in den Stammdaten geaendert und
        // nicht im Blatt. Ein beschreibbares Feld liesse Aenderungen zu, die
        // beim naechsten Aufbau spurlos verschwinden.
        anzeige.setEditable(false);
        anzeige.setPrefRowCount(24);
        anzeige.getStyleClass().add("blatt");

        Button ablegen = bausteine.createButton(texte.get("registration.save"));
        ablegen.setId(ID_ABLEGEN);
        ablegen.getStyleClass().add("schaltflaeche-haupt");
        ablegen.setOnAction(ereignis -> lege(blatt));

        VBox wurzel = new VBox(14);
        wurzel.getStyleClass().add("maske");
        wurzel.getChildren().addAll(new VBox(4, titel, erklaerung), luecken(blatt), anzeige,
                new HBox(10, ablegen));
        return wurzel;
    }

    private Registrierungsblatt blatt() {
        return new Registrierungsblatt(datenbank.ladeBetriebsdaten(),
                datenbank.getAllServiceProviders(), LocalDate.now());
    }

    /**
     * Die Zeile ueber dem Blatt: wie viele Angaben noch fehlen.
     *
     * <p>Sie steht <b>ueber</b> dem Blatt und nicht darunter. Der Abschnitt "4
     * Was noch fehlt" ist die letzte Zeile eines langen Textes, und wer das
     * Blatt ablegt, ohne zu blaettern, sieht ihn nie.</p>
     */
    private Region luecken(Registrierungsblatt blatt) {
        List<String> fehlend = blatt.fehlendeAngaben();
        String text;
        if (fehlend.isEmpty()) {
            text = texte.get("registration.complete");
        } else if (fehlend.size() == 1) {
            // Einzahl und Mehrzahl getrennt - "1 Angaben fehlen" ist im
            // Projekt schon zweimal vorgekommen.
            text = texte.get("registration.missingOne");
        } else {
            text = String.format(texte.get("registration.missing"), fehlend.size());
        }
        Label zeile = bausteine.createLabel(text);
        zeile.setId(ID_LUECKEN);
        zeile.getStyleClass().add(fehlend.isEmpty() ? "feld-hinweis" : "feld-beschriftung");
        zeile.setWrapText(true);
        return zeile;
    }

    private void lege(Registrierungsblatt blatt) {
        Path ziel = ablageordner.get().resolve(DATEINAME);
        try {
            Files.createDirectories(ziel.getParent());
            Files.writeString(ziel, blatt.alsText(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            meldungen.fehler(texte.get("registration.saveFailed") + e.getMessage());
            return;
        }
        // Mit dem Pfad: Ein "Gespeichert" ohne Ort laesst jemanden suchen.
        meldungen.erfolg(String.format(texte.get("registration.saved"), ziel));
    }
}
