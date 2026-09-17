package de.gkvtransmitter.wartung;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Betriebsdaten;

/**
 * Der Wartungskalender.
 *
 * <p>Er ist die Bedingung, unter der der eigene Uebermittlungsweg ueberhaupt
 * sinnvoll ist: Die Ersparnis gegenueber einer Abrechnungsstelle liegt bei
 * vierzig Kursterminen im Jahr zwischen fuenfzig und zweihundertvierzig Euro.
 * Kostet die Pflege mehr als ein bis drei Arbeitsstunden, ist der eigene Weg
 * trotz niedrigerer Gebuehr teurer.</p>
 */
@DisplayName("Der Wartungskalender")
class WartungskalenderTest {

    private static final LocalDate HEUTE = LocalDate.of(2026, 9, 17);

    private static Wartungskalender mitZertifikatBis(String datum) {
        Betriebsdaten betrieb = new Betriebsdaten();
        betrieb.setZertifikatBis(datum);
        return new Wartungskalender(betrieb, Unterlagenstand.lade());
    }

    /**
     * Die Seite schweigt, solange nichts ansteht.
     *
     * <p>Eine Meldung, die bei jedem Start erscheint, wird nach der dritten
     * Woche nicht mehr gelesen - dann meldet sich auch das Zertifikat
     * vergeblich.</p>
     */
    @Test
    @DisplayName("schweigt, solange nichts innerhalb von sechs Wochen ansteht")
    void schweigtOhneFaelligkeit() {
        assertTrue(mitZertifikatBis("2027-06-30").anstehende(HEUTE).isEmpty(),
                "" + mitZertifikatBis("2027-06-30").anstehende(HEUTE));
    }

    @Test
    @DisplayName("meldet ein Zertifikat, das in vier Wochen ablaeuft")
    void meldetZertifikat() {
        List<Faelligkeit> anstehend = mitZertifikatBis("2026-10-15").anstehende(HEUTE);

        assertEquals(1, anstehend.size(), "" + anstehend);
        assertTrue(anstehend.get(0).was().contains("Zertifikat"));
        assertEquals(28, anstehend.get(0).tageBis(HEUTE));
    }

    /**
     * Ein ueberschrittener Termin bleibt stehen.
     *
     * <p>Er ist dringender als einer, der bevorsteht - und verschwaende sonst
     * still von der Seite, genau dann, wenn er zaehlt.</p>
     */
    @Test
    @DisplayName("meldet ein abgelaufenes Zertifikat weiter")
    void abgelaufenesZertifikat() {
        List<Faelligkeit> anstehend = mitZertifikatBis("2026-08-01").anstehende(HEUTE);

        assertFalse(anstehend.isEmpty(), "Ein ueberschrittener Termin darf nicht verschwinden");
        assertTrue(anstehend.get(0).ueberfaellig(HEUTE));
    }

    /**
     * Ohne erfasstes Datum steht der Punkt trotzdem da.
     *
     * <p>Ein Kalender, der nur zeigt, was er weiss, sieht vollstaendig aus und
     * ist es nicht. Das Zertifikat ist die eine Aufgabe, die bleibt - es darf
     * nicht fehlen, nur weil niemand ein Datum eingetragen hat.</p>
     */
    @Test
    @DisplayName("fuehrt das Zertifikat auch ohne erfasstes Datum")
    void zertifikatOhneDatum() {
        Faelligkeit zertifikat = new Wartungskalender(null, Unterlagenstand.lade()).alle(HEUTE)
                .stream().filter(f -> f.was().contains("Zertifikat")).findFirst().orElseThrow();

        assertFalse(zertifikat.hatDatum());
        assertTrue(zertifikat.woher().contains("Betriebsdaten"),
                "Es muss dastehen, wo das Datum hingehoert: " + zertifikat.woher());
    }

    /**
     * Die bevorstehende Anlage 1 Version 22 steht im Kalender.
     *
     * <p>Anzuwenden ab 01.02.2027 - ein Termin, den vor G1 niemand kannte.</p>
     */
    @Test
    @DisplayName("uebernimmt die Termine aus dem Stand der Unterlagen")
    void uebernimmtUnterlagentermine() {
        List<Faelligkeit> alle = mitZertifikatBis(null).alle(HEUTE);

        assertTrue(alle.stream().anyMatch(f -> LocalDate.of(2027, 2, 1).equals(f.faelligAm())),
                "" + alle.stream().map(Faelligkeit::was).toList());
    }

    /**
     * Was kein Datum hat, steht mit dem Grund da.
     *
     * <p>Die Kostentraegerdatei wird vierteljaehrlich erneuert - aber die
     * Anwendung liest sie noch nicht ein, es gibt also kein Dateidatum. Ein
     * erfundener Termin waere schlimmer als keiner.</p>
     */
    @Test
    @DisplayName("nennt bei fehlendem Datum den Grund")
    void nenntDenGrund() {
        List<Faelligkeit> ohneDatum = mitZertifikatBis("2027-06-30").alle(HEUTE).stream()
                .filter(f -> !f.hatDatum()).toList();

        assertEquals(2, ohneDatum.size(),
                "Zertifikat ohne Datum gibt es hier nicht - erwartet: Kostentraegerdatei,"
                        + " Positionsnummernverzeichnis: " + ohneDatum);
        assertTrue(ohneDatum.stream().allMatch(f -> f.woher() != null && !f.woher().isBlank()));
    }

    /** Die naechste Faelligkeit steht oben, die ohne Datum unten. */
    @Test
    @DisplayName("ordnet nach Datum, Undatiertes zuletzt")
    void ordnetNachDatum() {
        List<Faelligkeit> alle = mitZertifikatBis("2026-10-15").alle(HEUTE);

        assertTrue(alle.get(0).hatDatum());
        assertFalse(alle.get(alle.size() - 1).hatDatum());
    }
}
