package de.gkvtransmitter.wartung;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Auf welchem Stand die Anwendung steht.
 *
 * <p><b>Der Grund steht im Projekt selbst:</b> Die verbindlichen Anlagen unter
 * {@code Information/} waren am 07.09.2026 vom Juli - zwei Monate ueberholt,
 * und es war niemandem aufgefallen. So sieht die Wartungslast in der Praxis
 * aus: nicht als Aufwand, den man einplant, sondern als <b>Veralten, das
 * niemand bemerkt</b>.</p>
 *
 * <p>Die Liste steht in {@code unterlagen/unterlagen.json} und nicht im
 * Quelltext: Wer eine neue Fassung ablegt, traegt sie dort nach, ohne die
 * Anwendung neu zu uebersetzen.</p>
 */
public final class Unterlagenstand {

    /** Wo die Liste liegt. */
    private static final String DATEI = "/unterlagen/unterlagen.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<Unterlage> unterlagen;
    private final String quelle;

    private Unterlagenstand(List<Unterlage> unterlagen, String quelle) {
        this.unterlagen = List.copyOf(unterlagen);
        this.quelle = quelle;
    }

    /** Liest die mitgelieferte Liste. */
    public static Unterlagenstand lade() {
        return lade(DATEI);
    }

    /**
     * Liest eine Liste aus dem Klassenpfad.
     *
     * <p>Eine fehlende oder unlesbare Datei fuehrt nicht zum Abbruch: Der Stand
     * ist eine Auskunft, keine Voraussetzung. Waere er es, koennte eine
     * kaputte JSON-Datei die Abrechnung aufhalten.</p>
     */
    public static Unterlagenstand lade(String pfad) {
        try (InputStream quelle = Unterlagenstand.class.getResourceAsStream(pfad)) {
            if (quelle == null) {
                return new Unterlagenstand(List.of(), null);
            }
            JsonNode wurzel = MAPPER.readTree(quelle);
            List<Unterlage> gelesen = new ArrayList<>();
            wurzel.path("unterlagen").forEach(eintrag -> gelesen.add(new Unterlage(
                    text(eintrag, "kennung"),
                    text(eintrag, "titel"),
                    text(eintrag, "datei"),
                    text(eintrag, "version"),
                    datum(eintrag, "stand"),
                    datum(eintrag, "anzuwendenAb"),
                    datum(eintrag, "gueltigBis"),
                    text(eintrag, "bemerkung"))));
            return new Unterlagenstand(gelesen, text(wurzel, "quelle"));
        } catch (Exception unlesbar) {
            return new Unterlagenstand(List.of(), null);
        }
    }

    /** Alle Unterlagen, nach Anwendungsdatum geordnet. */
    public List<Unterlage> alle() {
        return unterlagen.stream()
                .sorted(Comparator.comparing(Unterlage::anzuwendenAb,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Wo die Unterlagen im Netz stehen, oder {@code null}. */
    public String quelle() {
        return quelle;
    }

    /** Die Fassungen, die an einem Tag gelten. */
    public List<Unterlage> geltende(LocalDate tag) {
        return unterlagen.stream().filter(unterlage -> unterlage.giltAm(tag)).toList();
    }

    /**
     * Fassungen, die anzuwenden sind, aber noch nicht gelten - mit dem
     * naechsten Termin zuerst.
     *
     * <p><b>Das ist der Befund, den G1 sofort erbringt:</b> Anlage 1 und
     * Anlage 3 in der Version 22 liegen vor und sind ab dem 01.02.2027
     * anzuwenden. Das ist kein Fehler, solange das Datum nicht erreicht ist -
     * aber es ist ein Termin, den heute niemand kennt.</p>
     */
    public List<Unterlage> bevorstehende(LocalDate tag) {
        return unterlagen.stream()
                .filter(unterlage -> unterlage.stehtBevor(tag))
                .sorted(Comparator.comparing(Unterlage::anzuwendenAb))
                .toList();
    }

    /** Fassungen, deren Gueltigkeit abgelaufen ist. */
    public List<Unterlage> abgelaufene(LocalDate tag) {
        return unterlagen.stream().filter(unterlage -> unterlage.abgelaufen(tag)).toList();
    }

    /** Die naechste Unterlage, die anzuwenden sein wird. */
    public Optional<Unterlage> naechsterTermin(LocalDate tag) {
        return bevorstehende(tag).stream().findFirst();
    }

    private static String text(JsonNode knoten, String name) {
        JsonNode wert = knoten.path(name);
        return wert.isMissingNode() || wert.isNull() || wert.asText().isBlank()
                ? null : wert.asText().trim();
    }

    private static LocalDate datum(JsonNode knoten, String name) {
        String wert = text(knoten, name);
        try {
            return wert == null ? null : LocalDate.parse(wert);
        } catch (java.time.format.DateTimeParseException unlesbar) {
            // Ein unlesbares Datum ist kein Grund, die ganze Liste zu
            // verwerfen - der Eintrag steht dann eben ohne Termin da.
            return null;
        }
    }
}
