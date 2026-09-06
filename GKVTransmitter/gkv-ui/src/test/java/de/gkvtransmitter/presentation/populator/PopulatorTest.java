package de.gkvtransmitter.presentation.populator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.presentation.Feldbau;
import de.gkvtransmitter.presentation.JavaFxLaufzeit;
import de.gkvtransmitter.presentation.JavaFxUiFactory;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.TagConfigLoader;
import de.gkvtransmitter.util.TagList;
import javafx.scene.Node;

/**
 * Prueft die Uebersetzer zwischen Eingabefeld und Person.
 *
 * <p>Sie waren bis zum 05.09.2026 die groesste ungedeckte Stelle im Projekt,
 * und die Uebergabe nannte auch den Grund: <b>ein Fehler dort schreibt einen
 * Wert in das falsche Feld, ohne dass etwas fehlschlaegt.</b> Genau das war zu
 * diesem Zeitpunkt der Fall - siehe {@link EntityFieldPopulator} - und kein
 * Test bemerkte es, weil alle Maskentests einen eigenen, einfachen Populator
 * benutzten.</p>
 *
 * <p>Die Felder werden deshalb hier mit dem <b>echten</b> {@code Feldbau}
 * gebaut, so wie die Maske sie baut. Ein Test, der sich sein Feld selbst
 * zusammensteckt, prueft nur seine eigene Vorstellung davon.</p>
 */
@DisplayName("Populatoren")
class PopulatorTest {

    /** Ein IK mit richtiger Pruefziffer. */
    private static final int IK = 108310400;
    private static final int KASSEN_IK = 102137985;

    private Feldbau feldbau;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        feldbau = new Feldbau(new JavaFxUiFactory(),
                new AppMessages("/messages/ui-messages.json"));
    }

    @Nested
    @DisplayName("Ein Teilnehmer")
    class Teilnehmer {

        @Test
        @DisplayName("kommt in jedes Feld und aus jedem Feld unveraendert zurueck")
        void hinUndZurueck() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                PatientFieldPopulator populator = new PatientFieldPopulator();
                Patient anna = teilnehmer();

                Patient zurueck = new Patient();
                for (String feldname : FELDER) {
                    Node feld = feld(feldname);
                    populator.populateField(feld, feldname, anna);
                    populator.extractToEntity(feld, feldname, zurueck);
                }

                assertEquals("Anna", zurueck.getFirstname());
                assertEquals("Berger", zurueck.getLastname());
                assertEquals("Hauptstrasse", zurueck.getStreet());
                assertEquals("12a", zurueck.getHousenumber());
                assertEquals("DE", zurueck.getCountry());
                assertEquals(28195, zurueck.getPlz());
                assertEquals(IK, zurueck.getIk());
                assertEquals(KASSEN_IK, zurueck.getKassenIk());
                assertEquals(LocalDate.of(1992, 4, 17), zurueck.getBirthDate());
            });
        }

        /**
         * Der Fehler, den die Uebergabe vorhergesagt hat.
         *
         * <p>"Ein Fehler dort schreibt einen Wert in das falsche Feld, ohne
         * dass etwas fehlschlaegt." Ein Test, der nur pruefte, dass ueberhaupt
         * gespeichert wird, faende das nicht. Dieser hier vertauscht nichts -
         * er prueft, dass nichts vertauscht ist.</p>
         */
        @Test
        @DisplayName("bekommt in jedem Feld genau seinen eigenen Wert zu sehen")
        void keineVerwechslung() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                PatientFieldPopulator populator = new PatientFieldPopulator();
                Patient anna = teilnehmer();

                assertEquals("Anna", angezeigt(populator, "firstname", anna));
                assertEquals("Berger", angezeigt(populator, "lastname", anna));
                assertEquals("Hauptstrasse", angezeigt(populator, "street", anna));
                assertEquals("12a", angezeigt(populator, "housenumber", anna));
                assertEquals("DE", angezeigt(populator, "country", anna));
                assertEquals("28195", angezeigt(populator, "plz", anna));
                assertEquals(String.valueOf(IK), angezeigt(populator, "ik", anna));
                assertEquals(String.valueOf(KASSEN_IK), angezeigt(populator, "kassenIk", anna));
            });
        }

        /**
         * Das eigentliche Bearbeiten.
         *
         * <p>Es geht dabei nicht darum, dass gespeichert wird, sondern dass
         * der <em>geaenderte</em> Wert ankommt. Genau hier lag der Fehler:
         * {@code extractToEntity} griff auf die Huelle statt auf das
         * Bedienelement, bekam eine leere Zeichenkette und liess die Person
         * unveraendert - waehrend die Oberflaeche "erfolgreich aktualisiert"
         * meldete.</p>
         */
        @Test
        @DisplayName("uebernimmt eine Aenderung am Feld auch wirklich")
        void aenderungKommtAn() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                PatientFieldPopulator populator = new PatientFieldPopulator();
                Patient anna = teilnehmer();
                Node feld = feld("lastname");
                populator.populateField(feld, "lastname", anna);

                ((javafx.scene.control.TextInputControl) Feldbau.bedienelement(feld))
                        .setText("Christiansen");
                populator.extractToEntity(feld, "lastname", anna);

                assertEquals("Christiansen", anna.getLastname(),
                        "Sonst meldet die Maske Erfolg und hat nichts geaendert");
            });
        }

        @Test
        @DisplayName("wird im Anzeigenamen mit seiner Nummer unterscheidbar")
        void anzeigename() {
            Patient anna = teilnehmer();
            anna.setId(7);

            String name = new PatientFieldPopulator().getDisplayName(anna);

            assertTrue(name.contains("Anna"), name);
            assertTrue(name.contains("Berger"), name);
            assertTrue(name.contains("7"), "Zwei Frauen koennen gleich heissen: " + name);
        }
    }

    @Nested
    @DisplayName("Ein Dienstleister")
    class Dienstleister {

        @Test
        @DisplayName("kommt in jedes Feld und aus jedem Feld unveraendert zurueck")
        void hinUndZurueck() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                ServiceProviderFieldPopulator populator = new ServiceProviderFieldPopulator();
                ServiceProvider maria = new ServiceProvider("Maria", "Hebamme", "Kurweg", "DE",
                        "3", 28203, IK, IK, LocalDate.of(1980, 1, 9));

                ServiceProvider zurueck = new ServiceProvider();
                for (String feldname : FELDER) {
                    Node feld = feld(feldname);
                    populator.populateField(feld, feldname, maria);
                    populator.extractToEntity(feld, feldname, zurueck);
                }

                assertEquals("Maria", zurueck.getFirstname());
                assertEquals("Hebamme", zurueck.getLastname());
                assertEquals("Kurweg", zurueck.getStreet());
                assertEquals(28203, zurueck.getPlz());
                assertEquals(IK, zurueck.getIk());
                assertEquals(LocalDate.of(1980, 1, 9), zurueck.getBirthDate());
            });
        }
    }

    @Nested
    @DisplayName("Randfaelle")
    class Randfaelle {

        /**
         * Ein Vertipper darf keine Angabe vernichten.
         *
         * <p>Der Populator laesst die Person unveraendert. Die Maske hat den
         * Vertipper ohnehin schon unter dem Feld beanstandet und speichert
         * nicht; ihn zusaetzlich auf 0 zu setzen hiesse, eine Angabe zu
         * verlieren, die noch da war.</p>
         *
         * <p><b>Der Wert ist bewusst dreistellig.</b> Hier stand am 05.09.2026
         * „keine Zahl" — zehn Zeichen, und das Feld laesst hoechstens fuenf zu.
         * Der Text kam nie im Feld an; geprueft wurde also nicht der unlesbare
         * Wert, sondern ein leeres Feld. Aufgefallen erst, als das leere Feld
         * ein anderes Ergebnis bekam.</p>
         */
        @Test
        @DisplayName("Ein nicht lesbarer Zahlenwert laesst das Feld unveraendert")
        void unlesbareZahl() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                PatientFieldPopulator populator = new PatientFieldPopulator();
                Patient anna = teilnehmer();
                Node feld = feld("plz");
                javafx.scene.control.TextInputControl eingabe =
                        (javafx.scene.control.TextInputControl) Feldbau.bedienelement(feld);
                eingabe.setText("abc");
                assertEquals("abc", eingabe.getText(), "Der Text muss ins Feld passen, sonst prueft das hier nichts");

                populator.extractToEntity(feld, "plz", anna);

                assertEquals(28195, anna.getPlz(), "Der alte Wert bleibt stehen");
            });
        }

        /**
         * Ein geleertes Feld muss ankommen — sonst greift das Tor nicht.
         *
         * <p>{@code setEntityFieldValue} stieg bis zum 06.09.2026 bei leerem
         * Text sofort aus. Wer in der Bearbeitung ein IK loeschte, umging damit
         * die Pruefung in {@code PersonenMaske.gepruefteSpeicherung}: sie sah
         * den alten Wert und liess durch, was auf dem Bildschirm laengst leer
         * war. Nebenbei liess sich eine falsch eingetragene Strasse aendern,
         * aber nicht entfernen.</p>
         */
        @Test
        @DisplayName("Ein geleertes Feld wird uebernommen und nicht uebergangen")
        void geleertesFeldKommtAn() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                PatientFieldPopulator populator = new PatientFieldPopulator();
                Patient anna = teilnehmer();

                Node strasse = feld("street");
                populator.populateField(strasse, "street", anna);
                ((javafx.scene.control.TextInputControl) Feldbau.bedienelement(strasse)).setText("");
                populator.extractToEntity(strasse, "street", anna);

                Node kassenIk = feld("kassenIk");
                populator.populateField(kassenIk, "kassenIk", anna);
                ((javafx.scene.control.TextInputControl) Feldbau.bedienelement(kassenIk)).setText("");
                populator.extractToEntity(kassenIk, "kassenIk", anna);

                assertEquals("", anna.getStreet(), "Sonst liesse sich eine Strasse nie entfernen");
                assertEquals(0, anna.getKassenIk(),
                        "0 ist kein gueltiges IK - genau darauf soll die Pruefung anspringen");
            });
        }

        @Test
        @DisplayName("Ein leeres Datumsfeld setzt kein Datum")
        void leeresDatum() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                PatientFieldPopulator populator = new PatientFieldPopulator();
                Patient ohneDatum = new Patient();

                populator.populateField(feld("birthDate"), "birthDate", ohneDatum);
                populator.extractToEntity(feld("birthDate"), "birthDate", ohneDatum);

                assertNull(ohneDatum.getBirthDate());
            });
        }

        @Test
        @DisplayName("Ein unbekannter Feldname wirft nicht, sondern tut nichts")
        void unbekanntesFeld() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                PatientFieldPopulator populator = new PatientFieldPopulator();
                Patient anna = teilnehmer();

                populator.populateField(feld("firstname"), "gibtEsNicht", anna);
                populator.extractToEntity(feld("firstname"), "gibtEsNicht", anna);

                assertEquals("Anna", anna.getFirstname());
            });
        }
    }

    // --- Hilfsmittel ------------------------------------------------------

    /** Dieselben Felder, die {@code PersonenMaske} aus der Tag-Datei baut. */
    private static final java.util.List<String> FELDER = java.util.List.of(
            "firstname", "lastname", "street", "housenumber", "country",
            "plz", "ik", "kassenIk", "birthDate");

    private static Patient teilnehmer() {
        return new Patient("Anna", "Berger", "Hauptstrasse", "DE", "12a", 28195,
                IK, KASSEN_IK, LocalDate.of(1992, 4, 17));
    }

    /** Was in dem Feld steht, nachdem der Populator es gefuellt hat. */
    private String angezeigt(PatientFieldPopulator populator, String feldname, Patient person) {
        Node feld = feld(feldname);
        populator.populateField(feld, feldname, person);
        return ((javafx.scene.control.TextInputControl) Feldbau.bedienelement(feld)).getText();
    }

    /** Ein Feld, gebaut wie in der Maske - mit Huelle, Erklaerung und Zeichen. */
    private Node feld(String feldname) {
        TagList beschreibung = TagConfigLoader
                .loadTagConfig("/tags/person-tags.json").get(feldname);
        return feldbau.erzeugeFeld(feldname, beschreibung);
    }
}
