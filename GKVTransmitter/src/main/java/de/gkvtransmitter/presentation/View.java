package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.entity.Blueprint;
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
import javafx.scene.control.ChoiceDialog;
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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * View-Schicht der Anwendung - REFAKTORIERT
 *
 * Nach großem Refactoring jetzt mit: - Fokus auf Szenen-Management und
 * Hauptmenü - Delegation komplexer Logik an spezialisierte Komponenten - Klare
 * Separation of Concerns
 *
 * Delegationen: - Entity-Bearbeitung -> EditFormController - Feld-Populierung
 * -> EntityFieldPopulator - Menü-Erstellung -> MenuBuilder
 */
public class View {

    private final UiFactory componentFactory;
    private final Controller controller;
    private final AppMessages messages;
    private final ObjectMapper objectMapper;
    private final Map<String, List<String>> invoiceCodeOptions;
    private Map<String, String> currentInvoiceHeaderCodes = Map.of();
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

    /**
     * Erstellt die Hauptszene mit einem leeren Layout und einer Menüleiste.
     *
     * @param statusText Der Text, der im Statusbereich angezeigt werden soll
     * @param width Die Breite der Szene
     * @param height Die Höhe der Szene
     * @return Die erstellte Hauptszene
     */
    public Scene createMainScene(String statusText, double width, double height) {
        MenuBar menuBar = buildMainMenuBar();
        this.skeleton = componentFactory.createBorderPane(menuBar, null, null, null, null);
        return componentFactory.createScene(skeleton, width, height);
    }

    /**
     * Erstellt die Hauptmenüleiste mit dynamischen Einträgen basierend auf den
     *
     * @return die erstellte MenuBar für die Hauptszene
     */
    private MenuBar buildMainMenuBar() {
        MenuBuilder menuBuilder = new MenuBuilder(componentFactory, messages);

        Map<String, Runnable> invoiceHandlers = new LinkedHashMap<>();
        for (String name : controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet()) {
            invoiceHandlers.put(name, () -> createFormular(name));
        }
        menuBuilder.addAllInvoiceItems(invoiceHandlers);

        menuBuilder.addPatientItem(messages.get("menu.new"), this::createPerson);
        menuBuilder.addPatientItem(messages.get("menu.edit"), this::editPatient);
        menuBuilder.addPatientItem(messages.get("menu.delete"), this::deletePatient);

        menuBuilder.addSelfItem(messages.get("menu.new"), this::createSelfPerson);
        menuBuilder.addSelfItem(messages.get("menu.edit"), this::editServiceProvider);
        menuBuilder.addSelfItem(messages.get("menu.delete"), this::deleteServiceProvider);

        MenuBar menuBar = menuBuilder.build();

        // Add settlement (Abrechnung) menu
        javafx.scene.control.MenuItem settlementItem = componentFactory.createMenuItem(messages.get("menu.settlement"));
        settlementItem.setOnAction(ev -> createAbrechnung());
        menuBar.getMenus().add(componentFactory.createMenu(messages.get("menu.settlement"), settlementItem));

        return menuBar;
    }

    /**
     * Erstellt und zeigt das Formular für die angegebene Rechnungsvorlage.
     *
     * @param invoiceName Der Name der Rechnungsvorlage, die geladen werden
     * soll.
     */
    private void createFormular(String invoiceName) {
        DtaMessage dtaMessage = controller.getGlobalDefinitions().getInvoiceTemplateCollection().get(invoiceName);
        if (dtaMessage == null) {
            showErrorDialog(messages.get("dialog.error.title"), messages.get("msg.noTemplate"));
            return;
        }
        this.currentInvoiceHeaderCodes = dtaMessage.getHeaderCodes();
        Map<String, Node> allFieldNodes = new HashMap<>();
        for (SegmentInfo info : dtaMessage.getSegments()) {
            for (Map.Entry<String, ValueFieldEntry> entry : info.getValueFields().entrySet()) {
                if (!entry.getValue().isInternal() && entry.getValue().getPersonRole() == null) {
                        Node inputField = createInputfieldFromTag(
                            entry.getValue().getInputField(),
                            entry.getKey());
                    allFieldNodes.put(entry.getKey(), inputField);
                }
            }
        }

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        Label title = componentFactory.createLabel(invoiceName);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        Button saveBlueprintBtn = componentFactory.createButton(messages.get("button.saveBlueprint"));
        saveBlueprintBtn.setOnAction(evt -> {
            TextInputDialog dialog = new TextInputDialog(invoiceName + "-blueprint");
            dialog.setTitle(messages.get("button.saveBlueprint"));
            dialog.setHeaderText(null);
            dialog.setContentText("Name:");
            dialog.showAndWait().ifPresent(name -> {
                try {
                    Map<String, Object> values = collectVisibleFieldValues(allFieldNodes);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("template", invoiceName);
                    payload.put("headerCodes", this.currentInvoiceHeaderCodes);
                    payload.put("fields", values);
                    String json = objectMapper.writeValueAsString(payload);
                    Blueprint bp = new Blueprint(name, invoiceName, json, OffsetDateTime.now());
                    controller.getDatabase().saveBlueprint(bp);
                    showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.blueprintSaved"));
                } catch (Exception e) {
                    showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
                }
            });
        });

        HBox titleRow = new HBox(10, title, saveBlueprintBtn);
        vbox.getChildren().add(titleRow);
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
        this.currentInvoiceHeaderCodes = Map.of();
    }

    private Map<String, Object> collectVisibleFieldValues(Map<String, Node> fieldNodes) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Node> e : fieldNodes.entrySet()) {
            String key = e.getKey();
            Node node = e.getValue();
            Object value = null;
            if (node instanceof TextField) {
                value = ((TextField) node).getText();
            } else if (node instanceof ComboBox) {
                value = ((ComboBox<?>) node).getValue();
            } else if (node instanceof DatePicker) {
                value = ((DatePicker) node).getValue();
            } else if (node instanceof Spinner) {
                value = ((Spinner<?>) node).getValue();
            } else if (node instanceof javafx.scene.control.CheckBox) {
                value = ((javafx.scene.control.CheckBox) node).isSelected();
            } else if (node instanceof TextInputControl) {
                value = ((TextInputControl) node).getText();
            }
            result.put(key, value);
        }
        return result;
    }

    /**
     * Builds and shows the Abrechnung (settlement) panel where user can select
     * a blueprint, a service provider and participating patients.
     */
    private void createAbrechnung() {
        List<de.gkvtransmitter.entity.Blueprint> blueprints = controller.getDatabase().getAllBlueprints();
        List<ServiceProvider> serviceProviders = controller.getDatabase().getAllServiceProviders();
        List<Patient> patients = controller.getDatabase().getAllPatients();

        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label title = componentFactory.createLabel(messages.get("menu.settlement"));

        // Blueprint selector
        Label bpLabel = componentFactory.createLabel(messages.get("label.selectBlueprint"));
        ComboBox<de.gkvtransmitter.entity.Blueprint> bpCombo = new ComboBox<>();
        bpCombo.setPrefWidth(400);
        if (blueprints != null && !blueprints.isEmpty()) {
            bpCombo.getItems().addAll(blueprints);
            bpCombo.setConverter(new javafx.util.StringConverter<de.gkvtransmitter.entity.Blueprint>() {
                @Override
                public String toString(de.gkvtransmitter.entity.Blueprint object) {
                    return object == null ? "" : object.getName();
                }

                @Override
                public de.gkvtransmitter.entity.Blueprint fromString(String string) {
                    return null;
                }
            });
            bpCombo.getSelectionModel().selectFirst();
        }

        // Service Provider selector
        Label spLabel = componentFactory.createLabel(messages.get("label.selectServiceProvider"));
        ComboBox<ServiceProvider> spCombo = new ComboBox<>();
        spCombo.setPrefWidth(400);
        if (serviceProviders != null && !serviceProviders.isEmpty()) {
            spCombo.getItems().addAll(serviceProviders);
            spCombo.setConverter(new javafx.util.StringConverter<ServiceProvider>() {
                @Override
                public String toString(ServiceProvider object) {
                    return object == null ? "" : object.getFirstname() + " " + object.getLastname();
                }

                @Override
                public ServiceProvider fromString(String string) {
                    return null;
                }
            });
            spCombo.getSelectionModel().selectFirst();
        }

        // Patients checklist
        Label patsLabel = componentFactory.createLabel(messages.get("label.selectPatients"));
        VBox checklist = new VBox(4);
        List<javafx.scene.control.CheckBox> patientBoxes = new ArrayList<>();
        if (patients != null && !patients.isEmpty()) {
            for (Patient p : patients) {
                javafx.scene.control.CheckBox cb = componentFactory.createCheckBox(p.getFirstname() + " " + p.getLastname());
                patientBoxes.add(cb);
                checklist.getChildren().add(cb);
            }
        }

        Button start = componentFactory.createButton(messages.get("button.startSettlement"));
        start.setOnAction(ev -> {
            if (blueprints == null || blueprints.isEmpty()) {
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noBlueprints"));
                return;
            }
            if (serviceProviders == null || serviceProviders.isEmpty()) {
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noServiceProviders"));
                return;
            }
            if (patients == null || patients.isEmpty()) {
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noPatients"));
                return;
            }

            de.gkvtransmitter.entity.Blueprint chosen = bpCombo.getValue();
            ServiceProvider sp = spCombo.getValue();
            List<Patient> selectedPatients = new ArrayList<>();
            for (int i = 0; i < patientBoxes.size(); i++) {
                if (patientBoxes.get(i).isSelected()) {
                    selectedPatients.add(patients.get(i));
                }
            }

            String summary = String.format("Blaupause: %s\nDienstleister: %s %s\nTeilnehmer: %d",
                    chosen == null ? "-" : chosen.getName(),
                    sp == null ? "-" : sp.getFirstname(),
                    sp == null ? "" : sp.getLastname(),
                    selectedPatients.size());
            showInfoDialog(messages.get("dialog.info.title"), summary);
        });

        root.getChildren().addAll(title, bpLabel, bpCombo, spLabel, spCombo, patsLabel, checklist, start);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        skeleton.setCenter(scroll);
    }

    /**
     * Gibt den Textwert eines UI-Elements zurück, abhängig von dessen Typ.
     *
     * @param inputOption - die Eingabeoption, die den Typ des UI-Elements
     * angibt
     * @param directName - der direkte Name des Feldes, der für spezielle Fälle
     * wie Code-Auswahl verwendet werden kann
     * @param visible - ob das Feld sichtbar ist, was für die Rückgabe
     * berücksichtigt werden könnte
     * @param javaFieldType - der Java-Typ des Feldes, der für die Rückgabe
     * berücksichtigt werden könnte
     * @return der Textwert des UI-Elements als String
     */
    private Node createInputfieldFromTag(InputOption inputOption, String directName) {
        if (inputOption == null) {
            return null;
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
     * Erstellt ein Dropdown-Menü für Felder, die mit Codes gefüllt werden
     * sollen, basierend auf dem Feldnamen.
     *
     * @param fieldName der Name des Feldes, für das das Dropdown erstellt
     * werden soll
     * @return ein Node, das ein ComboBox mit den entsprechenden Code-Optionen
     * enthält, oder eine leere ComboBox, wenn keine Optionen gefunden wurden
     */
    private Node createCodeDropdownForInvoiceField(String fieldName) {
        List<String> options = resolveCodeOptionsForField(fieldName);
        String normalized = normalizeFieldKey(fieldName);
        String headerDefault = this.currentInvoiceHeaderCodes.getOrDefault(normalized, this.currentInvoiceHeaderCodes.get(fieldName));

        if (options.isEmpty()) {
            TextField tf = componentFactory.createTextField();
            if (headerDefault != null && !headerDefault.isBlank()) {
                tf.setText(headerDefault);
            } else {
                tf.setPromptText("Keine Codes vorhanden");
            }
            return tf;
        }

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(300);
        comboBox.getItems().addAll(options);

        if (headerDefault != null && !headerDefault.isBlank()) {
            if (comboBox.getItems().contains(headerDefault)) {
                comboBox.getSelectionModel().select(headerDefault);
            } else {
                comboBox.getItems().add(0, headerDefault);
                comboBox.getSelectionModel().selectFirst();
            }
        } else {
            comboBox.getSelectionModel().selectFirst();
        }

        return comboBox;
    }

    /**
     * Löst die entsprechenden Code-Optionen für ein gegebenes Feld basierend
     * auf dem
     *
     * @param fieldName der Name des Feldes, für das die Code-Optionen aufgelöst
     * werden sollen
     * @return eine Liste von Code-Optionen, die für das angegebene Feld
     * relevant sind, oder eine leere Liste, wenn keine Optionen gefunden wurden
     */
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

    /**
     * Normalisiert einen Feldnamen, indem er in Kleinbuchstaben umgewandelt und
     * alle
     *
     * @param key der Nicht-Alphanumerischen Zeichen entfernt werden, um eine
     * konsistente Basis für die Erkennung von Schlüsselwörtern wie
     * "rechnungsart" oder "status" zu schaffen, unabhängig von der
     * ursprünglichen Formatierung des Feldnamens.
     * @return der normalisierte Feldname, der nur aus Kleinbuchstaben und
     * Zahlen besteht, oder ein leerer String, wenn der Eingabewert null ist
     */
    private String normalizeFieldKey(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Lädt die Code-Optionen für Rechnungsarten und GES-Statuscodes aus den
     *
     * @return eine Map, die die geladenen Code-Optionen enthält, gruppiert nach
     * Kategorie (z.B. "rechnungsarten", "ges_statuscodes"), oder eine leere
     * Map, wenn keine Optionen geladen werden konnten
     */
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

    private void editPatient() {
        EditFormController<Patient> editController = new EditFormController<>(
                componentFactory,
                messages,
                patientPopulator,
                () -> controller.getDatabase().getAllPatients(),
                patient -> controller.getDatabase().savePatient(patient),
                patient -> controller.getDatabase().deletePatient(patient),
                false,
                fieldName -> createInputFieldFromTagList(fieldName,
                        TagConfigLoader.loadTagConfig("/tags/person-tags.json").get(fieldName)),
                "Patient",
            fc -> {
            },
            ec -> skeleton.setCenter(ec.buildEditForm())
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
                false,
                fieldName -> createInputFieldFromTagList(fieldName,
                        TagConfigLoader.loadTagConfig("/tags/person-tags.json").get(fieldName)),
                "ServiceProvider",
                fc -> {
                },
                ec -> skeleton.setCenter(ec.buildEditForm())
        );
        skeleton.setCenter(editController.buildEditForm());
        //TODO: Patienten bearbeiten
    }

    private void deletePatient() {
        deleteEntity(
                controller.getDatabase().getAllPatients(),
                patientPopulator::getDisplayName,
                patient -> controller.getDatabase().deletePatient(patient),
                messages.get("msg.noPatients"),
                messages.get("msg.patientDeleted"),
            messages.get("menu.patient"),
            messages.get("label.selectPatient"));
    }

    private void deleteServiceProvider() {
        deleteEntity(
                controller.getDatabase().getAllServiceProviders(),
                serviceProviderPopulator::getDisplayName,
                sp -> controller.getDatabase().deleteServiceProvider(sp),
                messages.get("msg.noServiceProviders"),
                messages.get("msg.selfDeleted"),
            messages.get("menu.self"),
            messages.get("label.selectServiceProvider"));
    }

    private <T> void deleteEntity(
            List<T> entities,
            Function<T, String> displayNameProvider,
            Consumer<T> deleteAction,
            String emptyMessage,
            String successMessage,
            String entityLabel,
            String selectionLabel) {
        if (entities == null || entities.isEmpty()) {
            showInfoDialog(messages.get("dialog.info.title"), emptyMessage);
            return;
        }

        List<String> displayNames = new ArrayList<>();
        Map<String, T> entityByDisplayName = new LinkedHashMap<>();
        for (T entity : entities) {
            String displayName = displayNameProvider.apply(entity);
            displayNames.add(displayName);
            entityByDisplayName.put(displayName, entity);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(displayNames.get(0), displayNames);
        dialog.setTitle(messages.get("msg.deleteConfirmTitle"));
        dialog.setHeaderText(messages.get("msg.deleteConfirmHeader"));
        dialog.setContentText(selectionLabel);

        Optional<String> selection = dialog.showAndWait();
        if (selection.isEmpty()) {
            return;
        }

        String chosenDisplayName = selection.get();
        T entity = entityByDisplayName.get(chosenDisplayName);
        if (entity == null) {
            showErrorDialog(messages.get("dialog.error.title"), "Ausgewählter Eintrag konnte nicht gefunden werden.");
            return;
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle(messages.get("msg.deleteConfirmTitle"));
        confirm.setHeaderText(messages.get("msg.deleteConfirmHeader"));
        confirm.setContentText(String.format(messages.get("msg.deleteConfirmBody"), chosenDisplayName));

        Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                deleteAction.accept(entity);
                showInfoDialog(messages.get("dialog.info.title"), successMessage);
            } catch (Exception e) {
                showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
            }
        }
    }

    /**
     * Öffnet das Formular zur Erstellung eines neuen Patienten.
     */
    private void createPerson() {
        showCreatePersonForm(messages.get("title.patient.new"), false);
    }

    /**
     * Öffnet das Formular zur Erstellung eines neuen Service Providers
     * (Selbst).
     */
    private void createSelfPerson() {
        showCreatePersonForm(messages.get("title.self.new"), true);
    }

    /**
     * Zeigt das Formular zur Erstellung eines neuen Patienten oder Service
     * Providers an.
     *
     * @param titleText Der Titel des Formulars
     * @param createServiceProvider Ob ein Service Provider (Selbst) oder ein
     * Patient erstellt werden soll
     */
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

    /**
     * Speichert einen neuen Patienten oder Service Provider basierend auf den
     * eingegebenen Daten.
     *
     * @param inputFields Die Map der Eingabefelder mit ihren zugehörigen
     * UI-Komponenten
     * @param saveAsServiceProvider Ob die Daten als Service Provider (Selbst)
     * oder als Patient gespeichert werden sollen
     */
    private void savePerson(Map<String, Node> inputFields, boolean saveAsServiceProvider) {
        try {
            String firstname = getFieldText(inputFields.get("firstname"));
            String lastname = getFieldText(inputFields.get("lastname"));
            String street = getFieldText(inputFields.get("street"));
            String country = getFieldText(inputFields.get("country"));
            String housenumber = getFieldText(inputFields.get("housenumber"));
            int plz = Integer.parseInt(getFieldText(inputFields.get("plz")));
            int ik = Integer.parseInt(getFieldText(inputFields.get("ik")));
            int kassenIk = Integer.parseInt(getFieldText(inputFields.get("kassenIk")));
            LocalDate birthDate = null;
            Node birthNode = inputFields.get("birthDate");
            if (birthNode instanceof DatePicker dp) {
                birthDate = dp.getValue();
            }

            if (saveAsServiceProvider) {
                ServiceProvider sp = new ServiceProvider(firstname, lastname, street, country, housenumber, plz, ik, kassenIk, birthDate);
                controller.getDatabase().saveServiceProvider(sp);
            } else {
                Patient p = new Patient(firstname, lastname, street, country, housenumber, plz, ik, kassenIk, birthDate);
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

    /**
     * Erstellt ein Eingabefeld basierend auf der TagList-Konfiguration für ein
     * bestimmtes Feld.
     *
     * @param fieldName - der Name des Feldes, für das das Eingabefeld erstellt
     * werden soll
     * @param tagList - die TagList, die die Konfiguration für das Feld enthält,
     * einschließlich der InputOption und möglicher Modifier
     * @return das erstellte Node, das als Eingabefeld für das angegebene Feld
     * verwendet werden kann
     */
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
            case CODE ->
                componentFactory.createComboBox(false);
            case NUMBER_SUGGESTION ->
                componentFactory.createComboBox(true);
            case DATE, TIME ->
                componentFactory.createDatePicker();
            default ->
                componentFactory.createTextField();
        };
    }

    /**
     * Erstellt ein validiertes Spinner-Node mit einem Fehlerlabel, das die
     * Eingabe basierend auf dem Feldnamen, der InputOption und dem Java-Feldtyp
     * validiert.
     *
     * @param fieldName - der Name des Feldes, das validiert werden soll, z.B.
     * "plz" für Postleitzahl
     * @param spinner - der Spinner, der validiert werden soll
     * @param inputOption - die InputOption, die den Typ der Eingabe angibt,
     * z.B. NUMBER oder PERCENT
     * @param javaFieldType - der Java-Typ des Feldes, z.B. "Integer" oder
     * "BigDecimal", der für die Validierung berücksichtigt werden kann
     * @return ein Node, das den Spinner und ein Fehlerlabel enthält, das die
     * Validierungsergebnisse anzeigt
     */
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

    /**
     * Erstellt ein Eingabefeld basierend auf der InputOption und anderen
     * Parametern, die in der TagList definiert sind.
     *
     * @param spinner - der Spinner, der validiert werden soll
     * @param fieldName - der Name des Feldes, das validiert werden soll, z.B.
     * "plz" für Postleitzahl
     * @param errorLabel - das Label, das Fehlermeldungen anzeigt, wenn die
     * Validierung fehlschlägt
     * @param inputOption - die InputOption, die den Typ der Eingabe angibt,
     * z.B. NUMBER oder PERCENT
     * @param javaFieldType - der Java-Typ des Feldes, z.B. "Integer" oder
     * "BigDecimal", der für die Validierung berücksichtigt werden kann
     */
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

    /**
     * Wendet den MaxLengthModifier aus der TagList auf ein TextField an, um die
     * maximale Länge der Eingabe zu begrenzen.
     *
     * @param textField das TextField, auf das der MaxLengthModifier angewendet
     * werden soll
     * @param tagList die TagList, die die Modifier enthält, einschließlich des
     * MaxLengthModifier
     */
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

    /**
     * Gibt den Textwert eines UI-Elements zurück, abhängig von dessen Typ. Wenn
     * das Element in einer VBox verpackt ist, wird das erste Kind der VBox als
     * Ziel für die Textgewinnung verwendet. Unterstützt verschiedene
     * UI-Komponenten wie TextInputControl, Spinner, ComboBox und DatePicker, um
     * den entsprechenden Textwert zurückzugeben. Wenn der Typ des UI-Elements
     * nicht erkannt wird oder kein Text extrahiert werden kann, wird ein leerer
     * String zurückgegeben.
     *
     * @param node das UI-Element, aus dem der Textwert extrahiert werden soll,
     * z.B. ein TextField, Spinner, ComboBox oder DatePicker
     * @return der Textwert des UI-Elements oder ein leerer String, wenn kein
     * Text extrahiert werden kann
     */
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

    /**
     * Zeigt einen Informationsdialog mit dem angegebenen Titel und der
     * Nachricht an.
     *
     * @param title Der Titel des Informationsdialogs
     * @param message Die Nachricht, die im Informationsdialog angezeigt werden
     * soll
     */
    public void showInfoDialog(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Zeigt einen Fehlerdialog mit dem angegebenen Titel und der Nachricht an.
     *
     * @param title Der Titel des Fehlerdialogs
     * @param message Die Nachricht, die im Fehlerdialog angezeigt werden soll
     */
    public void showErrorDialog(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
