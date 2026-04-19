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
 * Abstraktion fuer die Erstellung zentraler UI-Elemente.
 *
 * Entkoppelt die View von konkreten JavaFX-Konstruktoren und erleichtert
 * spaetere Austauschbarkeit sowie Tests.
 */
public interface UiFactory {

    /**
     * Erstellt ein Text-Label.
     */
    Label createLabel(String text);

    /**
     * Erstellt einen einfachen Root-Container fuer den Inhalt.
     */
    StackPane createStackPane(Label label);

    /**
     * Erstellt eine Scene mit den vorgegebenen Dimensionen.
     */
    Scene createScene(StackPane root, double width, double height);

    /**
     * Erstellt einen vorkonfigurierten Fehlerdialog.
     */
    Alert createErrorAlert(String title, String message);

    VBox createVBox(double spacing);

    HBox createHBox(double spacing);

    Button createButton(String text);

    <T> ComboBox<T> createDropdown(T... options);

    DatePicker createDatePicker();

    CheckBox createCheckBox(String text);

    Pane createPane();

}
