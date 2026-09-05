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
        Node bedienelement = bedienelementFuer(beschreibung, auswahl == null ? List.of() : auswahl);

        Label beanstandung = bausteine.createLabel("");
        beanstandung.getStyleClass().add(STIL_FEHLER);
        beanstandung.setWrapText(true);
        verbergen(beanstandung);

        Optional<String> text = (erklaerung == null || erklaerung.isBlank())
                ? erklaerung(feldname, beschreibung)
                : Optional.of(erklaerung);

        VBox feld = new VBox(3, kopfzeile(bedienelement, feldname, text));
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
    private Node kopfzeile(Node bedienelement, String feldname, Optional<String> erklaerung) {
        HBox zeile = new HBox(6, bedienelement, beiwerk(feldname, erklaerung));
        zeile.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bedienelement, Priority.ALWAYS);
        if (bedienelement instanceof Region breit) {
            breit.setMaxWidth(Double.MAX_VALUE);
        }
        return zeile;
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
    public Node bedienelement(Node feld) {
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
            case Spinner<?> zaehler -> zeichenkette(zaehler.getValue());
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

    private Node bedienelementFuer(TagList beschreibung, List<String> auswahl) {
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
            case DATE, TIME -> bausteine.createDatePicker();
            default -> bausteine.createTextField();
        };
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
     * <p>Das Feld bleibt bewusst beschreibbar, in beiden Faellen. Ein Vorschlag
     * darf nichts ausschliessen: kaeme ein Leistungsbereich mit einem anderen
     * Code hinzu oder gaelte einmal ein anderer Umsatzsteuersatz, waere ein
     * gesperrtes Feld eine Sackgasse - und die Codeliste laesst sich schneller
     * falsch pflegen als ein Vertrag geschlossen ist.</p>
     */
    private Node vorschlagsfeld(List<String> auswahl, int grenze) {
        if (auswahl.size() == 1) {
            TextField feld = textfeld(grenze);
            feld.setText(auswahl.get(0));
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
