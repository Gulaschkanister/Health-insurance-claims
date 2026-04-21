package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.gkvtransmitter.domain.DtaMessage;
import de.gkvtransmitter.domain.SegmentInfo;
import de.gkvtransmitter.domain.ValueFieldEntry;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * View-Schicht der Anwendung.
 *
 * Erzeugt die sichtbare Status-Ansicht und delegiert die konkrete Erstellung
 * einzelner JavaFX-Elemente an eine UI-Factory.
 */
public class View {

    private final UiFactory componentFactory;
    private final Controller controller;
    private BorderPane skeleton; // Hauptfenster

    public View(Controller controller) {
        this.controller = controller;
        this.componentFactory = new JavaFxUiFactory();
    }

    /**
     * Baut die Hauptszene mit einem einfachen Status-Text auf.
     */
    public Scene createMainScene(String statusText, double width, double height) {
        Menu loadedInvoices = componentFactory.createMenu("Geladene Rechnungen");
        List<MenuItem> invoiceItems = new java.util.ArrayList<>();
        for (String name : controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet()) {
            MenuItem item = componentFactory.createMenuItem(name);
            item.setOnAction(event -> createFormular(name)); // Korrekter EventHandler!
            invoiceItems.add(item);
        }
        loadedInvoices.getItems().addAll(invoiceItems);
        Menu menu = componentFactory.createMenu("Datei",
                loadedInvoices);
        MenuBar menuBar = componentFactory.createMenuBar(menu);

        // skeleton als Klassenvariable speichern!
        this.skeleton = componentFactory.createBorderPane(
                menuBar, null, null, null, null);

        return componentFactory.createScene(skeleton, width, height);
    }

    private void createFormular(String invoiceName) {

        DtaMessage dtaMessage = controller.getGlobalDefinitions().getInvoiceTemplateCollection().get(invoiceName);
        if (dtaMessage == null) {
            showErrorDialog("Fehler", "Keine Vorlage für die ausgewählte Rechnung gefunden.");
            return;
        }

        List<Node> fieldNodes = new ArrayList<>();
        List<SegmentInfo> segmentInfo = dtaMessage.getSegments();
        for (SegmentInfo info : segmentInfo) {
            Map<String, ValueFieldEntry> valueFields = info.getValueFields();
            for (Map.Entry<String, ValueFieldEntry> entry : valueFields.entrySet()) {
                // TODO: Hier die behandlung der verschiedenen Typen handhaben
                fieldNodes.add(componentFactory.createBorderPane(
                        componentFactory.createLabel(entry.getKey()),
                        componentFactory.createTextField(), null, null, null));
            }
        }

        VBox vbox = new VBox();
        Label title = componentFactory.createLabel(invoiceName);
        GridPane contentGrid = componentFactory.createGridPane(2,
                fieldNodes.toArray(new Node[0]));
        vbox.getChildren().add(title);
        vbox.getChildren().add(contentGrid);

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);

        BorderPane.setMargin(contentGrid, new Insets(0, 30, 0, 30));

        skeleton.setCenter(scrollPane);

    }

    /**
     * Zeigt einen blockierenden Fehlerdialog fuer den Benutzer an.
     */
    public void showErrorDialog(String title, String message) {
        Alert alert = componentFactory.createErrorAlert(title, message);
        alert.showAndWait();
    }
}
