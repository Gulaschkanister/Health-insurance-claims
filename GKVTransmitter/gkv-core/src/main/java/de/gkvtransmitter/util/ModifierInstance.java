package de.gkvtransmitter.util;

import de.gkvtransmitter.enums.Modifier;
import lombok.Getter;

@Getter
public abstract class ModifierInstance {

    private final Modifier modifier;

    protected ModifierInstance(Modifier modifier) {
        this.modifier = modifier;
    }
}
