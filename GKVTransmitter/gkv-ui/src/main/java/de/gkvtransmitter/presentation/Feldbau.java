package de.gkvtransmitter.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.FieldValidator;
import de.gkvtransmitter.util.Institutionskennzeichen;
import de.gkvtransmitter.util.ModifierInstance;
import de.gkvtransmitter.util.TagList;
import de.gkvtransmitter.util.modifiers.MaxLengthModifier;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
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
        Node bedienelement = bedienelementFuer(beschreibung);

        Label beanstandung = bausteine.createLabel("");
        beanstandung.getStyleClass().add(STIL_FEHLER);
        beanstandung.setWrapText(true);
        verbergen(beanstandung);

        VBox feld = new VBox(3, bedienelement);
        erklaerung(feldname, beschreibung).ifPresent(text -> {
            Label hinweis = bausteine.createLabel(text);
            hinweis.getStyleClass().add(STIL_HINWEIS);
            hinweis.setWrapText(true);
            feld.getChildren().add(hinweis);
        });
        feld.getChildren().add(beanstandung);

        ueberwache(feldname, beschreibung, bedienelement, beanstandung);
        return feld;
    }

    /** Das Bedienelement eines Feldes, ohne Erklaerung und Beanstandung. */
    public Node bedienelement(Node feld) {
        if (feld instanceof VBox huelle && !huelle.getChildren().isEmpty()) {
            return huelle.getChildren().get(0);
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

    private Node bedienelementFuer(TagList beschreibung) {
        if (beschreibung == null) {
            return bausteine.createTextField();
        }
        InputOption art = beschreibung.getInputOption();
        return switch (art) {
            case STRING -> {
                TextField feld = bausteine.createTextField();
                hoechstlaenge(beschreibung).ifPresent(grenze -> begrenze(feld, grenze));
                yield feld;
            }
            case NUMBER -> zaehler(Integer.class, art);
            case PERCENT, COST -> zaehler(BigDecimal.class, art);
            case CODE -> bausteine.createComboBox(false);
            case NUMBER_SUGGESTION -> bausteine.createComboBox(true);
            case DATE, TIME -> bausteine.createDatePicker();
            default -> bausteine.createTextField();
        };
    }

    private <T> Spinner<T> zaehler(Class<T> art, InputOption eingabeart) {
        Spinner<T> zaehler = bausteine.createSpinner(art, null, eingabeart);
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
    private void ueberwache(String feldname, TagList beschreibung, Node bedienelement, Label beanstandung) {
        boolean[] schonBeanstandet = {false};
        Runnable pruefen = () -> {
            Optional<String> befund = pruefe(feldname, beschreibung, textVon(bedienelement));
            if (befund.isEmpty()) {
                beanstandung.setText("");
                verbergen(beanstandung);
                bedienelement.getStyleClass().remove(STIL_FEHLERHAFT);
                return;
            }
            schonBeanstandet[0] = true;
            beanstandung.setText(befund.get());
            zeigen(beanstandung);
            if (!bedienelement.getStyleClass().contains(STIL_FEHLERHAFT)) {
                bedienelement.getStyleClass().add(STIL_FEHLERHAFT);
            }
        };

        bedienelement.focusedProperty().addListener((wert, hatteFokus, hatFokus) -> {
            if (!hatFokus) {
                pruefen.run();
            }
        });

        switch (bedienelement) {
            case TextInputControl eingabe -> eingabe.textProperty()
                    .addListener((wert, alt, neu) -> nachbessern(schonBeanstandet, pruefen));
            case Spinner<?> zaehler -> {
                zaehler.valueProperty().addListener((wert, alt, neu) -> pruefen.run());
                if (zaehler.getEditor() != null) {
                    zaehler.getEditor().textProperty()
                            .addListener((wert, alt, neu) -> nachbessern(schonBeanstandet, pruefen));
                }
            }
            case ComboBox<?> auswahl -> auswahl.valueProperty().addListener((wert, alt, neu) -> pruefen.run());
            case DatePicker kalender -> kalender.valueProperty().addListener((wert, alt, neu) -> pruefen.run());
            default -> { }
        }
    }

    /** Waehrend des Tippens nur nachbessern, was schon beanstandet wurde. */
    private void nachbessern(boolean[] schonBeanstandet, Runnable pruefen) {
        if (schonBeanstandet[0]) {
            pruefen.run();
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
            return Institutionskennzeichen.istGueltig(bereinigt)
                    ? Optional.empty()
                    : Optional.of(texte.get("msg.invalidIk"));
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

        if (eigener.isPresent() && grenze.isPresent()) {
            return Optional.of(eigener.get() + " " + grenze.get());
        }
        return eigener.or(() -> grenze);
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
