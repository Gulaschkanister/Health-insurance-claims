package de.gkvtransmitter.presentation.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import de.gkvtransmitter.presentation.UiFactory;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.presentation.populator.EntityFieldPopulator;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Generischer Controller für die Bearbeitung von Entities.
 *
 * Dieser Controller vereinheitlicht die Logik für:
 * - Selection von Entities (Dropdown)
 * - Populierung von Formularen
 * - Speichern und Löschen
 *
 * Dadurch wird die Duplikation zwischen PatientEdit und ServiceProviderEdit eliminiert.
 *
 * @param <T> Der Entity-Typ
 */
public class EditFormController<T> {
    private final UiFactory componentFactory;
    private final AppMessages messages;
    private final Meldungen meldungen;
    private final EntityFieldPopulator<T> populator;
    private final Supplier<List<T>> entityLoader;
    private final Consumer<T> entitySaver;
    private final Consumer<T> entityDeleter;
    private final boolean showDeleteButton;
    private final Function<String, Node> fieldBuilder;
    private final String entityTypeName;
    private final Consumer<VBox> onFormReady;
    private final Consumer<EditFormController<T>> onChanged;

    private VBox formContainer;

    public EditFormController(
            UiFactory componentFactory,
            AppMessages messages,
            Meldungen meldungen,
            EntityFieldPopulator<T> populator,
            Supplier<List<T>> entityLoader,
            Consumer<T> entitySaver,
            Consumer<T> entityDeleter,
            boolean showDeleteButton,
            Function<String, Node> fieldBuilder,
            String entityTypeName,
            Consumer<VBox> onFormReady,
            Consumer<EditFormController<T>> onChanged) {
        this.componentFactory = componentFactory;
        this.messages = messages;
        this.meldungen = meldungen;
        this.populator = populator;
        this.entityLoader = entityLoader;
        this.entitySaver = entitySaver;
        this.entityDeleter = entityDeleter;
        this.showDeleteButton = showDeleteButton;
        this.fieldBuilder = fieldBuilder;
        this.entityTypeName = entityTypeName;
        this.onFormReady = onFormReady;
        this.onChanged = onChanged;
    }

    /**
     * Baut das Bearbeitungsformular.
     *
     * @return Das Formular als ScrollPane
     */
    public ScrollPane buildEditForm() {
        List<T> entities = entityLoader.get();

        if (entities.isEmpty()) {
            return createEmptyStatePane();
        }

        // Wenn nur ein Eintrag übergeben wird, direkt in den Editor springen.
        formContainer = new VBox(10);
        formContainer.setPadding(new Insets(20));
        ScrollPane formScrollPane = new ScrollPane(formContainer);
        formScrollPane.setFitToWidth(true);

        if (entities.size() == 1) {
            populateEditForm(entities.get(0));
            return formScrollPane;
        }

        // Selection-Panel
        VBox selectionPane = buildSelectionPane(entities);

        // Main Layout
        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(10));
        mainVBox.getChildren().addAll(selectionPane, new Separator(), formScrollPane);

        ScrollPane mainScrollPane = new ScrollPane(mainVBox);
        mainScrollPane.setFitToWidth(true);
        return mainScrollPane;
    }

    /**
     * Baut das Selection-Panel mit Dropdown.
     */
    private VBox buildSelectionPane(List<T> entities) {
        VBox selectionPane = new VBox(10);
        selectionPane.setPadding(new Insets(20));

        Label selectLabel = componentFactory.createLabel(
                messages.get("label.select" + entityTypeName));
        selectLabel.getStyleClass().add("feld-beschriftung");

        ComboBox<String> entityCombo = new ComboBox<>();
        for (T entity : entities) {
            entityCombo.getItems().add(populator.getDisplayName(entity));
        }
        entityCombo.setPrefWidth(300);

        Map<String, T> entityMap = new HashMap<>();
        for (T entity : entities) {
            entityMap.put(populator.getDisplayName(entity), entity);
        }

        entityCombo.setOnAction(event -> {
            String selected = entityCombo.getValue();
            if (selected != null) {
                T entity = entityMap.get(selected);
                if (entity != null) {
                    populateEditForm(entity);
                }
            }
        });

        selectionPane.getChildren().addAll(selectLabel, entityCombo);
        return selectionPane;
    }

    /**
     * Populiert das Formular mit Entity-Daten.
     */
    private void populateEditForm(T entity) {
        formContainer.getChildren().clear();

        Map<String, TagList> tagConfig = TagConfigLoader.loadTagConfig("/tags/person-tags.json");
        Map<String, Node> inputFields = new HashMap<>();

        List<Node> fieldNodes = new java.util.ArrayList<>();
        for (Map.Entry<String, TagList> entry : tagConfig.entrySet()) {
            String fieldName = entry.getKey();
            Node inputField = fieldBuilder.apply(fieldName);

            // Populiere mit Entity-Daten
            populator.populateField(inputField, fieldName, entity);
            inputFields.put(fieldName, inputField);

            fieldNodes.add(componentFactory.createBorderPane(
                    componentFactory.createLabel(fieldName),
                    inputField,
                    null, null, null));
        }

        // Buttons
        HBox buttonBox = buildActionButtons(entity, inputFields);

        // Layout
        Label title = componentFactory.createLabel(
                messages.get("title." + entityTypeName.toLowerCase() + ".edit") + ": "
                        + populator.getDisplayName(entity));
        title.getStyleClass().add("masken-titel");

        GridPane contentGrid = componentFactory.createGridPane(2,
                fieldNodes.toArray(Node[]::new));

        formContainer.getChildren().addAll(title, contentGrid, buttonBox);
        onFormReady.accept(formContainer);
    }

    /**
     * Baut die Action-Buttons (Speichern, Löschen, Abbrechen).
     */
    private HBox buildActionButtons(T entity, Map<String, Node> inputFields) {
        Button updateButton = componentFactory.createButton(messages.get("button.update"));
        updateButton.getStyleClass().add("schaltflaeche-haupt");
        updateButton.setOnAction(event -> saveEntity(entity, inputFields));

        Button cancelButton = componentFactory.createButton(messages.get("button.cancel"));
        cancelButton.getStyleClass().add("schaltflaeche-still");
        cancelButton.setOnAction(event -> formContainer.getChildren().clear());

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        if (showDeleteButton) {
            Button deleteButton = new Button(messages.get("button.delete"));
            deleteButton.getStyleClass().add("schaltflaeche-gefahr");
            deleteButton.setOnAction(event -> confirmDelete(entity));
            buttonBox.getChildren().addAll(updateButton, deleteButton, cancelButton);
        } else {
            buttonBox.getChildren().addAll(updateButton, cancelButton);
        }

        return buttonBox;
    }

    /**
     * Speichert die Entity mit Daten aus den Eingabefeldern.
     */
    private void saveEntity(T entity, Map<String, Node> inputFields) {
        try {
            for (Map.Entry<String, Node> entry : inputFields.entrySet()) {
                populator.extractToEntity(entry.getValue(), entry.getKey(), entity);
            }

            entitySaver.accept(entity);
            // Nach der Rolle benannt, so wie beim Anlegen: dort stand einmal in
            // beiden Faellen "Teilnehmer erfolgreich erstellt!", auch wenn ein
            // Dienstleister entstand. Beim Bearbeiten war es dieselbe Luecke -
            // "Erfolgreich gespeichert!" sagt nicht, was gespeichert wurde. Die
            // Schluessel dafuer lagen unbenutzt in ui-messages.json.
            meldungen.erfolg(messages.get(
                    "msg." + entityTypeName.toLowerCase(java.util.Locale.ROOT) + "Updated", "msg.saved"));
            onChanged.accept(this);
            formContainer.getChildren().clear();
        } catch (NumberFormatException e) {
            meldungen.hinweis(messages.get("msg.invalidNumbers"));
        } catch (Exception e) {
            meldungen.fehler(e.getMessage());
        }
    }

    /**
     * Fragt zurück und löscht dann.
     *
     * <p>Die Rückfrage nennt den Eintrag beim Namen. Zuvor stand dort nur
     * „Soll %s wirklich gelöscht werden?" mit unersetztem Platzhalter — der
     * Text wurde nie formatiert.</p>
     */
    private void confirmDelete(T entity) {
        meldungen.frageNach(
                String.format(messages.get("msg.deleteConfirmBody"), populator.getDisplayName(entity)),
                messages.get("button.delete"),
                () -> deleteEntity(entity));
    }

    private void deleteEntity(T entity) {
        try {
            entityDeleter.accept(entity);
        } catch (Exception e) {
            meldungen.fehler(e.getMessage());
            return;
        }
        meldungen.erfolg(messages.get("msg.deleted"));
        onChanged.accept(this);
        formContainer.getChildren().clear();
    }

    /**
     * Baut einen Empty-State-Pane, wenn keine Entities vorhanden sind.
     */
    private ScrollPane createEmptyStatePane() {
        VBox emptyPane = new VBox();
        emptyPane.setPadding(new Insets(20));
        Label emptyLabel = componentFactory.createLabel(
                messages.get("msg.no" + entityTypeName + "s"));
        emptyPane.getChildren().add(emptyLabel);
        ScrollPane scrollPane = new ScrollPane(emptyPane);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
}
