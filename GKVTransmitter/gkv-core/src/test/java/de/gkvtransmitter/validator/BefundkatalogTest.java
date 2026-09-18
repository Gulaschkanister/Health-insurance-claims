package de.gkvtransmitter.validator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Haelt Quelltext und Befundkatalog zusammen.
 *
 * <p><b>Eine Beanstandung, die nur sagt, dass etwas nicht stimmt, ist die
 * halbe Auskunft.</b> Die Aufgabe A2 verlangt zu jedem Code eine Fundstelle
 * und eine Handlungsanweisung - und verlangt ausdruecklich, dass ein Test das
 * durchsetzt: "kein Fehlercode ohne Fundstelle existiert, durchgesetzt von
 * einem Test, der ueber alle Regeln laeuft".</p>
 *
 * <p>Der Test liest den <b>Quelltext</b> und nicht eine Liste im Quelltext.
 * Eine Liste, die von Hand gepflegt wird, geht auseinander - und zwar in der
 * Richtung, die niemandem auffaellt: Ein neuer Code wird ergaenzt, die Liste
 * nicht. Deshalb ist die Quelle der Wahrheit das, was tatsaechlich dasteht.</p>
 */
@DisplayName("Der Befundkatalog")
class BefundkatalogTest {

    /**
     * Wo Befunde entstehen. Relativ zum Modulverzeichnis, wie Maven es setzt.
     *
     * <p>Nur diese beiden Pakete: Das Muster erkennt Codes an ihrer Form, und
     * anderswo tragen auch Umgebungsvariablen ({@code GKV_HOME}) und
     * Modifizierernamen ({@code MAX_LENGTH}) dieselbe. Wer einen Befund in
     * einem dritten Paket meldet, traegt es hier nach - und merkt es daran,
     * dass dieser Test dann nichts sagt, wo er etwas sagen sollte.</p>
     */
    private static final List<Path> QUELLTEXT = List.of(
            Path.of("src", "main", "java", "de", "gkvtransmitter", "validator"),
            Path.of("src", "main", "java", "de", "gkvtransmitter", "dispatch"));

    /**
     * Was wie ein Pruefcode aussieht.
     *
     * <p><b>Nicht {@code .error("CODE"}.</b> Diese engere Fassung stand hier
     * zuerst und uebersah zwei Befunde: {@code LeistungspositionRegel} reicht
     * ihre Codes an eine Hilfsmethode weiter
     * ({@code pruefeWert(enf, ENF_BETRAG, bericht, "ENF_BETRAG_NULL", ...)}),
     * und an der Aufrufstelle steht dann kein {@code .error} davor. <b>Ein
     * Test, der nur die eine Schreibweise kennt, bestaetigt eine
     * Vollstaendigkeit, die es nicht gibt.</b></p>
     *
     * <p>Gesucht wird deshalb nach der Form des Codes selbst: Grossbuchstaben
     * und Ziffern mit mindestens einem Unterstrich. Segmentbezeichner wie
     * {@code "ENF"} oder {@code "UNB"} fallen dadurch heraus - sie haben
     * keinen.</p>
     */
    private static final Pattern BEFUND =
            Pattern.compile("\"([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+)\"");

    private final Befundkatalog katalog = Befundkatalog.standard();

    /** Jeder Code, den irgendeine Regel meldet. */
    private static Set<String> codesImQuelltext() throws IOException {
        Set<String> codes = new TreeSet<>();
        for (Path wurzel : QUELLTEXT) {
            try (Stream<Path> dateien = Files.walk(wurzel)) {
                for (Path datei : dateien.filter(pfad -> pfad.toString().endsWith(".java")).toList()) {
                    Matcher treffer = BEFUND.matcher(Files.readString(datei, StandardCharsets.UTF_8));
                    while (treffer.find()) {
                        codes.add(treffer.group(1));
                    }
                }
            }
        }
        return codes;
    }

    /**
     * Der Test, der die Luecke unmoeglich macht.
     *
     * <p>Wer eine Regel um einen Befund erweitert und den Katalog vergisst,
     * bekommt es hier gesagt - und zwar mit dem Namen des Codes.</p>
     */
    @Test
    @DisplayName("kennt jeden Code, den eine Regel meldet")
    void kenntJedenCode() throws IOException {
        Set<String> ohneEintrag = new TreeSet<>(codesImQuelltext());
        ohneEintrag.removeAll(katalog.codes());

        assertTrue(ohneEintrag.isEmpty(),
                "Diese Befunde haben keine Fundstelle in befunde/befunde.json: " + ohneEintrag);
    }

    /**
     * Und umgekehrt.
     *
     * <p>Ein Eintrag ohne Code ist ein Rest: Die Regel wurde umbenannt oder
     * entfernt, die Fundstelle blieb stehen. Sie sieht dann gepflegt aus und
     * gehoert zu nichts.</p>
     */
    @Test
    @DisplayName("fuehrt keinen Eintrag, den keine Regel meldet")
    void keineVerwaistenEintraege() throws IOException {
        Set<String> ohneRegel = new TreeSet<>(katalog.codes());
        ohneRegel.removeAll(codesImQuelltext());

        assertTrue(ohneRegel.isEmpty(),
                "Zu diesen Eintraegen gibt es keine Regel mehr: " + ohneRegel);
    }

    @Test
    @DisplayName("nennt zu jedem Code eine Fundstelle und eine Handlungsanweisung")
    void jederEintragIstAusgefuellt() {
        List<String> unvollstaendig = katalog.codes().stream()
                .filter(code -> katalog.zu(code)
                        .filter(eintrag -> !eintrag.fundstelle().isBlank()
                                && !eintrag.handlung().isBlank())
                        .isEmpty())
                .toList();

        assertTrue(unvollstaendig.isEmpty(),
                "Diese Eintraege sind unvollstaendig: " + unvollstaendig);
    }

    /**
     * Was keine Fundstelle hat, sagt das.
     *
     * <p>Das Pruefziffernverfahren der Institutionskennzeichen etwa ist in den
     * vorliegenden Unterlagen nicht beschrieben. Ein erfundener Abschnitt waere
     * schlimmer als das Eingestaendnis - <b>was keine Fundstelle hat, ist eine
     * Vermutung.</b></p>
     */
    @Test
    @DisplayName("kennzeichnet eigene Hinweise als solche")
    void eigeneHinweiseSindGekennzeichnet() {
        assertFalse(katalog.zu("IK_PRUEFZIFFER").orElseThrow().ausAnlage(),
                "Das Pruefziffernverfahren steht in keiner der vorliegenden Unterlagen");
        assertTrue(katalog.zu("POSITION_HEBAMME_LAENGE").orElseThrow().ausAnlage(),
                "Anlage 3, Abschnitt 8.2.6 liegt vor");
    }

    /** Die Fundstelle haengt am Befund und wandert mit ihm. */
    @Test
    @DisplayName("haengt an der einzelnen Beanstandung")
    void amBefund() {
        ValidationMessage befund = ValidationMessage.error("IK_PRUEFZIFFER", "UNB", "irgendwas");

        assertFalse(befund.fundstelle().isBlank());
        assertFalse(befund.handlung().isBlank());
    }
}
