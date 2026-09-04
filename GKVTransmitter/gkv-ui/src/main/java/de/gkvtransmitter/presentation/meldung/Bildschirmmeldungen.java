package de.gkvtransmitter.presentation.meldung;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import de.gkvtransmitter.presentation.Benachrichtigungen;
import de.gkvtransmitter.presentation.Benachrichtigungen.Art;
import de.gkvtransmitter.validator.ValidationReport;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Bringt die Meldungen in die Ecke oben rechts.
 *
 * <p>Auch die Rueckfrage und die Auswahl erscheinen dort - als Karte mit
 * Schaltflaechen, die stehen bleibt, bis entschieden ist. Damit oeffnet die
 * Anwendung im laufenden Betrieb kein einziges Fenster mehr.</p>
 */
public class Bildschirmmeldungen implements Meldungen {

    private final Benachrichtigungen ecke;

    public Bildschirmmeldungen(Benachrichtigungen ecke) {
        this.ecke = Objects.requireNonNull(ecke, "ecke must not be null");
    }

    @Override
    public void erfolg(String text) {
        ecke.zeige(Art.ERFOLG, text);
    }

    @Override
    public void hinweis(String text) {
        ecke.zeige(Art.HINWEIS, text);
    }

    @Override
    public void fehler(String text) {
        ecke.zeige(Art.FEHLER, text);
    }

    @Override
    public void pruefbericht(ValidationReport bericht) {
        VBox liste = new VBox(6);
        for (Pruefbefunde.Zeile zeile : Pruefbefunde.zeilen(bericht)) {
            liste.getChildren().add(befund(zeile));
        }
        ecke.zeigeBleibend(Art.FEHLER, Pruefbefunde.TITEL,
                Pruefbefunde.EINLEITUNG + " " + Pruefbefunde.kurzfassung(bericht), liste);
    }

    @Override
    public void frageNach(String frage, String bejahenBeschriftung, Runnable wennBejaht) {
        Button bejahen = new Button(bejahenBeschriftung);
        bejahen.getStyleClass().add("schaltflaeche-gefahr");
        Button abbrechen = new Button("Abbrechen");
        abbrechen.getStyleClass().add("schaltflaeche-still");

        HBox schaltflaechen = new HBox(8, bejahen, abbrechen);
        schaltflaechen.setAlignment(Pos.CENTER_LEFT);

        // Die Karte wird beim Klick geschlossen, gleich welche Antwort kommt.
        // Sonst bliebe die Frage stehen, nachdem sie beantwortet ist.
        Node karte = ecke.zeigeBleibend(Art.HINWEIS, "Rueckfrage", frage, schaltflaechen);
        bejahen.setOnAction(ereignis -> {
            ecke.entferne(karte);
            wennBejaht.run();
        });
        abbrechen.setOnAction(ereignis -> ecke.entferne(karte));
    }

    @Override
    public <T> void waehleAus(String titel, String text, List<T> eintraege,
            Function<T, String> anzeige, Consumer<T> wennGewaehlt) {
        if (eintraege == null || eintraege.isEmpty()) {
            return;
        }

        ChoiceBox<T> auswahl = new ChoiceBox<>();
        auswahl.getItems().addAll(eintraege);
        auswahl.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T eintrag) {
                return eintrag == null ? "" : anzeige.apply(eintrag);
            }

            @Override
            public T fromString(String zeichenkette) {
                return null;
            }
        });
        auswahl.getSelectionModel().selectFirst();
        auswahl.setMaxWidth(Double.MAX_VALUE);

        Button uebernehmen = new Button("Auswaehlen");
        uebernehmen.getStyleClass().add("schaltflaeche-haupt");
        Button abbrechen = new Button("Abbrechen");
        abbrechen.getStyleClass().add("schaltflaeche-still");

        VBox inhalt = new VBox(8, auswahl, new HBox(8, uebernehmen, abbrechen));
        Node karte = ecke.zeigeBleibend(Art.HINWEIS, titel, text, inhalt);

        uebernehmen.setOnAction(ereignis -> {
            T gewaehlt = auswahl.getValue();
            ecke.entferne(karte);
            if (gewaehlt != null) {
                wennGewaehlt.accept(gewaehlt);
            }
        });
        abbrechen.setOnAction(ereignis -> ecke.entferne(karte));
    }

    /** Eine Zeile des Pruefberichts: Beanstandung, darunter die Fundstelle. */
    private Node befund(Pruefbefunde.Zeile zeile) {
        Label text = new Label((zeile.istFehler() ? "• " : "– ") + zeile.text());
        text.getStyleClass().add("befund");
        text.setWrapText(true);
        text.setMaxWidth(310);
        if (zeile.ort().isEmpty()) {
            return text;
        }
        Label ort = new Label(zeile.ort());
        ort.getStyleClass().add("befund-ort");
        return new VBox(1, text, ort);
    }
}
