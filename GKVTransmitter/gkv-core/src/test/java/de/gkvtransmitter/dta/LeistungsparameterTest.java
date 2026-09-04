package de.gkvtransmitter.dta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Blueprint;

/**
 * Prueft das Auslesen der Leistungsangaben aus einer Blaupause.
 *
 * <p>Die Werte standen zuvor fest im Quelltext der {@link DtaFactory}, die
 * ausgewaehlte Blaupause ging in die erzeugte Nachricht gar nicht ein. Diese
 * Tests halten fest, dass sie jetzt wirkt - und dass eine unvollstaendige oder
 * unlesbare Blaupause nicht zum Abbruch fuehrt.</p>
 */
class LeistungsparameterTest {

    private static Blueprint blueprint(String payload) {
        return new Blueprint("Test", "vorlage", payload, OffsetDateTime.now());
    }

    @Nested
    @DisplayName("Werte aus der Blaupause")
    class AusBlaupause {

        @Test
        @DisplayName("Der Einzelbetrag wird uebernommen")
        void liestEinzelbetrag() {
            Leistungsparameter p = Leistungsparameter.ausBlueprint(blueprint(
                    "{\"fields\":{\"Durchschnittlicher Einzelbetrag\":\"2500,00\"}}"));

            assertEquals(new BigDecimal("2500.00"), p.einzelbetrag());
        }

        @Test
        @DisplayName("Abrechnungscode und Tarifkennzeichen werden uebernommen")
        void liestSchluessel() {
            Leistungsparameter p = Leistungsparameter.ausBlueprint(blueprint(
                    "{\"fields\":{\"Abrechnungscode\":\"62\",\"Tarifkennzeichen\":\"12345\"}}"));

            assertEquals("62", p.abrechnungscode());
            assertEquals("12345", p.tarifkennzeichen());
            assertEquals("62:12345", p.leistungserbringergruppe());
        }

        @Test
        @DisplayName("Die Positionsnummer wird uebernommen")
        void liestPositionsnummer() {
            Leistungsparameter p = Leistungsparameter.ausBlueprint(blueprint(
                    "{\"fields\":{\"Abrechnungspositionsnummer\":\"306050699\"}}"));

            assertEquals("306050699", p.positionsnummer());
        }

        @Test
        @DisplayName("Nicht angegebene Werte bleiben auf der Vorbelegung")
        void ergaenztFehlendeWerte() {
            Leistungsparameter p = Leistungsparameter.ausBlueprint(blueprint(
                    "{\"fields\":{\"Abrechnungscode\":\"62\"}}"));

            assertEquals("62", p.abrechnungscode());
            assertEquals(Leistungsparameter.VORBELEGUNG.tarifkennzeichen(), p.tarifkennzeichen());
            assertEquals(Leistungsparameter.VORBELEGUNG.einzelbetrag(), p.einzelbetrag());
        }
    }

    @Nested
    @DisplayName("Betragsschreibweisen")
    class Betraege {

        @Test
        @DisplayName("Deutsche Schreibweise mit Komma wird gelesen")
        void liestKomma() {
            Leistungsparameter p = Leistungsparameter.ausFeldern(
                    Map.of("Durchschnittlicher Einzelbetrag", "1234,56"));

            assertEquals(new BigDecimal("1234.56"), p.einzelbetrag());
        }

        @Test
        @DisplayName("Englische Schreibweise mit Punkt wird gelesen")
        void liestPunkt() {
            // Die Oberflaeche speichert Formularwerte so, wie sie eingegeben
            // wurden - je nach Eingabefeld mit Punkt oder Komma.
            Leistungsparameter p = Leistungsparameter.ausFeldern(
                    Map.of("Durchschnittlicher Einzelbetrag", "1234.56"));

            assertEquals(new BigDecimal("1234.56"), p.einzelbetrag());
        }

        @Test
        @DisplayName("Ein Tausenderpunkt wird nicht als Dezimaltrenner gelesen")
        void liestTausenderpunkt() {
            Leistungsparameter p = Leistungsparameter.ausFeldern(
                    Map.of("Durchschnittlicher Einzelbetrag", "15.000,00"));

            assertEquals(new BigDecimal("15000.00"), p.einzelbetrag());
        }

        @Test
        @DisplayName("Ein unlesbarer Betrag faellt auf die Vorbelegung zurueck")
        void faelltBeiUnlesbaremBetragZurueck() {
            Leistungsparameter p = Leistungsparameter.ausFeldern(
                    Map.of("Durchschnittlicher Einzelbetrag", "viel"));

            assertEquals(Leistungsparameter.VORBELEGUNG.einzelbetrag(), p.einzelbetrag());
        }

        @Test
        @DisplayName("Betraege werden in DTA-Schreibweise ausgegeben")
        void formatiertMitKomma() {
            Leistungsparameter p = Leistungsparameter.ausFeldern(
                    Map.of("Durchschnittlicher Einzelbetrag", "1234.5"));

            assertEquals("1234,50", p.einzelbetragFormatiert());
            assertEquals("0,00", p.zuzahlungFormatiert());
        }
    }

    @Nested
    @DisplayName("Rueckfallebene")
    class Rueckfall {

        @Test
        @DisplayName("Ohne Blaupause gilt die Vorbelegung")
        void ohneBlaupause() {
            assertSame(Leistungsparameter.VORBELEGUNG, Leistungsparameter.ausBlueprint(null));
        }

        @Test
        @DisplayName("Eine leere Nutzlast fuehrt zur Vorbelegung")
        void leereNutzlast() {
            assertSame(Leistungsparameter.VORBELEGUNG, Leistungsparameter.ausBlueprint(blueprint("")));
        }

        @Test
        @DisplayName("Unlesbares JSON fuehrt nicht zum Abbruch")
        void unlesbaresJson() {
            // Ein harter Fehler wuerde hier eine Abrechnung verhindern, die mit
            // den bisherigen Werten korrekt gewesen waere.
            assertSame(Leistungsparameter.VORBELEGUNG,
                    Leistungsparameter.ausBlueprint(blueprint("{kein gueltiges json")));
        }

        @Test
        @DisplayName("Eine Nutzlast ohne Feldabschnitt fuehrt zur Vorbelegung")
        void ohneFeldabschnitt() {
            assertSame(Leistungsparameter.VORBELEGUNG,
                    Leistungsparameter.ausBlueprint(blueprint("{\"template\":\"x\"}")));
        }

        @Test
        @DisplayName("Die Vorbelegung entspricht den zuvor fest verdrahteten Werten")
        void vorbelegungEntsprichtAltemVerhalten() {
            assertEquals(new BigDecimal("15000.00"), Leistungsparameter.VORBELEGUNG.einzelbetrag());
            assertEquals("61", Leistungsparameter.VORBELEGUNG.abrechnungscode());
            assertEquals("00000", Leistungsparameter.VORBELEGUNG.tarifkennzeichen());
            assertEquals("306050601", Leistungsparameter.VORBELEGUNG.positionsnummer());
        }
    }
}
