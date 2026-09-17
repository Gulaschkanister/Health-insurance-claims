package de.gkvtransmitter.presentation;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.entity.Protokolleintrag;
import de.gkvtransmitter.presentation.meldung.Meldungen;
import de.gkvtransmitter.repository.DataRepository;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Was wann an wen ging - und worauf noch Geld aussteht.
 *
 * <p>Das Protokoll ist keine Bequemlichkeit: Anlage 1, Abschnitt 3 Absatz 2
 * verlangt eine Dokumentation des Datenaustauschs, mindestens zwei Jahre
 * aufzubewahren. Geschrieben wird sie beim Versand
 * ({@code DtaDispatchService}); diese Maske zeigt sie.</p>
 *
 * <p><b>Der Zustand „bezahlt" ist der eigentliche Gewinn.</b> Absatz 4
 * desselben Abschnitts verlangt eine Sicherungskopie <em>bis zur
 * Bezahlung</em>. Ohne einen Vermerk darüber weiß niemand, welche Kopien noch
 * gebraucht werden - und niemand sieht, welche Lieferung noch nicht bezahlt
 * ist.</p>
 */
public class ProtokollMaske {

    /** Kennung der Zeile, die den Stand der offenen Zahlungen nennt. */
    public static final String ID_STAND = "protokoll-stand";
    /** Kennung der Liste der Eintraege. */
    public static final String ID_LISTE = "protokoll-liste";
    /** Kennungsanfang der Schaltflaechen „Bezahlt"; dahinter steht die Kennung. */
    public static final String ID_BEZAHLT = "protokoll-bezahlt-";

    private static final DateTimeFormatter ZEITPUNKT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final UiFactory bausteine;
    private final AppMessages texte;
    private final Meldungen meldungen;
    private final DataRepository datenbank;
    private final Maskenrahmen rahmen;

    public ProtokollMaske(UiFactory bausteine, AppMessages texte, Meldungen meldungen,
            DataRepository datenbank, Maskenrahmen rahmen) {
        this.bausteine = Objects.requireNonNull(bausteine, "bausteine must not be null");
        this.texte = Objects.requireNonNull(texte, "texte must not be null");
        this.meldungen = Objects.requireNonNull(meldungen, "meldungen must not be null");
        this.datenbank = Objects.requireNonNull(datenbank, "datenbank must not be null");
        this.rahmen = Objects.requireNonNull(rahmen, "rahmen must not be null");
    }

    /** Die Uebersicht. */
    public Region liste() {
        List<Protokolleintrag> eintraege = datenbank.ladeProtokoll();

        VBox wurzel = new VBox(16);
        wurzel.getStyleClass().add("maske");
        wurzel.getChildren().add(kopf());
        wurzel.getChildren().add(stand(eintraege));

        VBox liste = new VBox(8);
        liste.setId(ID_LISTE);
        if (eintraege.isEmpty()) {
            Label leer = bausteine.createLabel(texte.get("log.empty"));
            leer.getStyleClass().add("feld-hinweis");
            liste.getChildren().add(leer);
        } else {
            for (Protokolleintrag eintrag : eintraege) {
                liste.getChildren().add(zeile(eintrag));
            }
        }
        wurzel.getChildren().add(liste);
        return wurzel;
    }

    private Region kopf() {
        Label titel = bausteine.createLabel(texte.get("log.title"));
        titel.getStyleClass().add("masken-titel");
        Label erklaerung = bausteine.createLabel(texte.get("log.subtitle"));
        erklaerung.getStyleClass().add("feld-hinweis");
        erklaerung.setWrapText(true);
        return new VBox(4, titel, erklaerung);
    }

    /** Wie viele Lieferungen noch auf Zahlung warten. */
    private Region stand(List<Protokolleintrag> eintraege) {
        long offen = eintraege.stream().filter(Protokolleintrag::wartetAufZahlung).count();
        // Einzahl und Mehrzahl getrennt. „1 Lieferungen warten" stand hier
        // zuerst - derselbe Fehler, den die Erfolgsmeldung der Abrechnung
        // schon einmal hatte: eine Eins vor einer Mehrzahl.
        String stand;
        if (offen == 0) {
            stand = texte.get("log.allPaid");
        } else if (offen == 1) {
            stand = texte.get("log.openOne");
        } else {
            stand = String.format(texte.get("log.open"), offen);
        }
        Label zeile = bausteine.createLabel(stand);
        zeile.setId(ID_STAND);
        zeile.getStyleClass().add(offen == 0 ? "feld-hinweis" : "feld-beschriftung");
        zeile.setWrapText(true);
        return zeile;
    }

    /**
     * Eine Zeile je Lieferung.
     *
     * <p>Gezeigt wird, was jemand sucht, der eine Lieferung wiederfinden will:
     * Datei, Zeitpunkt, Empfaenger, laufende Nummer - und ob sie bezahlt ist.
     * Die uebrigen Pflichtangaben stehen in der Datenbank und sind fuer die
     * Aufbewahrungspflicht da, nicht fuer den Bildschirm.</p>
     */
    private Region zeile(Protokolleintrag eintrag) {
        Label kopfzeile = bausteine.createLabel(eintrag.getDateiname());
        kopfzeile.getStyleClass().add("feld-beschriftung");

        Label unterzeile = bausteine.createLabel(beschreibung(eintrag));
        unterzeile.getStyleClass().add("feld-hinweis");
        unterzeile.setWrapText(true);

        VBox texteZeile = new VBox(2, kopfzeile, unterzeile);
        HBox.setHgrow(texteZeile, Priority.ALWAYS);

        HBox zeile = new HBox(8, texteZeile);
        zeile.getStyleClass().add("karte");

        if (eintrag.wartetAufZahlung()) {
            Button bezahlt = new Button(texte.get("log.markPaid"));
            bezahlt.setId(ID_BEZAHLT + eintrag.getId());
            bezahlt.getStyleClass().add("schaltflaeche-still");
            bezahlt.setOnAction(ereignis -> markiereBezahlt(eintrag));
            zeile.getChildren().add(bezahlt);
        }
        return zeile;
    }

    private String beschreibung(Protokolleintrag eintrag) {
        StringBuilder text = new StringBuilder();
        if (eintrag.getErstelltAm() != null) {
            text.append(eintrag.getErstelltAm().format(ZEITPUNKT)).append(" · ");
        }
        text.append(texte.get("field.kassenIk")).append(' ').append(eintrag.getEmpfaengerIk())
                .append(" · Nr. ").append(eintrag.getLaufendeNummer())
                .append(" · ").append(eintrag.getGroesseBytes()).append(" Byte · ");
        if (!eintrag.istFehlerfrei()) {
            text.append(texte.get("log.failed"));
            if (eintrag.getFehlerstatus() != null) {
                text.append(": ").append(eintrag.getFehlerstatus());
            }
        } else if (eintrag.getBezahltAm() != null) {
            text.append(String.format(texte.get("log.paid"),
                    eintrag.getBezahltAm().format(ZEITPUNKT)));
        } else {
            text.append(texte.get("log.waiting"));
        }
        return text.toString();
    }

    private void markiereBezahlt(Protokolleintrag eintrag) {
        datenbank.markiereBezahlt(eintrag.getId());
        meldungen.erfolg(texte.get("log.markedPaid"));
        // Neu aufbauen, damit Stand und Schaltflaechen zur Datenbank passen.
        rahmen.zeige(liste());
    }
}
