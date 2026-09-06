package de.gkvtransmitter.dta;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.entity.Blueprint;

/**
 * Die abrechnungsrelevanten Angaben einer Leistungszeile.
 *
 * <p>Diese Werte standen fest im Quelltext der {@link DtaFactory}: der
 * Einzelbetrag als {@code BigDecimal.valueOf(15000)}, der Abrechnungscode als
 * Teil einer Formatzeichenkette. Jede Leistung wurde damit gleich abgerechnet,
 * unabhaengig davon, was in der ausgewaehlten Blaupause stand - die Blaupause
 * ging in die erzeugte Nachricht ueberhaupt nicht ein.</p>
 *
 * <p>Jetzt werden die Werte aus der Blaupause gelesen. Fehlt eine Angabe, greift
 * die {@link #VORBELEGUNG}, damit bestehende Blaupausen weiter funktionieren -
 * <b>ausser beim Einzelbetrag</b>, der dabei auf null faellt und vom Tor vor
 * dem Versand abgefangen wird. Die Feldnamen entsprechen denen aus
 * {@code resources/segments/enf.json}, weil die Oberflaeche die Formularwerte
 * unter genau diesen Namen ablegt.</p>
 *
 * <p><b>Diese Namensgleichheit ist die eigentliche Kopplung.</b> Bis zum
 * 05.09.2026 trug der Einzelbetrag in {@code enf.json} eine
 * {@code person}-Markierung, und {@code View.createFormular} blendet Felder mit
 * Personenrolle aus. Das Feld stand damit in keinem Formular, konnte in keiner
 * Blaupause landen, und diese Klasse fiel jedes Mal auf die Vorbelegung
 * zurueck - jede Rechnung lautete auf 15.000,00 je Termin. Wer hier einen Wert
 * ergaenzt, muss deshalb pruefen, dass das zugehoerige Feld in der
 * Segmentdefinition {@code "internal": false} traegt und keine
 * {@code person}-Markierung hat. Sonst liest diese Klasse ins Leere, ohne dass
 * irgendetwas fehlschlaegt.</p>
 */
public record Leistungsparameter(
        BigDecimal einzelbetrag,
        String abrechnungscode,
        String tarifkennzeichen,
        String positionsnummer,
        BigDecimal zuzahlung,
        String umsatzsteuersatz) {

    /** Feldname des Einzelbetrags in enf.json. */
    private static final String FELD_EINZELBETRAG = "Durchschnittlicher Einzelbetrag";
    private static final String FELD_ABRECHNUNGSCODE = "Abrechnungscode";
    private static final String FELD_TARIFKENNZEICHEN = "Tarifkennzeichen";
    private static final String FELD_POSITIONSNUMMER = "Abrechnungspositionsnummer";
    private static final String FELD_ZUZAHLUNG = "Zuzahlung pro Position";
    /** Feldname in ust.json - anders als die uebrigen kein enf-Feld. */
    private static final String FELD_UMSATZSTEUERSATZ = "Umsatzsteuersatz";

    /**
     * Alle Feldnamen, die diese Klasse aus einer Blaupause liest.
     *
     * <p>Oeffentlich, damit ein Test nachhalten kann, dass jeder davon in den
     * Segmentdefinitionen auch tatsaechlich ausfuellbar ist. Ohne diese
     * Gegenprobe konnte die Liste jahrelang Namen enthalten, die in keinem
     * Formular vorkamen - siehe {@code BlaupausenfelderTest}.</p>
     */
    public static final java.util.List<String> BLAUPAUSENFELDER = java.util.List.of(
            FELD_EINZELBETRAG,
            FELD_ABRECHNUNGSCODE,
            FELD_TARIFKENNZEICHEN,
            FELD_POSITIONSNUMMER,
            FELD_ZUZAHLUNG,
            FELD_UMSATZSTEUERSATZ);

    /**
     * Vorbelegung, falls die Blaupause nichts angibt.
     *
     * <p>Die Schluesselwerte entsprechen denen, die bisher fest im Quelltext
     * standen, und dem Beispiel in {@code Information/Valide.DTA}. Sie sind
     * bewusst als Rueckfallebene benannt und nicht als fachlich richtige
     * Vorgabe.</p>
     *
     * <p><b>Der Einzelbetrag ist seit dem 06.09.2026 null.</b> Er stand auf
     * 15.000,00 - dem Betrag aus der Beispieldatei, fuer einen Kurs um
     * Groessenordnungen zu hoch. Eine Blaupause ohne Preis rechnete damit jeden
     * Termin mit 15.000,00 ab, und weil die Nachricht in sich stimmig war
     * (BES und GES rechnen mit demselben Wert), hielt keine Regel sie auf.</p>
     *
     * <p>Null ist hier kein besserer Betrag, sondern ein Betrag, der nicht
     * durchkommt: {@code LeistungspositionRegel} weist ihn als Fehler zurueck,
     * und die Pruefung vor dem Versand haelt den ganzen Lauf an. <b>Ein
     * erfundener Wert, der plausibel aussieht, ist schlimmer als einer, den das
     * Tor abfaengt</b> - dieselbe Entscheidung wie beim fehlenden Geburtsdatum
     * in {@code DtaFactory.buildNadSegment}.</p>
     */
    public static final Leistungsparameter VORBELEGUNG = new Leistungsparameter(
            BigDecimal.ZERO, "61", "00000", "306050601", BigDecimal.ZERO, "19");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Liest die Parameter aus einer Blaupause.
     *
     * <p>Eine unlesbare Blaupause fuehrt nicht zum Abbruch: die Vorbelegung
     * greift, und die erzeugte Nachricht durchlaeuft anschliessend ohnehin die
     * Validierung. Ein harter Fehler an dieser Stelle wuerde eine Abrechnung
     * verhindern, die mit den bisherigen Werten korrekt gewesen waere.</p>
     */
    public static Leistungsparameter ausBlueprint(Blueprint blueprint) {
        if (blueprint == null || blueprint.getPayload() == null || blueprint.getPayload().isBlank()) {
            return VORBELEGUNG;
        }
        try {
            JsonNode wurzel = MAPPER.readTree(blueprint.getPayload());
            JsonNode felder = wurzel.path("fields");
            if (felder.isMissingNode() || !felder.isObject()) {
                return VORBELEGUNG;
            }
            return new Leistungsparameter(
                    betrag(felder, FELD_EINZELBETRAG, VORBELEGUNG.einzelbetrag()),
                    text(felder, FELD_ABRECHNUNGSCODE, VORBELEGUNG.abrechnungscode()),
                    text(felder, FELD_TARIFKENNZEICHEN, VORBELEGUNG.tarifkennzeichen()),
                    text(felder, FELD_POSITIONSNUMMER, VORBELEGUNG.positionsnummer()),
                    betrag(felder, FELD_ZUZAHLUNG, VORBELEGUNG.zuzahlung()),
                    text(felder, FELD_UMSATZSTEUERSATZ, VORBELEGUNG.umsatzsteuersatz()));
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return VORBELEGUNG;
        }
    }

    /** Baut die Parameter aus einer bereits gelesenen Feldbelegung. */
    public static Leistungsparameter ausFeldern(Map<String, String> felder) {
        if (felder == null || felder.isEmpty()) {
            return VORBELEGUNG;
        }
        return new Leistungsparameter(
                leseBetrag(felder.get(FELD_EINZELBETRAG)).orElse(VORBELEGUNG.einzelbetrag()),
                leseText(felder.get(FELD_ABRECHNUNGSCODE)).orElse(VORBELEGUNG.abrechnungscode()),
                leseText(felder.get(FELD_TARIFKENNZEICHEN)).orElse(VORBELEGUNG.tarifkennzeichen()),
                leseText(felder.get(FELD_POSITIONSNUMMER)).orElse(VORBELEGUNG.positionsnummer()),
                leseBetrag(felder.get(FELD_ZUZAHLUNG)).orElse(VORBELEGUNG.zuzahlung()),
                leseText(felder.get(FELD_UMSATZSTEUERSATZ)).orElse(VORBELEGUNG.umsatzsteuersatz()));
    }

    private static String text(JsonNode felder, String name, String vorbelegung) {
        return leseText(felder.path(name).asText(null)).orElse(vorbelegung);
    }

    private static BigDecimal betrag(JsonNode felder, String name, BigDecimal vorbelegung) {
        return leseBetrag(felder.path(name).asText(null)).orElse(vorbelegung);
    }

    private static Optional<String> leseText(String wert) {
        if (wert == null || wert.isBlank() || "null".equals(wert)) {
            return Optional.empty();
        }
        return Optional.of(wert.trim());
    }

    /** Liest einen Betrag, siehe {@link de.gkvtransmitter.util.Betrag}. */
    private static Optional<BigDecimal> leseBetrag(String wert) {
        return de.gkvtransmitter.util.Betrag.lese(wert);
    }

    /** Der Betrag in DTA-Schreibweise, also mit Komma als Trennzeichen. */
    public String einzelbetragFormatiert() {
        return String.format(Locale.GERMAN, "%.2f", einzelbetragGerundet());
    }

    /**
     * Der Einzelbetrag so, wie er in der Nachricht steht: auf zwei Stellen.
     *
     * <p><b>Damit ist zu rechnen, nicht mit dem ungerundeten Wert.</b> Das
     * {@code ENF} traegt den gerundeten Betrag, und die Kasse multipliziert
     * genau den mit der Menge. Rechnet die Fallsumme dagegen mit dem
     * ungerundeten, gehen die beiden auseinander: bei 12,505 und zwei Terminen
     * stuende im ENF 12,51, im BES aber 25,01 statt 25,02. Die eigene
     * Betragskonsistenzregel schlaegt dann an und blockiert den ganzen Lauf -
     * mit einer Meldung, die den Grund nicht nennt.</p>
     *
     * <p>Kaufmaennisch gerundet, wie in der Anzeige und wie
     * {@code String.format} es ohnehin tut.</p>
     */
    public BigDecimal einzelbetragGerundet() {
        return einzelbetrag.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Die Zuzahlung in DTA-Schreibweise. */
    public String zuzahlungFormatiert() {
        return String.format(Locale.GERMAN, "%.2f", zuzahlung);
    }

    /** Das Kompositfeld aus Abrechnungscode und Tarifkennzeichen. */
    public String leistungserbringergruppe() {
        return abrechnungscode + ":" + tarifkennzeichen;
    }
}
