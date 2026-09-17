package de.gkvtransmitter.presentation;

import java.util.LinkedHashMap;
import java.util.Map;

import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;

/**
 * Welche Felder ein Personenformular zeigt.
 *
 * <p>Teilnehmer und Dienstleister teilen sich Anschrift und Kennzeichen; sie
 * stehen in {@link #PERSON}. Was nur die eine Rolle betrifft, steht in einer
 * zweiten Datei - der Dienstleister fuehrt einen Abrechnungscode, die
 * Teilnehmerin nicht.</p>
 *
 * <p><b>Warum zwei Dateien und nicht eine mit einer Rollenspalte:</b> Die
 * gemeinsamen Felder sollen genau einmal beschrieben sein. Eine zweite
 * vollstaendige Datei fuer den Dienstleister waere beim ersten Aendern an der
 * Anschrift auseinandergelaufen, und zwar still.</p>
 *
 * <p>Die Reihenfolge bleibt die der Dateien, erst die gemeinsamen Felder, dann
 * die zusaetzlichen - siehe {@code TagConfigLoader}, wo eine {@code HashMap}
 * das Formular schon einmal durcheinandergebracht hat.</p>
 */
public final class Personenfelder {

    /** Die Felder, die jede Person hat. */
    public static final String PERSON = "/tags/person-tags.json";

    /** Die Felder, die nur ein Dienstleister hat. */
    public static final String DIENSTLEISTER = "/tags/dienstleister-tags.json";

    private Personenfelder() {
    }

    /**
     * Die Felder eines Formulars.
     *
     * @param zusatzdatei weitere Felder hinter den gemeinsamen, oder
     *        {@code null} fuer ein reines Personenformular
     */
    public static Map<String, TagList> mit(String zusatzdatei) {
        Map<String, TagList> felder = new LinkedHashMap<>(TagConfigLoader.loadTagConfig(PERSON));
        if (zusatzdatei != null && !zusatzdatei.isBlank()) {
            felder.putAll(TagConfigLoader.loadTagConfig(zusatzdatei));
        }
        return felder;
    }
}
