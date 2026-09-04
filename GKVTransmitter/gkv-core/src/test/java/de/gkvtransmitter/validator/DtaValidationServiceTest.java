package de.gkvtransmitter.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.dta.DtaDocument;

/**
 * Prueft die Validierung gegen die mitgelieferte Referenznachricht.
 *
 * <p>Grundlage ist {@code Information/Valide.DTA}. Diese Datei gilt im Projekt
 * als korrekte Lieferung, also muss die Validierung sie ohne Beanstandung
 * durchlassen. Jeder Einzeltest verletzt danach genau eine Vorgabe, damit
 * feststeht, dass die jeweilige Regel den Fehler auch tatsaechlich findet und
 * nicht nur zufaellig etwas meldet.</p>
 */
class DtaValidationServiceTest {

    private final DtaValidationService service = DtaValidationService.standard();

    /** Wortgleich mit Information/Valide.DTA. */
    private static final String REFERENZ = String.join("\n",
            "UNB+UNOC:3+123456780+987654324+20260308:1200+00001+H+HEB26030801+1'",
            "UNH+00001+SLGA:21:0:0'",
            "FKT+01++123456780+987654324+987654324+123456780'",
            "REC+00000000:0+20260307+1'",
            "UST+19'",
            "GES+00+120000,00+120000,00'",
            "GES+99+120000,00+120000,00'",
            "NAM+MUSTERMANN+MARIA+DR'",
            "UNT+000008+00001'",
            "UNH+00002+SLLA:21:0:0'",
            "FKT+01++123456780+987654324+987654324+123456780'",
            "REC+00000000:0+20260307+1'",
            "INV+012345678901++1+HEB2403001'",
            "NAD+MUSTERMANN+ANNA+19900101+MUSTERSTRASSE 1+12345+MUSTERSTADT'",
            "ENF+01+61:00000+306050601+08,00+15000,00+20240301+0,00'",
            "BES+120000,00'",
            "UNT+000008+00002'",
            "",
            "UNZ+000002+00001'");

    /**
     * Entfernt eine Zeile aus der Referenz.
     *
     * <p>Beide Schreibweisen sind noetig, weil die letzte Zeile keinen
     * Zeilenumbruch hinter sich hat - sonst bliebe sie unbemerkt stehen und der
     * Test wuerde gegen eine unveraenderte Nachricht pruefen.</p>
     */
    private static String ohne(String zeile) {
        if (REFERENZ.contains(zeile + "\n")) {
            return REFERENZ.replace(zeile + "\n", "");
        }
        return REFERENZ.replace("\n" + zeile, "");
    }

    private static String ersetze(String alt, String neu) {
        return REFERENZ.replace(alt, neu);
    }

    private static boolean enthaeltCode(ValidationReport bericht, String code) {
        return bericht.getMessages().stream().anyMatch(m -> m.code().equals(code));
    }

    @Test
    @DisplayName("Die Referenznachricht wird ohne Beanstandung angenommen")
    void referenzIstGueltig() {
        ValidationReport bericht = service.pruefe(REFERENZ);

        assertTrue(bericht.istVersandfaehig(),
                "Die als gueltig gefuehrte Referenz wurde beanstandet:\n" + bericht.alsText());
        assertTrue(bericht.getErrors().isEmpty(), bericht.alsText());
    }

    @Nested
    @DisplayName("Nachrichtenrahmen")
    class Rahmen {

        @Test
        @DisplayName("Eine falsche Nachrichtenanzahl im UNZ faellt auf")
        void erkenntFalscheNachrichtenanzahl() {
            ValidationReport bericht = service.pruefe(ersetze("UNZ+000002+00001'", "UNZ+000005+00001'"));

            assertTrue(enthaeltCode(bericht, "UNZ_ANZAHL"), bericht.alsText());
            assertFalse(bericht.istVersandfaehig());
        }

        @Test
        @DisplayName("Eine abweichende Datenaustauschreferenz faellt auf")
        void erkenntAbweichendeReferenz() {
            ValidationReport bericht = service.pruefe(ersetze("UNZ+000002+00001'", "UNZ+000002+00099'"));

            assertTrue(enthaeltCode(bericht, "RAHMEN_REFERENZ"), bericht.alsText());
        }

        @Test
        @DisplayName("Eine Uebertragung ohne UNZ wird abgelehnt")
        void erkenntFehlendesUnz() {
            ValidationReport bericht = service.pruefe(ohne("UNZ+000002+00001'"));

            assertTrue(enthaeltCode(bericht, "RAHMEN_UNZ_FEHLT"), bericht.alsText());
        }

        @Test
        @DisplayName("Eine Uebertragung ohne UNB wird abgelehnt")
        void erkenntFehlendesUnb() {
            ValidationReport bericht = service.pruefe(ohne("UNB+UNOC:3+123456780+987654324+20260308:1200+00001+H+HEB26030801+1'"));

            assertTrue(enthaeltCode(bericht, "RAHMEN_UNB_FEHLT"), bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Nachrichtenabschluss")
    class Abschluss {

        @Test
        @DisplayName("Eine falsche Segmentanzahl im UNT faellt auf")
        void erkenntFalscheSegmentanzahl() {
            ValidationReport bericht = service.pruefe(ersetze("UNT+000008+00001'", "UNT+000012+00001'"));

            assertTrue(enthaeltCode(bericht, "UNT_ANZAHL"), bericht.alsText());
        }

        @Test
        @DisplayName("Eine abweichende Nachrichtennummer im UNT faellt auf")
        void erkenntAbweichendeNachrichtennummer() {
            ValidationReport bericht = service.pruefe(ersetze("UNT+000008+00002'", "UNT+000008+00077'"));

            assertTrue(enthaeltCode(bericht, "NACHRICHT_NUMMER"), bericht.alsText());
        }

        @Test
        @DisplayName("Eine nicht abgeschlossene Nachricht faellt auf")
        void erkenntFehlendesUnt() {
            ValidationReport bericht = service.pruefe(ohne("UNT+000008+00001'"));

            assertTrue(enthaeltCode(bericht, "NACHRICHT_NICHT_GESCHLOSSEN")
                    || enthaeltCode(bericht, "UNH_OHNE_UNT"), bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Institutionskennzeichen")
    class Kennzeichen {

        @Test
        @DisplayName("Eine falsche Pruefziffer im Absender-IK faellt auf")
        void erkenntFalschePruefziffer() {
            // 123456780 ist gueltig, 123456781 nicht.
            ValidationReport bericht = service.pruefe(
                    ersetze("UNB+UNOC:3+123456780+987654324", "UNB+UNOC:3+123456781+987654324"));

            assertTrue(enthaeltCode(bericht, "IK_PRUEFZIFFER"), bericht.alsText());
        }

        @Test
        @DisplayName("Ein zu kurzes IK faellt auf")
        void erkenntFalscheLaenge() {
            ValidationReport bericht = service.pruefe(
                    ersetze("UNB+UNOC:3+123456780+987654324", "UNB+UNOC:3+12345+987654324"));

            assertTrue(enthaeltCode(bericht, "IK_LAENGE"), bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Betraege")
    class Betraege {

        @Test
        @DisplayName("Eine Fallsumme, die nicht zu den Leistungspositionen passt, faellt auf")
        void erkenntAbweichendeFallsumme() {
            ValidationReport bericht = service.pruefe(ersetze("BES+120000,00'", "BES+999999,00'"));

            assertTrue(enthaeltCode(bericht, "BETRAG_ENF_BES"), bericht.alsText());
        }

        @Test
        @DisplayName("Eine Gesamtsumme (GES 00), die nicht zur Fallsumme passt, ist ein Fehler")
        void erkenntAbweichendeGesamtsumme() {
            ValidationReport bericht = service.pruefe(
                    ersetze("GES+00+120000,00+120000,00'", "GES+00+130000,00+130000,00'"));

            assertTrue(enthaeltCode(bericht, "BETRAG_GES_BES"), bericht.alsText());
            assertFalse(bericht.istVersandfaehig());
        }

        @Test
        @DisplayName("Eine Abweichung bei GES 99 ist nur ein Hinweis")
        void meldetAbweichungBeiSchluessel99AlsHinweis() {
            // Schluessel 99 richtet sich laut den Codelisten nach der
            // Pruefvorgabe der Kasse und darf abweichen.
            ValidationReport bericht = service.pruefe(
                    ersetze("GES+99+120000,00+120000,00'", "GES+99+130000,00+130000,00'"));

            assertTrue(enthaeltCode(bericht, "BETRAG_GES99_ABWEICHUNG"), bericht.alsText());
            assertTrue(bericht.istVersandfaehig(),
                    "Eine Abweichung bei Schluessel 99 darf den Versand nicht aufhalten");
        }

        @Test
        @DisplayName("Eine geaenderte Menge verschiebt die Summe und faellt auf")
        void erkenntFalscheMenge() {
            ValidationReport bericht = service.pruefe(
                    ersetze("ENF+01+61:00000+306050601+08,00+15000,00+20240301+0,00'",
                            "ENF+01+61:00000+306050601+07,00+15000,00+20240301+0,00'"));

            assertTrue(enthaeltCode(bericht, "BETRAG_ENF_BES"), bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Versichertenangaben")
    class Versicherte {

        @Test
        @DisplayName("Ein fehlender Nachname faellt auf")
        void erkenntFehlendenNachnamen() {
            ValidationReport bericht = service.pruefe(
                    ersetze("NAD+MUSTERMANN+ANNA+", "NAD++ANNA+"));

            assertTrue(enthaeltCode(bericht, "VERSICHERTER_NACHNAME"), bericht.alsText());
        }

        @Test
        @DisplayName("Ein Geburtsdatum in der Zukunft faellt auf")
        void erkenntGeburtsdatumInZukunft() {
            ValidationReport bericht = service.pruefe(ersetze("+19900101+", "+21000101+"));

            assertTrue(enthaeltCode(bericht, "GEBURTSDATUM_ZUKUNFT"), bericht.alsText());
        }

        @Test
        @DisplayName("Ein unlesbares Geburtsdatum faellt auf")
        void erkenntUnlesbaresGeburtsdatum() {
            ValidationReport bericht = service.pruefe(ersetze("+19900101+", "+01.01.1990+"));

            assertTrue(enthaeltCode(bericht, "GEBURTSDATUM_FORMAT"), bericht.alsText());
        }

        @Test
        @DisplayName("Eine Versichertennummer aus lauter Nullen faellt auf")
        void erkenntLeereVersichertennummer() {
            ValidationReport bericht = service.pruefe(
                    ersetze("INV+012345678901++1+", "INV+000000000000++1+"));

            assertTrue(enthaeltCode(bericht, "VERSICHERTENNUMMER_NULL"), bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Syntax")
    class Syntax {

        @Test
        @DisplayName("Ein fehlendes Abschlusszeichen faellt auf")
        void erkenntFehlendenAbschluss() {
            ValidationReport bericht = service.pruefe(ersetze("UST+19'", "UST+19"));

            assertTrue(enthaeltCode(bericht, "SYNTAX_ABSCHLUSS"), bericht.alsText());
        }

        @Test
        @DisplayName("Ein unbekanntes Segment wird gemeldet, haelt den Versand aber nicht auf")
        void meldetUnbekanntesSegmentAlsWarnung() {
            ValidationReport bericht = service.pruefe(ersetze("UST+19'", "XYZ+19'"));

            assertTrue(enthaeltCode(bericht, "SYNTAX_UNBEKANNT"), bericht.alsText());
            assertTrue(bericht.getWarnings().stream().anyMatch(m -> m.code().equals("SYNTAX_UNBEKANNT")),
                    "Ein unbekanntes Segment soll eine Warnung sein, kein Fehler");
        }

        @Test
        @DisplayName("Eine leere Nachricht wird abgelehnt")
        void erkenntLeereNachricht() {
            ValidationReport bericht = service.pruefe("");

            assertTrue(enthaeltCode(bericht, "SYNTAX_LEER"), bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Erweiterbarkeit")
    class Erweiterbarkeit {

        @Test
        @DisplayName("Eine zusaetzliche Regel wird mit ausgefuehrt")
        void fuehrtZusaetzlicheRegelAus() {
            ValidationRule eigene = new ValidationRule() {
                @Override
                public String getName() {
                    return "Testregel";
                }

                @Override
                public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
                    bericht.warning("EIGENE_REGEL", "", "Wurde ausgefuehrt.");
                }
            };

            ValidationReport bericht = service.mitZusaetzlicherRegel(eigene).pruefe(REFERENZ);

            assertTrue(enthaeltCode(bericht, "EIGENE_REGEL"), bericht.alsText());
        }

        @Test
        @DisplayName("Eine abstuerzende Regel stoppt die uebrigen nicht")
        void faengtFehlerhafteRegelAb() {
            ValidationRule kaputt = new ValidationRule() {
                @Override
                public String getName() {
                    return "Kaputte Regel";
                }

                @Override
                public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
                    throw new IllegalStateException("absichtlich");
                }
            };

            DtaValidationService erweitert = new DtaValidationService(List.of(kaputt))
                    .mitZusaetzlicherRegel(new de.gkvtransmitter.validator.rules.NachrichtenRahmenRegel());

            ValidationReport bericht = erweitert.pruefe(ersetze("UNZ+000002+00001'", "UNZ+000009+00001'"));

            assertTrue(enthaeltCode(bericht, "REGEL_ABGEBROCHEN"), bericht.alsText());
            assertTrue(enthaeltCode(bericht, "UNZ_ANZAHL"),
                    "Die nachfolgende Regel muss trotz Absturz gelaufen sein:\n" + bericht.alsText());
        }
    }

    @Nested
    @DisplayName("Bericht")
    class Bericht {

        @Test
        @DisplayName("Warnungen allein halten den Versand nicht auf")
        void warnungenBlockierenNicht() {
            ValidationReport bericht = ValidationReport.builder()
                    .warning("W", "", "nur eine Warnung")
                    .build();

            assertTrue(bericht.istVersandfaehig());
            assertFalse(bericht.hatFehler());
        }

        @Test
        @DisplayName("Ein Fehler haelt den Versand auf")
        void fehlerBlockiert() {
            ValidationReport bericht = ValidationReport.builder()
                    .error("E", "", "ein Fehler")
                    .build();

            assertFalse(bericht.istVersandfaehig());
            assertEquals(1, bericht.getErrors().size());
        }

        @Test
        @DisplayName("Berichte lassen sich zusammenfuehren")
        void berichteLassenSichAddieren() {
            ValidationReport a = ValidationReport.builder().warning("W", "", "a").build();
            ValidationReport b = ValidationReport.builder().error("E", "", "b").build();

            ValidationReport zusammen = a.plus(b);

            assertEquals(2, zusammen.getMessages().size());
            assertTrue(zusammen.hatFehler());
        }
    }
}
