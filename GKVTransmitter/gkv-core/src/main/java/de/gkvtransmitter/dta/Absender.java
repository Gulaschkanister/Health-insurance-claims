package de.gkvtransmitter.dta;

import java.util.Objects;

/**
 * Die absendende Stelle einer Lieferung.
 *
 * <p><b>Nicht dasselbe wie der Leistungserbringer.</b> Im {@code UNB} steht,
 * wer die Datei schickt; im {@code FKT} steht, wer die Leistung erbracht hat.
 * Fuer eine allein arbeitende Hebamme ist das dieselbe Person und dasselbe IK -
 * sobald eine Abrechnungsstelle einspringt oder eine zweite Person abrechnet,
 * ist es das nicht mehr.</p>
 *
 * <p>Bis zum 17.09.2026 kannte das Programm den Unterschied nicht: Der Absender
 * stammte aus dem Dienstleister, der je Abrechnung ausgewaehlt wird, und die
 * Rolle stand fest auf „Selbstabrechner". Beides kommt jetzt aus den
 * {@code Betriebsdaten}.</p>
 *
 * @param ik              IK der absendenden Stelle; geht in den {@code UNB} und
 *                        mit den Stellen 3 bis 8 in den logischen Dateinamen
 * @param selbstabrechner ob der Leistungserbringer selbst absendet; bestimmt
 *                        die neunte Stelle des logischen Dateinamens
 *                        ({@code S} oder {@code A})
 */
public record Absender(String ik, boolean selbstabrechner) {

    public Absender {
        Objects.requireNonNull(ik, "ik must not be null");
    }

    /**
     * Der Absender, wie ihn ein Selbstabrechner ohne erfasste Betriebsdaten
     * hat: das IK des Leistungserbringers.
     *
     * <p>Die Rueckfallebene bildet den Stand vor den Betriebsdaten ab. Sie ist
     * fuer den haeufigsten Fall - eine Hebamme, die fuer sich selbst abrechnet -
     * richtig, und fuer jeden anderen falsch; deshalb meldet der Versanddienst
     * einen Hinweis, wenn er sie benutzen muss.</p>
     */
    public static Absender ausLeistungserbringer(String leistungserbringerIk) {
        return new Absender(leistungserbringerIk, true);
    }
}
