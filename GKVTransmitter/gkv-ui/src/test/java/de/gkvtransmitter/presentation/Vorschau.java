package de.gkvtransmitter.presentation;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import de.gkvtransmitter.application.AbrechnungService;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/**
 * Zeichnet die Oberflaeche in eine PNG-Datei, ohne ein Fenster zu oeffnen.
 *
 * <p><b>Kein Test</b>, sondern ein Werkzeug. Tests sagen, ob etwas
 * <em>funktioniert</em>; ob es <em>aussieht</em> wie gedacht, sagt kein Test.
 * Ein Blick auf das gezeichnete Bild hat in diesem Projekt schon mehrfach
 * Fehler gefunden, die keine Behauptung gefunden haette - eine
 * Entwicklerbegruendung, die als Feldhilfe im Formular stand, ein Platzhalter,
 * der seiner eigenen Erklaerung widersprach.</p>
 *
 * <p>Von Hand aufzurufen, das Ziel als erstes Argument:</p>
 *
 * <pre>
 * mvn -q -pl gkv-ui exec:java \
 *     -Dexec.classpathScope=test \
 *     -Dexec.mainClass=de.gkvtransmitter.presentation.Vorschau \
 *     -Dexec.args="ziel/verzeichnis"
 * </pre>
 *
 * <p>Die Datenbank liegt dabei in einer eigenen Datei unter dem Zielverzeichnis
 * und die Testdaten werden angelegt - die Vorschau soll gefuellte Listen
 * zeigen, nicht die Hinweise auf leere.</p>
 */
public final class Vorschau {

    private static final double BREITE = 1100;
    private static final double HOEHE = 720;

    private Vorschau() {
    }

    public static void main(String[] args) throws Exception {
        Path ziel = Path.of(args.length > 0 ? args[0] : "target/vorschau");
        java.nio.file.Files.createDirectories(ziel);

        System.setProperty("gkv.db.path", ziel.resolve("vorschau.db").toString());
        System.setProperty("gkv.testdaten", "true");

        JavaFxLaufzeit.starten();

        Controller controller = new Controller();
        View sicht = new View(controller, new AbrechnungService());

        JavaFxLaufzeit.aufFxFaden(() -> {
            Scene szene = sicht.createMainScene(BREITE, HOEHE);
            // Die Szene braucht ein Fenster, sonst bleibt sie ungezeichnet.
            // Es wird nicht gezeigt, nur aufgebaut.
            javafx.stage.Stage buehne = new javafx.stage.Stage();
            buehne.setScene(szene);
            buehne.setWidth(BREITE);
            buehne.setHeight(HOEHE);

            szene.getRoot().applyCss();
            szene.getRoot().layout();

            // Die Testdaten legt "seedIfEmpty" ueber Platform.runLater an, also
            // erst nach diesem Block. Fuer die Vorschau muessen sie schon da
            // sein - sonst zeigt sie ueberall nur den Hinweis auf leere Listen.
            oeffne(szene, "Testdaten anlegen");

            for (String bereich : new String[]{"Abrechnung", "Teilnehmer", "Gruppen", "Blaupausen"}) {
                oeffne(szene, bereich);
                schreibe(szene, ziel.resolve(bereich.toLowerCase(java.util.Locale.GERMAN) + ".png"));
            }

            // Ein Formular dazu: es ist laenger als das Fenster und zeigt
            // deshalb als einzige Ansicht die Bildlaufleiste.
            oeffne(szene, "Teilnehmer");
            klicke(szene, PersonenMaske.ID_NEU);
            schreibe(szene, ziel.resolve("formular-teilnehmer.png"));

            oeffne(szene, "Gruppen");
            klicke(szene, GruppenMaske.ID_NEU);
            schreibe(szene, ziel.resolve("formular-gruppe.png"));

            // NOCH OFFEN: die Bildlaufleiste bekommt die Vorschau nicht zu
            // Gesicht. Ein Schnappschuss nimmt die Groesse der Szene, und die
            // aendert sich nicht, wenn man dem Fenster nachtraeglich eine
            // andere Hoehe gibt, ohne es zu zeigen. Wer die Leiste pruefen
            // will, baut hier eine zweite, kleinere Szene auf - oder sieht
            // sich die Anwendung an.
        });

        System.out.println("Vorschau in " + ziel.toAbsolutePath());
        javafx.application.Platform.exit();
    }

    private static void oeffne(Scene szene, String bereich) {
        javafx.scene.Node eintrag = szene.getRoot().lookup(
                "#" + Hauptfenster.kennung(bereich));
        if (eintrag instanceof javafx.scene.control.ToggleButton knopf) {
            knopf.fire();
        }
        szene.getRoot().applyCss();
        szene.getRoot().layout();
    }

    /** Drueckt eine Schaltflaeche anhand ihrer Kennung. */
    private static void klicke(Scene szene, String kennung) {
        javafx.scene.Node knoten = szene.getRoot().lookup("#" + kennung);
        if (knoten instanceof javafx.scene.control.ButtonBase knopf) {
            knopf.fire();
        } else {
            System.err.println("Nicht gefunden: #" + kennung);
        }
        szene.getRoot().applyCss();
        szene.getRoot().layout();
    }

    private static void schreibe(Scene szene, Path datei) {
        try {
            WritableImage bild = szene.snapshot(null);
            ImageIO.write(nachAwt(bild), "png", datei.toFile());
            System.out.println("  " + datei.getFileName() + "  "
                    + (int) bild.getWidth() + "x" + (int) bild.getHeight());
        } catch (Exception e) {
            System.err.println("Konnte " + datei + " nicht schreiben: " + e);
        }
    }

    /**
     * Uebertraegt ein JavaFX-Bild Punkt fuer Punkt in ein AWT-Bild.
     *
     * <p>Dafuer gaebe es {@code SwingFXUtils}, das steckt aber in
     * {@code javafx-swing}. Eine Abhaengigkeit fuer ein Werkzeug aufzunehmen,
     * das nur von Hand laeuft, waere zu viel des Guten - diese zehn Zeilen
     * kosten weniger.</p>
     */
    private static BufferedImage nachAwt(WritableImage bild) {
        int breite = (int) bild.getWidth();
        int hoehe = (int) bild.getHeight();
        BufferedImage awt = new BufferedImage(breite, hoehe, BufferedImage.TYPE_INT_ARGB);
        PixelReader leser = bild.getPixelReader();
        for (int y = 0; y < hoehe; y++) {
            for (int x = 0; x < breite; x++) {
                awt.setRGB(x, y, leser.getArgb(x, y));
            }
        }
        return awt;
    }
}
