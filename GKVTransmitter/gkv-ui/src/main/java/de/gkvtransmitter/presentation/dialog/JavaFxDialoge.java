package de.gkvtransmitter.presentation.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/** Zeigt die Meldungen als JavaFX-Dialog. */
public class JavaFxDialoge implements Dialoge {

    @Override
    public void zeigeInfo(String titel, String nachricht) {
        zeige(AlertType.INFORMATION, titel, nachricht);
    }

    @Override
    public void zeigeFehler(String titel, String nachricht) {
        zeige(AlertType.ERROR, titel, nachricht);
    }

    private void zeige(AlertType art, String titel, String nachricht) {
        Alert alert = new Alert(art);
        alert.setTitle(titel);
        alert.setHeaderText(titel);
        alert.setContentText(nachricht);
        alert.showAndWait();
    }
}
