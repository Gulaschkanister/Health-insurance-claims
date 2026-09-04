package de.gkvtransmitter.util;

import java.util.List;

import de.gkvtransmitter.enums.InputOption;
import lombok.Getter;

@Getter
public class TagList {

    private final InputOption inputOption;
    private final List<ModifierInstance> modifierList;

    public TagList(InputOption inputOption, List<ModifierInstance> modifierList) {
        this.inputOption = inputOption;
        this.modifierList = modifierList;
    }
}
