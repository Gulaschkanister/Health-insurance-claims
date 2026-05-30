package de.gkvtransmitter.dispatch;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class DispatchBatch {
    private final int kassenIk;
    private final List<Path> files;

    public DispatchBatch(int kassenIk, List<Path> files) {
        this.kassenIk = kassenIk;
        this.files = List.copyOf(files);
    }

    public int getKassenIk() {
        return kassenIk;
    }

    public List<Path> getFiles() {
        return Collections.unmodifiableList(files);
    }
}