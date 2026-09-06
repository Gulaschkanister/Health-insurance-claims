package de.gkvtransmitter;


import de.gkvtransmitter.application.AbrechnungService;
import de.gkvtransmitter.presentation.Controller;
import de.gkvtransmitter.presentation.View;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Einstiegspunkt der JavaFX-Anwendung.
 *
 * Initialisiert den fachlichen Controller und zeigt die Hauptszene. Der
 * Ladestatus entsteht seit dem 05.09.2026 in der View, nicht mehr hier -
 * er gehoert dorthin, wo auch die Texte liegen.
 */
public class App extends Application {
    // Ueber diese Referenz bleibt der initialisierte Fachkontext waehrend der
    // gesamten Laufzeit erreichbar und im Debugger inspizierbar.

    /**
     * Die Groesse, in der das Fenster aufgeht.
     *
     * <p>Sie stand bis zum 06.09.2026 auf 900x600 und stammte aus einer Zeit,
     * in der die Oberflaeche aus einer Menueleiste bestand. Inzwischen hat sie
     * eine Seitenleiste von 224 Punkten und Listen mit vier Spalten und zwei
     * Schaltflaechen je Zeile — und bei 900x600 lag <b>„Löschen" ausserhalb des
     * Fensters</b>. Erreichbar war es (die Ansicht rollt quer), aber wer eine
     * Anwendung startet, soll nicht erst rollen muessen, um zu sehen, was sie
     * kann.</p>
     *
     * <p>Gemessen, nicht geschaetzt: die Blaupausenliste ist die breiteste
     * Ansicht, und sie passt bei 1100 Punkten vollstaendig ins Bild.</p>
     */
    private static final double STARTBREITE = 1180;
    private static final double STARTHOEHE = 760;

    /**
     * Wie klein sich das Fenster ziehen laesst.
     *
     * <p>Darunter faengt die Anwendung an, sich selbst zu verdecken. Die
     * Bildlaufleisten fangen zwar auf, was nicht mehr passt — aber ein Fenster,
     * in dem man rollen muss, um die Kopfzeile zu sehen, ist keines mehr. Das
     * Betriebssystem setzt die Grenze durch; wir muessen sie nur nennen.</p>
     */
    private static final double MINDESTBREITE = 900;
    private static final double MINDESTHOEHE = 600;

    private Controller controller;
    private View view;

    @Override
    public void start(Stage stage) {

        try {
            // Einstieg in die fachliche Initialisierung:
            // Controller -> ApplicationBootstrap -> JsonParserFactory -> GlobalDefinitions.
            // Breakpoint hier setzen, dann mit Step Into bis zur Registrierung laufen.
            controller = new Controller();
            view = new View(controller, new AbrechnungService(controller.getDatabase()));

            // Der Ladestatus wird nicht mehr hier zusammengebaut. Bis zum
            // 05.09.2026 entstand an dieser Stelle eine Aufzaehlung aller
            // JSON-Dateinamen samt Nachrichtentypen - der laengste Text im
            // Programm, der in der Statuszeile abgeschnitten wurde. Er gehoert
            // ohnehin dorthin, wo auch die Texte liegen: in die View.
            Scene scene = view.createMainScene(STARTBREITE, STARTHOEHE);
            // Gebunden statt gesetzt: die Leiste nennt den offenen Bereich,
            // und der wechselt. Siehe Hauptfenster.fenstertitel - warum die
            // Titelleiste die von Windows bleibt, steht dort.
            stage.titleProperty().bind(view.fenstertitel());
            stage.getIcons().addAll(Programmsymbol.alle());
            stage.setScene(scene);
            stage.setMinWidth(MINDESTBREITE);
            stage.setMinHeight(MINDESTHOEHE);
            stage.show();
        } catch (RuntimeException e) {
            scheitern("App konnte nicht starten", e);
        } catch (Exception e) {
            scheitern("Unerwarteter Fehler", e);
        }
    }

    /**
     * Meldet einen Fehler beim Start und beendet die Anwendung.
     *
     * <p>Hier steht bewusst ein Dialogfenster, obwohl es im laufenden Betrieb
     * keine mehr gibt: Die Meldungsecke haengt in der Hauptszene, und wenn der
     * Start scheitert, gibt es die noch gar nicht. Eine Meldung dorthin waere
     * unsichtbar.</p>
     *
     * <p>Zuvor lief dieser Zweig ueber {@code view.showErrorDialog(...)}. Wenn
     * aber schon der {@code Controller} scheiterte - der haeufigste Fall, etwa
     * bei unlesbaren JSON-Profilen -, war {@code view} noch {@code null}. Statt
     * der Ursache erschien dann eine {@code NullPointerException}.</p>
     */
    private static void scheitern(String titel, Exception fehler) {
        System.err.println(titel + ": " + fehler.getMessage());
        if (fehler.getCause() != null) {
            System.err.println("Ursache: " + fehler.getCause().getMessage());
        }
        Alert meldung = new Alert(AlertType.ERROR);
        meldung.setTitle(titel);
        meldung.setHeaderText(titel);
        meldung.setContentText(fehler.getMessage());
        meldung.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
