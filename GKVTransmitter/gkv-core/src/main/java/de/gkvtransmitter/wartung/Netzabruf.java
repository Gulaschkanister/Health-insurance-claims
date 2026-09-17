package de.gkvtransmitter.wartung;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Holt eine Seite - einmal, mit Zeitgrenze, ohne Bibliothek.
 *
 * <p>{@code java.net.http.HttpClient} gehoert seit Java 11 zum Sprachumfang.
 * Eine neue Abhaengigkeit fuer einen einzigen GET waere die falsche
 * Entscheidung; das Projekt haelt Block G ausdruecklich "ohne zusaetzliche
 * Elemente".</p>
 *
 * <p><b>Die Zeitgrenzen sind das Wichtige.</b> Ohne sie haengt die Oberflaeche
 * an einem Server, der nicht antwortet - und dann steht die Abrechnung still,
 * weil jemand auf "Auf neue Unterlagen pruefen" geklickt hat. Zehn Sekunden
 * sind lang genug fuer eine langsame Leitung und kurz genug, um nicht als
 * Absturz zu wirken.</p>
 */
public final class Netzabruf implements Unterlagenpruefung.Seitenabruf {

    /** Wie lange auf die Verbindung gewartet wird. */
    private static final Duration VERBINDUNG = Duration.ofSeconds(10);
    /** Wie lange auf die Antwort gewartet wird. */
    private static final Duration ANTWORT = Duration.ofSeconds(10);

    @Override
    public String lies(String adresse) throws Exception {
        HttpClient klient = HttpClient.newBuilder()
                .connectTimeout(VERBINDUNG)
                // Die Seiten liegen hinter Weiterleitungen; ohne dies kaeme
                // ein leerer Rumpf mit Statuscode 301 zurueck.
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest anfrage = HttpRequest.newBuilder(URI.create(adresse))
                .timeout(ANTWORT)
                .header("Accept", "text/html")
                .GET()
                .build();
        HttpResponse<String> antwort = klient.send(anfrage, HttpResponse.BodyHandlers.ofString());
        if (antwort.statusCode() / 100 != 2) {
            throw new java.io.IOException("Die Seite antwortete mit " + antwort.statusCode() + ".");
        }
        return antwort.body();
    }
}
