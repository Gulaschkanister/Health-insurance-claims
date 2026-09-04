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
 * der bisherige Wert als Vorbelegung, damit bestehende Blaupausen weiter
 * funktionieren. Die Feldnamen entsprechen denen aus
 * {@code resources/segments/enf.json}, weil die Oberflaeche die Formularwerte
 * unter genau diesen Namen ablegt.</p>
 */
public record Leistungsparameter(
        BigDecimal einzelbetrag,
        String abrechnungscode,
        String tarifkennzeichen,
        String positionsnummer,
        BigDecimal zuzahlung) {

    /** Feldname des Einzelbetrags in enf.json. */
    private static final String FELD_EINZELBETRAG = "Durchschnittlicher Einzelbetrag";
    private static final String FELD_ABRECHNUNGSCODE = "Abrechnungscode";
    private static final String FELD_TARIFKENNZEICHEN = "Tarifkennzeichen";
    private static final String FELD_POSITIONSNUMMER = "Abrechnungspositionsnummer";
    private static final String FELD_ZUZAHLUNG = "Zuzahlung pro Position";

    /**
     * Vorbelegung, falls die Blaupause nichts angibt.
     *
     * <p>Die Werte entsprechen denen, die bisher fest im Quelltext standen, und
     * dem Beispiel in {@code Information/Valide.DTA}. Sie sind bewusst als
     * Rueckfallebene benannt und nicht als fachlich richtige Vorgabe.</p>
     */
    public static final Leistungsparameter VORBELEGUNG = new Leistungsparameter(
            new BigDecimal("15000.00"), "61", "00000", "306050601", BigDecimal.ZERO);

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
                    betrag(felder, FELD_ZUZAHLUNG, VORBELEGUNG.zuzahlung()));
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
                leseBetrag(felder.get(FELD_ZUZAHLUNG)).orElse(VORBELEGUNG.zuzahlung()));
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

    /**
     * Liest einen Betrag sowohl in deutscher als auch in englischer
     * Schreibweise.
     *
     * <p>Die Oberflaeche speichert Formularwerte so, wie sie eingegeben wurden.
     * Je nach Eingabefeld steht dort {@code 15000,00} oder {@code 15000.00}.</p>
     */
    private static Optional<BigDecimal> leseBetrag(String wert) {
        Optional<String> bereinigt = leseText(wert);
        if (bereinigt.isEmpty()) {
            return Optional.empty();
        }
        String normalisiert = bereinigt.get()
                .replace(".", "")
                .replace(',', '.')
                .replace(" ", "");
        try {
            return Optional.of(new BigDecimal(normalisiert));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Der Betrag in DTA-Schreibweise, also mit Komma als Trennzeichen. */
    public String einzelbetragFormatiert() {
        return String.format(Locale.GERMAN, "%.2f", einzelbetrag);
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
