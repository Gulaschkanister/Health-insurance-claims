package de.gkvtransmitter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Prueft die IK-Pruefziffernrechnung.
 *
 * <p>Die gueltigen Beispiele sind echte Kassen-IK aus
 * {@code billing-office-endpoints.json}. Sie sind die belastbarste Grundlage
 * fuer diesen Test: erfundene Nummern wuerden nur belegen, dass die Rechnung zu
 * sich selbst passt.</p>
 */
class InstitutionskennzeichenTest {

    @ParameterizedTest(name = "{1} hat ein gueltiges IK: {0}")
    @CsvSource({
            "108310400, AOK Bayern",
            "104940005, BARMER",
            "102137985, Techniker Krankenkasse",
            "101560000, DAK-Gesundheit",
            "105508890, Knappschaft",
            "109905003, KKH",
            "103170002, hkk",
            "100696012, AOK Nordost",
            "108018007, AOK Baden-Wuerttemberg",
            "107299005, AOK PLUS",
            "123456780, Beispiel aus Valide.DTA (Absender)",
            "987654324, Beispiel aus Valide.DTA (Empfaenger)"
    })
    @DisplayName("Echte Kennzeichen werden als gueltig erkannt")
    void erkenntGueltigeKennzeichen(String ik, String name) {
        assertTrue(Institutionskennzeichen.istGueltig(ik), name + " sollte gueltig sein");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "108310401",  // Pruefziffer um eins verschoben
            "108310409",
            "104940000",
            "102137980"
    })
    @DisplayName("Eine falsche Pruefziffer wird erkannt")
    void erkenntFalschePruefziffer(String ik) {
        assertFalse(Institutionskennzeichen.istGueltig(ik));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "10831040",     // eine Stelle zu kurz
            "1083104000",   // eine Stelle zu lang
            "",
            "   ",
            "10831040A",    // keine reine Ziffernfolge
            "abcdefghi"
    })
    @DisplayName("Formal unbrauchbare Eingaben gelten als ungueltig")
    void erkenntFormfehler(String ik) {
        assertFalse(Institutionskennzeichen.istGueltig(ik));
    }

    @Test
    @DisplayName("null gilt als ungueltig und fuehrt nicht zum Absturz")
    void behandeltNull() {
        assertFalse(Institutionskennzeichen.istGueltig((String) null));
    }

    @Test
    @DisplayName("Ein Kennzeichen in Zahlenform wird mit fuehrenden Nullen geprueft")
    void prueftZahlenform() {
        assertTrue(Institutionskennzeichen.istGueltig(108310400L));
        assertFalse(Institutionskennzeichen.istGueltig(108310401L));
        assertFalse(Institutionskennzeichen.istGueltig(-1L));
    }

    /**
     * Das nicht ausgefuellte Feld.
     *
     * <p>Lauter Nullen bestehen die Rechnung: alle Stellen 3 bis 8 sind 0,
     * die Summe ist 0, und 0 mod 10 ist 0 - genau die letzte Ziffer. Ein
     * {@code int} ohne Zuweisung ist 0, und {@code Person.ik} ist ein
     * {@code int}. Eine Person, bei der niemand ein IK eingetragen hatte, kam
     * so durch die Eingabepruefung, durch die Speicherpruefung und durch die
     * Validierung vor dem Versand - bis die Kasse die Lieferung zurueckwies.</p>
     *
     * <p>Gefunden am 05.09.2026, als die Beanstandung die richtige Pruefziffer
     * nennen sollte und dabei auffiel, dass sie fuer 000000000 keine zu nennen
     * hatte.</p>
     */
    @Test
    @DisplayName("Lauter Nullen sind kein Kennzeichen, obwohl die Pruefziffer stimmt")
    void erkenntDasLeereFeld() {
        assertEquals(0, Institutionskennzeichen.berechnePruefziffer("000000000"),
                "Die Rechnung geht auf - das ist ja das Tueckische daran");
        assertFalse(Institutionskennzeichen.istGueltig("000000000"));
        assertFalse(Institutionskennzeichen.istGueltig(0L),
                "Ein int ohne Zuweisung ist 0; das darf kein gueltiges IK sein");
    }

    @Test
    @DisplayName("Die Pruefziffer wird nach dem dokumentierten Verfahren berechnet")
    void berechnetPruefziffer() {
        // 108310400: Stellen 3-8 sind 8,3,1,0,4,0
        // gewichtet 2,1,2,1,2,1 -> 16,3,2,0,8,0
        // Quersummen             ->  7,3,2,0,8,0 = 20 -> letzte Ziffer 0
        assertEquals(0, Institutionskennzeichen.berechnePruefziffer("108310400"));
        assertEquals(5, Institutionskennzeichen.berechnePruefziffer("104940005"));
        assertEquals(3, Institutionskennzeichen.berechnePruefziffer("109905003"));
    }

    @Test
    @DisplayName("Die Pruefziffer haengt nur von den Stellen 3 bis 8 ab")
    void ignoriertKlassifikationUndPruefstelle() {
        // Stellen 1, 2 und 9 duerfen sich aendern, ohne das Ergebnis zu bewegen.
        assertEquals(Institutionskennzeichen.berechnePruefziffer("108310400"),
                Institutionskennzeichen.berechnePruefziffer("998310409"));
    }

    @Test
    @DisplayName("Eine zu kurze Eingabe fuer die Berechnung wird abgewiesen")
    void weistZuKurzeEingabeAb() {
        assertThrows(IllegalArgumentException.class,
                () -> Institutionskennzeichen.berechnePruefziffer("1234"));
        assertThrows(IllegalArgumentException.class,
                () -> Institutionskennzeichen.berechnePruefziffer(null));
    }
}
