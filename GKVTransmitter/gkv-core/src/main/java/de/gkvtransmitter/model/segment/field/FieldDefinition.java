package de.gkvtransmitter.model.segment.field;

import de.gkvtransmitter.enums.InputOption;
import lombok.Getter;

/**
 * Diese Klasse soll spaeter die Regeln fuer ein einzelnes Feld wie Typ,
 * Pflichtstatus und Laengenbegrenzung beschreiben.
 */
@Getter
public class FieldDefinition {

    private final int position;
    private final FieldType type;
    private final boolean isMandatory;
    private final int maxLength;
    private final String name;
    private final InputOption inputType;
    private final boolean internal;
    private final PersonRole personRole;
    /** Was in das Feld gehoert, aus der Segmentdefinition. Kann leer sein. */
    private final String beschreibung;

    /**
     * Ein Feld einer Segmentdefinition.
     *
     * <p><b>Hier standen vier Konstruktoren</b>, der breiteste mit neun
     * Parametern - und <b>drei davon hatte nie jemand aufgerufen</b>. Sie
     * hielten die neun Parameter am Leben, ohne dass etwas davon gebraucht
     * wurde. Die vier Angaben zur Form des Wertes stehen jetzt in
     * {@link Feldform}.</p>
     *
     * @param position     Stelle im Segment, ab 1
     * @param name         Feldname aus der Segmentdefinition
     * @param form         was fuer ein Wert hineingehoert
     * @param internal     {@code true}, wenn die Anwendung das Feld selbst
     *                     setzt und es nicht im Blaupausenformular erscheint
     * @param personRole   welche Person das Feld betrifft, oder {@code null}
     * @param beschreibung was hineingehoert, aus der Segmentdefinition
     */
    public FieldDefinition(int position, String name, Feldform form, boolean internal,
            PersonRole personRole, String beschreibung) {
        this.position = position;
        this.type = form.type();
        this.isMandatory = form.isMandatory();
        this.maxLength = form.maxLength();
        this.name = name;
        this.inputType = form.inputType();
        this.internal = internal;
        this.personRole = personRole;
        this.beschreibung = beschreibung == null ? "" : beschreibung;
    }
}
