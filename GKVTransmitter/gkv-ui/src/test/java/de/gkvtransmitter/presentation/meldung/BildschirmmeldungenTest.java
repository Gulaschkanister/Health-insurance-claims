package de.gkvtransmitter.presentation.meldung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.presentation.Benachrichtigungen;
import de.gkvtransmitter.presentation.JavaFxLaufzeit;
import de.gkvtransmitter.validator.ValidationReport;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;

/**
 * Prueft den Weg von einer Meldung auf den Bildschirm.
 *
 * <p>{@code Benachrichtigungen} - die Ecke selbst - ist geprueft, und
 * {@code AufzeichnendeMeldungen} steht in jedem Maskentest. Dazwischen lag
 * diese Klasse ungeprueft: <b>die einzige Umsetzung von {@link Meldungen}, die
 * wirklich etwas anzeigt.</b> Alles, was die Anwendung im Betrieb meldet, geht
 * hier durch; in den Tests geht es an ihr vorbei.</p>
 *
 * <p>Am wichtigsten ist die Rueckfrage. Sie ersetzt ein Dialogfenster und darf
 * deshalb weder haengen bleiben, nachdem geantwortet wurde, noch etwas tun,
 * wenn abgebrochen wird - ein Loeschen laesst sich nicht zuruecknehmen.</p>
 */
@DisplayName("Bildschirmmeldungen")
class BildschirmmeldungenTest {

    private Benachrichtigungen ecke;
    private Bildschirmmeldungen meldungen;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        JavaFxLaufzeit.aufFxFaden(() -> {
            ecke = new Benachrichtigungen();
            meldungen = new Bildschirmmeldungen(ecke);
        });
    }

    @Nested
    @DisplayName("Die einfachen Meldungen")
    class Einfache {

        @Test
        @DisplayName("landen mit ihrem Text in der Ecke")
        void landenInDerEcke() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                meldungen.erfolg("Anna Berger angelegt.");
                meldungen.hinweis("Keine Gruppe gewaehlt.");
                meldungen.fehler("Datenbank gesperrt.");

                assertEquals(List.of("Anna Berger angelegt.", "Keine Gruppe gewaehlt.",
                        "Datenbank gesperrt."), ecke.offene());
            });
        }
    }

    @Nested
    @DisplayName("Die Rueckfrage")
    class Rueckfrage {

        @Test
        @DisplayName("tut nichts, solange niemand geantwortet hat")
        void wartetAufAntwort() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> geschehen = new ArrayList<>();

                meldungen.frageNach("Soll Anna geloescht werden?", "Löschen",
                        () -> geschehen.add("geloescht"));

                assertTrue(geschehen.isEmpty(),
                        "Ein Loeschen darf nicht schon durch das Fragen geschehen");
                assertEquals(1, ecke.offene().size(), "Die Frage muss stehen bleiben");
            });
        }

        @Test
        @DisplayName("fuehrt nach dem Ja aus und raeumt sich weg")
        void beiZustimmung() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> geschehen = new ArrayList<>();
                meldungen.frageNach("Soll Anna geloescht werden?", "Löschen",
                        () -> geschehen.add("geloescht"));

                knopf("Löschen").fire();

                assertEquals(List.of("geloescht"), geschehen);
                assertTrue(ecke.offene().isEmpty(),
                        "Sonst bliebe die Frage stehen, nachdem sie beantwortet ist");
            });
        }

        @Test
        @DisplayName("tut beim Abbrechen nichts und raeumt sich trotzdem weg")
        void beiAblehnung() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                List<String> geschehen = new ArrayList<>();
                meldungen.frageNach("Soll Anna geloescht werden?", "Löschen",
                        () -> geschehen.add("geloescht"));

                knopf("Abbrechen").fire();

                assertTrue(geschehen.isEmpty());
                assertTrue(ecke.offene().isEmpty());
            });
        }

        @Test
        @DisplayName("traegt die Beschriftung, die der Aufrufer vorgibt")
        void eigeneBeschriftung() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                meldungen.frageNach("Soll die Blaupause weg?", "Entfernen", () -> { });

                assertNotNull(knopf("Entfernen"),
                        "\"Löschen\" waere fuer manche Fragen die falsche Zusage");
            });
        }
    }

    @Nested
    @DisplayName("Der Pruefbericht")
    class Pruefbericht {

        @Test
        @DisplayName("nennt jede Beanstandung und wo sie steckt")
        void nenntJedeBeanstandung() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                meldungen.pruefbericht(ValidationReport.builder()
                        .error("IK_UNGUELTIG", "FKT Feld 3", "Das IK stimmt nicht.")
                        .warning("GES_99", "GES", "Der Bruttobetrag weicht ab.")
                        .build());

                String text = sichtbarerText();
                assertTrue(text.contains("Das IK stimmt nicht."), text);
                assertTrue(text.contains("FKT Feld 3"),
                        "Ohne die Fundstelle sucht man das Feld von Hand: " + text);
                assertTrue(text.contains("Der Bruttobetrag weicht ab."), text);
            });
        }

        @Test
        @DisplayName("bleibt stehen, weil er nicht uebersehen werden darf")
        void bleibtStehen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                meldungen.pruefbericht(ValidationReport.builder()
                        .error("IK_UNGUELTIG", "FKT", "Das IK stimmt nicht.")
                        .build());

                assertFalse(ecke.offene().isEmpty(),
                        "Eine abgewiesene Lieferung darf sich nicht davonstehlen");
            });
        }
    }

    // --- Hilfsmittel ------------------------------------------------------

    /** Die Schaltflaeche mit dieser Beschriftung, oder {@code null}. */
    private Button knopf(String beschriftung) {
        return knopfIn(ecke.bereich(), beschriftung);
    }

    private static Button knopfIn(Node knoten, String beschriftung) {
        if (knoten instanceof Button knopf && beschriftung.equals(knopf.getText())) {
            return knopf;
        }
        if (knoten instanceof Parent eltern) {
            for (Node kind : eltern.getChildrenUnmodifiable()) {
                Button gefunden = knopfIn(kind, beschriftung);
                if (gefunden != null) {
                    return gefunden;
                }
            }
        }
        return null;
    }

    private String sichtbarerText() {
        StringBuilder text = new StringBuilder();
        sammle(ecke.bereich(), text);
        return text.toString();
    }

    private static void sammle(Node knoten, StringBuilder text) {
        if (knoten instanceof Labeled beschriftet && beschriftet.getText() != null) {
            text.append(beschriftet.getText()).append('\n');
        }
        if (knoten instanceof Parent eltern) {
            for (Node kind : eltern.getChildrenUnmodifiable()) {
                sammle(kind, text);
            }
        }
    }
}
