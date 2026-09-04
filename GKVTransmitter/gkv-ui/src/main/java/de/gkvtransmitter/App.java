package de.gkvtransmitter;

import java.util.stream.Collectors;

import de.gkvtransmitter.application.AbrechnungService;
import de.gkvtransmitter.model.segment.SegmentInfo;
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
 * Initialisiert den fachlichen Controller, berechnet einen sichtbaren
 * Lade-Status aus den geladenen Profilen/Rechnungen und zeigt diesen in der
 * Hauptszene an.
 */
public class App extends Application {
    // Ueber diese Referenz bleibt der initialisierte Fachkontext waehrend der
    // gesamten Laufzeit erreichbar und im Debugger inspizierbar.

    private Controller controller;
    private View view;

    @Override
    public void start(Stage stage) {

        try {
            // Einstieg in die fachliche Initialisierung:
            // Controller -> ApplicationBootstrap -> JsonParserFactory -> GlobalDefinitions.
            // Breakpoint hier setzen, dann mit Step Into bis zur Registrierung laufen.
            controller = new Controller();
            view = new View(controller, new AbrechnungService());

            int loadedProfiles = controller.getGlobalDefinitions().getProfileCollection().size();
            String loadedTypes = controller.getGlobalDefinitions().getProfileCollection().keySet().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(", "));

            int loadedInvoices = controller.getGlobalDefinitions().getInvoiceTemplateCollection().size();
            String loadedInvoiceFiles = controller.getGlobalDefinitions().getInvoiceTemplateCollection().values()
                    .stream()
                    .map(invoice -> invoice.getSourceName() + " ["
                            + String.join(", ",
                                    invoice.getSegments().stream()
                                            .map(SegmentInfo::getMessageType)
                                            .filter(java.util.Objects::nonNull)
                                            .map(Enum::name)
                                            .distinct()
                                            .toList())
                            + "]")
                    .sorted()
                    .collect(Collectors.joining(", "));

            // Sichtbare Debug-Hilfe im UI: Profile und echte Rechnungsdateien getrennt.
            String profileStatus = loadedProfiles > 0
                    ? "Profile: " + loadedProfiles + " (" + loadedTypes + ")"
                    : "Profile: 0";
            String invoiceStatus = loadedInvoices > 0
                    ? "Invoices: " + loadedInvoices + " (" + loadedInvoiceFiles + ")"
                    : "Invoices: 0";
            String statusText = "GKVTransmitter geladen - " + profileStatus + " | " + invoiceStatus;

            Scene scene = view.createMainScene(statusText, 900, 600);
            stage.setTitle("GKVTransmitter");
            stage.setScene(scene);
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
