package de.gkvtransmitter.hibernate.sqllite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Legt fest, gegen welche Datei die Anwendung arbeitet und wie mit dem Schema
 * umgegangen wird.
 *
 * <p>Zuvor stand beides fest in der {@code hibernate.cfg.xml}: der Pfad
 * {@code database.db} relativ zum Arbeitsverzeichnis und {@code hbm2ddl.auto=create},
 * was das Schema bei jedem Programmstart neu angelegt und damit alle Daten
 * verworfen hat. Beides ist jetzt von aussen steuerbar, damit Produktivbetrieb,
 * manuelle Tests und automatisierte Tests auf getrennten Datenbanken laufen
 * koennen.</p>
 */
public final class DatabaseSettings {

    /** Systemeigenschaft fuer den Pfad der SQLite-Datei. */
    public static final String PATH_PROPERTY = "gkv.db.path";
    /** Umgebungsvariable fuer den Pfad der SQLite-Datei. */
    public static final String PATH_ENV = "GKV_DB_PATH";
    /** Systemeigenschaft fuer den Schema-Modus. */
    public static final String SCHEMA_PROPERTY = "gkv.db.schema";
    /** Umgebungsvariable fuer den Schema-Modus. */
    public static final String SCHEMA_ENV = "GKV_DB_SCHEMA";

    private static final String DEFAULT_PATH = "database.db";

    /**
     * Ergaenzt fehlende Tabellen und Spalten, laesst vorhandene Daten aber stehen.
     * Bewusst der Standard - {@code create} wuerde bei jedem Start alles verwerfen.
     */
    private static final String DEFAULT_SCHEMA_MODE = "update";

    private static final Set<String> ERLAUBTE_SCHEMA_MODI =
            Set.of("none", "validate", "update", "create", "create-drop");

    /** Modi, die bestehende Daten zerstoeren. */
    private static final Set<String> ZERSTOERENDE_MODI = Set.of("create", "create-drop");

    /**
     * Wartezeit in Millisekunden, bevor ein blockierter Schreibzugriff aufgibt.
     *
     * <p>SQLite laesst genau einen Schreiber zur selben Zeit zu. Ohne diese
     * Einstellung bricht ein zweiter gleichzeitiger Zugriff sofort mit
     * {@code SQLITE_BUSY} ab, statt kurz zu warten - was etwa zwei parallele
     * Abrechnungslaeufe scheitern liesse. Mit dem Zeitfenster reihen sich die
     * Schreiber hintereinander ein.</p>
     */
    private static final int BUSY_TIMEOUT_MS = 10_000;

    /**
     * Verbindungsparameter fuer Dateidatenbanken.
     *
     * <p>{@code journal_mode=WAL} laesst Leser und Schreiber nebeneinander
     * arbeiten, statt dass ein Schreibvorgang alle Leser blockiert.
     * {@code foreign_keys=on} ist noetig, weil SQLite Fremdschluessel sonst
     * zwar entgegennimmt, aber nicht durchsetzt.</p>
     */
    private static final String DATEI_PARAMETER =
            "?busy_timeout=" + BUSY_TIMEOUT_MS + "&journal_mode=WAL&foreign_keys=on";

    private final String jdbcUrl;
    private final String schemaMode;
    private final String beschreibung;

    private DatabaseSettings(String jdbcUrl, String schemaMode, String beschreibung) {
        this.jdbcUrl = jdbcUrl;
        this.schemaMode = schemaMode;
        this.beschreibung = beschreibung;
    }

    /**
     * Liest die Einstellungen aus Systemeigenschaften, ersatzweise aus
     * Umgebungsvariablen, ersatzweise aus den Standardwerten.
     */
    public static DatabaseSettings fromEnvironment() {
        String pfad = ersteBelegung(PATH_PROPERTY, PATH_ENV, DEFAULT_PATH);
        String modus = normalisiereSchemaModus(
                ersteBelegung(SCHEMA_PROPERTY, SCHEMA_ENV, DEFAULT_SCHEMA_MODE));
        Path datei = Paths.get(pfad).toAbsolutePath();
        return new DatabaseSettings(dateiUrl(datei), modus, datei.toString());
    }

    /** Richtet die Einstellungen auf eine konkrete Datei aus. */
    public static DatabaseSettings forFile(Path databaseFile) {
        Objects.requireNonNull(databaseFile, "databaseFile must not be null");
        Path absolut = databaseFile.toAbsolutePath();
        return new DatabaseSettings(dateiUrl(absolut), "update", absolut.toString());
    }

    /**
     * Datenbank, die nur im Arbeitsspeicher existiert - fuer Tests, die keine
     * Datei hinterlassen sollen.
     *
     * <p>Der Name ist Teil der URL, weil sich sonst jede Verbindung des Pools
     * ihre eigene, leere Datenbank anlegen wuerde. {@code cache=shared} sorgt
     * dafuer, dass alle Verbindungen dieselbe Datenbank sehen.</p>
     */
    public static DatabaseSettings inMemory(String name) {
        Objects.requireNonNull(name, "name must not be null");
        String url = "jdbc:sqlite:file:" + name + "?mode=memory&cache=shared&busy_timeout=" + BUSY_TIMEOUT_MS;
        return new DatabaseSettings(url, "create", "In-Memory (" + name + ")");
    }

    private static String dateiUrl(Path datei) {
        return "jdbc:sqlite:" + datei + DATEI_PARAMETER;
    }

    private static String ersteBelegung(String systemProperty, String umgebungsVariable, String standard) {
        String wert = System.getProperty(systemProperty);
        if (wert == null || wert.isBlank()) {
            wert = System.getenv(umgebungsVariable);
        }
        return (wert == null || wert.isBlank()) ? standard : wert.trim();
    }

    private static String normalisiereSchemaModus(String modus) {
        String normalisiert = modus.toLowerCase(Locale.ROOT);
        if (!ERLAUBTE_SCHEMA_MODI.contains(normalisiert)) {
            throw new IllegalArgumentException(
                    "Unbekannter Schema-Modus '" + modus + "'. Erlaubt sind: " + ERLAUBTE_SCHEMA_MODI);
        }
        return normalisiert;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getSchemaMode() {
        return schemaMode;
    }

    /** Lesbare Herkunft der Datenbank, fuer Log- und Fehlerausgaben. */
    public String getBeschreibung() {
        return beschreibung;
    }

    /** Ob der gewaehlte Modus bestehende Daten verwirft. */
    public boolean istZerstoerend() {
        return ZERSTOERENDE_MODI.contains(schemaMode);
    }

    @Override
    public String toString() {
        return "DatabaseSettings[" + beschreibung + ", schema=" + schemaMode + "]";
    }
}
