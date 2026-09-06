package de.gkvtransmitter.dta;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.model.segment.SegmentInfo;
import de.gkvtransmitter.parser.json.JsonParserFactory;

/**
 * Haelt die Kette zwischen Segmentdefinition und {@link Leistungsparameter}
 * zusammen.
 *
 * <p>Diese Tests gaebe es nicht, wenn die Kette gehalten haette. Am 05.09.2026
 * fiel auf: {@code Leistungsparameter} suchte in der Blaupause nach
 * "Durchschnittlicher Einzelbetrag", waehrend dieses Feld in
 * {@code segments/enf.json} eine {@code person}-Markierung trug. Die
 * Oberflaeche blendet solche Felder aus, das Feld stand also in keinem
 * Formular, konnte in keiner Blaupause landen, und die Klasse fiel jedes Mal
 * auf ihre Vorbelegung zurueck - <b>jede Rechnung lautete auf 15.000,00 je
 * Termin</b>.</p>
 *
 * <p>Kein einziger Test schlug dabei fehl, weil alle gegen die Vorbelegung
 * prueften. Der Bruch lag zwischen zwei Bausteinen, die je fuer sich in Ordnung
 * waren: eine JSON-Datei und eine Klasse, die Namen darin sucht. Genau diese
 * Fuge pruefen die folgenden Tests.</p>
 */
@DisplayName("Die Felder, die eine Blaupause tragen muss")
class BlaupausenfelderTest {

    /**
     * Baut die Schluessel aller Felder, die im Blaupausenformular erscheinen.
     *
     * <p>Bewusst ueber {@code parseInvoices()} und nicht ueber die
     * Segmentdefinitionen: das ist derselbe Weg, den {@code View.createFormular}
     * geht, samt derselben Schluesselbildung. {@code speichereBlaupause} legt
     * die Werte anschliessend unter genau diesen Schluesseln ab, und
     * {@link Leistungsparameter} sucht danach. Wuerde der Test die Definitionen
     * direkt lesen, entginge ihm eine Abweichung in der Schluesselbildung - und
     * die haette dieselbe Wirkung wie der Fehler, der ihn veranlasst hat.</p>
     *
     * <p>Die Bedingung ist dieselbe wie in der Maske: nicht {@code internal}
     * und ohne Personenrolle. Aendert sie sich dort, muss sie hier mitwandern.</p>
     */
    private static List<String> ausfuellbareFelder() {
        List<String> schluessel = new ArrayList<>();
        for (DtaMessage nachricht : new JsonParserFactory().parseInvoices()) {
            for (SegmentInfo segment : nachricht.getSegments()) {
                segment.getValueFields().forEach((name, eintrag) -> {
                    if (!eintrag.isInternal() && eintrag.getPersonRole() == null) {
                        schluessel.add(name);
                    }
                });
            }
        }
        return schluessel;
    }

    @Nested
    @DisplayName("Jeder Name, den Leistungsparameter sucht,")
    class JederGesuchteName {

        @Test
        @DisplayName("ist in den Segmentdefinitionen auch ausfuellbar")
        void istAusfuellbar() {
            List<String> ausfuellbar = ausfuellbareFelder();
            List<String> fehlend = Leistungsparameter.BLAUPAUSENFELDER.stream()
                    .filter(name -> !ausfuellbar.contains(name))
                    .toList();

            assertTrue(fehlend.isEmpty(),
                    "Leistungsparameter liest Felder, die in keinem Formular stehen und damit nie "
                            + "in einer Blaupause ankommen koennen: " + fehlend
                            + ". Ausfuellbar sind: " + ausfuellbar);
        }
    }

    @Nested
    @DisplayName("Der Einzelbetrag")
    class Einzelbetrag {

        @Test
        @DisplayName("steht im Formular, denn er bestimmt den Rechnungsbetrag")
        void stehtImFormular() {
            assertTrue(ausfuellbareFelder().contains("Durchschnittlicher Einzelbetrag"),
                    "Ohne dieses Feld laesst sich der Preis nicht einstellen und jede Rechnung "
                            + "lautet auf die Vorbelegung von "
                            + Leistungsparameter.VORBELEGUNG.einzelbetrag());
        }
    }

    @Nested
    @DisplayName("Felder, die die Anwendung selbst setzt,")
    class SelbstGesetzteFelder {

        @Test
        @DisplayName("werden nicht zusaetzlich vom Anwender erfragt")
        void werdenNichtErfragt() {
            List<String> ausfuellbar = ausfuellbareFelder();
            // DtaFactory rechnet die Summen aus Einzelbetrag mal Menge und setzt
            // Menge und Leistungsdatum aus der Abrechnung. Stuenden sie im
            // Formular, fragte es nach Werten, die es anschliessend verwirft.
            for (String selbstGesetzt : List.of(
                    "Summe Leistung", "Summe Gesamtbetrag", "Gesamtbetrag",
                    "Anzahl/Menge", "Leistungsdatum")) {
                assertFalse(ausfuellbar.contains(selbstGesetzt),
                        "\"" + selbstGesetzt + "\" wird im Formular erfragt, aber von DtaFactory "
                                + "selbst gesetzt - die Eingabe erreicht die Nachricht nie");
            }
        }
    }
}
