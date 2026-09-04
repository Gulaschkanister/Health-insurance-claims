package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.application.AbrechnungService;
import de.gkvtransmitter.dispatch.DispatchBatch;
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
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.CheckBox;
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
    private final AbrechnungService abrechnungService;

    public View(Controller controller, AbrechnungService abrechnungService) {
        this.controller = controller;
        this.componentFactory = new JavaFxUiFactory();
        this.messages = new AppMessages("/messages/ui-messages.json");
        this.objectMapper = new ObjectMapper();
        this.invoiceCodeOptions = loadInvoiceCodeOptions();
        this.patientPopulator = new PatientFieldPopulator();
        this.serviceProviderPopulator = new ServiceProviderFieldPopulator();
        this.abrechnungService = java.util.Objects.requireNonNull(abrechnungService,
            "abrechnungService must not be null");
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
        Scene scene = componentFactory.createScene(skeleton, width, height);
        Platform.runLater(this::seedIfEmpty);
        return scene;
    }

    /** Systemeigenschaft, mit der sich das Anlegen von Testdaten einschalten laesst. */
    private static final String TESTDATEN_PROPERTY = "gkv.testdaten";

    /**
     * Legt Testdaten an, sofern das ausdruecklich eingeschaltet wurde.
     *
     * <p>Bisher lief das bei jedem Start ungefragt: waren keine Gruppen oder
     * Blaupausen vorhanden, wurden "Max Muster" und drei erfundene Patientinnen
     * in die Datenbank geschrieben. Zusammen mit dem damaligen
     * {@code hbm2ddl.auto=create}, das die Datenbank bei jedem Start leerte,
     * war die Bedingung praktisch immer erfuellt - die Testdaten landeten also
     * verlaesslich in der Produktivdatenbank.</p>
     *
     * <p>Der Menuepunkt unter "Dev" legt die Daten weiterhin auf Wunsch an.
     * Automatisch geschieht das nur noch mit
     * {@code -Dgkv.testdaten=true}.</p>
     */
    private void seedIfEmpty() {
        if (!Boolean.parseBoolean(System.getProperty(TESTDATEN_PROPERTY, "false"))) {
            return;
        }
        try {
            boolean noGroups = controller.getDatabase().getAllPersonGroups().isEmpty();
            boolean noBlue = controller.getDatabase().getAllBlueprints().isEmpty();
            if (noGroups || noBlue) {
                seedTestData();
            }
        } catch (RuntimeException e) {
            // Ein Fehler beim Anlegen der Testdaten darf den Start nicht verhindern.
            System.err.println("Anlegen der Testdaten fehlgeschlagen: " + e.getMessage());
        }
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

        menuBuilder.addGroupItem(messages.get("menu.new"), this::createGroup);
        menuBuilder.addGroupItem(messages.get("menu.edit"), this::editGroup);
        menuBuilder.addGroupItem(messages.get("menu.delete"), this::deleteGroup);

        MenuBar menuBar = menuBuilder.build();

        // Add settlement (Abrechnung) as a single top-level menu (click to open panel)
        javafx.scene.control.Menu settlementMenu = componentFactory.createMenu(messages.get("menu.settlement"));
        javafx.scene.control.MenuItem openSettlement = componentFactory.createMenuItem(messages.get("menu.settlement"));
        openSettlement.setOnAction(ev -> createAbrechnung());
        settlementMenu.getItems().add(openSettlement);
        // trigger the item immediately when the top-level menu is activated (single-click behaviour)
        settlementMenu.setOnShowing(ev -> {
            try {
                openSettlement.fire();
            } finally {
                settlementMenu.hide();
            }
            ev.consume();
        });
        menuBar.getMenus().add(settlementMenu);

        // Dev menu: seed test data
        javafx.scene.control.Menu devMenu = componentFactory.createMenu("Dev");
        javafx.scene.control.MenuItem seedItem = componentFactory.createMenuItem("Seed Test Data");
        seedItem.setOnAction(ev -> seedTestData());
        devMenu.getItems().add(seedItem);
        menuBar.getMenus().add(devMenu);

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
        List<PersonGroup> groups = controller.getDatabase().getAllPersonGroups();

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

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
            // no default selection for blueprint
        }

        // Group selector (optional)
        Label groupLabel = componentFactory.createLabel(messages.get("label.selectGroupForSettlement"));
        ComboBox<PersonGroup> groupCombo = new ComboBox<>();
        groupCombo.setPrefWidth(400);
        if (groups != null && !groups.isEmpty()) {
            groupCombo.getItems().addAll(groups);
            groupCombo.setConverter(new javafx.util.StringConverter<PersonGroup>() {
                @Override
                public String toString(PersonGroup object) {
                    return object == null ? "" : object.getName();
                }

                @Override
                public PersonGroup fromString(String string) {
                    return null;
                }
            });
            // no default selection for group
        }

        // Note: Dienstleister-Dropdown removed; Dienstleister werden in der Tabelle angezeigt

        // no quick-selects or role filters; show tables for group members

        // Combined members: providers + patients tables
        Label patsLabel = componentFactory.createLabel(messages.get("label.selectPatients"));
        GridPane providerGrid = new GridPane();
        providerGrid.setHgap(8);
        providerGrid.setVgap(6);
        Label provHeader = componentFactory.createLabel("Dienstleister");

        GridPane patientGrid = new GridPane();
        patientGrid.setHgap(8);
        patientGrid.setVgap(6);
        Label patHeader = componentFactory.createLabel("Patienten");

        List<javafx.scene.control.CheckBox> providerBoxes = new ArrayList<>();
        List<ServiceProvider> providerEntities = new ArrayList<>();

        List<javafx.scene.control.CheckBox> patientBoxes = new ArrayList<>();
        List<Spinner<Integer>> patientSpinners = new ArrayList<>();
        List<Patient> patientEntities = new ArrayList<>();

        // helper to (re)build checklist from group or all lists
        VBox membersBox = new VBox(8);

        Runnable buildChecklist = () -> {
            membersBox.getChildren().clear();
            providerBoxes.clear();
            providerEntities.clear();
            patientBoxes.clear();
            patientSpinners.clear();
            patientEntities.clear();

            List<Patient> sourcePatients = new ArrayList<>();
            List<ServiceProvider> sourceProviders = new ArrayList<>();

            if (groupCombo.getValue() != null) {
                PersonGroup sel = groupCombo.getValue();
                if (sel.getPatients() != null) sourcePatients.addAll(sel.getPatients());
                if (sel.getServiceProviders() != null) sourceProviders.addAll(sel.getServiceProviders());
            }

            // provider table header
            providerGrid.getChildren().clear();
            providerGrid.add(componentFactory.createLabel("Auswählen"), 0, 0);
            providerGrid.add(componentFactory.createLabel("Name"), 1, 0);
            providerGrid.add(componentFactory.createLabel("ID"), 2, 0);
            int prow = 1;
            for (ServiceProvider prov : sourceProviders) {
                CheckBox cb = componentFactory.createCheckBox("");
                javafx.scene.control.Label name = componentFactory.createLabel(prov.getFirstname() + " " + prov.getLastname());
                javafx.scene.control.Label idLbl = componentFactory.createLabel(String.valueOf(prov.getId()));
                providerGrid.add(cb, 0, prow);
                providerGrid.add(name, 1, prow);
                providerGrid.add(idLbl, 2, prow);
                providerBoxes.add(cb);
                providerEntities.add(prov);
                prow++;
            }

            // patient table header and rows
            patientGrid.getChildren().clear();
            patientGrid.add(componentFactory.createLabel("Auswählen"), 0, 0);
            patientGrid.add(componentFactory.createLabel("Name"), 1, 0);
            patientGrid.add(componentFactory.createLabel("ID"), 2, 0);
            patientGrid.add(componentFactory.createLabel(messages.get("label.appointments")), 3, 0);
            int r = 1;
            for (Patient p : sourcePatients) {
                CheckBox cb = componentFactory.createCheckBox("");
                javafx.scene.control.Label name = componentFactory.createLabel(p.getFirstname() + " " + p.getLastname());
                javafx.scene.control.Label idLbl = componentFactory.createLabel(String.valueOf(p.getId()));
                Spinner<Integer> spinner = componentFactory.createSpinner(Integer.class, null, InputOption.NUMBER);
                spinner.setPrefWidth(100);
                spinner.getValueFactory().setValue(1);
                patientGrid.add(cb, 0, r);
                patientGrid.add(name, 1, r);
                patientGrid.add(idLbl, 2, r);
                patientGrid.add(spinner, 3, r);
                patientBoxes.add(cb);
                patientSpinners.add(spinner);
                patientEntities.add(p);
                r++;
            }

            if (providerEntities.isEmpty() && patientEntities.isEmpty()) {
                membersBox.getChildren().add(componentFactory.createLabel(messages.get("msg.noPatients")));
            } else {
                if (!providerEntities.isEmpty()) membersBox.getChildren().addAll(provHeader, providerGrid);
                if (!patientEntities.isEmpty()) membersBox.getChildren().addAll(patHeader, patientGrid);
            }
        };

        // initial build
        buildChecklist.run();

        // rebuild on group changes
        groupCombo.setOnAction(ev -> buildChecklist.run());

        // rebuild on group changes (single handler)
        // (filters/quick-selects removed)

        Button start = componentFactory.createButton(messages.get("button.startSettlement"));
        start.setOnAction(ev -> {
            if (blueprints == null || blueprints.isEmpty()) {
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noBlueprints"));
                return;
            }
            if (groupCombo.getValue() == null) {
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.selectGroupRequired"));
                return;
            }
            de.gkvtransmitter.entity.Blueprint chosen = bpCombo.getValue();

            List<Patient> selectedPatients = new ArrayList<>();
            Map<Integer, Integer> appointments = new LinkedHashMap<>();

            for (int i = 0; i < patientBoxes.size(); i++) {
                javafx.scene.control.CheckBox cb = patientBoxes.get(i);
                if (cb.isSelected()) {
                    Patient p = patientEntities.get(i);
                    selectedPatients.add(p);
                    Spinner<Integer> spn = patientSpinners.get(i);
                    Integer count = spn != null ? spn.getValue() : 0;
                    appointments.put(p.getId(), count != null ? count : 0);
                }
            }

            // For per-participant DTA: generate one DTA file per selected patient
            if (selectedPatients.isEmpty()) {
                showInfoDialog(messages.get("dialog.info.title"), "Keine Teilnehmer ausgewählt.");
                return;
            }

            PersonGroup selGroup = groupCombo.getValue();
            java.nio.file.Path outDir = java.nio.file.Paths.get("dta_output");
            java.util.List<DispatchBatch> batches;
            try {
                batches = abrechnungService.createAndDispatch(selectedPatients, selGroup, chosen, appointments, outDir);
            } catch (de.gkvtransmitter.dispatch.DtaValidierungsException e) {
                // Beanstandungen einzeln anzeigen: die Anwenderin soll alle auf
                // einmal sehen und nicht nach jeder Korrektur neu anstossen.
                zeigePruefbericht(e.getBericht());
                return;
            } catch (RuntimeException e) {
                showErrorDialog(messages.get("dialog.error.title"),
                        "Fehler beim Versand der DTA-Dateien: " + e.getMessage());
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d DTA-Batches erzeugt:\n", batches.size()));
            for (DispatchBatch batch : batches) {
                sb.append("Kassen-IK: ").append(batch.getKassenIk()).append('\n');
                for (java.nio.file.Path pth : batch.getFiles()) {
                    sb.append(pth.toString()).append('\n');
                }
            }
            showInfoDialog(messages.get("dialog.info.title"), sb.toString());
        });

        root.getChildren().addAll(title, bpLabel, bpCombo, groupLabel, groupCombo, patsLabel, membersBox, start);
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
        Patient selectedPatient = selectEntity(
            controller.getDatabase().getAllPatients(),
            patientPopulator::getDisplayName,
            messages.get("menu.patient"),
            messages.get("label.selectPatient"));
        if (selectedPatient == null) {
            return;
        }

        EditFormController<Patient> editController = new EditFormController<>(
                componentFactory,
                messages,
                patientPopulator,
            () -> List.of(selectedPatient),
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
        ServiceProvider selectedServiceProvider = selectEntity(
            controller.getDatabase().getAllServiceProviders(),
            serviceProviderPopulator::getDisplayName,
            messages.get("menu.self"),
            messages.get("label.selectServiceProvider"));
        if (selectedServiceProvider == null) {
            return;
        }

        EditFormController<ServiceProvider> editController = new EditFormController<>(
                componentFactory,
                messages,
                serviceProviderPopulator,
            () -> List.of(selectedServiceProvider),
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
            messages.get("label.selectPatient"));
    }

    private void deleteServiceProvider() {
        deleteEntity(
                controller.getDatabase().getAllServiceProviders(),
                serviceProviderPopulator::getDisplayName,
                sp -> controller.getDatabase().deleteServiceProvider(sp),
                messages.get("msg.noServiceProviders"),
                messages.get("msg.selfDeleted"),
            messages.get("label.selectServiceProvider"));
    }

    private <T> void deleteEntity(
            List<T> entities,
            Function<T, String> displayNameProvider,
            Consumer<T> deleteAction,
            String emptyMessage,
            String successMessage,
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

    private void createGroup() {
        showGroupForm(null);
    }

    private void editGroup() {
        List<PersonGroup> groups = controller.getDatabase().getAllPersonGroups();
        if (groups == null || groups.isEmpty()) {
            showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noGroups"));
            return;
        }

        PersonGroup selectedGroup = selectEntity(groups, this::groupDisplayName, messages.get("menu.groups"), messages.get("label.selectGroup"));
        if (selectedGroup != null) {
            showGroupForm(selectedGroup);
        }
    }

    private void deleteGroup() {
        List<PersonGroup> groups = controller.getDatabase().getAllPersonGroups();
        if (groups == null || groups.isEmpty()) {
            showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.noGroups"));
            return;
        }

        PersonGroup selectedGroup = selectEntity(groups, this::groupDisplayName, messages.get("menu.groups"), messages.get("label.selectGroup"));
        if (selectedGroup == null) {
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(messages.get("msg.deleteConfirmTitle"));
        alert.setHeaderText(messages.get("msg.deleteConfirmHeader"));
        alert.setContentText(String.format(messages.get("msg.deleteConfirmBody"), groupDisplayName(selectedGroup)));

        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                controller.getDatabase().deletePersonGroup(selectedGroup);
                showInfoDialog(messages.get("dialog.info.title"), messages.get("msg.groupDeleted"));
            } catch (Exception e) {
                showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
            }
        }
    }

    private <T> T selectEntity(List<T> entities, Function<T, String> displayNameProvider, String title, String contentText) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        List<String> groupNames = new ArrayList<>();
        Map<String, T> entityByName = new LinkedHashMap<>();
        for (T entity : entities) {
            String display = displayNameProvider.apply(entity);
            groupNames.add(display);
            entityByName.put(display, entity);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(groupNames.get(0), groupNames);
        dialog.setTitle(title);
        dialog.setHeaderText(contentText);
        dialog.setContentText(contentText);

        Optional<String> selection = dialog.showAndWait();
        if (selection.isEmpty()) {
            return null;
        }

        return entityByName.get(selection.get());
    }

    private void showGroupForm(PersonGroup existingGroup) {
        PersonGroup group = existingGroup != null ? existingGroup : new PersonGroup();
        boolean editing = existingGroup != null;

        List<Patient> patients = controller.getDatabase().getAllPatients();
        List<ServiceProvider> serviceProviders = controller.getDatabase().getAllServiceProviders();

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label title = componentFactory.createLabel(editing ? messages.get("title.group.edit") : messages.get("title.group.new"));
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        TextField nameField = componentFactory.createTextField();
        nameField.setPrefWidth(400);
        nameField.setText(group.getName() != null ? group.getName() : "");

        List<CheckBox> patientBoxes = new ArrayList<>();
        VBox patientBox = new VBox(6, componentFactory.createLabel(messages.get("label.groupPatients")));
        if (patients == null || patients.isEmpty()) {
            patientBox.getChildren().add(componentFactory.createLabel(messages.get("msg.noPatients")));
        } else {
            for (Patient patient : patients) {
                CheckBox checkBox = componentFactory.createCheckBox(
                        patient.getFirstname() + " " + patient.getLastname() + " (ID: " + patient.getId() + ")");
                if (containsPatientId(group.getPatients(), patient.getId())) {
                    checkBox.setSelected(true);
                }
                patientBoxes.add(checkBox);
                patientBox.getChildren().add(checkBox);
            }
        }

        List<CheckBox> serviceProviderBoxes = new ArrayList<>();
        VBox serviceProviderBox = new VBox(6, componentFactory.createLabel(messages.get("label.groupServiceProviders")));
        if (serviceProviders == null || serviceProviders.isEmpty()) {
            serviceProviderBox.getChildren().add(componentFactory.createLabel(messages.get("msg.noServiceProviders")));
        } else {
            for (ServiceProvider serviceProvider : serviceProviders) {
                CheckBox checkBox = componentFactory.createCheckBox(
                        serviceProvider.getFirstname() + " " + serviceProvider.getLastname() + " (ID: " + serviceProvider.getId() + ")");
                if (containsServiceProviderId(group.getServiceProviders(), serviceProvider.getId())) {
                    checkBox.setSelected(true);
                }
                serviceProviderBoxes.add(checkBox);
                serviceProviderBox.getChildren().add(checkBox);
            }
        }

        HBox nameRow = new HBox(10, componentFactory.createLabel(messages.get("label.groupName")), nameField);

        Button saveButton = componentFactory.createButton(messages.get("button.save"));
        saveButton.setOnAction(event -> {
            String groupName = nameField.getText() != null ? nameField.getText().trim() : "";
            if (groupName.isBlank()) {
                showErrorDialog(messages.get("dialog.error.title"), "Bitte einen Gruppennamen eingeben.");
                return;
            }

            group.setName(groupName);
            group.setPatients(collectSelectedPatients(patients, patientBoxes));
            group.setServiceProviders(collectSelectedServiceProviders(serviceProviders, serviceProviderBoxes));

            try {
                controller.getDatabase().savePersonGroup(group);
                showInfoDialog(messages.get("dialog.info.title"),
                        editing ? messages.get("msg.groupUpdated") : messages.get("msg.groupCreated"));
                skeleton.setCenter(null);
            } catch (Exception e) {
                showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
            }
        });

        Button cancelButton = componentFactory.createButton(messages.get("button.cancel"));
        cancelButton.setOnAction(event -> skeleton.setCenter(null));

        HBox buttonRow = new HBox(10, saveButton, cancelButton);

        root.getChildren().addAll(title, nameRow, patientBox, serviceProviderBox, buttonRow);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        skeleton.setCenter(scrollPane);
    }

    private boolean containsPatientId(Set<Patient> selectedPatients, int id) {
        if (selectedPatients == null || selectedPatients.isEmpty()) {
            return false;
        }

        for (Patient selected : selectedPatients) {
            if (selected != null && selected.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private boolean containsServiceProviderId(Set<ServiceProvider> selectedServiceProviders, int id) {
        if (selectedServiceProviders == null || selectedServiceProviders.isEmpty()) {
            return false;
        }

        for (ServiceProvider selected : selectedServiceProviders) {
            if (selected != null && selected.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private Set<Patient> collectSelectedPatients(List<Patient> patients, List<CheckBox> patientBoxes) {
        Set<Patient> selectedPatients = new HashSet<>();
        for (int i = 0; i < patientBoxes.size(); i++) {
            if (patientBoxes.get(i).isSelected()) {
                selectedPatients.add(patients.get(i));
            }
        }
        return selectedPatients;
    }

    private Set<ServiceProvider> collectSelectedServiceProviders(List<ServiceProvider> serviceProviders,
            List<CheckBox> serviceProviderBoxes) {
        Set<ServiceProvider> selectedServiceProviders = new HashSet<>();
        for (int i = 0; i < serviceProviderBoxes.size(); i++) {
            if (serviceProviderBoxes.get(i).isSelected()) {
                selectedServiceProviders.add(serviceProviders.get(i));
            }
        }
        return selectedServiceProviders;
    }

    private String groupDisplayName(PersonGroup group) {
        return group.getName() + " (ID: " + group.getId() + ")";
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
    /**
     * Zeigt einen Pruefbericht mit allen Beanstandungen.
     *
     * <p>Fehler und Warnungen stehen getrennt, weil sie unterschiedliche
     * Bedeutung haben: Fehler haben den Versand aufgehalten, Warnungen sind
     * Hinweise. Ohne die Trennung waere aus der Liste nicht ersichtlich, was
     * behoben werden muss.</p>
     */
    private void zeigePruefbericht(de.gkvtransmitter.validator.ValidationReport bericht) {
        StringBuilder text = new StringBuilder();
        text.append("Die Abrechnung wurde nicht versendet.").append(System.lineSeparator());
        text.append(System.lineSeparator());

        if (!bericht.getErrors().isEmpty()) {
            text.append("Zu beheben:").append(System.lineSeparator());
            for (de.gkvtransmitter.validator.ValidationMessage befund : bericht.getErrors()) {
                text.append("  - ").append(befund.text());
                if (!befund.ort().isEmpty()) {
                    text.append("  [").append(befund.ort()).append(']');
                }
                text.append(System.lineSeparator());
            }
        }
        if (!bericht.getWarnings().isEmpty()) {
            text.append(System.lineSeparator()).append("Hinweise:").append(System.lineSeparator());
            for (de.gkvtransmitter.validator.ValidationMessage befund : bericht.getWarnings()) {
                text.append("  - ").append(befund.text()).append(System.lineSeparator());
            }
        }
        showErrorDialog("Pruefung nicht bestanden", text.toString());
    }

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

    /**
     * Dev helper: creates a test blueprint, a group with 3 participants and 1 provider
     */
    private void seedTestData() {
        try {
            // create service provider
            ServiceProvider prov = new ServiceProvider("Max", "Muster", "Musterstr.", "DE", "1", 12345, 1001, 2001, null);
            controller.getDatabase().saveServiceProvider(prov);

            // create participants
            Patient p1 = new Patient("Anna", "A", "Str1", "DE", "1", 11111, 101, 201, null);
            Patient p2 = new Patient("Bernd", "B", "Str2", "DE", "2", 22222, 102, 202, null);
            Patient p3 = new Patient("Clara", "C", "Str3", "DE", "3", 33333, 103, 203, null);
            controller.getDatabase().savePatient(p1);
            controller.getDatabase().savePatient(p2);
            controller.getDatabase().savePatient(p3);

            // create group
            PersonGroup group = new PersonGroup();
            group.setName("Testgruppe 1");
            java.util.Set<Patient> ps = new java.util.LinkedHashSet<>();
            ps.add(p1);
            ps.add(p2);
            ps.add(p3);
            group.setPatients(ps);
            java.util.Set<ServiceProvider> ss = new java.util.LinkedHashSet<>();
            ss.add(prov);
            group.setServiceProviders(ss);
            controller.getDatabase().savePersonGroup(group);

            // create simple blueprint
            String payload = "{\"template\":\"test\",\"fields\":{}}";
            Blueprint bp = new Blueprint("Test Blaupause", "test-template", payload, OffsetDateTime.now());
            controller.getDatabase().saveBlueprint(bp);

            showInfoDialog(messages.get("dialog.info.title"), "Testdaten angelegt: 1 Dienstleister, 3 Teilnehmer, 1 Gruppe, 1 Blaupause.");
        } catch (Exception e) {
            showErrorDialog(messages.get("dialog.error.title"), e.getMessage());
        }
    }
}
