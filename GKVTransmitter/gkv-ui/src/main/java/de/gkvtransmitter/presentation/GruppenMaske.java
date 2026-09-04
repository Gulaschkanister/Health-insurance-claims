package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.Person;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Anlegen, Bearbeiten und Loeschen von Gruppen.
 *
 * <p>Eine Gruppe fasst zusammen, wer gemeinsam abgerechnet wird: die
 * Teilnehmer und den Dienstleister, der die Leistung erbracht hat. Sie ist
 * damit die Voraussetzung fuer jeden Abrechnungslauf.</p>
 *
 * <p>Herausgeloest aus {@code View}. Was von aussen kommt, kommt ueber den
 * Konstruktor: Daten, Meldungen und der Platz, an dem die Maske erscheint.</p>
 */
public class GruppenMaske {

    /** Kennung des Eingabefelds fuer den Gruppennamen. */
    public static final String ID_NAME = "gruppe-name";
    /** Kennung der Schaltflaeche zum Speichern. */
    public static final String ID_SPEICHERN = "gruppe-speichern";
    /** Kennung der Schaltflaeche zum Abbrechen. */
    public static final String ID_ABBRECHEN = "gruppe-abbrechen";
    /** Vorsatz der Auswahlkaestchen je Teilnehmer, gefolgt von dessen Kennnummer. */
    public static final String ID_TEILNEHMER = "gruppe-teilnehmer-";
    /** Vorsatz der Auswahlkaestchen je Dienstleister, gefolgt von dessen Kennnummer. */
    public static final String ID_DIENSTLEISTER = "gruppe-dienstleister-";
    /** Kennung der Schaltflaeche, die ein leeres Formular oeffnet. */
    public static final String ID_NEU = "gruppe-neu";
    /** Kennungsvorsatz der Gruppenliste. */
    public static final String KENNUNG = "gruppe";
    /** Nachsatz der Kennung einer Bearbeiten-Schaltflaeche. */
    public static final String AKTION_BEARBEITEN = "bearbeiten";
    /** Nachsatz der Kennung einer Loeschen-Schaltflaeche. */
    public static final String AKTION_LOESCHEN = "loeschen";

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final DataRepository datenbank;
    private final Maskenrahmen rahmen;

    public GruppenMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
            DataRepository datenbank, Maskenrahmen rahmen) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
        this.rahmen = Objects.requireNonNull(rahmen, "rahmen must not be null");
    }

    /** Zeigt ein leeres Formular fuer eine neue Gruppe. */
    public void neu() {
        rahmen.zeige(formular(null));
    }

    /**
     * Die Uebersicht aller Gruppen, mit Suchfeld und Schaltflaechen je Zeile.
     *
     * <p>Sie zeigt nebenbei, wie viele Teilnehmer und Dienstleister eine
     * Gruppe hat. Das war zuvor nur zu erfahren, indem man sie oeffnete - und
     * eine Gruppe ohne Dienstleister laesst sich gar nicht abrechnen.</p>
     */
    public Region liste() {
        return Maskenkopf.mitListe(bausteine, texte.get("title.group.new"), ID_NEU, this::neu,
                new Listenbau<PersonGroup>(bausteine, KENNUNG)
                        .spalten(texte.get("label.groupName"), texte.get("label.groupPatients"),
                                texte.get("label.groupServiceProviders"))
                        .zellen(gruppe -> List.of(
                                gruppe.getName() == null ? "" : gruppe.getName(),
                                String.valueOf(anzahl(gruppe.getPatients())),
                                String.valueOf(anzahl(gruppe.getServiceProviders()))))
                        .kennnummer(gruppe -> String.valueOf(gruppe.getId()))
                        .durchsuchbar(texte.get("label.searchGroup"),
                                gruppe -> gruppe.getName() == null ? "" : gruppe.getName())
                        .hinweisWennLeer(texte.get("msg.noGroups"))
                        .aktion(texte.get("menu.edit"), AKTION_BEARBEITEN, "schaltflaeche-still",
                                gruppe -> rahmen.zeige(formular(gruppe)))
                        .aktion(texte.get("menu.delete"), AKTION_LOESCHEN, "schaltflaeche-gefahr",
                                this::frageUndLoesche)
                        .baue(datenbank.getAllPersonGroups()));
    }

    private static int anzahl(Set<?> mitglieder) {
        return mitglieder == null ? 0 : mitglieder.size();
    }

    /**
     * Fragt zurueck, ehe geloescht wird.
     *
     * <p>Auf der Schaltflaeche steht "Loeschen", nicht "Ja". Wer nach einem
     * Moment Ablenkung auf die Frage zurueckkommt, liest sonst nur noch die
     * Antwortmoeglichkeiten und weiss nicht mehr, wozu.</p>
     */
    private void frageUndLoesche(PersonGroup gruppe) {
        meldungen.frageNach(String.format(texte.get("msg.deleteConfirmBody"), anzeigename(gruppe)),
                texte.get("button.delete"), () -> loesche(gruppe));
    }

    private void loesche(PersonGroup gruppe) {
        try {
            datenbank.deletePersonGroup(gruppe);
        } catch (RuntimeException e) {
            meldungen.fehler(e.getMessage());
            return;
        }
        meldungen.erfolg(texte.get("msg.groupDeleted"));
        rahmen.leeren();
    }

    /**
     * Baut das Formular auf.
     *
     * @param vorhandene die zu bearbeitende Gruppe, oder {@code null} fuer eine neue
     */
    Region formular(PersonGroup vorhandene) {
        PersonGroup gruppe = vorhandene != null ? vorhandene : new PersonGroup();
        boolean bearbeitet = vorhandene != null;

        List<Patient> patienten = sicher(datenbank.getAllPatients());
        List<ServiceProvider> dienstleister = sicher(datenbank.getAllServiceProviders());

        VBox wurzel = new VBox(14);
        wurzel.getStyleClass().add("maske");

        Label ueberschrift = bausteine.createLabel(
                texte.get(bearbeitet ? "title.group.edit" : "title.group.new"));
        ueberschrift.getStyleClass().add("masken-titel");

        TextField namensfeld = bausteine.createTextField();
        namensfeld.setId(ID_NAME);
        namensfeld.setPrefWidth(400);
        namensfeld.setText(gruppe.getName() != null ? gruppe.getName() : "");

        List<CheckBox> teilnehmerKaestchen = new ArrayList<>();
        VBox teilnehmerBereich = kaestchenliste(texte.get("label.groupPatients"), patienten,
                texte.get("msg.noPatients"), ID_TEILNEHMER,
                person -> enthaelt(gruppe.getPatients(), person.getId()),
                GruppenMaske::personenname, teilnehmerKaestchen);

        List<CheckBox> dienstleisterKaestchen = new ArrayList<>();
        VBox dienstleisterBereich = kaestchenliste(texte.get("label.groupServiceProviders"), dienstleister,
                texte.get("msg.noServiceProviders"), ID_DIENSTLEISTER,
                person -> enthaelt(gruppe.getServiceProviders(), person.getId()),
                GruppenMaske::personenname, dienstleisterKaestchen);

        HBox namenszeile = new HBox(10, bausteine.createLabel(texte.get("label.groupName")), namensfeld);

        Button speichern = bausteine.createButton(texte.get("button.save"));
        speichern.setId(ID_SPEICHERN);
        speichern.getStyleClass().add("schaltflaeche-haupt");
        speichern.setOnAction(ereignis -> speichere(gruppe, bearbeitet, namensfeld,
                patienten, teilnehmerKaestchen, dienstleister, dienstleisterKaestchen));

        Button abbrechen = bausteine.createButton(texte.get("button.cancel"));
        abbrechen.setId(ID_ABBRECHEN);
        abbrechen.getStyleClass().add("schaltflaeche-still");
        abbrechen.setOnAction(ereignis -> rahmen.leeren());

        wurzel.getChildren().addAll(ueberschrift, namenszeile, teilnehmerBereich, dienstleisterBereich,
                new HBox(10, speichern, abbrechen));
        return wurzel;
    }

    private void speichere(PersonGroup gruppe, boolean bearbeitet, TextField namensfeld,
            List<Patient> patienten, List<CheckBox> teilnehmerKaestchen,
            List<ServiceProvider> dienstleister, List<CheckBox> dienstleisterKaestchen) {
        String name = namensfeld.getText() != null ? namensfeld.getText().trim() : "";
        if (name.isBlank()) {
            meldungen.hinweis(texte.get("msg.groupNameRequired"));
            return;
        }

        gruppe.setName(name);
        gruppe.setPatients(angehakte(patienten, teilnehmerKaestchen));
        gruppe.setServiceProviders(angehakte(dienstleister, dienstleisterKaestchen));

        try {
            datenbank.savePersonGroup(gruppe);
        } catch (RuntimeException e) {
            meldungen.fehler(e.getMessage());
            return;
        }
        meldungen.erfolg(texte.get(bearbeitet ? "msg.groupUpdated" : "msg.groupCreated"));
        rahmen.leeren();
    }

    /**
     * Baut eine beschriftete Liste von Auswahlkaestchen.
     *
     * <p>Die Kaestchen landen in {@code kaestchen}, in derselben Reihenfolge
     * wie {@code personen}. Auf diesem Gleichlauf beruht
     * {@link #angehakte(List, List)}.</p>
     */
    private <T extends Person> VBox kaestchenliste(String beschriftung,
            List<T> personen, String hinweisWennLeer, String kennungsvorsatz,
            Predicate<T> istGewaehlt,
            Function<T, String> anzeige, List<CheckBox> kaestchen) {
        VBox bereich = new VBox(6, bausteine.createLabel(beschriftung));
        if (personen.isEmpty()) {
            bereich.getChildren().add(bausteine.createLabel(hinweisWennLeer));
            return bereich;
        }
        for (T person : personen) {
            CheckBox kaestchenFuerPerson = bausteine.createCheckBox(anzeige.apply(person));
            kaestchenFuerPerson.setId(kennungsvorsatz + person.getId());
            kaestchenFuerPerson.setSelected(istGewaehlt.test(person));
            kaestchen.add(kaestchenFuerPerson);
            bereich.getChildren().add(kaestchenFuerPerson);
        }
        return bereich;
    }

    /**
     * Sammelt die angehakten Personen ein.
     *
     * <p>Bewusst ein {@code LinkedHashSet}: die Anzeigereihenfolge bleibt
     * damit erhalten, und die Abrechnungsmaske listet die Mitglieder spaeter
     * so auf, wie sie hier ausgewaehlt wurden.</p>
     */
    private <T> Set<T> angehakte(List<T> personen, List<CheckBox> kaestchen) {
        Set<T> gewaehlt = new LinkedHashSet<>();
        for (int i = 0; i < kaestchen.size(); i++) {
            if (kaestchen.get(i).isSelected()) {
                gewaehlt.add(personen.get(i));
            }
        }
        return gewaehlt;
    }

    private boolean enthaelt(Set<? extends Person> personen, int id) {
        if (personen == null) {
            return false;
        }
        return personen.stream().anyMatch(person -> person != null && person.getId() == id);
    }

    private String anzeigename(PersonGroup gruppe) {
        return gruppe.getName() + " (ID: " + gruppe.getId() + ")";
    }

    private static <T> List<T> sicher(List<T> liste) {
        return liste == null ? List.of() : liste;
    }

    /** Benennt eine Person so, wie sie in den Listen erscheint. */
    private static String personenname(Person person) {
        return person.getFirstname() + " " + person.getLastname() + " (ID: " + person.getId() + ")";
    }
}
