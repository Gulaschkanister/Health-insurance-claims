package de.gkvtransmitter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Erzeugt das Programmsymbol als PNG in mehreren Groessen und als ICO.
 *
 * <p><b>Kein Test</b>, sondern ein Werkzeug - es steht hier, weil so der
 * Uebersetzer darueber wacht und weil nichts davon ins ausgelieferte Paket
 * gehoert. Von Hand aufzurufen, wenn sich das Symbol aendern soll:</p>
 *
 * <pre>
 * java gkv-ui/src/test/java/de/gkvtransmitter/SymbolErzeugen.java \
 *      gkv-ui/src/main/resources/symbol
 * </pre>
 *
 * <p>Die erzeugten Dateien liegen im Projekt und werden mitversioniert. Der
 * Erzeuger bleibt aufbewahrt, damit sich das Symbol nachvollziehen und aendern
 * laesst, statt eine Bilddatei unbekannter Herkunft im Projekt zu haben - und
 * damit die Farben nachweislich dieselben sind wie in {@code gkv.css}.</p>
 *
 * <p>Was das Symbol prueft, prueft {@code ProgrammsymbolTest}: dass die Dateien
 * vorhanden und lesbar sind und die erwarteten Kantenlaengen haben.</p>
 */
public final class SymbolErzeugen {

    // Dieselben Werte wie in gkv.css: -farbe-akzent und -farbe-akzent-dunkel.
    private static final Color AKZENT = new Color(0x2D, 0x6E, 0x8E);
    private static final Color AKZENT_DUNKEL = new Color(0x24, 0x5A, 0x75);
    private static final Color BLATT = Color.WHITE;

    private static final int[] GROESSEN = {16, 24, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws IOException {
        Path ziel = Path.of(args[0]);
        Files.createDirectories(ziel);

        List<byte[]> pngs = new ArrayList<>();
        for (int groesse : GROESSEN) {
            BufferedImage bild = zeichne(groesse);
            Path datei = ziel.resolve("symbol-" + groesse + ".png");
            ImageIO.write(bild, "png", datei.toFile());
            ByteArrayOutputStream puffer = new ByteArrayOutputStream();
            ImageIO.write(bild, "png", puffer);
            pngs.add(puffer.toByteArray());
            System.out.println("geschrieben: " + datei + " (" + puffer.size() + " Bytes)");
        }

        Path ico = ziel.resolve("symbol.ico");
        Files.write(ico, baueIco(GROESSEN, pngs));
        System.out.println("geschrieben: " + ico + " (" + Files.size(ico) + " Bytes)");
    }

    /**
     * Zeichnet das Symbol: abgerundetes Quadrat in der Akzentfarbe mit einem
     * weissen Haken darauf.
     *
     * <p>Der Haken traegt die Aussage: das Programm prueft, ehe etwas zur Kasse
     * geht. Der erste Entwurf legte den Haken auf ein weisses Blatt mit Zeilen -
     * bei 128 Pixel sah das gut aus, bei 16 verschmolzen Blatt und Haken zu
     * einem grauen Fleck. Ein Symbol muss in der Taskleiste lesbar sein, nicht
     * in der Vergroesserung; deshalb <b>ein</b> Zeichen und sonst nichts.</p>
     *
     * <p>Die Andeutung eines Belegs bleibt: ab 48 Pixel liegen drei Zeilen in
     * gedaempftem Weiss <em>hinter</em> dem Haken. Sie sind schwach genug, um
     * ihn nicht zu stoeren, und tragen bei den grossen Groessen die zweite
     * Aussage - dass hier Rechnungen gestellt werden.</p>
     */
    private static BufferedImage zeichne(int n) {
        BufferedImage bild = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bild.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double eck = n * 0.22;
        g.setPaint(new GradientPaint(0, 0, AKZENT, 0, n, AKZENT_DUNKEL));
        g.fill(new RoundRectangle2D.Double(0, 0, n, n, eck, eck));

        if (n >= 48) {
            g.setColor(new Color(255, 255, 255, 46));
            double dicke = Math.max(1, n * 0.045);
            g.setStroke(new BasicStroke((float) dicke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double[] breiten = {0.46, 0.34, 0.40};
            for (int zeile = 0; zeile < breiten.length; zeile++) {
                double y = n * (0.28 + zeile * 0.155);
                g.draw(new Line2D.Double(n * 0.24, y, n * (0.24 + breiten[zeile]), y));
            }
        }

        Path2D haken = new Path2D.Double();
        haken.moveTo(n * 0.245, n * 0.520);
        haken.lineTo(n * 0.425, n * 0.700);
        haken.lineTo(n * 0.760, n * 0.305);

        g.setColor(BLATT);
        g.setStroke(new BasicStroke((float) Math.max(1.8, n * 0.135),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(haken);

        g.dispose();
        return bild;
    }

    /**
     * Baut eine ICO-Datei aus fertigen PNG.
     *
     * <p>Seit Windows Vista darf ein ICO-Eintrag ein PNG enthalten, statt ein
     * Bitmap mit Maske zu sein - das erspart die AND-Maske und haelt die Datei
     * klein. Die Groesse 256 wird im Verzeichniseintrag als 0 geschrieben; das
     * Feld ist nur ein Byte breit.</p>
     */
    private static byte[] baueIco(int[] groessen, List<byte[]> pngs) throws IOException {
        ByteArrayOutputStream aus = new ByteArrayOutputStream();
        int anzahl = groessen.length;

        schreibeKurz(aus, 0);       // reserviert
        schreibeKurz(aus, 1);       // Typ 1 = Symbol
        schreibeKurz(aus, anzahl);

        int versatz = 6 + 16 * anzahl;
        for (int i = 0; i < anzahl; i++) {
            int groesse = groessen[i];
            aus.write(groesse >= 256 ? 0 : groesse);   // Breite
            aus.write(groesse >= 256 ? 0 : groesse);   // Hoehe
            aus.write(0);                              // Farben in der Palette
            aus.write(0);                              // reserviert
            schreibeKurz(aus, 1);                      // Farbebenen
            schreibeKurz(aus, 32);                     // Bits je Bildpunkt
            schreibeLang(aus, pngs.get(i).length);
            schreibeLang(aus, versatz);
            versatz += pngs.get(i).length;
        }
        for (byte[] png : pngs) {
            aus.write(png);
        }
        return aus.toByteArray();
    }

    private static void schreibeKurz(OutputStream aus, int wert) throws IOException {
        aus.write(wert & 0xFF);
        aus.write((wert >> 8) & 0xFF);
    }

    private static void schreibeLang(OutputStream aus, int wert) throws IOException {
        aus.write(wert & 0xFF);
        aus.write((wert >> 8) & 0xFF);
        aus.write((wert >> 16) & 0xFF);
        aus.write((wert >> 24) & 0xFF);
    }
}
