package de.gkvtransmitter.presentation;

import java.util.Locale;

/**
 * Macht aus einer Beschriftung eine Kennung, unter der sich ein Bedienelement
 * finden laesst.
 *
 * <p>Klingt nach Kleinkram, ist aber eine Voraussetzung. Ein
 * {@code lookup("#...")} arbeitet mit CSS-Auswahlausdruecken, und die lesen
 * ein Leerzeichen als Trennung zwischen zwei Bedingungen: die Kennung
 * {@code nav-testdaten anlegen} sucht nach einem Nachfahren namens
 * {@code anlegen} unterhalb von {@code nav-testdaten} und findet nie etwas.
 * Ein Punkt oder Komma im Namen wirkt aehnlich.</p>
 *
 * <p>Betroffen waren beide Stellen, an denen eine Kennung aus einem
 * <em>Text</em> entsteht statt aus einer Konstanten: die Seitenleiste
 * ("Testdaten anlegen") und die Felder des Blaupausenformulars, die ihre
 * Namen aus den Segmentdefinitionen bekommen ("Durchschnittlicher
 * Einzelbetrag"). Die Felder trugen bis zum 05.09.2026 ueberhaupt keine
 * Kennung.</p>
 *
 * <p><b>Was nicht adressierbar ist, laesst sich nicht steuern</b> - und auch
 * nicht pruefen. Aufgefallen ist es, als die Vorschau einen Punkt anklicken
 * wollte; die vorhandenen Tests trafen nur einwortige Bereiche.</p>
 */
final class Kennungen {

    private Kennungen() {
    }

    /**
     * Kleinbuchstaben, und alles, was kein Buchstabe und keine Ziffer ist,
     * wird zum Bindestrich.
     *
     * <p>Umlaute bleiben: JavaFX kommt damit zurecht, und
     * {@code blaupause-feld-durchschnittlicher-einzelbetrag} ist lesbarer als
     * eine Umschrift. Deutsche Buchstaben zaehlen ueber
     * {@code \p{IsAlphabetic}} mit.</p>
     */
    static String aus(String vorsatz, String beschriftung) {
        return vorsatz + beschriftung.toLowerCase(Locale.GERMAN)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
