package de.gkvtransmitter.util;

/**
 * Die Texte, die der Kern ausgibt.
 *
 * <p>Der Kern gibt Text aus, auch ohne Oberflaeche: den Wartungskalender, das
 * Registrierungsblatt. <b>Bis zum 18.09.2026 trug er seine deutschen Woerter
 * im Quelltext</b>, weil {@code AppMessages} im Oberflaechenmodul lag und der
 * Kern kein JavaFX sehen darf. Die Klasse hatte mit JavaFX nie etwas zu tun;
 * sie ist umgezogen, und der Kern hat jetzt seine eigene Sammlung.</p>
 *
 * <p><b>Warum eine eigene Datei und nicht {@code ui-messages.json}:</b> Die
 * beiden beschreiben Verschiedenes. Hier steht, was die Fachlichkeit sagt -
 * „Zertifikat erneuern", „jährlich, Antrag 1–2 Wochen vorher". Dort steht, wie
 * die Oberflaeche es nennt. Eine gemeinsame Datei haette den Kern von der
 * Oberflaeche abhaengig gemacht, und genau das verhindert die
 * Enforcer-Regel.</p>
 *
 * <p>Die Beanstandungen haben ihre dritte eigene Datei,
 * {@code befunde/befunde.json} - dort steht neben dem Text auch die Fundstelle
 * und die Handlungsanweisung.</p>
 */
public final class Kerntexte {

    private static final String DATEI = "/messages/kern-texte.json";

    /** Einmal gelesen; die Texte aendern sich zur Laufzeit nicht. */
    private static final AppMessages TEXTE = new AppMessages(DATEI);

    private Kerntexte() {
    }

    /** Der Text zu einem Schluessel, oder der Schluessel selbst. */
    public static String get(String schluessel) {
        return TEXTE.get(schluessel);
    }

    /** Ein Text mit eingesetzten Werten. */
    public static String format(String schluessel, Object... werte) {
        return String.format(TEXTE.get(schluessel), werte);
    }
}
