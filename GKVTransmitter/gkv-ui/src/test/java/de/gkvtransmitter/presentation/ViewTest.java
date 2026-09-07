package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.application.AbrechnungService;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

/**
 * Prueft den Einstieg in die Oberflaeche.
 *
 * <p>{@code View} baut die Szene, fuellt die Seitenleiste und verteilt von
 * dort auf die Masken. Jede einzelne Maske ist geprueft — <b>dass sie von der
 * Seitenleiste aus auch erreichbar ist, war es bis zum 06.09.2026 nicht.</b>
 * Ein Vertipper in einer Kennung, eine vergessene Verdrahtung oder eine
 * Ausnahme beim Aufbau waere nur beim Klicken aufgefallen.</p>
 *
 * <p>Der wichtigste Test hier ist deshalb {@link Bereiche#jederBereichOeffnet}:
 * er geht die Seitenleiste durch und verlangt von jedem Eintrag, dass er
 * etwas zeigt. Das ist wenig — aber es ist genau das, was zwischen „die Maske
 * funktioniert" und „man kommt hin" liegt.</p>
 *
 * <p>Hier laeuft ein echter {@code Controller} mit einer eigenen Datenbank,
 * kein {@code @TempDir}: die Verbindung bleibt offen, solange die Anwendung
 * laeuft, und Windows laesst eine offene Datei nicht loeschen.</p>
 *
 * <p>Der Pfad dorthin ist <b>absolut</b>. Ein relativer wuerde von
 * {@code Anwendungsverzeichnis} gegen den Datenordner der Anwendung aufgeloest
 * — die Testdatenbank laege dann im Benutzerprofil statt unter {@code target},
 * und {@code mvn clean} raeumte sie nie weg. Bis zum 06.09.2026 lagen dort
 * sechs davon.</p>
 */
@DisplayName("View")
class ViewTest {

    private static Scene szene;
    private static Controller controller;
    private static View sicht;
    private static AppMessages texte;

    @BeforeAll
    static void anwendungAufbauen() {
        System.setProperty("gkv.db.path",
                Path.of("target", "view", "view.db").toAbsolutePath().toString());
        // Ohne Testdaten: geprueft wird die Verdrahtung, nicht der Inhalt. Eine
        // leere Liste ist dafuer sogar der haertere Fall - dann muss der
        // Hinweis auf die leere Liste stehen, und nicht nichts.
        System.setProperty("gkv.testdaten", "false");
        JavaFxLaufzeit.starten();
        texte = new AppMessages("/messages/ui-messages.json");
        controller = new Controller();
        sicht = new View(controller, new AbrechnungService(controller.getDatabase()));
        JavaFxLaufzeit.aufFxFaden(() -> {
            szene = sicht.createMainScene(1180, 760);
            szene.getRoot().applyCss();
            szene.getRoot().layout();
        });
    }

    @Nested
    @DisplayName("Die Seitenleiste")
    class Bereiche {

        /**
         * Der Test, der bisher fehlte — und geprueft wird der <b>gezeigte
         * Inhalt</b>, nicht die Ueberschrift.
         *
         * <p>Er klickt jeden Eintrag der Seitenleiste an. „Testdaten anlegen"
         * bleibt aussen vor: der Punkt schreibt in die Datenbank und zeigt
         * nichts; dass er da ist, prueft {@link #entwicklungspunktIstDa}.</p>
         *
         * <p>Die erste Fassung dieses Tests sah nur auf die Ueberschrift im
         * Rahmen — und die setzt {@code Hauptfenster.oeffne} <em>vor</em> dem
         * Aufruf des Bereichs. Ein Bereich, der still nichts tut, waere damit
         * durchgekommen; aufgefallen waere nur einer, der <em>wirft</em>. Die
         * Gegenprobe von damals machte die Verdrahtung kaputt, indem sie eine
         * Ausnahme warf — sie belegte also genau den Fall, den der Test ohnehin
         * fand, und nicht den, den er verfehlte. <b>Eine Gegenprobe, die den
         * falschen Fehler einbaut, beweist nichts.</b></p>
         *
         * <p>Jetzt muss nach jedem Klick ein Inhalt im Rahmen stehen, und zwar
         * ein <em>anderer</em> als zuvor. Damit faellt auch der stille Nichtstuer
         * auf.</p>
         */
        @Test
        @DisplayName("Jeder Bereich laesst sich oeffnen und zeigt eine eigene Maske")
        void jederBereichOeffnet() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> geprueft = new ArrayList<>();
                Node vorher = gezeigterInhalt();
                for (ToggleButton eintrag : navigationseintraege()) {
                    if (texte.get("nav.testdata").equals(eintrag.getText())) {
                        continue;
                    }
                    eintrag.setSelected(true);
                    eintrag.fire();
                    szene.getRoot().applyCss();
                    szene.getRoot().layout();

                    Node inhalt = gezeigterInhalt();
                    assertNotNull(inhalt, "\"" + eintrag.getText() + "\" zeigt nichts");
                    assertNotSame(vorher, inhalt,
                            "\"" + eintrag.getText() + "\" hat den Bereich nicht neu aufgebaut - "
                                    + "die Ueberschrift wechselt auch dann, wenn nichts geschieht");
                    assertEquals(eintrag.getText(),
                            ((Label) szene.getRoot().lookup("#" + Hauptfenster.ID_TITEL)).getText(),
                            "Die Ueberschrift muss dem gewaehlten Bereich folgen");
                    vorher = inhalt;
                    geprueft.add(eintrag.getText());
                }
                assertTrue(geprueft.size() >= 5,
                        "Erwartet werden mindestens Abrechnung, Teilnehmer, Dienstleister, "
                                + "Gruppen und Blaupausen - gefunden: " + geprueft);
            });
        }

        @Test
        @DisplayName("Die Reihenfolge ist die des Arbeitsablaufs")
        void reihenfolgeDesArbeitsablaufs() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> namen = navigationseintraege().stream()
                        .map(ToggleButton::getText).toList();

                assertEquals(texte.get("menu.settlement"), namen.get(0),
                        "Zuerst das, wozu das Programm da ist");
                int teilnehmer = namen.indexOf(texte.get("menu.patient"));
                int blaupausen = namen.indexOf(texte.get("menu.blueprints"));
                // Beide Male auf Vorhandensein pruefen: ein fehlender Eintrag
                // ergibt -1, und -1 ist kleiner als jeder Index - der Vergleich
                // allein waere also auch dann gruen, wenn es "Teilnehmer" gar
                // nicht mehr gaebe.
                assertTrue(teilnehmer >= 0, "Kein Bereich \"Teilnehmer\": " + namen);
                assertTrue(blaupausen >= 0, "Kein Bereich \"Blaupausen\": " + namen);
                assertTrue(teilnehmer < blaupausen, "Stammdaten vor Blaupausen: " + namen);
                assertEquals(texte.get("nav.testdata"), namen.get(namen.size() - 1),
                        "Der Entwicklungspunkt gehoert ans Ende");
            });
        }

        @Test
        @DisplayName("Die Vorlagen stehen mit ihrem Kursnamen darin, nicht mit dem vollen Namen")
        void vorlagenMitKursnamen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> namen = navigationseintraege().stream()
                        .map(ToggleButton::getText).toList();

                assertTrue(namen.contains("Geburtsvorbereitungskurs"),
                        "Erwartet wird der Kursname, gefunden: " + namen);
                assertTrue(namen.stream().noneMatch(name -> name.contains("Einzelabrechnung")),
                        "Der Zusatz zur Abrechnungsart ist bei allen Vorlagen gleich "
                                + "und sprengt die schmale Leiste: " + namen);
            });
        }

        @Test
        @DisplayName("Der Entwicklungspunkt ist vorhanden, aber nicht Teil des Ablaufs")
        void entwicklungspunktIstDa() {
            JavaFxLaufzeit.aufFxFaden(() -> assertTrue(
                    navigationseintraege().stream()
                            .anyMatch(e -> texte.get("nav.testdata").equals(e.getText())),
                    "Ohne ihn liesse sich der Ablauf nicht durchspielen"));
        }

        @Test
        @DisplayName("Unter jeder Ueberschrift steht ein Satz, wozu der Bereich da ist")
        void jederBereichErklaertSich() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                for (String bereich : List.of(texte.get("menu.settlement"), texte.get("menu.patient"),
                        texte.get("menu.self"), texte.get("menu.groups"), texte.get("menu.blueprints"))) {
                    oeffne(bereich);
                    Label untertitel = (Label) szene.getRoot().lookup("#" + Hauptfenster.ID_UNTERTITEL);
                    assertNotNull(untertitel, "Keine Zeile unter der Ueberschrift bei " + bereich);
                    assertTrue(untertitel.isVisible() && !untertitel.getText().isBlank(),
                            "Kein Satz unter \"" + bereich + "\"");
                }
            });
        }
    }

    /**
     * Die dunkle Fassung haengt an einer Stilklasse an der Wurzel.
     *
     * <p>Mehr braucht es nicht, weil alle Farben in {@code gkv.css} als
     * benannte Werte stehen und die dunkle Fassung dieselben Namen anders
     * besetzt. Der Test haelt genau diese Verabredung fest: <b>geht die
     * Stilklasse verloren oder wird sie umbenannt, faellt der Dunkelmodus
     * lautlos aus</b> - man saehe es erst am Bildschirm.</p>
     */
    @Nested
    @DisplayName("Die Darstellung")
    class Darstellung {

        @Test
        @DisplayName("setzt und nimmt die Stilklasse an der Wurzel")
        void umschalten() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                sicht.setzeDarstellung(EinstellungenMaske.DUNKEL);
                assertTrue(szene.getRoot().getStyleClass().contains(EinstellungenMaske.DUNKEL));

                sicht.setzeDarstellung(EinstellungenMaske.HELL);
                assertTrue(!szene.getRoot().getStyleClass().contains(EinstellungenMaske.DUNKEL));
            });
        }

        @Test
        @DisplayName("setzt die Klasse nicht zweimal")
        void nichtDoppelt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                sicht.setzeDarstellung(EinstellungenMaske.DUNKEL);
                sicht.setzeDarstellung(EinstellungenMaske.DUNKEL);

                assertEquals(1, szene.getRoot().getStyleClass().stream()
                        .filter(EinstellungenMaske.DUNKEL::equals).count());
                sicht.setzeDarstellung(EinstellungenMaske.HELL);
            });
        }
    }

    @Nested
    @DisplayName("Die Statuszeile")
    class Statuszeile {

        @Test
        @DisplayName("nennt die Anzahl der Vorlagen, nicht ihre Dateinamen")
        void nenntDieAnzahl() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                int anzahl = controller.getGlobalDefinitions().getInvoiceTemplateCollection().size();
                Label zeile = (Label) szene.getRoot().lookup("#" + Hauptfenster.ID_STATUS);

                assertNotNull(zeile);
                assertEquals(String.format(texte.get("status.templatesLoaded"), anzahl), zeile.getText());
                assertTrue(!zeile.getText().contains(".json"),
                        "Wer abrechnet, hat mit Dateinamen nichts zu schaffen: " + zeile.getText());
            });
        }

        @Test
        @DisplayName("traegt ein Info-Zeichen, solange es Vorlagen gibt")
        void mitInfozeichen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Node zeichen = szene.getRoot().lookup("#" + Hauptfenster.ID_STATUS_INFO);

                assertNotNull(zeichen);
                assertTrue(zeichen.isVisible(),
                        "Es gibt Vorlagen, also gibt es auch etwas dahinter");
            });
        }
    }

    @Nested
    @DisplayName("Der Kursname einer Vorlage")
    class Kursname {

        @Test
        @DisplayName("ist der Teil vor dem Komma")
        void vorDemKomma() {
            assertEquals("Geburtsvorbereitungskurs",
                    View.kursname("Geburtsvorbereitungskurs, Einzelabrechnung"));
            assertEquals("Rückbildungskurs nach Geburten",
                    View.kursname("Rückbildungskurs nach Geburten, Einzelabrechnung"));
        }

        @Test
        @DisplayName("bleibt unveraendert, wenn es kein Komma gibt")
        void ohneKomma() {
            assertEquals("Kurs", View.kursname("Kurs"));
            assertEquals("", View.kursname(""));
        }
    }

    // --- Hilfsmittel ------------------------------------------------------

    /**
     * Was im Rahmen steht.
     *
     * <p>Ueber die Stilklasse des Inhaltsbereichs, weil der Test die
     * {@code Hauptfenster}-Instanz nicht hat — nur die Szene. Der Inhalt eines
     * {@code ScrollPane} haengt erst nach dem Aufbau der Darstellung im
     * Knotenbaum; deshalb der {@code applyCss()}/{@code layout()}-Aufruf vor
     * jedem Zugriff.</p>
     */
    private static Node gezeigterInhalt() {
        Node bereich = szene.getRoot().lookup(".inhalt");
        assertNotNull(bereich, "Kein Inhaltsbereich im Rahmen");
        return ((javafx.scene.control.ScrollPane) bereich).getContent();
    }

    private static void oeffne(String bereich) {
        ToggleButton eintrag = (ToggleButton) szene.getRoot()
                .lookup("#" + Hauptfenster.kennung(bereich));
        assertNotNull(eintrag, "Kein Bereich \"" + bereich + "\" in der Seitenleiste");
        eintrag.setSelected(true);
        eintrag.fire();
        szene.getRoot().applyCss();
        szene.getRoot().layout();
    }

    /** Alle Eintraege der Seitenleiste in ihrer Reihenfolge. */
    private static List<ToggleButton> navigationseintraege() {
        List<ToggleButton> gefunden = new ArrayList<>();
        sammle(szene.getRoot(), gefunden);
        return gefunden;
    }

    private static void sammle(Node knoten, List<ToggleButton> gefunden) {
        if (knoten instanceof ToggleButton eintrag && eintrag.getStyleClass().contains("nav-eintrag")) {
            gefunden.add(eintrag);
        }
        if (knoten instanceof Parent eltern) {
            for (Node kind : eltern.getChildrenUnmodifiable()) {
                sammle(kind, gefunden);
            }
        }
    }
}
