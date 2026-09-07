package de.gkvtransmitter.einstellung;

/**
 * Die Einstellungen, die es gibt.
 *
 * <p>Als Aufzaehlung und nicht als freie Zeichenketten: ein Schluessel, den es
 * nur an zwei Stellen gibt - einmal beim Schreiben, einmal beim Lesen -, laeuft
 * beim ersten Vertipper auseinander, ohne dass etwas fehlschlaegt. Wer eine
 * Einstellung hinzufuegt, traegt sie hier ein und bekommt beide Seiten vom
 * Uebersetzer geprueft.</p>
 *
 * <p>Die Vorgabe ist Teil der Aufzaehlung, damit sie nicht dreimal im Quelltext
 * steht. Sie gilt, solange niemand etwas anderes gewaehlt hat, und ebenso, wenn
 * sich die Tabelle nicht lesen laesst.</p>
 */
public enum Einstellung {

    /**
     * Helle oder dunkle Oberflaeche.
     *
     * <p>Werte: {@code hell} oder {@code dunkel}. Was daraus wird, entscheidet
     * die Oberflaeche - der Kern kennt keine Farben.</p>
     */
    DARSTELLUNG("darstellung", "hell"),

    /**
     * Was im UNB-Segment als Art der Datei steht.
     *
     * <p>Werte: {@code test}, {@code erprobung}, {@code echt}. Die Technische
     * Anlage (Anlage 1, Abschnitt 5.4) sieht an der letzten Stelle des UNB
     * genau diese Unterscheidung vor: <b>0 = Testdatei, 1 = Erprobungsdatei,
     * 2 = Echtdatei</b>.</p>
     *
     * <p><b>Bis zum 07.09.2026 stand dort fest eine 1.</b> Jede erzeugte Datei
     * bezeichnete sich damit als Erprobungsdatei - was zum heutigen Stand des
     * Programms passt, aber niemand wusste es, und beim ersten Echtversand
     * waere es die stillste denkbare Zurueckweisung: eine Kasse verarbeitet
     * eine Erprobungsdatei nicht als Forderung.</p>
     *
     * <p>Die Vorgabe bleibt bewusst {@code erprobung}. Ein Programm, das
     * Forderungen an Krankenkassen stellt, darf nicht von selbst in den
     * Echtbetrieb wechseln, weil sich eine Zeile in der Datenbank nicht lesen
     * laesst.</p>
     */
    UEBERMITTLUNGSART("uebermittlungsart", "erprobung");

    private final String schluessel;
    private final String vorgabe;

    Einstellung(String schluessel, String vorgabe) {
        this.schluessel = schluessel;
        this.vorgabe = vorgabe;
    }

    /** Der Schluessel in der Tabelle {@code einstellung}. */
    public String schluessel() {
        return schluessel;
    }

    /** Was gilt, solange niemand etwas anderes gewaehlt hat. */
    public String vorgabe() {
        return vorgabe;
    }
}
