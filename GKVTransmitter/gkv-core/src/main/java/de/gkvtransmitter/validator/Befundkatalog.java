package de.gkvtransmitter.validator;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Zu jedem Pruefcode die Fundstelle und eine Handlungsanweisung.
 *
 * <p><b>Eine Beanstandung, die nur sagt, <em>dass</em> etwas nicht stimmt, ist
 * die halbe Auskunft.</b> "Die Abrechnungspositionsnummer hat 9 Stellen" laesst
 * offen, wo das steht und was zu tun ist. Beides gehoert dazu - und beides
 * steht in den Unterlagen unter {@code Information/}, es ist Nachschlagen und
 * keine Beschaffung.</p>
 *
 * <p>Der Katalog liegt in {@code befunde/befunde.json} und nicht im Quelltext:
 * Wer eine Fundstelle berichtigt, aendert eine Zeile und uebersetzt nichts
 * neu.</p>
 *
 * <p><b>Was keine Fundstelle hat, sagt das.</b> Fuenf Befunde stammen von
 * dieser Anwendung und nicht aus einer Anlage - das Pruefziffernverfahren der
 * IK etwa ist in den vorliegenden Unterlagen nicht beschrieben. Sie tragen
 * {@code quelle: anwendung}, statt eine Fundstelle zu erfinden.</p>
 */
public final class Befundkatalog {

    private static final String DATEI = "/befunde/befunde.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Die einmal gelesene Fassung; der Katalog aendert sich zur Laufzeit nicht. */
    private static final Befundkatalog STANDARD = lade(DATEI);

    /**
     * Was zu einem Code zu wissen ist.
     *
     * @param text        die Beanstandung als Formatzeichenkette
     * @param fundstelle  Anlage und Abschnitt, oder der Hinweis, dass es keine gibt
     * @param handlung    was zu tun ist, in ganzen Saetzen
     * @param ausAnlage   ob die Angabe aus den Unterlagen stammt
     * @param pruefstufe  in welcher Stufe die Kasse das faende, oder leer
     */
    public record Eintrag(String text, String fundstelle, String handlung, boolean ausAnlage,
            String pruefstufe) {

        /** Der Text mit eingesetzten Werten. */
        public String text(Object... werte) {
            return werte.length == 0 ? text : String.format(text, werte);
        }
    }

    private final Map<String, Eintrag> eintraege;

    private Befundkatalog(Map<String, Eintrag> eintraege) {
        this.eintraege = Map.copyOf(eintraege);
    }

    /** Der mitgelieferte Katalog. */
    public static Befundkatalog standard() {
        return STANDARD;
    }

    /**
     * Liest einen Katalog aus dem Klassenpfad.
     *
     * <p>Eine fehlende oder unlesbare Datei fuehrt nicht zum Abbruch: Die
     * Fundstelle ist eine Zugabe zur Beanstandung, keine Voraussetzung. Waere
     * sie es, koennte eine kaputte JSON-Datei die Pruefung anhalten - und
     * damit die Abrechnung.</p>
     */
    public static Befundkatalog lade(String pfad) {
        Map<String, Eintrag> gelesen = new LinkedHashMap<>();
        try (InputStream quelle = Befundkatalog.class.getResourceAsStream(pfad)) {
            if (quelle == null) {
                return new Befundkatalog(gelesen);
            }
            JsonNode wurzel = MAPPER.readTree(quelle).path("befunde");
            wurzel.fields().forEachRemaining(eintrag -> gelesen.put(eintrag.getKey(),
                    new Eintrag(text(eintrag.getValue(), "text"),
                            text(eintrag.getValue(), "fundstelle"),
                            text(eintrag.getValue(), "handlung"),
                            "anlage".equals(text(eintrag.getValue(), "quelle")),
                            text(eintrag.getValue(), "pruefstufe"))));
        } catch (Exception unlesbar) {
            return new Befundkatalog(Map.of());
        }
        return new Befundkatalog(gelesen);
    }

    /**
     * Der Beanstandungstext zu einem Code, mit eingesetzten Werten.
     *
     * <p>Fehlt der Eintrag, kommt der Code selbst zurueck - wie bei
     * {@code AppMessages}. Das ist im Entwickeln nuetzlich und faellt auf;
     * {@code BefundkatalogTest} laesst es gar nicht erst so weit kommen.</p>
     */
    public String text(String code, Object... werte) {
        return zu(code).map(eintrag -> eintrag.text(werte)).orElse(code);
    }

    /** Der Eintrag zu einem Code, oder leer. */
    public Optional<Eintrag> zu(String code) {
        return Optional.ofNullable(eintraege.get(code));
    }

    /** Alle Codes, die der Katalog kennt. */
    public java.util.Set<String> codes() {
        return eintraege.keySet();
    }

    private static String text(JsonNode knoten, String name) {
        JsonNode wert = knoten.path(name);
        return wert.isMissingNode() || wert.isNull() ? "" : wert.asText().trim();
    }
}
