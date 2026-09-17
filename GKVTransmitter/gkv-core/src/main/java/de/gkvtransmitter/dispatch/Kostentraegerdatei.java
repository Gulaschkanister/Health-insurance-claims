package de.gkvtransmitter.dispatch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;

/**
 * Die Kostentraegerdatei: welche Stelle nimmt fuer welche Kasse entgegen.
 *
 * <p><b>Warum es diese Klasse gibt.</b> Die Zuordnung Kasse zu
 * Datenannahmestelle steht in einer oeffentlichen, vierteljaehrlich erneuerten
 * Datei. Im Projekt wurde sie bis hierher in
 * {@code billing-office-endpoints.json} von Hand gepflegt - mit
 * dreiundzwanzig einzelnen <em>Kassen</em> und damit dem <b>falschen
 * Empfaenger</b>: Zugestellt wird an die Annahmestelle, nicht an die Kasse.</p>
 *
 * <p>Format und Schluessel stehen in
 * {@code Information/Anhang_3_Kostentraegerdatei_V10_20260414.pdf}. Die Datei
 * benutzt dieselben Trennzeichen wie eine DTA-Nachricht - {@code +} zwischen
 * Datenelementen, {@code :} innerhalb, {@code '} am Segmentende - und laesst
 * sich deshalb mit {@link DtaDocument} einlesen, ohne dass es dafuer einen
 * zweiten Leser braucht.</p>
 *
 * <h2>Der Weg von der Versichertenkarte zur Annahmestelle</h2>
 *
 * <pre>
 * IDK  IK der Versichertenkarte
 *  |
 *  +-- VKG 01 --&gt; Kostentraeger              (Anhang 3, Abschnitt 8.3)
 *                  |
 *                  +-- VKG 03 --&gt; Datenannahmestelle MIT Entschluesselungsbefugnis
 *                  |               (VKG 02 waere eine OHNE - an die darf nichts gehen)
 *                  |
 *                  +-- IDK der Annahmestelle
 *                       +-- DFU --&gt; Protokoll und Kanal (Abschnitt 8.5)
 * </pre>
 *
 * <p>Der Verweis 01 kann fehlen: Traegt die Karte schon das IK des
 * Kostentraegers, steht der Verweis auf die Annahmestelle unmittelbar an
 * diesem IDK. Beide Wege werden versucht.</p>
 */
public final class Kostentraegerdatei {

    private static final String IDK = "IDK";
    private static final String VKG = "VKG";
    private static final String DFU = "DFU";

    /** Verweis vom IK der Versichertenkarte zum Kostentraeger. */
    private static final String VERWEIS_KOSTENTRAEGER = "01";
    /** Verweis auf eine Datenannahmestelle <b>mit</b> Entschluesselungsbefugnis. */
    private static final String VERWEIS_ANNAHMESTELLE = "03";

    /**
     * Sammelschluessel: gilt fuer alle Leistungen des Teilprojekts 5.
     *
     * <p>Fussnote 4 zum VKG-Segment: Der Sammelschluessel schliesst die
     * Verwendung von Gruppen- und Einzelschluesseln aus - wo er steht, gilt er
     * fuer alles.</p>
     */
    private static final String SAMMELSCHLUESSEL = "00";

    // --- Feldpositionen, 0-basiert ohne den Bezeichner -------------------
    /** IDK: das Institutionskennzeichen. */
    private static final int IDK_IK = 0;
    /** IDK: die Kurzbezeichnung. */
    private static final int IDK_NAME = 2;
    /** VKG: die Art der Verknuepfung. */
    private static final int VKG_ART = 0;
    /** VKG: das IK des Verknuepfungspartners. */
    private static final int VKG_PARTNER = 1;
    /** VKG: der Abrechnungscode, fuer Teilprojekt 5 zwingend. */
    private static final int VKG_ABRECHNUNGSCODE = 8;
    /** DFU: der Schluessel des Uebertragungsprotokolls. */
    private static final int DFU_PROTOKOLL = 1;
    /** DFU: der Kommunikationskanal, etwa eine E-Mail-Anschrift. */
    private static final int DFU_KANAL = 6;

    /** Je IK der Block seiner Segmente, in der Reihenfolge der Datei. */
    private final Map<Integer, List<DtaSegment>> bloecke;

    private Kostentraegerdatei(Map<Integer, List<DtaSegment>> bloecke) {
        this.bloecke = bloecke;
    }

    /**
     * Liest eine Kostentraegerdatei ein.
     *
     * <p>Ein Block beginnt mit einem {@code IDK} und reicht bis zum naechsten.
     * Segmente vor dem ersten {@code IDK} - der Nachrichtenrahmen - gehoeren zu
     * keinem Block und werden uebergangen.</p>
     */
    public static Kostentraegerdatei lies(String inhalt) {
        Objects.requireNonNull(inhalt, "inhalt must not be null");

        Map<Integer, List<DtaSegment>> bloecke = new LinkedHashMap<>();
        List<DtaSegment> aktuell = null;
        for (DtaSegment segment : DtaDocument.parse(inhalt).getSegments()) {
            if (IDK.equals(segment.tag())) {
                // Ein IDK ohne lesbares IK laesst sich nicht zuordnen. Der
                // Block danach wird uebergangen, statt ihn an den vorigen zu
                // haengen - dort gehoerte er sicher nicht hin.
                aktuell = zahlAus(segment.element(IDK_IK))
                        .map(ik -> bloecke.computeIfAbsent(ik, unbenutzt -> new ArrayList<>()))
                        .orElse(null);
            }
            if (aktuell != null) {
                aktuell.add(segment);
            }
        }
        return new Kostentraegerdatei(bloecke);
    }

    /** Ob die Datei ueberhaupt etwas enthaelt. */
    public boolean istLeer() {
        return bloecke.isEmpty();
    }

    /**
     * Die Annahmestelle zu einer Kasse und einem Abrechnungscode.
     *
     * @param kassenIk         das IK von der Versichertenkarte
     * @param abrechnungscode  zweistellig, etwa {@code 50} fuer eine Hebamme
     * @return die Annahmestelle, oder leer, wenn die Datei fuer diese
     *         Kombination keine fuehrt - <b>dann ist nichts zu erfinden</b>
     */
    public Optional<Annahmestelle> annahmestelleFuer(int kassenIk, String abrechnungscode) {
        List<DtaSegment> karte = bloecke.get(kassenIk);
        if (karte == null) {
            return Optional.empty();
        }

        // Erst am IK der Karte selbst nachsehen, dann hinter dem Verweis 01:
        // Traegt die Karte bereits das IK des Kostentraegers, gibt es keinen.
        Optional<Annahmestelle> unmittelbar = annahmestelleImBlock(karte, abrechnungscode);
        if (unmittelbar.isPresent()) {
            return unmittelbar;
        }
        return partner(karte, VERWEIS_KOSTENTRAEGER)
                .map(bloecke::get)
                .flatMap(kostentraeger -> annahmestelleImBlock(kostentraeger, abrechnungscode));
    }

    /** Der Verweis 03 eines Blocks, passend zum Abrechnungscode. */
    private Optional<Annahmestelle> annahmestelleImBlock(List<DtaSegment> block, String abrechnungscode) {
        if (block == null) {
            return Optional.empty();
        }
        return block.stream()
                .filter(segment -> VKG.equals(segment.tag()))
                .filter(segment -> VERWEIS_ANNAHMESTELLE.equals(segment.element(VKG_ART).trim()))
                .filter(segment -> passt(segment.element(VKG_ABRECHNUNGSCODE).trim(), abrechnungscode))
                .findFirst()
                .flatMap(segment -> zahlAus(segment.element(VKG_PARTNER)))
                .map(this::beschreibe);
    }

    /**
     * Ob ein Eintrag der Datei fuer den gesuchten Abrechnungscode gilt.
     *
     * <p>Drei Faelle nach Fussnote 4 zum VKG-Segment: der Code selbst, der
     * <b>Gruppenschluessel</b> (die Zehnerstelle, {@code 47} liegt unter
     * {@code 40}) und der <b>Sammelschluessel</b> {@code 00}.</p>
     *
     * <p>Der Sonderschluessel {@code 99} zaehlt <b>nicht</b> dazu: Er gilt
     * ausdruecklich fuer <em>nicht aufgefuehrte</em> Gruppen, und ihn auf einen
     * aufgefuehrten Code anzuwenden hiesse, an eine Stelle zu liefern, die
     * dafuer nicht benannt ist.</p>
     *
     * <p>Ein <b>leeres</b> Feld gilt ebenfalls nicht. Fussnote 4: „Der
     * Abrechnungscode ist zwingend fuer das Teilprojekt 5 anzugeben." Wer ein
     * leeres Feld als „gilt fuer alles" liest, macht aus einer lueckenhaften
     * Datei stillschweigend einen Empfaenger - <b>und genau das hat beim
     * Entwickeln einen Fehler in den Pruefdaten verdeckt</b>, weil ein um eine
     * Stelle verrutschter Code dort ankam, wo nichts stand.</p>
     */
    private static boolean passt(String eintrag, String gesucht) {
        if (eintrag.isEmpty()) {
            return false;
        }
        if (SAMMELSCHLUESSEL.equals(eintrag)) {
            return true;
        }
        String code = gesucht == null ? "" : gesucht.trim();
        return eintrag.equals(code)
                || (code.length() == 2 && eintrag.equals(code.charAt(0) + "0"));
    }

    /** Name und Uebertragungsweg zu einem IK. */
    private Annahmestelle beschreibe(int ik) {
        List<DtaSegment> block = bloecke.get(ik);
        if (block == null) {
            // Die Datei verweist auf ein IK, das sie selbst nicht fuehrt. Das
            // ist ein Mangel der Datei; hier bleibt der Verweis trotzdem
            // brauchbar - nur ohne Namen und ohne Weg.
            return new Annahmestelle(ik, "", "", "");
        }
        String name = block.stream()
                .filter(segment -> IDK.equals(segment.tag()))
                .findFirst()
                .map(segment -> segment.element(IDK_NAME).trim())
                .orElse("");
        return block.stream()
                .filter(segment -> DFU.equals(segment.tag()))
                .findFirst()
                .map(segment -> new Annahmestelle(ik, name,
                        segment.element(DFU_PROTOKOLL).trim(), segment.element(DFU_KANAL).trim()))
                .orElseGet(() -> new Annahmestelle(ik, name, "", ""));
    }

    /** Das IK hinter einem Verweis der angegebenen Art. */
    private static Optional<Integer> partner(List<DtaSegment> block, String art) {
        return block.stream()
                .filter(segment -> VKG.equals(segment.tag()))
                .filter(segment -> art.equals(segment.element(VKG_ART).trim()))
                .findFirst()
                .flatMap(segment -> zahlAus(segment.element(VKG_PARTNER)));
    }

    private static Optional<Integer> zahlAus(String text) {
        String ziffern = text == null ? "" : text.trim();
        if (ziffern.isEmpty() || !ziffern.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(ziffern));
        } catch (NumberFormatException zuGross) {
            return Optional.empty();
        }
    }
}
