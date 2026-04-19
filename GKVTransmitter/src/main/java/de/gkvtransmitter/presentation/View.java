package de.gkvtransmitter.presentation;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * View-Schicht der Anwendung.
 *
 * Erzeugt die sichtbare Status-Ansicht und delegiert die konkrete Erstellung
 * einzelner JavaFX-Elemente an eine UI-Factory.
 */
public class View {

    private final UiFactory componentFactory;

    public View() {
        this(new JavaFxUiFactory());
    }

    public View(UiFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

    /**
     * Baut die Hauptszene mit einem einfachen Status-Text auf.
     */
    public Scene createStatusScene(String statusText, double width, double height) {
        Label label = componentFactory.createLabel(statusText);
        StackPane root = componentFactory.createStackPane(label);
        //TODO Hier gehts weiter Kopfleiste und danach Formular erstellen
        VBox vBox = componentFactory.createVBox(height);
        HBox hBox = componentFactory.createHBox(width);
        return componentFactory.createScene(root, width, height);
    }

    /**
     * Zeigt einen blockierenden Fehlerdialog fuer den Benutzer an.
     */
    public void showErrorDialog(String title, String message) {
        Alert alert = componentFactory.createErrorAlert(title, message);
        alert.showAndWait();
    }
}
