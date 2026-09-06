package de.gkvtransmitter.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Legt fest, wo die Anwendung ihre Daten ablegt.
 *
 * <p>Zuvor lagen Datenbank und Versandordner unter relativen Pfaden
 * ({@code database.db}, {@code dta_output}) und damit im jeweils aktuellen
 * Arbeitsverzeichnis. Solange die Anwendung aus der Entwicklungsumgebung
 * gestartet wurde, war das der Projektordner. Bei einer ausgelieferten
 * Anwendung ist es das Verzeichnis, aus dem der Anwender sie gerade startet -
 * mal der Desktop, mal {@code C:\Program Files}, wo sich ueberhaupt nicht
 * schreiben laesst. Die Daten waeren also je nach Startart woanders gelandet
 * oder gar nicht erst angelegt worden.</p>
 *
 * <p>Der Ablageort ist jetzt fest an den Benutzer gebunden und unabhaengig
 * davon, von wo die Anwendung startet:</p>
 *
 * <ul>
 *   <li>Windows: {@code %LOCALAPPDATA%\GKV-Abrechnung}</li>
 *   <li>macOS: {@code ~/Library/Application Support/GKV-Abrechnung}</li>
 *   <li>sonst: {@code $XDG_DATA_HOME/gkv-abrechnung}, ersatzweise
 *       {@code ~/.local/share/gkv-abrechnung}</li>
 * </ul>
 *
 * <p>Ueberschreibbar ueber die Systemeigenschaft {@code gkv.home} oder die
 * Umgebungsvariable {@code GKV_HOME} - etwa um mehrere Staende nebeneinander
 * zu betreiben oder die Daten auf ein anderes Laufwerk zu legen.</p>
 *
 * <h2>Der Umzug vom alten Namen</h2>
 *
 * <p>Bis zum 06.09.2026 hiess der Ordner {@code GKVTransmitter}. Eine
 * Umbenennung ohne Umzug haette die bestehende Datenbank am alten Ort liegen
 * lassen, und die Anwendung waere mit leeren Listen aufgegangen - fuer jemanden,
 * der ein Jahr Stammdaten darin hat, ist das nicht von Datenverlust zu
 * unterscheiden. Der alte Ordner wird deshalb beim ersten Start umbenannt,
 * siehe {@link #umgezogen}.</p>
 */
public final class Anwendungsverzeichnis {

    /** Systemeigenschaft fuer das Datenverzeichnis. */
    public static final String BASIS_PROPERTY = "gkv.home";
    /** Umgebungsvariable fuer das Datenverzeichnis. */
    public static final String BASIS_ENV = "GKV_HOME";

    private static final String ORDNERNAME_WINDOWS = "GKV-Abrechnung";
    private static final String ORDNERNAME_UNIX = "gkv-abrechnung";

    /** Wie der Ordner bis zum 06.09.2026 hiess, siehe {@link #umgezogen}. */
    private static final String ALTNAME_WINDOWS = "GKVTransmitter";
    private static final String ALTNAME_UNIX = "gkvtransmitter";

    private static final String DATENBANK_DATEI = "database.db";
    private static final String VERSAND_ORDNER = "dta_output";

    private Anwendungsverzeichnis() {
    }

    /** Das Verzeichnis, in dem alle Daten der Anwendung liegen. */
    public static Path basis() {
        String vorgabe = System.getProperty(BASIS_PROPERTY);
        if (vorgabe == null || vorgabe.isBlank()) {
            vorgabe = System.getenv(BASIS_ENV);
        }
        if (vorgabe != null && !vorgabe.isBlank()) {
            return Paths.get(vorgabe.trim()).toAbsolutePath();
        }
        return standardBasis();
    }

    private static Path standardBasis() {
        String betriebssystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String heim = System.getProperty("user.home", ".");

        if (betriebssystem.contains("win")) {
            // LOCALAPPDATA ist der vorgesehene Ort fuer Anwendungsdaten, die
            // nicht ins servergespeicherte Profil gehoeren. Eine Datenbank
            // gehoert dort nicht hinein, weil sie bei jeder Anmeldung
            // synchronisiert wuerde.
            String lokal = System.getenv("LOCALAPPDATA");
            Path wurzel = (lokal != null && !lokal.isBlank())
                    ? Paths.get(lokal)
                    : Paths.get(heim, "AppData", "Local");
            return umgezogen(wurzel.resolve(ORDNERNAME_WINDOWS).toAbsolutePath(),
                    wurzel.resolve(ALTNAME_WINDOWS).toAbsolutePath());
        }

        if (betriebssystem.contains("mac")) {
            Path wurzel = Paths.get(heim, "Library", "Application Support");
            return umgezogen(wurzel.resolve(ORDNERNAME_WINDOWS).toAbsolutePath(),
                    wurzel.resolve(ALTNAME_WINDOWS).toAbsolutePath());
        }

        String xdg = System.getenv("XDG_DATA_HOME");
        Path wurzel = (xdg != null && !xdg.isBlank())
                ? Paths.get(xdg)
                : Paths.get(heim, ".local", "share");
        return umgezogen(wurzel.resolve(ORDNERNAME_UNIX).toAbsolutePath(),
                wurzel.resolve(ALTNAME_UNIX).toAbsolutePath());
    }

    /**
     * Benennt den Ordner aus der Zeit vor der Umbenennung um.
     *
     * <p>Nur dann, wenn es den neuen noch nicht gibt und den alten schon: sonst
     * wuerde ein zweiter Stand einen bestehenden ueberschreiben.</p>
     *
     * <p><b>Scheitert der Umzug, wird weiter am alten Ort gearbeitet.</b> Das
     * ist die entscheidende Eigenschaft dieser Methode. Die Datei kann gesperrt
     * sein, weil noch eine zweite Programmfassung laeuft, oder die
     * Berechtigungen koennen fehlen - in beiden Faellen waere es das Schlimmste,
     * mit einem leeren neuen Ordner aufzugehen: fuer jemanden mit einem Jahr
     * Stammdaten ist das von Datenverlust nicht zu unterscheiden, und er wuerde
     * anfangen, alles noch einmal einzugeben.</p>
     *
     * <p>Paketsichtbar, damit sich genau das pruefen laesst.</p>
     *
     * @param neu wohin die Daten gehoeren
     * @param alt wo sie bis zum 06.09.2026 lagen
     * @return der Ordner, mit dem die Anwendung arbeiten soll
     */
    static Path umgezogen(Path neu, Path alt) {
        if (java.nio.file.Files.exists(neu) || !java.nio.file.Files.isDirectory(alt)) {
            return neu;
        }
        try {
            java.nio.file.Files.move(alt, neu);
            return neu;
        } catch (java.io.IOException | RuntimeException e) {
            System.err.println("Der Datenordner liess sich nicht umbenennen (" + e.getMessage()
                    + "). Es wird weiter mit " + alt + " gearbeitet.");
            return alt;
        }
    }

    /** Ablageort der SQLite-Datenbank. */
    public static Path datenbank() {
        return basis().resolve(DATENBANK_DATEI);
    }

    /** Ablageort der erzeugten und versendeten DTA-Dateien. */
    public static Path versandordner() {
        return basis().resolve(VERSAND_ORDNER);
    }

    /**
     * Loest einen konfigurierten Pfad auf.
     *
     * <p>Ein absoluter Pfad bleibt unveraendert. Ein relativer wird gegen das
     * Datenverzeichnis aufgeloest und nicht gegen das Arbeitsverzeichnis - die
     * Zielordner aus {@code billing-office-endpoints.json} sind relativ notiert
     * und laegen sonst je nach Startart woanders.</p>
     */
    public static Path aufloesen(Path pfad) {
        if (pfad == null) {
            return basis();
        }
        return pfad.isAbsolute() ? pfad : basis().resolve(pfad);
    }
}
