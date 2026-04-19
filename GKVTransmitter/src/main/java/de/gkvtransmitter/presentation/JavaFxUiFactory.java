package de.gkvtransmitter.presentation;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
    public StackPane createStackPane(Label label) {
        return new StackPane(label);
    }

    @Override
    public Scene createScene(StackPane root, double width, double height) {
        return new Scene(root, width, height);
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
    public <T> ComboBox<T> createDropdown(T... options) {
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
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
    public Pane createPane() {
        return new Pane();
    }

    @Override
    public HBox createHBox(double spacing) {
        return new HBox(spacing);
    }

    @Override
    public VBox createVBox(double spacing) {
        return new VBox(spacing);
    }
}
