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

        // Der Pfad wird von Anwendungsverzeichnis aufgeloest und landet
        // deshalb unter %LOCALAPPDATA%, nicht neben den Bildern. Ein
        // "rm -rf target/vorschau" trifft ihn also nicht, und die Testdaten
        // haeuften sich von Lauf zu Lauf: in der Teilnehmerliste stand Anna
        // Berger zweimal, dreimal, viermal. Deshalb hier von Hand raeumen -
        // eine Vorschau soll immer dasselbe zeigen.
        Path datenbank = de.gkvtransmitter.util.Anwendungsverzeichnis
                .aufloesen(ziel.resolve("vorschau.db"));
        java.nio.file.Files.deleteIfExists(datenbank);
        System.setProperty("gkv.db.path", ziel.resolve("vorschau.db").toString());
        System.setProperty("gkv.testdaten", "true");

        JavaFxLaufzeit.starten();

        Controller controller = new Controller();
        View sicht = new View(controller, new AbrechnungService(controller.getDatabase()));

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

            // Das laengste Formular der Anwendung, und das einzige, dessen
            // Erklaerungen aus den Segmentdefinitionen stammen statt aus
            // ui-messages.json. Genau hier faellt auf, ob die Info-Zeichen
            // etwas bringen.
            oeffne(szene, "Blaupausen");
            klicke(szene, BlaupausenMaske.ID_NEU);
            schreibe(szene, ziel.resolve("formular-blaupause.png"));

            // Die Bildlaufleiste steht in teilnehmer.png: die Testdaten
            // ergeben mehr Zeilen, als bei 720 Punkten Hoehe hineinpassen.
            // Sie hier vergeblich ueber die Fensterhoehe erzwingen zu wollen,
            // war ein Umweg - ein Schnappschuss nimmt die Groesse der Szene,
            // und die aendert sich nicht, wenn man dem ungezeigten Fenster
            // nachtraeglich eine andere Hoehe gibt. Genug Inhalt tut es auch.
        });

        zeigeSchmalesFenster(controller, ziel);

        System.out.println("Vorschau in " + ziel.toAbsolutePath());
        javafx.application.Platform.exit();
    }

    /** Wie schmal ein Fenster wird, ehe man es nicht mehr zumutet. */
    private static final double SCHMALE_BREITE = 900;
    private static final double SCHMALE_HOEHE = 600;

    /**
     * Dieselben Ansichten in einem kleinen Fenster.
     *
     * <p>Das war der offene Punkt aus Abschnitt G der Uebergabe: „einmal das
     * Fenster klein ziehen und hinsehen". Von Hand ist das nicht
     * nachzustellen - ein Schnappschuss nimmt die Groesse der Szene, und die
     * aendert sich nicht, wenn man dem ungezeigten Fenster nachtraeglich eine
     * andere Hoehe gibt. <b>Eine zweite, kleinere Szene tut es aber.</b></p>
     *
     * <p>Sie braucht eine eigene {@code View}: ein Knoten haengt in genau
     * einer Szene, und die erste ist schon aufgebaut. Der {@code Controller}
     * wird weiterverwendet - Profile und Vorlagen sind geladen, und die
     * Datenbank soll dieselbe sein.</p>
     *
     * <p>Zu sehen ist damit zweierlei: ob die Bildlaufleiste so aussieht wie
     * gestaltet, und ob bei wenig Platz wieder etwas abgeschnitten wird.
     * Genau das war der schlimmste Fund der Vorschau - „Lösc…" neben
     * „Bearbei…".</p>
     */
    private static void zeigeSchmalesFenster(Controller controller, Path ziel) {
        View schmal = new View(controller, new AbrechnungService(controller.getDatabase()));
        JavaFxLaufzeit.aufFxFaden(() -> {
            Scene szene = schmal.createMainScene(SCHMALE_BREITE, SCHMALE_HOEHE);
            javafx.stage.Stage buehne = new javafx.stage.Stage();
            buehne.setScene(szene);
            buehne.setWidth(SCHMALE_BREITE);
            buehne.setHeight(SCHMALE_HOEHE);
            szene.getRoot().applyCss();
            szene.getRoot().layout();

            for (String bereich : new String[]{"Teilnehmer", "Blaupausen"}) {
                oeffne(szene, bereich);
                schreibe(szene, ziel.resolve("schmal-"
                        + bereich.toLowerCase(java.util.Locale.GERMAN) + ".png"));
            }

            oeffne(szene, "Teilnehmer");
            klicke(szene, PersonenMaske.ID_NEU);
            schreibe(szene, ziel.resolve("schmal-formular.png"));
        });
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
