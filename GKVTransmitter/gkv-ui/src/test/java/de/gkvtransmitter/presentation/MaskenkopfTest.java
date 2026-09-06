package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Prueft den immer gleichen Kopf einer Uebersicht.
 *
 * <p>Die Klasse ist klein und laeuft in jedem Maskentest mit — aber genau
 * <em>weil</em> sie in jedem mitlaeuft, wird sie in keinem geprueft: die Tests
 * dort suchen die Schaltflaeche ueber ihre Kennung und interessieren sich
 * nicht dafuer, wie der Kopf zustande kommt. Sie steht hier, damit die
 * Uebersichten nicht auseinanderdriften.</p>
 *
 * <p>Ihr Zweck ist Gleichheit: eine Uebersicht soll in allen Bereichen gleich
 * aussehen und gleich zu bedienen sein. Was dieser Test festhaelt, ist deshalb
 * nicht „irgendein Kopf entsteht", sondern <em>welcher</em>.</p>
 */
@DisplayName("Maskenkopf")
class MaskenkopfTest {

    private static final UiFactory BAUSTEINE = new JavaFxUiFactory();

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @Test
    @DisplayName("Die Schaltflaeche traegt ein Pluszeichen, die Beschriftung und die Kennung")
    void schaltflaecheIstAuffindbar() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Region kopf = Maskenkopf.mitListe(BAUSTEINE, "Neuer Teilnehmer",
                    PersonenMaske.ID_NEU, () -> { }, new VBox());

            Button neu = (Button) kopf.lookup("#" + PersonenMaske.ID_NEU);
            assertNotNull(neu, "Ohne Kennung findet sie kein Test und kein Bedienwerkzeug");
            assertEquals("+ Neuer Teilnehmer", neu.getText());
            assertTrue(neu.getStyleClass().contains("schaltflaeche-haupt"),
                    "Es ist die Haupthandlung der Uebersicht");
        });
    }

    @Test
    @DisplayName("Ein Klick loest genau einmal aus, was uebergeben wurde")
    void klickLoestAus() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            List<String> geschehen = new ArrayList<>();
            Region kopf = Maskenkopf.mitListe(BAUSTEINE, "Neue Gruppe",
                    GruppenMaske.ID_NEU, () -> geschehen.add("neu"), new VBox());

            ((Button) kopf.lookup("#" + GruppenMaske.ID_NEU)).fire();

            assertEquals(List.of("neu"), geschehen);
        });
    }

    /**
     * Die Liste kommt unveraendert durch.
     *
     * <p>Wichtig, weil die Masken ihre Liste selbst bauen und sie hier nur
     * durchreichen. Wuerde sie eingehuellt, liefe jedes {@code lookup} auf die
     * Zeilen ins Leere, solange die Darstellung nicht aufgebaut ist — dieselbe
     * Falle wie beim {@code ScrollPane} des Rahmens.</p>
     */
    @Test
    @DisplayName("Die Liste steht unveraendert unter dem Kopf")
    void listeStehtDarunter() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            VBox liste = new VBox();

            Region kopf = Maskenkopf.mitListe(BAUSTEINE, "Neu", "irgendeine-kennung",
                    () -> { }, liste);

            VBox wurzel = (VBox) kopf;
            assertEquals(2, wurzel.getChildren().size(), "Kopfzeile und Liste, sonst nichts");
            assertSame(liste, wurzel.getChildren().get(1),
                    "Die Liste darf nicht eingehuellt werden");
        });
    }

    /**
     * Keine eigene Ueberschrift.
     *
     * <p>Den Namen des Bereichs traegt bereits die Kopfzeile des Rahmens.
     * Stuende er hier noch einmal, staende er zweimal untereinander — und
     * genau so sah es aus, ehe der Rahmen eine Kopfzeile bekam.</p>
     */
    @Test
    @DisplayName("Der Kopf wiederholt den Namen des Bereichs nicht")
    void keineZweiteUeberschrift() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            Region kopf = Maskenkopf.mitListe(BAUSTEINE, "Neuer Teilnehmer",
                    PersonenMaske.ID_NEU, () -> { }, new VBox());

            assertTrue(kopf.lookupAll(".masken-titel").isEmpty(),
                    "Die Ueberschrift steht im Rahmen, nicht in der Maske");
            assertTrue(kopf.getStyleClass().contains("maske"),
                    "Ohne die Stilklasse fehlen Rand und Abstaende");
        });
    }
}
