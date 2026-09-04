package de.gkvtransmitter;

/**
 * Haelt den klassischen Main-Einstiegspunkt getrennt von der eigentlichen
 * JavaFX-Application-Klasse.
 *
 * <p>Die Klasse darf nicht von {@code Application} erben. Nur so laesst sich
 * JavaFX ueber den Klassenpfad starten, ohne dass die Laufzeit "JavaFX runtime
 * components are missing" meldet.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
