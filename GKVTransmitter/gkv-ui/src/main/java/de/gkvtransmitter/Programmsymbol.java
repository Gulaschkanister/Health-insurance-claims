package de.gkvtransmitter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.image.Image;

/**
 * Das Symbol der Anwendung, fuer Fenster und Taskleiste.
 *
 * <p>Bis zum 05.09.2026 gab es keines: kein {@code stage.getIcons()}, keine
 * Bilddatei, kein {@code --icon} bei {@code jpackage}. In der Taskleiste stand
 * deshalb das Java-Standardsymbol - bei einem Programm, das eigenstaendig
 * ausgeliefert wird, der deutlichste Hinweis darauf, dass es unfertig ist.</p>
 *
 * <p>Mehrere Groessen, nicht eine: das Betriebssystem waehlt die passende
 * selbst. Waere nur eine hinterlegt, muesste es skalieren, und gerade die
 * kleinen Groessen leiden darunter am meisten - genau die, die man den ganzen
 * Tag in der Taskleiste sieht.</p>
 *
 * <p>Die Dateien sind gezeichnet, nicht gefunden: {@code SymbolErzeugen} unter
 * {@code src/test/java} erzeugt sie. Wer das Symbol aendern will, aendert dort
 * und laesst es neu laufen.</p>
 */
public final class Programmsymbol {

    /** Die vorhandenen Kantenlaengen in Bildpunkten. */
    static final int[] GROESSEN = {16, 24, 32, 48, 64, 128, 256};

    private static final String ORT = "/symbol/symbol-%d.png";

    private Programmsymbol() {
    }

    /**
     * Alle Groessen des Symbols.
     *
     * <p>Fehlt eine Datei, wird sie uebergangen. Ein fehlendes Symbol ist ein
     * Schoenheitsfehler und darf den Start nicht verhindern - anders als bei
     * einem fehlenden Profil steht dahinter keine Fachlichkeit.</p>
     */
    public static List<Image> alle() {
        List<Image> bilder = new ArrayList<>();
        for (int groesse : GROESSEN) {
            Image bild = lade(groesse);
            if (bild != null) {
                bilder.add(bild);
            }
        }
        return bilder;
    }

    /** Eine bestimmte Groesse, oder {@code null}, wenn es sie nicht gibt. */
    static Image lade(int groesse) {
        String pfad = String.format(ORT, groesse);
        try (InputStream quelle = Programmsymbol.class.getResourceAsStream(pfad)) {
            if (quelle == null) {
                return null;
            }
            Image bild = new Image(quelle);
            return bild.isError() ? null : bild;
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
