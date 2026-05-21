package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.entity.InvoiceBlueprint;
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
import de.gkvtransmitter.validator.ValidationResult;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class View {

    private static final Path DTA_OUTBOX = Paths.get("dta", "outbox");
    private static final Path DTA_SENT = Paths.get("dta", "sent");
    private static final Path DTA_INBOX = Paths.get("dta", "inbox");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

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

        Map<String, Runnable> invoiceHandlers = new LinkedHashMap<>();
        List<String> invoiceNames = controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet()
                .stream()
                .sorted()
                .toList();
        for (String name : invoiceNames) {
            invoiceHandlers.put(name, () -> createFormular(name));
        }

        List<InvoiceBlueprint> blueprints = controller.getDatabase().getAllInvoiceBlueprints().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
        for (InvoiceBlueprint blueprint : blueprints) {
            String templateName = blueprint.getInvoiceTemplateName();
            if (controller.getGlobalDefinitions().getInvoiceTemplateCollection().containsKey(templateName)) {
                invoiceHandlers.put("[Vorlage] " + blueprint.getName(),
                        () -> createFormular(templateName, blueprint));
            }
        }

        menuBuilder.addAllInvoiceItems(invoiceHandlers);
        menuBuilder.addInvoiceItem(messages.get("menu.inbox"), this::receiveReplies);

        menuBuilder.addPatientItem(messages.get("menu.new"), this::createPerson);
        menuBuilder.addPatientItem(messages.get("menu.edit"), this::editPatient);

        menuBuilder.addSelfItem(messages.get("menu.new"), this::createSelfPerson);
        menuBuilder.addSelfItem(messages.get("menu.edit"), this::editServiceProvider);

        return menuBuilder.build();
    }

    private void createFormular(String invoiceName) {
        createFormular(invoiceName, null);
    }

    private void createFormular(String invoiceName, InvoiceBlueprint blueprint) {
        DtaMessage dtaMessage = controller.getGlobalDefinitions().getInvoiceTemplateCollection().get(invoiceName);
        if (dtaMessage == null) {
            showErrorDialog(messages.get("dialog.error.title"), messages.get("msg.noTemplate"));
            return;
        }

        Map<String, Node> allFieldNodes = new LinkedHashMap<>();
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

        if (blueprint != null) {
            applyStoredFieldValues(allFieldNodes, blueprint.getFieldPayload());
        }

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.getStyleClass().add("form-container");
        Label title = componentFactory.createLabel(invoiceName);
        title.getStyleClass().add("app-title");
        vbox.getChildren().add(title);

        List<Node> fieldNodes = new ArrayList<>();
        for (Map.Entry<String, Node> entry : allFieldNodes.entrySet()) {
            fieldNodes.add(componentFactory.createBorderPane(
                    componentFactory.createLabel(entry.getKey()),
                    entry.getValue(),
                    null, null, null));
        }

        GridPane contentGrid = componentFactory.createGridPane(2, fieldNodes.toArray(Node[]::new));
        contentGrid.getStyleClass().add("form-grid");

        List<Patient> allPatients = controller.getDatabase().getAllPatients();
        Set<Integer> preselectedPatientIds = parseSelectedPatientIds(blueprint);
        Map<Integer, CheckMenuItem> patientChecks = new HashMap<>();
        VBox patientSelection = buildPatientSelection(allPatients, preselectedPatientIds, patientChecks);

        Button saveTemplateButton = componentFactory.createButton(messages.get("button.saveTemplate"));
        saveTemplateButton.setOnAction(e -> saveBlueprint(invoiceName, allFieldNodes, patientChecks));

        Button generateDtaButton = componentFactory.createButton(messages.get("button.generateDta"));
        generateDtaButton.setOnAction(e -> generateDta(invoiceName, dtaMessage, allFieldNodes, patientChecks, false));

        Button sendDtaButton = componentFactory.createButton(messages.get("button.sendDta"));
        sendDtaButton.setOnAction(e -> generateDta(invoiceName, dtaMessage, allFieldNodes, patientChecks, true));

        Button receiveRepliesButton = componentFactory.createButton(messages.get("button.receiveReplies"));
        receiveRepliesButton.setOnAction(e -> receiveReplies());

        HBox buttonBox = new HBox(10, saveTemplateButton, generateDtaButton, sendDtaButton, receiveRepliesButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.getStyleClass().add("button-row");

        VBox invoiceSection = createSection(messages.get("section.invoiceData"), contentGrid);
        VBox patientSection = createSection(messages.get("section.patientSelection"), patientSelection);
        VBox actionSection = createSection(messages.get("section.actions"), buttonBox);

        vbox.getChildren().addAll(invoiceSection, patientSection, actionSection);

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("app-scroll-pane");
        skeleton.setCenter(scrollPane);
    }

    private VBox buildPatientSelection(List<Patient> patients, Set<Integer> preselectedPatientIds,
            Map<Integer, CheckMenuItem> patientChecks) {
        VBox box = new VBox(6);
        Label label = componentFactory.createLabel(messages.get("label.patientPreset"));

        MenuButton patientSelector = new MenuButton(messages.get("prompt.patientPreset"));
        if (patients.isEmpty()) {
            patientSelector.setDisable(true);
            patientSelector.setText(messages.get("msg.noPatients"));
        } else {
            for (Patient patient : patients) {
                String itemText = patient.getFirstname() + " " + patient.getLastname() + " (ID " + patient.getId() + ")";
                CheckMenuItem item = new CheckMenuItem(itemText);
                item.setSelected(preselectedPatientIds.contains(patient.getId()));
                patientChecks.put(patient.getId(), item);
                patientSelector.getItems().add(item);
            }
        }
        box.getChildren().addAll(label, patientSelector);
        return box;
    }

    private VBox createSection(String title, Node content) {
        VBox section = new VBox(8);
        section.getStyleClass().add("form-section");
        Label sectionTitle = componentFactory.createLabel(title);
        sectionTitle.getStyleClass().add("section-title");
        section.getChildren().addAll(sectionTitle, content);
        return section;
    }

    private void saveBlueprint(String invoiceTemplateName, Map<String, Node> fieldNodes,
            Map<Integer, CheckMenuItem> patientChecks) {
        TextInputDialog dialog = new TextInputDialog(invoiceTemplateName + " Vorlage");
        dialog.setTitle(messages.get("button.saveTemplate"));
        dialog.setHeaderText(messages.get("button.saveTemplate"));
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) {
                return;
            }

            try {
                String payload = objectMapper.writeValueAsString(collectFieldValues(fieldNodes));
                String selectedIds = collectSelectedPatientIds(patientChecks);
                InvoiceBlueprint blueprint = new InvoiceBlueprint(name.trim(), invoiceTemplateName, payload, selectedIds);
                controller.getDatabase().saveInvoiceBlueprint(blueprint);
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.blueprintSaved"));
            } catch (Exception ex) {
                showErrorDialog(messages.get("dialog.error.title"), ex.getMessage());
            }
        });
    }

    private Map<String, String> collectFieldValues(Map<String, Node> fieldNodes) {
        Map<String, String> values = new LinkedHashMap<>();
        fieldNodes.forEach((key, node) -> values.put(key, getFieldText(node)));
        return values;
    }

    private String collectSelectedPatientIds(Map<Integer, CheckMenuItem> patientChecks) {
        return patientChecks.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(entry -> String.valueOf(entry.getKey()))
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private Set<Integer> parseSelectedPatientIds(InvoiceBlueprint blueprint) {
        Set<Integer> ids = new HashSet<>();
        if (blueprint == null || blueprint.getSelectedPatientIds() == null || blueprint.getSelectedPatientIds().isBlank()) {
            return ids;
        }
        for (String part : blueprint.getSelectedPatientIds().split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private void applyStoredFieldValues(Map<String, Node> fieldNodes, String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            Map<String, String> values = objectMapper.readValue(payload, new TypeReference<Map<String, String>>() {
            });
            values.forEach((key, value) -> {
                Node node = fieldNodes.get(key);
                if (node != null) {
                    setNodeValue(node, value);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void setNodeValue(Node node, String value) {
        if (value == null) {
            return;
        }
        Node target = node;
        if (node instanceof VBox v && !v.getChildren().isEmpty()) {
            target = v.getChildren().get(0);
        }

        if (target instanceof TextInputControl tic) {
            tic.setText(value);
            return;
        }

        if (target instanceof Spinner<?> spinner) {
            try {
                if (spinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intFactory) {
                    intFactory.setValue(Integer.parseInt(value));
                } else if (spinner.getValueFactory() != null && spinner.getValueFactory().getValue() instanceof BigDecimal) {
                    @SuppressWarnings("unchecked")
                    SpinnerValueFactory<BigDecimal> factory = (SpinnerValueFactory<BigDecimal>) spinner
                            .getValueFactory();
                    factory.setValue(new BigDecimal(value));
                }
                if (spinner.getEditor() != null) {
                    spinner.getEditor().setText(value);
                }
            } catch (RuntimeException ignored) {
            }
            return;
        }

        if (target instanceof ComboBox<?> comboBox) {
            @SuppressWarnings("unchecked")
            ComboBox<String> cb = (ComboBox<String>) comboBox;
            if (!cb.getItems().contains(value)) {
                cb.getItems().add(value);
            }
            cb.setValue(value);
            return;
        }

        if (target instanceof DatePicker datePicker) {
            try {
                datePicker.setValue(LocalDate.parse(value));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void generateDta(String invoiceName, DtaMessage dtaMessage, Map<String, Node> fieldNodes,
            Map<Integer, CheckMenuItem> patientChecks, boolean sendAfterCreate) {
        try {
            ensureDtaDirs();
            Map<String, String> values = collectFieldValues(fieldNodes);
            List<Integer> selectedPatients = patientChecks.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();

            String dtaContent = createDtaPayload(dtaMessage, values, selectedPatients);
            String fileBase = sanitizeFileName(invoiceName) + "_" + LocalDateTime.now().format(FILE_TS) + ".dta";
            Path createdFile = DTA_OUTBOX.resolve(fileBase);
            Files.writeString(createdFile, dtaContent, StandardCharsets.UTF_8);
            showInfoDialog(messages.get("dialog.info.title"), String.format(messages.get("msg.dtaCreated"), createdFile));

            if (sendAfterCreate) {
                Path sentFile = DTA_SENT.resolve(fileBase);
                Files.move(createdFile, sentFile, StandardCopyOption.REPLACE_EXISTING);

                Path replyFile = DTA_INBOX.resolve(fileBase + ".reply.txt");
                String replyContent = "ACK|" + sentFile.getFileName() + "|PATIENT_IDS=" + selectedPatients;
                Files.writeString(replyFile, replyContent, StandardCharsets.UTF_8);
                showInfoDialog(messages.get("dialog.info.title"), String.format(messages.get("msg.dtaSent"), sentFile));
            }
        } catch (Exception ex) {
            showErrorDialog(messages.get("dialog.error.title"), ex.getMessage());
        }
    }

    private String createDtaPayload(DtaMessage dtaMessage, Map<String, String> values, List<Integer> selectedPatients) {
        StringBuilder builder = new StringBuilder();
        builder.append("UNA:+.? '").append(System.lineSeparator());
        for (SegmentInfo segment : dtaMessage.getSegments().stream().sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .toList()) {
            builder.append(segment.getSegmentType());
            for (Map.Entry<String, ValueFieldEntry> fieldEntry : segment.getValueFields().entrySet()) {
                String key = fieldEntry.getKey();
                ValueFieldEntry valueFieldEntry = fieldEntry.getValue();
                String value = values.getOrDefault(key, "");
                if (value.isBlank() && valueFieldEntry.getValue() != null) {
                    value = String.valueOf(valueFieldEntry.getValue());
                }
                builder.append("+").append(escapeDtaValue(value));
            }
            builder.append("'").append(System.lineSeparator());
        }
        if (!selectedPatients.isEmpty()) {
            builder.append("FTX+PAT+").append(selectedPatients).append("'").append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String escapeDtaValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", " ").replace("+", " ").replace(System.lineSeparator(), " ").trim();
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void ensureDtaDirs() throws IOException {
        Files.createDirectories(DTA_OUTBOX);
        Files.createDirectories(DTA_SENT);
        Files.createDirectories(DTA_INBOX);
    }

    private void receiveReplies() {
        try {
            ensureDtaDirs();
            List<Path> replies;
            try (var stream = Files.list(DTA_INBOX)) {
                replies = stream
                        .filter(path -> path.getFileName().toString().endsWith(".reply.txt"))
                        .sorted()
                        .toList();
            }

            if (replies.isEmpty()) {
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noReplies"));
                return;
            }

            StringBuilder details = new StringBuilder();
            for (Path reply : replies) {
                details.append(reply.getFileName()).append(": ")
                        .append(Files.readString(reply, StandardCharsets.UTF_8))
                        .append(System.lineSeparator());
            }

            showInfoDialog(messages.get("dialog.info.title"),
                    String.format(messages.get("msg.repliesReceived"), replies.size())
                            + System.lineSeparator() + details);
        } catch (Exception ex) {
            showErrorDialog(messages.get("dialog.error.title"), ex.getMessage());
        }
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
                fc -> skeleton.setCenter(null));
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
                fc -> skeleton.setCenter(null));
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
        saveButton.setOnAction(event -> savePerson(inputFields, createServiceProvider));

        Button cancelButton = componentFactory.createButton(messages.get("button.cancel"));
        cancelButton.setOnAction(event -> skeleton.setCenter(null));

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getStyleClass().add("button-row");
        buttonBox.getChildren().addAll(saveButton, cancelButton);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.getStyleClass().add("form-container");
        Label title = componentFactory.createLabel(titleText);
        title.getStyleClass().add("app-title");
        GridPane contentGrid = componentFactory.createGridPane(2, fieldNodes.toArray(Node[]::new));
        contentGrid.getStyleClass().add("form-grid");

        VBox personDataSection = createSection(messages.get("section.personData"), contentGrid);
        VBox actionSection = createSection(messages.get("section.actions"), buttonBox);
        vbox.getChildren().addAll(title, personDataSection, actionSection);

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("app-scroll-pane");
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
                ServiceProvider sp = new ServiceProvider(firstname, lastname, street, country, housenumber, plz, ik,
                        birthDate);
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
        if (inputOption == null) {
            return null;
        }

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
        if (tagList == null) {
            return componentFactory.createTextField();
        }

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
                if (!newF) {
                    validate.run();
                }
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

        ValidationResult res = FieldValidator.validate(fieldName, valText, inputOption, javaFieldType);
        if (res == null || res.isValid()) {
            errorLabel.setVisible(false);
            spinner.setStyle(null);
        } else {
            errorLabel.setText(res.getErrorMessage() != null ? res.getErrorMessage() : "Ungültiger Wert");
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

        if (target instanceof TextInputControl tic) {
            return tic.getText();
        }

        if (target instanceof Spinner<?> spinner) {
            Object value = spinner.getValue();
            return value != null ? String.valueOf(value) : "";
        }

        if (target instanceof ComboBox<?> cb) {
            Object value = cb.getValue();
            return value != null ? String.valueOf(value) : "";
        }

        if (target instanceof DatePicker dp) {
            var value = dp.getValue();
            return value != null ? value.toString() : "";
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
