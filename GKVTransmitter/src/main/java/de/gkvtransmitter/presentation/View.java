package de.gkvtransmitter.presentation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.model.segment.SegmentInfo;
import de.gkvtransmitter.model.segment.ValueFieldEntry;
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
        // Invoice Menu
        Menu loadedInvoices = componentFactory.createMenu("Rechnung");
        List<MenuItem> invoiceItems = new ArrayList<>();
        for (String name : controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet()) {
            MenuItem item = componentFactory.createMenuItem(name);
            item.setOnAction(event -> createFormular(name)); // Korrekter EventHandler!
            invoiceItems.add(item);
        }
        loadedInvoices.getItems().addAll(invoiceItems);
        // Patient Menu
        MenuItem newPatient = new MenuItem("Neu");
        newPatient.setOnAction(event -> createPerson());
        MenuItem editPatient = new MenuItem("Bearbeiten");
        editPatient.setOnAction(event -> editPerson());
        Menu patient = componentFactory.createMenu("Patient", newPatient, editPatient);

        // Me Menu
        Menu self = componentFactory.createMenu("Ich", new MenuItem("Bearbeiten"));
        self.setOnAction(event -> editPerson());
        // Everything combined
        MenuBar menuBar = componentFactory.createMenuBar(loadedInvoices, patient, self);
        // TODO: ebenso die
        // Eingabemaske und prüfen, welche Daten noch in den jeweiligen Profilen Fehlen
        // und die db Passwortgeschützt erstellen?
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
                // Überspringe interne Felder
                if (entry.getValue().isInternal()) {
                    continue;
                }
                fieldNodes.add(componentFactory.createBorderPane(
                        componentFactory.createLabel(entry.getKey()),
                        createInputfieldFromTag(entry.getValue().getInputField(), entry.getKey(),
                                entry.getValue().isInternal(),
                                entry.getValue().getFieldJavaType()),
                        null, null, null));
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

    private void editPerson() {
        // TODO: Wait to implement
    }

    private void createPerson() {
        // TODO: Wait to implement
    }

    private void deletePerson() {
        // TODO: Wait to implement
    }

    /**
     * Hier wird entschieden, welcher Typ für das jeweilige Formularfeld verwendet
     * wird
     * 
     * @param inputOption   Eingabe Tag
     * @param directName    Anzeigename der möglich wäre
     * @param javaFieldType JavaType
     * @return Node
     */
    private Node createInputfieldFromTag(InputOption inputOption, String directName, boolean internal,
            String javaFieldType)
            throws NullPointerException {

        if (inputOption == null || internal) {
            return null;
            // throw new NullPointerException(
            // "Eingabefeld kann nicht erstellt werden, da keine Inputoption gewählt wurde"
            // + inputOption);
        }
        switch (inputOption) {
            case InputOption.CODE:
                return componentFactory.createComboBox(false);

            case InputOption.NUMBER_SUGGESTION:
                return componentFactory.createComboBox(true);

            case InputOption.NUMBER:
                return componentFactory.createSpinner(Integer.class, null, inputOption, javaFieldType);

            case InputOption.STRING:
                return componentFactory.createTextField();

            case InputOption.PERCENT:
                return componentFactory.createSpinner(BigDecimal.class, null, inputOption, javaFieldType);

            case InputOption.COST:
                return componentFactory.createSpinner(BigDecimal.class, null, inputOption, javaFieldType);

            case InputOption.BOOLEAN:
                return componentFactory.createCheckBox(directName);

            case InputOption.DATE:
                return componentFactory.createDatePicker();

            default:
                throw new IllegalArgumentException("Unbekannter InputType: " + inputOption);

        }

    }

    /**
     * Zeigt einen blockierenden Fehlerdialog fuer den Benutzer an.
     */
    public void showErrorDialog(String title, String message) {
        Alert alert = componentFactory.createErrorAlert(title, message);
        alert.showAndWait();
    }
}
