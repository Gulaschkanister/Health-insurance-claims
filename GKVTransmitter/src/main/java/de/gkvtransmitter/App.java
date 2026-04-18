package de.gkvtransmitter;

import java.util.stream.Collectors;

import de.gkvtransmitter.domain.SegmentInfo;
import de.gkvtransmitter.presentation.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
                try {
                        // Einstieg in die fachliche Initialisierung:
                        // Controller -> ApplicationBootstrap -> JsonParserFactory -> GlobalDefinitions.
                        // Breakpoint hier setzen, dann mit Step Into bis zur Registrierung laufen.
                        controller = new Controller();

                        int loadedProfiles = controller.getGlobalDefinitions().getProfileCollection().size();
                        String loadedTypes = controller.getGlobalDefinitions().getProfileCollection().keySet().stream()
                                        .map(Enum::name)
                                        .sorted()
                                        .collect(Collectors.joining(", "));

                                                int loadedInvoices = controller.getGlobalDefinitions().getInvoiceTemplateCollection().size();
                        String loadedInvoiceFiles = controller.getGlobalDefinitions().getInvoiceTemplateCollection().values().stream()
                                        .map(invoice -> invoice.getSourceName() + " [" +
                                             String.join(", ",
                                                 invoice.getSegments().stream()
                                                        .map(SegmentInfo::getMessageType)
                                                        .filter(java.util.Objects::nonNull)
                                                        .map(Enum::name)
                                                        .distinct()
                                                        .toList()) + "]")
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
                } catch (RuntimeException e) {
                        System.err.println("Fehler beim App-Start: " + e.getMessage());
                        if (e.getCause() != null) {
                                System.err.println("Ursache: " + e.getCause().getMessage());
                        }
                        showErrorDialog("App konnte nicht starten", e.getMessage());
                        Platform.exit();
                } catch (Exception e) {
                        System.err.println("Unerwarteter Fehler: " + e.getMessage());
                        if (e.getCause() != null) {
                                System.err.println("Ursache: " + e.getCause().getMessage());
                        }
                        showErrorDialog("Unerwarteter Fehler", e.getMessage());
                        Platform.exit();
                }
        }

        private void showErrorDialog(String title, String message) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("GKVTransmitter - Fehler");
                alert.setHeaderText(title);
                alert.setContentText(message);
                alert.showAndWait();
        }

        public static void main(String[] args) {
                launch(args);
        }
}
