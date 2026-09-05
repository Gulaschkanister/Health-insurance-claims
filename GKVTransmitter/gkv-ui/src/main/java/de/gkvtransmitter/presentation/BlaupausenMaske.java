package de.gkvtransmitter.presentation;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.model.DtaMessage;
import de.gkvtransmitter.model.segment.SegmentInfo;
import de.gkvtransmitter.model.segment.ValueFieldEntry;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import de.gkvtransmitter.util.TagList;
import de.gkvtransmitter.util.modifiers.MaxLengthModifier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Anlegen, Ansehen, Bearbeiten und Loeschen von Blaupausen.
 *
 * <p>Eine Blaupause haelt fest, was an einer Leistung unabhaengig von der
 * einzelnen Teilnehmerin gleich bleibt: der Preis je Termin, der
 * Abrechnungscode, das Tarifkennzeichen. Sie ist damit die Vorlage, gegen die
 * ein Kurs Monat fuer Monat abgerechnet wird.</p>
 *
 * <p>Bis zum 05.09.2026 liess sich eine Blaupause anlegen und danach nie wieder
 * ansehen. Sie erschien allein als Name im Auswahlfeld der Abrechnung; ein
 * Vertipper im Betrag war weder zu erkennen noch zu berichtigen, und die
 * falsche Blaupause blieb dort stehen, einen Klick vom Versand entfernt.
 * {@code DataRepository} hatte fuer sie als einzige gefuehrte Entitaet kein
 * Loeschen.</p>
 *
 * <p>Die Felder kommen aus den Segmentdefinitionen und werden ueber
 * {@link Feldbau} gebaut - damit gilt hier dasselbe wie in den anderen Masken:
 * unter jedem Feld steht, was hineingehoert, und eine unzulaessige Eingabe wird
 * beanstandet. Zuvor baute {@code View} diese Felder selbst, ohne Erklaerung
 * und ohne Pruefung.</p>
 */
public class BlaupausenMaske {

    /** Kennung des Eingabefelds fuer den Namen der Blaupause. */
    public static final String ID_NAME = "blaupause-name";
    /** Kennung der Schaltflaeche zum Speichern. */
    public static final String ID_SPEICHERN = "blaupause-speichern";
    /** Kennung der Schaltflaeche zum Abbrechen. */
    public static final String ID_ABBRECHEN = "blaupause-abbrechen";
    /** Kennungsvorsatz der Blaupausenliste. */
    public static final String KENNUNG = "blaupause";
    /** Nachsatz der Kennung einer Bearbeiten-Schaltflaeche. */
    public static final String AKTION_BEARBEITEN = "bearbeiten";
    /** Nachsatz der Kennung einer Loeschen-Schaltflaeche. */
    public static final String AKTION_LOESCHEN = "loeschen";

    /**
     * Feldname des Preises je Termin.
     *
     * <p>Muss mit dem Namen in {@code segments/enf.json} uebereinstimmen, weil
     * {@link de.gkvtransmitter.dta.Leistungsparameter} genau danach sucht. Der Test
     * {@code BlaupausenfelderTest} haelt das zusammen.</p>
     */
    static final String FELD_EINZELBETRAG = "Durchschnittlicher Einzelbetrag";

    private static final DateTimeFormatter ANGELEGT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final DataRepository datenbank;
    private final Maskenrahmen rahmen;
    private final Feldbau feldbau;
    private final Vorlagen vorlagen;
    private final ObjectMapper leser = new ObjectMapper();

    /**
     * Woher die Feldliste einer Vorlage kommt.
     *
     * <p>Als Schnittstelle, damit die Maske ohne {@code Controller} und ohne
     * geladene Definitionen geprueft werden kann.</p>
     */
    public interface Vorlagen {
        /** Alle bekannten Vorlagen, Name auf Nachricht. */
        Map<String, DtaMessage> alle();

        /** Hinterlegte Werte fuer ein Feld, oder leer fuer freie Eingabe. */
        List<String> auswahlFuer(String feldname);
    }

    public BlaupausenMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
            DataRepository datenbank, Maskenrahmen rahmen, Feldbau feldbau, Vorlagen vorlagen) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
        this.rahmen = Objects.requireNonNull(rahmen, "rahmen must not be null");
        this.feldbau = Objects.requireNonNull(feldbau, "feldbau must not be null");
        this.vorlagen = Objects.requireNonNull(vorlagen, "vorlagen must not be null");
    }

    /**
     * Die Uebersicht aller Blaupausen.
     *
     * <p>Sie nennt den Preis je Termin in der Zeile. Das ist die Angabe, wegen
     * der man eine Blaupause nachschlaegt - sie erst im Formular zu zeigen,
     * hiesse, sie zum Vergleichen einzeln oeffnen zu muessen.</p>
     */
    public Region liste() {
        return Maskenkopf.mitListe(bausteine, texte.get("title.blueprint.new"), ID_NEU, this::neu,
                new Listenbau<Blueprint>(bausteine, KENNUNG)
                        .spalten(texte.get("label.blueprintName"), texte.get("label.template"),
                                texte.get("label.pricePerAppointment"), texte.get("label.created"))
                        .zellen(blaupause -> List.of(
                                text(blaupause.getName()),
                                // Der Kursname statt des vollen Vorlagennamens:
                                // "Geburtsvorbereitungskurs, Einzelabrechnung"
                                // sprengte die Spalte, und der Zusatz zur
                                // Abrechnungsart ist bei allen Vorlagen gleich.
                                View.kursname(text(blaupause.getTemplateName())),
                                preis(blaupause),
                                angelegt(blaupause.getCreatedAt())))
                        .kennnummer(blaupause -> String.valueOf(blaupause.getId()))
                        .durchsuchbar(texte.get("label.searchBlueprint"),
                                blaupause -> text(blaupause.getName()) + " " + text(blaupause.getTemplateName()))
                        .hinweisWennLeer(texte.get("msg.noBlueprints"))
                        .aktion(texte.get("menu.edit"), AKTION_BEARBEITEN, "schaltflaeche-still",
                                blaupause -> rahmen.zeige(formular(blaupause.getTemplateName(), blaupause)))
                        .aktion(texte.get("menu.delete"), AKTION_LOESCHEN, "schaltflaeche-gefahr",
                                this::frageUndLoesche)
                        .baue(datenbank.getAllBlueprints()));
    }

    /** Kennung der Schaltflaeche, die ein leeres Formular oeffnet. */
    public static final String ID_NEU = "blaupause-neu";

    /**
     * Oeffnet ein leeres Formular fuer die erste bekannte Vorlage.
     *
     * <p>Gibt es mehrere Vorlagen, fuehrt der Weg ueber die Seitenleiste; das
     * "+ Neu" ueber der Liste nimmt die erste, damit der haeufige Fall einer
     * einzigen Vorlage ohne Zwischenschritt auskommt.</p>
     */
    public void neu() {
        Map<String, DtaMessage> alle = vorlagen.alle();
        if (alle.isEmpty()) {
            meldungen.hinweis(texte.get("msg.noTemplate"));
            return;
        }
        String erste = alle.keySet().iterator().next();
        rahmen.zeige(formular(erste, null));
    }

    /** Oeffnet ein leeres Formular fuer eine bestimmte Vorlage. */
    public void neu(String vorlagenname) {
        rahmen.zeige(formular(vorlagenname, null));
    }

    /**
     * Baut das Formular auf.
     *
     * @param vorlagenname die Nachrichtenvorlage, aus der die Felder stammen
     * @param vorhandene   die zu bearbeitende Blaupause, oder {@code null}
     */
    Region formular(String vorlagenname, Blueprint vorhandene) {
        DtaMessage vorlage = vorlagen.alle().get(vorlagenname);
        if (vorlage == null) {
            // Kann vorkommen, wenn eine Vorlage umbenannt oder entfernt wurde:
            // die Blaupause haelt deren Namen fest. Die Meldung nennt den Namen,
            // sonst steht der Anwender vor einer leeren Maske ohne Anhalt.
            VBox leer = new VBox(14);
            leer.getStyleClass().add("maske");
            Label hinweis = bausteine.createLabel(
                    String.format(texte.get("msg.templateMissing"), text(vorlagenname)));
            hinweis.setWrapText(true);
            leer.getChildren().add(hinweis);
            meldungen.fehler(String.format(texte.get("msg.templateMissing"), text(vorlagenname)));
            return leer;
        }

        Map<String, String> gespeichert = werteAus(vorhandene);

        VBox wurzel = new VBox(14);
        wurzel.getStyleClass().add("maske");

        Label ueberschrift = bausteine.createLabel(
                texte.get(vorhandene != null ? "title.blueprint.edit" : "title.blueprint.new"));
        ueberschrift.getStyleClass().add("masken-titel");

        TextField namensfeld = bausteine.createTextField();
        namensfeld.setId(ID_NAME);
        namensfeld.setPrefWidth(320);
        namensfeld.setPromptText(texte.get("label.blueprintName"));
        namensfeld.setText(vorhandene != null && vorhandene.getName() != null
                ? vorhandene.getName()
                : vorlagenname + "-blueprint");

        HBox namenszeile = new HBox(10, bausteine.createLabel(texte.get("label.blueprintName")), namensfeld);

        Map<String, Node> felder = new LinkedHashMap<>();
        VBox feldbereich = new VBox(12);
        for (SegmentInfo segment : vorlage.getSegments()) {
            segment.getValueFields().forEach((name, eintrag) -> {
                if (eintrag.isInternal() || eintrag.getPersonRole() != null) {
                    return;
                }
                Node feld = feldbau.erzeugeFeld(name, beschreibungAus(eintrag),
                        eintrag.getBeschreibung(), vorlagen.auswahlFuer(name));
                vorbelegen(feld, gespeichert.get(name));
                felder.put(name, feld);

                Label beschriftung = bausteine.createLabel(name);
                feldbereich.getChildren().add(new VBox(3, beschriftung, feld));
            });
        }

        Button speichern = bausteine.createButton(texte.get("button.save"));
        speichern.setId(ID_SPEICHERN);
        speichern.getStyleClass().add("schaltflaeche-haupt");
        speichern.setOnAction(ereignis -> speichere(vorlagenname, vorhandene, namensfeld, felder));

        Button abbrechen = bausteine.createButton(texte.get("button.cancel"));
        abbrechen.setId(ID_ABBRECHEN);
        abbrechen.getStyleClass().add("schaltflaeche-still");
        abbrechen.setOnAction(ereignis -> rahmen.zeige(liste()));

        wurzel.getChildren().addAll(ueberschrift, namenszeile, feldbereich,
                new HBox(10, speichern, abbrechen));
        return wurzel;
    }

    // --- Speichern und Loeschen ------------------------------------------

    private void speichere(String vorlagenname, Blueprint vorhandene, TextField namensfeld,
            Map<String, Node> felder) {
        String name = namensfeld.getText() == null ? "" : namensfeld.getText().trim();
        if (name.isBlank()) {
            meldungen.hinweis(texte.get("msg.blueprintNameRequired"));
            return;
        }

        Map<String, String> werte = new LinkedHashMap<>();
        felder.forEach((feldname, feld) -> werte.put(feldname, feldbau.textVon(feld)));

        List<String> beanstandungen = new ArrayList<>(feldbau.beanstandungen(felder.values()));
        // Ohne Preis greift die Vorbelegung aus Leistungsparameter - 15.000,00
        // je Termin. Das faellt niemandem auf, weil nichts fehlschlaegt: die
        // Nachricht entsteht, durchlaeuft die Pruefung und geht an die Kasse.
        // Eine Blaupause ohne Preis ist deshalb schlimmer als gar keine.
        if (werte.getOrDefault(FELD_EINZELBETRAG, "").isBlank()) {
            beanstandungen.add(texte.get("msg.blueprintNeedsPrice"));
        }
        if (!beanstandungen.isEmpty()) {
            meldungen.fehler(texte.get("msg.notSaved") + "\n· " + String.join("\n· ", beanstandungen));
            return;
        }

        try {
            Map<String, Object> inhalt = new LinkedHashMap<>();
            inhalt.put("template", vorlagenname);
            inhalt.put("fields", werte);
            String json = leser.writeValueAsString(inhalt);

            Blueprint blaupause = vorhandene != null ? vorhandene
                    : new Blueprint(name, vorlagenname, json, OffsetDateTime.now());
            blaupause.setName(name);
            blaupause.setTemplateName(vorlagenname);
            blaupause.setPayload(json);
            datenbank.saveBlueprint(blaupause);
        } catch (Exception e) {
            meldungen.fehler(e.getMessage());
            return;
        }

        meldungen.erfolg(texte.get("msg.blueprintSaved"));
        rahmen.zeige(liste());
    }

    private void frageUndLoesche(Blueprint blaupause) {
        meldungen.frageNach(String.format(texte.get("msg.deleteConfirmBody"), text(blaupause.getName())),
                texte.get("button.delete"), () -> loesche(blaupause));
    }

    private void loesche(Blueprint blaupause) {
        try {
            datenbank.deleteBlueprint(blaupause);
        } catch (RuntimeException e) {
            meldungen.fehler(e.getMessage());
            return;
        }
        meldungen.erfolg(texte.get("msg.blueprintDeleted"));
        rahmen.leeren();
    }

    // --- Hilfen ----------------------------------------------------------

    /**
     * Uebersetzt einen Feldeintrag in das, was {@link Feldbau} braucht.
     *
     * <p>{@code TagList} ist die Sprache, in der die Personenmasken ihre Felder
     * beschreiben. Sie hier wiederzuverwenden erspart eine zweite Beschreibung
     * derselben Sache - und damit die Gefahr, dass beide auseinanderlaufen.</p>
     */
    private TagList beschreibungAus(ValueFieldEntry eintrag) {
        List<de.gkvtransmitter.util.ModifierInstance> regeln = new ArrayList<>();
        if (eintrag.getMaxLength() > 0) {
            regeln.add(new MaxLengthModifier(eintrag.getMaxLength()));
        }
        return new TagList(eintrag.getInputField(), regeln);
    }

    /** Traegt einen gespeicherten Wert in ein Feld ein. */
    private void vorbelegen(Node feld, String wert) {
        if (wert == null || wert.isBlank()) {
            return;
        }
        Node bedienelement = feldbau.bedienelement(feld);
        if (bedienelement instanceof javafx.scene.control.TextInputControl eingabe) {
            eingabe.setText(wert);
        } else if (bedienelement instanceof javafx.scene.control.ComboBox<?> auswahl) {
            auswahl.getEditor().setText(wert);
        }
    }

    /** Liest die gespeicherten Feldwerte einer Blaupause. */
    private Map<String, String> werteAus(Blueprint blaupause) {
        Map<String, String> werte = new LinkedHashMap<>();
        if (blaupause == null || blaupause.getPayload() == null || blaupause.getPayload().isBlank()) {
            return werte;
        }
        try {
            JsonNode felder = leser.readTree(blaupause.getPayload()).path("fields");
            if (felder.isObject()) {
                felder.fields().forEachRemaining(eintrag ->
                        werte.put(eintrag.getKey(), eintrag.getValue().asText("")));
            }
        } catch (Exception e) {
            // Eine unlesbare Blaupause soll das Formular nicht verhindern - sie
            // laesst sich dann neu ausfuellen und ueberschreiben.
            meldungen.hinweis(texte.get("msg.blueprintUnreadable"));
        }
        return werte;
    }

    /** Der Preis je Termin, wie er in der Blaupause steht. */
    private String preis(Blueprint blaupause) {
        String wert = werteAus(blaupause).get(FELD_EINZELBETRAG);
        return wert == null || wert.isBlank() ? texte.get("label.notSet") : wert;
    }

    private String angelegt(OffsetDateTime zeitpunkt) {
        return zeitpunkt == null ? "" : zeitpunkt.format(ANGELEGT);
    }

    private static String text(String wert) {
        return wert == null ? "" : wert;
    }
}
