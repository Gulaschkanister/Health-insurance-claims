package de.gkvtransmitter.wartung;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gibt es dort etwas Neueres als das, was hier liegt?
 *
 * <p>Die Anlagen stehen frei auf {@code gkv-datenaustausch.de}. Der Abgleich
 * braucht <b>keine neue Bibliothek</b> - {@code java.net.http.HttpClient}
 * gehoert seit Java 11 zum Sprachumfang.</p>
 *
 * <h2>Drei Festlegungen, bewusst getroffen</h2>
 *
 * <ol>
 *   <li><b>Auf Knopfdruck, nicht im Hintergrund.</b> Eine Anwendung, die
 *       Patientendaten haelt, ruft nicht unaufgefordert im Netz an.</li>
 *   <li><b>Ein Fehlschlag ist kein Fehler.</b> Kein Netz, Seite umgebaut,
 *       Zeitueberlauf: Das meldet sich als Hinweis und haelt nichts auf. Sonst
 *       stuende die Abrechnung still, weil eine Webseite sich geaendert hat.
 *       Deshalb wirft diese Klasse nichts - sie liefert ein {@link Ergebnis}
 *       auch dann, wenn nichts ging.</li>
 *   <li><b>Pruefen, nicht herunterladen.</b> Gemeldet wird "es gibt eine
 *       neuere Fassung, Stand X"; das Holen und Ablegen bleibt Handarbeit,
 *       weil danach ohnehin gelesen werden muss, was sich geaendert hat.</li>
 * </ol>
 *
 * <p>Erkannt werden die Dateinamen in der ausgelieferten Seite. Sie folgen
 * seit je demselben Muster - {@code Anlage_1_TP5_V22_20260521.pdf} -, und aus
 * ihm lassen sich Version und Stand ablesen. <b>Bricht das Muster, meldet die
 * Pruefung "nichts gefunden" und nicht "alles aktuell":</b> Der Unterschied
 * entscheidet darueber, ob jemand nachsieht.</p>
 */
public final class Unterlagenpruefung {

    /**
     * Das Muster der Dateinamen: Name, Version, Datum.
     *
     * <p>{@code Anlage_1_TP5_V21_20260115.pdf} und
     * {@code Anhang_03_Anlage_1_TP5_V10_20260414.pdf} folgen ihm beide.
     * {@code Anhang_1_Anlage_1_TP5_20170831.pdf} fuehrt keine Version -
     * deshalb ist die Versionsgruppe wahlfrei.</p>
     */
    private static final Pattern DATEINAME = Pattern.compile(
            "((?:Anlage|Anhang)_[A-Za-z0-9_]*?)(?:_V(\\d+))?_(\\d{8})\\.pdf",
            Pattern.CASE_INSENSITIVE);

    /** Wie eine gefundene Datei aussieht. */
    public record Gefunden(String dateiname, String version, LocalDate stand) {
    }

    /**
     * Was die Pruefung ergeben hat.
     *
     * @param erreichbar ob die Seite ueberhaupt gelesen werden konnte
     * @param neuere     Dateien, die neuer sind als alles Hinterlegte
     * @param gefunden   wie viele Dateien die Seite ueberhaupt nannte
     * @param hinweis    warum es nicht ging, oder {@code null}
     */
    public record Ergebnis(boolean erreichbar, List<Gefunden> neuere, int gefunden, String hinweis) {

        /** Ob es etwas Neues gibt. */
        public boolean gibtNeueres() {
            return !neuere.isEmpty();
        }

        /**
         * Ob die Seite zwar erreichbar war, aber kein Dateiname darin passte.
         *
         * <p>Das ist <b>nicht</b> dasselbe wie "alles aktuell". Wer die beiden
         * gleich behandelt, meldet Aktualitaet, weil eine Webseite umgebaut
         * wurde.</p>
         */
        public boolean nichtsErkannt() {
            return erreichbar && gefunden == 0;
        }
    }

    /**
     * Woher der Seiteninhalt kommt.
     *
     * <p>Als einmethodige Schnittstelle, damit die Auswertung ohne Netz
     * pruefbar ist - dieselbe Ueberlegung wie bei
     * {@code Datenaustauschreferenzen} und {@code Protokollfuehrung}. Der Netz-
     * zugriff selbst liegt in {@link Netzabruf}.</p>
     */
    @FunctionalInterface
    public interface Seitenabruf {
        /**
         * Liefert den Inhalt der Seite.
         *
         * @throws Exception wenn sie nicht zu erreichen ist
         */
        String lies(String adresse) throws Exception;
    }

    private final Unterlagenstand hinterlegt;
    private final Seitenabruf abruf;

    public Unterlagenpruefung(Unterlagenstand hinterlegt, Seitenabruf abruf) {
        this.hinterlegt = Objects.requireNonNull(hinterlegt, "hinterlegt must not be null");
        this.abruf = Objects.requireNonNull(abruf, "abruf must not be null");
    }

    /**
     * Prueft gegen die Bezugsquelle aus der hinterlegten Liste.
     *
     * <p>Wirft nichts: Ein Fehlschlag ist kein Fehler, sondern ein Hinweis.</p>
     */
    public Ergebnis pruefe() {
        String adresse = hinterlegt.quelle();
        if (adresse == null || adresse.isBlank()) {
            return new Ergebnis(false, List.of(), 0, "Keine Bezugsquelle hinterlegt.");
        }
        String seite;
        try {
            seite = abruf.lies(adresse);
        } catch (Exception nichtErreichbar) {
            return new Ergebnis(false, List.of(), 0,
                    nichtErreichbar.getMessage() == null
                            ? nichtErreichbar.getClass().getSimpleName()
                            : nichtErreichbar.getMessage());
        }
        if (seite == null) {
            return new Ergebnis(false, List.of(), 0, "Die Seite lieferte nichts.");
        }
        List<Gefunden> alle = dateienIn(seite);
        return new Ergebnis(true, neuereAls(alle), alle.size(), null);
    }

    /** Alle Dateinamen der Seite, die dem Muster folgen - ohne Doppelte. */
    static List<Gefunden> dateienIn(String seite) {
        Set<String> gesehen = new LinkedHashSet<>();
        List<Gefunden> gefunden = new ArrayList<>();
        Matcher treffer = DATEINAME.matcher(seite);
        while (treffer.find()) {
            String dateiname = treffer.group(0);
            if (!gesehen.add(dateiname.toLowerCase(Locale.ROOT))) {
                continue;
            }
            LocalDate stand = datumAus(treffer.group(3));
            if (stand != null) {
                gefunden.add(new Gefunden(dateiname, treffer.group(2), stand));
            }
        }
        return gefunden;
    }

    /**
     * Welche der gefundenen Dateien neuer sind als das, was hier liegt.
     *
     * <p><b>Je Unterlage und nicht insgesamt.</b> Die Bezugsquelle ist ein
     * Archiv: Sie fuehrt jede Fassung seit 2008, von
     * {@code Anlage_1_TP5_V7_20110610.pdf} bis heute. Ein Vergleich gegen
     * "alles Hinterlegte" meldete deshalb achtzehn Treffer, darunter
     * {@code Anhang_03_Anlage_1_TP5_20120912.pdf} - vierzehn Jahre alt. Das
     * ist beim ersten Lauf gegen die echte Seite herausgekommen und in keinem
     * Test davor.</p>
     *
     * <p>Verglichen wird ueber den <b>Stand</b> und nicht ueber die
     * Versionsnummer: Eine Fassung kann fortgeschrieben werden, ohne dass die
     * Version steigt - genau das ist zwischen
     * {@code Anlage_3_TP5_V22_20260218} und {@code ..._20260521} geschehen.</p>
     *
     * <p>Unbekannte Namen werden <b>uebergangen</b>, nicht gemeldet. Auf der
     * Seite stehen fast hundert Dateien, die meisten davon gehen diese
     * Anwendung nichts an.</p>
     */
    private List<Gefunden> neuereAls(List<Gefunden> gefunden) {
        List<Gefunden> neuere = new ArrayList<>();
        for (Gefunden datei : gefunden) {
            juengsterStandZu(datei.dateiname())
                    .filter(hier -> datei.stand().isAfter(hier))
                    .ifPresent(hier -> neuere.add(datei));
        }
        return neuere;
    }

    /**
     * Der juengste Stand, den wir zu dieser Datei hier liegen haben.
     *
     * <p>Zugeordnet wird ueber {@code quellname} - den Namensanfang, unter dem
     * dieselbe Unterlage auf der Bezugsquelle steht. Die Namen weichen ab:
     * {@code Anhang_3_Kostentraegerdatei_V10_20260414.pdf} heisst dort
     * {@code Anhang_03_Anlage_1_TP5_V10_20260414.pdf}. Der laengste passende
     * Name gewinnt, damit {@code Anhang_03_Anlage_1_TP5} nicht an einem
     * kuerzeren haengenbleibt.</p>
     *
     * <p>Leer heisst: nicht zugeordnet und damit nicht zu melden - <b>lieber
     * gar nicht geprueft als falsch verglichen.</b></p>
     */
    private Optional<java.time.LocalDate> juengsterStandZu(String dateiname) {
        String klein = dateiname.toLowerCase(Locale.ROOT);
        return hinterlegt.alle().stream()
                .filter(Unterlage::istPruefbar)
                .filter(unterlage -> klein.startsWith(unterlage.quellname().toLowerCase(Locale.ROOT)))
                .filter(unterlage -> unterlage.stand() != null)
                .max(java.util.Comparator.comparing(Unterlage::stand))
                .map(Unterlage::stand);
    }

    /** {@code JJJJMMTT} als Datum, oder {@code null} - eine Pruefung stuerzt nicht ab. */
    private static LocalDate datumAus(String achtstellig) {
        try {
            return LocalDate.of(Integer.parseInt(achtstellig.substring(0, 4)),
                    Integer.parseInt(achtstellig.substring(4, 6)),
                    Integer.parseInt(achtstellig.substring(6, 8)));
        } catch (RuntimeException unlesbar) {
            return null;
        }
    }
}
