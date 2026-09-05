package de.gkvtransmitter.presentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    /** Kennung des Suchfelds ueber den Teilnehmern. */
    public static final String ID_TEILNEHMER_SUCHE = "gruppe-teilnehmer-suche";
    /** Kennung des Suchfelds ueber den Dienstleistern. */
    public static final String ID_DIENSTLEISTER_SUCHE = "gruppe-dienstleister-suche";
    /** Kennung der Zeile "x von y ausgewaehlt" ueber den Teilnehmern. */
    public static final String ID_TEILNEHMER_ZAEHLER = "gruppe-teilnehmer-zaehler";
    /** Kennung der Zeile "x von y ausgewaehlt" ueber den Dienstleistern. */
    public static final String ID_DIENSTLEISTER_ZAEHLER = "gruppe-dienstleister-zaehler";
    /** Kennung des Hinweises, wenn die Suche nichts findet (Teilnehmer). */
    public static final String ID_TEILNEHMER_LEER = "gruppe-teilnehmer-leer";
    /** Kennung des Hinweises, wenn die Suche nichts findet (Dienstleister). */
    public static final String ID_DIENSTLEISTER_LEER = "gruppe-dienstleister-leer";
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
        meldungen.erfolg(String.format(texte.get("msg.groupDeleted"), gruppe.getName()));
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

        Auswahlliste<Patient> teilnehmerauswahl = new Auswahlliste<>(
                texte.get("label.groupPatients"), patienten, texte.get("msg.noPatients"),
                ID_TEILNEHMER, ID_TEILNEHMER_SUCHE, ID_TEILNEHMER_ZAEHLER, ID_TEILNEHMER_LEER,
                person -> enthaelt(gruppe.getPatients(), person.getId()));

        Auswahlliste<ServiceProvider> dienstleisterauswahl = new Auswahlliste<>(
                texte.get("label.groupServiceProviders"), dienstleister,
                texte.get("msg.noServiceProviders"),
                ID_DIENSTLEISTER, ID_DIENSTLEISTER_SUCHE, ID_DIENSTLEISTER_ZAEHLER,
                ID_DIENSTLEISTER_LEER,
                person -> enthaelt(gruppe.getServiceProviders(), person.getId()));

        HBox namenszeile = new HBox(10, bausteine.createLabel(texte.get("label.groupName")), namensfeld);

        Button speichern = bausteine.createButton(texte.get("button.save"));
        speichern.setId(ID_SPEICHERN);
        speichern.getStyleClass().add("schaltflaeche-haupt");
        speichern.setOnAction(ereignis -> speichere(gruppe, bearbeitet, namensfeld,
                teilnehmerauswahl, dienstleisterauswahl));

        Button abbrechen = bausteine.createButton(texte.get("button.cancel"));
        abbrechen.setId(ID_ABBRECHEN);
        abbrechen.getStyleClass().add("schaltflaeche-still");
        abbrechen.setOnAction(ereignis -> rahmen.leeren());

        wurzel.getChildren().addAll(ueberschrift, namenszeile, teilnehmerauswahl.ansicht(),
                dienstleisterauswahl.ansicht(), new HBox(10, speichern, abbrechen));
        return wurzel;
    }

    private void speichere(PersonGroup gruppe, boolean bearbeitet, TextField namensfeld,
            Auswahlliste<Patient> teilnehmerauswahl, Auswahlliste<ServiceProvider> dienstleisterauswahl) {
        String name = namensfeld.getText() != null ? namensfeld.getText().trim() : "";
        if (name.isBlank()) {
            meldungen.hinweis(texte.get("msg.groupNameRequired"));
            return;
        }

        Set<Patient> gewaehlteTeilnehmer = teilnehmerauswahl.gewaehlte();
        Set<ServiceProvider> gewaehlteDienstleister = dienstleisterauswahl.gewaehlte();

        // Eine Gruppe ohne Teilnehmer oder ohne Dienstleister laesst sich nicht
        // abrechnen: der Lauf erzeugt keine einzige Nachricht beziehungsweise
        // hat kein Absender-IK. Das erst in der Abrechnungsmaske zu bemerken
        // heisst, bis dahin mit einer Gruppe gearbeitet zu haben, die keine ist.
        List<String> fehlt = new ArrayList<>();
        if (gewaehlteTeilnehmer.isEmpty()) {
            fehlt.add(texte.get("msg.groupNeedsPatients"));
        }
        if (gewaehlteDienstleister.isEmpty()) {
            fehlt.add(texte.get("msg.groupNeedsProvider"));
        }
        if (!fehlt.isEmpty()) {
            meldungen.fehler(texte.get("msg.notSaved") + "\n· " + String.join("\n· ", fehlt));
            return;
        }

        gruppe.setName(name);
        gruppe.setPatients(gewaehlteTeilnehmer);
        gruppe.setServiceProviders(gewaehlteDienstleister);

        try {
            datenbank.savePersonGroup(gruppe);
        } catch (RuntimeException e) {
            meldungen.fehler(e.getMessage());
            return;
        }
        meldungen.erfolg(String.format(texte.get(bearbeitet ? "msg.groupUpdated" : "msg.groupCreated"), name));
        rahmen.leeren();
    }

    /**
     * Eine beschriftete Liste von Auswahlkaestchen mit Suche und Zaehler.
     *
     * <p>Zuvor waren das zwei parallele Listen - die Personen und ihre
     * Kaestchen -, und das Einsammeln der Auswahl lief ueber den gemeinsamen
     * Index. Dieser Gleichlauf war eine stille Bedingung: haette jemand die
     * Personen sortiert oder gefiltert, ohne die Kaestchen mitzuziehen, waeren
     * die falschen Personen in der Gruppe gelandet, ohne dass irgendetwas
     * fehlschlaegt. Die Zuordnung liegt jetzt in einer Abbildung und kann nicht
     * mehr auseinanderlaufen - erst dadurch ist die Suche gefahrlos.</p>
     *
     * <p>Die Suche <b>blendet aus, statt zu entfernen</b>. Ein ausgeblendetes
     * Kaestchen behaelt seinen Haken; wer erst Anna sucht und anhakt und dann
     * Bea, verliert Anna nicht. Genau deshalb steht der Zaehler daneben: sonst
     * waere eine Auswahl zu sehen, die kleiner ist als sie ist.</p>
     */
    private final class Auswahlliste<T extends Person> {

        private final VBox bereich;
        private final Map<CheckBox, T> zuordnung = new LinkedHashMap<>();
        private final Label zaehler;
        private final Label ohneTreffer;
        private final int gesamt;

        private Auswahlliste(String beschriftung, List<T> personen, String hinweisWennLeer,
                String kennungsvorsatz, String kennungSuche, String kennungZaehler,
                String kennungLeer, Predicate<T> istGewaehlt) {
            this.gesamt = personen.size();
            this.zaehler = bausteine.createLabel("");
            this.zaehler.setId(kennungZaehler);
            this.zaehler.getStyleClass().add("feld-hinweis");
            this.ohneTreffer = bausteine.createLabel("");
            this.ohneTreffer.setId(kennungLeer);
            this.ohneTreffer.getStyleClass().add("feld-hinweis");
            this.ohneTreffer.setVisible(false);
            this.ohneTreffer.setManaged(false);

            Label ueberschrift = bausteine.createLabel(beschriftung);
            this.bereich = new VBox(6, ueberschrift);

            if (personen.isEmpty()) {
                bereich.getChildren().add(bausteine.createLabel(hinweisWennLeer));
                return;
            }

            VBox kaestchenbereich = new VBox(4);
            for (T person : personen) {
                CheckBox kaestchen = bausteine.createCheckBox(personenname(person));
                kaestchen.setId(kennungsvorsatz + person.getId());
                kaestchen.setSelected(istGewaehlt.test(person));
                kaestchen.selectedProperty().addListener((wert, vorher, nachher) -> zaehleNeu());
                zuordnung.put(kaestchen, person);
                kaestchenbereich.getChildren().add(kaestchen);
            }

            TextField suche = bausteine.createTextField();
            suche.setId(kennungSuche);
            suche.setPromptText(texte.get("label.searchMember"));
            suche.setPrefWidth(300);
            suche.textProperty().addListener((wert, vorher, nachher) -> filtere(nachher));

            bereich.getChildren().addAll(new HBox(12, suche, zaehler), ohneTreffer, kaestchenbereich);
            zaehleNeu();
        }

        /**
         * Blendet aus, was nicht passt.
         *
         * <p>{@code setManaged(false)} muss mitlaufen: ohne das bliebe die
         * Luecke stehen, wo das Kaestchen war, und die Liste waere nach der
         * Suche genauso lang wie vorher.</p>
         */
        private void filtere(String suchbegriff) {
            String gesucht = suchbegriff == null ? "" : suchbegriff.trim().toLowerCase(Locale.GERMAN);
            int sichtbar = 0;
            for (Map.Entry<CheckBox, T> eintrag : zuordnung.entrySet()) {
                boolean passt = gesucht.isEmpty()
                        || personenname(eintrag.getValue()).toLowerCase(Locale.GERMAN).contains(gesucht);
                eintrag.getKey().setVisible(passt);
                eintrag.getKey().setManaged(passt);
                if (passt) {
                    sichtbar++;
                }
            }
            boolean leer = sichtbar == 0;
            ohneTreffer.setText(leer ? String.format(texte.get("msg.noMatch"), gesucht) : "");
            ohneTreffer.setVisible(leer);
            ohneTreffer.setManaged(leer);
        }

        private void zaehleNeu() {
            zaehler.setText(String.format(texte.get("label.selectedCount"), gewaehlte().size(), gesamt));
        }

        private Region ansicht() {
            return bereich;
        }

        /**
         * Die angehakten Personen.
         *
         * <p>Bewusst ein {@code LinkedHashSet}: die Anzeigereihenfolge bleibt
         * erhalten, und die Abrechnungsmaske listet die Mitglieder spaeter so
         * auf, wie sie hier stehen. Ausgeblendete Kaestchen zaehlen mit - ein
         * Suchbegriff im Feld darf die Gruppe nicht beschneiden.</p>
         */
        private Set<T> gewaehlte() {
            Set<T> gewaehlt = new LinkedHashSet<>();
            zuordnung.forEach((kaestchen, person) -> {
                if (kaestchen.isSelected()) {
                    gewaehlt.add(person);
                }
            });
            return gewaehlt;
        }
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
