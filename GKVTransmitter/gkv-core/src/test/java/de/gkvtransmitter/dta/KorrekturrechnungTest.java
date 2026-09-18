package de.gkvtransmitter.dta;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Anschrift;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.Protokolleintrag;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.DtaValidationService;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Die Korrekturrechnung.
 *
 * <p><b>Fuer Hebammen ist das Verfahren seit dem 01.10.2025 verpflichtend</b>
 * (Anlage 1, Abschnitt 7.1) - die feste {@code FKT+01} war keine
 * Vereinfachung, sondern eine Luecke. Ohne sie gibt es keinen Weg, eine bereits
 * angenommene Rechnung zu berichtigen: Dieselbe Rechnung noch einmal als
 * Erstabrechnung zu schicken sieht fuer die Kasse wie eine Doppelabrechnung
 * aus.</p>
 */
@DisplayName("Korrekturrechnung")
class KorrekturrechnungTest {

    private final DtaValidationService pruefung = DtaValidationService.standard();

    private static final String ERBRINGER_IK = "261914007";

    private static Abrechnung abrechnung() {
        ServiceProvider erbringer = new ServiceProvider("Maria", "Hebamme", new Anschrift("Kurweg", "3", 28203, "DE"), 261914007, 101560000, LocalDate.of(1980, 1, 9));
        erbringer.setId(2);
        erbringer.setAbrechnungscode("50");
        Patient anna = new Patient("Anna", "Beispiel", new Anschrift("Musterstrasse", "1", 12345, "DE"), 108310400, 108310400, LocalDate.of(1990, 1, 1));
        anna.setId(1);
        return new Abrechnung(anna, erbringer, Testblaupause.mitPreis(), 2);
    }

    private static Korrekturbezug bezug() {
        return new Korrekturbezug(ERBRINGER_IK, 41L, LocalDate.of(2026, 3, 6), 1);
    }

    private static String dta(Rechnungsangaben rechnung) {
        Abrechnung abrechnung = abrechnung();
        return DtaFactory.buildDtaFor(abrechnung, 2L, ERBRINGER_IK, "108310400",
                Leistungsparameter.ausBlueprint(abrechnung.getBlueprint())
                        .mitAbrechnungscode("50"),
                Uebermittlungsart.ERPROBUNG, Absender.ausLeistungserbringer(ERBRINGER_IK), rechnung);
    }

    @Nested
    @DisplayName("Die erzeugte Nachricht")
    class DieNachricht {

        private final String korrektur = dta(new Rechnungsangaben(42L, 1)
                .alsKorrektur(Verarbeitungskennzeichen.KORREKTURRECHNUNG, bezug()));

        @Test
        @DisplayName("traegt das Verarbeitungskennzeichen 04 in beiden FKT")
        void kennzeichenInBeidenFkt() {
            assertEquals(2, korrektur.lines().filter(zeile -> zeile.startsWith("FKT+04+")).count(),
                    "Gesamtaufstellung und Leistungsaufstellung fuehren je ein FKT:\n" + korrektur);
        }

        /**
         * Das {@code URI} steht zwischen {@code INV} und {@code NAD}.
         *
         * <p>Anlage 1, Abschnitt 7.3 sagt genau das: "Dieses Segment befindet
         * sich in den Abrechnungsdaten (Nachrichtentyp SLLA) zwischen den
         * Segmenten INV und NAD."</p>
         */
        @Test
        @DisplayName("stellt das URI zwischen INV und NAD")
        void uriZwischenInvUndNad() {
            java.util.List<String> zeilen = korrektur.lines().toList();
            int inv = indexVon(zeilen, "INV+");
            int uri = indexVon(zeilen, "URI+");
            int nad = indexVon(zeilen, "NAD+");

            assertTrue(inv < uri && uri < nad,
                    "INV bei " + inv + ", URI bei " + uri + ", NAD bei " + nad + ":\n" + korrektur);
        }

        private int indexVon(java.util.List<String> zeilen, String vorsatz) {
            for (int i = 0; i < zeilen.size(); i++) {
                if (zeilen.get(i).startsWith(vorsatz)) {
                    return i;
                }
            }
            return -1;
        }

        /** IK, Rechnungsnummer mit Einzelnummer, Rechnungsdatum, Belegnummer. */
        @Test
        @DisplayName("nennt im URI alle vier Ursprungsangaben")
        void uriTraegtAlleAngaben() {
            assertTrue(korrektur.contains("URI+261914007+00000041:0+20260306+1'"),
                    "Anlage 1, Abschnitt 5.5.3.1:\n" + korrektur);
        }

        @Test
        @DisplayName("besteht die Pruefung")
        void bestehtDiePruefung() {
            ValidationReport bericht = pruefung.pruefe(korrektur);

            assertTrue(bericht.istVersandfaehig(), bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Eine Erstabrechnung")
    class Erstabrechnung {

        private final String erst = dta(new Rechnungsangaben(42L, 1));

        @Test
        @DisplayName("traegt FKT+01 und kein URI")
        void ohneUri() {
            assertTrue(erst.contains("FKT+01+"), erst);
            assertFalse(erst.contains("URI+"),
                    "Anlage 1, Abschnitt 7.3: URI darf nur bei Kennzeichen ungleich 01 stehen");
        }
    }

    /**
     * Nicht jeder Wert ist fuer Hebammen zulaessig.
     *
     * <p>Anlage 1, Abschnitt 7.1: im Leistungsbereich Hebammen nur {@code 02}
     * und {@code 04}. Zuzahlungsforderung und Wiederaufnahme gehoeren nicht
     * dazu, so naheliegend sie aussehen.</p>
     */
    @Nested
    @DisplayName("Der Leistungsbereich")
    class Leistungsbereich {

        @Test
        @DisplayName("laesst 02 und 04 zu")
        void zulaessige() {
            assertTrue(Verarbeitungskennzeichen.NACHFORDERUNG.zulaessigFuerHebammen());
            assertTrue(Verarbeitungskennzeichen.KORREKTURRECHNUNG.zulaessigFuerHebammen());
        }

        @Test
        @DisplayName("laesst 03 und 10 nicht zu")
        void unzulaessige() {
            assertFalse(Verarbeitungskennzeichen.ZUZAHLUNGSFORDERUNG.zulaessigFuerHebammen());
            assertFalse(Verarbeitungskennzeichen.WIEDERAUFNAHME.zulaessigFuerHebammen());
        }

        @Test
        @DisplayName("wird in der Nachricht geprueft")
        void wirdGeprueft() {
            String mitZuzahlung = dta(new Rechnungsangaben(42L, 1)
                    .alsKorrektur(Verarbeitungskennzeichen.ZUZAHLUNGSFORDERUNG, bezug()));

            ValidationReport bericht = pruefung.pruefe(mitZuzahlung);

            assertTrue(bericht.getErrors().stream()
                            .anyMatch(b -> "FKT_KENNZEICHEN_LEISTUNGSBEREICH".equals(b.code())),
                    bericht.alsText());
        }
    }

    /**
     * Worauf sich eine Korrektur beziehen darf.
     *
     * <p>Anlage 1, Abschnitt 7.2: "Voraussetzung fuer die Anwendung des
     * Korrekturverfahrens ist der erfolgreiche Dateneingang der den Nachtrag
     * betreffenden Ursprungsrechnung beim Kostentraeger." <b>Eine
     * zurueckgewiesene Rechnung ist deshalb kein Fall fuer das
     * Korrekturverfahren</b> - sie wird als Erstabrechnung erneut gesendet.</p>
     */
    @Nested
    @DisplayName("Der Bezug auf eine fruehere Lieferung")
    class DerBezug {

        private Protokolleintrag eintrag(boolean fehlerfrei) {
            Protokolleintrag eintrag = new Protokolleintrag();
            eintrag.setDateiname("patient_1.dta");
            eintrag.setErstelltAm(OffsetDateTime.now());
            eintrag.setAbsenderIk(ERBRINGER_IK);
            eintrag.setRechnungsnummer(41L);
            eintrag.setBelegnummer(1);
            eintrag.setRechnungsdatum(LocalDate.of(2026, 3, 6));
            eintrag.setFehlerfrei(fehlerfrei);
            return eintrag;
        }

        @Test
        @DisplayName("entsteht aus einem Protokolleintrag")
        void ausProtokoll() {
            Korrekturbezug bezug = Korrekturbezug.ausProtokoll(eintrag(true));

            assertEquals("URI+261914007+00000041:0+20260306+1'", bezug.alsSegment());
        }

        @Test
        @DisplayName("gilt nicht fuer eine fehlgeschlagene Uebermittlung")
        void nichtBeiFehlschlag() {
            assertFalse(eintrag(false).korrigierbar(),
                    "Anlage 1, Abschnitt 7.2 setzt den erfolgreichen Dateneingang voraus");
        }

        /** Ein Eintrag aus der Zeit vor D4 fuehrt keine Rechnungsnummer. */
        @Test
        @DisplayName("fehlt bei einem Eintrag ohne Rechnungsnummer")
        void ohneRechnungsnummer() {
            Protokolleintrag alt = eintrag(true);
            alt.setRechnungsnummer(0L);

            assertNull(Korrekturbezug.ausProtokoll(alt));
            assertFalse(alt.korrigierbar());
        }
    }
}
