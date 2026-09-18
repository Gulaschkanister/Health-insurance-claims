package de.gkvtransmitter.presentation.meldung;

/**
 * Die Zeichen, mit denen Text zusammengesetzt wird.
 *
 * <p><b>Warum sie nicht in {@code ui-messages.json} stehen:</b> Dort steht,
 * <em>was</em> die Anwendung sagt. Ein Mittelpunkt zwischen zwei Angaben sagt
 * nichts - er ist Satzbild, keine Sprache, und wer die Oberflaeche uebersetzte,
 * uebersetzte ihn nicht mit.</p>
 *
 * <p><b>Warum sie trotzdem an einer Stelle stehen:</b> {@code " · "} kam an
 * elf Stellen im Quelltext vor, in drei verschiedenen Schreibweisen - mit
 * einem Leerzeichen, mit zweien, und einmal ganz ohne. Auf dem Bildschirm
 * sieht man das sofort nebeneinander, im Quelltext nie.</p>
 */
public final class Textbau {

    /** Zwischen zwei Angaben derselben Zeile: Datum · Nummer · Groesse. */
    public static final String PUNKT = " · ";

    /** Zwischen Angaben, die weiter auseinanderliegen: Ort  ·  Fundstelle. */
    public static final String WEITER = "  ·  ";

    /** Zwischen Schluessel und Bedeutung: {@code 04 — Korrekturrechnung}. */
    public static final String STRICH = " — ";

    /** Zwischen Beschriftung und Wert: {@code IK der Krankenkasse: 108310400}. */
    public static final String DOPPELPUNKT = ": ";

    /** Zwischen den Gliedern einer Aufzaehlung. */
    public static final String AUFZAEHLUNG = ", ";

    /** Vor einer Beanstandung, die den Versand aufgehalten hat. */
    public static final String MARKE_FEHLER = "• ";

    /** Vor einem Hinweis, der nichts aufgehalten hat. */
    public static final String MARKE_HINWEIS = "– ";

    /** Vor der Handlungsanweisung unter einer Beanstandung. */
    public static final String MARKE_HANDLUNG = "→ ";

    /** Vor einem Glied einer aufgezaehlten Liste in einer Meldung. */
    public static final String MARKE_LISTE = "· ";

    private Textbau() {
    }
}
