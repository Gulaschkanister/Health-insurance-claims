package de.gkvtransmitter.presentation.meldung;

import java.util.Objects;

import de.gkvtransmitter.presentation.Benachrichtigungen;
import de.gkvtransmitter.presentation.Benachrichtigungen.Art;
import de.gkvtransmitter.validator.ValidationReport;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Bringt die Meldungen in die Ecke oben rechts.
 *
 * <p>Auch die Rueckfrage und die Auswahl erscheinen dort - als Karte mit
 * Schaltflaechen, die stehen bleibt, bis entschieden ist. Damit oeffnet die
 * Anwendung im laufenden Betrieb kein einziges Fenster mehr.</p>
 */
public class Bildschirmmeldungen implements Meldungen {

    private final Benachrichtigungen ecke;
    private final de.gkvtransmitter.util.AppMessages texte;
    private final Pruefbefunde befunde;

    /**
     * Ohne eigene Texte - fuer Tests und fuer den Aufbau vor dem Laden.
     *
     * <p>Laedt die mitgelieferte Textsammlung selbst. Die Oberflaeche gibt
     * ihre eigene mit, damit nicht zwei Sammlungen nebeneinanderstehen.</p>
     */
    public Bildschirmmeldungen(Benachrichtigungen ecke) {
        this(ecke, new de.gkvtransmitter.util.AppMessages("/messages/ui-messages.json"));
    }

    public Bildschirmmeldungen(Benachrichtigungen ecke, de.gkvtransmitter.util.AppMessages texte) {
        this.texte = java.util.Objects.requireNonNull(texte, "texte must not be null");
        this.befunde = new Pruefbefunde(texte);
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
        pruefbericht(bericht, Pruefbefunde.Lage.VERSAND);
    }

    @Override
    public void pruefbericht(ValidationReport bericht, Pruefbefunde.Lage lage) {
        VBox liste = new VBox(6);
        for (Pruefbefunde.Zeile zeile : befunde.zeilen(bericht)) {
            liste.getChildren().add(befund(zeile));
        }
        // Rot nur, wenn der Lauf tatsaechlich aufgehalten wurde. Ein Bericht
        // aus reinen Hinweisen ist kein Fehler - er bleibt aber stehen, denn
        // eine Warnung, die nach sechs Sekunden verschwindet, ist keine.
        Art art = bericht.hatFehler() ? Art.FEHLER : Art.HINWEIS;
        ecke.zeigeBleibend(art, befunde.titel(bericht),
                befunde.einleitung(bericht, lage) + " " + befunde.kurzfassung(bericht),
                liste);
    }

    @Override
    public void frageNach(String frage, String bejahenBeschriftung, Runnable wennBejaht) {
        Button bejahen = new Button(bejahenBeschriftung);
        bejahen.getStyleClass().add("schaltflaeche-gefahr");
        Button abbrechen = new Button(texte.get("button.cancelQuestion"));
        abbrechen.getStyleClass().add("schaltflaeche-still");

        HBox schaltflaechen = new HBox(8, bejahen, abbrechen);
        schaltflaechen.setAlignment(Pos.CENTER_LEFT);

        // Die Karte wird beim Klick geschlossen, gleich welche Antwort kommt.
        // Sonst bliebe die Frage stehen, nachdem sie beantwortet ist.
        Node karte = ecke.zeigeBleibend(Art.HINWEIS, texte.get("dialog.question"), frage,
                schaltflaechen);
        bejahen.setOnAction(ereignis -> {
            ecke.entferne(karte);
            wennBejaht.run();
        });
        abbrechen.setOnAction(ereignis -> ecke.entferne(karte));
    }

    /** Eine Zeile des Pruefberichts: Beanstandung, darunter die Fundstelle. */
    private Node befund(Pruefbefunde.Zeile zeile) {
        String marke = zeile.istFehler() ? Textbau.MARKE_FEHLER : Textbau.MARKE_HINWEIS;
        VBox eintrag = new VBox(1, zeilentext(marke + zeile.text(), "befund"));
        // Was zu tun ist, vor der Fundstelle: Wer eine Beanstandung liest,
        // sucht zuerst die Handlung und erst danach den Beleg dafuer.
        if (!zeile.handlung().isBlank()) {
            eintrag.getChildren().add(zeilentext(Textbau.MARKE_HANDLUNG + zeile.handlung(), "befund-handlung"));
        }
        String herkunft = herkunft(zeile);
        if (!herkunft.isBlank()) {
            eintrag.getChildren().add(zeilentext(herkunft, "befund-ort"));
        }
        return eintrag;
    }

    /** Stelle in der Datei und Fundstelle in der Anlage, in einer Zeile. */
    private static String herkunft(Pruefbefunde.Zeile zeile) {
        if (zeile.fundstelle().isBlank()) {
            return zeile.ort();
        }
        return zeile.ort().isBlank()
                ? zeile.fundstelle() : zeile.ort() + Textbau.WEITER + zeile.fundstelle();
    }

    /**
     * Eine Textzeile eines Befunds.
     *
     * <p><b>{@code setMinHeight} ist das Entscheidende hier</b>, und es fehlte.
     * Seit jeder Befund drei Zeilen traegt - Beanstandung, Handlung,
     * Fundstelle -, passt eine lange Liste nicht mehr ins Fenster. Eine
     * {@code VBox} staucht dann ihre Kinder, und ein gestauchtes
     * {@code Label} kuerzt mit Auslassungspunkten: <b>Der Bericht wurde durch
     * die Zugabe unvollstaendiger als vorher.</b> Aufgefallen am
     * Bildschirmfoto, nicht in einem Test.</p>
     *
     * <p>Mit der Mindesthoehe behaelt jeder Befund seine volle Hoehe. Ist die
     * Liste laenger als das Fenster, wird sie unten abgeschnitten - das ist
     * sichtbar, waehrend drei Punkte mitten im Satz wie das Ende aussehen.</p>
     */
    private static Label zeilentext(String text, String stilklasse) {
        Label label = new Label(text);
        label.getStyleClass().add(stilklasse);
        label.setWrapText(true);
        label.setMaxWidth(310);
        label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return label;
    }
}
