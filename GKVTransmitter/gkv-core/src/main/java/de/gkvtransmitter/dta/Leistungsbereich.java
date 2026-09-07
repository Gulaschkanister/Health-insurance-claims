package de.gkvtransmitter.dta;

import java.util.Map;

/**
 * Der Leistungserbringer-Sammelgruppenschluessel im UNB.
 *
 * <p>Das sechste Element des UNB traegt nicht, wie ich lange annahm, irgendeine
 * Kennung, sondern nach Anlage 1 Abschnitt 5.4 den <b>Leistungsbereich</b>: den
 * Sammelgruppenschluessel des Leistungserbringers aus Anlage 3, Abschnitt
 * 8.1.14. Er ist ein einzelner Buchstabe und ergibt sich aus dem
 * Abrechnungscode der Leistungszeile.</p>
 *
 * <p><b>Bis zum 07.09.2026 stand dort fest ein {@code H}.</b> H ist der Bereich
 * "Leistungserbringer von Rehabilitationssport" - fuer eine Hebamme falsch.
 * Richtig ist {@code F} (Hebammen, Abrechnungscode 50). Der Wert stammte aus
 * {@code Information/Valide.DTA}, das im selben Zug den Abrechnungscode 61
 * fuehrt; die Referenzdatei war in diesem Punkt in sich stimmig und in der
 * Sache dennoch nicht die einer Hebamme.</p>
 *
 * <p>Der Fehler waere in Pruefstufe 3 aufgefallen - dort, wo die Kasse
 * Schluesselausprägungen gegen das Schluesselverzeichnis haelt. Die Lieferung
 * waere angenommen und zurueckgewiesen worden, nicht bezahlt.</p>
 *
 * <p>Quelle: Anlage 3 zu den Richtlinien nach § 302 SGB V, Version 21/22,
 * Abschnitt 8.1.14.</p>
 */
public final class Leistungsbereich {

    /**
     * Was gilt, wenn sich zu einem Abrechnungscode nichts finden laesst.
     *
     * <p>{@code J} ist der Sammelgruppenschluessel fuer "Weitere Sonstige
     * Leistungserbringer, sofern nicht unter A - I und K - O aufgefuehrt". Das
     * ist der einzige Wert, der bei einem unbekannten Code nicht die Unwahrheit
     * sagt - besser als ein erfundener Buchstabe, der zufaellig zu einem
     * fremden Bereich gehoert.</p>
     */
    public static final String UNBEKANNT = "J";

    /**
     * Abrechnungscode auf Sammelgruppenschluessel.
     *
     * <p>Vollstaendig nach Anlage 3, Abschnitt 8.1.14. Mehrere Codes koennen
     * auf denselben Buchstaben fallen - C und D teilen sich sogar dieselben
     * Codes 31 bis 34, weil sie sich nur im Leistungsbereich unterscheiden;
     * dort gewinnt die haeusliche Krankenpflege, weil sie der haeufigere Fall
     * ist. Wer Haushaltshilfe abrechnet, muss den Bereich setzen und kann sich
     * nicht auf die Ableitung verlassen.</p>
     */
    private static final Map<String, String> ZU_BEREICH = Map.ofEntries(
            Map.entry("11", "A"), Map.entry("12", "A"), Map.entry("13", "A"), Map.entry("14", "A"),
            Map.entry("15", "A"), Map.entry("16", "A"), Map.entry("17", "A"), Map.entry("18", "A"),
            Map.entry("19", "A"),
            Map.entry("21", "B"), Map.entry("22", "B"), Map.entry("23", "B"), Map.entry("24", "B"),
            Map.entry("25", "B"), Map.entry("26", "B"), Map.entry("27", "B"), Map.entry("28", "B"),
            Map.entry("29", "B"), Map.entry("71", "B"), Map.entry("72", "B"), Map.entry("73", "B"),
            Map.entry("74", "B"),
            Map.entry("31", "C"), Map.entry("32", "C"), Map.entry("33", "C"), Map.entry("34", "C"),
            Map.entry("41", "E"), Map.entry("42", "E"), Map.entry("43", "E"), Map.entry("44", "E"),
            Map.entry("45", "E"), Map.entry("46", "E"), Map.entry("47", "E"), Map.entry("48", "E"),
            Map.entry("49", "E"),
            Map.entry("50", "F"),
            Map.entry("55", "G"), Map.entry("56", "G"), Map.entry("57", "G"),
            Map.entry("61", "H"),
            Map.entry("62", "I"),
            Map.entry("65", "J"),
            Map.entry("66", "K"),
            Map.entry("63", "L"), Map.entry("67", "L"),
            Map.entry("68", "M"),
            Map.entry("69", "N"),
            Map.entry("75", "O"),
            Map.entry("76", "P"),
            Map.entry("91", "Q"), Map.entry("92", "Q"), Map.entry("93", "Q"), Map.entry("94", "Q"),
            Map.entry("A1", "R"), Map.entry("A2", "R"), Map.entry("A3", "R"), Map.entry("A4", "R"),
            Map.entry("A5", "R"), Map.entry("A6", "R"), Map.entry("A7", "R"), Map.entry("A8", "R"),
            Map.entry("B1", "S"));

    private Leistungsbereich() {
    }

    /**
     * Der Sammelgruppenschluessel zu einem Abrechnungscode.
     *
     * @param abrechnungscode zweistellig, etwa {@code 50} fuer eine Hebamme
     * @return der Buchstabe fuer das UNB, oder {@link #UNBEKANNT}
     */
    public static String zuAbrechnungscode(String abrechnungscode) {
        if (abrechnungscode == null) {
            return UNBEKANNT;
        }
        return ZU_BEREICH.getOrDefault(abrechnungscode.trim().toUpperCase(java.util.Locale.ROOT), UNBEKANNT);
    }
}
