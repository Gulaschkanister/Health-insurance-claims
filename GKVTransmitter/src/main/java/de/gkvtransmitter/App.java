package de.gkvtransmitter;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Startet die JavaFX-Anwendung und haengt spaeter die UI-Wizard-Schicht an den
 * fachlichen Kern an.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("GKVTransmitter Architekturgeruest geladen");
        StackPane root = new StackPane(label);

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("GKVTransmitter");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
