package de.gkvtransmitter.presentation.controller;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.gkvtransmitter.presentation.populator.EntityFieldPopulator;

/**
 * Woran ein Formular arbeitet und was es damit tun darf.
 *
 * <p>Sechs Werte, die zusammen eine Entitaetsart beschreiben. Sie standen
 * einzeln im Konstruktor von {@link EditFormController}; <b>drei davon waren
 * {@code Consumer<T>} und {@code Supplier<List<T>>}</b> - vertauscht man zwei,
 * uebersetzt es und tut etwas anderes.</p>
 *
 * @param felder     traegt Werte ins Formular und wieder heraus
 * @param laden      woher die Eintraege kommen
 * @param speichern  wohin ein geaenderter Eintrag geht
 * @param loeschen   wie ein Eintrag entfernt wird
 * @param typname    fuer die Textschluessel, etwa {@code Patient}
 * @param mitLoeschen ob das Formular eine Loeschen-Schaltflaeche zeigt
 * @param <T>        der Entitaetstyp
 */
public record Entitaetszugriff<T>(EntityFieldPopulator<T> felder, Supplier<List<T>> laden,
        Consumer<T> speichern, Consumer<T> loeschen, String typname, boolean mitLoeschen) {
}
