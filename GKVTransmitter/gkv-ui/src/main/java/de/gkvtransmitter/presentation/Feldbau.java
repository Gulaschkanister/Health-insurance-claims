package de.gkvtransmitter.presentation;

import java.math.BigDecimal;
import java.util.Objects;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.FieldValidator;
import de.gkvtransmitter.util.ModifierInstance;
import de.gkvtransmitter.util.TagList;
import de.gkvtransmitter.util.modifiers.MaxLengthModifier;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;

/**
 * Baut die Eingabefelder der Formulare und liest sie wieder aus.
 *
 * <p>Herausgeloest aus {@code View}, wo dieser Teil zwischen den Masken lag
 * und von allen gebraucht wurde: von den Personenformularen ebenso wie von der
 * Blaupausenmaske. Er gehoert keiner der beiden, sondern beiden.</p>
 *
 * <p>Welche Art Feld entsteht, entscheidet die Angabe aus
 * {@code /tags/person-tags.json}: eine Zeichenkette wird ein Textfeld, eine
 * Zahl ein Zaehler, ein Datum ein Kalenderfeld. Zahlenfelder tragen eine
 * Beschriftung fuer die Beanstandung unter sich - deshalb liefert
 * {@link #erzeugeFeld} einen {@code VBox} und nicht das Bedienelement selbst,
 * und deshalb sieht {@link #textVon} in dessen erstes Kind.</p>
 */
public class Feldbau {

    /** Stilklasse eines Feldes, dessen Inhalt beanstandet wurde. */
    private static final String FEHLERHAFT = "feld-fehlerhaft";

    private final UiFactory bausteine;
    private final AppMessages texte;

    public Feldbau(UiFactory bausteine, AppMessages texte) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
    }
    /**
     * Erstellt ein Eingabefeld basierend auf der TagList-Konfiguration für ein
     * bestimmtes Feld.
     *
     * @param fieldName - der Name des Feldes, für das das Eingabefeld erstellt
     * werden soll
     * @param tagList - die TagList, die die Konfiguration für das Feld enthält,
     * einschließlich der InputOption und möglicher Modifier
     * @return das erstellte Node, das als Eingabefeld für das angegebene Feld
     * verwendet werden kann
     */
    public Node erzeugeFeld(String fieldName, TagList tagList) {
        if (tagList == null) {
            return bausteine.createTextField();
        }

        InputOption inputOption = tagList.getInputOption();
        return switch (inputOption) {
            case STRING -> {
                TextField tf = bausteine.createTextField();
                applyMaxLengthModifier(tf, tagList);
                yield tf;
            }
            case NUMBER -> {
                Spinner<Integer> spinner = bausteine.createSpinner(Integer.class, null, inputOption);
                yield createValidatedSpinnerNode(fieldName, spinner, inputOption, "Integer");
            }
            case PERCENT, COST -> {
                Spinner<BigDecimal> spinner = bausteine.createSpinner(BigDecimal.class, null, inputOption);
                yield createValidatedSpinnerNode(fieldName, spinner, inputOption, "BigDecimal");
            }
            case CODE ->
                bausteine.createComboBox(false);
            case NUMBER_SUGGESTION ->
                bausteine.createComboBox(true);
            case DATE, TIME ->
                bausteine.createDatePicker();
            default ->
                bausteine.createTextField();
        };
    }

    /**
     * Erstellt ein validiertes Spinner-Node mit einem Fehlerlabel, das die
     * Eingabe basierend auf dem Feldnamen, der InputOption und dem Java-Feldtyp
     * validiert.
     *
     * @param fieldName - der Name des Feldes, das validiert werden soll, z.B.
     * "plz" für Postleitzahl
     * @param spinner - der Spinner, der validiert werden soll
     * @param inputOption - die InputOption, die den Typ der Eingabe angibt,
     * z.B. NUMBER oder PERCENT
     * @param javaFieldType - der Java-Typ des Feldes, z.B. "Integer" oder
     * "BigDecimal", der für die Validierung berücksichtigt werden kann
     * @return ein Node, das den Spinner und ein Fehlerlabel enthält, das die
     * Validierungsergebnisse anzeigt
     */
    private Node createValidatedSpinnerNode(String fieldName, Spinner<?> spinner,
            InputOption inputOption, String javaFieldType) {
        Label errorLabel = bausteine.createLabel("");
        errorLabel.getStyleClass().add("feld-fehler");
        errorLabel.setVisible(false);

        VBox box = new VBox(4);
        box.getChildren().addAll(spinner, errorLabel);

        Runnable validate = () -> validateSpinner(spinner, fieldName, errorLabel, inputOption, javaFieldType);

        spinner.valueProperty().addListener((obs, o, n) -> validate.run());
        if (spinner.getEditor() != null) {
            spinner.getEditor().textProperty().addListener((obs, o, n) -> validate.run());
            spinner.getEditor().focusedProperty().addListener((obs, oldF, newF) -> {
                if (!newF) {
                    validate.run();
                }
            });
        }

        validate.run();
        return box;
    }

    /**
     * Erstellt ein Eingabefeld basierend auf der InputOption und anderen
     * Parametern, die in der TagList definiert sind.
     *
     * @param spinner - der Spinner, der validiert werden soll
     * @param fieldName - der Name des Feldes, das validiert werden soll, z.B.
     * "plz" für Postleitzahl
     * @param errorLabel - das Label, das Fehlermeldungen anzeigt, wenn die
     * Validierung fehlschlägt
     * @param inputOption - die InputOption, die den Typ der Eingabe angibt,
     * z.B. NUMBER oder PERCENT
     * @param javaFieldType - der Java-Typ des Feldes, z.B. "Integer" oder
     * "BigDecimal", der für die Validierung berücksichtigt werden kann
     */
    private void validateSpinner(Spinner<?> spinner, String fieldName, Label errorLabel,
            InputOption inputOption, String javaFieldType) {
        String valText = "";
        try {
            if (spinner.getEditor() != null && !spinner.getEditor().getText().isBlank()) {
                valText = spinner.getEditor().getText();
            } else if (spinner.getValue() != null) {
                valText = String.valueOf(spinner.getValue());
            }
        } catch (Exception ignored) {
        }

        if ("plz".equalsIgnoreCase(fieldName) && !valText.isBlank()) {
            if (!valText.matches("\\d{5}")) {
                errorLabel.setText("PLZ muss 5-stellig sein");
                errorLabel.setVisible(true);
                beanstande(spinner);
                return;
            }
        }

        if (spinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intVf) {
            try {
                int min = intVf.getMin();
                int max = intVf.getMax();
                if (!valText.isBlank()) {
                    int cur = Integer.parseInt(valText);
                    if (cur < min || cur > max) {
                        errorLabel.setText(String.format("Wert muss zwischen %d und %d liegen", min, max));
                        errorLabel.setVisible(true);
                        beanstande(spinner);
                        return;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        FieldValidator.Feldbefund res = FieldValidator.validate(fieldName, valText, inputOption, javaFieldType);
        if (res == null || res.isValid) {
            errorLabel.setVisible(false);
            entlaste(spinner);
        } else {
            errorLabel.setText(res.errorMessage != null ? res.errorMessage : "Ungültiger Wert");
            errorLabel.setVisible(true);
            beanstande(spinner);
        }
    }

    /** Kennzeichnet ein Feld als beanstandet. */
    private void beanstande(Spinner<?> spinner) {
        if (!spinner.getStyleClass().contains(FEHLERHAFT)) {
            spinner.getStyleClass().add(FEHLERHAFT);
        }
    }

    /** Nimmt die Kennzeichnung wieder weg. */
    private void entlaste(Spinner<?> spinner) {
        spinner.getStyleClass().remove(FEHLERHAFT);
    }

    /**
     * Wendet den MaxLengthModifier aus der TagList auf ein TextField an, um die
     * maximale Länge der Eingabe zu begrenzen.
     *
     * @param textField das TextField, auf das der MaxLengthModifier angewendet
     * werden soll
     * @param tagList die TagList, die die Modifier enthält, einschließlich des
     * MaxLengthModifier
     */
    private void applyMaxLengthModifier(TextField textField, TagList tagList) {
        for (ModifierInstance modifier : tagList.getModifierList()) {
            if (modifier instanceof MaxLengthModifier mmod) {
                int maxLength = mmod.getMaxLength();
                textField.setTextFormatter(new TextFormatter<>(change -> {
                    if (change.getControlNewText().length() <= maxLength) {
                        return change;
                    }
                    return null;
                }));
            }
        }
    }

    /**
     * Gibt den Textwert eines UI-Elements zurück, abhängig von dessen Typ. Wenn
     * das Element in einer VBox verpackt ist, wird das erste Kind der VBox als
     * Ziel für die Textgewinnung verwendet. Unterstützt verschiedene
     * UI-Komponenten wie TextInputControl, Spinner, ComboBox und DatePicker, um
     * den entsprechenden Textwert zurückzugeben. Wenn der Typ des UI-Elements
     * nicht erkannt wird oder kein Text extrahiert werden kann, wird ein leerer
     * String zurückgegeben.
     *
     * @param node das UI-Element, aus dem der Textwert extrahiert werden soll,
     * z.B. ein TextField, Spinner, ComboBox oder DatePicker
     * @return der Textwert des UI-Elements oder ein leerer String, wenn kein
     * Text extrahiert werden kann
     */
    public String textVon(Node node) {
        Node target = node;
        if (node instanceof VBox v && !v.getChildren().isEmpty()) {
            target = v.getChildren().get(0);
        }

        switch (target) {
            case TextInputControl tic -> {
                return tic.getText();
            }
            case Spinner<?> spinner -> {
                Object value = spinner.getValue();
                return value != null ? String.valueOf(value) : "";
            }
            case ComboBox<?> cb -> {
                Object value = cb.getValue();
                return value != null ? String.valueOf(value) : "";
            }
            case DatePicker dp -> {
                var value = dp.getValue();
                return value != null ? value.toString() : "";
            }
            default -> {
            }
        }
        return "";
    }
}
