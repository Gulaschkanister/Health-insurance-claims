package de.gkvtransmitter.dispatch;

import java.util.Objects;

/**
 * Die Stelle, die eine Lieferung tatsaechlich entgegennimmt.
 *
 * <p><b>Nicht die Kasse.</b> Zugestellt wird an die Datenannahmestelle
 * <em>mit Entschluesselungsbefugnis</em> - die Kasse selbst ist nur der
 * Kostentraeger, an den die Forderung geht. Wer die beiden verwechselt,
 * liefert an einen Empfaenger, der die Datei gar nicht oeffnen darf.</p>
 *
 * @param ik           IK der Annahmestelle
 * @param name         Kurzbezeichnung aus dem IDK-Segment
 * @param protokoll    Schluessel des DFUE-Protokolls nach Anhang 3, Abschnitt
 *                     8.5: {@code 070} E-Mail, {@code 016} FTAM, {@code 080}
 *                     KIM. Leer, wenn die Datei keinen DFU-Eintrag fuehrt.
 * @param kanal        der Kommunikationskanal - bei {@code 070} die
 *                     E-Mail-Anschrift
 */
public record Annahmestelle(int ik, String name, String protokoll, String kanal) {

    /** Schluessel fuer E-Mail ueber das Internet, Anhang 3 Abschnitt 8.5. */
    public static final String PROTOKOLL_EMAIL = "070";
    /** Schluessel fuer FTAM, Anhang 3 Abschnitt 8.5. */
    public static final String PROTOKOLL_FTAM = "016";
    /** Schluessel fuer KIM-Mail innerhalb der TI, Anhang 3 Abschnitt 8.5. */
    public static final String PROTOKOLL_KIM = "080";

    public Annahmestelle {
        name = name == null ? "" : name;
        protokoll = protokoll == null ? "" : protokoll;
        kanal = kanal == null ? "" : kanal;
    }

    /** Ob ein Weg hinterlegt ist, ueber den sich etwas zustellen laesst. */
    public boolean hatUebertragungsweg() {
        return !protokoll.isEmpty() && !kanal.isEmpty();
    }

    @Override
    public String toString() {
        return Objects.toString(name, "") + " (" + ik + ")"
                + (hatUebertragungsweg() ? ", " + protokoll + " " + kanal : "");
    }
}
