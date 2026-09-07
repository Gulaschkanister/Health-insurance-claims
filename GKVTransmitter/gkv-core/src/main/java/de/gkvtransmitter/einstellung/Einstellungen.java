package de.gkvtransmitter.einstellung;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.util.Anwendungsverzeichnis;

/**
 * Was sich jemand einmal einstellt und beim naechsten Start wiederfinden will.
 *
 * <p>Bis zum 07.09.2026 gab es dafuer keinen Ort: die Anwendung merkte sich
 * nichts ausser Fachdaten. Eine Einstellungsseite ohne diesen Ort waere eine
 * Seite, die beim naechsten Start vergisst, was man ihr gesagt hat - <b>und das
 * ist schlechter als keine Seite.</b></p>
 *
 * <h2>Warum eine eigene Datei</h2>
 *
 * <p>Simons Vorschlag, und er traegt: <em>"eine JSON-Datei, aehnlich wie die
 * Datenbank, die ja auch nicht in der EXE liegt."</em> Sie liegt im selben
 * Datenordner wie die Datenbank und erbt damit alles, was dafuer schon
 * geregelt ist - den Umzug beim Umbenennen der Anwendung, die Sicherung, und
 * die Umlenkung ueber {@code gkv.home} in Tests und Werkzeugen.</p>
 *
 * <p>Die beiden Gegenentwuerfe wurden verworfen: eine Tabelle in der Datenbank
 * vermischt Einstellungen mit Fachdaten und ginge beim Austausch der Datei
 * verloren; {@code java.util.prefs} landet in der Windows-Registry und entzieht
 * sich damit jedem Umzug und jeder Sicherung.</p>
 *
 * <h2>Was hier nicht hineingehoert</h2>
 *
 * <p><b>Keine Zugangsdaten, keine Schluessel.</b> Die Datei liegt im Klartext
 * im Benutzerprofil. Sobald der Versand echte Zugangsdaten braucht, gehoeren
 * die in den Windows-Anmeldeinformationsspeicher oder einen Schluesselbund -
 * nicht hierher.</p>
 *
 * <h2>Verhalten im Fehlerfall</h2>
 *
 * <p><b>Ein Lesefehler haelt den Start nicht auf.</b> Ist die Datei
 * beschaedigt, gesperrt oder halb geschrieben, gelten die Vorgaben, und die
 * Anwendung laeuft - wie {@code Leistungsparameter} bei unlesbarer Blaupause.
 * Eine Farbwahl ist nichts, wofuer eine Anwendung nicht starten darf.</p>
 *
 * <p>Beim Schreiben wird zuerst eine Nebendatei angelegt und dann umbenannt.
 * Sonst bliebe nach einem Stromausfall mitten im Schreiben eine halbe Datei
 * zurueck - und die naechste Fassung waere weg, obwohl die alte gereicht
 * haette.</p>
 */
public final class Einstellungen {

    /** Name der Datei im Datenordner der Anwendung. */
    public static final String DATEINAME = "einstellungen.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path datei;
    private final Map<String, String> werte;

    private Einstellungen(Path datei, Map<String, String> werte) {
        this.datei = datei;
        this.werte = new LinkedHashMap<>(werte);
    }

    /** Liest die Einstellungen aus dem Datenordner der Anwendung. */
    public static Einstellungen laden() {
        return laden(Anwendungsverzeichnis.basis().resolve(DATEINAME));
    }

    /**
     * Liest die Einstellungen aus einer bestimmten Datei.
     *
     * <p>Gibt es sie nicht oder ist sie unlesbar, gelten die Vorgaben. Der
     * zweite Fall wird auf {@code System.err} vermerkt: still darueber
     * hinwegzugehen hiesse, dass jemand seine Einstellungen verliert und nie
     * erfaehrt, warum.</p>
     */
    public static Einstellungen laden(Path datei) {
        Objects.requireNonNull(datei, "datei must not be null");
        if (!Files.isRegularFile(datei)) {
            return new Einstellungen(datei, Map.of());
        }
        try {
            String inhalt = Files.readString(datei, StandardCharsets.UTF_8);
            Map<String, String> gelesen = MAPPER.readValue(inhalt,
                    new TypeReference<LinkedHashMap<String, String>>() { });
            return new Einstellungen(datei, gelesen == null ? Map.of() : gelesen);
        } catch (IOException | RuntimeException unlesbar) {
            System.err.println("Die Einstellungen in " + datei + " liessen sich nicht lesen ("
                    + unlesbar.getMessage() + "). Es gelten die Vorgaben.");
            return new Einstellungen(datei, Map.of());
        }
    }

    /** Der Wert einer Einstellung, oder die Vorgabe. */
    public String get(Einstellung einstellung) {
        Objects.requireNonNull(einstellung, "einstellung must not be null");
        String wert = werte.get(einstellung.schluessel());
        return wert == null || wert.isBlank() ? einstellung.vorgabe() : wert;
    }

    /**
     * Setzt einen Wert und schreibt die Datei.
     *
     * <p>Sofort schreiben und nicht erst beim Beenden: eine Anwendung, die
     * abstuerzt, hat die Einstellung sonst nie gehabt - und wer sie gesetzt
     * hat, weiss nicht, ob sie galt.</p>
     *
     * @return ob das Schreiben gelungen ist
     */
    public boolean setze(Einstellung einstellung, String wert) {
        Objects.requireNonNull(einstellung, "einstellung must not be null");
        if (wert == null || wert.isBlank()) {
            werte.remove(einstellung.schluessel());
        } else {
            werte.put(einstellung.schluessel(), wert.trim());
        }
        return schreibe();
    }

    /** Die Datei, in der die Einstellungen liegen. */
    public Path ort() {
        return datei;
    }

    /**
     * Schreibt ueber eine Nebendatei.
     *
     * <p>{@code ATOMIC_MOVE} ist bewusst <em>nicht</em> gefordert: auf manchen
     * Dateisystemen gibt es das nicht, und ein Ersetzen ohne diese Zusage ist
     * immer noch besser als direkt in die Zieldatei zu schreiben.</p>
     */
    private boolean schreibe() {
        Path neben = datei.resolveSibling(datei.getFileName() + ".neu");
        try {
            Files.createDirectories(datei.getParent());
            Files.writeString(neben, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(werte),
                    StandardCharsets.UTF_8);
            Files.move(neben, datei, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("Die Einstellungen liessen sich nicht speichern (" + e.getMessage() + ").");
            try {
                Files.deleteIfExists(neben);
            } catch (IOException aufraeumen) {
                // Eine liegengebliebene Nebendatei stoert niemanden.
            }
            return false;
        }
    }
}
