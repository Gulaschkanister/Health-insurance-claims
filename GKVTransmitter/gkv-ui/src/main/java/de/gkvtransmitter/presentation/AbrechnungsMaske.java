package de.gkvtransmitter.presentation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.gkvtransmitter.dispatch.DispatchBatch;
import de.gkvtransmitter.dispatch.DtaValidierungsException;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Die Maske, in der eine Abrechnung zusammengestellt und angestossen wird.
 *
 * <p>Herausgeloest aus {@code View}, wo sie als eine von fuenf Aufgaben in
 * einer Klasse mit rund 1400 Zeilen lag. Der Zuschnitt folgt der Frage, was
 * die Maske von aussen braucht: Daten, einen Abrechnungslauf, einen Zielordner
 * und einen Weg, Meldungen loszuwerden. Alle vier sind Konstruktorwerte, damit
 * sich das Verhalten pruefen laesst - ohne Datenbank und ohne Dialogfenster.</p>
 *
 * <p>Die Bedienelemente tragen feste Kennungen ({@code abrechnung-start} und
 * so fort). Sie dienen dem Zugriff aus Tests und bleiben stabil, auch wenn
 * sich die Anordnung aendert.</p>
 */
public class AbrechnungsMaske {

    /** Kennung der Blaupausen-Auswahl. */
    public static final String ID_BLAUPAUSE = "abrechnung-blaupause";
    /** Kennung der Gruppen-Auswahl. */
    public static final String ID_GRUPPE = "abrechnung-gruppe";
    /** Kennung der Schaltflaeche, die den Lauf anstoesst. */
    public static final String ID_START = "abrechnung-start";
    /** Kennung des Bereichs mit den Teilnehmerlisten. */
    public static final String ID_TEILNEHMERLISTE = "abrechnung-teilnehmerliste";
    /** Vorsatz der Auswahlkaestchen je Teilnehmer, gefolgt von dessen Kennnummer. */
    public static final String ID_TEILNEHMER = "abrechnung-teilnehmer-";
    /** Vorsatz der Terminzaehler je Teilnehmer, gefolgt von dessen Kennnummer. */
    public static final String ID_TERMINE = "abrechnung-termine-";
    /** Kennung der Zeile, die den Dienstleister nennt. */
    public static final String ID_DIENSTLEISTER = "abrechnung-dienstleister";
    /** Kennung der Schaltflaeche, die alle Teilnehmer anhakt. */
    public static final String ID_ALLE = "abrechnung-alle";
    /** Kennung der Schaltflaeche, die alle Haken entfernt. */
    public static final String ID_KEINEN = "abrechnung-keinen";
    /** Kennung des Zaehlers fuer die Terminzahl aller Teilnehmer. */
    public static final String ID_TERMINE_ALLE = "abrechnung-termine-alle";
    /** Kennung der Schaltflaeche, die diese Terminzahl uebernimmt. */
    public static final String ID_TERMINE_SETZEN = "abrechnung-termine-setzen";
    /** Kennung der Zeile, die die Auswahl zusammenfasst. */
    public static final String ID_ZUSAMMENFASSUNG = "abrechnung-zusammenfassung";

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final DataRepository datenbank;
    private final Abrechnungslauf abrechnungslauf;
    private final Supplier<Path> versandordner;

    public AbrechnungsMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
            DataRepository datenbank, Abrechnungslauf abrechnungslauf, Supplier<Path> versandordner) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
        this.abrechnungslauf = Objects.requireNonNull(abrechnungslauf, "abrechnungslauf must not be null");
        this.versandordner = Objects.requireNonNull(versandordner, "versandordner must not be null");
    }

    /**
     * Baut die Maske auf.
     *
     * <p>Bewusst ohne umschliessendes {@code ScrollPane}: dessen Inhalt haengt
     * erst nach dem Aufbau der Darstellung im Knotenbaum, so dass eine Suche
     * nach den Bedienelementen ins Leere liefe. Das Scrollen ergaenzt der
     * Aufrufer.</p>
     */
    public Region erzeuge() {
        List<Blueprint> blaupausen = datenbank.getAllBlueprints();
        List<PersonGroup> gruppen = datenbank.getAllPersonGroups();

        VBox wurzel = new VBox(14);
        wurzel.getStyleClass().add("maske");

        Label blaupauseBeschriftung = beschriftung(texte.get("label.selectBlueprint"));
        ComboBox<Blueprint> blaupauseAuswahl = auswahlfeld(ID_BLAUPAUSE, blaupausen, Blueprint::getName);

        Label gruppeBeschriftung = beschriftung(texte.get("label.selectGroupForSettlement"));
        ComboBox<PersonGroup> gruppeAuswahl = auswahlfeld(ID_GRUPPE, gruppen, PersonGroup::getName);

        Label teilnehmerBeschriftung = beschriftung(texte.get("label.selectPatients"));

        Teilnehmerliste liste = new Teilnehmerliste();
        liste.baueAuf(gruppeAuswahl.getValue());
        // Am Wert haengen, nicht am Bedienereignis: entscheidend ist, welche
        // Gruppe gewaehlt ist, nicht auf welchem Weg sie gewaehlt wurde.
        gruppeAuswahl.valueProperty().addListener((wert, alt, neu) -> liste.baueAuf(neu));

        Button start = bausteine.createButton(texte.get("button.startSettlement"));
        start.setId(ID_START);
        start.getStyleClass().add("schaltflaeche-haupt");
        start.setOnAction(ereignis -> starteAbrechnung(blaupausen, blaupauseAuswahl, gruppeAuswahl, liste));

        wurzel.getChildren().addAll(blaupauseBeschriftung, blaupauseAuswahl,
                gruppeBeschriftung, gruppeAuswahl, teilnehmerBeschriftung, liste.bereich(), start);
        return wurzel;
    }

    /** Eine Beschriftung ueber einem Eingabefeld. */
    private Label beschriftung(String text) {
        Label label = bausteine.createLabel(text);
        label.getStyleClass().add("feld-beschriftung");
        return label;
    }

    /** Baut ein Auswahlfeld, das Eintraege ueber ihren Namen darstellt. */
    private <T> ComboBox<T> auswahlfeld(String kennung, List<T> eintraege, Function<T, String> name) {
        ComboBox<T> feld = new ComboBox<>();
        feld.setId(kennung);
        feld.setPrefWidth(400);
        if (eintraege != null && !eintraege.isEmpty()) {
            feld.getItems().addAll(eintraege);
            feld.setConverter(new StringConverter<T>() {
                @Override
                public String toString(T eintrag) {
                    return eintrag == null ? "" : name.apply(eintrag);
                }

                @Override
                public T fromString(String text) {
                    return null;
                }
            });
        }
        // Keine Vorauswahl: die Wahl soll bewusst getroffen werden.
        return feld;
    }

    /**
     * Prueft die Voraussetzungen und stoesst den Lauf an.
     *
     * <p>Die Reihenfolge ist die des bisherigen Verhaltens: erst ob ueberhaupt
     * Blaupausen vorliegen, dann ob eine Gruppe gewaehlt ist, dann ob
     * mindestens ein Teilnehmer angehakt wurde.</p>
     */
    private void starteAbrechnung(List<Blueprint> blaupausen, ComboBox<Blueprint> blaupauseAuswahl,
            ComboBox<PersonGroup> gruppeAuswahl, Teilnehmerliste liste) {
        if (blaupausen == null || blaupausen.isEmpty()) {
            meldungen.hinweis(texte.get("msg.noBlueprints"));
            return;
        }
        if (gruppeAuswahl.getValue() == null) {
            meldungen.hinweis(texte.get("msg.selectGroupRequired"));
            return;
        }

        List<Patient> gewaehlte = liste.gewaehlteTeilnehmer();
        if (gewaehlte.isEmpty()) {
            meldungen.hinweis(texte.get("msg.noParticipantsSelected"));
            return;
        }

        List<DispatchBatch> lieferungen;
        try {
            lieferungen = abrechnungslauf.starte(gewaehlte, gruppeAuswahl.getValue(),
                    blaupauseAuswahl.getValue(), liste.termine(), versandordner.get());
        } catch (DtaValidierungsException e) {
            // Beanstandungen vollstaendig anzeigen: die Anwenderin soll alle
            // auf einmal sehen und nicht nach jeder Korrektur neu anstossen.
            meldungen.pruefbericht(e.getBericht());
            return;
        } catch (RuntimeException e) {
            meldungen.fehler(texte.get("msg.dispatchFailed") + e.getMessage());
            return;
        }

        meldungen.erfolg(fasseZusammen(lieferungen));
    }

    /** Listet die erzeugten Dateien je Krankenkasse auf. */
    private String fasseZusammen(List<DispatchBatch> lieferungen) {
        StringBuilder text = new StringBuilder();
        text.append(String.format(texte.get("msg.batchesCreated"), lieferungen.size()))
                .append(System.lineSeparator());
        for (DispatchBatch lieferung : lieferungen) {
            text.append(texte.get("field.kassenIk")).append(": ").append(lieferung.getKassenIk())
                    .append(System.lineSeparator());
            for (Path datei : lieferung.getFiles()) {
                text.append(datei).append(System.lineSeparator());
            }
        }
        return text.toString();
    }

    /**
     * Die Teilnehmer der gewaehlten Gruppe, mit Auswahl und Terminzahl.
     *
     * <p>Auf diese Maske kommt es an, wenn Monat fuer Monat derselbe Kurs
     * abgerechnet wird: die Gruppe bleibt, wer da war und wie oft, aendert
     * sich. Deshalb steht ueber der Liste eine Werkzeugleiste - alle
     * auswaehlen, keinen, und eine Terminzahl fuer alle auf einmal setzen -
     * und darunter eine Zeile, die sagt, was gerade abgerechnet wuerde.</p>
     *
     * <p>Vorausgewaehlt wird bewusst niemand. Ein Haken zu viel bedeutet eine
     * Forderung an die Kasse fuer eine Leistung, die nicht erbracht wurde;
     * "Alle auswaehlen" ist ein Klick und dann eine bewusste Handlung.</p>
     */
    private final class Teilnehmerliste {

        /**
         * Eine Zeile der Tabelle.
         *
         * <p>Person, Kaestchen und Zaehler gehoeren zusammen. Zuvor lagen sie
         * in drei gleich langen Listen nebeneinander, und ihr Gleichlauf war
         * nur durch Hinsehen zu pruefen.</p>
         */
        private record Zeile(Patient person, CheckBox kaestchen, Spinner<Integer> termine) {
        }

        private final VBox bereich = new VBox(10);
        private final GridPane gitter = neuesGitter();
        private final Label zusammenfassung = bausteine.createLabel("");
        private final List<Zeile> zeilen = new ArrayList<>();

        private Teilnehmerliste() {
            bereich.setId(ID_TEILNEHMERLISTE);
            zusammenfassung.setId(ID_ZUSAMMENFASSUNG);
            zusammenfassung.getStyleClass().add("zeile-still");
        }

        private VBox bereich() {
            return bereich;
        }

        private void baueAuf(PersonGroup gruppe) {
            bereich.getChildren().clear();
            zeilen.clear();

            List<Patient> teilnehmer = mitglieder(gruppe == null ? null : gruppe.getPatients());
            List<ServiceProvider> dienstleister =
                    mitglieder(gruppe == null ? null : gruppe.getServiceProviders());

            if (teilnehmer.isEmpty() && dienstleister.isEmpty()) {
                // Zwei verschiedene Lagen, die frueher denselben Satz bekamen:
                // "Keine Patienten vorhanden" stand auch da, wenn nur noch
                // keine Gruppe gewaehlt war - und liess einen suchen, wo keine
                // fehlten.
                bereich.getChildren().add(bausteine.createLabel(texte.get(
                        gruppe == null ? "msg.selectGroupFirst" : "msg.emptyGroup")));
                return;
            }

            if (!dienstleister.isEmpty()) {
                bereich.getChildren().add(dienstleisterzeile(dienstleister));
            }
            if (teilnehmer.isEmpty()) {
                bereich.getChildren().add(bausteine.createLabel(texte.get("msg.emptyGroup")));
                return;
            }

            fuelle(teilnehmer);
            bereich.getChildren().addAll(werkzeugleiste(), gitter, zusammenfassung);
            aktualisiereZusammenfassung();
        }

        /**
         * Nennt den Dienstleister, ohne ihn zur Auswahl zu stellen.
         *
         * <p>Zuvor stand hier ein Auswahlkaestchen je Dienstleister. Es liess
         * sich anhaken, hatte aber keine Wirkung: der Dienstleister wird
         * ohnehin aus der Gruppe genommen, und zwar der erste. Eine Auswahl,
         * die nichts auswaehlt, ist irrefuehrender als gar keine.</p>
         */
        private Node dienstleisterzeile(List<ServiceProvider> dienstleister) {
            String namen = dienstleister.stream()
                    .map(person -> anzeigename(person.getFirstname(), person.getLastname()))
                    .collect(Collectors.joining(", "));
            Label zeile = bausteine.createLabel(texte.get("label.providers") + ": " + namen);
            zeile.setId(ID_DIENSTLEISTER);
            zeile.getStyleClass().add("zeile-still");
            return zeile;
        }

        /**
         * Alle auswaehlen, keinen, und eine Terminzahl fuer alle.
         *
         * <p>Der haeufige Fall im Monatsbetrieb ist "alle waren da, jeder
         * viermal". Ohne diese Leiste waeren das bei fuenfzehn Teilnehmern
         * dreissig Handgriffe.</p>
         */
        private Node werkzeugleiste() {
            Button alle = bausteine.createButton(texte.get("button.selectAll"));
            alle.setId(ID_ALLE);
            alle.getStyleClass().add("schaltflaeche-still");
            alle.setOnAction(ereignis -> haken(true));

            Button keinen = bausteine.createButton(texte.get("button.selectNone"));
            keinen.setId(ID_KEINEN);
            keinen.getStyleClass().add("schaltflaeche-still");
            keinen.setOnAction(ereignis -> haken(false));

            Spinner<Integer> fuerAlle = bausteine.createSpinner(Integer.class);
            fuerAlle.setId(ID_TERMINE_ALLE);
            fuerAlle.setPrefWidth(90);
            fuerAlle.getValueFactory().setValue(1);

            Button setzen = bausteine.createButton(texte.get("button.apply"));
            setzen.setId(ID_TERMINE_SETZEN);
            setzen.getStyleClass().add("schaltflaeche-still");
            setzen.setOnAction(ereignis -> termineFuerAlle(wert(fuerAlle)));

            HBox leiste = new HBox(8, alle, keinen,
                    bausteine.createLabel(texte.get("label.appointmentsForAll")), fuerAlle, setzen);
            leiste.setAlignment(Pos.CENTER_LEFT);
            return leiste;
        }

        private void fuelle(List<Patient> teilnehmer) {
            gitter.getChildren().clear();
            kopfzeile();
            int zeilennummer = 1;
            for (Patient person : teilnehmer) {
                CheckBox kaestchen = bausteine.createCheckBox("");
                kaestchen.setId(ID_TEILNEHMER + person.getId());
                kaestchen.selectedProperty().addListener(
                        (wert, alt, neu) -> aktualisiereZusammenfassung());

                Spinner<Integer> termine = bausteine.createSpinner(Integer.class);
                termine.setId(ID_TERMINE + person.getId());
                termine.setPrefWidth(100);
                termine.getValueFactory().setValue(1);
                termine.valueProperty().addListener((wert, alt, neu) -> aktualisiereZusammenfassung());

                gitter.add(kaestchen, 0, zeilennummer);
                gitter.add(bausteine.createLabel(
                        anzeigename(person.getFirstname(), person.getLastname())), 1, zeilennummer);
                gitter.add(bausteine.createLabel(String.valueOf(person.getKassenIk())), 2, zeilennummer);
                gitter.add(termine, 3, zeilennummer);

                zeilen.add(new Zeile(person, kaestchen, termine));
                zeilennummer++;
            }
        }

        private void kopfzeile() {
            gitter.add(kopf(texte.get("label.select")), 0, 0);
            gitter.add(kopf(texte.get("label.name")), 1, 0);
            gitter.add(kopf(texte.get("field.kassenIk")), 2, 0);
            gitter.add(kopf(texte.get("label.appointments")), 3, 0);
        }

        private Label kopf(String text) {
            Label ueberschrift = bausteine.createLabel(text);
            ueberschrift.getStyleClass().add("tabellenkopf");
            return ueberschrift;
        }

        /** Setzt bei allen Zeilen den Haken. */
        private void haken(boolean gesetzt) {
            zeilen.forEach(zeile -> zeile.kaestchen().setSelected(gesetzt));
        }

        /**
         * Setzt dieselbe Terminzahl bei allen angehakten Teilnehmern.
         *
         * <p>Ist niemand angehakt, gilt sie fuer alle - sonst muesste man erst
         * auswaehlen, um eine Zahl vorzugeben, die man ohnehin fuer alle
         * meint.</p>
         */
        private void termineFuerAlle(int anzahl) {
            boolean jemandGewaehlt = zeilen.stream().anyMatch(zeile -> zeile.kaestchen().isSelected());
            for (Zeile zeile : zeilen) {
                if (!jemandGewaehlt || zeile.kaestchen().isSelected()) {
                    zeile.termine().getValueFactory().setValue(anzahl);
                }
            }
        }

        private void aktualisiereZusammenfassung() {
            List<Zeile> gewaehlt = zeilen.stream().filter(zeile -> zeile.kaestchen().isSelected()).toList();
            int summe = gewaehlt.stream().mapToInt(zeile -> wert(zeile.termine())).sum();
            zusammenfassung.setText(String.format(texte.get("msg.selectionSummary"),
                    gewaehlt.size(), zeilen.size(), summe));
        }

        private List<Patient> gewaehlteTeilnehmer() {
            return zeilen.stream().filter(zeile -> zeile.kaestchen().isSelected())
                    .map(Zeile::person).toList();
        }

        /** Termine je gewaehltem Teilnehmer, in der Reihenfolge der Anzeige. */
        private Map<Integer, Integer> termine() {
            Map<Integer, Integer> je = new LinkedHashMap<>();
            for (Zeile zeile : zeilen) {
                if (zeile.kaestchen().isSelected()) {
                    je.put(zeile.person().getId(), wert(zeile.termine()));
                }
            }
            return je;
        }

        private int wert(Spinner<Integer> zaehler) {
            Integer anzahl = zaehler.getValue();
            return anzahl == null ? 0 : anzahl;
        }

        private String anzeigename(String vorname, String nachname) {
            return vorname + " " + nachname;
        }

        private <T> List<T> mitglieder(Set<T> menge) {
            return menge == null ? List.of() : new ArrayList<>(menge);
        }

        private GridPane neuesGitter() {
            GridPane gitter = new GridPane();
            gitter.setHgap(16);
            gitter.setVgap(6);
            return gitter;
        }
    }
}
