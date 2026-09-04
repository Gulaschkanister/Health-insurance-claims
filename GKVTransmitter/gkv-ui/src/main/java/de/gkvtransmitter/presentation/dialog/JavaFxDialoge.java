package de.gkvtransmitter.presentation.dialog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;

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

    @Override
    public <T> Optional<T> waehleAus(String titel, String text, List<T> eintraege,
            Function<T, String> anzeige) {
        if (eintraege == null || eintraege.isEmpty()) {
            return Optional.empty();
        }
        // Der Dialog arbeitet mit Zeichenketten; die Zuordnung zurueck auf den
        // Eintrag laeuft ueber diese Abbildung. Gleichnamige Eintraege wuerden
        // sich dabei ueberdecken - die Aufrufer nehmen die Kennnummer mit in
        // den Anzeigenamen auf, damit das nicht vorkommt.
        List<String> namen = new ArrayList<>();
        Map<String, T> nachName = new LinkedHashMap<>();
        for (T eintrag : eintraege) {
            String name = anzeige.apply(eintrag);
            namen.add(name);
            nachName.put(name, eintrag);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(namen.get(0), namen);
        dialog.setTitle(titel);
        dialog.setHeaderText(text);
        dialog.setContentText(text);
        return dialog.showAndWait().map(nachName::get);
    }

    @Override
    public boolean bestaetige(String titel, String kopfzeile, String text) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(titel);
        alert.setHeaderText(kopfzeile);
        alert.setContentText(text);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void zeige(AlertType art, String titel, String nachricht) {
        Alert alert = new Alert(art);
        alert.setTitle(titel);
        alert.setHeaderText(titel);
        alert.setContentText(nachricht);
        alert.showAndWait();
    }
}
