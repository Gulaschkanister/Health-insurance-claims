package de.gkvtransmitter.presentation;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
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

    @Override
    public Label createLabel(String text) {
        return new Label(text);
    }

    @Override
    public Scene createScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        return scene;
    }

    @Override
    public Alert createErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("GKVTransmitter - Fehler");
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert;
    }

    @Override
    public Button createButton(String text) {
        return new Button(text);
    }

    @Override
    public ComboBox<Node> createDropdown(Node... nodes) {
        ComboBox<Node> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(nodes);
        return comboBox;
    }

    @Override
    public DatePicker createDatePicker() {
        return new DatePicker();
    }

    @Override
    public CheckBox createCheckBox(String text) {
        return new CheckBox(text);
    }

    @Override
    public ToolBar createToolBar(Node... nodes) {
        ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(nodes);
        return toolBar;
    }

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

    @Override
    public ScrollBar createScrollBar(Orientation orientation) {
        ScrollBar scrollBar = new ScrollBar();
        scrollBar.setOrientation(orientation);
        return scrollBar;
    }

    @Override
    public Menu createMenu(String text, MenuItem... subMenus) {
        Menu menu = new Menu(text);
        menu.getItems().addAll(subMenus);
        return menu;
    }

    @Override
    public MenuBar createMenuBar(Menu... nodes) {
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(nodes);
        return menuBar;
    }

    @Override
    public MenuItem createMenuItem(String text) {
        return new MenuItem(text);
    }

    @Override
    public TextField createTextField() {
        return new TextField();
    }

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
}
