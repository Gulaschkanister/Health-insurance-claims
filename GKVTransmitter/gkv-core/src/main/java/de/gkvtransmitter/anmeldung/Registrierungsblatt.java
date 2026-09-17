package de.gkvtransmitter.anmeldung;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.dta.DtaFactory;
import de.gkvtransmitter.dta.Leistungsbereich;
import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.entity.ServiceProvider;

/**
 * Alles, was bei der Anmeldung zum Datenaustausch gefragt wird, auf einem
 * Blatt.
 *
 * <p><b>Ein Formular gibt die Anlage nicht vor.</b> Anlage 1, Abschnitt 2
 * Absatz 1 sagt nur: "Die Einzelheiten zur Durchfuehrung der Datenuebermittlung
 * sind rechtzeitig vor der erstmaligen Durchfuehrung oder Aenderung des
 * Datenaustauschverfahrens zwischen dem Absender und dem Empfaenger der Daten
 * abzustimmen." Was die ARGE·IK, ein Trust Center und die Annahmestelle im
 * Einzelnen abfragen, steht in deren eigenen Formularen, und die liegen dem
 * Projekt nicht vor.</p>
 *
 * <p>Dieses Blatt ist deshalb <b>kein Antrag, sondern eine Zusammenstellung</b>
 * - das, was man bei dieser Abstimmung zur Hand haben muss, an einer Stelle
 * statt in vier Masken. Ein Blatt, das sich als amtliches Formular ausgaebe,
 * waere schlimmer als keines.</p>
 *
 * <p>Der letzte Abschnitt ist der wichtigste: <b>was noch fehlt</b>. Eine
 * Zusammenstellung, die Luecken verschweigt, sieht vollstaendig aus und ist es
 * nicht.</p>
 */
public final class Registrierungsblatt {

    private static final DateTimeFormatter TAG = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    /** Breite der Beschriftungsspalte, damit die Werte untereinander stehen. */
    private static final int SPALTE = 26;

    private final Betriebsdaten betrieb;
    private final List<ServiceProvider> leistungserbringer;
    private final LocalDate stand;

    public Registrierungsblatt(Betriebsdaten betrieb, List<ServiceProvider> leistungserbringer,
            LocalDate stand) {
        this.betrieb = betrieb;
        this.leistungserbringer = leistungserbringer == null ? List.of() : List.copyOf(leistungserbringer);
        this.stand = Objects.requireNonNull(stand, "stand must not be null");
    }

    /** Das Blatt als Text, zum Anzeigen, Ablegen und Ausdrucken. */
    public String alsText() {
        StringBuilder blatt = new StringBuilder();
        kopf(blatt);
        absender(blatt);
        erbringer(blatt);
        verfahren(blatt);
        luecken(blatt);
        return blatt.toString();
    }

    private void kopf(StringBuilder blatt) {
        blatt.append("REGISTRIERUNGSBLATT FUER DEN DATENAUSTAUSCH NACH § 302 SGB V")
                .append(System.lineSeparator())
                .append("Stand: ").append(stand.format(TAG))
                .append(" · erzeugt von GKV-Abrechnung")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("Anlage 1, Abschnitt 2 Absatz 1: \"Die Einzelheiten zur Durchfuehrung der")
                .append(System.lineSeparator())
                .append("Datenuebermittlung sind rechtzeitig vor der erstmaligen Durchfuehrung [...]")
                .append(System.lineSeparator())
                .append("zwischen dem Absender und dem Empfaenger der Daten abzustimmen.\"")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("Ein Formular gibt die Anlage dafuer nicht vor. Dieses Blatt sammelt, was")
                .append(System.lineSeparator())
                .append("bei dieser Abstimmung zur Hand sein muss - es ist kein Antrag.")
                .append(System.lineSeparator());
    }

    private void absender(StringBuilder blatt) {
        abschnitt(blatt, "1  ABSENDER DER DATEIEN");
        if (betrieb == null) {
            blatt.append("   (keine Betriebsdaten erfasst)").append(System.lineSeparator());
            return;
        }
        zeile(blatt, "Praxis / Betrieb", betrieb.getPraxisname());
        zeile(blatt, "Strasse, Hausnummer", zusammen(betrieb.getStrasse(), betrieb.getHausnummer()));
        zeile(blatt, "PLZ, Ort", zusammen(betrieb.getPlz(), betrieb.getOrt()));
        zeile(blatt, "Institutionskennzeichen", betrieb.getIk());
        zeile(blatt, "Abrechnet", betrieb.istSelbstabrechner()
                ? "selbst (Leistungserbringer)" : "ueber eine Abrechnungsstelle");
        zeile(blatt, "Steuernummer", betrieb.getSteuernummer());
        zeile(blatt, "Ansprechpartner", betrieb.getAnsprechpartner());
        zeile(blatt, "Telefon", betrieb.getTelefon());
        zeile(blatt, "E-Mail", betrieb.getEmail());
        zeile(blatt, "Zertifikat gueltig bis", betrieb.getZertifikatBis());
    }

    private void erbringer(StringBuilder blatt) {
        abschnitt(blatt, "2  LEISTUNGSERBRINGER");
        if (leistungserbringer.isEmpty()) {
            blatt.append("   (keine erfasst)").append(System.lineSeparator());
            return;
        }
        for (ServiceProvider person : leistungserbringer) {
            blatt.append(System.lineSeparator());
            zeile(blatt, "Name", (person.getFirstname() + " " + person.getLastname()).strip());
            zeile(blatt, "Institutionskennzeichen", String.format("%09d", Math.max(person.getIk(), 0)));
            zeile(blatt, "Abrechnungscode", abrechnungscode(person));
            zeile(blatt, "Steuernummer", person.getSteuernummer());
            zeile(blatt, "Umsatzsteuer", person.istUmsatzsteuerbefreit()
                    ? "befreit nach § 4 UStG" : "nicht befreit");
            zeile(blatt, "Ansprechpartner", person.getAnsprechpartner());
            zeile(blatt, "Telefon", person.getTelefon());
            zeile(blatt, "E-Mail", person.getEmail());
        }
    }

    /** Der Code mit seinem Sammelgruppenschluessel - beides wird gefragt. */
    private static String abrechnungscode(ServiceProvider person) {
        if (!person.hatAbrechnungscode()) {
            return null;
        }
        String code = person.getAbrechnungscode().trim();
        return code + " (Leistungsbereich " + Leistungsbereich.zuAbrechnungscode(code) + ")";
    }

    private void verfahren(StringBuilder blatt) {
        abschnitt(blatt, "3  VERFAHREN, MIT DEM DIESE ANWENDUNG SENDET");
        zeile(blatt, "Zeichensatz (UNB)", DtaFactory.ZEICHENSATZ);
        zeile(blatt, "Nachrichtentypen", "SLGA:" + DtaFactory.NACHRICHTENVERSION
                + " und SLLA:" + DtaFactory.NACHRICHTENVERSION);
        zeile(blatt, "Richtlinie", "Anlage 1 zu den Richtlinien nach § 302 SGB V");
    }

    /**
     * Was noch fehlt.
     *
     * <p>Geprueft wird nur, was diese Anwendung wissen kann. Ein Zertifikat,
     * das es noch nicht gibt, kann sie nicht kennen - dass es gebraucht wird,
     * schon.</p>
     */
    private void luecken(StringBuilder blatt) {
        List<String> fehlend = fehlendeAngaben();
        abschnitt(blatt, "4  WAS NOCH FEHLT");
        if (fehlend.isEmpty()) {
            blatt.append("   Alle Angaben, die diese Anwendung fuehrt, sind vorhanden.")
                    .append(System.lineSeparator());
            return;
        }
        for (String luecke : fehlend) {
            blatt.append("   · ").append(luecke).append(System.lineSeparator());
        }
    }

    /** Die Luecken einzeln - auch fuer die Anzeige auf dem Bildschirm. */
    public List<String> fehlendeAngaben() {
        List<String> fehlend = new ArrayList<>();
        if (betrieb == null) {
            fehlend.add("Betriebsdaten: nicht erfasst");
            return fehlend;
        }
        pruefe(fehlend, "Betriebsdaten: Praxisname", betrieb.getPraxisname());
        pruefe(fehlend, "Betriebsdaten: Institutionskennzeichen", betrieb.getIk());
        pruefe(fehlend, "Betriebsdaten: Steuernummer", betrieb.getSteuernummer());
        pruefe(fehlend, "Betriebsdaten: Ansprechpartner", betrieb.getAnsprechpartner());
        pruefe(fehlend, "Betriebsdaten: Telefon", betrieb.getTelefon());
        pruefe(fehlend, "Betriebsdaten: E-Mail", betrieb.getEmail());
        pruefe(fehlend, "Betriebsdaten: Ablaufdatum des Zertifikats", betrieb.getZertifikatBis());
        if (leistungserbringer.isEmpty()) {
            fehlend.add("Leistungserbringer: keiner erfasst");
        }
        for (ServiceProvider person : leistungserbringer) {
            String name = (person.getFirstname() + " " + person.getLastname()).strip();
            if (!person.hatAbrechnungscode()) {
                fehlend.add(name + ": Abrechnungscode");
            }
            if (!person.hatSteuerangaben()) {
                fehlend.add(name + ": Steuernummer");
            }
        }
        return fehlend;
    }

    private static void pruefe(List<String> fehlend, String bezeichnung, String wert) {
        if (wert == null || wert.isBlank()) {
            fehlend.add(bezeichnung);
        }
    }

    private static void abschnitt(StringBuilder blatt, String ueberschrift) {
        blatt.append(System.lineSeparator()).append(ueberschrift).append(System.lineSeparator())
                .append("-".repeat(ueberschrift.length())).append(System.lineSeparator());
    }

    /**
     * Eine Zeile - und fuer einen fehlenden Wert eine Linie zum Eintragen.
     *
     * <p>Nicht weglassen und nicht leer lassen: Wer das Blatt vor sich hat,
     * soll sehen, <em>dass</em> dort etwas hingehoert. Ein weggelassenes Feld
     * faellt niemandem auf.</p>
     */
    private static void zeile(StringBuilder blatt, String bezeichnung, String wert) {
        String gefuellt = wert == null || wert.isBlank() ? "________________________" : wert.trim();
        blatt.append("   ").append(bezeichnung)
                .append(" ".repeat(Math.max(1, SPALTE - bezeichnung.length())))
                .append(gefuellt).append(System.lineSeparator());
    }

    private static String zusammen(String erstes, String zweites) {
        String links = erstes == null ? "" : erstes.trim();
        String rechts = zweites == null ? "" : zweites.trim();
        String beides = (links + " " + rechts).strip();
        return beides.isEmpty() ? null : beides;
    }
}
