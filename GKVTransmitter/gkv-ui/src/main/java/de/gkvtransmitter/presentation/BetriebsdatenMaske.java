package de.gkvtransmitter.presentation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.Institutionskennzeichen;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Die Angaben zum eigenen Betrieb.
 *
 * <p>Zwei davon gehen in jede Datei - das <b>eigene IK</b> und die
 * <b>Rolle</b>; ohne sie ist der logische Dateiname nicht bildbar. Alle
 * uebrigen stehen in keiner Nachricht: Sie sind ein <em>Aktenordner</em> fuer
 * die Anmeldungen bei ARGE·IK, ITSG-Trust-Center und Datenannahmestelle, die im
 * Kern dieselben Angaben abfragen.</p>
 *
 * <p><b>Ein Speichern-Knopf, anders als in {@link EinstellungenMaske}.</b> Dort
 * ist jede Zeile eine Wahl fuer sich; hier gehoeren die Angaben zusammen und
 * werden am Stueck eingegeben. Ein Feld, das nach jedem Zeichen speichert,
 * schriebe auf halbem Weg ein halbes IK weg.</p>
 */
public class BetriebsdatenMaske {

    /** Kennung des Felds fuer den Praxisnamen. */
    public static final String ID_NAME = "betrieb-name";
    /** Kennung des Felds fuer das eigene Institutionskennzeichen. */
    public static final String ID_IK = "betrieb-ik";
    /** Kennung des Auswahlfelds fuer die Rolle. */
    public static final String ID_ROLLE = "betrieb-rolle";
    /** Kennung des Felds fuer die Strasse. */
    public static final String ID_STRASSE = "betrieb-strasse";
    /** Kennung des Felds fuer die Hausnummer. */
    public static final String ID_HAUSNUMMER = "betrieb-hausnummer";
    /** Kennung des Felds fuer die Postleitzahl. */
    public static final String ID_PLZ = "betrieb-plz";
    /** Kennung des Felds fuer den Ort. */
    public static final String ID_ORT = "betrieb-ort";
    /** Kennung des Felds fuer die Steuernummer. */
    public static final String ID_STEUERNUMMER = "betrieb-steuernummer";
    /** Kennung des Felds fuer den Ansprechpartner. */
    public static final String ID_ANSPRECHPARTNER = "betrieb-ansprechpartner";
    /** Kennung des Felds fuer die Telefonnummer. */
    public static final String ID_TELEFON = "betrieb-telefon";
    /** Kennung des Felds fuer die E-Mail-Anschrift. */
    public static final String ID_EMAIL = "betrieb-email";
    /** Kennung des Felds fuer das Ablaufdatum des Zertifikats. */
    public static final String ID_ZERTIFIKAT = "betrieb-zertifikat";
    /** Kennung der Schaltflaeche zum Speichern. */
    public static final String ID_SPEICHERN = "betrieb-speichern";

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final DataRepository datenbank;

    public BetriebsdatenMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
            DataRepository datenbank) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
    }

    /** Die Maske, gefuellt mit dem, was schon erfasst ist. */
    public Region maske() {
        Betriebsdaten vorhanden = datenbank.ladeBetriebsdaten();
        Betriebsdaten stand = vorhanden == null ? new Betriebsdaten() : vorhanden;

        // Die Felder liegen in einer Abbildung statt in zwoelf Variablen: Sie
        // wandern sonst als zwoelf Parameter weiter ins Speichern, und genau
        // diese Parameterlisten sind im Projekt schon einmal als Befund
        // aufgeschlagen.
        Map<String, TextField> felder = new LinkedHashMap<>();
        felder.put(ID_NAME, feld(ID_NAME, stand.getPraxisname()));
        felder.put(ID_IK, feld(ID_IK, stand.getIk()));
        // Die Felder der Anschrift und der Kontaktzeile stehen nebeneinander
        // und tragen deshalb ihren Namen als Platzhalter: eine Reihe leerer
        // Kaesten sagt sonst nicht, was wohin gehoert.
        felder.put(ID_STRASSE, feld(ID_STRASSE, stand.getStrasse(), texte.get("company.street")));
        felder.put(ID_HAUSNUMMER,
                feld(ID_HAUSNUMMER, stand.getHausnummer(), texte.get("company.housenumber")));
        felder.put(ID_PLZ, feld(ID_PLZ, stand.getPlz(), texte.get("company.plz")));
        felder.put(ID_ORT, feld(ID_ORT, stand.getOrt(), texte.get("company.city")));
        felder.put(ID_STEUERNUMMER, feld(ID_STEUERNUMMER, stand.getSteuernummer()));
        felder.put(ID_ANSPRECHPARTNER,
                feld(ID_ANSPRECHPARTNER, stand.getAnsprechpartner(), texte.get("company.contact")));
        felder.put(ID_TELEFON, feld(ID_TELEFON, stand.getTelefon(), texte.get("company.phone")));
        felder.put(ID_EMAIL, feld(ID_EMAIL, stand.getEmail(), texte.get("company.email")));
        felder.put(ID_ZERTIFIKAT, feld(ID_ZERTIFIKAT, stand.getZertifikatBis()));

        ComboBox<String> rolle = new ComboBox<>();
        rolle.setId(ID_ROLLE);
        rolle.getItems().addAll(texte.get("company.role.self"), texte.get("company.role.agency"));
        rolle.getSelectionModel().select(stand.istSelbstabrechner() ? 0 : 1);
        rolle.setMaxWidth(Double.MAX_VALUE);

        Button speichern = new Button(texte.get("company.save"));
        speichern.setId(ID_SPEICHERN);
        speichern.getStyleClass().add("schaltflaeche-haupt");
        speichern.setOnAction(ereignis -> speichere(stand, felder, rolle));

        VBox wurzel = new VBox(16);
        wurzel.getStyleClass().add("maske");
        wurzel.getChildren().addAll(
                ueberschrift(),
                karte(texte.get("company.name"), texte.get("company.name.help"), felder.get(ID_NAME)),
                karte(texte.get("company.ik"), texte.get("company.ik.help"), felder.get(ID_IK)),
                karte(texte.get("company.role"), texte.get("company.role.help"), rolle),
                karte(texte.get("company.address"), "", new HBox(8, felder.get(ID_STRASSE),
                        felder.get(ID_HAUSNUMMER), felder.get(ID_PLZ), felder.get(ID_ORT))),
                karte(texte.get("company.contact"), texte.get("company.contact.help"),
                        new HBox(8, felder.get(ID_ANSPRECHPARTNER), felder.get(ID_TELEFON),
                                felder.get(ID_EMAIL))),
                karte(texte.get("company.taxnumber"), texte.get("company.noAccount"),
                        felder.get(ID_STEUERNUMMER)),
                karte(texte.get("company.certificate"), texte.get("company.certificate.help"),
                        felder.get(ID_ZERTIFIKAT)),
                speichern);
        return wurzel;
    }

    /**
     * Uebernimmt die Eingaben.
     *
     * <p>Das IK wird gegen die Pruefziffer gehalten, <b>bevor</b> etwas
     * gespeichert wird - ein falsches faellt sonst erst beim Versand auf, und
     * dann nennt die Meldung ein Feld im UNB statt die Stammangabe. Leer
     * bleiben darf es: Wer die Maske oeffnet, um den Ansprechpartner
     * nachzutragen, soll nicht am IK haengenbleiben.</p>
     */
    private void speichere(Betriebsdaten stand, Map<String, TextField> felder, ComboBox<String> rolle) {
        String kennzeichen = text(felder, ID_IK);
        if (!kennzeichen.isEmpty() && !Institutionskennzeichen.istGueltig(kennzeichen)) {
            meldungen.fehler(texte.get("company.ikInvalid"));
            return;
        }

        stand.setPraxisname(text(felder, ID_NAME));
        stand.setIk(kennzeichen);
        stand.setSelbstabrechner(rolle.getSelectionModel().getSelectedIndex() != 1);
        stand.setStrasse(text(felder, ID_STRASSE));
        stand.setHausnummer(text(felder, ID_HAUSNUMMER));
        stand.setPlz(text(felder, ID_PLZ));
        stand.setOrt(text(felder, ID_ORT));
        stand.setSteuernummer(text(felder, ID_STEUERNUMMER));
        stand.setAnsprechpartner(text(felder, ID_ANSPRECHPARTNER));
        stand.setTelefon(text(felder, ID_TELEFON));
        stand.setEmail(text(felder, ID_EMAIL));
        stand.setZertifikatBis(text(felder, ID_ZERTIFIKAT));

        datenbank.speichereBetriebsdaten(stand);
        meldungen.erfolg(texte.get("company.saved"));
    }

    private Region ueberschrift() {
        Label titel = bausteine.createLabel(texte.get("company.title"));
        titel.getStyleClass().add("masken-titel");
        Label erklaerung = bausteine.createLabel(texte.get("company.subtitle"));
        erklaerung.getStyleClass().add("feld-hinweis");
        erklaerung.setWrapText(true);
        return new VBox(4, titel, erklaerung);
    }

    private TextField feld(String kennung, String wert) {
        return feld(kennung, wert, "");
    }

    /**
     * @param platzhalter was im leeren Feld steht; leer, wenn die Karte
     *        darueber schon sagt, worum es geht
     */
    private TextField feld(String kennung, String wert, String platzhalter) {
        TextField eingabe = new TextField(wert == null ? "" : wert);
        eingabe.setId(kennung);
        eingabe.setMaxWidth(Double.MAX_VALUE);
        if (!platzhalter.isEmpty()) {
            eingabe.setPromptText(platzhalter);
        }
        return eingabe;
    }

    private static String text(Map<String, TextField> felder, String kennung) {
        TextField feld = felder.get(kennung);
        return feld == null || feld.getText() == null ? "" : feld.getText().trim();
    }

    private Region karte(String titel, String erklaerung, javafx.scene.Node bedienelement) {
        Label beschriftung = bausteine.createLabel(titel);
        beschriftung.getStyleClass().add("feld-beschriftung");

        VBox karte = new VBox(8, beschriftung, bedienelement);
        if (!erklaerung.isEmpty()) {
            Label hinweis = bausteine.createLabel(erklaerung);
            hinweis.getStyleClass().add("feld-hinweis");
            hinweis.setWrapText(true);
            karte.getChildren().add(hinweis);
        }
        karte.getStyleClass().add("karte");
        return karte;
    }

    /** Die Felder, die diese Maske fuehrt - fuer Tests und Uebersicht. */
    public static List<String> felder() {
        return List.of(ID_NAME, ID_IK, ID_ROLLE, ID_STRASSE, ID_HAUSNUMMER, ID_PLZ, ID_ORT,
                ID_STEUERNUMMER, ID_ANSPRECHPARTNER, ID_TELEFON, ID_EMAIL, ID_ZERTIFIKAT);
    }
}
