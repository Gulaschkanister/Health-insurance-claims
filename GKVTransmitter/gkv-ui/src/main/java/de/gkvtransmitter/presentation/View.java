package de.gkvtransmitter.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.application.AbrechnungService;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.presentation.meldung.Bildschirmmeldungen;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.presentation.populator.PatientFieldPopulator;
import de.gkvtransmitter.presentation.populator.ServiceProviderFieldPopulator;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.Anwendungsverzeichnis;
import de.gkvtransmitter.util.AppMessages;
import javafx.application.Platform;
import javafx.scene.Scene;

/**
 * Der Einstieg in die Oberflaeche.
 *
 * <p>Baut die Hauptszene, fuellt die Seitenleiste und verteilt von dort auf
 * die Masken. Seit dem 05.09.2026 baut sie keine Maske mehr selbst; geblieben
 * sind Szene, Navigation und die Testdaten.</p>
 */
public class View {

    private final UiFactory componentFactory;
    private final Controller controller;
    private final AppMessages messages;
    private final ObjectMapper objectMapper;
    /** Die hinterlegten Abrechnungscodes, siehe {@link #ladeAbrechnungscodes()}. */
    private final List<String> abrechnungscodes;

    private final PatientFieldPopulator patientPopulator;
    private final ServiceProviderFieldPopulator serviceProviderPopulator;
    private final AbrechnungService abrechnungService;

    /** Die Meldungsecke oben rechts, siehe {@link Benachrichtigungen}. */
    private final Benachrichtigungen benachrichtigungen = new Benachrichtigungen();

    /** Alle Meldungen an den Anwender laufen hierueber, siehe {@link Meldungen}. */
    private final Meldungen meldungen = new Bildschirmmeldungen(benachrichtigungen);

    /** Baut die Eingabefelder der Formulare und liest sie wieder aus. */
    private final Feldbau feldbau;

    /** Seitenleiste, Maske, Statuszeile - und zugleich der Platz der Masken. */
    private final Hauptfenster hauptfenster = new Hauptfenster(benachrichtigungen.bereich());

    public View(Controller controller, AbrechnungService abrechnungService) {
        this.controller = controller;
        this.componentFactory = new JavaFxUiFactory();
        this.messages = new AppMessages("/messages/ui-messages.json");
        this.objectMapper = new ObjectMapper();
        this.abrechnungscodes = ladeAbrechnungscodes();
        this.feldbau = new Feldbau(this.componentFactory, this.messages);
        this.patientPopulator = new PatientFieldPopulator();
        this.serviceProviderPopulator = new ServiceProviderFieldPopulator();
        this.abrechnungService = java.util.Objects.requireNonNull(abrechnungService,
            "abrechnungService must not be null");
        // Erst hier und nicht am Feld: die Einstellungen kommen aus der
        // Datenbank, und die haelt der Controller.
        this.einstellungen = de.gkvtransmitter.einstellung.Einstellungen.aus(
                controller.getDatabase(), controller.getDatenbankOrt());
    }

    /** Das Stylesheet der Anwendung. Ohne es sieht alles nach JavaFX-Vorgabe aus. */
    static final String STYLESHEET = "/style/gkv.css";

    /** Die Einstellungen aus der Tabelle {@code einstellung} der Datenbank. */
    private final de.gkvtransmitter.einstellung.Einstellungen einstellungen;

    /**
     * Setzt die helle oder dunkle Fassung.
     *
     * <p>Es genuegt eine Stilklasse an der Wurzel: alle Farben stehen in
     * {@code gkv.css} als benannte Werte, und die dunkle Fassung besetzt
     * dieselben Namen anders. <b>Die Umschaltung wirkt sofort</b> - eine
     * Einstellung, die erst nach einem Neustart greift, sieht aus wie ein
     * kaputter Schalter.</p>
     */
    void setzeDarstellung(String wert) {
        if (hauptfenster.wurzel() == null) {
            return;
        }
        var klassen = hauptfenster.wurzel().getStyleClass();
        klassen.remove(EinstellungenMaske.DUNKEL);
        if (EinstellungenMaske.DUNKEL.equals(wert)) {
            klassen.add(EinstellungenMaske.DUNKEL);
        }
    }

    /**
     * Der Name, unter dem die Anwendung auftritt: Fenstertitel und Seitenleiste.
     *
     * <p>Hiess bis zum 06.09.2026 „GKVTransmitter". Simons Einwand traf zu:
     * <b>„Transmitter" beschrieb den einen Schritt, den das Programm noch gar
     * nicht wirklich kann.</b> Der Versand ist dateibasiert, ein echter
     * Uebermittlungsweg fehlt. Was es tatsaechlich tut, ist Stammdaten
     * verwalten, Kurse und Preise festhalten, daraus DTA erzeugen und pruefen.
     * Ein Name soll halten, was er verspricht.</p>
     *
     * <p>Der Java-Paketname {@code de.gkvtransmitter} bleibt vorerst - er ist
     * nach aussen unsichtbar, und ein Umbenennen waere ein breiter Eingriff
     * ohne Gewinn fuer denjenigen, der das Programm benutzt. Zum Datenpfad
     * siehe {@link de.gkvtransmitter.util.Anwendungsverzeichnis}.</p>
     */
    public static final String PROGRAMMNAME = "GKV-Abrechnung";

    /**
     * Erstellt die Hauptszene.
     *
     * <p>Der Aufbau ist zweischichtig: unten der Rahmen mit Menue, Maske und
     * Statuszeile, darueber die Meldungsecke. Sie liegt bewusst <em>ueber</em>
     * der Maske statt darin - so kann eine Meldung erscheinen, ohne dass sich
     * die Maske darunter verschiebt.</p>
     */
    public Scene createMainScene(double width, double height) {
        hauptfenster.beiBereichswechsel(benachrichtigungen::leeren);
        baueNavigation();
        hauptfenster.setzeMarke(PROGRAMMNAME, messages.get("app.subtitle"));
        setzeLadestatus();
        hauptfenster.oeffneErstenBereich();
        setzeDarstellung(einstellungen.get(de.gkvtransmitter.einstellung.Einstellung.DARSTELLUNG));

        Scene scene = componentFactory.createScene(hauptfenster.wurzel(), width, height);
        scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        Platform.runLater(this::seedIfEmpty);
        return scene;
    }

    /**
     * Der Fenstertitel, der dem offenen Bereich folgt.
     *
     * <p>Bis zum 05.09.2026 stand in der Titelleiste immer nur
     * "GKVTransmitter". In der Taskleiste und beim Umschalten zwischen
     * Fenstern sagte das nichts darueber, wo man gerade ist.</p>
     */
    public javafx.beans.property.ReadOnlyStringProperty fenstertitel() {
        return hauptfenster.fenstertitel();
    }

    /**
     * Schreibt in die Statuszeile, wie viele Vorlagen geladen sind.
     *
     * <p>Dort stand bis zum 05.09.2026 eine Aufzaehlung der JSON-Dateinamen
     * samt Nachrichtentypen, gebaut in {@code App}, deren eigener Kommentar sie
     * "sichtbare Debug-Hilfe im UI" nannte. Sie war der laengste Text im
     * Programm und wurde entsprechend abgeschnitten. Wer abrechnet, hat mit
     * Dateinamen nichts zu schaffen.</p>
     *
     * <p>Die Namen sind weiterhin zu erfahren - ueber das Info-Zeichen daneben,
     * und zwar die <b>Anzeigenamen</b> der Vorlagen, nicht die der Dateien.</p>
     */
    private void setzeLadestatus() {
        List<String> vorlagennamen = List.copyOf(
                controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet());

        hauptfenster.setzeStatus(vorlagennamen.isEmpty()
                ? messages.get("status.noTemplates")
                : String.format(messages.get("status.templatesLoaded"), vorlagennamen.size()));

        // Ohne Vorlagen gibt es nichts aufzuzaehlen; dann verschwindet das
        // Zeichen, statt eine leere Liste anzubieten.
        hauptfenster.setzeStatusInfo(messages.get("status.info"),
                vorlagennamen.isEmpty() ? null
                        : () -> meldungen.hinweis(messages.get("status.templateList")
                                + "\n· " + String.join("\n· ", vorlagennamen)));
    }

    /**
     * Fuellt die Seitenleiste.
     *
     * <p>Die Reihenfolge ist die des Arbeitsablaufs: zuerst das, wozu das
     * Programm da ist, dann die Daten, die es dafuer braucht, dann die
     * Vorlagen. Zuvor standen alle Punkte gleichrangig nebeneinander in einer
     * Menueleiste.</p>
     */
    private void baueNavigation() {
        hauptfenster.ergaenzeAbschnitt(messages.get("nav.section.billing"));
        hauptfenster.ergaenzeBereich(messages.get("menu.settlement"), this::createAbrechnung);

        hauptfenster.ergaenzeAbschnitt(messages.get("nav.section.data"));
        hauptfenster.ergaenzeBereich(messages.get("menu.patient"),
                () -> hauptfenster.zeige(personenMaske().teilnehmerliste()));
        hauptfenster.ergaenzeBereich(messages.get("menu.self"),
                () -> hauptfenster.zeige(personenMaske().dienstleisterliste()));
        hauptfenster.ergaenzeBereich(messages.get("menu.groups"),
                () -> hauptfenster.zeige(gruppenMaske().liste()));

        hauptfenster.ergaenzeBereich(messages.get("menu.blueprints"),
                () -> hauptfenster.zeige(blaupausenMaske().liste()));

        // Die Vorlagen stehen weiter einzeln in der Leiste: sie sind der Weg zu
        // einer neuen Blaupause. Zuvor waren sie der einzige Weg ueberhaupt -
        // was einmal gespeichert war, tauchte nur noch als Name im Auswahlfeld
        // der Abrechnung auf.
        Set<String> vorlagen = controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet();
        if (!vorlagen.isEmpty()) {
            hauptfenster.ergaenzeAbschnitt(messages.get("nav.section.templates"));
            for (String name : vorlagen) {
                hauptfenster.ergaenzeBereich(kursname(name), name, () -> blaupausenMaske().neu(name));
            }
        }

        hauptfenster.ergaenzeAbschnitt(messages.get("nav.section.dev"));
        hauptfenster.ergaenzeBereich(messages.get("menu.settings"),
                () -> hauptfenster.zeige(new EinstellungenMaske(componentFactory, messages,
                        meldungen, einstellungen, this::setzeDarstellung).maske()));
        hauptfenster.ergaenzeBereich(messages.get("nav.testdata"), this::seedTestData);

        erklaereBereiche();
    }

    /**
     * Ein Satz unter jeder Ueberschrift.
     *
     * <p>In der Kopfzeile stand bis zum 05.09.2026 nur das Wort aus der
     * Seitenleiste, das daneben ohnehin hervorgehoben ist - eine Zeile, die
     * nichts sagte, was man nicht schon sah. Der Satz beantwortet stattdessen
     * die Frage, die sich beim ersten Oeffnen stellt: wozu ist dieser Bereich
     * da, und was tue ich hier als Naechstes.</p>
     *
     * <p>Die Vorlagen bekommen keinen: sie tragen ihren Kursnamen und fuehren
     * unmittelbar in ein Formular, das sich selbst erklaert.</p>
     */
    private void erklaereBereiche() {
        hauptfenster.erklaereBereich(messages.get("menu.settlement"), messages.get("intro.settlement"));
        hauptfenster.erklaereBereich(messages.get("menu.patient"), messages.get("intro.patient"));
        hauptfenster.erklaereBereich(messages.get("menu.self"), messages.get("intro.self"));
        hauptfenster.erklaereBereich(messages.get("menu.groups"), messages.get("intro.groups"));
        hauptfenster.erklaereBereich(messages.get("menu.blueprints"), messages.get("intro.blueprints"));
        hauptfenster.erklaereBereich(messages.get("menu.settings"), messages.get("intro.settings"));
        hauptfenster.erklaereBereich(messages.get("nav.testdata"), messages.get("intro.testdata"));
    }

    /**
     * Der Kursname einer Vorlage, ohne den Zusatz zur Abrechnungsart.
     *
     * <p>Aus "Geburtsvorbereitungskurs, Einzelabrechnung" wird
     * "Geburtsvorbereitungskurs". In der schmalen Seitenleiste kuerzte JavaFX
     * den vollen Namen sonst zu "Geburtsvorbereitungsk..." - und bei zwei
     * Vorlagen ist eine abgeschnittene Beschriftung nicht nur unschoen,
     * sondern moeglicherweise mehrdeutig. Der volle Name bleibt als
     * Kurzhinweis daran haengen und bleibt auch der Schluessel, unter dem eine
     * Blaupause ihre Vorlage findet.</p>
     */
    static String kursname(String vorlagenname) {
        int komma = vorlagenname.indexOf(',');
        return komma < 0 ? vorlagenname : vorlagenname.substring(0, komma).trim();
    }

    /** Systemeigenschaft, mit der sich das Anlegen von Testdaten einschalten laesst. */
    private static final String TESTDATEN_PROPERTY = "gkv.testdaten";

    /**
     * Legt Testdaten an, sofern das ausdruecklich eingeschaltet wurde.
     *
     * <p>Bisher lief das bei jedem Start ungefragt: waren keine Gruppen oder
     * Blaupausen vorhanden, wurden "Max Muster" und drei erfundene Patientinnen
     * in die Datenbank geschrieben. Zusammen mit dem damaligen
     * {@code hbm2ddl.auto=create}, das die Datenbank bei jedem Start leerte,
     * war die Bedingung praktisch immer erfuellt - die Testdaten landeten also
     * verlaesslich in der Produktivdatenbank.</p>
     *
     * <p>Der Menuepunkt unter "Dev" legt die Daten weiterhin auf Wunsch an.
     * Automatisch geschieht das nur noch mit
     * {@code -Dgkv.testdaten=true}.</p>
     */
    private void seedIfEmpty() {
        if (!Boolean.parseBoolean(System.getProperty(TESTDATEN_PROPERTY, "false"))) {
            return;
        }
        try {
            boolean noGroups = controller.getDatabase().getAllPersonGroups().isEmpty();
            boolean noBlue = controller.getDatabase().getAllBlueprints().isEmpty();
            if (noGroups || noBlue) {
                seedTestData();
            }
        } catch (RuntimeException e) {
            // Ein Fehler beim Anlegen der Testdaten darf den Start nicht verhindern.
            System.err.println("Anlegen der Testdaten fehlgeschlagen: " + e.getMessage());
        }
    }

    /**
     * Zeigt die Abrechnungsmaske.
     *
     * <p>Der Aufbau liegt in {@link AbrechnungsMaske}; hier bleibt nur das
     * Einhaengen in den Rahmen. Das umschliessende {@code ScrollPane} entsteht
     * erst hier, weil sein Inhalt sonst nicht durchsuchbar waere - die Maske
     * liefert deshalb den nackten Bereich.</p>
     */
    private void createAbrechnung() {
        AbrechnungsMaske maske = new AbrechnungsMaske(componentFactory, messages, meldungen,
                controller.getDatabase(), abrechnungService::createAndDispatch,
                Anwendungsverzeichnis::versandordner);
        hauptfenster.zeige(maske.erzeuge());
    }

    /**
     * Liefert der Blaupausenmaske ihre Vorlagen und Codelisten.
     *
     * <p>Frueher baute {@code View} das Blaupausenformular selbst - rund 270
     * Zeilen, mit eigener Felderzeugung ohne Erklaerung und ohne Pruefung, und
     * mit Codelisten fuer Rechnungsart und Statuscode, die beide gar nicht
     * mehr im Formular stehen. Geblieben ist die Codeliste, die wirklich eine
     * Auswahl hergibt: die Abrechnungscodes.</p>
     */
    private BlaupausenMaske.Vorlagen vorlagen() {
        return new BlaupausenMaske.Vorlagen() {
            @Override
            public Map<String, DtaMessage> alle() {
                return controller.getGlobalDefinitions().getInvoiceTemplateCollection();
            }

            @Override
            public List<String> auswahlFuer(String feldname) {
                return switch (feldname) {
                    case "Abrechnungscode" -> abrechnungscodes;
                    case "Umsatzsteuersatz" -> UMSATZSTEUERSAETZE;
                    default -> List.of();
                };
            }
        };
    }

    /**
     * Die Umsatzsteuersaetze, die in Frage kommen.
     *
     * <p>19 ist der Regelsatz, 7 der ermaessigte, 0 der Fall ohne
     * Umsatzsteuer - bei einer Hebamme der haeufigste, weil
     * Heilbehandlungen nach &sect;&nbsp;4 Nr. 14 UStG steuerfrei sind. Kurse
     * werden nicht durchweg gleich behandelt; welcher Satz gilt, sagt die
     * Steuerberatung und nicht dieses Programm. Deshalb sind es Vorschlaege
     * und keine Auswahl: das Feld bleibt beschreibbar.</p>
     *
     * <p>Steht hier und nicht in einer JSON-Datei, weil es keine Codeliste
     * aus Anlage 3 ist, sondern allgemeines Steuerrecht.</p>
     */
    private static final List<String> UMSATZSTEUERSAETZE = List.of("19", "7", "0");

    /**
     * Die hinterlegten Abrechnungscodes.
     *
     * <p>Ein Tarifkennzeichen steht bewusst nicht daneben: es wird vertraglich
     * vereinbart, es gibt dafuer keine allgemeine Liste. Ein leeres Auswahlfeld
     * dafuer anzubieten waere schlimmer als ein Textfeld - man klappte es auf
     * und faende nichts.</p>
     */
    private List<String> ladeAbrechnungscodes() {
        List<String> codes = new ArrayList<>();
        try (InputStream quelle = getClass().getResourceAsStream("/codes/abrechnungscodes.json")) {
            if (quelle != null) {
                JsonNode wurzel = objectMapper.readTree(quelle).path("codes");
                if (wurzel.isArray()) {
                    for (JsonNode eintrag : wurzel) {
                        String code = eintrag.path("code").asText("");
                        if (!code.isBlank()) {
                            codes.add(code);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ohne Liste bleibt das Feld eine freie Eingabe. Das ist besser als
            // ein Start, der an einer Codeliste scheitert.
            return List.of();
        }
        return codes;
    }

    /** Baut die Blaupausenmaske auf den aktuellen Rahmen, siehe {@link #gruppenMaske()}. */
    private BlaupausenMaske blaupausenMaske() {
        return new BlaupausenMaske(componentFactory, messages, meldungen, controller.getDatabase(),
                hauptfenster, feldbau, vorlagen());
    }


    /** Baut die Personenmaske auf den aktuellen Rahmen, siehe {@link #gruppenMaske()}. */
    private PersonenMaske personenMaske() {
        return new PersonenMaske(componentFactory, messages, meldungen, controller.getDatabase(), hauptfenster,
                feldbau, patientPopulator, serviceProviderPopulator);
    }

    /**
     * Baut die Gruppenmaske auf den aktuellen Rahmen.
     *
     * <p>Bewusst je Aufruf neu: die Maske haelt keinen Zustand ueber ihren
     * Aufbau hinaus, und so gibt es keine Gelegenheit, dass sie auf einen
     * veralteten Rahmen zeigt.</p>
     */
    private GruppenMaske gruppenMaske() {
        return new GruppenMaske(componentFactory, messages, meldungen, controller.getDatabase(), hauptfenster);
    }

    /**
     * Legt einen Satz Testdaten an, mit dem sich die Anwendung durchspielen
     * laesst.
     *
     * <p>Die Daten selbst stehen in {@link Testdaten} - als eigene Klasse,
     * damit ein Test nachweisen kann, dass eine Abrechnung damit die Pruefung
     * besteht. Als private Methode hier war das nicht moeglich, und genau
     * deshalb fiel nie auf, dass die vorigen Testdaten es nicht taten.</p>
     */
    private void seedTestData() {
        try {
            DataRepository datenbank = controller.getDatabase();

            List<ServiceProvider> dienstleister = Testdaten.dienstleister();
            dienstleister.forEach(datenbank::saveServiceProvider);

            List<Patient> teilnehmerinnen = Testdaten.teilnehmerinnen();
            teilnehmerinnen.forEach(datenbank::savePatient);

            List<PersonGroup> gruppen = Testdaten.gruppen(teilnehmerinnen, dienstleister);
            gruppen.forEach(datenbank::savePersonGroup);

            // Alle Vorlagen, nicht nur die erste: seit es einen zweiten Kurs
            // gibt, soll auch der eine Blaupause zum Ausprobieren haben.
            List<Blueprint> blaupausen = Testdaten.blaupausen(List.copyOf(
                    controller.getGlobalDefinitions().getInvoiceTemplateCollection().keySet()));
            blaupausen.forEach(datenbank::saveBlueprint);

            // Gezaehlt statt in den Text geschrieben: die Meldung nannte "2
            // Blaupausen", nachdem es drei geworden waren. Eine Zahl, die im
            // Meldungstext steht, veraltet beim naechsten Zusatz still.
            meldungen.erfolg(String.format(messages.get("msg.testDataCreated"),
                    dienstleister.size(), teilnehmerinnen.size(), gruppen.size(), blaupausen.size()));
        } catch (Exception e) {
            meldungen.fehler(e.getMessage());
        }
    }
}
