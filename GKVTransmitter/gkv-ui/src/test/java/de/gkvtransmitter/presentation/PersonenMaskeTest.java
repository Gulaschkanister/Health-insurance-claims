package de.gkvtransmitter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.presentation.populator.PatientFieldPopulator;
import de.gkvtransmitter.presentation.populator.ServiceProviderFieldPopulator;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Prueft die Personenmaske.
 *
 * <p>Teilnehmer und Dienstleister teilen sich das Formular. Die Tests achten
 * deshalb vor allem darauf, dass die Rolle sich durchhaelt: dass ein
 * Dienstleister auch als solcher gespeichert und gemeldet wird.</p>
 */
@DisplayName("Personenmaske")
class PersonenMaskeTest {

    private static final int KASSEN_IK = 108310400;

    private SpeicherRepository datenbank;
    private AufzeichnendeMeldungen meldungen;
    private AufzeichnenderRahmen rahmen;
    private AppMessages texte;

    @BeforeAll
    static void laufzeitHochfahren() {
        JavaFxLaufzeit.starten();
    }

    @BeforeEach
    void aufsetzen() {
        datenbank = new SpeicherRepository();
        meldungen = new AufzeichnendeMeldungen();
        rahmen = new AufzeichnenderRahmen();
        texte = new AppMessages("/messages/ui-messages.json");
    }

    @Nested
    @DisplayName("Anlegen")
    class Anlegen {

        @Test
        @DisplayName("Ein Teilnehmer wird mit den eingegebenen Werten angelegt")
        void teilnehmerAngelegt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(1, datenbank.getAllPatients().size());
                Patient angelegt = datenbank.getAllPatients().get(0);
                assertEquals("Anna", angelegt.getFirstname());
                assertEquals("Muster", angelegt.getLastname());
                assertEquals("Musterweg", angelegt.getStreet());
                assertEquals(12345, angelegt.getPlz());
                assertEquals(KASSEN_IK, angelegt.getKassenIk());
                assertTrue(datenbank.getAllServiceProviders().isEmpty(),
                        "Es darf kein Dienstleister entstanden sein");
            });
        }

        @Test
        @DisplayName("Ein Dienstleister wird als Dienstleister gespeichert und auch so gemeldet")
        void dienstleisterAngelegt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(true);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(1, datenbank.getAllServiceProviders().size());
                ServiceProvider angelegt = datenbank.getAllServiceProviders().get(0);
                assertEquals("Anna", angelegt.getFirstname());
                assertTrue(datenbank.getAllPatients().isEmpty(), "Es darf kein Teilnehmer entstanden sein");
                assertEquals(String.format(texte.get("msg.selfCreated"), "Anna Muster"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Ein Teilnehmer wird als Teilnehmer gemeldet")
        void teilnehmerGemeldet() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(String.format(texte.get("msg.patientCreated"), "Anna Muster"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Ein eingetragenes Geburtsdatum wird uebernommen")
        void geburtsdatum() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                kalender(formular, "birthDate").setValue(LocalDate.of(1980, 5, 17));
                speichern(formular).fire();

                assertEquals(LocalDate.of(1980, 5, 17), datenbank.getAllPatients().get(0).getBirthDate());
            });
        }

        @Test
        @DisplayName("Nach dem Speichern wird der Bereich geraeumt")
        void bereichGeraeumt() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                assertEquals(1, rahmen.wieOftGeleert());
            });
        }

        @Test
        @DisplayName("Abbrechen raeumt den Bereich, ohne zu speichern")
        void abbrechen() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                ((Button) formular.lookup("#" + PersonenMaske.ID_ABBRECHEN)).fire();

                assertEquals(1, rahmen.wieOftGeleert());
                assertTrue(datenbank.getAllPatients().isEmpty());
                assertTrue(meldungen.leer());
            });
        }

        @Test
        @DisplayName("Ein Fehlschlag beim Speichern wird gemeldet und der Bereich bleibt stehen")
        void speichernScheitert() {
            datenbank.scheitertBeimSpeichern(new IllegalStateException("Datenbank gesperrt"));
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);
                speichern(formular).fire();

                AufzeichnendeMeldungen.Meldung meldung = meldungen.einzige();
                assertEquals(AufzeichnendeMeldungen.Art.FEHLER, meldung.art());
                assertTrue(meldung.text().contains("Datenbank gesperrt"), meldung.text());
                assertEquals(0, rahmen.wieOftGeleert());
            });
        }

        @Test
        @DisplayName("Ein Kassen-IK mit falscher Pruefziffer wird nicht gespeichert")
        void ungueltigesKassenIk() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);
                fuelleAus(formular);

                zahl(formular, "kassenIk", 108310401);
                speichern(formular).fire();

                assertTrue(datenbank.getAllPatients().isEmpty(),
                        "Sonst faellt es erst beim Versand auf - als Ablehnung der ganzen Lieferung");
                assertTrue(meldungen.einzige().text().contains(texte.get("msg.invalidIk")),
                        meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Ein gueltiges Kassen-IK wird nicht beanstandet")
        void gueltigesKassenIk() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);

                fuelleAus(formular);

                assertNull(beanstandung(formular, "kassenIk"));
            });
        }

        @Test
        @DisplayName("Das Formular bringt fuer jedes beschriebene Feld ein Eingabeelement mit")
        void alleFelder() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region formular = formular(false);

                for (String feld : new String[] {"firstname", "lastname", "street", "country",
                        "housenumber", "plz", "ik", "kassenIk", "birthDate"}) {
                    assertNotNull(formular.lookup("#" + PersonenMaske.ID_FELD + feld),
                            "Kein Eingabefeld fuer " + feld);
                }
            });
        }
    }

    @Nested
    @DisplayName("Uebersicht")
    class Uebersicht {

        @Test
        @DisplayName("Ohne Teilnehmer steht ein Hinweis in der Liste")
        void keineTeilnehmer() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = maske().teilnehmerliste();

                assertNull(zeilenknopf(liste, PersonenMaske.KENNUNG_TEILNEHMER, 1,
                        PersonenMaske.AKTION_BEARBEITEN));
                assertTrue(meldungen.leer(), "Der Hinweis gehoert in die Liste, nicht in eine Meldung");
            });
        }

        @Test
        @DisplayName("Teilnehmer und Dienstleister stehen in getrennten Listen")
        void getrennteListen() {
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region teilnehmer = maske().teilnehmerliste();
                assertNotNull(zeilenknopf(teilnehmer, PersonenMaske.KENNUNG_TEILNEHMER, 1,
                        PersonenMaske.AKTION_BEARBEITEN));
                assertNull(zeilenknopf(teilnehmer, PersonenMaske.KENNUNG_TEILNEHMER, 9,
                        PersonenMaske.AKTION_BEARBEITEN), "Der Dienstleister gehoert nicht hierher");

                Region dienstleister = maske().dienstleisterliste();
                assertNotNull(zeilenknopf(dienstleister, PersonenMaske.KENNUNG_DIENSTLEISTER, 9,
                        PersonenMaske.AKTION_BEARBEITEN));
            });
        }

        @Test
        @DisplayName("Das Suchfeld filtert nach Namen")
        void suche() {
            datenbank.mitPatient(patient(1, "Anna")).mitPatient(patient(2, "Bernd"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = maske().teilnehmerliste();
                TextField suche = (TextField) liste.lookup(
                        "#" + PersonenMaske.KENNUNG_TEILNEHMER + Listenbau.SUCHE);
                assertNotNull(suche, "Kein Suchfeld in der Teilnehmerliste");

                suche.setText("bernd");

                assertNull(zeilenknopf(liste, PersonenMaske.KENNUNG_TEILNEHMER, 1,
                        PersonenMaske.AKTION_BEARBEITEN));
                assertNotNull(zeilenknopf(liste, PersonenMaske.KENNUNG_TEILNEHMER, 2,
                        PersonenMaske.AKTION_BEARBEITEN));
            });
        }

        @Test
        @DisplayName("Die Schaltflaeche fuer einen neuen Eintrag oeffnet das Formular")
        void neuerEintrag() {
            JavaFxLaufzeit.aufFxFaden(() -> {
                Region liste = maske().teilnehmerliste();
                ((Button) liste.lookup("#" + PersonenMaske.ID_NEU)).fire();

                assertNotNull(rahmen.inhalt());
                assertNotNull(rahmen.inhalt().lookup("#" + PersonenMaske.ID_SPEICHERN));
            });
        }
    }

    @Nested
    @DisplayName("Loeschen")
    class Loeschen {

        @Test
        @DisplayName("Ohne Zustimmung bleibt der Teilnehmer bestehen")
        void ohneZustimmung() {
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().teilnehmerliste(), PersonenMaske.KENNUNG_TEILNEHMER, 1).fire();

                assertEquals(1, meldungen.gestellteRueckfragen().size(), "Es muss nachgefragt werden");
                assertEquals(1, datenbank.getAllPatients().size());
                assertTrue(meldungen.leer());
            });
        }

        @Test
        @DisplayName("Nach Zustimmung wird der Teilnehmer geloescht und das gemeldet")
        void mitZustimmung() {
            datenbank.mitPatient(patient(1, "Anna"));
            meldungen.stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().teilnehmerliste(), PersonenMaske.KENNUNG_TEILNEHMER, 1).fire();

                assertTrue(datenbank.getAllPatients().isEmpty());
                assertEquals(String.format(texte.get("msg.patientDeleted"), "Anna Muster (ID: 1)"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Der Dienstleister wird eigenstaendig geloescht und eigenstaendig gemeldet")
        void dienstleisterGeloescht() {
            datenbank.mitPatient(patient(1, "Anna")).mitDienstleister(dienstleister(9, "Max"));
            meldungen.stimmtZu();

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().dienstleisterliste(), PersonenMaske.KENNUNG_DIENSTLEISTER, 9).fire();

                assertTrue(datenbank.getAllServiceProviders().isEmpty());
                assertEquals(1, datenbank.getAllPatients().size(), "Der Teilnehmer bleibt unberuehrt");
                assertEquals(String.format(texte.get("msg.selfDeleted"), "Max Muster (ID: 9)"), meldungen.einzige().text());
            });
        }

        @Test
        @DisplayName("Die Rueckfrage nennt den Teilnehmer beim Namen")
        void rueckfrageNenntNamen() {
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                loeschenKnopf(maske().teilnehmerliste(), PersonenMaske.KENNUNG_TEILNEHMER, 1).fire();

                assertTrue(meldungen.gestellteRueckfragen().get(0).contains("Anna"),
                        meldungen.gestellteRueckfragen().toString());
            });
        }
    }

    @Nested
    @DisplayName("Bearbeiten")
    class Bearbeiten {

        @Test
        @DisplayName("Der gewaehlte Teilnehmer wird zum Bearbeiten gezeigt")
        void formularGezeigt() {
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                zeilenknopf(maske().teilnehmerliste(), PersonenMaske.KENNUNG_TEILNEHMER, 1,
                        PersonenMaske.AKTION_BEARBEITEN).fire();

                assertNotNull(rahmen.inhalt(), "Das Bearbeitungsformular muss gezeigt werden");
            });
        }

        /**
         * Die Gegenprobe zum Anlegen.
         *
         * <p>Seit dem 05.09.2026 laesst sich keine ungueltige Person mehr
         * <em>anlegen</em>. Das Bearbeiten lief aber ueber
         * {@code EditFormController}, und der uebertrug die Eingaben ungeprueft
         * in die Person und reichte sie weiter. Die schaerfere Eingangspruefung
         * haette so nur den Weg verlagert, auf dem falsche Stammdaten
         * entstehen - man haette Anna richtig angelegt und danach kaputt
         * bearbeitet, mit demselben Ergebnis: "Pruefung nicht bestanden" beim
         * Versand.</p>
         */
        @Test
        @DisplayName("Eine Person mit falschem IK laesst sich nicht speichern")
        void ungueltigeAenderungWirdAbgewiesen() {
            // patient(...) traegt bewusst das IK 101 und kein Geburtsdatum -
            // genau die Daten, die die Kasse zurueckweisen wuerde.
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                zeilenknopf(maske().teilnehmerliste(), PersonenMaske.KENNUNG_TEILNEHMER, 1,
                        PersonenMaske.AKTION_BEARBEITEN).fire();
                aktualisieren(rahmen.inhalt()).fire();

                assertEquals(AufzeichnendeMeldungen.Art.FEHLER, meldungen.einzige().art());
                assertTrue(datenbank.gespeichertePatienten().isEmpty(),
                        "Eine Person, die sich nicht abrechnen laesst, darf nicht in die Datenbank");
            });
        }

        @Test
        @DisplayName("Die Beanstandung nennt beide Ursachen, nicht nur die erste")
        void nenntAlleUrsachen() {
            datenbank.mitPatient(patient(1, "Anna"));

            JavaFxLaufzeit.aufFxFaden(() -> {
                zeilenknopf(maske().teilnehmerliste(), PersonenMaske.KENNUNG_TEILNEHMER, 1,
                        PersonenMaske.AKTION_BEARBEITEN).fire();
                aktualisieren(rahmen.inhalt()).fire();

                String text = meldungen.einzige().text();
                assertTrue(text.contains(texte.get("field.birthDate")),
                        "Das fehlende Geburtsdatum fehlt in der Meldung: " + text);
                assertTrue(text.contains(texte.get("msg.invalidIk")),
                        "Das falsche Kennzeichen fehlt in der Meldung: " + text);
            });
        }

        @Test
        @DisplayName("Eine gueltige Person laesst sich weiterhin speichern")
        void gueltigeAenderungGehtDurch() {
            Patient anna = patient(1, "Anna");
            anna.setIk(IK_DIENSTLEISTER);
            anna.setKassenIk(KASSEN_IK);
            anna.setBirthDate(LocalDate.of(1990, 5, 17));
            datenbank.mitPatient(anna);

            JavaFxLaufzeit.aufFxFaden(() -> {
                zeilenknopf(maske().teilnehmerliste(), PersonenMaske.KENNUNG_TEILNEHMER, 1,
                        PersonenMaske.AKTION_BEARBEITEN).fire();
                aktualisieren(rahmen.inhalt()).fire();

                assertEquals(List.of(anna), datenbank.gespeichertePatienten());
                assertEquals(String.format(texte.get("msg.patientUpdated"), "Anna Muster (ID: 1)"), meldungen.einzige().text());
            });
        }
    }

    // --- Aufbau und Bedienung -------------------------------------------

    private PersonenMaske maske() {
        UiFactory bausteine = new JavaFxUiFactory();
        return new PersonenMaske(bausteine, texte, meldungen, datenbank, rahmen,
                new Feldbau(bausteine, texte), new PatientFieldPopulator(),
                new ServiceProviderFieldPopulator());
    }

    private Region formular(boolean alsDienstleister) {
        return maske().formular("Ueberschrift", alsDienstleister);
    }

    /**
     * Traegt einen vollstaendigen, gueltigen Satz Werte ein.
     *
     * <p>Gueltig heisst seit dem 05.09.2026 mehr als vorher: die Maske
     * speichert nicht mehr, was sich nicht abrechnen laesst. Das IK stand hier
     * auf {@code 101} - drei Stellen statt neun -, und ein Geburtsdatum fehlte
     * ganz. Beides haette die Kasse zurueckgewiesen; die Maske liess es
     * durch, und die Tests bestaetigten das.</p>
     */
    private void fuelleAus(Region formular) {
        text(formular, "firstname").setText("Anna");
        text(formular, "lastname").setText("Muster");
        text(formular, "street").setText("Musterweg");
        text(formular, "country").setText("DE");
        text(formular, "housenumber").setText("1");
        zahl(formular, "plz", 12345);
        zahl(formular, "ik", IK_DIENSTLEISTER);
        zahl(formular, "kassenIk", KASSEN_IK);
        kalender(formular, "birthDate").setValue(GEBURTSTAG);
    }

    /** Ein IK mit richtiger Pruefziffer, siehe {@code Institutionskennzeichen}. */
    private static final int IK_DIENSTLEISTER = 261914007;

    private static final java.time.LocalDate GEBURTSTAG = java.time.LocalDate.of(1990, 5, 17);

    private TextField text(Region formular, String feldname) {
        return (TextField) bedienelement(formular, feldname);
    }

    private DatePicker kalender(Region formular, String feldname) {
        return (DatePicker) bedienelement(formular, feldname);
    }

    /**
     * Traegt eine Zahl ein.
     *
     * <p>Zahlenfelder mit mehr als zwei Stellen sind seit dem 05.09.2026
     * Textfelder statt Zaehler - Auf- und Ab-Pfeile nuetzen bei einer
     * neunstelligen Nummer niemandem, und ein Zaehler gab eine nicht
     * bestaetigte Eingabe gar nicht erst zurueck. Der Helfer beherrscht
     * weiterhin beides, damit er auch fuer kurze Felder taugt.</p>
     */
    private void zahl(Region formular, String feldname, int wert) {
        Node bedienelement = bedienelement(formular, feldname);
        if (bedienelement instanceof TextField eingabe) {
            eingabe.setText(String.valueOf(wert));
            return;
        }
        @SuppressWarnings("unchecked")
        Spinner<Integer> zaehler = (Spinner<Integer>) bedienelement;
        zaehler.getValueFactory().setValue(wert);
    }

    /**
     * Das Bedienelement eines Feldes.
     *
     * <p>Ueber {@code Feldbau} und nicht ueber das erste Kind der Huelle: seit
     * neben dem Bedienelement ein Info-Zeichen stehen kann, ist das erste Kind
     * mitunter eine Zeile aus beidem. Der Test ging denselben Weg wie die
     * Maske - jetzt geht er wirklich denselben.</p>
     */
    private Node bedienelement(Region formular, String feldname) {
        return Feldbau.bedienelement(feld(formular, feldname));
    }

    /** Die sichtbare Beanstandung unter einem Feld, oder {@code null}. */
    private String beanstandung(Region formular, String feldname) {
        return ((VBox) feld(formular, feldname)).getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(zeile -> zeile.getStyleClass().contains(Feldbau.STIL_FEHLER))
                .filter(Label::isVisible)
                .map(Label::getText)
                .findFirst()
                .orElse(null);
    }

    private Node feld(Region formular, String feldname) {
        Node feld = formular.lookup("#" + PersonenMaske.ID_FELD + feldname);
        assertNotNull(feld, "Kein Eingabefeld fuer " + feldname);
        return feld;
    }

    private Button speichern(Region formular) {
        return (Button) formular.lookup("#" + PersonenMaske.ID_SPEICHERN);
    }

    private Button zeilenknopf(Region liste, String kennungsvorsatz, int personId, String aktion) {
        return (Button) liste.lookup("#" + kennungsvorsatz + Listenbau.ZEILE + personId + "-" + aktion);
    }

    /**
     * Die Schaltflaeche "Aktualisieren" im Bearbeitungsformular.
     *
     * <p>Das Formular kommt als {@code ScrollPane}; dessen Inhalt haengt erst
     * im Knotenbaum, wenn die Darstellung erzeugt ist, und die entsteht erst in
     * einer Szene. Ohne diesen Schritt fande der {@code lookup} nichts.</p>
     */
    private Button aktualisieren(Region formular) {
        assertNotNull(formular, "Es wurde kein Formular gezeigt");
        new Scene(formular);
        formular.applyCss();
        formular.layout();

        List<Button> treffer = formular.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(knopf -> texte.get("button.update").equals(knopf.getText()))
                .toList();
        assertEquals(1, treffer.size(), "Erwartet war genau eine Schaltflaeche zum Aktualisieren");
        return treffer.get(0);
    }

    private Button loeschenKnopf(Region liste, String kennungsvorsatz, int personId) {
        Button knopf = zeilenknopf(liste, kennungsvorsatz, personId, PersonenMaske.AKTION_LOESCHEN);
        assertNotNull(knopf, "Keine Loeschen-Schaltflaeche fuer " + personId);
        return knopf;
    }

    // --- Testdaten -------------------------------------------------------

    private static Patient patient(int id, String vorname) {
        Patient person = new Patient(vorname, "Muster", "Musterweg", "DE", "1", 12345, 101, KASSEN_IK, null);
        person.setId(id);
        return person;
    }

    private static ServiceProvider dienstleister(int id, String vorname) {
        ServiceProvider person =
                new ServiceProvider(vorname, "Muster", "Musterstr.", "DE", "1", 12345, 1001, KASSEN_IK, null);
        person.setId(id);
        return person;
    }
}
