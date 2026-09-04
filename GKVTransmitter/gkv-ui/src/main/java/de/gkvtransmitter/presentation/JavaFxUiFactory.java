package de.gkvtransmitter.presentation;

import java.math.BigDecimal;

import de.gkvtransmitter.enums.InputOption;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

/**
 * JavaFX-spezifische Umsetzung der UiFactory.
 *
 * Kapselt die direkte Verwendung von JavaFX-Klassen, damit die View nur gegen
 * die abstrakte Factory arbeitet.
 */
public class JavaFxUiFactory implements UiFactory {

    /**
     * Erstellt ein Label mit dem angegebenen Text.
     *
     * @param text Der Text für das Label.
     * @return Das erstellte Label.
     */
    @Override
    public Label createLabel(String text) {
        return new Label(text);
    }

    /**
     * Erstellt eine Scene mit dem angegebenen Root-Knoten und den Dimensionen.
     *
     * @param root Der Root-Knoten der Scene.
     * @param width Die Breite der Scene.
     * @param height Die Höhe der Scene.
     * @return Die erstellte Scene.
     */
    @Override
    public Scene createScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        return scene;
    }

    /**
     * Erstellt einen Button mit dem angegebenen Text.
     *
     * @param text Der Text für den Button.
     * @return Der erstellte Button.
     */
    @Override
    public Button createButton(String text) {
        return new Button(text);
    }

    /**
     * Erstellt ein DatePicker.
     *
     * @return Der erstellte DatePicker.
     */
    @Override
    public DatePicker createDatePicker() {
        return new DatePicker();
    }

    /**
     * * Erstellt eine ComboBox mit den angegebenen Nodes als Optionen.
     *
     * @param setEditable Ob die ComboBox editierbar sein soll.
     * @param nodes Die Optionen für die ComboBox.
     * @return Die erstellte ComboBox.
     */
    @Override
    public CheckBox createCheckBox(String text) {
        return new CheckBox(text);
    }

    /**
     * * Erstellt eine ComboBox mit den angegebenen Nodes als Optionen.
     *
     * @param setEditable Ob die ComboBox editierbar sein soll.
     * @param nodes Die Optionen für die ComboBox.
     * @return Die erstellte ComboBox.
     */
    @Override
    public BorderPane createBorderPane(Node topProperty, Node centerProperty, Node bottomProperty, Node leftProperty,
            Node rightProperty) {
        BorderPane borderPane = new BorderPane();
        borderPane.topProperty().set(topProperty);
        borderPane.centerProperty().set(centerProperty);
        borderPane.bottomProperty().set(bottomProperty);
        borderPane.leftProperty().set(leftProperty);
        borderPane.rightProperty().set(rightProperty);
        return borderPane;
    }

    /**
     * * Erstellt ein TextField.
     *
     * @return Der erstellte TextField.
     */
    @Override
    public TextField createTextField() {
        return new TextField();
    }

    /**
     * * Erstellt ein GridPane mit den angegebenen Nodes.
     *
     * @param columns Die Anzahl der Spalten.
     * @param nodes Die Nodes für das GridPane.
     * @return Das erstellte GridPane.
     */
    @Override
    public GridPane createGridPane(int columns, Node... nodes) {
        GridPane gridPane = new GridPane();

        // Abstände zwischen Cells
        gridPane.setHgap(50);
        gridPane.setVgap(10);

        // Alle Spalten gleich breit
        for (int i = 0; i < columns; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / columns);
            gridPane.getColumnConstraints().add(col);
        }

        // Nodes hinzufügen
        for (int i = 0; i < nodes.length; i++) {
            gridPane.add(nodes[i], i % columns, i / columns);

        }

        return gridPane;
    }

    /**
     * * Erstellt einen Spinner mit dem angegebenen Typ und Optionen.
     *
     * @param type Der Typ des Spinners.
     * @param formatType Der Format-Typ für die Anzeige.
     * @param inputOption Die Eingabe-Optionen für den Spinner.
     * @return Der erstellte Spinner.
     */
    @Override
    public <T> Spinner<T> createSpinner(Class<T> type, String formatType, InputOption inputOption) {
        // TODO: formatTyoe aktuell ungenutzt ziel für die ui anzeige formatieren von
        // anzeigen
        if (Integer.class.equals(type)) {
            Spinner<Integer> spinner = new Spinner<>(
                    new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(
                            Integer.MIN_VALUE, Integer.MAX_VALUE, 0));
            spinner.setEditable(true);
            spinner.setPrefWidth(300);
            @SuppressWarnings("unchecked")
            Spinner<T> casted = (Spinner<T>) spinner;
            return casted;
        }
        if (BigDecimal.class.equals(type)) {
            // Create a BigDecimal spinner with 0.01 step
            SpinnerValueFactory<java.math.BigDecimal> vf = new SpinnerValueFactory<java.math.BigDecimal>() {
                private final java.math.BigDecimal STEP = new java.math.BigDecimal("0.01");

                {
                    setValue(java.math.BigDecimal.ZERO);
                }

                @Override
                public void decrement(int steps) {
                    setValue(getValue().subtract(STEP.multiply(java.math.BigDecimal.valueOf(steps))));
                }

                @Override
                public void increment(int steps) {
                    setValue(getValue().add(STEP.multiply(java.math.BigDecimal.valueOf(steps))));
                }
            };
            Spinner<java.math.BigDecimal> spinner = new Spinner<>(vf);
            spinner.setEditable(true);
            spinner.setPrefWidth(300);
            @SuppressWarnings("unchecked")
            Spinner<T> casted = (Spinner<T>) spinner;
            return casted;
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    /**
     * * Erstellt eine ComboBox mit den angegebenen Nodes als Optionen.
     *
     * @param setEditable Ob die ComboBox editierbar sein soll.
     * @param nodes Die Optionen für die ComboBox.
     * @return Die erstellte ComboBox.
     */
    @Override
    public ComboBox<Node> createComboBox(boolean setEditable, Node... nodes) {
        // TODO: hier gehts weiter
        ComboBox<Node> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(nodes);
        comboBox.setEditable(setEditable);
        return comboBox;
    }

}
