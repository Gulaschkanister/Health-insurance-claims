package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

/**
 * Prueft die durchsuchbare Liste, die in allen Uebersichten steckt.
 */
@DisplayName("Liste mit Suche und Zeilenaktionen")
class ListenbauTest {

    /** Ein Eintrag, wie ihn die Masken hineingeben. */
    private record Eintrag(int id, String name, String ort) {
    }

    private static final List<Eintrag> BESTAND = List.of(
            new Eintrag(1, "Anna Muster", "Berlin"),
            new Eintrag(2, "Bernd Berg", "Hamburg"),
            new Eintrag(3, "Clara Christ", "Bremen"));

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @Nested
    @DisplayName("Darstellung")
    class Darstellung {

        @Test
        @DisplayName("Ueberschriften und Werte stehen in der Liste")
        void werte() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> texte = alleTexte(liste(BESTAND));

                assertTrue(texte.contains("Name"), texte.toString());
                assertTrue(texte.contains("Ort"), texte.toString());
                assertTrue(texte.contains("Anna Muster"), texte.toString());
                assertTrue(texte.contains("Bremen"), texte.toString());
            });
        }

        @Test
        @DisplayName("Ein leerer Bestand zeigt den Hinweis und kein Suchfeld")
        void leererBestand() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(List.of());

                assertTrue(alleTexte(liste).contains("Noch niemand angelegt"), alleTexte(liste).toString());
                assertNull(liste.lookup("#probe" + Listenbau.SUCHE),
                        "Ohne Eintraege braucht es kein Suchfeld");
            });
        }

        @Test
        @DisplayName("Jede Zeile traegt ihre eigene Kennung")
        void kennungen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(BESTAND);

                assertNotNull(knopf(liste, 1));
                assertNotNull(knopf(liste, 2));
                assertNotNull(knopf(liste, 3));
                assertNull(knopf(liste, 4), "Fuer einen Eintrag, den es nicht gibt, auch keine Zeile");
            });
        }
    }

    @Nested
    @DisplayName("Suche")
    class Suche {

        @Test
        @DisplayName("Was nicht passt, verschwindet")
        void filtert() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(BESTAND);

                suchfeld(liste).setText("berg");

                assertNull(knopf(liste, 1));
                assertNotNull(knopf(liste, 2));
                assertNull(knopf(liste, 3));
            });
        }

        @Test
        @DisplayName("Gesucht wird auch in den weiteren Angaben, nicht nur im Namen")
        void suchtImGanzenText() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(BESTAND);

                suchfeld(liste).setText("Bremen");

                assertNotNull(knopf(liste, 3));
                assertNull(knopf(liste, 1));
            });
        }

        @Test
        @DisplayName("Ein geleertes Suchfeld zeigt wieder alles")
        void zuruecksetzen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(BESTAND);
                suchfeld(liste).setText("berg");
                suchfeld(liste).setText("");

                assertNotNull(knopf(liste, 1));
                assertNotNull(knopf(liste, 3));
            });
        }

        @Test
        @DisplayName("Findet die Suche nichts, steht der Hinweis da")
        void nichtsGefunden() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(BESTAND);

                suchfeld(liste).setText("Niemand");

                assertTrue(alleTexte(liste).contains("Noch niemand angelegt"));
            });
        }
    }

    @Nested
    @DisplayName("Aktionen")
    class Aktionen {

        @Test
        @DisplayName("Die Schaltflaeche einer Zeile bekommt genau deren Eintrag")
        void richtigerEintrag() {
            List<Eintrag> angeklickt = new ArrayList<>();
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(BESTAND, angeklickt);

                knopf(liste, 2).fire();

                assertEquals(1, angeklickt.size());
                assertEquals("Bernd Berg", angeklickt.get(0).name());
            });
        }

        @Test
        @DisplayName("Auch nach dem Filtern trifft die Schaltflaeche den richtigen Eintrag")
        void richtigerEintragNachFilter() {
            List<Eintrag> angeklickt = new ArrayList<>();
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = liste(BESTAND, angeklickt);
                suchfeld(liste).setText("clara");

                knopf(liste, 3).fire();

                assertEquals("Clara Christ", angeklickt.get(0).name());
            });
        }
    }

    // --- Aufbau ----------------------------------------------------------

    private static Region liste(List<Eintrag> bestand) {
        return liste(bestand, new ArrayList<>());
    }

    private static Region liste(List<Eintrag> bestand, List<Eintrag> angeklickt) {
        return new Listenbau<Eintrag>(new JavaFxUiFactory(), "probe")
                .spalten("Name", "Ort")
                .zellen(eintrag -> List.of(eintrag.name(), eintrag.ort()))
                .kennnummer(eintrag -> String.valueOf(eintrag.id()))
                .durchsuchbar("Suchen", eintrag -> eintrag.name() + " " + eintrag.ort())
                .hinweisWennLeer("Noch niemand angelegt")
                .aktion("Bearbeiten", "bearbeiten", "schaltflaeche-still", angeklickt::add)
                .baue(bestand);
    }

    private static Button knopf(Region liste, int id) {
        return (Button) liste.lookup("#probe" + Listenbau.ZEILE + id + "-bearbeiten");
    }

    private static TextField suchfeld(Region liste) {
        TextField feld = (TextField) liste.lookup("#probe" + Listenbau.SUCHE);
        assertNotNull(feld, "Kein Suchfeld in der Liste");
        return feld;
    }

    private static List<String> alleTexte(Region liste) {
        return liste.lookupAll(".label").stream()
                .filter(Label.class::isInstance)
                .map(knoten -> ((Label) knoten).getText())
                .toList();
    }
}
