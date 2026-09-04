package de.gkvtransmitter.presentation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.Person;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.presentation.controller.EditFormController;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.presentation.populator.PatientFieldPopulator;
import de.gkvtransmitter.presentation.populator.ServiceProviderFieldPopulator;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Anlegen, Bearbeiten und Loeschen von Teilnehmern und Dienstleistern.
 *
 * <p>Beide sind Personen und unterscheiden sich fachlich nur in ihrer Rolle:
 * der Dienstleister erbringt die Leistung, der Teilnehmer empfaengt sie. Sie
 * teilen sich deshalb Formular und Ablauf; was sich unterscheidet, sind die
 * Ueberschrift, die Meldung und wohin gespeichert wird.</p>
 *
 * <p>Herausgeloest aus {@code View}. Die Felder selbst baut {@link Feldbau}.</p>
 */
public class PersonenMaske {

    /** Kennung der Schaltflaeche zum Speichern. */
    public static final String ID_SPEICHERN = "person-speichern";
    /** Kennung der Schaltflaeche zum Abbrechen. */
    public static final String ID_ABBRECHEN = "person-abbrechen";
    /** Vorsatz der Eingabefelder, gefolgt vom Feldnamen aus der Tag-Datei. */
    public static final String ID_FELD = "person-feld-";
    /** Kennung der Schaltflaeche, die ein leeres Formular oeffnet. */
    public static final String ID_NEU = "person-neu";
    /** Kennungsvorsatz der Teilnehmerliste. */
    public static final String KENNUNG_TEILNEHMER = "teilnehmer";
    /** Kennungsvorsatz der Dienstleisterliste. */
    public static final String KENNUNG_DIENSTLEISTER = "dienstleister";
    /** Nachsatz der Kennung einer Bearbeiten-Schaltflaeche. */
    public static final String AKTION_BEARBEITEN = "bearbeiten";
    /** Nachsatz der Kennung einer Loeschen-Schaltflaeche. */
    public static final String AKTION_LOESCHEN = "loeschen";

    /** Woher die Feldbeschreibungen einer Person stammen. */
    private static final String TAGS = "/tags/person-tags.json";

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final DataRepository datenbank;
    private final Maskenrahmen rahmen;
    private final Feldbau feldbau;
    private final PatientFieldPopulator teilnehmerFelder;
    private final ServiceProviderFieldPopulator dienstleisterFelder;

    public PersonenMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen, DataRepository datenbank,
            Maskenrahmen rahmen, Feldbau feldbau, PatientFieldPopulator teilnehmerFelder,
            ServiceProviderFieldPopulator dienstleisterFelder) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
        this.rahmen = Objects.requireNonNull(rahmen, "rahmen must not be null");
        this.feldbau = Objects.requireNonNull(feldbau, "feldbau must not be null");
        this.teilnehmerFelder = Objects.requireNonNull(teilnehmerFelder, "teilnehmerFelder must not be null");
        this.dienstleisterFelder =
                Objects.requireNonNull(dienstleisterFelder, "dienstleisterFelder must not be null");
    }

    // --- Anlegen ---------------------------------------------------------

    /** Zeigt ein leeres Formular fuer einen neuen Teilnehmer. */
    public void neuerTeilnehmer() {
        rahmen.zeige(formular(texte.get("title.patient.new"), false));
    }

    /** Zeigt ein leeres Formular fuer einen neuen Dienstleister. */
    public void neuerDienstleister() {
        rahmen.zeige(formular(texte.get("title.self.new"), true));
    }

    // --- Liste -----------------------------------------------------------

    /**
     * Die Uebersicht aller Teilnehmer, mit Suchfeld und Schaltflaechen je
     * Zeile.
     *
     * <p>Sie ersetzt die frueheren Menuepunkte "Bearbeiten" und "Loeschen",
     * die jeweils erst eine Auswahlbox oeffneten. Man musste wissen, wen man
     * sucht; hier sieht man es.</p>
     */
    public Region teilnehmerliste() {
        return uebersicht(texte.get("menu.patient"), texte.get("title.patient.new"),
                this::neuerTeilnehmer,
                new Listenbau<Patient>(bausteine, KENNUNG_TEILNEHMER)
                        .spalten(texte.get("label.name"), texte.get("field.plz"),
                                texte.get("field.ik"), texte.get("field.kassenIk"))
                        .zellen(PersonenMaske::spaltenwerte)
                        .kennnummer(person -> String.valueOf(person.getId()))
                        .durchsuchbar(texte.get("label.searchPerson"), PersonenMaske::suchtext)
                        .hinweisWennLeer(texte.get("msg.noPatients"))
                        .aktion(texte.get("menu.edit"), AKTION_BEARBEITEN, "schaltflaeche-still",
                                this::zeigeTeilnehmerform)
                        .aktion(texte.get("menu.delete"), AKTION_LOESCHEN, "schaltflaeche-gefahr",
                                person -> frageUndLoesche(person, teilnehmerFelder.getDisplayName(person),
                                        datenbank::deletePatient, texte.get("msg.patientDeleted")))
                        .baue(datenbank.getAllPatients()));
    }

    /** Die Uebersicht aller Dienstleister. */
    public Region dienstleisterliste() {
        return uebersicht(texte.get("menu.self"), texte.get("title.self.new"),
                this::neuerDienstleister,
                new Listenbau<ServiceProvider>(bausteine, KENNUNG_DIENSTLEISTER)
                        .spalten(texte.get("label.name"), texte.get("field.plz"),
                                texte.get("field.ik"), texte.get("field.kassenIk"))
                        .zellen(PersonenMaske::spaltenwerte)
                        .kennnummer(person -> String.valueOf(person.getId()))
                        .durchsuchbar(texte.get("label.searchPerson"), PersonenMaske::suchtext)
                        .hinweisWennLeer(texte.get("msg.noServiceProviders"))
                        .aktion(texte.get("menu.edit"), AKTION_BEARBEITEN, "schaltflaeche-still",
                                this::zeigeDienstleisterform)
                        .aktion(texte.get("menu.delete"), AKTION_LOESCHEN, "schaltflaeche-gefahr",
                                person -> frageUndLoesche(person,
                                        dienstleisterFelder.getDisplayName(person),
                                        datenbank::deleteServiceProvider, texte.get("msg.selfDeleted")))
                        .baue(datenbank.getAllServiceProviders()));
    }

    private Region uebersicht(String ueberschriftText, String neuBeschriftung, Runnable neu, Region liste) {
        return Maskenkopf.mitListe(bausteine, neuBeschriftung, ID_NEU, neu, liste);
    }

    private static List<String> spaltenwerte(Person person) {
        return List.of(person.getFirstname() + " " + person.getLastname(),
                String.valueOf(person.getPlz()),
                String.valueOf(person.getIk()),
                String.valueOf(person.getKassenIk()));
    }

    private static String suchtext(Person person) {
        return person.getFirstname() + " " + person.getLastname() + " " + person.getPlz();
    }

    private void zeigeTeilnehmerform(Patient teilnehmer) {
        EditFormController<Patient> steuerung = new EditFormController<>(bausteine, texte, meldungen, teilnehmerFelder,
                () -> List.of(teilnehmer),
                datenbank::savePatient,
                datenbank::deletePatient,
                false,
                feldname -> feldbau.erzeugeFeld(feldname, feldbeschreibung(feldname)),
                "Patient",
                unbenutzt -> { },
                erneut -> rahmen.zeige(erneut.buildEditForm()));
        rahmen.zeige(steuerung.buildEditForm());
    }

    private void zeigeDienstleisterform(ServiceProvider dienstleister) {
        EditFormController<ServiceProvider> steuerung = new EditFormController<>(bausteine, texte, meldungen,
                dienstleisterFelder,
                () -> List.of(dienstleister),
                datenbank::saveServiceProvider,
                datenbank::deleteServiceProvider,
                false,
                feldname -> feldbau.erzeugeFeld(feldname, feldbeschreibung(feldname)),
                "ServiceProvider",
                unbenutzt -> { },
                erneut -> rahmen.zeige(erneut.buildEditForm()));
        rahmen.zeige(steuerung.buildEditForm());
    }

    // --- Loeschen --------------------------------------------------------

    /**
     * Fragt zurueck, ehe geloescht wird.
     *
     * <p>Auf der Schaltflaeche steht "Loeschen", nicht "Ja". Wer nach einem
     * Moment Ablenkung auf die Frage zurueckkommt, liest sonst nur noch die
     * Antwortmoeglichkeiten und weiss nicht mehr, wozu.</p>
     */
    private <T> void frageUndLoesche(T eintrag, String anzeigename, Consumer<T> loeschen,
            String wennGeloescht) {
        meldungen.frageNach(String.format(texte.get("msg.deleteConfirmBody"), anzeigename),
                texte.get("button.delete"), () -> loesche(eintrag, loeschen, wennGeloescht));
    }

    private <T> void loesche(T eintrag, Consumer<T> loeschen, String wennGeloescht) {
        try {
            loeschen.accept(eintrag);
        } catch (RuntimeException e) {
            meldungen.fehler(e.getMessage());
            return;
        }
        meldungen.erfolg(wennGeloescht);
        rahmen.leeren();
    }

    // --- Formular --------------------------------------------------------

    /**
     * Baut das Formular fuer eine neue Person.
     *
     * @param alsDienstleister ob ein Dienstleister statt eines Teilnehmers entsteht
     */
    Region formular(String ueberschriftText, boolean alsDienstleister) {
        Map<String, TagList> beschreibungen = TagConfigLoader.loadTagConfig(TAGS);
        Map<String, Node> felder = new LinkedHashMap<>();
        List<Node> zeilen = new ArrayList<>();

        for (Map.Entry<String, TagList> eintrag : beschreibungen.entrySet()) {
            String feldname = eintrag.getKey();
            Node feld = feldbau.erzeugeFeld(feldname, eintrag.getValue());
            feld.setId(ID_FELD + feldname);
            felder.put(feldname, feld);
            zeilen.add(bausteine.createBorderPane(
                    bausteine.createLabel(texte.get("field." + feldname)), feld, null, null, null));
        }

        Button speichern = bausteine.createButton(texte.get("button.save"));
        speichern.setId(ID_SPEICHERN);
        speichern.getStyleClass().add("schaltflaeche-haupt");
        speichern.setOnAction(ereignis -> speichere(felder, alsDienstleister));

        Button abbrechen = bausteine.createButton(texte.get("button.cancel"));
        abbrechen.setId(ID_ABBRECHEN);
        abbrechen.getStyleClass().add("schaltflaeche-still");
        abbrechen.setOnAction(ereignis -> rahmen.leeren());

        HBox schaltflaechen = new HBox(10, speichern, abbrechen);

        Label ueberschrift = bausteine.createLabel(ueberschriftText);
        ueberschrift.getStyleClass().add("masken-titel");

        VBox wurzel = new VBox(14);
        wurzel.getStyleClass().add("maske");
        GridPane gitter = bausteine.createGridPane(2, zeilen.toArray(Node[]::new));
        wurzel.getChildren().addAll(ueberschrift, gitter, schaltflaechen);
        return wurzel;
    }

    /**
     * Legt die Person aus den Eingaben an.
     *
     * <p>Die Meldung richtet sich nach der Rolle. Zuvor stand hier in beiden
     * Faellen "Teilnehmer erfolgreich erstellt!", auch wenn ein Dienstleister
     * angelegt wurde.</p>
     */
    private void speichere(Map<String, Node> felder, boolean alsDienstleister) {
        String vorname = text(felder, "firstname");
        String nachname = text(felder, "lastname");
        String strasse = text(felder, "street");
        String land = text(felder, "country");
        String hausnummer = text(felder, "housenumber");

        int plz;
        int ik;
        int kassenIk;
        try {
            plz = Integer.parseInt(text(felder, "plz"));
            ik = Integer.parseInt(text(felder, "ik"));
            kassenIk = Integer.parseInt(text(felder, "kassenIk"));
        } catch (NumberFormatException e) {
            meldungen.hinweis(texte.get("msg.invalidNumbers"));
            return;
        }

        LocalDate geburtstag = feldbau.datumVon(felder.get("birthDate"));

        try {
            if (alsDienstleister) {
                datenbank.saveServiceProvider(new ServiceProvider(vorname, nachname, strasse, land,
                        hausnummer, plz, ik, kassenIk, geburtstag));
            } else {
                datenbank.savePatient(new Patient(vorname, nachname, strasse, land,
                        hausnummer, plz, ik, kassenIk, geburtstag));
            }
        } catch (RuntimeException e) {
            meldungen.fehler(e.getMessage());
            return;
        }

        meldungen.erfolg(texte.get(alsDienstleister ? "msg.selfCreated" : "msg.patientCreated"));
        rahmen.leeren();
    }

    private String text(Map<String, Node> felder, String feldname) {
        return feldbau.textVon(felder.get(feldname));
    }

    private TagList feldbeschreibung(String feldname) {
        return TagConfigLoader.loadTagConfig(TAGS).get(feldname);
    }
}
