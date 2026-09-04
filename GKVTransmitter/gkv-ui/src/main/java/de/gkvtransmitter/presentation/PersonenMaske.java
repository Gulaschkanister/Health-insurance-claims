package de.gkvtransmitter.presentation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.presentation.controller.EditFormController;
import de.gkvtransmitter.presentation.dialog.Dialoge;
import de.gkvtransmitter.presentation.populator.PatientFieldPopulator;
import de.gkvtransmitter.presentation.populator.ServiceProviderFieldPopulator;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
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

    /** Woher die Feldbeschreibungen einer Person stammen. */
    private static final String TAGS = "/tags/person-tags.json";

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Dialoge dialoge;
    private final DataRepository datenbank;
    private final Maskenrahmen rahmen;
    private final Feldbau feldbau;
    private final PatientFieldPopulator teilnehmerFelder;
    private final ServiceProviderFieldPopulator dienstleisterFelder;

    public PersonenMaske(UiFactory bausteine, AppMessages texte, Dialoge dialoge, DataRepository datenbank,
            Maskenrahmen rahmen, Feldbau feldbau, PatientFieldPopulator teilnehmerFelder,
            ServiceProviderFieldPopulator dienstleisterFelder) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.dialoge = Objects.requireNonNull(dialoge, "dialoge must not be null");
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

    // --- Bearbeiten ------------------------------------------------------

    /** Laesst einen Teilnehmer auswaehlen und zeigt ihn zum Bearbeiten. */
    public void teilnehmerBearbeiten() {
        waehle(datenbank.getAllPatients(), teilnehmerFelder::getDisplayName,
                texte.get("menu.patient"), texte.get("label.selectPatient"), texte.get("msg.noPatients"))
                .ifPresent(this::zeigeTeilnehmerform);
    }

    /** Laesst einen Dienstleister auswaehlen und zeigt ihn zum Bearbeiten. */
    public void dienstleisterBearbeiten() {
        waehle(datenbank.getAllServiceProviders(), dienstleisterFelder::getDisplayName,
                texte.get("menu.self"), texte.get("label.selectServiceProvider"),
                texte.get("msg.noServiceProviders"))
                .ifPresent(this::zeigeDienstleisterform);
    }

    private void zeigeTeilnehmerform(Patient teilnehmer) {
        EditFormController<Patient> steuerung = new EditFormController<>(bausteine, texte, teilnehmerFelder,
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
        EditFormController<ServiceProvider> steuerung = new EditFormController<>(bausteine, texte,
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

    /** Laesst einen Teilnehmer auswaehlen und loescht ihn nach Rueckfrage. */
    public void teilnehmerLoeschen() {
        loesche(datenbank.getAllPatients(), teilnehmerFelder::getDisplayName, datenbank::deletePatient,
                texte.get("label.selectPatient"), texte.get("msg.noPatients"),
                texte.get("msg.patientDeleted"));
    }

    /** Laesst einen Dienstleister auswaehlen und loescht ihn nach Rueckfrage. */
    public void dienstleisterLoeschen() {
        loesche(datenbank.getAllServiceProviders(), dienstleisterFelder::getDisplayName,
                datenbank::deleteServiceProvider, texte.get("label.selectServiceProvider"),
                texte.get("msg.noServiceProviders"), texte.get("msg.selfDeleted"));
    }

    private <T> void loesche(List<T> bestand, Function<T, String> anzeige, Consumer<T> loeschen,
            String auswahltext, String wennLeer, String wennGeloescht) {
        Optional<T> gewaehlt = waehle(bestand, anzeige, texte.get("msg.deleteConfirmTitle"),
                auswahltext, wennLeer);
        if (gewaehlt.isEmpty()) {
            return;
        }
        T eintrag = gewaehlt.get();
        boolean zugestimmt = dialoge.bestaetige(texte.get("msg.deleteConfirmTitle"),
                texte.get("msg.deleteConfirmHeader"),
                String.format(texte.get("msg.deleteConfirmBody"), anzeige.apply(eintrag)));
        if (!zugestimmt) {
            return;
        }
        try {
            loeschen.accept(eintrag);
            dialoge.zeigeInfo(texte.get("dialog.info.title"), wennGeloescht);
        } catch (RuntimeException e) {
            dialoge.zeigeFehler(texte.get("dialog.error.title"), e.getMessage());
        }
    }

    /** Laesst einen Eintrag auswaehlen und meldet, wenn es gar keinen gibt. */
    private <T> Optional<T> waehle(List<T> bestand, Function<T, String> anzeige, String titel,
            String text, String wennLeer) {
        if (bestand == null || bestand.isEmpty()) {
            dialoge.zeigeInfo(texte.get("dialog.info.title"), wennLeer);
            return Optional.empty();
        }
        return dialoge.waehleAus(titel, text, bestand, anzeige);
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
        speichern.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        speichern.setOnAction(ereignis -> speichere(felder, alsDienstleister));

        Button abbrechen = bausteine.createButton(texte.get("button.cancel"));
        abbrechen.setId(ID_ABBRECHEN);
        abbrechen.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        abbrechen.setOnAction(ereignis -> rahmen.leeren());

        HBox schaltflaechen = new HBox(10, speichern, abbrechen);
        schaltflaechen.setPadding(new Insets(10));

        Label ueberschrift = bausteine.createLabel(ueberschriftText);
        ueberschrift.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        VBox wurzel = new VBox(10);
        wurzel.setPadding(new Insets(20));
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
            dialoge.zeigeFehler(texte.get("dialog.error.title"), texte.get("msg.invalidNumbers"));
            return;
        }

        LocalDate geburtstag = null;
        if (felder.get("birthDate") instanceof DatePicker kalender) {
            geburtstag = kalender.getValue();
        }

        try {
            if (alsDienstleister) {
                datenbank.saveServiceProvider(new ServiceProvider(vorname, nachname, strasse, land,
                        hausnummer, plz, ik, kassenIk, geburtstag));
            } else {
                datenbank.savePatient(new Patient(vorname, nachname, strasse, land,
                        hausnummer, plz, ik, kassenIk, geburtstag));
            }
        } catch (RuntimeException e) {
            dialoge.zeigeFehler(texte.get("dialog.error.title"), e.getMessage());
            return;
        }

        dialoge.zeigeInfo(texte.get("dialog.info.title"),
                texte.get(alsDienstleister ? "msg.selfCreated" : "msg.patientCreated"));
        rahmen.leeren();
    }

    private String text(Map<String, Node> felder, String feldname) {
        return feldbau.textVon(felder.get(feldname));
    }

    private TagList feldbeschreibung(String feldname) {
        return TagConfigLoader.loadTagConfig(TAGS).get(feldname);
    }
}
