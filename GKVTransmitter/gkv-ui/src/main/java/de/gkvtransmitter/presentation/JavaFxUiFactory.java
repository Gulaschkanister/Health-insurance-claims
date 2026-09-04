package de.gkvtransmitter.presentation;

import java.math.BigDecimal;

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
     * Erstellt ein Kontrollkästchen mit der angegebenen Beschriftung.
     *
     * @param text Die Beschriftung neben dem Kästchen.
     * @return Das erstellte Kontrollkästchen.
     */
    @Override
    public CheckBox createCheckBox(String text) {
        return new CheckBox(text);
    }

    /**
     * Erstellt ein BorderPane mit den angegebenen Knoten in seinen fünf Feldern.
     *
     * @param topProperty Der Knoten oben, oder {@code null}.
     * @param centerProperty Der Knoten in der Mitte, oder {@code null}.
     * @param bottomProperty Der Knoten unten, oder {@code null}.
     * @param leftProperty Der Knoten links, oder {@code null}.
     * @param rightProperty Der Knoten rechts, oder {@code null}.
     * @return Das erstellte BorderPane.
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
     * Erstellt ein TextField.
     *
     * @return Der erstellte TextField.
     */
    @Override
    public TextField createTextField() {
        return new TextField();
    }

    /**
     * Erstellt ein GridPane mit den angegebenen Nodes.
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
     * Erstellt einen Zähler für den angegebenen Zahlentyp.
     *
     * @param type Der Typ des Zählers.
     * @return Der erstellte Zähler.
     */
    @Override
    public <T> Spinner<T> createSpinner(Class<T> type) {
        if (Integer.class.equals(type)) {
            Spinner<Integer> spinner = new Spinner<>(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            Integer.MIN_VALUE, Integer.MAX_VALUE, 0));
            spinner.setEditable(true);
            spinner.setPrefWidth(300);
            @SuppressWarnings("unchecked")
            Spinner<T> casted = (Spinner<T>) spinner;
            return casted;
        }
        if (BigDecimal.class.equals(type)) {
            // Create a BigDecimal spinner with 0.01 step
            SpinnerValueFactory<BigDecimal> vf = new SpinnerValueFactory<BigDecimal>() {
                private final BigDecimal schritt = new BigDecimal("0.01");

                {
                    setValue(BigDecimal.ZERO);
                }

                @Override
                public void decrement(int steps) {
                    setValue(getValue().subtract(schritt.multiply(BigDecimal.valueOf(steps))));
                }

                @Override
                public void increment(int steps) {
                    setValue(getValue().add(schritt.multiply(BigDecimal.valueOf(steps))));
                }
            };
            Spinner<BigDecimal> spinner = new Spinner<>(vf);
            spinner.setEditable(true);
            spinner.setPrefWidth(300);
            @SuppressWarnings("unchecked")
            Spinner<T> casted = (Spinner<T>) spinner;
            return casted;
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    /**
     * Erstellt eine ComboBox mit den angegebenen Nodes als Optionen.
     *
     * @param setEditable Ob die ComboBox editierbar sein soll.
     * @param nodes Die Optionen für die ComboBox.
     * @return Die erstellte ComboBox.
     */
    @Override
    public ComboBox<Node> createComboBox(boolean setEditable, Node... nodes) {
        ComboBox<Node> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(nodes);
        comboBox.setEditable(setEditable);
        return comboBox;
    }

}
