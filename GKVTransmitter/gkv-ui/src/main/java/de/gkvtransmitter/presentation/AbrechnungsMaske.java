package de.gkvtransmitter.presentation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import de.gkvtransmitter.dispatch.DispatchBatch;
import de.gkvtransmitter.dispatch.DtaValidierungsException;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
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
    /** Vorsatz der Auswahlkaestchen je Dienstleister, gefolgt von dessen Kennnummer. */
    public static final String ID_DIENSTLEISTER = "abrechnung-dienstleister-";

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

        VBox wurzel = new VBox(10);
        wurzel.setPadding(new Insets(20));

        Label ueberschrift = bausteine.createLabel(texte.get("menu.settlement"));

        Label blaupauseBeschriftung = bausteine.createLabel(texte.get("label.selectBlueprint"));
        ComboBox<Blueprint> blaupauseAuswahl = auswahlfeld(ID_BLAUPAUSE, blaupausen, Blueprint::getName);

        Label gruppeBeschriftung = bausteine.createLabel(texte.get("label.selectGroupForSettlement"));
        ComboBox<PersonGroup> gruppeAuswahl = auswahlfeld(ID_GRUPPE, gruppen, PersonGroup::getName);

        Label teilnehmerBeschriftung = bausteine.createLabel(texte.get("label.selectPatients"));

        Teilnehmerliste liste = new Teilnehmerliste();
        liste.baueAuf(gruppeAuswahl.getValue());
        // Am Wert haengen, nicht am Bedienereignis: entscheidend ist, welche
        // Gruppe gewaehlt ist, nicht auf welchem Weg sie gewaehlt wurde.
        gruppeAuswahl.valueProperty().addListener((wert, alt, neu) -> liste.baueAuf(neu));

        Button start = bausteine.createButton(texte.get("button.startSettlement"));
        start.setId(ID_START);
        start.setOnAction(ereignis -> starteAbrechnung(blaupausen, blaupauseAuswahl, gruppeAuswahl, liste));

        wurzel.getChildren().addAll(ueberschrift, blaupauseBeschriftung, blaupauseAuswahl,
                gruppeBeschriftung, gruppeAuswahl, teilnehmerBeschriftung, liste.bereich(), start);
        return wurzel;
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
     * Die beiden Tabellen mit den Mitgliedern der gewaehlten Gruppe.
     *
     * <p>Haelt Kaestchen, Zaehler und die zugehoerigen Personen in gleich
     * langen Listen zusammen. Zuvor lagen diese sechs Listen als lokale
     * Variablen in einem ueber zweihundert Zeilen langen Methodenrumpf
     * nebeneinander, und ihr Gleichlauf war nur durch Hinsehen zu pruefen.</p>
     */
    private final class Teilnehmerliste {

        private final VBox bereich = new VBox(8);
        private final GridPane dienstleisterGitter = gitter();
        private final GridPane teilnehmerGitter = gitter();

        private final List<CheckBox> teilnehmerKaestchen = new ArrayList<>();
        private final List<Spinner<Integer>> terminZaehler = new ArrayList<>();
        private final List<Patient> teilnehmer = new ArrayList<>();
        private final List<CheckBox> dienstleisterKaestchen = new ArrayList<>();
        private final List<ServiceProvider> dienstleister = new ArrayList<>();

        private Teilnehmerliste() {
            bereich.setId(ID_TEILNEHMERLISTE);
        }

        private VBox bereich() {
            return bereich;
        }

        private void baueAuf(PersonGroup gruppe) {
            bereich.getChildren().clear();
            teilnehmerKaestchen.clear();
            terminZaehler.clear();
            teilnehmer.clear();
            dienstleisterKaestchen.clear();
            dienstleister.clear();

            List<Patient> quellePatienten = new ArrayList<>();
            List<ServiceProvider> quelleDienstleister = new ArrayList<>();
            if (gruppe != null) {
                if (gruppe.getPatients() != null) {
                    quellePatienten.addAll(gruppe.getPatients());
                }
                if (gruppe.getServiceProviders() != null) {
                    quelleDienstleister.addAll(gruppe.getServiceProviders());
                }
            }

            fuelleDienstleister(quelleDienstleister);
            fuelleTeilnehmer(quellePatienten);

            if (dienstleister.isEmpty() && teilnehmer.isEmpty()) {
                bereich.getChildren().add(bausteine.createLabel(texte.get("msg.noPatients")));
                return;
            }
            if (!dienstleister.isEmpty()) {
                bereich.getChildren().addAll(bausteine.createLabel(texte.get("label.providers")),
                        dienstleisterGitter);
            }
            if (!teilnehmer.isEmpty()) {
                bereich.getChildren().addAll(bausteine.createLabel(texte.get("label.patients")),
                        teilnehmerGitter);
            }
        }

        private void fuelleDienstleister(List<ServiceProvider> quelle) {
            dienstleisterGitter.getChildren().clear();
            kopfzeile(dienstleisterGitter, false);
            int zeile = 1;
            for (ServiceProvider person : quelle) {
                CheckBox kaestchen = bausteine.createCheckBox("");
                kaestchen.setId(ID_DIENSTLEISTER + person.getId());
                dienstleisterGitter.add(kaestchen, 0, zeile);
                dienstleisterGitter.add(bausteine.createLabel(
                        anzeigename(person.getFirstname(), person.getLastname())), 1, zeile);
                dienstleisterGitter.add(bausteine.createLabel(String.valueOf(person.getId())), 2, zeile);
                dienstleisterKaestchen.add(kaestchen);
                dienstleister.add(person);
                zeile++;
            }
        }

        private void fuelleTeilnehmer(List<Patient> quelle) {
            teilnehmerGitter.getChildren().clear();
            kopfzeile(teilnehmerGitter, true);
            int zeile = 1;
            for (Patient person : quelle) {
                CheckBox kaestchen = bausteine.createCheckBox("");
                kaestchen.setId(ID_TEILNEHMER + person.getId());
                Spinner<Integer> zaehler = bausteine.createSpinner(Integer.class, null, InputOption.NUMBER);
                zaehler.setId(ID_TERMINE + person.getId());
                zaehler.setPrefWidth(100);
                zaehler.getValueFactory().setValue(1);
                teilnehmerGitter.add(kaestchen, 0, zeile);
                teilnehmerGitter.add(bausteine.createLabel(
                        anzeigename(person.getFirstname(), person.getLastname())), 1, zeile);
                teilnehmerGitter.add(bausteine.createLabel(String.valueOf(person.getId())), 2, zeile);
                teilnehmerGitter.add(zaehler, 3, zeile);
                teilnehmerKaestchen.add(kaestchen);
                terminZaehler.add(zaehler);
                teilnehmer.add(person);
                zeile++;
            }
        }

        private void kopfzeile(GridPane gitter, boolean mitTerminen) {
            gitter.add(bausteine.createLabel(texte.get("label.select")), 0, 0);
            gitter.add(bausteine.createLabel(texte.get("label.name")), 1, 0);
            gitter.add(bausteine.createLabel(texte.get("label.id")), 2, 0);
            if (mitTerminen) {
                gitter.add(bausteine.createLabel(texte.get("label.appointments")), 3, 0);
            }
        }

        private List<Patient> gewaehlteTeilnehmer() {
            List<Patient> gewaehlte = new ArrayList<>();
            for (int i = 0; i < teilnehmerKaestchen.size(); i++) {
                if (teilnehmerKaestchen.get(i).isSelected()) {
                    gewaehlte.add(teilnehmer.get(i));
                }
            }
            return gewaehlte;
        }

        /** Termine je gewaehltem Teilnehmer, in der Reihenfolge der Anzeige. */
        private Map<Integer, Integer> termine() {
            Map<Integer, Integer> je = new LinkedHashMap<>();
            for (int i = 0; i < teilnehmerKaestchen.size(); i++) {
                if (!teilnehmerKaestchen.get(i).isSelected()) {
                    continue;
                }
                Integer anzahl = terminZaehler.get(i).getValue();
                je.put(teilnehmer.get(i).getId(), anzahl != null ? anzahl : 0);
            }
            return je;
        }

        private String anzeigename(String vorname, String nachname) {
            return vorname + " " + nachname;
        }

        private GridPane gitter() {
            GridPane gitter = new GridPane();
            gitter.setHgap(8);
            gitter.setVgap(6);
            return gitter;
        }
    }
}
