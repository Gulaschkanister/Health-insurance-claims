package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.dta.DtaFactory;
import de.gkvtransmitter.dta.Leistungsparameter;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.parser.json.JsonParserFactory;
import de.gkvtransmitter.util.Institutionskennzeichen;
import de.gkvtransmitter.validator.DtaValidationService;
import de.gkvtransmitter.validator.ValidationMessage;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Prueft, dass sich mit den Testdaten wirklich abrechnen laesst.
 *
 * <p>Der Anlass ist eine Beobachtung von Simon: die Testdaten fuehrten bis zum
 * 05.09.2026 zwangslaeufig zu "Pruefung nicht bestanden". Die
 * Institutionskennzeichen lauteten {@code 1001} und {@code 201} statt neun
 * Stellen mit Pruefziffer, Geburtsdaten fehlten, und die Blaupause zeigte auf
 * eine Vorlage, die es nicht gab. Wer die Anwendung damit ausprobierte, uebte
 * auf falscher Grundlage und bekam den Fehler erst ganz am Ende.</p>
 *
 * <p>Diese Tests halten das fest, statt sich darauf zu verlassen, dass es
 * jemandem auffaellt.</p>
 */
@DisplayName("Testdaten")
class TestdatenTest {

    private static final int TERMINE = 4;

    @Nested
    @DisplayName("Die Kennzeichen")
    class Kennzeichen {

        @Test
        @DisplayName("haben alle eine richtige Pruefziffer")
        void habenRichtigePruefziffer() {
            List<Integer> alle = new ArrayList<>();
            Testdaten.dienstleister().forEach(d -> {
                alle.add(d.getIk());
                alle.add(d.getKassenIk());
            });
            Testdaten.teilnehmerinnen().forEach(t -> {
                alle.add(t.getIk());
                alle.add(t.getKassenIk());
            });

            for (int ik : alle) {
                assertTrue(Institutionskennzeichen.istGueltig(ik),
                        "IK " + ik + " wuerde von der Kasse zurueckgewiesen");
            }
        }
    }

    @Nested
    @DisplayName("Die Teilnehmerinnen")
    class Teilnehmerinnen {

        @Test
        @DisplayName("haben ein Geburtsdatum")
        void habenGeburtsdatum() {
            Testdaten.teilnehmerinnen().forEach(t ->
                    assertTrue(t.getBirthDate() != null,
                            t.getFirstname() + " hat kein Geburtsdatum; DtaFactory setzte sonst"
                                    + " stillschweigend den 01.01.1990 ein"));
        }

        @Test
        @DisplayName("verteilen sich auf mehrere Kassen")
        void verteilenSichAufKassen() {
            long kassen = Testdaten.teilnehmerinnen().stream()
                    .map(Patient::getKassenIk)
                    .distinct()
                    .count();

            assertTrue(kassen >= 3,
                    "Mit nur einer Kasse wird die Zuordnung auf Zielordner nie benutzt, war aber " + kassen);
        }
    }

    @Nested
    @DisplayName("Die Blaupausen")
    class Blaupausen {

        @Test
        @DisplayName("zeigen auf eine Vorlage, die es gibt")
        void zeigenAufEchteVorlage() {
            String vorlage = ersteVorlage();

            Testdaten.blaupausen(vorlage).forEach(b ->
                    assertEquals(vorlage, b.getTemplateName(),
                            "Sonst laesst sich die Blaupause nicht einmal oeffnen"));
        }

        @Test
        @DisplayName("tragen einen Preis und fallen nicht auf die Vorbelegung zurueck")
        void tragenEigenenPreis() {
            for (Blueprint blaupause : Testdaten.blaupausen(ersteVorlage())) {
                Leistungsparameter parameter = Leistungsparameter.ausBlueprint(blaupause);

                assertFalse(parameter.einzelbetrag().equals(Leistungsparameter.VORBELEGUNG.einzelbetrag()),
                        blaupause.getName() + " rechnet mit der Vorbelegung von 15.000,00");
            }
        }
    }

    @Nested
    @DisplayName("Eine Abrechnung mit diesen Daten")
    class EineAbrechnung {

        @Test
        @DisplayName("besteht die Pruefung ohne Beanstandung")
        void bestehtDiePruefung() {
            List<ServiceProvider> dienstleister = Testdaten.dienstleister();
            List<Patient> teilnehmerinnen = Testdaten.teilnehmerinnen();
            List<PersonGroup> gruppen = Testdaten.gruppen(teilnehmerinnen, dienstleister);
            Blueprint blaupause = Testdaten.blaupausen(ersteVorlage()).get(0);

            DtaValidationService pruefung = DtaValidationService.standard();
            long referenz = 1;

            for (PersonGroup gruppe : gruppen) {
                ServiceProvider leiterin = gruppe.getServiceProviders().iterator().next();
                for (Patient teilnehmerin : gruppe.getPatients()) {
                    // Kennnummern vergibt sonst Hibernate; ohne sie waere die
                    // Versichertennummer im INV lauter Nullen und wuerde als
                    // fehlend beanstandet.
                    teilnehmerin.setId((int) referenz);
                    String dta = DtaFactory.buildDtaFor(
                            new Abrechnung(teilnehmerin, leiterin, blaupause, TERMINE),
                            referenz++,
                            String.valueOf(leiterin.getIk()),
                            String.valueOf(teilnehmerin.getKassenIk()));

                    ValidationReport bericht = pruefung.pruefe(dta);
                    assertTrue(bericht.istVersandfaehig(),
                            "Abrechnung fuer " + teilnehmerin.getFirstname() + " beanstandet:\n"
                                    + bericht.getMessages().stream()
                                            .map(ValidationMessage::toString)
                                            .reduce("", (a, b) -> a + "\n" + b)
                                    + "\n\n" + dta);
                }
            }
        }
    }

    private static String ersteVorlage() {
        for (DtaMessage nachricht : new JsonParserFactory().parseInvoices()) {
            return nachricht.getInvoicerName();
        }
        throw new IllegalStateException("Keine Vorlage vorhanden");
    }
}
