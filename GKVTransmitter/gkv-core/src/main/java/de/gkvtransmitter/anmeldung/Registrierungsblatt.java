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
import de.gkvtransmitter.util.Kerntexte;

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
        zeileAus(blatt, Kerntexte.get("blatt.titel"));
        zeileAus(blatt, Kerntexte.format("blatt.stand", stand.format(TAG)));
        blatt.append(System.lineSeparator());
        zeileAus(blatt, Kerntexte.get("blatt.zitat"));
        blatt.append(System.lineSeparator());
        zeileAus(blatt, Kerntexte.get("blatt.kein_antrag"));
    }

    /** Ein Textblock samt Zeilenumbruch am Ende. */
    private static void zeileAus(StringBuilder blatt, String text) {
        blatt.append(text).append(System.lineSeparator());
    }

    private void absender(StringBuilder blatt) {
        abschnitt(blatt, Kerntexte.get("blatt.absender"));
        if (betrieb == null) {
            blatt.append("   ").append(Kerntexte.get("blatt.absender.leer"))
                    .append(System.lineSeparator());
            return;
        }
        zeile(blatt, "blatt.feld.praxis", betrieb.getPraxisname());
        zeile(blatt, "blatt.feld.strasse", zusammen(betrieb.getStrasse(), betrieb.getHausnummer()));
        zeile(blatt, "blatt.feld.ort", zusammen(betrieb.getPlz(), betrieb.getOrt()));
        zeile(blatt, "blatt.feld.ik", betrieb.getIk());
        zeile(blatt, "blatt.feld.abrechnet", Kerntexte.get(betrieb.istSelbstabrechner()
                ? "blatt.wert.selbstabrechner" : "blatt.wert.abrechnungsstelle"));
        zeile(blatt, "blatt.feld.steuernummer", betrieb.getSteuernummer());
        zeile(blatt, "blatt.feld.ansprechpartner", betrieb.getAnsprechpartner());
        zeile(blatt, "blatt.feld.telefon", betrieb.getTelefon());
        zeile(blatt, "blatt.feld.email", betrieb.getEmail());
        zeile(blatt, "blatt.feld.zertifikat", betrieb.getZertifikatBis());
    }

    private void erbringer(StringBuilder blatt) {
        abschnitt(blatt, Kerntexte.get("blatt.erbringer"));
        if (leistungserbringer.isEmpty()) {
            blatt.append("   ").append(Kerntexte.get("blatt.erbringer.leer"))
                    .append(System.lineSeparator());
            return;
        }
        for (ServiceProvider person : leistungserbringer) {
            blatt.append(System.lineSeparator());
            zeile(blatt, "blatt.feld.name",
                    (person.getFirstname() + " " + person.getLastname()).strip());
            zeile(blatt, "blatt.feld.ik", String.format("%09d", Math.max(person.getIk(), 0)));
            zeile(blatt, "blatt.feld.abrechnungscode", abrechnungscode(person));
            zeile(blatt, "blatt.feld.steuernummer", person.getSteuernummer());
            zeile(blatt, "blatt.feld.umsatzsteuer", Kerntexte.get(person.istUmsatzsteuerbefreit()
                    ? "blatt.wert.ust_befreit" : "blatt.wert.ust_pflichtig"));
            zeile(blatt, "blatt.feld.ansprechpartner", person.getAnsprechpartner());
            zeile(blatt, "blatt.feld.telefon", person.getTelefon());
            zeile(blatt, "blatt.feld.email", person.getEmail());
        }
    }

    /** Der Code mit seinem Sammelgruppenschluessel - beides wird gefragt. */
    private static String abrechnungscode(ServiceProvider person) {
        if (!person.hatAbrechnungscode()) {
            return null;
        }
        String code = person.getAbrechnungscode().trim();
        return Kerntexte.format("blatt.wert.leistungsbereich", code,
                Leistungsbereich.zuAbrechnungscode(code));
    }

    private void verfahren(StringBuilder blatt) {
        abschnitt(blatt, Kerntexte.get("blatt.verfahren"));
        zeile(blatt, "blatt.feld.zeichensatz", DtaFactory.ZEICHENSATZ);
        zeile(blatt, "blatt.feld.nachrichtentypen",
                Kerntexte.format("blatt.wert.nachrichtentypen", DtaFactory.NACHRICHTENVERSION));
        zeile(blatt, "blatt.feld.richtlinie", Kerntexte.get("blatt.wert.richtlinie"));
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
        abschnitt(blatt, Kerntexte.get("blatt.luecken"));
        if (fehlend.isEmpty()) {
            blatt.append("   ").append(Kerntexte.get("blatt.luecken.keine"))
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
            fehlend.add(Kerntexte.get("blatt.fehlt.betriebsdaten"));
            return fehlend;
        }
        pruefe(fehlend, "blatt.fehlt.praxisname", betrieb.getPraxisname());
        pruefe(fehlend, "blatt.fehlt.ik", betrieb.getIk());
        pruefe(fehlend, "blatt.fehlt.steuernummer", betrieb.getSteuernummer());
        pruefe(fehlend, "blatt.fehlt.ansprechpartner", betrieb.getAnsprechpartner());
        pruefe(fehlend, "blatt.fehlt.telefon", betrieb.getTelefon());
        pruefe(fehlend, "blatt.fehlt.email", betrieb.getEmail());
        pruefe(fehlend, "blatt.fehlt.zertifikat", betrieb.getZertifikatBis());
        if (leistungserbringer.isEmpty()) {
            fehlend.add(Kerntexte.get("blatt.fehlt.erbringer"));
        }
        for (ServiceProvider person : leistungserbringer) {
            String name = (person.getFirstname() + " " + person.getLastname()).strip();
            if (!person.hatAbrechnungscode()) {
                fehlend.add(Kerntexte.format("blatt.fehlt.erbringer_code", name));
            }
            if (!person.hatSteuerangaben()) {
                fehlend.add(Kerntexte.format("blatt.fehlt.erbringer_steuer", name));
            }
        }
        return fehlend;
    }

    /** Traegt eine Luecke ein, wenn der Wert fehlt. */
    private static void pruefe(List<String> fehlend, String schluessel, String wert) {
        if (wert == null || wert.isBlank()) {
            fehlend.add(Kerntexte.get(schluessel));
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
    private static void zeile(StringBuilder blatt, String schluessel, String wert) {
        String bezeichnung = Kerntexte.get(schluessel);
        String gefuellt = wert == null || wert.isBlank() ? LINIE : wert.trim();
        blatt.append("   ").append(bezeichnung)
                .append(" ".repeat(Math.max(1, SPALTE - bezeichnung.length())))
                .append(gefuellt).append(System.lineSeparator());
    }

    /** Die Linie, auf die ein fehlender Wert von Hand einzutragen ist. */
    private static final String LINIE = "________________________";

    private static String zusammen(String erstes, String zweites) {
        String links = erstes == null ? "" : erstes.trim();
        String rechts = zweites == null ? "" : zweites.trim();
        String beides = (links + " " + rechts).strip();
        return beides.isEmpty() ? null : beides;
    }
}
