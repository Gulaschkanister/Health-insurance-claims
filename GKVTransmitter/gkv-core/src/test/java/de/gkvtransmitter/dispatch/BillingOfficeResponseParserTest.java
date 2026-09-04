package de.gkvtransmitter.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Prueft die Auswertung von Kassenrueckmeldungen.
 *
 * <p>Der Schwerpunkt liegt auf den beiden Fehlern der frueheren Auswertung.
 * Beide wirkten in dieselbe, gefaehrliche Richtung: eine nicht angenommene
 * Lieferung konnte als angenommen gelten und waere dann nicht nachverfolgt
 * worden.</p>
 */
class BillingOfficeResponseParserTest {

    private final BillingOfficeResponseParser parser = new BillingOfficeResponseParser();

    @Nested
    @DisplayName("Regressionen der frueheren Auswertung")
    class Regressionen {

        @Test
        @DisplayName("Ein technischer Fehler wird nicht als fachliche Ablehnung gewertet")
        void technischerFehlerIstKeineAblehnung() {
            // Zuvor wurde auf "fehler" vor "technisch" geprueft, weshalb diese
            // Antwort als REJECTED galt und die Einstufung TECHNICAL_ERROR
            // praktisch unerreichbar war.
            BillingOfficeResponse antwort = parser.parse("Technischer Fehler bei der Verarbeitung");

            assertEquals(BillingOfficeResponseType.TECHNICAL_ERROR, antwort.getType());
        }

        @Test
        @DisplayName("Das Wort Protokoll wird nicht als Zustimmung gelesen")
        void protokollIstKeineZustimmung() {
            // Zuvor traf die Teilzeichenkette "ok" in "Protokoll", wodurch
            // diese Ablehnung als Annahme gewertet wurde.
            BillingOfficeResponse antwort = parser.parse("Fehlerprotokoll liegt vor");

            assertEquals(BillingOfficeResponseType.REJECTED, antwort.getType(),
                    "Eine Antwort mit dem Wort Protokoll darf nicht als Annahme gelten");
        }

        @Test
        @DisplayName("Ein Protokoll ohne Fehlerbegriff gilt nicht als Annahme")
        void reinesProtokollIstUnbekannt() {
            BillingOfficeResponse antwort = parser.parse("Protokoll zur Lieferung 00001");

            assertEquals(BillingOfficeResponseType.UNKNOWN, antwort.getType());
        }
    }

    @Nested
    @DisplayName("Einstufungen")
    class Einstufungen {

        @ParameterizedTest
        @ValueSource(strings = {
                "Die Lieferung wurde angenommen.",
                "Verarbeitung erfolgreich abgeschlossen",
                "Datei akzeptiert",
                "Status: OK"
        })
        @DisplayName("Zustimmende Rueckmeldungen gelten als angenommen")
        void erkenntAnnahme(String text) {
            assertEquals(BillingOfficeResponseType.ACCEPTED, parser.parse(text).getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Die Lieferung wurde abgelehnt.",
                "Rechnung zurueckgewiesen",
                "Der Datensatz wurde beanstandet"
        })
        @DisplayName("Ablehnende Rueckmeldungen gelten als abgelehnt")
        void erkenntAblehnung(String text) {
            assertEquals(BillingOfficeResponseType.REJECTED, parser.parse(text).getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Syntaxfehler in Segment UNB",
                "Ungueltige Laenge im Feld 3",
                "Feldverschiebung erkannt"
        })
        @DisplayName("Formfehler gelten als Syntaxfehler")
        void erkenntSyntaxfehler(String text) {
            assertEquals(BillingOfficeResponseType.SYNTAX_ERROR, parser.parse(text).getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Timeout bei der Uebertragung",
                "Der Endpunkt ist nicht erreichbar",
                "Verbindungsfehler zum Annahmesystem"
        })
        @DisplayName("Uebertragungsprobleme gelten als technischer Fehler")
        void erkenntTechnischenFehler(String text) {
            assertEquals(BillingOfficeResponseType.TECHNICAL_ERROR, parser.parse(text).getType());
        }

        @Test
        @DisplayName("Ein Syntaxfehler wiegt schwerer als ein zustimmender Nebensatz")
        void syntaxfehlerSchlaegtZustimmung() {
            BillingOfficeResponse antwort = parser.parse(
                    "Die Datei konnte erfolgreich entschluesselt werden. Typ: PLAIN_EDIFACT Syntaxfehler");

            assertEquals(BillingOfficeResponseType.SYNTAX_ERROR, antwort.getType());
        }
    }

    @Nested
    @DisplayName("Randfaelle")
    class Randfaelle {

        @Test
        @DisplayName("Eine leere Antwort gilt nicht als Annahme")
        void leereAntwortIstUnbekannt() {
            assertEquals(BillingOfficeResponseType.UNKNOWN, parser.parse("").getType());
            assertEquals(BillingOfficeResponseType.UNKNOWN, parser.parse("   ").getType());
            assertEquals(BillingOfficeResponseType.UNKNOWN, parser.parse(null).getType());
        }

        @Test
        @DisplayName("Eine unverstaendliche Antwort gilt nicht als Annahme")
        void unbekannteAntwortIstUnbekannt() {
            assertEquals(BillingOfficeResponseType.UNKNOWN,
                    parser.parse("Bitte wenden Sie sich an Ihren Ansprechpartner.").getType());
        }

        @Test
        @DisplayName("Die Rohantwort bleibt erhalten")
        void behaeltRohtext() {
            String roh = "Die Lieferung wurde angenommen.";

            assertEquals(roh, parser.parse(roh).getRawContent());
        }

        @Test
        @DisplayName("parseOderScheitern wirft bei jeder Nicht-Annahme")
        void scheitertBeiNichtAnnahme() {
            assertNotNull(parser.parseOderScheitern("Lieferung angenommen"));
            assertThrows(IllegalStateException.class, () -> parser.parseOderScheitern("abgelehnt"));
            assertThrows(IllegalStateException.class, () -> parser.parseOderScheitern(null));
        }
    }
}
