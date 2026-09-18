package de.gkvtransmitter.presentation.controller;

import java.util.function.Function;

import de.gkvtransmitter.presentation.UiFactory;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.Node;

/**
 * Womit ein Formular gebaut wird.
 *
 * <p>Vier Werte, die jedes Formular gleichermassen braucht und die nie
 * einzeln wechseln. Sie standen als vier von zwoelf Parametern im
 * Konstruktor von {@link EditFormController} - <b>zwoelf Parameter hintereinander
 * lassen sich an der Aufrufstelle nicht mehr lesen</b>, und zwei vertauschte
 * {@code Consumer<T>} faellt der Uebersetzer nicht auf.</p>
 *
 * @param bausteine    erzeugt die Bedienelemente
 * @param texte        die Beschriftungen
 * @param meldungen    wohin Erfolg und Fehler gehen
 * @param feldbau      baut ein Eingabefeld zu einem Feldnamen
 */
public record Formularbausteine(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
        Function<String, Node> feldbau) {
}
