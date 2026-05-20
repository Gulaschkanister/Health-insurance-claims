package de.gkvtransmitter.presentation.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.gkvtransmitter.presentation.UiFactory;
import de.gkvtransmitter.util.AppMessages;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * FormBuilder vereinheitlicht die Erstellung von Formularkomponenten.
 *
 * Diese Klasse kuemmert sich um:
 * - Konsistente Anordnung von Formularelementen
 * - Styling und Layout
 * - Button-Verwaltung
 * - ScrollPane-Handling
 */
public class FormBuilder {
    private final UiFactory componentFactory;
    private final AppMessages messages;
    private String title;
    private final String titleStyleClass = "-fx-font-size: 18; -fx-font-weight: bold;";
    private final List<FormField> fields = new ArrayList<>();
    private final List<FormButton> buttons = new ArrayList<>();
    private double fieldSpacing = 10.0;
    private double padding = 20.0;
    private int gridColumns = 2;

    public FormBuilder(UiFactory componentFactory, AppMessages messages) {
        this.componentFactory = componentFactory;
        this.messages = messages;
    }

    /**
     * Setzt den Formulartitel.
     */
    public FormBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Fügt ein Feld zum Formular hinzu.
     *
     * @param labelKey Der Nachrichtenschlüssel für das Label
     * @param field Das UI-Feld
     * @return this für Fluent API
     */
    public FormBuilder addField(String labelKey, Node field) {
        String label = messages.get(labelKey);
        fields.add(new FormField(label, field));
        return this;
    }

    /**
     * Fügt ein Feld mit benutzerdefinierten Label hinzu.
     */
    public FormBuilder addField(String label, Node field, boolean useMessageKey) {
        String resolvedLabel = useMessageKey ? messages.get(label) : label;
        fields.add(new FormField(resolvedLabel, field));
        return this;
    }

    /**
     * Fügt einen Button zum Formular hinzu.
     *
     * @param label Der Button-Text
     * @param onAction Der Click-Handler
     * @return this für Fluent API
     */
    public FormBuilder addButton(String label, Runnable onAction) {
        buttons.add(new FormButton(label, onAction));
        return this;
    }

    /**
     * Setzt die Anzahl der Grid-Spalten.
     */
    public FormBuilder withGridColumns(int columns) {
        this.gridColumns = columns;
        return this;
    }

    /**
     * Setzt den Abstand zwischen Feldern.
     */
    public FormBuilder withFieldSpacing(double spacing) {
        this.fieldSpacing = spacing;
        return this;
    }

    /**
     * Setzt den Padding des Formulars.
     */
    public FormBuilder withPadding(double padding) {
        this.padding = padding;
        return this;
    }

    /**
     * Baut das Formular und gibt es als ScrollPane zurück.
     *
     * @return Das vollständig konstruierte Formular
     */
    public ScrollPane build() {
        VBox mainVBox = new VBox(fieldSpacing);
        mainVBox.setPadding(new Insets(padding));

        // Titel
        if (title != null && !title.isBlank()) {
            Label titleLabel = componentFactory.createLabel(title);
            titleLabel.setStyle(titleStyleClass);
            mainVBox.getChildren().add(titleLabel);
        }

        // Felder in Grid
        List<Node> fieldNodes = new ArrayList<>();
        for (FormField field : fields) {
            Node fieldContainer = componentFactory.createBorderPane(
                    componentFactory.createLabel(field.label),
                    field.component,
                    null, null, null);
            fieldNodes.add(fieldContainer);
        }

        if (!fieldNodes.isEmpty()) {
            GridPane contentGrid = componentFactory.createGridPane(gridColumns,
                    fieldNodes.toArray(Node[]::new));
            mainVBox.getChildren().add(contentGrid);
        }

        // Buttons
        if (!buttons.isEmpty()) {
            HBox buttonBox = buildButtonBox();
            mainVBox.getChildren().add(buttonBox);
        }

        ScrollPane scrollPane = new ScrollPane(mainVBox);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    /**
     * Baut die Button-Leiste.
     */
    private HBox buildButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));

        for (FormButton btn : buttons) {
            Button button = componentFactory.createButton(btn.label);
            button.setStyle("-fx-padding: 10; -fx-font-size: 14;");
            button.setOnAction(event -> btn.onAction.run());
            buttonBox.getChildren().add(button);
        }

        return buttonBox;
    }

    /**
     * Gibt alle Formularfelder als Map mit ihren Labels zurück.
     */
    public Map<String, Node> getFieldMap() {
        Map<String, Node> map = new java.util.LinkedHashMap<>();
        for (FormField field : fields) {
            map.put(field.label, field.component);
        }
        return map;
    }

    /**
     * Gibt alle Formularfeld-Komponenten zurück.
     */
    public List<Node> getFieldComponents() {
        return fields.stream().map(f -> f.component).toList();
    }

    /**
     * Innere Klasse für ein Formularfeld.
     */
    private static class FormField {
        String label;
        Node component;

        FormField(String label, Node component) {
            this.label = label;
            this.component = component;
        }
    }

    /**
     * Innere Klasse für einen Button.
     */
    private static class FormButton {
        String label;
        Runnable onAction;

        FormButton(String label, Runnable onAction) {
            this.label = label;
            this.onAction = onAction;
        }
    }
}
