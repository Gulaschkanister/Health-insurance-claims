package de.gkvtransmitter.entity;

import jakarta.persistence.Embeddable;

/**
 * Wo jemand wohnt - vier Angaben, die immer zusammen auftreten.
 *
 * <p><b>Der Grund ist ein Checkstyle-Befund, aber nicht nur.</b> Die
 * Konstruktoren von {@link Person}, {@link Patient} und {@link ServiceProvider}
 * trugen je neun Parameter, davon vier fuer die Anschrift, alle als
 * {@code String} oder {@code int}. Eine Zeile wie</p>
 *
 * <pre>new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345, ...)</pre>
 *
 * <p>laesst sich nicht lesen, ohne die Signatur danebenzulegen - und
 * <b>"DE" an der Stelle der Hausnummer faellt nicht auf</b>, weil beides
 * Zeichenketten sind. Genau das ist der Fehler, den ein Buendel verhindert.</p>
 *
 * <p>{@code @Embeddable}: Die Spalten bleiben dieselben. Eine Umbenennung
 * haette bestehende Datenbanken leerlaufen lassen - Hibernate legt fehlende
 * Spalten an, aber es fuellt sie nicht.</p>
 *
 * @param strasse     die Strasse ohne Hausnummer
 * @param hausnummer  die Hausnummer, gegebenenfalls mit Zusatz
 * @param plz         fuenfstellig
 * @param land        Laenderkennzeichen, fuer Deutschland {@code DE}
 */
@Embeddable
public record Anschrift(String strasse, String hausnummer, int plz, String land) {

    /** Eine leere Anschrift - fuer Hibernate und fuer noch nicht Erfasstes. */
    public Anschrift() {
        this(null, null, 0, null);
    }
}
