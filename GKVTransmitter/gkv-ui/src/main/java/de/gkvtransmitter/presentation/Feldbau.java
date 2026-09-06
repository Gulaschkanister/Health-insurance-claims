package de.gkvtransmitter.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.FieldValidator;
import de.gkvtransmitter.util.Institutionskennzeichen;
import de.gkvtransmitter.util.ModifierInstance;
import de.gkvtransmitter.util.TagList;
import de.gkvtransmitter.util.modifiers.MaxLengthModifier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Baut die Eingabefelder der Formulare, erklaert sie und liest sie wieder aus.
 *
 * <p>Ein Feld besteht aus drei Teilen untereinander: dem Bedienelement, einer
 * Erklaerung, was hineingehoert, und einer Zeile fuer die Beanstandung.</p>
 *
 * <p>Die Erklaerung stand bisher nirgends - die Beschriftung "IK" musste
 * genuegen, obwohl weder Laenge noch Herkunft daraus hervorgingen. Geprueft
 * wurde nur in Zahlenfeldern; in Textfeldern schnitt ein Formatierer
 * stillschweigend ab, sobald die Hoechstlaenge erreicht war, ohne das zu
 * sagen.</p>
 *
 * <p>Am wichtigsten ist die Pruefziffer der Institutionskennzeichen. Das
 * Verfahren liegt seit je in {@code gkv-core}, wurde aber nur beim Versand
 * angewandt: ein falsches Kassen-IK fiel erst auf, nachdem Teilnehmer und
 * Termine zusammengestellt waren. Jetzt faellt es beim Verlassen des Feldes
 * auf.</p>
 *
 * <p>Weil jedes Feld eingehuellt ist, liefert {@link #erzeugeFeld} eine
 * {@code VBox} und nicht das Bedienelement selbst. Wer daran muss, nimmt
 * {@link #bedienelement(Node)}; wer nur den Wert braucht, {@link #textVon(Node)}
 * oder {@link #datumVon(Node)}.</p>
 */
public class Feldbau {

    /** Stilklasse eines Bedienelements, dessen Inhalt beanstandet wurde. */
    public static final String STIL_FEHLERHAFT = "feld-fehlerhaft";
    /** Stilklasse der Zeile mit der Beanstandung. */
    public static final String STIL_FEHLER = "feld-fehler";
    /** Stilklasse der Zeile mit der Erklaerung. */
    public static final String STIL_HINWEIS = "feld-hinweis";
    /** Stilklasse des Info-Zeichens neben einem Feld. */
    public static final String STIL_INFOZEICHEN = "info-zeichen";
    /** Stilklasse der Einheit hinter einem Feld, siehe {@link #einheit}. */
    public static final String STIL_EINHEIT = "feld-einheit";
    /** Stilklasse eines Feldes, das nur zu lesen ist, siehe {@link #vorschlagsfeld}. */
    public static final String STIL_FEST = "feld-fest";
    /** Vorsatz der Kennung eines Info-Zeichens, gefolgt vom Feldnamen. */
    public static final String ID_INFO = "feld-info-";

    /** Vorsatz der Schluessel, unter denen die Erklaerungen stehen. */
    private static final String HINWEIS_SCHLUESSEL = "help.";

    private final UiFactory bausteine;
    private final AppMessages texte;

    public Feldbau(UiFactory bausteine, AppMessages texte) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
    }

    /**
     * Baut ein Feld: Bedienelement, Erklaerung, Platz fuer die Beanstandung.
     *
     * @param feldname     der Name aus der Tag-Datei, etwa {@code kassenIk}
     * @param beschreibung wie das Feld auszusehen hat, oder {@code null}
     */
    public Node erzeugeFeld(String feldname, TagList beschreibung) {
        return erzeugeFeld(feldname, beschreibung, null, List.of());
    }

    /**
     * Baut ein Feld mit mitgegebener Erklaerung und Auswahlliste.
     *
     * <p>Fuer Felder, die nicht aus einer Tag-Datei stammen, sondern aus den
     * Segmentdefinitionen: dort steht die Erklaerung als {@code description}
     * neben dem Feld und muss nicht noch einmal in
     * {@code ui-messages.json} gepflegt werden.</p>
     *
     * @param feldname     Name des Feldes, auch Schluessel fuer {@code help.}
     * @param beschreibung wie das Feld auszusehen hat, oder {@code null}
     * @param erklaerung   was hineingehoert; hat Vorrang vor {@code help.}
     * @param auswahl      hinterlegte Werte, oder leer fuer freie Eingabe
     */
    public Node erzeugeFeld(String feldname, TagList beschreibung, String erklaerung, List<String> auswahl) {
        Node bedienelement = bedienelementFuer(feldname, beschreibung,
                auswahl == null ? List.of() : auswahl);

        Label beanstandung = bausteine.createLabel("");
        beanstandung.getStyleClass().add(STIL_FEHLER);
        beanstandung.setWrapText(true);
        verbergen(beanstandung);

        Optional<String> text = (erklaerung == null || erklaerung.isBlank())
                ? erklaerung(feldname, beschreibung)
                : Optional.of(erklaerung);

        VBox feld = new VBox(3, kopfzeile(bedienelement, feldname, beschreibung, text));
        text.ifPresent(inhalt -> {
            Label hinweis = bausteine.createLabel(inhalt);
            hinweis.getStyleClass().add(STIL_HINWEIS);
            hinweis.setWrapText(true);
            if (!stetsSichtbar(feldname)) {
                verbergen(hinweis);
            }
            feld.getChildren().add(hinweis);
        });
        feld.getChildren().add(beanstandung);
        // Das Bedienelement steckt jetzt in einer Zeile mit dem Info-Zeichen
        // und ist nicht mehr das erste Kind. Wer es sucht, findet es hier -
        // unabhaengig davon, wie das Feld sonst noch umgebaut wird.
        feld.getProperties().put(BEDIENELEMENT, bedienelement);

        // Dieselbe Pruefung fuer beides: den Fokuswechsel und den Abruf beim
        // Speichern. Es waren einmal zwei, und nur eine merkte sich, dass
        // beanstandet wurde - siehe ueberwache.
        feld.getProperties().put(PRUEFUNG,
                ueberwache(feldname, beschreibung, bedienelement, beanstandung));
        return feld;
    }

    /** Schluessel, unter dem ein Feld seine eigene Pruefung mit sich fuehrt. */
    private static final String PRUEFUNG = "gkv.pruefung";

    /** Schluessel, unter dem ein Feld sein Bedienelement mit sich fuehrt. */
    private static final String BEDIENELEMENT = "gkv.bedienelement";

    /**
     * Felder, deren Erklaerung ausgeklappt bleibt.
     *
     * <p>Simons Vorschlag war, alle Erklaerungen hinter ein Info-Zeichen zu
     * legen: die Formulare sind lang, und die Erklaerung wird meist nur beim
     * ersten Mal gebraucht. Dagegen steht ein Einwand, der ebenso stimmt -
     * <b>was hinter einem Zeichen liegt, liest niemand.</b></p>
     *
     * <p>Der Mittelweg: eingeklappt ist die Regel, ausgeklappt die Ausnahme.
     * Und zwar dort, wo der richtige Wert sich nicht erraten laesst und ein
     * falscher teuer ist:</p>
     *
     * <ul>
     *   <li>die beiden Institutionskennzeichen - sie werden vergeben, man
     *       kann sie sich nicht ausdenken, und ein falsches laesst die Kasse
     *       die ganze Lieferung abweisen;</li>
     *   <li>Positionsnummer und Tarifkennzeichen - sie stehen im Vertrag
     *       beziehungsweise in Anlage 3, nirgends sonst;</li>
     *   <li>die beiden Betraege - hier entscheidet die Schreibweise mit Komma
     *       und zwei Nachkommastellen ueber Annahme oder Syntaxfehler.</li>
     * </ul>
     *
     * <p>Alles andere - Vorname, Strasse, Ort - erklaert sich von selbst; dort
     * stand unter dem Feld ohnehin nur die Hoechstlaenge.</p>
     *
     * <p>Die Liste steht bewusst hier und nicht in den JSON-Dateien: sie ist
     * eine Aussage darueber, was <em>jemand am Bildschirm</em> braucht, keine
     * ueber das Datenformat.</p>
     */
    private static final List<String> STETS_ERKLAERT = List.of(
            "ik", "kassenIk",
            "Abrechnungspositionsnummer", "Tarifkennzeichen",
            "Durchschnittlicher Einzelbetrag", "Zuzahlung pro Position");

    private static boolean stetsSichtbar(String feldname) {
        return STETS_ERKLAERT.stream().anyMatch(name -> name.equalsIgnoreCase(feldname));
    }

    /**
     * Bedienelement und Info-Zeichen nebeneinander.
     *
     * <p>Ohne Erklaerung kein Zeichen: eines, hinter dem nichts liegt, ist
     * eine Falle. Dieselbe Regel gilt in der Statuszeile.</p>
     */
    private Node kopfzeile(Node bedienelement, String feldname, TagList beschreibung,
            Optional<String> erklaerung) {
        HBox zeile = new HBox(6, bedienelement, einheitszeichen(beschreibung),
                beiwerk(feldname, erklaerung));
        zeile.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bedienelement, Priority.ALWAYS);
        if (bedienelement instanceof Region breit) {
            breit.setMaxWidth(Double.MAX_VALUE);
        }
        return zeile;
    }

    /**
     * Die Einheit, die hinter dem Feld steht.
     *
     * <p>Sie muss nicht gepflegt werden: die Segmentdefinitionen sagen sie
     * bereits. {@code PERCENT} ist ein Prozentsatz, {@code COST} ein Betrag in
     * Euro - das Verfahren rechnet in keiner anderen Waehrung.</p>
     *
     * <p><b>Warum sie ans Feld gehoert und nicht in die Erklaerung darunter:</b>
     * die Erklaerung ist eingeklappt, sobald das Feld nicht zu
     * {@link #STETS_ERKLAERT} gehoert - beim Umsatzsteuersatz also immer. Dort
     * stand dann eine 19 ohne jede Angabe, wovon. Simon (K1): "Statt 19 in dem
     * Feld Umsatzsteuer koennte dort noch % danach angezeigt werden."</p>
     *
     * <p>Daneben und nicht hinein: ein Zeichen <em>im</em> Feld waere Teil des
     * Wertes und ginge so in die Nachricht - {@code 19 %} statt {@code 19}.</p>
     */
    private static Optional<String> einheit(TagList beschreibung) {
        if (beschreibung == null) {
            return Optional.empty();
        }
        return switch (beschreibung.getInputOption()) {
            case PERCENT -> Optional.of("%");
            case COST -> Optional.of("€");
            default -> Optional.empty();
        };
    }

    /** Breite der Einheitenspalte, gross genug fuer % und €. */
    private static final double BREITE_EINHEIT = 12;

    /**
     * Das Zeichen - oder sein Platz.
     *
     * <p>Derselbe Grund wie beim Info-Zeichen: bliebe der Platz leer, waeren
     * die Felder mit Einheit um die Breite des Zeichens schmaler als die
     * uebrigen. Im Blaupausenformular stehen beide Arten untereinander, und
     * die rechte Kante liefe sichtbar aus. Am gezeichneten Bild sofort zu
     * sehen - so ist es auch aufgefallen, beim zweiten Mal.</p>
     */
    private Node einheitszeichen(TagList beschreibung) {
        Optional<String> zeichen = einheit(beschreibung);
        if (zeichen.isEmpty()) {
            Region platzhalter = new Region();
            platzhalter.setMinWidth(BREITE_EINHEIT);
            platzhalter.setPrefWidth(BREITE_EINHEIT);
            return platzhalter;
        }
        Label einheit = bausteine.createLabel(zeichen.get());
        einheit.getStyleClass().add(STIL_EINHEIT);
        einheit.setMinWidth(BREITE_EINHEIT);
        return einheit;
    }

    /**
     * Was rechts neben dem Bedienelement steht: das Zeichen oder sein Platz.
     *
     * <p>Der Platz bleibt auch dann frei, wenn kein Zeichen dort steht. Sonst
     * waeren die Felder mit stehender Erklaerung siebzehn Punkte breiter als
     * die uebrigen, und in einem zweispaltigen Formular faellt genau das auf -
     * am gezeichneten Bild sofort zu sehen gewesen.</p>
     */
    private Node beiwerk(String feldname, Optional<String> erklaerung) {
        if (erklaerung.isEmpty() || stetsSichtbar(feldname)) {
            Region platzhalter = new Region();
            platzhalter.setMinWidth(BREITE_INFOZEICHEN);
            platzhalter.setPrefWidth(BREITE_INFOZEICHEN);
            return platzhalter;
        }
        Button zeichen = bausteine.createButton("i");
        zeichen.getStyleClass().add(STIL_INFOZEICHEN);
        zeichen.setId(ID_INFO + feldname);
        zeichen.setFocusTraversable(false);
        // Der Text haengt zusaetzlich als Kurzhinweis daran. Wer mit der Maus
        // darueberfaehrt, muss nicht erst klicken - und wer klickt, bekommt
        // ihn stehend, weil ein Kurzhinweis von selbst wieder verschwindet.
        zeichen.setTooltip(new Tooltip(erklaerung.get()));
        zeichen.setOnAction(ereignis -> erklaerungUmschalten(zeichen));
        return zeichen;
    }

    /** Kantenlaenge des Info-Zeichens, wie in {@code gkv.css} festgelegt. */
    private static final double BREITE_INFOZEICHEN = 17;

    /** Klappt die Erklaerung unter dem Feld auf oder wieder zu. */
    private void erklaerungUmschalten(Node zeichen) {
        Node feld = zeichen.getParent() == null ? null : zeichen.getParent().getParent();
        if (!(feld instanceof VBox huelle)) {
            return;
        }
        huelle.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(zeile -> zeile.getStyleClass().contains(STIL_HINWEIS))
                .findFirst()
                .ifPresent(zeile -> {
                    if (zeile.isVisible()) {
                        verbergen(zeile);
                    } else {
                        zeigen(zeile);
                    }
                });
    }

    /**
     * Prueft ein Feld erneut und zeigt die Beanstandung an.
     *
     * <p>Die Pruefung lief bisher nur beim Verlassen des Feldes. Wer ein
     * Formular ausfuellte und sofort speicherte, ohne das letzte Feld je
     * verlassen zu haben, bekam nie eine Beanstandung zu sehen - und die Maske
     * speicherte, weil sie gar nicht fragte. Der Fehler fiel dann erst beim
     * Versand auf, Wochen spaeter und als Ablehnung der ganzen Lieferung.</p>
     *
     * @return die Beanstandung, oder leer wenn das Feld in Ordnung ist
     */
    @SuppressWarnings("unchecked")
    public Optional<String> beanstandung(Node feld) {
        if (feld == null) {
            return Optional.empty();
        }
        Object pruefung = feld.getProperties().get(PRUEFUNG);
        if (pruefung instanceof Supplier<?> abruf) {
            return (Optional<String>) abruf.get();
        }
        return Optional.empty();
    }

    /**
     * Prueft alle Felder und liefert die Beanstandungen in einer Liste.
     *
     * <p>Prueft <em>alle</em>, nicht bis zur ersten: wer drei Felder falsch
     * ausgefuellt hat, soll das in einem Zug sehen und nicht dreimal
     * hintereinander speichern muessen.</p>
     */
    public List<String> beanstandungen(Collection<Node> felder) {
        List<String> befunde = new ArrayList<>();
        for (Node feld : felder) {
            beanstandung(feld).ifPresent(befunde::add);
        }
        return befunde;
    }

    /**
     * Das Bedienelement eines Feldes, ohne Erklaerung und Beanstandung.
     *
     * <p>Bis zum 05.09.2026 war das schlicht das erste Kind der Huelle. Seit
     * neben dem Bedienelement ein Info-Zeichen stehen kann, ist das erste Kind
     * mitunter eine Zeile aus beidem - deshalb fuehrt das Feld sein
     * Bedienelement als Eigenschaft mit sich. Der Rueckgriff auf das erste
     * Kind bleibt fuer Huellen, die anderswo gebaut wurden.</p>
     */
    public static Node bedienelement(Node feld) {
        if (feld instanceof VBox huelle) {
            Object hinterlegt = huelle.getProperties().get(BEDIENELEMENT);
            if (hinterlegt instanceof Node element) {
                return element;
            }
            if (!huelle.getChildren().isEmpty()) {
                return huelle.getChildren().get(0);
            }
        }
        return feld;
    }

    /**
     * Der Textwert eines Feldes.
     *
     * @return der Inhalt, oder eine leere Zeichenkette
     */
    public String textVon(Node feld) {
        return switch (bedienelement(feld)) {
            case TextInputControl eingabe -> eingabe.getText() == null ? "" : eingabe.getText();
            // commitValue zuerst, sonst liefert ein beschreibbarer Zaehler den
            // Wert vor der letzten Eingabe - siehe JavaFxUiFactory.
            case Spinner<?> zaehler -> {
                JavaFxUiFactory.uebernimm(zaehler);
                yield zeichenkette(zaehler.getValue());
            }
            case ComboBox<?> auswahl -> zeichenkette(auswahl.getValue());
            case DatePicker kalender -> zeichenkette(kalender.getValue());
            default -> "";
        };
    }

    /**
     * Der Datumswert eines Feldes.
     *
     * @return das Datum, oder {@code null}
     */
    public LocalDate datumVon(Node feld) {
        return bedienelement(feld) instanceof DatePicker kalender ? kalender.getValue() : null;
    }

    // --- Aufbau ----------------------------------------------------------

    /**
     * Ab welcher Stellenzahl ein Zaehler keinem mehr nuetzt.
     *
     * <p>Auf- und Ab-Pfeile lohnen sich bei einem Verarbeitungskennzeichen
     * {@code 01}. Bei einer neunstelligen Positionsnummer oder einer
     * Postleitzahl klickt sie niemand hoch - man tippt. Ein Zaehler schadet
     * dort sogar doppelt: er steht mit einer Null vorbelegt da, und was
     * hineingetippt und nicht mit der Eingabetaste bestaetigt wird, liefert
     * {@code getValue()} gar nicht erst zurueck. Genau daher stammte der
     * Eindruck, eine Eingabe verschwinde wieder.</p>
     */
    private static final int ZAEHLERGRENZE = 2;

    private Node bedienelementFuer(String feldname, TagList beschreibung, List<String> auswahl) {
        InputOption art = beschreibung == null ? InputOption.STRING : beschreibung.getInputOption();
        int grenze = hoechstlaenge(beschreibung).orElse(0);

        // Hinterlegte Werte schlagen die Art des Feldes. Zuvor entschied die
        // Art zuerst, und die Vorschlagsmechanik traf deshalb genau die
        // falschen Felder: der Abrechnungscode bekam ein Aufklappmenue mit
        // einer einzigen Zeile, waehrend der Umsatzsteuersatz - wo drei
        // Vorschlaege helfen - als PERCENT zum leeren Textfeld wurde.
        if (!auswahl.isEmpty() && art != InputOption.DATE && art != InputOption.TIME) {
            return vorschlagsfeld(auswahl, grenze);
        }
        if (beschreibung == null) {
            return bausteine.createTextField();
        }

        return switch (art) {
            case STRING -> textfeld(grenze);
            case NUMBER -> grenze > ZAEHLERGRENZE ? textfeld(grenze) : zaehler(Integer.class);
            case PERCENT, COST -> grenze > ZAEHLERGRENZE ? textfeld(grenze) : zaehler(BigDecimal.class);
            // Ohne hinterlegte Werte waere ein Auswahlfeld eine Zumutung: es
            // liesse sich aufklappen und enthielte nichts. Das Tarifkennzeichen
            // ist vertraglich vereinbart, es gibt dafuer keine Codeliste.
            case CODE, NUMBER_SUGGESTION -> textfeld(grenze);
            case DATE, TIME -> datumsfeld(feldname);
            default -> bausteine.createTextField();
        };
    }

    /**
     * Ein Kalenderfeld; bei einem Geburtsdatum nur mit moeglichen Tagen.
     *
     * <p>Ausgegraut ist alles, was {@link #befundZu} ablehnen wuerde - die
     * Zukunft <em>und</em> die Jahrgaenge, die kein Alter zwischen
     * {@value #MINDESTALTER} und {@value #HOECHSTALTER} ergeben. <b>Ein Fehler,
     * der gar nicht erst entsteht, muss auch nicht erklaert werden.</b></p>
     *
     * <p>Die Beanstandung in {@link #beanstandeGeburtsdatum} bleibt trotzdem
     * noetig - der Kalender laesst sich auch beschreiben, und ueber
     * {@code EntityFieldPopulator} kommen Werte herein, die nie durch ihn
     * gegangen sind.</p>
     */
    private Node datumsfeld(String feldname) {
        DatePicker kalender = bausteine.createDatePicker();
        if (istGeburtsdatum(feldname)) {
            kalender.setDayCellFactory(spalte -> new javafx.scene.control.DateCell() {
                @Override
                public void updateItem(LocalDate tag, boolean leer) {
                    super.updateItem(tag, leer);
                    setDisable(leer || befundZu(tag) != Geburtsdatum.MOEGLICH);
                }
            });
        }
        return kalender;
    }

    /**
     * Ein Feld mit hinterlegten Vorschlaegen.
     *
     * <p><b>Ein einziger Vorschlag ist keine Auswahl.</b> Simons Einwand galt
     * dem Abrechnungscode: {@code codes/abrechnungscodes.json} enthaelt genau
     * einen Eintrag, und ein Aufklappmenue mit einer Zeile ist Bedienlast ohne
     * Nutzen - man klappt es auf, um zu erfahren, dass es nichts zu waehlen
     * gibt. Es wird deshalb ein Textfeld, in dem der Wert schon steht.</p>
     *
     * <p><b>Bei genau einem Wert ist das Feld seit dem 06.09.2026 nur noch zu
     * lesen.</b> Simon nach dem ersten Durchgang (K2): "Felder die Readonly
     * sind sollten entsprechend markiert werden und nicht als Eingabefeld zu
     * sehen sein." Ein vorbelegtes Feld, das aussieht wie ein leeres, lädt zum
     * Ueberschreiben ein - und der Abrechnungscode ist nichts, was man sich
     * aussucht.</p>
     *
     * <p>Hier stand zuvor die entgegengesetzte Begruendung: das Feld bleibe
     * beschreibbar, weil ein gesperrtes eine Sackgasse waere, sollte je ein
     * anderer Code gelten. <b>Der Einwand faellt mit der Mechanik selbst weg:</b>
     * gesperrt ist das Feld nur, solange die Liste <em>einen</em> Eintrag hat.
     * Bringt ein weiterer Leistungsbereich einen zweiten, wird daraus von
     * selbst wieder ein beschreibbares Auswahlfeld - ohne dass hier etwas zu
     * aendern waere. Die Tuer ist nicht zugemauert, sie geht nur auf, wenn es
     * etwas zu waehlen gibt.</p>
     *
     * <p>{@code setEditable(false)} und nicht {@code setDisable(true)}: ein
     * abgeschaltetes Feld waere ausgegraut, nicht auswaehlbar und nicht
     * kopierbar. Man soll den Wert lesen und mitnehmen koennen.</p>
     */
    private Node vorschlagsfeld(List<String> auswahl, int grenze) {
        if (auswahl.size() == 1) {
            TextField feld = textfeld(grenze);
            feld.setText(auswahl.get(0));
            feld.setEditable(false);
            feld.setFocusTraversable(false);
            feld.getStyleClass().add(STIL_FEST);
            return feld;
        }
        return auswahlfeld(auswahl);
    }

    /**
     * Ein Textfeld, hoechstens so lang wie erlaubt.
     *
     * <p>Ohne Platzhalter: was hineingehoert, steht in der Erklaerung darunter.
     * Ein Platzhalter "0,00" ueber der Erklaerung "als ganze Zahl" widerspraeche
     * ihr - zwei Angaben, die sich widersprechen, sind schlechter als eine.</p>
     */
    private TextField textfeld(int grenze) {
        TextField feld = bausteine.createTextField();
        if (grenze > 0) {
            begrenze(feld, grenze);
        }
        return feld;
    }

    private ComboBox<String> auswahlfeld(List<String> auswahl) {
        ComboBox<String> feld = new ComboBox<>();
        feld.getItems().addAll(auswahl);
        feld.setEditable(true);
        feld.setMaxWidth(Double.MAX_VALUE);
        return feld;
    }

    private <T> Spinner<T> zaehler(Class<T> art) {
        Spinner<T> zaehler = bausteine.createSpinner(art);
        zaehler.setPrefWidth(220);
        return zaehler;
    }

    /**
     * Haengt die Pruefung an das Bedienelement.
     *
     * <p>Waehrend des Tippens ist eine unfertige Eingabe zwangslaeufig falsch -
     * ein IK ist nach drei Ziffern noch keines. Die Beanstandung erscheint
     * deshalb erst, wenn das Feld den Fokus verliert; danach bessert sie sich
     * bei jedem Tastendruck nach, damit man beim Berichtigen sieht, wann es
     * stimmt.</p>
     */
    private Supplier<Optional<String>> ueberwache(String feldname, TagList beschreibung,
            Node bedienelement, Label beanstandung) {
        boolean[] schonBeanstandet = {false};
        // Eine einzige Stelle, die prueft und anzeigt - und die sich merkt,
        // dass beanstandet wurde. Zuvor gab es diese Logik zweimal: hier fuer
        // den Fokuswechsel und noch einmal in zeigeBefund fuer den Abruf beim
        // Speichern. Nur die erste setzte schonBeanstandet. Wer also auf
        // Speichern drueckte, eine Beanstandung bekam und das Feld berichtigte,
        // sah die rote Zeile stehenbleiben, bis er das Feld verliess.
        Supplier<Optional<String>> pruefen = () -> {
            Optional<String> befund = pruefe(feldname, beschreibung, textVon(bedienelement));
            if (befund.isEmpty()) {
                beanstandung.setText("");
                verbergen(beanstandung);
                bedienelement.getStyleClass().remove(STIL_FEHLERHAFT);
                return befund;
            }
            schonBeanstandet[0] = true;
            beanstandung.setText(befund.get());
            zeigen(beanstandung);
            if (!bedienelement.getStyleClass().contains(STIL_FEHLERHAFT)) {
                bedienelement.getStyleClass().add(STIL_FEHLERHAFT);
            }
            return befund;
        };

        bedienelement.focusedProperty().addListener((wert, hatteFokus, hatFokus) -> {
            if (!hatFokus) {
                pruefen.get();
            }
        });

        switch (bedienelement) {
            case TextInputControl eingabe -> eingabe.textProperty()
                    .addListener((wert, alt, neu) -> nachbessern(schonBeanstandet, pruefen));
            case Spinner<?> zaehler -> {
                zaehler.valueProperty().addListener((wert, alt, neu) -> pruefen.get());
                if (zaehler.getEditor() != null) {
                    zaehler.getEditor().textProperty()
                            .addListener((wert, alt, neu) -> nachbessern(schonBeanstandet, pruefen));
                }
            }
            case ComboBox<?> auswahl -> auswahl.valueProperty().addListener((wert, alt, neu) -> pruefen.get());
            case DatePicker kalender -> kalender.valueProperty().addListener((wert, alt, neu) -> pruefen.get());
            default -> { }
        }
        return pruefen;
    }

    /** Waehrend des Tippens nur nachbessern, was schon beanstandet wurde. */
    private void nachbessern(boolean[] schonBeanstandet, Supplier<Optional<String>> pruefen) {
        if (schonBeanstandet[0]) {
            pruefen.get();
        }
    }

    // --- Pruefung --------------------------------------------------------

    /**
     * Prueft einen Wert.
     *
     * <p>Paketsichtbar, damit sich die Regeln ohne Bedienelement pruefen
     * lassen.</p>
     *
     * @return die Beanstandung, oder leer
     */
    Optional<String> pruefe(String feldname, TagList beschreibung, String wert) {
        if (wert == null || wert.isBlank()) {
            return Optional.empty();
        }
        String bereinigt = wert.trim();
        if (istKennzeichen(feldname)) {
            return beanstandeKennzeichen(bereinigt);
        }
        if (istGeburtsdatum(feldname)) {
            return beanstandeGeburtsdatum(bereinigt);
        }
        if ("plz".equalsIgnoreCase(feldname) && !bereinigt.matches("\\d{5}")) {
            return Optional.of(texte.get("msg.invalidPlz"));
        }
        Optional<String> zuLang = zuLang(beschreibung, bereinigt);
        if (zuLang.isPresent()) {
            return zuLang;
        }
        if (beschreibung == null) {
            return Optional.empty();
        }
        FieldValidator.Feldbefund befund = FieldValidator.validate(feldname, bereinigt,
                beschreibung.getInputOption(), javatyp(beschreibung.getInputOption()));
        if (befund == null || befund.isValid) {
            return Optional.empty();
        }
        return Optional.of(befund.errorMessage == null ? texte.get("msg.invalidValue") : befund.errorMessage);
    }

    /**
     * Ob ein Feld ein Geburtsdatum traegt.
     *
     * <p>Ueber den Namen und nicht ueber {@code InputOption.DATE}: nicht jedes
     * Datum ist ein Geburtstag. Ein Leistungsdatum darf in der Zukunft liegen,
     * ein Geburtstag nicht - und eine Regel, die auf alle Datumsfelder wirkt,
     * waere beim naechsten Terminfeld im Weg.</p>
     */
    private static boolean istGeburtsdatum(String feldname) {
        if (feldname == null) {
            return false;
        }
        String klein = feldname.toLowerCase(java.util.Locale.GERMAN);
        return klein.contains("birthdate") || klein.contains("geburtsdatum");
    }

    /** Juengstes Alter, das noch angenommen wird, siehe {@link #beanstandeGeburtsdatum}. */
    static final int MINDESTALTER = 10;
    /** Aeltestes Alter, das noch angenommen wird. */
    static final int HOECHSTALTER = 120;

    /**
     * Beanstandet ein unmoegliches Geburtsdatum.
     *
     * <p>Simon (K5): "Man kann Geburtsdaten in die Zukunft verlegen?" Ja - im
     * Kern gibt es die Regel seit jeher ({@code GEBURTSDATUM_ZUKUNFT} in
     * {@code VersichertenangabenRegel}), aber sie greift erst vor dem Versand.
     * Ein Vertipper wurde also gespeichert und faellt Wochen spaeter auf, wenn
     * ein ganzer Lauf daran haengenbleibt. <b>Am Feld ist er in dem Augenblick
     * zu berichtigen, in dem er entsteht.</b></p>
     *
     * <p><b>Zur Untergrenze.</b> Simons zweiter Gedanke war "eventuell keine
     * 1-Jaehrigen". Richtig - aber die Grenze muss tief liegen: eine
     * Fuenfzehnjaehrige, die einen Geburtsvorbereitungskurs besucht, gibt es,
     * und eine Anwendung, die sie abweist, waere schlimmer als eine, die einen
     * Tippfehler durchlaesst. {@value #MINDESTALTER} Jahre trifft die
     * Vertipper im Jahr (2020 statt 1990) und keinen einzigen Lebenslauf.</p>
     *
     * <p>Die Obergrenze entspricht {@code VersichertenangabenRegel}. Dort ist
     * sie eine Warnung, hier eine Beanstandung: der Kern prueft eine fertige
     * Nachricht, in der sich nichts mehr aendern laesst, das Feld einen Wert,
     * den gerade jemand eintippt.</p>
     */
    Optional<String> beanstandeGeburtsdatum(String wert) {
        LocalDate datum;
        try {
            datum = LocalDate.parse(wert);
        } catch (java.time.format.DateTimeParseException unlesbar) {
            // Ein DatePicker liefert nichts Unlesbares; ein von Hand
            // getippter Text schon. Den beanstandet die allgemeine Pruefung.
            return Optional.empty();
        }
        return switch (befundZu(datum)) {
            case ZUKUNFT -> Optional.of(texte.get("msg.birthDateFuture"));
            case ALTER -> Optional.of(String.format(texte.get("msg.birthDateImplausible"),
                    java.time.Period.between(datum, LocalDate.now()).getYears()));
            case MOEGLICH -> Optional.empty();
        };
    }

    /** Warum ein Geburtsdatum nicht in Frage kommt - oder dass es das tut. */
    private enum Geburtsdatum { MOEGLICH, ZUKUNFT, ALTER }

    /**
     * Die eine Stelle, die ueber ein Geburtsdatum entscheidet.
     *
     * <p><b>Kalender und Pruefung muessen dasselbe sagen.</b> Bis zum
     * 06.09.2026 taten sie es nicht: der Kalender sperrte nur die Zukunft,
     * waehrend die Pruefung zusaetzlich das Alter mass. Wer den 12.03.2024
     * anklickte, durfte ihn waehlen und bekam danach eine Beanstandung -
     * <b>eine Auswahl anzubieten und sie anschliessend abzulehnen, ist
     * schlechter als sie gar nicht erst anzubieten.</b> Simon: "wenn es keinen
     * Sinn macht, soll es auch nicht moeglich sein, eins auszuwaehlen".</p>
     *
     * <p>Beide gehen deshalb hier durch. Zwei Stellen mit derselben Regel
     * laufen auseinander, sobald jemand eine davon aendert.</p>
     */
    private static Geburtsdatum befundZu(LocalDate datum) {
        LocalDate heute = LocalDate.now();
        if (datum.isAfter(heute)) {
            return Geburtsdatum.ZUKUNFT;
        }
        int alter = java.time.Period.between(datum, heute).getYears();
        return alter < MINDESTALTER || alter > HOECHSTALTER
                ? Geburtsdatum.ALTER : Geburtsdatum.MOEGLICH;
    }

    /**
     * Beanstandet ein Institutionskennzeichen und nennt den Ausweg.
     *
     * <p>Die alte Meldung lautete "Die Pruefziffer stimmt nicht. Ein IK hat
     * neun Ziffern." und war eine Sackgasse: sie sagte nicht, dass man sich
     * kein IK ausdenken kann. Wer neun plausible Ziffern eintippte, stand vor
     * einer Ablehnung ohne Hinweis, wie er zu einer gueltigen Zahl kaeme - denn
     * dafuer muss man die Pruefziffer ausrechnen.</p>
     *
     * <p>Ausrechnen kann {@link Institutionskennzeichen#berechnePruefziffer}
     * das laengst; es wurde nur nirgends angezeigt. Jetzt steht die richtige
     * Ziffer in der Beanstandung. Das macht aus der Sackgasse einen Hinweis -
     * und ist keine Einladung, sich ein IK zu bauen: die Ziffer passt zu den
     * <em>eingegebenen</em> acht Stellen, ob es dieses IK gibt, sagt sie
     * nicht.</p>
     */
    Optional<String> beanstandeKennzeichen(String wert) {
        if (Institutionskennzeichen.istGueltig(wert)) {
            return Optional.empty();
        }
        if (!wert.matches("\\d{" + Institutionskennzeichen.LAENGE + "}")) {
            return Optional.of(texte.get("msg.invalidIkLength"));
        }
        String vorderteil = wert.substring(0, Institutionskennzeichen.LAENGE - 1);
        int pruefziffer = Institutionskennzeichen.berechnePruefziffer(wert);
        return Optional.of(texte.get("msg.invalidIk") + " "
                + String.format(texte.get("msg.ikPruefziffer"),
                        vorderteil, pruefziffer, vorderteil + pruefziffer));
    }

    /**
     * Ob das Feld ein Institutionskennzeichen traegt.
     *
     * <p>Das eigene IK des Dienstleisters heisst {@code ik}, das der Kasse
     * {@code kassenIk}; beide folgen demselben Verfahren.</p>
     */
    private boolean istKennzeichen(String feldname) {
        return "ik".equalsIgnoreCase(feldname) || "kassenIk".equalsIgnoreCase(feldname);
    }

    private Optional<String> zuLang(TagList beschreibung, String wert) {
        return hoechstlaenge(beschreibung)
                .filter(grenze -> wert.length() > grenze)
                .map(grenze -> String.format(texte.get("msg.tooLong"), grenze));
    }

    private Optional<Integer> hoechstlaenge(TagList beschreibung) {
        if (beschreibung == null || beschreibung.getModifierList() == null) {
            return Optional.empty();
        }
        for (ModifierInstance modifier : beschreibung.getModifierList()) {
            if (modifier instanceof MaxLengthModifier grenze) {
                return Optional.of(grenze.getMaxLength());
            }
        }
        return Optional.empty();
    }

    /**
     * Begrenzt die Eingabe auf die Hoechstlaenge.
     *
     * <p>Der Formatierer schneidet weiterhin ab - anders liesse sich eine zu
     * lange Eingabe kaum verhindern -, aber die Erklaerung unter dem Feld nennt
     * die Grenze vorher. Zuvor hoerte das Feld bei Zeichen 101 einfach auf zu
     * reagieren, ohne dass jemand wusste, warum.</p>
     */
    private void begrenze(TextField feld, int grenze) {
        feld.setTextFormatter(new TextFormatter<>(aenderung ->
                aenderung.getControlNewText().length() <= grenze ? aenderung : null));
    }

    private String javatyp(InputOption art) {
        return switch (art) {
            case NUMBER, NUMBER_SUGGESTION -> "Integer";
            case PERCENT, COST -> "BigDecimal";
            default -> "String";
        };
    }

    // --- Erklaerungen ----------------------------------------------------

    /**
     * Die Erklaerung zu einem Feld, gefolgt von der Hoechstlaenge, sofern es
     * eine gibt.
     *
     * <p>Fehlt der Text, bleibt das Feld ohne Erklaerung - besser keine als
     * eine nichtssagende.</p>
     */
    private Optional<String> erklaerung(String feldname, TagList beschreibung) {
        String schluessel = HINWEIS_SCHLUESSEL + feldname;
        String text = texte.get(schluessel);
        Optional<String> eigener = schluessel.equals(text) ? Optional.empty() : Optional.of(text);
        Optional<String> grenze = hoechstlaenge(beschreibung)
                .map(hoechstens -> String.format(texte.get("help.maxLength"), hoechstens));

        if (eigener.isPresent() && grenze.isPresent() && !festeLaenge(feldname)) {
            return Optional.of(eigener.get() + " " + grenze.get());
        }
        return eigener.or(() -> grenze);
    }

    /**
     * Ob das Feld eine feste Laenge hat und keine Hoechstlaenge.
     *
     * <p>Unter dem IK stand "... neunstellig ... Hoechstens 9 Zeichen.", unter
     * der Postleitzahl "Fuenfstellig ... Hoechstens 5 Zeichen." - zweimal
     * dasselbe, und beim zweiten Mal falsch: ein achtstelliges IK ist nicht
     * etwa kuerzer und damit in Ordnung, es ist ungueltig. {@link #pruefe}
     * verlangt fuer genau diese Felder eine <em>feste</em> Laenge; die
     * Erklaerung sagt es dort selbst, und die allgemeine Zeile entfaellt.</p>
     */
    private boolean festeLaenge(String feldname) {
        return istKennzeichen(feldname) || "plz".equalsIgnoreCase(feldname);
    }

    // --- Kleinkram -------------------------------------------------------

    private static String zeichenkette(Object wert) {
        return wert == null ? "" : String.valueOf(wert);
    }

    /**
     * Blendet eine Zeile aus, ohne dass ihr Platz erhalten bleibt.
     *
     * <p>Ohne {@code setManaged(false)} bliebe eine leere Zeile stehen und die
     * Felder eines Formulars staenden weiter auseinander als noetig.</p>
     */
    private static void verbergen(Label zeile) {
        zeile.setVisible(false);
        zeile.setManaged(false);
    }

    private static void zeigen(Label zeile) {
        zeile.setVisible(true);
        zeile.setManaged(true);
    }
}
