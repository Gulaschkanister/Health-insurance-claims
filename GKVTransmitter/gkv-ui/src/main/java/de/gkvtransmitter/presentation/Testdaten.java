package de.gkvtransmitter.presentation;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;

/**
 * Ein Satz Daten zum Ausprobieren.
 *
 * <p>Die vorige Fassung steckte in {@code View} und erzeugte Daten, mit denen
 * <b>keine</b> Abrechnung gelingen konnte: die Institutionskennzeichen lauteten
 * {@code 1001} und {@code 201} - ein IK hat neun Stellen mit Pruefziffer -,
 * Geburtsdaten fehlten ganz, und die Blaupause verwies auf eine Vorlage namens
 * {@code test-template}, die es nie gab; sie liess sich deshalb nicht einmal
 * oeffnen. Wer damit bis zur Abrechnung ging, bekam "Pruefung nicht bestanden"
 * und hatte bis dahin auf falscher Grundlage geuebt.</p>
 *
 * <p>Eigene Klasse, damit ein Test nachweisen kann, dass diese Daten die
 * Pruefung <em>bestehen</em>. Als private Methode in {@code View} war das nicht
 * moeglich - und genau deshalb fiel jahrelang niemandem auf, dass sie es nicht
 * taten.</p>
 */
public final class Testdaten {

    private Testdaten() {
    }

    // Nachgerechnete Kennzeichen, siehe Institutionskennzeichen.istGueltig.
    /** IK der Hebamme, die die Kurse leitet. */
    public static final int IK_HEBAMME_SANDER = 261914007;
    /** IK der vertretenden Hebamme. */
    public static final int IK_HEBAMME_VOGT = 261915019;
    /** Techniker Krankenkasse. */
    public static final int IK_TK = 102137985;
    /** BARMER. */
    public static final int IK_BARMER = 104940005;
    /** AOK Bremen/Bremerhaven. */
    public static final int IK_AOK_BREMEN = 103119199;
    /** hkk Erste Gesundheit. */
    public static final int IK_HKK = 103170002;

    /**
     * Die Dienstleisterinnen.
     *
     * <p>Zwei, damit die Auswahl eines Dienstleisters etwas zu waehlen hat und
     * nicht nur zu bestaetigen ist.</p>
     */
    public static List<ServiceProvider> dienstleister() {
        return List.of(
                new ServiceProvider("Martina", "Sander", "Lindenweg", "DE", "12",
                        28195, IK_HEBAMME_SANDER, IK_AOK_BREMEN, LocalDate.of(1979, 4, 22)),
                new ServiceProvider("Kerstin", "Vogt", "Am Deich", "DE", "3a",
                        28203, IK_HEBAMME_VOGT, IK_AOK_BREMEN, LocalDate.of(1985, 11, 8)));
    }

    /**
     * Die Teilnehmerinnen.
     *
     * <p>Bei vier verschiedenen Kassen: so verteilt ein Lauf auf mehrere
     * Zielordner statt nur auf einen, und die Zuordnung ueber die Kassen-IK
     * wird tatsaechlich benutzt.</p>
     */
    public static List<Patient> teilnehmerinnen() {
        return List.of(
                teilnehmerin("Anna", "Berger", "Wachtstrasse", "7", 28195, IK_TK, LocalDate.of(1994, 3, 12)),
                teilnehmerin("Bea", "Christiansen", "Osterdeich", "44", 28203, IK_TK, LocalDate.of(1990, 7, 30)),
                teilnehmerin("Carla", "Dohmen", "Schwachhauser Ring", "2", 28209, IK_BARMER, LocalDate.of(1988, 1, 5)),
                teilnehmerin("Dilara", "Erdem", "Vor dem Steintor", "19", 28203, IK_BARMER, LocalDate.of(1996, 9, 21)),
                teilnehmerin("Eva", "Freytag", "Am Wall", "101", 28195, IK_AOK_BREMEN, LocalDate.of(1992, 12, 2)),
                teilnehmerin("Frauke", "Groth", "Contrescarpe", "8", 28195, IK_HKK, LocalDate.of(1986, 5, 17)));
    }

    /**
     * Zwei Gruppen: ein voller Kurs und ein kleiner.
     *
     * @param teilnehmerinnen aus {@link #teilnehmerinnen()}, mindestens sechs
     * @param dienstleister   aus {@link #dienstleister()}, mindestens zwei
     */
    public static List<PersonGroup> gruppen(List<Patient> teilnehmerinnen,
            List<ServiceProvider> dienstleister) {
        return List.of(
                gruppe("Geburtsvorbereitung Januar", teilnehmerinnen.subList(0, 4), dienstleister.get(0)),
                gruppe("Geburtsvorbereitung Februar (klein)", teilnehmerinnen.subList(4, 6),
                        dienstleister.get(1)));
    }

    /**
     * Blaupausen zu den vorhandenen Nachrichtenvorlagen.
     *
     * <p>Zwei auf der ersten Vorlage, mit <b>verschiedenen Preisen</b> - eine
     * einzige verriete nicht, ob der Betrag ueberhaupt gelesen wird oder ob
     * still die Vorbelegung von 15.000,00 greift. Dazu je eine auf jeder
     * weiteren Vorlage, damit auch der zweite Kurs einen Fall hat.</p>
     *
     * @param vorlagen Namen der Nachrichtenvorlagen, mindestens eine
     */
    public static List<Blueprint> blaupausen(List<String> vorlagen) {
        if (vorlagen.isEmpty()) {
            return List.of();
        }
        List<Blueprint> alle = new ArrayList<>(List.of(
                blaupause("Kurs Vormittag", vorlagen.get(0), "12,50"),
                blaupause("Kurs Abend", vorlagen.get(0), "14,00")));
        for (int i = 1; i < vorlagen.size(); i++) {
            alle.add(blaupause(kursname(vorlagen.get(i)), vorlagen.get(i), "13,25"));
        }
        return List.copyOf(alle);
    }

    /** Der Kursname ohne den Zusatz zur Abrechnungsart. */
    private static String kursname(String vorlage) {
        int komma = vorlage.indexOf(',');
        return komma < 0 ? vorlage : vorlage.substring(0, komma);
    }

    private static Patient teilnehmerin(String vorname, String nachname, String strasse, String hausnummer,
            int plz, int kassenIk, LocalDate geboren) {
        // Das eigene IK einer versicherten Person ist das ihrer Kasse: sie ist
        // keine Leistungserbringerin. Ein erfundener zweiter Wert stuende sonst
        // im FKT und wuerde beanstandet.
        return new Patient(vorname, nachname, strasse, "DE", hausnummer, plz, kassenIk, kassenIk, geboren);
    }

    private static PersonGroup gruppe(String name, List<Patient> teilnehmerinnen, ServiceProvider leiterin) {
        PersonGroup gruppe = new PersonGroup();
        gruppe.setName(name);
        gruppe.setPatients(new LinkedHashSet<>(teilnehmerinnen));
        gruppe.setServiceProviders(new LinkedHashSet<>(List.of(leiterin)));
        return gruppe;
    }

    private static Blueprint blaupause(String name, String vorlage, String preis) {
        String inhalt = "{\"template\":\"" + vorlage + "\",\"fields\":{"
                + "\"" + BlaupausenMaske.FELD_EINZELBETRAG + "\":\"" + preis + "\","
                + "\"Abrechnungscode\":\"61\",\"Tarifkennzeichen\":\"00000\","
                + "\"Abrechnungspositionsnummer\":\"306050601\","
                + "\"Umsatzsteuersatz\":\"\",\"Zuzahlung pro Position\":\"0,00\"}}";
        return new Blueprint(name, vorlage, inhalt, OffsetDateTime.now());
    }
}
