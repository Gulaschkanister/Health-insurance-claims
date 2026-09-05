package de.gkvtransmitter.parser.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.model.segment.SegmentInfo;

/**
 * Prueft die Nachrichtenvorlagen, die die Blaupausenmaske zur Auswahl stellt.
 *
 * <p>Eine Vorlage ist ueber ihren Anzeigenamen an eine Blaupause gebunden:
 * {@code Blueprint.templateName} traegt genau diese Zeichenkette, und
 * {@code GlobalDefinitions.registerInvoiceTemplate} legt die Vorlagen in einer
 * Abbildung <b>ueber den Namen</b> ab. Zwei gleich benannte Vorlagen
 * verdraengten einander dort stillschweigend - die zweite gewaenne, die erste
 * verschwaende, und jede Blaupause, die auf sie zeigte, liesse sich nicht mehr
 * oeffnen. Genau so verhielt sich die Testdaten-Blaupause, die auf eine Vorlage
 * namens {@code test-template} zeigte, die es nie gab.</p>
 */
@DisplayName("Die Nachrichtenvorlagen")
class VorlagenTest {

    private static List<DtaMessage> vorlagen() {
        return new JsonParserFactory().parseInvoices();
    }

    @Nested
    @DisplayName("Jede Vorlage")
    class JedeVorlage {

        @Test
        @DisplayName("traegt einen Anzeigenamen")
        void traegtEinenNamen() {
            for (DtaMessage vorlage : vorlagen()) {
                assertFalse(vorlage.getInvoicerName() == null || vorlage.getInvoicerName().isBlank(),
                        vorlage.getSourceName() + " haette keinen Namen, unter dem eine Blaupause"
                                + " sie wiederfinden koennte");
            }
        }

        @Test
        @DisplayName("enthaelt die Leistungszeile, aus der die Blaupause ihre Felder bezieht")
        void enthaeltDieLeistungszeile() {
            for (DtaMessage vorlage : vorlagen()) {
                assertTrue(segmenttypen(vorlage).contains("ENF"),
                        vorlage.getInvoicerName() + " haette kein ENF - die Blaupause koennte"
                                + " weder Preis noch Abrechnungscode aufnehmen");
            }
        }
    }

    @Nested
    @DisplayName("Die Vorlagen zusammen")
    class DieVorlagenZusammen {

        @Test
        @DisplayName("heissen paarweise verschieden")
        void heissenVerschieden() {
            List<String> namen = vorlagen().stream().map(DtaMessage::getInvoicerName).toList();
            Set<String> ohneDoppelte = new LinkedHashSet<>(namen);

            assertEquals(namen.size(), ohneDoppelte.size(),
                    "Gleiche Namen verdraengen einander in getInvoiceTemplateCollection,"
                            + " ohne dass irgendetwas fehlschlaegt: " + namen);
        }

        @Test
        @DisplayName("umfassen Geburtsvorbereitung und Rueckbildung")
        void umfassenBeideKurse() {
            List<String> namen = new ArrayList<>(
                    vorlagen().stream().map(DtaMessage::getInvoicerName).toList());

            assertTrue(namen.stream().anyMatch(name -> name.startsWith("Geburtsvorbereitungskurs")),
                    "Vorhanden: " + namen);
            assertTrue(namen.stream().anyMatch(name -> name.startsWith("Rückbildungskurs")),
                    "Vorhanden: " + namen);
        }

        @Test
        @DisplayName("haben denselben Aufbau, weil beide Kurse dieselbe Nachricht ergeben")
        void habenDenselbenAufbau() {
            List<DtaMessage> alle = vorlagen();
            List<String> erste = segmenttypen(alle.get(0));

            for (DtaMessage weitere : alle) {
                assertEquals(erste, segmenttypen(weitere),
                        weitere.getInvoicerName() + " weicht im Aufbau ab. Beide Kurse werden"
                                + " im Leistungsbereich SGS H abgerechnet und ergeben dieselbe"
                                + " Segmentfolge; was sie unterscheidet - Positionsnummer,"
                                + " Abrechnungscode, Preis - steht in der Blaupause, nicht hier.");
            }
        }
    }

    private static List<String> segmenttypen(DtaMessage vorlage) {
        List<String> typen = new ArrayList<>();
        for (SegmentInfo segment : vorlage.getSegments()) {
            typen.add(segment.getSegmentType());
        }
        return typen;
    }
}
