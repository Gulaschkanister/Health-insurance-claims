package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.application.AbrechnungService;
import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.model.segment.SegmentInfo;
import de.gkvtransmitter.model.segment.ValueFieldEntry;
import de.gkvtransmitter.presentation.builder.MenuBuilder;
import de.gkvtransmitter.presentation.controller.EditFormController;
import de.gkvtransmitter.presentation.dialog.Dialoge;
import de.gkvtransmitter.presentation.dialog.JavaFxDialoge;
import de.gkvtransmitter.presentation.populator.PatientFieldPopulator;
import de.gkvtransmitter.presentation.populator.ServiceProviderFieldPopulator;
import de.gkvtransmitter.util.Anwendungsverzeichnis;
import de.gkvtransmitter.util.AppMessages;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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

    /** Alle Meldungen an den Anwender laufen hierueber, siehe {@link Dialoge}. */
    private final Dialoge dialoge = new JavaFxDialoge();

    /** Baut die Eingabefelder der Formulare und liest sie wieder aus. */
    private final Feldbau feldbau;

    /**
     * Der Platz, an dem die Masken erscheinen: die Mitte des Rahmens.
     *
     * <p>Das umhuellende {@code ScrollPane} entsteht erst hier. Die Masken
     * liefern den nackten Bereich, sonst waeren ihre Bedienelemente vor dem
     * ersten Zeichnen nicht auffindbar - und damit nicht pruefbar.</p>
     */
    private final Maskenrahmen rahmen = new Maskenrahmen() {
        @Override
        public void zeige(Region inhalt) {
            // Was schon scrollt, wird nicht ein zweites Mal eingehuellt:
            // EditFormController liefert bereits ein ScrollPane.
            if (inhalt instanceof ScrollPane) {
                skeleton.setCenter(inhalt);
                return;
            }
            ScrollPane scroll = new ScrollPane(inhalt);
            scroll.setFitToWidth(true);
            skeleton.setCenter(scroll);
        }

        @Override
        public void leeren() {
            skeleton.setCenter(null);
        }
    };

    public View(Controller controller, AbrechnungService abrechnungService) {
        this.controller = controller;
        this.componentFactory = new JavaFxUiFactory();
        this.messages = new AppMessages("/messages/ui-messages.json");
        this.objectMapper = new ObjectMapper();
        this.invoiceCodeOptions = loadInvoiceCodeOptions();
        this.feldbau = new Feldbau(this.componentFactory, this.messages);
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
    /**
     * Zeigt die Abrechnungsmaske.
     *
     * <p>Der Aufbau liegt in {@link AbrechnungsMaske}; hier bleibt nur das
     * Einhaengen in den Rahmen. Das umschliessende {@code ScrollPane} entsteht
     * erst hier, weil sein Inhalt sonst nicht durchsuchbar waere - die Maske
     * liefert deshalb den nackten Bereich.</p>
     */
    private void createAbrechnung() {
        AbrechnungsMaske maske = new AbrechnungsMaske(componentFactory, messages, dialoge,
                controller.getDatabase(), abrechnungService::createAndDispatch,
                Anwendungsverzeichnis::versandordner);
        rahmen.zeige(maske.erzeuge());
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
        personenMaske().teilnehmerBearbeiten();
    }

    private void editServiceProvider() {
        personenMaske().dienstleisterBearbeiten();
    }

    private void deletePatient() {
        personenMaske().teilnehmerLoeschen();
    }

    private void deleteServiceProvider() {
        personenMaske().dienstleisterLoeschen();
    }

    private void createPerson() {
        personenMaske().neuerTeilnehmer();
    }

    private void createSelfPerson() {
        personenMaske().neuerDienstleister();
    }

    /** Baut die Personenmaske auf den aktuellen Rahmen, siehe {@link #gruppenMaske()}. */
    private PersonenMaske personenMaske() {
        return new PersonenMaske(componentFactory, messages, dialoge, controller.getDatabase(), rahmen,
                feldbau, patientPopulator, serviceProviderPopulator);
    }

    private void createGroup() {
        gruppenMaske().neu();
    }

    private void editGroup() {
        gruppenMaske().bearbeiten();
    }

    private void deleteGroup() {
        gruppenMaske().loeschen();
    }

    /**
     * Baut die Gruppenmaske auf den aktuellen Rahmen.
     *
     * <p>Bewusst je Aufruf neu: die Maske haelt keinen Zustand ueber ihren
     * Aufbau hinaus, und so gibt es keine Gelegenheit, dass sie auf einen
     * veralteten Rahmen zeigt.</p>
     */
    private GruppenMaske gruppenMaske() {
        return new GruppenMaske(componentFactory, messages, dialoge, controller.getDatabase(), rahmen);
    }

    /**
     * Zeigt einen Informationsdialog an.
     *
     * <p>Bleibt oeffentlich, weil {@code App} bei einem Startfehler darauf
     * zurueckgreift, bevor es ueberhaupt eine Maske gibt.</p>
     */
    public void showInfoDialog(String title, String message) {
        dialoge.zeigeInfo(title, message);
    }

    /** Zeigt einen Fehlerdialog an. */
    public void showErrorDialog(String title, String message) {
        dialoge.zeigeFehler(title, message);
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
