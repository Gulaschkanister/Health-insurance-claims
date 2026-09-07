package de.gkvtransmitter.einstellung;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.gkvtransmitter.repository.DataRepository;

/**
 * Was sich jemand einmal einstellt und beim naechsten Start wiederfinden will.
 *
 * <p>Bis zum 07.09.2026 gab es dafuer keinen Ort: die Anwendung merkte sich
 * nichts ausser Fachdaten. Eine Einstellungsseite ohne diesen Ort waere eine
 * Seite, die beim naechsten Start vergisst, was man ihr gesagt hat - <b>und das
 * ist schlechter als keine Seite.</b></p>
 *
 * <h2>Warum in der Datenbank</h2>
 *
 * <p>Am Vormittag des 07.09.2026 war es eine eigene Datei
 * {@code einstellungen.json}, am Nachmittag eine Tabelle. Simons Einwand
 * dazwischen: <em>"da kann es auch einfach in der DB gespeichert werden,
 * muessen ja nicht extra Dateien herzaubern."</em></p>
 *
 * <p>Er trifft. Die Datei lag im selben Ordner wie die Datenbank, wurde von
 * derselben Sicherung erfasst, zog mit ihr um und folgte demselben
 * {@code gkv.home} - <b>sie konnte nichts, was die Datenbank nicht auch
 * kann.</b> Mitgebracht hat sie einen zweiten Ablageweg mit eigenem Lesefehler,
 * eigenem Schreibfehler und eigener Nebendatei. Das ist kein Preis fuer einen
 * Vorteil, sondern einer fuer nichts.</p>
 *
 * <p>Der Einwand, den ich gegen die Tabelle vorgebracht hatte - sie "vermische
 * Einstellungen mit Fachdaten" -, war eine Behauptung ohne Rechnung. Eine
 * eigene Tabelle vermischt nichts.</p>
 *
 * <h2>Was hier nicht hineingehoert</h2>
 *
 * <p><b>Keine Zugangsdaten, keine Schluessel.</b> Die Datenbank ist
 * unverschluesselt. Sobald der Versand echte Zugangsdaten braucht, gehoeren die
 * in den Windows-Anmeldeinformationsspeicher oder einen Schluesselbund.</p>
 *
 * <h2>Verhalten im Fehlerfall</h2>
 *
 * <p><b>Ein Lesefehler haelt den Start nicht auf.</b> Laesst sich die Tabelle
 * nicht lesen, gelten die Vorgaben, und die Anwendung laeuft - wie
 * {@code Leistungsparameter} bei unlesbarer Blaupause. Eine Farbwahl ist
 * nichts, wofuer eine Anwendung nicht starten darf.</p>
 */
public final class Einstellungen {

    private final DataRepository datenbank;
    private final String ort;
    private final Map<String, String> werte;

    private Einstellungen(DataRepository datenbank, String ort, Map<String, String> werte) {
        this.datenbank = datenbank;
        this.ort = ort;
        this.werte = new LinkedHashMap<>(werte);
    }

    /**
     * Liest die Einstellungen aus der Datenbank.
     *
     * <p>Scheitert das Lesen, gelten die Vorgaben. Der Fall wird auf
     * {@code System.err} vermerkt: still darueber hinwegzugehen hiesse, dass
     * jemand seine Einstellungen verliert und nie erfaehrt, warum.</p>
     *
     * @param ort wie der Ablageort einem Menschen zu nennen ist - die Angabe
     *        kommt von aussen, weil ein Repository nicht sagen soll, wo es
     *        liegt
     */
    public static Einstellungen aus(DataRepository datenbank, String ort) {
        Objects.requireNonNull(datenbank, "datenbank must not be null");
        try {
            return new Einstellungen(datenbank, ort, datenbank.ladeEinstellungen());
        } catch (RuntimeException unlesbar) {
            System.err.println("Die Einstellungen liessen sich nicht lesen ("
                    + unlesbar.getMessage() + "). Es gelten die Vorgaben.");
            return new Einstellungen(datenbank, ort, Map.of());
        }
    }

    /** Wie {@link #aus(DataRepository, String)}, ohne Angabe zum Ablageort. */
    public static Einstellungen aus(DataRepository datenbank) {
        return aus(datenbank, "");
    }

    /** Der Wert einer Einstellung, oder die Vorgabe. */
    public String get(Einstellung einstellung) {
        Objects.requireNonNull(einstellung, "einstellung must not be null");
        String wert = werte.get(einstellung.schluessel());
        return wert == null || wert.isBlank() ? einstellung.vorgabe() : wert;
    }

    /**
     * Setzt einen Wert und schreibt ihn weg.
     *
     * <p>Sofort schreiben und nicht erst beim Beenden: eine Anwendung, die
     * abstuerzt, hat die Einstellung sonst nie gehabt - und wer sie gesetzt
     * hat, weiss nicht, ob sie galt.</p>
     *
     * <p><b>Der Wert gilt auch dann, wenn das Schreiben scheitert.</b> Wer die
     * dunkle Fassung waehlt, soll sie sehen; dass sie die Sitzung nicht
     * ueberlebt, sagt die Meldungsecke.</p>
     *
     * @return ob das Speichern gelungen ist
     */
    public boolean setze(Einstellung einstellung, String wert) {
        Objects.requireNonNull(einstellung, "einstellung must not be null");
        if (wert == null || wert.isBlank()) {
            werte.remove(einstellung.schluessel());
        } else {
            werte.put(einstellung.schluessel(), wert.trim());
        }
        try {
            datenbank.speichereEinstellung(einstellung.schluessel(), wert);
            return true;
        } catch (RuntimeException e) {
            System.err.println("Die Einstellung " + einstellung.schluessel()
                    + " liess sich nicht speichern (" + e.getMessage() + ").");
            return false;
        }
    }

    /** Wo die Einstellungen liegen, in Worten fuer einen Menschen. */
    public String ort() {
        return ort;
    }
}
