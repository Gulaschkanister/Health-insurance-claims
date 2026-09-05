package de.gkvtransmitter.presentation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

/**
 * Startet die JavaFX-Laufzeit fuer Tests und fuehrt Code auf deren Faden aus.
 *
 * <p>Bedienelemente lassen sich nur erzeugen, wenn die Laufzeit steht, und
 * veraendern lassen sie sich nur auf ihrem eigenen Faden. Beides erledigt
 * diese Klasse.</p>
 *
 * <p>Bewusst ohne TestFX: dessen Wert liegt im Nachstellen echter Maus- und
 * Tastatureingaben. Seit die Dialoge hinter {@code Dialoge} liegen, blockiert
 * nichts mehr, und die Masken lassen sich unmittelbar aufbauen und auswerten.
 * Damit entfaellt der Roboter - und mit ihm die Abhaengigkeit von Monocle, das
 * jeder JavaFX-Fassung hinterherhinkt.</p>
 *
 * <p>Auf einem Rechner ohne Bildschirm - etwa im Bauknecht - braucht die
 * Laufzeit dennoch eine Anzeige. Unter Linux leistet das {@code xvfb-run};
 * siehe {@code .github/workflows/build.yml}.</p>
 */
public final class JavaFxLaufzeit {

    /** Wartezeit fuer einen Durchgang. Reichlich bemessen, damit ein langsamer Rechner nicht scheitert. */
    private static final long ZEITGRENZE_SEKUNDEN = 30;

    private static boolean gestartet;

    private JavaFxLaufzeit() {
    }

    /** Faehrt die Laufzeit hoch, einmal je Testlauf. */
    public static synchronized void starten() {
        if (gestartet) {
            return;
        }
        CountDownLatch bereit = new CountDownLatch(1);
        try {
            Platform.startup(bereit::countDown);
        } catch (IllegalStateException bereitsGestartet) {
            // Eine andere Testklasse war schneller. Kein Fehler.
            bereit.countDown();
        }
        warte(bereit, "Die JavaFX-Laufzeit ist nicht hochgefahren");
        // Ohne das faehrt die Laufzeit herunter, sobald das letzte Fenster
        // geschlossen ist - und dann scheitert jeder folgende Test.
        Platform.setImplicitExit(false);
        gestartet = true;
    }

    /**
     * Fuehrt die Arbeit auf dem JavaFX-Faden aus und wartet, bis sie fertig
     * ist.
     *
     * <p>Ausnahmen werden auf den aufrufenden Faden zurueckgereicht. Ohne das
     * verschwaende eine fehlgeschlagene Zusicherung im Nichts und der Test
     * gaelte als bestanden.</p>
     */
    public static void aufFxFaden(Runnable arbeit) {
        starten();
        AtomicReference<Throwable> fehler = new AtomicReference<>();
        CountDownLatch fertig = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                arbeit.run();
            } catch (Throwable t) {
                fehler.set(t);
            } finally {
                fertig.countDown();
            }
        });
        warte(fertig, "Die Arbeit auf dem JavaFX-Faden wurde nicht fertig");
        reicheWeiter(fehler.get());
    }

    private static void warte(CountDownLatch riegel, String was) {
        try {
            if (!riegel.await(ZEITGRENZE_SEKUNDEN, TimeUnit.SECONDS)) {
                throw new IllegalStateException(was + " (Zeitgrenze " + ZEITGRENZE_SEKUNDEN + "s)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(was + " (unterbrochen)", e);
        }
    }

    private static void reicheWeiter(Throwable fehler) {
        if (fehler == null) {
            return;
        }
        if (fehler instanceof Error error) {
            throw error;
        }
        if (fehler instanceof RuntimeException laufzeitfehler) {
            throw laufzeitfehler;
        }
        throw new IllegalStateException(fehler);
    }
}
