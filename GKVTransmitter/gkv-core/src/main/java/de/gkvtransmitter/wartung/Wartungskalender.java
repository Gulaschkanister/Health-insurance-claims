package de.gkvtransmitter.wartung;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.entity.Betriebsdaten;

/**
 * Was wann faellig ist - aus dem, was das Programm ohnehin weiss.
 *
 * <p><b>Damit schrumpft die Wartung auf eine Aufgabe im Jahr:</b> das
 * Zertifikat erneuern. Alles andere meldet sich selbst. Genau das ist die
 * Bedingung, unter der der eigene Uebermittlungsweg ueberhaupt sinnvoll ist -
 * die Ersparnis gegenueber einer Abrechnungsstelle liegt bei vierzig
 * Kursterminen im Jahr zwischen fuenfzig und zweihundertvierzig Euro. Kostet
 * die Pflege mehr als ein bis drei Arbeitsstunden, ist der eigene Weg trotz
 * niedrigerer Gebuehr teurer.</p>
 *
 * <p>Zusammengetragen wird aus drei Quellen: den Betriebsdaten (Zertifikat),
 * dem {@link Unterlagenstand} (neue Fassungen) und dem, was gar kein Datum
 * hat. <b>Der dritte Teil steht ausdruecklich mit drauf</b> - ein Kalender,
 * der nur zeigt, was er weiss, sieht vollstaendig aus und ist es nicht.</p>
 */
public final class Wartungskalender {

    /** Innerhalb dieser Frist meldet sich eine Faelligkeit von selbst. */
    public static final int VORWARNUNG_WOCHEN = 6;

    private final Betriebsdaten betrieb;
    private final Unterlagenstand unterlagen;

    public Wartungskalender(Betriebsdaten betrieb, Unterlagenstand unterlagen) {
        this.betrieb = betrieb;
        this.unterlagen = Objects.requireNonNull(unterlagen, "unterlagen must not be null");
    }

    /** Alle Faelligkeiten, die naechste zuerst; die ohne Datum zuletzt. */
    public List<Faelligkeit> alle(LocalDate heute) {
        List<Faelligkeit> faelligkeiten = new ArrayList<>();
        faelligkeiten.add(zertifikat());
        faelligkeiten.addAll(neueFassungen(heute));
        faelligkeiten.add(kostentraegerdatei());
        faelligkeiten.add(positionsnummern());
        return faelligkeiten.stream()
                .sorted(Comparator.comparing(Faelligkeit::faelligAm,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Was innerhalb der Vorwarnfrist ansteht.
     *
     * <p>Die Seite soll <b>schweigen</b>, solange nichts ansteht. Eine Meldung,
     * die bei jedem Start erscheint, wird nach der dritten Woche nicht mehr
     * gelesen.</p>
     */
    public List<Faelligkeit> anstehende(LocalDate heute) {
        return alle(heute).stream()
                .filter(faelligkeit -> faelligkeit.stehtAn(heute, VORWARNUNG_WOCHEN))
                .toList();
    }

    /**
     * Das Zertifikat - die eine Aufgabe, die bleibt.
     *
     * <p>Der Antrag will ein bis zwei Wochen vorher gestellt sein; deshalb
     * meldet sich der Termin sechs Wochen im Voraus und nicht am Ablauftag.
     * Das Datum steht in den Betriebsdaten (B1) und nirgends sonst - die
     * Anwendung kann ein Zertifikat weder lesen noch pruefen.</p>
     */
    private Faelligkeit zertifikat() {
        LocalDate ablauf = datumAus(betrieb == null ? null : betrieb.getZertifikatBis());
        return new Faelligkeit("Zertifikat erneuern", "jaehrlich, Antrag 1-2 Wochen vorher", ablauf,
                ablauf != null
                        ? "Ablaufdatum aus den Betriebsdaten"
                        : "Kein Ablaufdatum erfasst - unter \"Betriebsdaten\" nachtragen");
    }

    /**
     * Neue Fassungen der Anlagen - und abgelaufene.
     *
     * <p>Abgelaufene zuerst und mit eigenem Eintrag: Das ist der Fall, den
     * niemand bemerkt, weil die Anwendung weitersendet und erst die Kasse
     * zurueckweist.</p>
     */
    private List<Faelligkeit> neueFassungen(LocalDate heute) {
        List<Faelligkeit> faelligkeiten = new ArrayList<>();
        for (Unterlage abgelaufen : unterlagen.abgelaufene(heute)) {
            faelligkeiten.add(new Faelligkeit(
                    abgelaufen.titel() + " Version " + abgelaufen.version() + " abgelaufen",
                    "einmalig", abgelaufen.gueltigBis(),
                    "Deckblatt der Nachfolgefassung"));
        }
        for (Unterlage bevorstehend : unterlagen.bevorstehende(heute)) {
            faelligkeiten.add(new Faelligkeit(
                    bevorstehend.titel() + " Version " + bevorstehend.version() + " anwenden",
                    "bei neuer Fassung, 3 Monate Uebergangsfrist", bevorstehend.anzuwendenAb(),
                    "Deckblatt der vorliegenden Fassung"));
        }
        return faelligkeiten;
    }

    /**
     * Die Kostentraegerdatei - vierteljaehrlich, und ohne Datum.
     *
     * <p><b>Die Anwendung liest sie noch nicht ein.</b>
     * {@code Kostentraegerdatei} kann es seit dem 17.09.2026 und hat in der
     * Anwendung keinen Aufrufer - dasselbe Muster wie bei
     * {@code nextDtaInterchangeReference}, das die gepruefte Methode hatte und
     * niemand rief. Solange das so ist, gibt es kein Dateidatum, aus dem sich
     * ein Termin bilden liesse, und ein erfundener waere schlimmer als
     * keiner.</p>
     */
    private Faelligkeit kostentraegerdatei() {
        return new Faelligkeit("Kostentraegerdatei erneuern", "vierteljaehrlich", null,
                "Kein Datum bekannt - die Anwendung liest die Datei noch nicht ein");
    }

    /** Ein Merkposten ohne Datum: Das Verzeichnis liegt dem Projekt nicht vor. */
    private Faelligkeit positionsnummern() {
        return new Faelligkeit("Positionsnummernverzeichnis pruefen", "bei Vertragsaenderung", null,
                "Kein Datum bekannt - das Verzeichnis der Hebammenhilfe-Verguetungsvereinbarung"
                        + " liegt dem Projekt nicht vor");
    }

    /** {@code yyyy-MM-dd}, oder {@code null} - ein Kalender stuerzt nicht ab. */
    private static LocalDate datumAus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException unlesbar) {
            return null;
        }
    }
}
