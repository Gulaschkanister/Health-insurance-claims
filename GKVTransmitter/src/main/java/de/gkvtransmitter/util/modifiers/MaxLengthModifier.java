package de.gkvtransmitter.util.modifiers;

import de.gkvtransmitter.enums.Modifier;
import de.gkvtransmitter.util.ModifierInstance;

public class MaxLengthModifier extends ModifierInstance {

    private final int maxLength;

    public MaxLengthModifier(int maxLength) {
        super(Modifier.MAX_LENGTH);
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }
}
