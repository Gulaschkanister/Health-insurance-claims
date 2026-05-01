package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
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
 * View-Schicht der Anwendung.
 *
 * Erzeugt die sichtbare Status-Ansicht und delegiert die konkrete Erstellung
 * einzelner JavaFX-Elemente an eine UI-Factory.
 */
public class View {

    private final UiFactory componentFactory;
    private final Controller controller;
    private final AppMessages messages;
    private final ObjectMapper objectMapper;
    private final Map<String, List<String>> invoiceCodeOptions;
    private BorderPane skeleton; // Hauptfenster

    public View(Controller controller) {
        this.controller = controller;
        this.componentFactory = new JavaFxUiFactory();
        this.messages = new AppMessages("/messages/ui-messages.json");
        this.objectMapper = new ObjectMapper();
        this.invoiceCodeOptions = loadInvoiceCodeOptions();
    }

    /**
     * Baut die Hauptszene mit einem einfachen Status-Text auf.
     */
    public Scene createMainScene(String statusText, double width, double height) {
        // Invoice Menu
        Menu loadedInvoices = componentFactory.createMenu(messages.get("menu.invoice"));
        List<MenuItem> invoiceItems = new ArrayList<>();
        for (String name : controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet()) {
            MenuItem item = componentFactory.createMenuItem(name);
            item.setOnAction(event -> createFormular(name)); // Korrekter EventHandler!
            invoiceItems.add(item);
        }
        loadedInvoices.getItems().addAll(invoiceItems);
        // Patient Menu
        MenuItem newPatient = new MenuItem(messages.get("menu.new"));
        newPatient.setOnAction(event -> createPerson());
        MenuItem editPatient = new MenuItem(messages.get("menu.edit"));
        editPatient.setOnAction(event -> editPerson());
        Menu patient = componentFactory.createMenu(messages.get("menu.patient"), newPatient, editPatient);

        // Me Menu
        MenuItem newSelf = new MenuItem(messages.get("menu.new"));
        newSelf.setOnAction(event -> createSelfPerson());
        MenuItem editSelf = new MenuItem(messages.get("menu.edit"));
        editSelf.setOnAction(event -> editPerson());
        Menu self = componentFactory.createMenu(messages.get("menu.self"), newSelf, editSelf);
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
            showErrorDialog(messages.get("dialog.error.title"), messages.get("msg.noTemplate"));
            return;
        }

        // Collect all fields from the invoice JSON
        Map<String, Node> allFieldNodes = new HashMap<>();
        List<SegmentInfo> segmentInfo = dtaMessage.getSegments();

        for (SegmentInfo info : segmentInfo) {
            Map<String, ValueFieldEntry> valueFields = info.getValueFields();
            for (Map.Entry<String, ValueFieldEntry> entry : valueFields.entrySet()) {
                // Überspringe interne Felder
                if (entry.getValue().isInternal()) {
                    continue;
                }
                String fieldName = entry.getKey();
                Node inputField = createInputfieldFromTag(entry.getValue().getInputField(), fieldName,
                        entry.getValue().isInternal(),
                        entry.getValue().getFieldJavaType());
                allFieldNodes.put(fieldName, inputField);
            }
        }

        // Build the form layout
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        Label title = componentFactory.createLabel(invoiceName);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        vbox.getChildren().add(title);

        // Add form fields using the invoice JSON field names directly
        List<Node> fieldNodes = new ArrayList<>();
        for (Map.Entry<String, Node> entry : allFieldNodes.entrySet()) {
            fieldNodes.add(componentFactory.createBorderPane(
                    componentFactory.createLabel(entry.getKey()),
                    entry.getValue(),
                    null, null, null));
        }

        GridPane contentGrid = componentFactory.createGridPane(2,
                fieldNodes.toArray(Node[]::new));
        vbox.getChildren().add(contentGrid);

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);

        skeleton.setCenter(scrollPane);

    }

    // TODO: könnte auch noch spezifischer für patient und me machen statt generell.
    // Aktuell sind diese aber exakt gleich
    private void editPerson() {
        Map<String, TagList> tagConfig = TagConfigLoader.loadTagConfig("/tags/person-tags.json");

        // Load all patients
        List<Patient> patients = controller.getDatabase().getAllPatients();
        if (patients.isEmpty()) {
            showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noPatients"));
            return;
        }

        // Create patient selection dropdown
        List<String> patientDisplayNames = new ArrayList<>();
        Map<String, Patient> patientMap = new HashMap<>();
        for (Patient p : patients) {
            String displayName = p.getFirstname() + " " + p.getLastname() + " (ID: " + p.getId() + ")";
            patientDisplayNames.add(displayName);
            patientMap.put(displayName, p);
        }

        // Patient selection UI
        VBox selectionPane = new VBox(10);
        selectionPane.setPadding(new Insets(20));
        Label selectLabel = componentFactory.createLabel("Patienten auswählen:");
        selectLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        javafx.scene.control.ComboBox<String> patientCombo = new javafx.scene.control.ComboBox<>();
        patientCombo.getItems().addAll(patientDisplayNames);
        patientCombo.setPrefWidth(300);

        selectionPane.getChildren().addAll(selectLabel, patientCombo);

        // Create form container (initially empty, will be populated on selection)
        VBox formContainer = new VBox(10);
        formContainer.setPadding(new Insets(20));
        ScrollPane formScrollPane = new ScrollPane(formContainer);
        formScrollPane.setFitToWidth(true);

        // Handle patient selection
        patientCombo.setOnAction(event -> {
            String selectedDisplay = patientCombo.getValue();
            Patient selected = patientMap.get(selectedDisplay);
            if (selected != null) {
                formContainer.getChildren().clear();
                populateEditForm(formContainer, tagConfig, selected);
            }
        });

        // Main layout
        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(10));
        mainVBox.getChildren().addAll(selectionPane, new javafx.scene.control.Separator(), formScrollPane);

        ScrollPane mainScrollPane = new ScrollPane(mainVBox);
        mainScrollPane.setFitToWidth(true);
        skeleton.setCenter(mainScrollPane);
    }

    /**
     * Populates the edit form with patient data and input fields.
     */
    private void populateEditForm(VBox formContainer, Map<String, TagList> tagConfig, Patient patient) {
        Map<String, Node> inputFields = new HashMap<>();
        List<Node> fieldNodes = new ArrayList<>();

        // Build input fields from tag configuration and populate with patient data
        for (Map.Entry<String, TagList> entry : tagConfig.entrySet()) {
            String fieldName = entry.getKey();
            TagList tagList = entry.getValue();

            Node inputField = createInputFieldFromTagList(fieldName, tagList);

            // Populate field with patient data
            populateFieldValue(inputField, fieldName, patient);
            inputFields.put(fieldName, inputField);

            fieldNodes.add(componentFactory.createBorderPane(
                    componentFactory.createLabel(fieldName),
                    inputField,
                    null, null, null));
        }

        // Create update button
        Button updateButton = new Button("Aktualisieren");
        updateButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        updateButton.setOnAction(event -> updatePerson(inputFields, patient));

        // Create delete button
        Button deleteButton = new Button("Löschen");
        deleteButton.setStyle("-fx-padding: 10; -fx-font-size: 14; -fx-text-fill: white; -fx-background-color: #d9534f;");
        deleteButton.setOnAction(event -> confirmDeletePerson(patient));

        // Create cancel button
        Button cancelButton = new Button("Abbrechen");
        cancelButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        cancelButton.setOnAction(event -> createMainScene("Bereit", 800, 600));

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(updateButton, deleteButton, cancelButton);

        Label title = componentFactory.createLabel(messages.get("title.patient.edit") + ": " + patient.getFirstname() + " " + patient.getLastname());
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        GridPane contentGrid = componentFactory.createGridPane(2,
                fieldNodes.toArray(Node[]::new));

        formContainer.getChildren().addAll(title, contentGrid, buttonBox);
    }

    /**
     * Populates a form field with patient data.
     */
    private void populateFieldValue(Node field, String fieldName, Patient patient) {
        String value = getPatientFieldValue(fieldName, patient);
        Node target = field;
        if (field instanceof VBox v && !v.getChildren().isEmpty()) {
            target = v.getChildren().get(0);
        }
        if (target instanceof TextField textField) {
            textField.setText(value != null ? value : "");
        } else if (target instanceof Spinner<?> spinner && value != null && !value.isBlank()) {
            try {
                int numericValue = Integer.parseInt(value);
                @SuppressWarnings("unchecked")
                Spinner<Integer> integerSpinner = (Spinner<Integer>) spinner;
                integerSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(Integer.MIN_VALUE, Integer.MAX_VALUE, numericValue));
            } catch (NumberFormatException e) {
                // Ignore non-numeric values for spinner fields.
            }
        }
    }

    /**
     * Get patient field value by name.
     */
    private String getPatientFieldValue(String fieldName, Patient patient) {
        return switch (fieldName) {
            case "firstname" ->
                patient.getFirstname();
            case "lastname" ->
                patient.getLastname();
            case "street" ->
                patient.getStreet();
            case "country" ->
                patient.getCountry();
            case "housenumber" ->
                patient.getHousenumber();
            case "plz" ->
                String.valueOf(patient.getPlz());
            case "ik" ->
                String.valueOf(patient.getIk());
            default ->
                "";
        };
    }

    /**
     * Updates patient data and saves to database.
     */
    private void updatePerson(Map<String, Node> inputFields, Patient patient) {
        try {
            patient.setFirstname(getTextFromField(inputFields.get("firstname")));
            patient.setLastname(getTextFromField(inputFields.get("lastname")));
            patient.setStreet(getTextFromField(inputFields.get("street")));
            patient.setCountry(getTextFromField(inputFields.get("country")));
            patient.setHousenumber(getTextFromField(inputFields.get("housenumber")));
            patient.setPlz(Integer.parseInt(getTextFromField(inputFields.get("plz"))));
            patient.setIk(Integer.parseInt(getTextFromField(inputFields.get("ik"))));

            controller.getDatabase().savePatient(patient);
            showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.patientUpdated"));
            createMainScene(messages.get("msg.patientUpdated"), 800, 600);
        } catch (NumberFormatException e) {
            showErrorDialog(messages.get("dialog.error.title"), messages.get("msg.invalidNumbers"));
        } catch (Exception e) {
            showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
        }
    }

    /**
     * Confirms and deletes a patient.
     */
    private void confirmDeletePerson(Patient patient) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.get("msg.deleteConfirmTitle"));
        alert.setHeaderText(messages.get("msg.deleteConfirmHeader"));
        alert.setContentText(String.format(messages.get("msg.deleteConfirmBody"), patient.getFirstname() + " " + patient.getLastname()));

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            deletePerson(patient);
        }
    }

    /**
     * Deletes a patient from the database.
     */
    private void deletePerson(Patient patient) {
        try {
            controller.getDatabase().deletePatient(patient);
            showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.patientDeleted"));
            createMainScene(messages.get("msg.patientDeleted"), 800, 600);
        } catch (Exception e) {
            showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
        }
    }

    private void createPerson() {
        showCreatePersonForm(messages.get("title.patient.new"), false);
    }

    private void createSelfPerson() {
        showCreatePersonForm(messages.get("title.self.new"), true);
    }

    private void showCreatePersonForm(String titleText, boolean createServiceProvider) {
        // Load tag configuration for Person/Patient
        Map<String, TagList> tagConfig = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
        Map<String, Node> inputFields = new HashMap<>();

        // Build input fields from tag configuration
        List<Node> fieldNodes = new ArrayList<>();
        for (Map.Entry<String, TagList> entry : tagConfig.entrySet()) {
            String fieldName = entry.getKey();
            TagList tagList = entry.getValue();

            Node inputField = createInputFieldFromTagList(fieldName, tagList);
            inputFields.put(fieldName, inputField);

            fieldNodes.add(componentFactory.createBorderPane(
                    componentFactory.createLabel(messages.get("field." + fieldName)),
                    inputField,
                    null, null, null));
        }

        // VBox presetBox = createPatientPresetBox(inputFields);
        // Create save button
        Button saveButton = new Button(messages.get("button.save"));
        saveButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        saveButton.setOnAction(event -> savePerson(inputFields, createServiceProvider));

        Button cancelButton = new Button(messages.get("button.cancel"));
        cancelButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        cancelButton.setOnAction(event -> createMainScene(titleText, 800, 600));

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(saveButton, cancelButton);

        // Build layout
        VBox vbox = new VBox();
        Label title = componentFactory.createLabel(titleText);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        GridPane contentGrid = componentFactory.createGridPane(2,
                fieldNodes.toArray(Node[]::new));
        vbox.getChildren().add(title);
        // vbox.getChildren().add(presetBox);
        vbox.getChildren().add(contentGrid);
        vbox.getChildren().add(buttonBox);
        vbox.setPadding(new Insets(20));
        vbox.setSpacing(10);

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);

        skeleton.setCenter(scrollPane);
    }

    /**
     * Creates an input field based on the TagList configuration. Applies
     * modifiers (e.g., MaxLength) to the field.
     */
    private Node createInputFieldFromTagList(String fieldName, TagList tagList) {
        InputOption inputOption = tagList.getInputOption();
        switch (inputOption) {
            case STRING -> {
                TextField textField = (TextField) componentFactory.createTextField();
                applyMaxLengthModifier(textField, tagList);
                return textField;

            }

            case NUMBER -> {
                Spinner<Integer> spinner = componentFactory.createSpinner(Integer.class, null, inputOption);
                return createValidatedSpinnerNode(fieldName, spinner, inputOption, "Integer");
            }

            case PERCENT, COST -> {
                Spinner<java.math.BigDecimal> spinner = componentFactory.createSpinner(BigDecimal.class, null, inputOption);
                return createValidatedSpinnerNode(fieldName, spinner, inputOption, "Integer");
            }
            case CODE -> {
                return componentFactory.createComboBox(false);
            }
            case NUMBER_SUGGESTION -> {
                return componentFactory.createComboBox(true);
            }
            case DATE, TIME -> {

                return componentFactory.createTextField();
            }
        }
        return null;
    }

    /**
     * Wraps a Spinner in a VBox with an error label and attaches validation
     * listeners.
     */
    private Node createValidatedSpinnerNode(String fieldName, Spinner<?> spinner, InputOption inputOption, String javaFieldType) {
        Label errorLabel = componentFactory.createLabel("");
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11;");
        errorLabel.setVisible(false);

        VBox box = new VBox(4);
        box.getChildren().addAll(spinner, errorLabel);

        Runnable validate = () -> {
            String valText = "";
            try {
                if (spinner.getEditor() != null && !spinner.getEditor().getText().isBlank()) {
                    valText = spinner.getEditor().getText();
                } else if (spinner.getValue() != null) {
                    valText = String.valueOf(spinner.getValue());
                }
            } catch (Exception e) {
                valText = "";
            }

            // Special-case: PLZ format (5 digits)
            if ("plz".equalsIgnoreCase(fieldName) && !valText.isBlank()) {
                if (!valText.matches("\\d{5}")) {
                    errorLabel.setText("PLZ muss 5-stellig sein");
                    errorLabel.setVisible(true);
                    spinner.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
                    return;
                }
            }

            // Numeric range check for IntegerSpinnerValueFactory
            if (spinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intVf) {
                try {
                    int min = intVf.getMin();
                    int max = intVf.getMax();
                    int cur = 0;
                    if (!valText.isBlank()) {
                        cur = Integer.parseInt(valText);
                        if (cur < min || cur > max) {
                            errorLabel.setText(String.format("Wert muss zwischen %d und %d liegen", min, max));
                            errorLabel.setVisible(true);
                            spinner.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
                            return;
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // will be handled by FieldValidator
                }
            }

            FieldValidator.ValidationResult res = FieldValidator.validate(fieldName, valText, inputOption, javaFieldType);
            if (res == null) {
                errorLabel.setVisible(false);
                spinner.setStyle(null);
                return;
            }
            if (!res.isValid) {
                errorLabel.setText(res.errorMessage != null ? res.errorMessage : "Ungültiger Wert");
                errorLabel.setVisible(true);
                spinner.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
            } else {
                errorLabel.setVisible(false);
                spinner.setStyle(null);
            }
        };

        // Attach listeners
        spinner.valueProperty().addListener((obs, o, n) -> validate.run());
        if (spinner.getEditor() != null) {
            spinner.getEditor().textProperty().addListener((obs, o, n) -> validate.run());
            spinner.getEditor().focusedProperty().addListener((obs, oldF, newF) -> {
                if (!newF) {
                    validate.run();
                }
            });
        }

        // Initial validation
        validate.run();

        return box;
    }

    /**
     * Applies MaxLength modifier to a TextField if present in the modifier
     * list.
     */
    private void applyMaxLengthModifier(TextField textField, TagList tagList) {
        for (ModifierInstance modifier : tagList.getModifierList()) {
            if (modifier instanceof MaxLengthModifier maxLengthMod) {
                int maxLength = maxLengthMod.getMaxLength();
                // Limit text input length
                textField.setTextFormatter(new TextFormatter<>(change -> {
                    if (change.getControlNewText().length() <= maxLength) {
                        return change;
                    }
                    return null;
                }));
            }
        }
    }

    /**
     * Saves the person data from the input fields.
     */
    private void savePerson(Map<String, Node> inputFields, boolean saveAsServiceProvider) {
        try {
            // Extract values from input fields
            String firstname = getFieldText(inputFields.get("firstname"));
            String lastname = getFieldText(inputFields.get("lastname"));
            String street = getFieldText(inputFields.get("street"));
            String country = getFieldText(inputFields.get("country"));
            String housenumber = getFieldText(inputFields.get("housenumber"));
            int plz = Integer.parseInt(getFieldText(inputFields.get("plz")));
            int ik = Integer.parseInt(getFieldText(inputFields.get("ik")));

            if (saveAsServiceProvider) {
                ServiceProvider serviceProvider = new ServiceProvider(firstname, lastname, street, country, housenumber, plz, ik);
                controller.getDatabase().saveServiceProvider(serviceProvider);
            } else {
                Patient patient = new Patient(firstname, lastname, street, country, housenumber, plz, ik);
                controller.getDatabase().savePatient(patient);
            }

            showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.patientCreated"));
            // Refresh main scene after save
            createMainScene(messages.get("msg.patientCreated"), 800, 600);
        } catch (NumberFormatException e) {
            showErrorDialog(messages.get("dialog.error.title"), messages.get("msg.invalidNumbers"));
        } catch (Exception e) {
            showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
        }
    }

    /**
     * Extracts text from a text input control or returns empty string.
     */
    private String getTextFromField(Node node) {
        return getFieldText(node);
    }

    private String getFieldText(Node node) {
        Node target = node;
        if (node instanceof VBox v && !v.getChildren().isEmpty()) {
            target = v.getChildren().get(0);
        }
        if (target instanceof TextInputControl textInputControl) {
            return textInputControl.getText();
        }
        if (target instanceof Spinner<?> spinner) {
            Object value = spinner.getValue();
            return value != null ? String.valueOf(value) : "";
        }
        if (target instanceof ComboBox<?> comboBox) {
            Object value = comboBox.getValue();
            return value != null ? String.valueOf(value) : "";
        }
        return "";
    }

    private VBox createPatientPresetBox(Map<String, Node> inputFields) {
        VBox presetBox = new VBox(6);
        presetBox.setPadding(new Insets(0, 0, 10, 0));

        Label presetLabel = componentFactory.createLabel(messages.get("label.patientPreset"));
        ComboBox<String> patientCombo = new ComboBox<>();
        patientCombo.setPromptText(messages.get("prompt.patientPreset"));

        List<Patient> patients = controller.getDatabase().getAllPatients();
        Map<String, Patient> patientMap = new HashMap<>();
        for (Patient patient : patients) {
            String displayName = patient.getFirstname() + " " + patient.getLastname() + " (ID: " + patient.getId() + ")";
            patientCombo.getItems().add(displayName);
            patientMap.put(displayName, patient);
        }

        patientCombo.setOnAction(event -> {
            String selected = patientCombo.getValue();
            if (selected == null) {
                return;
            }
            Patient patient = patientMap.get(selected);
            if (patient != null) {
                populatePersonFields(inputFields, patient);
            }
        });

        presetBox.getChildren().addAll(presetLabel, patientCombo);
        return presetBox;
    }

    private void populatePersonFields(Map<String, Node> inputFields, Patient patient) {
        for (Map.Entry<String, Node> entry : inputFields.entrySet()) {
            populateFieldValue(entry.getValue(), entry.getKey(), patient);
        }
    }

    /**
     * Hier wird entschieden, welcher Typ für das jeweilige Formularfeld
     * verwendet wird
     *
     * @param inputOption Eingabe Tag
     * @param directName Anzeigename der möglich wäre
     * @param javaFieldType JavaType
     * @return Node
     */
    private Node createInputfieldFromTag(InputOption inputOption, String directName, boolean visible,
            String javaFieldType)
            throws NullPointerException {

        if (inputOption == null) {
            return null;
            // throw new NullPointerException(
            // "Eingabefeld kann nicht erstellt werden, da keine Inputoption gewählt wurde"
            // + inputOption);
        }
        return switch (inputOption) {
            case CODE ->
                createCodeDropdownForInvoiceField(directName);
            case NUMBER_SUGGESTION ->
                componentFactory.createComboBox(true);
            case NUMBER ->
                componentFactory.createSpinner(Integer.class, null, inputOption);
            case STRING ->
                componentFactory.createTextField();
            case PERCENT, COST ->
                componentFactory.createSpinner(BigDecimal.class, null, inputOption);
            case BOOLEAN ->
                componentFactory.createCheckBox(directName);
            case DATE ->
                componentFactory.createDatePicker();
            default ->
                throw new IllegalArgumentException("Unbekannter InputType: " + inputOption);
        };

    }

    /**
     * Creates a code dropdown for invoice fields based on known field-to-code mappings.
     */
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
        if (normalized.contains("abrechnungscode")) {
            return invoiceCodeOptions.getOrDefault("abrechnungscodes", List.of());
        }
        if (normalized.contains("nachricht") && normalized.contains("code")) {
            return invoiceCodeOptions.getOrDefault("nachrichtentypen", List.of());
        }
        if (normalized.contains("verarbeitungskennzeichen")) {
            return invoiceCodeOptions.getOrDefault("verarbeitungskennzeichen", List.of());
        }

        return List.of();
    }

    private String normalizeFieldKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase();
        normalized = normalized.replace('-', '_');
        normalized = normalized.replace('/', '_');
        normalized = normalized.replace(' ', '_');
        return normalized;
    }

    private Map<String, List<String>> loadInvoiceCodeOptions() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("rechnungsarten", readCodeOptions("/codes/rechnungsarten.json"));
        map.put("ges_statuscodes", readCodeOptions("/codes/ges_statuscodes.json"));
        map.put("abrechnungscodes", readCodeOptions("/codes/abrechnungscodes.json"));
        map.put("nachrichtentypen", readCodeOptions("/codes/nachrichtentypen.json"));
        map.put("verarbeitungskennzeichen", readCodeOptions("/codes/verarbeitungskennzeichen.json"));
        return map;
    }

    private List<String> readCodeOptions(String resourcePath) {
        List<String> options = new ArrayList<>();
        try (InputStream in = View.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return options;
            }
            JsonNode root = objectMapper.readTree(in);
            JsonNode codesNode = root.get("codes");
            if (codesNode == null || !codesNode.isArray()) {
                return options;
            }
            for (JsonNode codeNode : codesNode) {
                JsonNode codeValue = codeNode.get("code");
                if (codeValue != null && !codeValue.asText().isBlank()) {
                    options.add(codeValue.asText());
                }
            }
        } catch (IOException e) {
            // keep empty list if loading fails
        }
        return options;
    }

    /**
     * Zeigt einen blockierenden Fehlerdialog fuer den Benutzer an.
     */
    public void showErrorDialog(String title, String message) {
        Alert alert = componentFactory.createErrorAlert(title, message);
        alert.showAndWait();
    }

    /**
     * Zeigt einen blockierenden Infodialog fuer den Benutzer an.
     */
    public void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
