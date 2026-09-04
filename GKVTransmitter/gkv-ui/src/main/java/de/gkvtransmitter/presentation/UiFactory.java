package de.gkvtransmitter.presentation;

import de.gkvtransmitter.enums.InputOption;
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
import javafx.scene.control.Spinner;
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

    /**
     * Erstellt ein BorderPane mit den angegebenen Knoten in den entsprechenden
     * Bereichen.
     *
     * @param topProperty Der Knoten, der im oberen Bereich des BorderPane
     * platziert werden soll
     * @param centerProperty Der Knoten, der im zentralen Bereich des BorderPane
     * platziert werden soll
     * @param bottomProperty Der Knoten, der im unteren Bereich des BorderPane
     * platziert werden soll
     * @param leftProperty Der Knoten, der im linken Bereich des BorderPane
     * platziert werden soll
     * @param rightProperty Der Knoten, der im rechten Bereich des BorderPane
     * platziert werden soll
     * @return Das erstellte BorderPane mit den angegebenen Knoten in den
     * entsprechenden Bereichen
     */
    BorderPane createBorderPane(Node topProperty, Node centerProperty, Node bottomProperty, Node leftProperty,
            Node rightProperty);

    Button createButton(String text);

    DatePicker createDatePicker();

    CheckBox createCheckBox(String text);

    ToolBar createToolBar(Node... buttons);

    ScrollBar createScrollBar(Orientation orientation);

    MenuBar createMenuBar(Menu... menus);

    Menu createMenu(String text, MenuItem... subMenus);

    MenuItem createMenuItem(String text);

    /**
     * Erstellt ein GridPane mit der angegebenen Anzahl von Spalten und den übergebenen Knoten.
     * @param columns Die Anzahl der Spalten im GridPane
     * @param nodes Die Knoten, die im GridPane platziert werden sollen
     * @return Das erstellte GridPane mit den angegebenen Knoten
     */
    GridPane createGridPane(int columns, Node... nodes);

    TextField createTextField();

    <T> Spinner<T> createSpinner(Class<T> type, String format, InputOption inputOption);

    ComboBox<Node> createComboBox(boolean setEditable, Node... options);
}
