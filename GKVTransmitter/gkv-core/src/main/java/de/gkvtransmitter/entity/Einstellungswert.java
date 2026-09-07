package de.gkvtransmitter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Eine gespeicherte Einstellung: ein Schluessel, ein Wert.
 *
 * <p>Am 07.09.2026 lagen die Einstellungen noch in einer eigenen Datei
 * {@code einstellungen.json} neben der Datenbank. Simons Einwand am selben Tag
 * traegt weiter als der Vorschlag davor: <em>"da kann es auch einfach in der DB
 * gespeichert werden, muessen ja nicht extra Dateien herzaubern."</em></p>
 *
 * <p>Die Datei haette ihre Berechtigung nur, wenn sie etwas koennte, was die
 * Datenbank nicht kann. Sie kann es nicht: <b>beide liegen im selben
 * Datenordner</b>, werden von derselben Sicherung erfasst, ziehen gemeinsam um
 * und folgen gemeinsam {@code gkv.home}. Was die Datei dagegen mitbrachte, war
 * ein zweiter Ablageweg mit eigenem Lesefehler, eigenem Schreibfehler und
 * eigener Nebendatei - <b>Aufwand ohne Gegenwert.</b></p>
 *
 * <p>Der Verdacht, den ich gegen die Tabelle vorgebracht hatte, war
 * ueberdies falsch: Einstellungen wuerden Fachdaten "vermischen". Eine eigene
 * Tabelle vermischt nichts. Wer die Datenbank austauscht, nimmt seine
 * Darstellung mit - und dass die Uebermittlungsart demselben Stand folgt wie
 * die Abrechnungen, ist eher richtig als falsch.</p>
 *
 * <p><b>Keine Zugangsdaten, keine Schluessel.</b> Die Datenbank ist
 * unverschluesselt. Sobald der Versand echte Zugangsdaten braucht, gehoeren die
 * in den Windows-Anmeldeinformationsspeicher oder einen Schluesselbund - nicht
 * hierher.</p>
 */
@Entity
@Table(name = "einstellung")
public class Einstellungswert {

    @Id
    @Column(name = "schluessel", nullable = false)
    private String schluessel;

    @Column(name = "wert")
    private String wert;

    public Einstellungswert() {
    }

    public Einstellungswert(String schluessel, String wert) {
        this.schluessel = schluessel;
        this.wert = wert;
    }

    public String getSchluessel() {
        return schluessel;
    }

    public void setSchluessel(String schluessel) {
        this.schluessel = schluessel;
    }

    public String getWert() {
        return wert;
    }

    public void setWert(String wert) {
        this.wert = wert;
    }
}
