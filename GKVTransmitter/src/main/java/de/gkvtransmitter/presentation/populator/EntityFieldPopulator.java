package de.gkvtransmitter.presentation.populator;

import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;

/**
 * Abstrakte Basis für die Populierung von Formularfeldern mit Entity-Daten.
 *
 * Diese Klasse definiert die Schnittstelle und gemeinsame Logik zum Laden von
 * Entity-Feldern in JavaFX-UI-Elemente und zum Speichern von Werten zurück.
 *
 * @param <T> Der Entity-Typ (z.B. Patient, ServiceProvider)
 */
public abstract class EntityFieldPopulator<T> {

    /**
     * Populiert ein Formularfeld mit Daten aus einer Entity.
     *
     * @param field Das UI-Feld (TextField, DatePicker, Spinner, etc.)
     * @param fieldName Der Name des Feldes
     * @param entity Die Entity mit den Daten
     */
    public void populateField(Node field, String fieldName, T entity) {
        Node target = unwrapField(field);

        // Spezialbehandlung für Geburtsdatum
        if (isDateField(fieldName)) {
            populateDateField(target, fieldName, entity);
            return;
        }

        String value = getFieldValue(fieldName, entity);
        if (value == null || value.isBlank()) {
            return;
        }

        if (target instanceof TextInputControl textInput) {
            textInput.setText(value);
        } else if (target instanceof Spinner<?> spinner) {
            populateSpinner(spinner, value);
        }
    }

    /**
     * Extrahiert den Wert aus einem Formularfeld zurück in die Entity.
     *
     * @param field Das UI-Feld
     * @param fieldName Der Feldname
     * @param entity Die Ziel-Entity
     */
    public void extractToEntity(Node field, String fieldName, T entity) {
        Node target = unwrapField(field);
        String value = extractFieldValue(target);
        setEntityFieldValue(fieldName, entity, value);
    }

    /**
     * Gibt den Wert eines Entity-Feldes als String zurück.
     *
     * @param fieldName Der Feldname
     * @param entity Die Entity
     * @return Der Feldwert als String
     */
    protected abstract String getFieldValue(String fieldName, T entity);

    /**
     * Setzt einen Feldwert auf eine Entity.
     *
     * @param fieldName Der Feldname
     * @param entity Die Entity
     * @param value Der zu setzende Wert
     */
    protected abstract void setEntityFieldValue(String fieldName, T entity, String value);

    /**
     * Gibt die Anzeigename der Entity zurück (für Dropdowns, etc.).
     *
     * @param entity Die Entity
     * @return Anzeigename (z.B. "Max Mustermann (ID: 42)")
     */
    public abstract String getDisplayName(T entity);

    /**
     * Gibt die eindeutige ID der Entity zurück.
     *
     * @param entity Die Entity
     * @return Die ID
     */
    public abstract Object getId(T entity);

    /**
     * Prüft, ob ein Feldname einem Datumfeld entspricht.
     *
     * @param fieldName Der Feldname
     * @return true wenn es ein Datumfeld ist
     */
    protected boolean isDateField(String fieldName) {
        return "birthDate".equalsIgnoreCase(fieldName) || "birthdate".equalsIgnoreCase(fieldName);
    }

    /**
     * Packt ein Feld aus, das in einer VBox verpackt sein könnte.
     *
     * @param field Das möglicherweise verpackte Feld
     * @return Das eigentliche UI-Element
     */
    private Node unwrapField(Node field) {
        if (field instanceof VBox v && !v.getChildren().isEmpty()) {
            return v.getChildren().get(0);
        }
        return field;
    }

    /**
     * Extrahiert einen Wert aus einem UI-Feld.
     *
     * @param target Das UI-Feld
     * @return Der Feldwert
     */
    private String extractFieldValue(Node target) {
        if (target instanceof TextInputControl textInput) {
            return textInput.getText();
        }

        if (target instanceof Spinner<?> spinner) {
            Object value = spinner.getValue();
            return value != null ? value.toString() : "";
        }

        if (target instanceof DatePicker dp) {
            var value = dp.getValue();
            return value != null ? value.toString() : "";
        }
        return "";
    }

    /**
     * Populiert ein Datumfeld mit Entity-Daten.
     *
     * @param target Das Ziel-UI-Element
     * @param fieldName Der Feldname
     * @param entity Die Entity
     */
    protected void populateDateField(Node target, String fieldName, T entity) {
        var dateValue = getDateFieldValue(fieldName, entity);
        
        if (dateValue == null) {
            return;
        }

        if (target instanceof DatePicker dp) {
            dp.setValue(dateValue);
        } else if (target instanceof TextInputControl tic) {
            tic.setText(dateValue.toString());
        }
    }

    /**
     * Gibt einen LocalDate-Wert aus der Entity zurück.
     * Muss von Subklassen überschrieben werden, die Datumfelder unterstützen.
     *
     * @param fieldName Der Feldname
     * @param entity Die Entity
     * @return Das Datum oder null
     */
    protected java.time.LocalDate getDateFieldValue(String fieldName, T entity) {
        return null;
    }

    /**
     * Populiert einen Spinner mit einem String-Wert.
     *
     * @param spinner Der Spinner
     * @param value Der zu setzende Wert
     */
    private void populateSpinner(Spinner<?> spinner, String value) {
        try {
            if (spinner.getValueFactory() instanceof javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory intVf) {
                int intValue = Integer.parseInt(value);
                intVf.setValue(intValue);
            }
        } catch (NumberFormatException ignored) {
            // Ignoriere Konvertierungsfehler
        }
    }
}
