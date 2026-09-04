package de.gkvtransmitter.util.modifiers;

import de.gkvtransmitter.enums.Modifier;
import de.gkvtransmitter.util.ModifierInstance;

public class DecimalPlaceModifier extends ModifierInstance {

    private final int decimalPlaces;

    public DecimalPlaceModifier(int decimalPlaces) {
        super(Modifier.DECIMAL_PLACE);
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
