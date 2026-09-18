package de.gkvtransmitter.validator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Texte der Beanstandungen kommen aus dem Katalog.
 *
 * <p>Sie standen bis zum 18.09.2026 in den Regeln selbst - und damit ein Satz
 * eines Befunds im Quelltext, waehrend seine beiden anderen (Fundstelle und
 * Handlungsanweisung) schon in {@code befunde/befunde.json} standen. <b>Drei
 * Saetze zu einer Sache an zwei Orten:</b> Wer einen umformulierte, fand die
 * anderen nicht.</p>
 *
 * <p>Diese Tests halten fest, dass beim Umzug nichts verlorengegangen ist -
 * die Werte werden weiterhin eingesetzt, und die Saetze lauten wie zuvor.</p>
 */
@DisplayName("Die Texte der Beanstandungen")
class BefundtexteTest {

    private final Befundkatalog katalog = Befundkatalog.standard();

    /**
     * Ein Text ohne eingesetzte Werte ist eine halbe Auskunft.
     *
     * <p>"Das IK hat eine falsche Pruefziffer" sagt nicht, <em>welches</em>.
     * Beim Umzug in eine Formatzeichenkette ist genau das die Gefahr: Die
     * Platzhalter bleiben stehen, weil niemand die Werte mitgibt.</p>
     */
    @Test
    @DisplayName("setzen die Werte ein, statt Platzhalter stehen zu lassen")
    void werteWerdenEingesetzt() {
        String text = ValidationReport.builder()
                .fehler("IK_PRUEFZIFFER", "UNB", "Kassen", "108310401", 0)
                .build().getErrors().get(0).text();

        assertFalse(text.contains("%"), text);
        assertTrue(text.contains("108310401"), text);
        assertTrue(text.contains("Kassen"), text);
    }

    /** Ein Text ohne Werte bleibt, wie er ist. */
    @Test
    @DisplayName("lassen einen Text ohne Werte unveraendert")
    void ohneWerte() {
        assertEquals(katalog.zu("RAHMEN_UNB_FEHLT").orElseThrow().text(),
                ValidationReport.builder().fehler("RAHMEN_UNB_FEHLT", "")
                        .build().getErrors().get(0).text());
    }

    /**
     * Kein Text darf einen unbesetzten Platzhalter behalten.
     *
     * <p>Der Katalog kann das nicht selbst pruefen - wie viele Werte ein Text
     * braucht, weiss nur die Regel. Was er pruefen kann: dass kein Text mit
     * einem Platzhalter <em>beginnt</em> oder nur aus einem besteht. Genau so
     * sahen zwanzig Eintraege nach dem ersten, fehlerhaften Umzug aus.</p>
     */
    @Test
    @DisplayName("bestehen nicht nur aus Platzhaltern")
    void keineReinenPlatzhalter() {
        List<String> verdaechtig = katalog.codes().stream()
                .filter(code -> {
                    String text = katalog.zu(code).orElseThrow().text();
                    return text.isBlank() || text.strip().equals("%s")
                            || text.startsWith("%") || text.startsWith(" ");
                })
                .toList();

        assertTrue(verdaechtig.isEmpty(),
                "Diese Texte sehen nach einem misslungenen Umzug aus: " + verdaechtig);
    }

    /**
     * Umlaute sind hier erlaubt.
     *
     * <p>Java-Kommentare vermeiden sie im ganzen Projekt ({@code Pruefung}),
     * und die Textliterale hatten das mitgeerbt: "Pruefziffer", "gueltig",
     * "Fuer die Kasse". <b>In einer Textdatei gilt die Regel nicht</b>, und
     * der Anwender liest den Text, nicht den Quelltext.</p>
     */
    @Test
    @DisplayName("tragen Umlaute, wo der Quelltext keine hatte")
    void umlaute() {
        assertTrue(katalog.zu("IK_PRUEFZIFFER").orElseThrow().text().contains("Prüfziffer"));
        assertTrue(katalog.zu("ANNAHMESTELLE_UNBEKANNT").orElseThrow().text().contains("Für"));
    }

    /**
     * Ein Code, zwei Bedeutungen - das war einer zu wenig.
     *
     * <p>{@code betrag(...)} meldete unter demselben Code sowohl "fehlt ganz"
     * als auch "ist kein gueltiger Betrag". Ein Code ist eine Kennung; zwei
     * Bedeutungen darunter machen ihn als Kennung wertlos. Beim Umzug fiel das
     * auf, weil ein Code nur einen Text haben kann.</p>
     */
    @Test
    @DisplayName("unterscheiden einen fehlenden von einem unlesbaren Betrag")
    void fehlendUndUnlesbarSindZwei() {
        assertTrue(katalog.zu("BES_BETRAG_FEHLT").orElseThrow().text().contains("fehlt"));
        assertTrue(katalog.zu("BES_BETRAG_UNGUELTIG").orElseThrow().text().contains("kein gültiger"));
    }
}
