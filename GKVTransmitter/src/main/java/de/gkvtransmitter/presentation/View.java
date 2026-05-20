package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.model.segment.SegmentInfo;
import de.gkvtransmitter.model.segment.ValueFieldEntry;
import de.gkvtransmitter.presentation.builder.MenuBuilder;
import de.gkvtransmitter.presentation.controller.EditFormController;
import de.gkvtransmitter.presentation.populator.PatientFieldPopulator;
import de.gkvtransmitter.presentation.populator.ServiceProviderFieldPopulator;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.FieldValidator;
import de.gkvtransmitter.util.ModifierInstance;
import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import de.gkvtransmitter.util.modifiers.MaxLengthModifier;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * View-Schicht der Anwendung - REFAKTORIERT
 *
 * Nach großem Refactoring jetzt mit:
 * - Fokus auf Szenen-Management und Hauptmenü
 * - Delegation komplexer Logik an spezialisierte Komponenten
 * - Klare Separation of Concerns
 *
 * Delegationen:
 * - Entity-Bearbeitung -> EditFormController
 * - Feld-Populierung -> EntityFieldPopulator
 * - Menü-Erstellung -> MenuBuilder
 */
public class View {

    private final UiFactory componentFactory;
    private final Controller controller;
    private final AppMessages messages;
    private final ObjectMapper objectMapper;
    private final Map<String, List<String>> invoiceCodeOptions;
    private BorderPane skeleton;
    
    private final PatientFieldPopulator patientPopulator;
    private final ServiceProviderFieldPopulator serviceProviderPopulator;

    public View(Controller controller) {
        this.controller = controller;
        this.componentFactory = new JavaFxUiFactory();
        this.messages = new AppMessages("/messages/ui-messages.json");
        this.objectMapper = new ObjectMapper();
        this.invoiceCodeOptions = loadInvoiceCodeOptions();
        this.patientPopulator = new PatientFieldPopulator();
        this.serviceProviderPopulator = new ServiceProviderFieldPopulator();
    }

    public Scene createMainScene(String statusText, double width, double height) {
        MenuBar menuBar = buildMainMenuBar();
        this.skeleton = componentFactory.createBorderPane(menuBar, null, null, null, null);
        return componentFactory.createScene(skeleton, width, height);
    }

    private MenuBar buildMainMenuBar() {
        MenuBuilder menuBuilder = new MenuBuilder(componentFactory, messages);

        Map<String, Runnable> invoiceHandlers = new HashMap<>();
        for (String name : controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet()) {
            invoiceHandlers.put(name, () -> createFormular(name));
        }
        menuBuilder.addAllInvoiceItems(invoiceHandlers);

        menuBuilder.addPatientItem(messages.get("menu.new"), this::createPerson);
        menuBuilder.addPatientItem(messages.get("menu.edit"), this::editPatient);

        menuBuilder.addSelfItem(messages.get("menu.new"), this::createSelfPerson);
        menuBuilder.addSelfItem(messages.get("menu.edit"), this::editServiceProvider);

        return menuBuilder.build();
    }

    private void createFormular(String invoiceName) {
        DtaMessage dtaMessage = controller.getGlobalDefinitions().getInvoiceTemplateCollection().get(invoiceName);
        if (dtaMessage == null) {
            showErrorDialog(messages.get("dialog.error.title"), messages.get("msg.noTemplate"));
            return;
        }

        Map<String, Node> allFieldNodes = new HashMap<>();
        for (SegmentInfo info : dtaMessage.getSegments()) {
            for (Map.Entry<String, ValueFieldEntry> entry : info.getValueFields().entrySet()) {
                if (!entry.getValue().isInternal()) {
                    Node inputField = createInputfieldFromTag(
                            entry.getValue().getInputField(),
                            entry.getKey(),
                            entry.getValue().isInternal(),
                            entry.getValue().getFieldJavaType());
                    allFieldNodes.put(entry.getKey(), inputField);
                }
            }
        }

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        Label title = componentFactory.createLabel(invoiceName);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        vbox.getChildren().add(title);

        List<Node> fieldNodes = new ArrayList<>();
        for (Map.Entry<String, Node> entry : allFieldNodes.entrySet()) {
            fieldNodes.add(componentFactory.createBorderPane(
                    componentFactory.createLabel(entry.getKey()),
                    entry.getValue(),
                    null, null, null));
        }

        GridPane contentGrid = componentFactory.createGridPane(2, fieldNodes.toArray(Node[]::new));
        vbox.getChildren().add(contentGrid);

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        skeleton.setCenter(scrollPane);
    }

    private void editPatient() {
        EditFormController<Patient> editController = new EditFormController<>(
                componentFactory,
                messages,
                patientPopulator,
                () -> controller.getDatabase().getAllPatients(),
                patient -> controller.getDatabase().savePatient(patient),
                patient -> controller.getDatabase().deletePatient(patient),
                fieldName -> createInputFieldFromTagList(fieldName, 
                        TagConfigLoader.loadTagConfig("/tags/person-tags.json").get(fieldName)),
                "Patient",
                fc -> skeleton.setCenter(null)
        );
        skeleton.setCenter(editController.buildEditForm());
    }

    private void editServiceProvider() {
        EditFormController<ServiceProvider> editController = new EditFormController<>(
                componentFactory,
                messages,
                serviceProviderPopulator,
                () -> controller.getDatabase().getAllServiceProviders(),
                sp -> controller.getDatabase().saveServiceProvider(sp),
                sp -> controller.getDatabase().deleteServiceProvider(sp),
                fieldName -> createInputFieldFromTagList(fieldName,
                        TagConfigLoader.loadTagConfig("/tags/person-tags.json").get(fieldName)),
                "ServiceProvider",
                fc -> skeleton.setCenter(null)
        );
        skeleton.setCenter(editController.buildEditForm());
    }

    private void createPerson() {
        showCreatePersonForm(messages.get("title.patient.new"), false);
    }

    private void createSelfPerson() {
        showCreatePersonForm(messages.get("title.self.new"), true);
    }

    private void showCreatePersonForm(String titleText, boolean createServiceProvider) {
        Map<String, TagList> tagConfig = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
        Map<String, Node> inputFields = new HashMap<>();
        List<Node> fieldNodes = new ArrayList<>();

        for (Map.Entry<String, TagList> entry : tagConfig.entrySet()) {
            String fieldName = entry.getKey();
            Node inputField = createInputFieldFromTagList(fieldName, entry.getValue());
            inputFields.put(fieldName, inputField);

            fieldNodes.add(componentFactory.createBorderPane(
                    componentFactory.createLabel(messages.get("field." + fieldName)),
                    inputField,
                    null, null, null));
        }

        Button saveButton = componentFactory.createButton(messages.get("button.save"));
        saveButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        saveButton.setOnAction(event -> savePerson(inputFields, createServiceProvider));

        Button cancelButton = componentFactory.createButton(messages.get("button.cancel"));
        cancelButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        cancelButton.setOnAction(event -> skeleton.setCenter(null));

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(saveButton, cancelButton);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        Label title = componentFactory.createLabel(titleText);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        GridPane contentGrid = componentFactory.createGridPane(2, fieldNodes.toArray(Node[]::new));
        vbox.getChildren().addAll(title, contentGrid, buttonBox);

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        skeleton.setCenter(scrollPane);
    }

    private void savePerson(Map<String, Node> inputFields, boolean saveAsServiceProvider) {
        try {
            String firstname = getFieldText(inputFields.get("firstname"));
            String lastname = getFieldText(inputFields.get("lastname"));
            String street = getFieldText(inputFields.get("street"));
            String country = getFieldText(inputFields.get("country"));
            String housenumber = getFieldText(inputFields.get("housenumber"));
            int plz = Integer.parseInt(getFieldText(inputFields.get("plz")));
            int ik = Integer.parseInt(getFieldText(inputFields.get("ik")));
            LocalDate birthDate = null;
            Node birthNode = inputFields.get("birthDate");
            if (birthNode instanceof DatePicker dp) {
                birthDate = dp.getValue();
            }

            if (saveAsServiceProvider) {
                ServiceProvider sp = new ServiceProvider(firstname, lastname, street, country, housenumber, plz, ik, birthDate);
                controller.getDatabase().saveServiceProvider(sp);
            } else {
                Patient p = new Patient(firstname, lastname, street, country, housenumber, plz, ik, birthDate);
                controller.getDatabase().savePatient(p);
            }

            showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.patientCreated"));
            skeleton.setCenter(null);
        } catch (NumberFormatException e) {
            showErrorDialog(messages.get("dialog.error.title"), messages.get("msg.invalidNumbers"));
        } catch (Exception e) {
            showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
        }
    }

    private Node createInputfieldFromTag(InputOption inputOption, String directName, 
            boolean visible, String javaFieldType) {
        if (inputOption == null) return null;

        return switch (inputOption) {
            case CODE -> createCodeDropdownForInvoiceField(directName);
            case NUMBER_SUGGESTION -> componentFactory.createComboBox(true);
            case NUMBER -> componentFactory.createSpinner(Integer.class, null, inputOption);
            case STRING -> componentFactory.createTextField();
            case PERCENT, COST -> componentFactory.createSpinner(BigDecimal.class, null, inputOption);
            case BOOLEAN -> componentFactory.createCheckBox(directName);
            case DATE -> componentFactory.createDatePicker();
            default -> throw new IllegalArgumentException("Unbekannter InputType: " + inputOption);
        };
    }

    private Node createInputFieldFromTagList(String fieldName, TagList tagList) {
        if (tagList == null) return componentFactory.createTextField();

        InputOption inputOption = tagList.getInputOption();
        return switch (inputOption) {
            case STRING -> {
                TextField tf = componentFactory.createTextField();
                applyMaxLengthModifier(tf, tagList);
                yield tf;
            }
            case NUMBER -> {
                Spinner<Integer> spinner = componentFactory.createSpinner(Integer.class, null, inputOption);
                yield createValidatedSpinnerNode(fieldName, spinner, inputOption, "Integer");
            }
            case PERCENT, COST -> {
                Spinner<BigDecimal> spinner = componentFactory.createSpinner(BigDecimal.class, null, inputOption);
                yield createValidatedSpinnerNode(fieldName, spinner, inputOption, "BigDecimal");
            }
            case CODE -> componentFactory.createComboBox(false);
            case NUMBER_SUGGESTION -> componentFactory.createComboBox(true);
            case DATE, TIME -> componentFactory.createDatePicker();
            default -> componentFactory.createTextField();
        };
    }

    private Node createValidatedSpinnerNode(String fieldName, Spinner<?> spinner, 
            InputOption inputOption, String javaFieldType) {
        Label errorLabel = componentFactory.createLabel("");
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11;");
        errorLabel.setVisible(false);

        VBox box = new VBox(4);
        box.getChildren().addAll(spinner, errorLabel);

        Runnable validate = () -> validateSpinner(spinner, fieldName, errorLabel, inputOption, javaFieldType);

        spinner.valueProperty().addListener((obs, o, n) -> validate.run());
        if (spinner.getEditor() != null) {
            spinner.getEditor().textProperty().addListener((obs, o, n) -> validate.run());
            spinner.getEditor().focusedProperty().addListener((obs, oldF, newF) -> {
                if (!newF) validate.run();
            });
        }

        validate.run();
        return box;
    }

    private void validateSpinner(Spinner<?> spinner, String fieldName, Label errorLabel,
            InputOption inputOption, String javaFieldType) {
        String valText = "";
        try {
            if (spinner.getEditor() != null && !spinner.getEditor().getText().isBlank()) {
                valText = spinner.getEditor().getText();
            } else if (spinner.getValue() != null) {
                valText = String.valueOf(spinner.getValue());
            }
        } catch (Exception ignored) {
        }

        if ("plz".equalsIgnoreCase(fieldName) && !valText.isBlank()) {
            if (!valText.matches("\\d{5}")) {
                errorLabel.setText("PLZ muss 5-stellig sein");
                errorLabel.setVisible(true);
                spinner.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
                return;
            }
        }

        if (spinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intVf) {
            try {
                int min = intVf.getMin();
                int max = intVf.getMax();
                if (!valText.isBlank()) {
                    int cur = Integer.parseInt(valText);
                    if (cur < min || cur > max) {
                        errorLabel.setText(String.format("Wert muss zwischen %d und %d liegen", min, max));
                        errorLabel.setVisible(true);
                        spinner.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
                        return;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        FieldValidator.ValidationResult res = FieldValidator.validate(fieldName, valText, inputOption, javaFieldType);
        if (res == null || res.isValid) {
            errorLabel.setVisible(false);
            spinner.setStyle(null);
        } else {
            errorLabel.setText(res.errorMessage != null ? res.errorMessage : "Ungültiger Wert");
            errorLabel.setVisible(true);
            spinner.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
        }
    }

    private void applyMaxLengthModifier(TextField textField, TagList tagList) {
        for (ModifierInstance modifier : tagList.getModifierList()) {
            if (modifier instanceof MaxLengthModifier mmod) {
                int maxLength = mmod.getMaxLength();
                textField.setTextFormatter(new TextFormatter<>(change -> {
                    if (change.getControlNewText().length() <= maxLength) {
                        return change;
                    }
                    return null;
                }));
            }
        }
    }

    private Node createCodeDropdownForInvoiceField(String fieldName) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(300);

        List<String> options = resolveCodeOptionsForField(fieldName);
        if (!options.isEmpty()) {
            comboBox.getItems().addAll(options);
            comboBox.getSelectionModel().selectFirst();
        } else {
            comboBox.setPromptText("Keine Codes vorhanden");
        }
        return comboBox;
    }

    private List<String> resolveCodeOptionsForField(String fieldName) {
        String normalized = normalizeFieldKey(fieldName);

        if (normalized.contains("rechnungsart")) {
            return invoiceCodeOptions.getOrDefault("rechnungsarten", List.of());
        }
        if (normalized.contains("status") || normalized.contains("summen")) {
            return invoiceCodeOptions.getOrDefault("ges_statuscodes", List.of());
        }
        return List.of();
    }

    private String normalizeFieldKey(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private Map<String, List<String>> loadInvoiceCodeOptions() {
        Map<String, List<String>> options = new LinkedHashMap<>();
        try (InputStream is = getClass().getResourceAsStream("/codes/rechnungsarten.json")) {
            if (is != null) {
                JsonNode root = objectMapper.readTree(is);
                List<String> rechnungsarten = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        String value = node.get("value") != null ? node.get("value").asText() : node.asText();
                        rechnungsarten.add(value);
                    }
                }
                options.put("rechnungsarten", rechnungsarten);
            }
        } catch (IOException ignored) {
        }

        try (InputStream is = getClass().getResourceAsStream("/codes/ges_statuscodes.json")) {
            if (is != null) {
                JsonNode root = objectMapper.readTree(is);
                List<String> statuscodes = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        String value = node.get("value") != null ? node.get("value").asText() : node.asText();
                        statuscodes.add(value);
                    }
                }
                options.put("ges_statuscodes", statuscodes);
            }
        } catch (IOException ignored) {
        }

        return options;
    }

    private String getFieldText(Node node) {
        Node target = node;
        if (node instanceof VBox v && !v.getChildren().isEmpty()) {
            target = v.getChildren().get(0);
        }

        switch (target) {
            case TextInputControl tic -> {
                return tic.getText();
            }
            case Spinner<?> spinner -> {
                Object value = spinner.getValue();
                return value != null ? String.valueOf(value) : "";
            }
            case ComboBox<?> cb -> {
                Object value = cb.getValue();
                return value != null ? String.valueOf(value) : "";
            }
            case DatePicker dp -> {
                var value = dp.getValue();
                return value != null ? value.toString() : "";
            }
            default -> {
            }
        }
        return "";
    }

    public void showInfoDialog(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showErrorDialog(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
