package de.gkvtransmitter.anmeldung;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.entity.ServiceProvider;

/**
 * Das Registrierungsblatt.
 *
 * <p>Es ist kein Antrag: Anlage 1, Abschnitt 2 Absatz 1 gibt kein Formular vor,
 * sondern verlangt eine Abstimmung zwischen Absender und Empfaenger. Geprueft
 * wird deshalb nicht eine Form, sondern zweierlei - dass alles darin steht, was
 * die Anwendung weiss, und <b>dass die Luecken darin stehen</b>.</p>
 */
@DisplayName("Das Registrierungsblatt")
class RegistrierungsblattTest {

    private static final LocalDate STAND = LocalDate.of(2026, 9, 17);

    private static Betriebsdaten betrieb() {
        Betriebsdaten betrieb = new Betriebsdaten();
        betrieb.setPraxisname("Hebammenpraxis Bremen");
        betrieb.setStrasse("Kurweg");
        betrieb.setHausnummer("3");
        betrieb.setPlz("28203");
        betrieb.setOrt("Bremen");
        betrieb.setIk("261914007");
        betrieb.setSteuernummer("60/123/45678");
        betrieb.setAnsprechpartner("Maria Hebamme");
        betrieb.setTelefon("0421 123456");
        betrieb.setEmail("praxis@example.de");
        betrieb.setZertifikatBis("2027-03-31");
        return betrieb;
    }

    private static ServiceProvider erbringer() {
        ServiceProvider person = new ServiceProvider("Maria", "Hebamme", "Kurweg", "DE", "3",
                28203, 261914007, 101560000, LocalDate.of(1980, 1, 9));
        person.setAbrechnungscode("50");
        person.setSteuernummer("60/123/45678");
        person.setUmsatzsteuerbefreit(true);
        return person;
    }

    private static String blatt(Betriebsdaten betrieb, ServiceProvider... erbringer) {
        return new Registrierungsblatt(betrieb, List.of(erbringer), STAND).alsText();
    }

    @Test
    @DisplayName("nennt Betrieb, Kennzeichen und Erreichbarkeit")
    void nenntDenBetrieb() {
        String blatt = blatt(betrieb(), erbringer());

        assertTrue(blatt.contains("Hebammenpraxis Bremen"), blatt);
        assertTrue(blatt.contains("261914007"), blatt);
        assertTrue(blatt.contains("praxis@example.de"), blatt);
        assertTrue(blatt.contains("2027-03-31"), "Das Ablaufdatum des Zertifikats wird gefragt");
    }

    /**
     * Der Abrechnungscode kommt mit seinem Sammelgruppenschluessel.
     *
     * <p>Beides wird gefragt, und der Buchstabe laesst sich aus dem Code
     * ableiten (Anlage 3, Abschnitt 8.1.14). Wer ihn von Hand nachschlagen
     * muesste, schlaegt ihn falsch nach - das Projekt stand ein halbes Jahr
     * auf {@code H} statt {@code F}.</p>
     */
    @Test
    @DisplayName("nennt zum Abrechnungscode den Leistungsbereich")
    void nenntDenLeistungsbereich() {
        assertTrue(blatt(betrieb(), erbringer()).contains("50 (Leistungsbereich F)"),
                blatt(betrieb(), erbringer()));
    }

    @Test
    @DisplayName("nennt die Version, mit der gesendet wird")
    void nenntDieVersion() {
        String blatt = blatt(betrieb(), erbringer());

        assertTrue(blatt.contains("SLGA:21:0:0"), blatt);
        assertTrue(blatt.contains("UNOC:3"), blatt);
    }

    @Test
    @DisplayName("meldet bei vollstaendigen Angaben keine Luecke")
    void keineLuecken() {
        assertTrue(new Registrierungsblatt(betrieb(), List.of(erbringer()), STAND)
                .fehlendeAngaben().isEmpty());
    }

    /**
     * Fehlende Angaben stehen drin.
     *
     * <p>Eine Zusammenstellung, die Luecken verschweigt, sieht vollstaendig
     * aus und ist es nicht. Das ist der Abschnitt, wegen dem das Blatt
     * nuetzlich ist.</p>
     */
    @Test
    @DisplayName("nennt jede fehlende Angabe beim Namen")
    void nenntLuecken() {
        Betriebsdaten ohneSteuernummer = betrieb();
        ohneSteuernummer.setSteuernummer(null);
        ServiceProvider ohneCode = erbringer();
        ohneCode.setAbrechnungscode(null);

        List<String> fehlend =
                new Registrierungsblatt(ohneSteuernummer, List.of(ohneCode), STAND).fehlendeAngaben();

        assertTrue(fehlend.stream().anyMatch(luecke -> luecke.contains("Steuernummer")), "" + fehlend);
        assertTrue(fehlend.stream().anyMatch(luecke -> luecke.contains("Abrechnungscode")), "" + fehlend);
        assertTrue(blatt(ohneSteuernummer, ohneCode).contains("WAS NOCH FEHLT"));
    }

    /**
     * Ein fehlender Wert wird zur Linie, nicht zur Leerstelle.
     *
     * <p>Wer das Blatt vor sich hat, soll sehen, <em>dass</em> dort etwas
     * hingehoert. Ein weggelassenes Feld faellt niemandem auf.</p>
     */
    @Test
    @DisplayName("laesst Platz zum Eintragen, statt die Zeile wegzulassen")
    void luecheAlsLinie() {
        Betriebsdaten ohneTelefon = betrieb();
        ohneTelefon.setTelefon(null);

        String blatt = blatt(ohneTelefon, erbringer());

        assertTrue(blatt.contains("Telefon"), "Die Zeile bleibt stehen");
        assertTrue(blatt.contains("____"), "Und traegt eine Linie:\n" + blatt);
    }

    /** Ohne Betriebsdaten bricht nichts ab - das Blatt sagt, dass sie fehlen. */
    @Test
    @DisplayName("kommt ohne Betriebsdaten aus")
    void ohneBetriebsdaten() {
        String blatt = new Registrierungsblatt(null, List.of(), STAND).alsText();

        assertTrue(blatt.contains("keine Betriebsdaten erfasst"), blatt);
        assertFalse(new Registrierungsblatt(null, List.of(), STAND).fehlendeAngaben().isEmpty());
    }
}
