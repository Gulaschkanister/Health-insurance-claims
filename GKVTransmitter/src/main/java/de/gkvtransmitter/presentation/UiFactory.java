package de.gkvtransmitter.presentation;

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
import javafx.scene.layout.GridPane;

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
     * Erstellt eine Scene mit den vorgegebenen Dimensionen.
     */
    Scene createScene(Parent root, double width, double height);

    /**
     * Erstellt einen vorkonfigurierten Fehlerdialog.
     */
    Alert createErrorAlert(String title, String message);

    BorderPane createBorderPane(Node topProperty, Node centerProperty, Node bottomProperty, Node leftProperty,
            Node rightProperty);

    Button createButton(String text);

    ComboBox<Node> createDropdown(Node... options);

    DatePicker createDatePicker();

    CheckBox createCheckBox(String text);

    ToolBar createToolBar(Node... buttons);

    ScrollBar createScrollBar(Orientation orientation);

    MenuBar createMenuBar(Menu... menus);

    Menu createMenu(String text, MenuItem... subMenus);

    MenuItem createMenuItem(String text);

    GridPane createGridPane(int columns, Node... nodes);

    TextField createTextField();

}
