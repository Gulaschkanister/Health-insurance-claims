package de.gkvtransmitter.presentation;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
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

    /**
     * Erstellt ein GridPane mit der angegebenen Anzahl von Spalten und den übergebenen Knoten.
     * @param columns Die Anzahl der Spalten im GridPane
     * @param nodes Die Knoten, die im GridPane platziert werden sollen
     * @return Das erstellte GridPane mit den angegebenen Knoten
     */
    GridPane createGridPane(int columns, Node... nodes);

    TextField createTextField();

    /**
     * Erstellt einen Zaehler fuer den angegebenen Zahlentyp.
     *
     * <p>Die Art des Bedienelements ergibt sich allein aus dem Typ: ganze
     * Zahlen bekommen einen Schrittweite-1-Zaehler, Betraege einen mit
     * Hundertstelschritten. Frueher nahm die Methode zusaetzlich ein
     * Anzeigeformat und eine {@code InputOption} entgegen - beide wurden im
     * Rumpf nie gelesen, und saemtliche Aufrufer uebergaben {@code null}
     * beziehungsweise einen Wert, der schon im Typ steckte.</p>
     *
     * @param type {@code Integer} oder {@code BigDecimal}
     */
    <T> Spinner<T> createSpinner(Class<T> type);

    ComboBox<Node> createComboBox(boolean setEditable, Node... options);
}
