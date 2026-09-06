package de.gkvtransmitter.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Der Rahmen der Anwendung: Seitenleiste links, Maske rechts, Meldungen oben
 * rechts, Statuszeile unten.
 *
 * <p>Tritt an die Stelle der Menueleiste. Ein Menue zeigt nach dem Loslassen
 * keine Spur mehr davon, wo man ist; die Seitenleiste laesst den gewaehlten
 * Bereich stehen. Nebenbei verschwindet damit ein Kunstgriff, der noetig war,
 * um "Abrechnung" mit einem Klick zu oeffnen: das Menue feuerte seinen einzigen
 * Eintrag beim Aufklappen selbst ab und schloss sich sofort wieder.</p>
 *
 * <p>Der Rahmen ist zugleich der {@link Maskenrahmen}. {@link #leeren()} zeigt
 * nicht etwa nichts, sondern oeffnet den gewaehlten Bereich erneut - nach dem
 * Speichern eines Teilnehmers landet man so wieder in der Teilnehmerliste und
 * nicht vor einer leeren Flaeche.</p>
 */
public class Hauptfenster implements Maskenrahmen {

    /** Kennung der Ueberschrift ueber der Maske. */
    public static final String ID_TITEL = "kopf-titel";
    /** Vorsatz der Kennung eines Navigationseintrags, gefolgt von seinem Namen. */
    public static final String ID_NAVIGATION = "nav-";
    /** Kennung der Statuszeile. */
    public static final String ID_STATUS = "statuszeile";
    /** Kennung des Info-Zeichens neben der Statuszeile. */
    public static final String ID_STATUS_INFO = "statuszeile-info";
    /** Kennung der Zeile unter der Ueberschrift. */
    public static final String ID_UNTERTITEL = "kopf-untertitel";

    private final BorderPane geruest = new BorderPane();
    private final VBox seitenleiste = new VBox();
    private final ToggleGroup navigationsgruppe = new ToggleGroup();
    private final ScrollPane inhaltsbereich = new ScrollPane();
    private final Label ueberschrift = new Label();
    private final Label untertitel = new Label();
    private final Label statuszeile = new Label();

    /**
     * Der Fenstertitel, gebunden an den offenen Bereich.
     *
     * <p>Die Titelleiste ist die von Windows und bleibt es auch. Sie selbst zu
     * zeichnen hiesse {@code StageStyle.UNDECORATED} und Verschieben,
     * Groessenaenderung, Maximieren und Schliessen von Hand nachzubauen - und
     * dabei das Andocken zu verlieren, mit dem man ein Fenster an den
     * Bildschirmrand zieht. Fuer eine Anwendung, in der jemand einen Monat
     * lang arbeitet, ist das ein schlechter Tausch: man gewaenne Aussehen und
     * verloere Bedienung.</p>
     *
     * <p>Was sich <em>ohne</em> diesen Tausch verbessern liess, ist der Inhalt
     * der Leiste. Dort stand immer nur "GKVTransmitter"; in der Taskleiste und
     * beim Umschalten zwischen Fenstern sagte das nichts darueber, wo man
     * gerade ist. Jetzt steht der offene Bereich davor.</p>
     */
    private final javafx.beans.property.ReadOnlyStringWrapper fenstertitel =
            new javafx.beans.property.ReadOnlyStringWrapper(View.PROGRAMMNAME);
    /** Das Info-Zeichen rechts neben der Statuszeile, siehe {@link #setzeStatusInfo}. */
    private final Button statusinfo = new Button("i");
    private final HBox statusbereich = new HBox(8);
    private final StackPane wurzel;

    /** Die Bereiche in der Reihenfolge ihrer Aufnahme. */
    private final Map<String, Runnable> bereiche = new LinkedHashMap<>();

    private String offenerBereich;

    /** Was beim Wechsel in einen anderen Bereich geschieht, siehe {@link #oeffne}. */
    private Runnable beimBereichswechsel = () -> { };

    /**
     * Legt fest, was beim Wechsel in einen anderen Bereich geschieht.
     *
     * <p>Gedacht fuer das Raeumen der Meldungsecke. Als Haken und nicht als
     * Konstruktorparameter, weil der Rahmen die Meldungen sonst kennen muesste
     * und die Meldungsecke bereits als fertiger Knoten hereinkommt.</p>
     */
    public void beiBereichswechsel(Runnable was) {
        this.beimBereichswechsel = was == null ? () -> { } : was;
    }

    /**
     * @param meldungsecke der Knoten, der ueber die Maske gelegt wird
     */
    public Hauptfenster(Region meldungsecke) {
        Objects.requireNonNull(meldungsecke, "meldungsecke must not be null");

        seitenleiste.getStyleClass().add("seitenleiste");

        ueberschrift.setId(ID_TITEL);
        ueberschrift.getStyleClass().add("kopf-titel");
        untertitel.setId(ID_UNTERTITEL);
        untertitel.getStyleClass().add("kopf-untertitel");
        untertitel.setWrapText(true);
        untertitel.setVisible(false);
        untertitel.setManaged(false);
        VBox kopfzeile = new VBox(2, ueberschrift, untertitel);
        kopfzeile.getStyleClass().add("kopfzeile");

        inhaltsbereich.getStyleClass().add("inhalt");
        inhaltsbereich.setFitToWidth(true);

        statuszeile.setId(ID_STATUS);
        statuszeile.getStyleClass().add("statuszeile-text");

        statusinfo.setId(ID_STATUS_INFO);
        statusinfo.getStyleClass().add("info-zeichen");
        statusinfo.setVisible(false);
        statusinfo.setManaged(false);

        statusbereich.getStyleClass().add("statuszeile");
        statusbereich.setMaxWidth(Double.MAX_VALUE);
        statusbereich.setAlignment(Pos.CENTER_LEFT);
        statusbereich.getChildren().addAll(statuszeile, statusinfo);

        // Die Seitenleiste reicht ueber die volle Hoehe; Kopfzeile und
        // Statuszeile gehoeren nur zur Maske daneben. Laege die Kopfzeile im
        // aeusseren Rahmen oben, schoebe sie die Seitenleiste nach unten und
        // der Programmname stuende nicht mehr in der Ecke.
        BorderPane inhaltsspalte = new BorderPane();
        inhaltsspalte.setTop(kopfzeile);
        inhaltsspalte.setCenter(inhaltsbereich);
        inhaltsspalte.setBottom(statusbereich);

        geruest.setLeft(seitenleistenbereich());
        geruest.setCenter(inhaltsspalte);

        wurzel = new StackPane(geruest, meldungsecke);
        // Oben links verankert, nicht zentriert. Eine StackPane zentriert ihre
        // Kinder, und wenn der Rahmen groesser wird als das Fenster - bei
        // wenig Hoehe tut er das -, verliert man dabei oben *und* unten
        // gleichzeitig. Am 06.09.2026 fehlte so bei 760x500 der Programmname
        // in der Ecke, waehrend unten "Testdaten anlegen" abgeschnitten war.
        // Verankert laeuft der Ueberhang nach rechts unten, wo ihn die
        // Bildlaufleisten auffangen.
        StackPane.setAlignment(geruest, Pos.TOP_LEFT);
        // Unten rechts, nicht oben rechts. Oben rechts liegt bei einem
        // zweispaltigen Formular genau ueber der rechten Spalte: eine stehende
        // Fehlermeldung verdeckte dort "Nachname" und "Land" - also die
        // Felder, zu deren Berichtigung sie auffordert. Unten rechts ist in
        // allen Masken die leerste Ecke; im schlimmsten Fall verdeckt eine
        // Meldung dort eine Listenzeile, und die kann man wegscrollen.
        StackPane.setAlignment(meldungsecke, Pos.BOTTOM_RIGHT);
    }

    /**
     * Die Seitenleiste in einem Bereich, der bei wenig Hoehe rollt.
     *
     * <p>Sie wuchs mit jedem Kurs: Marke, Untertitel, drei Abschnitte, acht
     * Eintraege — und je Vorlage einer mehr. Bei 600 Punkten Hoehe, der
     * <b>Startgroesse der Anwendung</b>, reichte der Platz am 06.09.2026 schon
     * nicht mehr: die umbrechenden Beschriftungen wurden auf eine Zeile
     * gequetscht und mit Auslassungspunkten gekuerzt
     * ("Rückbildungskurs nach …"), und bei noch weniger Hoehe fielen die
     * unteren Eintraege ganz heraus.</p>
     *
     * <p>Die Bildlaufleiste erscheint nur, wenn sie gebraucht wird; quer wird
     * nie gerollt, die Leiste hat eine feste Breite.</p>
     *
     * <p><b>Folge fuer Tests:</b> der Inhalt eines {@code ScrollPane} haengt
     * erst im Knotenbaum, wenn die Darstellung aufgebaut ist. Ein
     * {@code lookup} auf einen Navigationseintrag laeuft vorher ins Leere —
     * dieselbe Falle wie bei den Masken. {@code HauptfensterTest} haengt den
     * Rahmen deshalb kurz in eine {@code Scene}.</p>
     */
    private Region seitenleistenbereich() {
        ScrollPane bereich = new ScrollPane(seitenleiste);
        bereich.getStyleClass().add("seitenleisten-bereich");
        bereich.setFitToWidth(true);
        bereich.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bereich.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Ohne das bestimmt der Inhalt die Mindesthoehe, und der Bereich
        // koennte gar nicht kleiner werden als das, was er anzeigen soll -
        // dann rollte nichts, sondern es liefe wieder ueber.
        bereich.setMinHeight(0);
        return bereich;
    }

    /** Setzt Name und Untertitel oben in der Seitenleiste. */
    public void setzeMarke(String name, String zusatz) {
        Label marke = new Label(name);
        marke.getStyleClass().add("marke");
        Label untertitel = new Label(zusatz);
        untertitel.getStyleClass().add("marke-zusatz");
        untertitel.setWrapText(true);
        seitenleiste.getChildren().addAll(0, java.util.List.of(marke, untertitel));
    }

    /** Ueberschrift ueber einer Gruppe von Bereichen. */
    public void ergaenzeAbschnitt(String beschriftung) {
        Label ueberschriftDesAbschnitts = new Label(beschriftung.toUpperCase(java.util.Locale.GERMAN));
        ueberschriftDesAbschnitts.getStyleClass().add("nav-gruppe");
        seitenleiste.getChildren().add(ueberschriftDesAbschnitts);
    }

    /**
     * Ergaenzt einen Bereich in der Seitenleiste.
     *
     * @param oeffnen was geschieht, wenn er gewaehlt wird
     */
    public void ergaenzeBereich(String beschriftung, Runnable oeffnen) {
        ergaenzeBereich(beschriftung, null, oeffnen);
    }

    /**
     * Ergaenzt einen Bereich mit einem ausfuehrlicheren Namen dahinter.
     *
     * <p>Die Seitenleiste ist schmal, und JavaFX kuerzt zu lange
     * Beschriftungen mit Auslassungspunkten: aus der Vorlage
     * "Geburtsvorbereitungskurs, Einzelabrechnung" wurde
     * "Geburtsvorbereitungsk...". Bei <em>zwei</em> Vorlagen, die beide mit
     * demselben Wortstamm beginnen koennten, ist das nicht nur haesslich,
     * sondern mehrdeutig.</p>
     *
     * <p>Die Zeile traegt deshalb den kurzen Namen und den vollen als
     * Kurzhinweis. Ein Kurzhinweis ist kein Fenster - er haelt keinen Faden an
     * und verlangt keinen Klick.</p>
     *
     * @param vollerName was beim Verweilen erscheint, oder {@code null}
     */
    public void ergaenzeBereich(String beschriftung, String vollerName, Runnable oeffnen) {
        bereiche.put(beschriftung, oeffnen);

        ToggleButton eintrag = new ToggleButton(beschriftung);
        if (vollerName != null && !vollerName.equals(beschriftung)) {
            eintrag.setTooltip(new Tooltip(vollerName));
        }
        eintrag.setId(kennung(beschriftung));
        eintrag.getStyleClass().add("nav-eintrag");
        eintrag.setMaxWidth(Double.MAX_VALUE);
        // Umbrechen statt kuerzen. Ein Vorlagenname wie "Rueckbildungskurs nach
        // Geburten" passt in keine vertretbar schmale Leiste; abgeschnitten
        // ("Rueckbildungskurs nach Geb...") sagt er weniger als zweizeilig.
        eintrag.setWrapText(true);
        eintrag.setToggleGroup(navigationsgruppe);
        // Ein erneuter Klick auf den offenen Bereich soll ihn neu laden und
        // nicht die Auswahl aufheben - sonst stuende man ohne Bereich da.
        eintrag.setOnAction(ereignis -> {
            eintrag.setSelected(true);
            oeffne(beschriftung);
        });
        seitenleiste.getChildren().add(eintrag);
    }

    /**
     * Die Kennung eines Navigationseintrags.
     *
     * <p>Alles, was kein Buchstabe und keine Ziffer ist, wird zum Bindestrich.
     * Zuvor stand die Beschriftung unveraendert in der Kennung, und
     * "Testdaten anlegen" ergab die Kennung {@code nav-testdaten anlegen} -
     * <b>mit Leerzeichen</b>. Ein {@code lookup("#nav-testdaten anlegen")}
     * findet die nie: der Doppelpunkt-Ausdruck liest das Leerzeichen als
     * Trennung zwischen zwei Bedingungen und sucht nach einem Nachfahren.
     * Aufgefallen ist es erst, als die Vorschau den Punkt anklicken wollte -
     * die vorhandenen Tests treffen nur einwortige Bereiche.</p>
     *
     * <p>Fuer das Bedienwerkzeug aus Abschnitt H der Uebergabe ist das die
     * Voraussetzung: was nicht adressierbar ist, laesst sich nicht steuern.</p>
     */
    static String kennung(String beschriftung) {
        return Kennungen.aus(ID_NAVIGATION, beschriftung);
    }

    /** Oeffnet den ersten aufgenommenen Bereich. Fuer den Programmstart. */
    public void oeffneErstenBereich() {
        bereiche.keySet().stream().findFirst().ifPresent(erster -> {
            navigationsgruppe.getToggles().stream()
                    .filter(ToggleButton.class::isInstance)
                    .map(ToggleButton.class::cast)
                    .filter(eintrag -> erster.equals(eintrag.getText()))
                    .findFirst()
                    .ifPresent(eintrag -> eintrag.setSelected(true));
            oeffne(erster);
        });
    }

    private void oeffne(String beschriftung) {
        Runnable oeffnen = bereiche.get(beschriftung);
        if (oeffnen == null) {
            return;
        }
        // Beim Wechsel in einen anderen Bereich die Meldungen raeumen. Ein
        // Fehler bleibt stehen, bis jemand ihn zur Kenntnis nimmt - aber
        // woandershin zu gehen ist Kenntnisnahme. Ohne das stand nach dem
        // Durchlauf vom 05.09.2026 in der Abrechnungsmaske noch die
        // Beanstandung aus dem Teilnehmerformular: "Nicht gespeichert", waehrend
        // seither dreimal erfolgreich gespeichert worden war.
        //
        // Nur beim echten Wechsel: rahmen.leeren() oeffnet denselben Bereich
        // erneut, und das geschieht unmittelbar nach einer Erfolgsmeldung -
        // die wuerde sonst geloescht, ehe sie jemand liest.
        if (!beschriftung.equals(offenerBereich)) {
            beimBereichswechsel.run();
        }
        offenerBereich = beschriftung;
        ueberschrift.setText(beschriftung);
        setzeUntertitel(untertitel.getProperties().get(beschriftung));
        fenstertitel.set(fenstertitel(beschriftung));
        oeffnen.run();
    }

    /**
     * Hinterlegt eine Zeile, die unter der Ueberschrift eines Bereichs steht.
     *
     * <p>Die Kopfzeile trug bisher nur ein Wort - "Teilnehmer", "Gruppen" -
     * und darunter kam sofort die Liste. Ein Satz dazu, was der Bereich ist,
     * kostet nichts und beantwortet die Frage, die sich beim ersten Mal
     * stellt. Fehlt er, bleibt die Zeile weg und nimmt keinen Platz.</p>
     */
    public void erklaereBereich(String beschriftung, String satz) {
        untertitel.getProperties().put(beschriftung, satz);
        if (beschriftung.equals(offenerBereich)) {
            setzeUntertitel(satz);
        }
    }

    private void setzeUntertitel(Object satz) {
        boolean vorhanden = satz instanceof String text && !text.isBlank();
        untertitel.setText(vorhanden ? String.valueOf(satz) : "");
        untertitel.setVisible(vorhanden);
        untertitel.setManaged(vorhanden);
    }

    /** Der Fenstertitel; folgt dem offenen Bereich. */
    public javafx.beans.property.ReadOnlyStringProperty fenstertitel() {
        return fenstertitel.getReadOnlyProperty();
    }

    /**
     * Bereich und Programmname, aber nicht zweimal dasselbe Wort.
     *
     * <p>Seit die Anwendung „GKV-Abrechnung" heisst, ergab der Bereich
     * „Abrechnung" die Titelleiste „Abrechnung – GKV-Abrechnung". Das ist
     * nicht falsch, liest sich aber wie ein Versehen. Steht der Bereichsname
     * schon im Programmnamen, genuegt dieser.</p>
     */
    static String fenstertitel(String bereich) {
        if (bereich == null || bereich.isBlank()
                || View.PROGRAMMNAME.toLowerCase(java.util.Locale.GERMAN)
                        .contains(bereich.toLowerCase(java.util.Locale.GERMAN))) {
            return View.PROGRAMMNAME;
        }
        return bereich + " – " + View.PROGRAMMNAME;
    }

    public void setzeStatus(String text) {
        statuszeile.setText(text == null ? "" : text);
    }

    /**
     * Haengt ein Info-Zeichen an die Statuszeile.
     *
     * <p>Dort stand bis zum 05.09.2026 die Aufzaehlung aller geladenen
     * JSON-Dateien samt Nachrichtentypen - der laengste Text im ganzen
     * Programm, entsprechend abgeschnitten, und ihr eigener Kommentar nannte
     * sie "sichtbare Debug-Hilfe im UI". Der Dateiname interessiert niemanden,
     * der abrechnet. Die Zeile sagt jetzt, <em>wie viele</em> Vorlagen geladen
     * sind; wer wissen will, welche, klickt hier.</p>
     *
     * <p>Die Einzelheiten erscheinen ueber {@code Meldungen} in der
     * Meldungsecke, nicht in einem Fenster - die Anwendung oeffnet keine.</p>
     *
     * <p>Der Kurzhinweis ist kein Beiwerk: ein "i" allein sagt nicht, was
     * dahinter liegt, und wer es nicht anklickt, erfaehrt es nie. Der Text
     * stand seit dem 05.09.2026 als {@code status.info} in
     * {@code ui-messages.json} und wurde nirgends verwendet - dieselbe Falle
     * wie ein Zeichen ohne Inhalt, nur andersherum.</p>
     *
     * @param kurzhinweis was beim Ueberfahren erscheint
     * @param beiKlick was gezeigt wird, oder {@code null}, um das Zeichen zu
     *        verbergen
     */
    public void setzeStatusInfo(String kurzhinweis, Runnable beiKlick) {
        statusinfo.setOnAction(beiKlick == null ? null : ereignis -> beiKlick.run());
        statusinfo.setTooltip(kurzhinweis == null || kurzhinweis.isBlank()
                ? null : new Tooltip(kurzhinweis));
        statusinfo.setVisible(beiKlick != null);
        statusinfo.setManaged(beiKlick != null);
    }

    /** Der Knoten, der in die Szene gehaengt wird. */
    public Parent wurzel() {
        return wurzel;
    }

    @Override
    public void zeige(Region inhalt) {
        inhaltsbereich.setContent(inhalt);
    }

    /**
     * Was gerade zu sehen ist, oder {@code null}.
     *
     * <p>Nicht ueber den Knotenbaum zu erreichen: der Inhalt eines
     * {@code ScrollPane} haengt erst darin, wenn die Darstellung aufgebaut ist.
     * Genau deshalb liefern die Masken ihren nackten Bereich.</p>
     */
    public Node gezeigterInhalt() {
        return inhaltsbereich.getContent();
    }

    /**
     * Kehrt zum offenen Bereich zurueck.
     *
     * <p>Zuvor hiess Leeren woertlich: nach dem Speichern einer Gruppe blieb
     * eine leere Flaeche zurueck, und man musste sich selbst wieder
     * hinnavigieren.</p>
     */
    @Override
    public void leeren() {
        if (offenerBereich != null) {
            oeffne(offenerBereich);
            return;
        }
        inhaltsbereich.setContent(null);
    }
}
