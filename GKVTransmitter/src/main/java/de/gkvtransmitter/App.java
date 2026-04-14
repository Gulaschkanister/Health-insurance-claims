package de.gkvtransmitter;

import java.util.stream.Collectors;

import de.gkvtransmitter.domain.DtaMessage;
import de.gkvtransmitter.presentation.Controller;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Startet die JavaFX-Anwendung und haengt spaeter die UI-Wizard-Schicht an den
 * fachlichen Kern an.
 */
public class App extends Application {
    // Ueber diese Referenz bleibt der initialisierte Fachkontext waehrend der
    // gesamten Laufzeit erreichbar und im Debugger inspizierbar.
    @SuppressWarnings("unused")
    private Controller controller;

    @Override
    public void start(Stage stage) {
        // Einstieg in die fachliche Initialisierung:
        // Controller -> ApplicationBootstrap -> JsonParserFactory -> GlobalDefinitions.
        // Breakpoint hier setzen, dann mit Step Into bis zur Registrierung laufen.
        controller = new Controller();

        int loadedProfiles = controller.getGlobalDefinitions().getProfileCollection().size();
        String loadedTypes = controller.getGlobalDefinitions().getProfileCollection().keySet().stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(", "));

        int loadedInvoices = controller.getGlobalDefinitions().getInvoiceCollection().size();
        String loadedInvoiceFiles = controller.getGlobalDefinitions().getInvoiceCollection().stream()
            .map(DtaMessage::getSourceName)
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

        Label label = new Label(statusText);
        StackPane root = new StackPane(label);

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("GKVTransmitter");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
