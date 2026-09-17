package de.gkvtransmitter.dispatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.dta.Testblaupause;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.ValidationSeverity;

/**
 * Der Abrechnungscode kommt aus dem Dienstleisterprofil.
 *
 * <p>Er stand in der Blaupause, also bei der Leistung. Fachlich haengt er am
 * <b>Beruf</b>: {@code 50} ist Hebamme/Entbindungspfleger, {@code 61}
 * Leistungserbringer von Rehabilitationssport (Anlage 3, Abschnitt 8.1.5). Wer
 * zwei Kursarten anbot, pflegte denselben Code zweimal - und aus ihm ergibt
 * sich der Leistungsbereich im {@code UNB}, in dem das Projekt schon einmal
 * falsch lag.</p>
 */
class AbrechnungscodeAusProfilTest {

    @TempDir
    Path tempDir;

    /** Die Kasse, fuer die ein Ziel hinterlegt ist. */
    private static final int KASSE = 108310400;

    private Versandergebnis laufMit(ServiceProvider erbringer) {
        Map<Integer, BillingOfficeEndpoint> endpunkte = new LinkedHashMap<>();
        endpunkte.put(KASSE, BillingOfficeEndpoint.fileEndpoint(KASSE, "Test-Kasse", tempDir.resolve("ziel")));
        DtaDispatchService dienst = new DtaDispatchService(
                new BillingOfficeEndpointRegistry(endpunkte, tempDir.resolve("fallback")),
                new FileBillingOfficeTransport());
        return dienst.generateAndRoute(List.of(abrechnung(erbringer)), tempDir);
    }

    private static Abrechnung abrechnung(ServiceProvider erbringer) {
        Blueprint blaupause = Testblaupause.mitPreis();
        Patient anna = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345,
                KASSE, KASSE, LocalDate.of(1990, 1, 1));
        anna.setId(1);
        return new Abrechnung(anna, erbringer, blaupause, 1);
    }

    private static ServiceProvider hebamme() {
        ServiceProvider erbringer = new ServiceProvider("Maria", "Hebamme", "Kurweg", "DE", "3",
                28203, 104940005, 101560000, LocalDate.of(1980, 1, 9));
        erbringer.setId(10);
        return erbringer;
    }

    /** Die erste erzeugte Datei unterhalb des Zielordners. */
    private String erzeugteDatei() throws Exception {
        try (var dateien = Files.walk(tempDir.resolve("ziel"))) {
            Path datei = dateien.filter(Files::isRegularFile).findFirst().orElseThrow();
            return Files.readString(datei);
        }
    }

    /**
     * Der Code aus dem Profil steht in der Nachricht - und zwar an beiden
     * Stellen, an denen er wirkt.
     *
     * <p>Im {@code ENF} als erster Teil der Leistungserbringergruppe, im
     * {@code UNB} als abgeleiteter Leistungsbereich: {@code 61} ist Bereich
     * {@code H}. Die Blaupause nennt keinen Code, es gilt also die Vorbelegung
     * {@code 50} - Bereich {@code F}.</p>
     */
    @Test
    @DisplayName("Traegt der Dienstleister einen Code, steht er in der Nachricht")
    void profilcodeGehtInDieNachricht() throws Exception {
        ServiceProvider erbringer = hebamme();
        erbringer.setAbrechnungscode("61");

        laufMit(erbringer);
        String dta = erzeugteDatei();

        assertTrue(dta.contains("ENF+01+61:"),
                "Die Leistungserbringergruppe beginnt mit dem Abrechnungscode: " + dta);
        assertTrue(dta.contains("+H+"),
                "Aus 61 folgt der Leistungsbereich H im UNB: " + dta);
    }

    /**
     * Ohne Code am Profil bleibt es bei der Blaupause.
     *
     * <p>Ein leeres Profilfeld darf eine gepflegte Blaupause nicht
     * ueberschreiben - sonst haette das Einfuehren des Feldes jeden
     * bestehenden Dienstleister auf einen leeren Code gesetzt.</p>
     */
    @Test
    @DisplayName("Ohne Code am Profil gilt weiter die Blaupause")
    void ohneProfilcodeBleibtDieBlaupause() throws Exception {
        laufMit(hebamme());

        assertTrue(erzeugteDatei().contains("ENF+01+50:"),
                "Ohne Angabe im Profil gilt der Wert aus der Blaupause");
    }

    /**
     * Weichen beide voneinander ab, gewinnt das Profil - aber nicht still.
     *
     * <p>Ein stiller Wechsel des Abrechnungscodes ist genau der Fall, der in
     * Pruefstufe 3 als Zurueckweisung zurueckkommt: Die Datei ist in sich
     * stimmig und nennt trotzdem den falschen Beruf.</p>
     */
    @Test
    @DisplayName("Weicht das Profil von der Blaupause ab, wird das gemeldet")
    void abweichungWirdGemeldet() {
        ServiceProvider erbringer = hebamme();
        erbringer.setAbrechnungscode("61");

        Versandergebnis ergebnis = laufMit(erbringer);

        assertTrue(ergebnis.bericht().mitGewicht(ValidationSeverity.INFO).stream()
                        .anyMatch(befund -> "ABRECHNUNGSCODE_AUS_PROFIL".equals(befund.code())),
                "Der Vorrang des Profils gehoert auf den Schirm: " + ergebnis.bericht().alsText());
        assertTrue(ergebnis.bericht().istVersandfaehig(),
                "Ein Hinweis haelt nichts auf - die Nachricht ist richtig, nur nicht erwartbar");
    }

    /** Stimmen beide ueberein, gibt es nichts zu melden. */
    @Test
    @DisplayName("Stimmen Profil und Blaupause ueberein, schweigt der Bericht")
    void gleicherCodeMeldetNichts() {
        ServiceProvider erbringer = hebamme();
        erbringer.setAbrechnungscode("50");

        Versandergebnis ergebnis = laufMit(erbringer);

        assertFalse(ergebnis.bericht().mitGewicht(ValidationSeverity.INFO).stream()
                        .anyMatch(befund -> "ABRECHNUNGSCODE_AUS_PROFIL".equals(befund.code())),
                "Wo nichts abweicht, ist auch nichts zu melden");
    }
}
