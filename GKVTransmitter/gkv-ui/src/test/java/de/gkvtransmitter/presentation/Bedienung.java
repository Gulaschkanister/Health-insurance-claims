package de.gkvtransmitter.presentation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Labeled;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;

/**
 * Bedient die Anwendung ueber eine Folge von Befehlen, ohne dass jemand klickt.
 *
 * <p>Simons Idee aus der Uebergabe, Abschnitt H. Der Nutzen ist ein doppelter:
 * ein Ablauf laesst sich <b>als Ganzes</b> pruefen - Person anlegen, Gruppe
 * bilden, Blaupause waehlen, abrechnen -, und wer die Anwendung nicht vor sich
 * hat, kann sie trotzdem bedienen und sehen, was dabei auf dem Bildschirm
 * steht.</p>
 *
 * <p><b>Es tritt nicht an die Stelle der Maskentests.</b> Die sind schnell und
 * sagen genau, was kaputt ist; ein Ablauf sagt nur, dass irgendwo etwas
 * klemmt. Es tritt an die Stelle der Tests, die es <em>nicht</em> gibt: der
 * ueber mehrere Masken hinweg. Bis heute prueft jeder Test eine Maske fuer
 * sich, und dass die vier zusammenpassen, prueft nichts.</p>
 *
 * <h2>Warum das hier und nicht in der Anwendung liegt</h2>
 *
 * <p>Die Uebergabe schlug einen Schalter vor, etwa {@code -Dgkv.dev=true}. Ein
 * Schalter ist aber nur eine Zusage; dieser Quelltext liegt unter
 * {@code src/test/java} und ist damit im ausgelieferten Paket <b>gar nicht
 * vorhanden</b>. Eine Anwendung, die echte Forderungen an Krankenkassen
 * stellt, sollte nicht fernsteuerbar sein - und der beste Weg, das
 * sicherzustellen, ist, die Fernsteuerung nicht mitzuliefern.</p>
 *
 * <h2>Die Befehle</h2>
 *
 * <pre>
 * oeffne Teilnehmer            einen Bereich aus der Seitenleiste
 * klick person-neu             eine Schaltflaeche ueber ihre Kennung
 * setze person-feld-firstname Anna
 * setze person-feld-birthDate 1990-04-17     ein Datum als JJJJ-MM-TT
 * lies person-feld-plz         schreibt den Wert des Feldes
 * zeige                        alles, was auf dem Bildschirm steht
 * erwarte Anna Berger          irgendwo auf dem Bildschirm
 * erwarte-meldung gespeichert  in der Meldungsecke
 * erwarte-nicht Fehler         nirgends auf dem Bildschirm
 * bild schritt3                ein PNG ins Zielverzeichnis
 * #                            eine Zeile Anmerkung
 * </pre>
 *
 * <p>Von Hand aufzurufen mit einer Befehlsdatei:</p>
 *
 * <pre>
 * mvn -q -pl gkv-ui exec:java \
 *     -Dexec.classpathScope=test \
 *     -Dexec.mainClass=de.gkvtransmitter.presentation.Bedienung \
 *     -Dexec.args="ablaeufe/monat.txt"
 * </pre>
 *
 * <p>Ein fehlgeschlagenes {@code erwarte} bricht ab und nennt die Zeile. So
 * laesst sich derselbe Ablauf auch aus einem Test heraus fahren, siehe
 * {@code AblaufTest}.</p>
 */
public final class Bedienung {

    /** Was beim Abarbeiten geschah, Zeile fuer Zeile. */
    private final List<String> protokoll = new ArrayList<>();

    private final Scene szene;
    private final Path ziel;

    public Bedienung(Scene szene, Path ziel) {
        this.szene = szene;
        this.ziel = ziel;
    }

    /**
     * Baut die Anwendung auf und liefert eine Bedienung darauf.
     *
     * <p>Wie in {@link Vorschau}: die Szene braucht ein Fenster, sonst bleibt
     * sie ungezeichnet, aber gezeigt wird es nicht. Die Datenbank liegt unter
     * dem Zielverzeichnis und wird zuvor geloescht - ein Ablauf, der auf
     * Bestaenden des letzten Laufs aufsetzt, prueft nichts Verlaessliches.</p>
     */
    public static Bedienung aufbauen(Path uebergebenesZiel, boolean mitTestdaten) throws Exception {
        // Absolut, sonst loest Anwendungsverzeichnis den Pfad gegen den
        // Datenordner der Anwendung auf - und die Testdatenbank laege im
        // Benutzerprofil statt neben den Bildern. Siehe Fallstricke.
        Path ziel = uebergebenesZiel.toAbsolutePath();
        Files.createDirectories(ziel);
        Path datenbank = de.gkvtransmitter.util.Anwendungsverzeichnis
                .aufloesen(ziel.resolve("bedienung.db"));
        Files.deleteIfExists(datenbank);
        System.setProperty("gkv.db.path", ziel.resolve("bedienung.db").toString());
        System.setProperty("gkv.testdaten", String.valueOf(mitTestdaten));

        JavaFxLaufzeit.starten();

        Controller controller = new Controller();
        View sicht = new View(controller,
                new de.gkvtransmitter.application.AbrechnungService(controller.getDatabase()));

        Bedienung[] gebaut = new Bedienung[1];
        JavaFxLaufzeit.aufFxFaden(() -> {
            Scene szene = sicht.createMainScene(1100, 720);
            javafx.stage.Stage buehne = new javafx.stage.Stage();
            buehne.setScene(szene);
            buehne.setWidth(1100);
            buehne.setHeight(720);
            szene.getRoot().applyCss();
            szene.getRoot().layout();
            gebaut[0] = new Bedienung(szene, ziel);
        });
        return gebaut[0];
    }

    // --- Ausfuehren -------------------------------------------------------

    /** Arbeitet eine Befehlsdatei ab. */
    public void ausDatei(Path skript) throws Exception {
        fuehreAus(Files.readAllLines(skript));
    }

    /**
     * Arbeitet eine Folge von Befehlen ab, alles auf dem JavaFX-Faden.
     *
     * @throws AssertionError sobald ein {@code erwarte} nicht zutrifft
     */
    public void fuehreAus(List<String> zeilen) {
        JavaFxLaufzeit.aufFxFaden(() -> {
            int nummer = 0;
            for (String zeile : zeilen) {
                nummer++;
                String befehl = zeile.strip();
                if (befehl.isEmpty() || befehl.startsWith("#")) {
                    continue;
                }
                try {
                    einzelnerBefehl(befehl);
                } catch (AssertionError fehler) {
                    throw new AssertionError("Zeile " + nummer + ": " + befehl
                            + "\n" + fehler.getMessage()
                            + "\n\nWas auf dem Bildschirm stand:\n" + bildschirm(), fehler);
                }
            }
        });
    }

    private void einzelnerBefehl(String befehl) {
        String[] teile = befehl.split("\\s+", 2);
        String wort = teile[0].toLowerCase(Locale.GERMAN);
        String rest = teile.length > 1 ? teile[1] : "";

        switch (wort) {
            case "oeffne" -> oeffne(rest);
            case "klick" -> klick(rest);
            case "setze" -> setze(rest);
            case "lies" -> notiere("lies " + rest + " = \"" + wertVon(rest) + "\"");
            case "zeige" -> notiere("zeige\n" + bildschirm());
            case "erwarte" -> erwarte(rest);
            case "erwarte-meldung" -> erwarteMeldung(rest);
            case "erwarte-nicht" -> erwarteNicht(rest);
            case "bild" -> bild(rest);
            default -> throw new AssertionError("Unbekannter Befehl: " + wort);
        }
    }

    // --- Die einzelnen Befehle -------------------------------------------

    private void oeffne(String bereich) {
        Node eintrag = suche(Hauptfenster.kennung(bereich));
        if (!(eintrag instanceof ToggleButton knopf)) {
            throw new AssertionError("Kein Bereich \"" + bereich + "\" in der Seitenleiste."
                    + " Vorhanden: " + bereiche());
        }
        knopf.setSelected(true);
        knopf.fire();
        neuZeichnen();
        notiere("oeffne " + bereich);
    }

    private void klick(String kennung) {
        Node knoten = suche(kennung);
        if (!(knoten instanceof ButtonBase knopf)) {
            throw new AssertionError("Keine Schaltflaeche #" + kennung + " auf dem Bildschirm.");
        }
        knopf.fire();
        neuZeichnen();
        notiere("klick " + kennung);
    }

    private void setze(String rest) {
        String[] teile = rest.split("\\s+", 2);
        if (teile.length < 2) {
            throw new AssertionError("setze braucht Kennung und Wert");
        }
        Node feld = suche(teile[0]);
        if (feld == null) {
            throw new AssertionError("Kein Feld #" + teile[0] + " auf dem Bildschirm.");
        }
        // Die Kennung haengt an der Huelle, nicht am Bedienelement - so setzt
        // sie PersonenMaske. Der Weg dorthin ist derselbe wie beim Auslesen
        // durch die Maske selbst.
        Node bedienelement = huelleAufloesen(feld);
        setzeWert(bedienelement, teile[1]);
        neuZeichnen();
        notiere("setze " + teile[0] + " = \"" + teile[1] + "\"");
    }

    private static void setzeWert(Node bedienelement, String wert) {
        switch (bedienelement) {
            case TextInputControl eingabe -> eingabe.setText(wert);
            case DatePicker kalender -> kalender.setValue(java.time.LocalDate.parse(wert));
            case ComboBox<?> auswahl -> auswahlSetzen(auswahl, wert);
            case Spinner<?> zaehler -> zaehler.getEditor().setText(wert);
            default -> throw new AssertionError(
                    "Weiss nicht, wie man " + bedienelement.getClass().getSimpleName() + " setzt");
        }
    }

    /**
     * Waehlt einen Eintrag eines Auswahlfelds.
     *
     * <p>Ueber die angezeigte Beschriftung und nicht ueber den Index: ein
     * Index ist beim Lesen eines Ablaufs nicht zu verstehen und wird falsch,
     * sobald ein Eintrag hinzukommt. Beschreibbare Felder duerfen auch einen
     * Wert bekommen, den es in der Liste nicht gibt.</p>
     */
    private static void auswahlSetzen(ComboBox<?> auswahl, String wert) {
        for (Object eintrag : auswahl.getItems()) {
            if (beschriftung(auswahl, eintrag).equals(wert)) {
                auswahl.getSelectionModel().select(auswahl.getItems().indexOf(eintrag));
                return;
            }
        }
        if (auswahl.isEditable()) {
            auswahl.getEditor().setText(wert);
            return;
        }
        List<String> vorhanden = auswahl.getItems().stream()
                .map(eintrag -> beschriftung(auswahl, eintrag)).toList();
        throw new AssertionError("\"" + wert + "\" steht nicht zur Auswahl. Vorhanden: " + vorhanden);
    }

    @SuppressWarnings("unchecked")
    private static String beschriftung(ComboBox<?> auswahl, Object eintrag) {
        javafx.util.StringConverter<Object> wandler =
                (javafx.util.StringConverter<Object>) auswahl.getConverter();
        return wandler == null ? String.valueOf(eintrag) : wandler.toString(eintrag);
    }

    private String wertVon(String kennung) {
        Node feld = suche(kennung);
        if (feld == null) {
            throw new AssertionError("Kein Feld #" + kennung + " auf dem Bildschirm.");
        }
        return switch (huelleAufloesen(feld)) {
            case TextInputControl eingabe -> eingabe.getText() == null ? "" : eingabe.getText();
            case DatePicker kalender -> String.valueOf(kalender.getValue());
            case ComboBox<?> auswahl -> String.valueOf(auswahl.getValue());
            case Spinner<?> zaehler -> String.valueOf(zaehler.getValue());
            case Labeled beschriftet -> beschriftet.getText();
            default -> "";
        };
    }

    private void erwarte(String text) {
        if (!bildschirm().contains(text)) {
            throw new AssertionError("\"" + text + "\" steht nicht auf dem Bildschirm.");
        }
        notiere("erwarte " + text + "  ✓");
    }

    private void erwarteNicht(String text) {
        if (bildschirm().contains(text)) {
            throw new AssertionError("\"" + text + "\" steht auf dem Bildschirm, sollte aber nicht.");
        }
        notiere("erwarte-nicht " + text + "  ✓");
    }

    private void erwarteMeldung(String text) {
        List<String> meldungen = meldungen();
        if (meldungen.stream().noneMatch(meldung -> meldung.contains(text))) {
            throw new AssertionError("Keine Meldung enthaelt \"" + text + "\"."
                    + " Vorhanden: " + meldungen);
        }
        notiere("erwarte-meldung " + text + "  ✓");
    }

    /**
     * Ein Schnappschuss der Szene.
     *
     * <p><b>Eine gerade erst gezeigte Meldung fehlt darauf.</b> Die
     * Meldungsecke blendet ihre Karten ein, und die Ueberblendung braucht
     * einen Zeichentakt - den es innerhalb eines Befehlsblocks nicht gibt. Die
     * Karte haengt in der Szene, hat aber noch die Deckkraft null. Wer wissen
     * will, was steht, nimmt {@code zeige} oder {@code erwarte-meldung}; das
     * Bild ist fuer das Aussehen da, nicht fuer den Nachweis.</p>
     */
    private void bild(String name) {
        try {
            javafx.scene.image.WritableImage bild = szene.snapshot(null);
            java.awt.image.BufferedImage awt = new java.awt.image.BufferedImage(
                    (int) bild.getWidth(), (int) bild.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            javafx.scene.image.PixelReader leser = bild.getPixelReader();
            for (int y = 0; y < (int) bild.getHeight(); y++) {
                for (int x = 0; x < (int) bild.getWidth(); x++) {
                    awt.setRGB(x, y, leser.getArgb(x, y));
                }
            }
            javax.imageio.ImageIO.write(awt, "png", ziel.resolve(name + ".png").toFile());
            notiere("bild " + name + ".png");
        } catch (Exception e) {
            throw new AssertionError("Konnte kein Bild schreiben: " + e);
        }
    }

    // --- Was auf dem Bildschirm steht ------------------------------------

    /**
     * Aller sichtbare Text, Zeile fuer Zeile.
     *
     * <p>Das ist der eigentliche Grund fuer dieses Werkzeug: wer die Anwendung
     * nicht vor sich hat, sieht hier trotzdem, was dasteht - und zwar so, wie
     * es dasteht, und nicht so, wie es dem Quelltext nach dastehen muesste.
     * Ein Bild sagt mehr, laesst sich aber nicht durchsuchen.</p>
     */
    public String bildschirm() {
        StringBuilder text = new StringBuilder();
        sammle(szene.getRoot(), text);
        return text.toString();
    }

    private static void sammle(Node knoten, StringBuilder text) {
        if (!knoten.isVisible()) {
            return;
        }
        if (knoten instanceof Labeled beschriftet && beschriftet.getText() != null
                && !beschriftet.getText().isBlank()) {
            text.append("  ").append(beschriftet.getText());
            if (knoten.getId() != null) {
                text.append("   [#").append(knoten.getId()).append(']');
            }
            text.append('\n');
        } else if (knoten instanceof TextInputControl eingabe) {
            text.append("  [").append(eingabe.getText() == null ? "" : eingabe.getText()).append(']');
            if (knoten.getId() != null) {
                text.append("   [#").append(knoten.getId()).append(']');
            }
            text.append('\n');
        }
        if (knoten instanceof Parent eltern) {
            for (Node kind : eltern.getChildrenUnmodifiable()) {
                sammle(kind, text);
            }
        }
    }

    /** Die Texte in der Meldungsecke. */
    public List<String> meldungen() {
        List<String> gefunden = new ArrayList<>();
        sammleMeldungen(szene.getRoot(), gefunden);
        return gefunden;
    }

    private static void sammleMeldungen(Node knoten, List<String> gefunden) {
        if (knoten.getStyleClass().contains("meldung")) {
            StringBuilder text = new StringBuilder();
            sammle(knoten, text);
            gefunden.add(text.toString().replace('\n', ' ').strip());
            return;
        }
        if (knoten instanceof Parent eltern) {
            for (Node kind : eltern.getChildrenUnmodifiable()) {
                sammleMeldungen(kind, gefunden);
            }
        }
    }

    /** Was das Werkzeug getan hat, in der Reihenfolge der Befehle. */
    public List<String> protokoll() {
        return List.copyOf(protokoll);
    }

    // --- Kleinkram --------------------------------------------------------

    private List<String> bereiche() {
        List<String> namen = new ArrayList<>();
        sammleBereiche(szene.getRoot(), namen);
        return namen;
    }

    private static void sammleBereiche(Node knoten, List<String> namen) {
        if (knoten instanceof ToggleButton eintrag && eintrag.getText() != null) {
            namen.add(eintrag.getText());
        }
        if (knoten instanceof Parent eltern) {
            for (Node kind : eltern.getChildrenUnmodifiable()) {
                sammleBereiche(kind, namen);
            }
        }
    }

    /**
     * Das Bedienelement zu einer Huelle - ueber {@code Feldbau} selbst.
     *
     * <p>Nicht nachgebaut: der Weg dorthin hat sich schon einmal geaendert,
     * als neben das Bedienelement ein Info-Zeichen trat. Ein Werkzeug, das die
     * Anwendung bedienen soll, muss denselben Weg gehen wie die Anwendung,
     * sonst prueft es einen Weg, den niemand nimmt.</p>
     */
    private static final Feldbau FELDBAU =
            new Feldbau(new JavaFxUiFactory(),
                    new de.gkvtransmitter.util.AppMessages("/messages/ui-messages.json"));

    private static Node huelleAufloesen(Node feld) {
        return Feldbau.bedienelement(feld);
    }

    private Node suche(String kennung) {
        return szene.getRoot().lookup("#" + kennung);
    }

    private void neuZeichnen() {
        szene.getRoot().applyCss();
        szene.getRoot().layout();
    }

    private void notiere(String zeile) {
        protokoll.add(zeile);
    }

    // --- Von Hand ---------------------------------------------------------

    /**
     * Von Hand aufzurufen.
     *
     * <p><b>Alles in einem {@code try/finally}, und der Ausstieg gehoert ins
     * {@code finally}.</b> Am 06.09.2026 blieb ein Lauf zwanzig Minuten lang
     * einfach stehen, ohne eine Zeile auszugeben. Die Ursache war ein
     * veraltetes {@code gkv-core}-Jar und damit ein
     * {@code NoSuchMethodError} — aber zu sehen war das nicht:</p>
     *
     * <ol>
     *   <li>Der Fehler flog aus {@code aufbauen}, also vor dem
     *       {@code Platform.exit()} am Ende.</li>
     *   <li>Der JavaFX-Faden ist kein Daemon und lief damit weiter.</li>
     *   <li>{@code exec:java} wartet auf alle Nicht-Daemon-Faeden, ehe es die
     *       Ausnahme meldet — es wartete also ewig auf einen Faden, den nur
     *       die nie erreichte Zeile haette beenden koennen.</li>
     * </ol>
     *
     * <p>Ein Werkzeug, das einen Fehler verschweigt und stattdessen haengt,
     * ist schlimmer als keines. Deshalb hier: melden, was schiefging, und in
     * jedem Fall aussteigen.</p>
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Aufruf: Bedienung <befehlsdatei> [zielverzeichnis]");
            System.exit(2);
        }
        int schluss = 0;
        try {
            Path ziel = Path.of(args.length > 1 ? args[1] : "target/bedienung").toAbsolutePath();
            Bedienung bedienung = aufbauen(ziel, true);
            try {
                bedienung.ausDatei(Path.of(args[0]));
                System.out.println("Ablauf durchgelaufen.");
            } catch (AssertionError fehler) {
                System.out.println("Ablauf abgebrochen.");
                System.out.println(fehler.getMessage());
                schluss = 1;
            }
            bedienung.protokoll().forEach(System.out::println);
        } catch (Throwable aufbaufehler) {
            // Auch Error faengt hier: NoSuchMethodError aus einem veralteten
            // gkv-core-Jar ist genau der Fall, der das hier noetig gemacht hat.
            System.out.println("Der Aufbau ist gescheitert:");
            aufbaufehler.printStackTrace(System.out);
            schluss = 2;
        } finally {
            javafx.application.Platform.exit();
        }
        System.exit(schluss);
    }
}
