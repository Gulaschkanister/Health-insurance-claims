package de.gkvtransmitter.dta;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;

/**
 * Erzeugt aus einer {@link Abrechnung} die DTA-Nachricht.
 *
 * <p>Eine Lieferung besteht aus zwei Nachrichten: der SLGA mit den
 * Gesamtsummen und der SLLA mit den Einzelleistungen. Beide sind in den
 * Rahmen aus {@code UNB} und {@code UNZ} eingefasst.</p>
 *
 * <p>Die Zaehler in {@code UNT} und {@code UNZ} werden aus den tatsaechlich
 * erzeugten Segmenten abgeleitet. Die Anzahl im {@code UNZ} stand zuvor fest
 * auf 2 - richtig, solange genau zwei Nachrichten entstehen, aber still falsch,
 * sobald sich daran etwas aendert.</p>
 */
public final class DtaFactory {

    private static final DateTimeFormatter HEADER_TIME = DateTimeFormatter.ofPattern("yyyyMMdd:HHmm");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    /** Ortsangabe, solange die Stammdaten keinen Ort fuehren. */
    private static final String ORT_UNBEKANNT = "ORT";

    private DtaFactory() {
    }

    /** Erzeugt die Nachricht mit den Leistungsangaben aus der Blaupause der Abrechnung. */
    public static String buildDtaFor(Abrechnung a, long interchangeRefValue, String senderIk, String receiverIk) {
        return buildDtaFor(a, interchangeRefValue, senderIk, receiverIk,
                Leistungsparameter.ausBlueprint(a.getBlueprint()));
    }

    /**
     * Erzeugt die Nachricht mit ausdruecklich vorgegebenen Leistungsangaben.
     *
     * <p>Ohne Angabe der Art gilt {@link Uebermittlungsart#ERPROBUNG} - der
     * Wert, der bis zum 07.09.2026 fest im UNB stand. Wer eine Echtdatei
     * erzeugen will, muss das sagen.</p>
     *
     * @param a                  die Abrechnung
     * @param interchangeRefValue laufende Datenaustauschreferenz
     * @param senderIk           IK des Absenders
     * @param receiverIk         IK des Empfaengers
     * @param leistung           Betrag und Schluessel der Leistungszeile
     */
    public static String buildDtaFor(Abrechnung a, long interchangeRefValue, String senderIk, String receiverIk,
            Leistungsparameter leistung) {
        return buildDtaFor(a, interchangeRefValue, senderIk, receiverIk, leistung,
                Uebermittlungsart.ERPROBUNG);
    }

    /**
     * Erzeugt die Nachricht und sagt, wofuer sie sich ausgibt.
     *
     * @param a                  die Abrechnung
     * @param interchangeRefValue laufende Datenaustauschreferenz
     * @param senderIk           IK des Absenders
     * @param receiverIk         IK des Empfaengers (Datenannahmestelle)
     * @param leistung           Betrag und Schluessel der Leistungszeile
     * @param art                Test-, Erprobungs- oder Echtdatei
     */
    public static String buildDtaFor(Abrechnung a, long interchangeRefValue, String senderIk, String receiverIk,
            Leistungsparameter leistung, Uebermittlungsart art) {
        return buildDtaFor(a, interchangeRefValue, senderIk, receiverIk, leistung, art,
                Absender.ausLeistungserbringer(senderIk));
    }

    /**
     * Die Version, mit der die Anwendung ihre Nachrichten erzeugt.
     *
     * <p>Sie stand zweimal als Zeichenkette im Quelltext, einmal fuer
     * {@code SLGA} und einmal fuer {@code SLLA}. Als Konstante ist sie
     * ablesbar - das Registrierungsblatt nennt sie, und wer sieht, womit er
     * sendet, merkt einen Wechsel. <b>Anlage 3 in der Fassung V22 ist ab dem
     * 01.02.2027 anzuwenden</b>; bis dahin ist 21 richtig, danach nicht mehr.</p>
     */
    public static final String NACHRICHTENVERSION = "21:0:0";

    /** Der Zeichensatz im {@code UNB}, Anlage 1 Abschnitt 5.4. */
    public static final String ZEICHENSATZ = "UNOC:3";

    /**
     * Erzeugt die Nachricht mit einem eigenen Absender.
     *
     * <p>Der Unterschied zwischen {@code senderIk} und {@code absender} ist der
     * zwischen <b>wer geleistet hat</b> und <b>wer schickt</b>. Das erste steht
     * im {@code FKT}, das zweite im {@code UNB} und im logischen Dateinamen.
     * Sie fallen nur zusammen, solange der Leistungserbringer selbst
     * abrechnet - siehe {@link Absender}.</p>
     *
     * @param senderIk IK des Leistungserbringers, fuer das {@code FKT}
     * @param absender die absendende Stelle, fuer {@code UNB} und Dateinamen
     */
    public static String buildDtaFor(Abrechnung a, long interchangeRefValue, String senderIk, String receiverIk,
            Leistungsparameter leistung, Uebermittlungsart art, Absender absender) {
        return buildDtaFor(a, interchangeRefValue, senderIk, receiverIk, leistung, art, absender,
                Rechnungsangaben.OHNE);
    }

    /**
     * Erzeugt die Nachricht mit vergebenen Rechnungsnummern.
     *
     * <p>{@code REC} und {@code INV} trugen bis zum 18.09.2026 feste Werte:
     * {@code 00000000:0} und eine frei gebildete Belegnummer. Beide sind nach
     * Anlage 1, Abschnitt 5.5.2 Pflichtangaben, und auf Rechnungsnummer und
     * Rechnungsdatum verweist jede spaetere Korrektur.</p>
     *
     * @param rechnung Rechnungs- und Belegnummer; {@link Rechnungsangaben#OHNE}
     *        laesst die Pruefung anschlagen, statt eine Nummer zu erfinden
     */
    public static String buildDtaFor(Abrechnung a, long interchangeRefValue, String senderIk, String receiverIk,
            Leistungsparameter leistung, Uebermittlungsart art, Absender absender,
            Rechnungsangaben rechnung) {
        LocalDateTime now = a.getCreatedAt();
        LocalDate serviceDate = now.toLocalDate();
        String interchangeRef = String.format("%05d", interchangeRefValue);
        // Elf Stellen nach Anhang 1 zur Anlage 1, Abschnitt 4.2 - keine
        // freie Bildung, siehe LogischerDateiname.
        String applicationRef = LogischerDateiname.bilde(absender.ik(), absender.selbstabrechner(), serviceDate);
        // Der Leistungsbereich folgt dem Abrechnungscode der Leistungszeile,
        // er stand bis zum 07.09.2026 fest auf H (Rehabilitationssport).
        String leistungsbereich = Leistungsbereich.zuAbrechnungscode(leistung.abrechnungscode());

        // Menge und Summe muessen aus derselben Zahl entstehen. Zuvor rechnete
        // die Fallsumme mit mindestens einem Termin, die Menge im ENF aber mit
        // der tatsaechlichen Anzahl - bei null Terminen entstand dadurch eine
        // Rechnung ueber 15.000,00 mit einer Leistungsmenge von 0,00.
        int menge = Math.max(0, a.getAppointments());
        // Mit dem GERUNDETEN Einzelbetrag rechnen, denn genau der steht im ENF
        // und genau den multipliziert die Kasse. Zuvor stand hier der
        // ungerundete: bei 12,505 und zwei Terminen ergab das im BES 25,01,
        // waehrend die Kasse aus 12,51 mal 2,00 auf 25,02 kam - die eigene
        // Betragskonsistenzregel schlug an und blockierte den ganzen Lauf.
        BigDecimal fallSum = leistung.einzelbetragGerundet().multiply(BigDecimal.valueOf(menge));
        String sum = formatAmount(fallSum);

        List<String> lines = new ArrayList<>();
        // Die letzte Stelle sagt, wofuer sich die Datei ausgibt: 0 Test,
        // 1 Erprobung, 2 Echt. Sie stand bis zum 07.09.2026 fest auf 1.
        lines.add(String.format("UNB+" + ZEICHENSATZ + "+%s+%s+%s+%s+%s+%s+%s'",
                absender.ik(),
                receiverIk,
                now.format(HEADER_TIME),
                interchangeRef,
                leistungsbereich,
                applicationRef,
                art.kennzeichen()));

        String slgaRef = "00001";
        List<String> slga = new ArrayList<>();
        slga.add(String.format("UNH+%s+SLGA:%s'", slgaRef, NACHRICHTENVERSION));
        slga.add(fktSegment(rechnung, senderIk, receiverIk));
        slga.add(rechnungssegment(rechnung, serviceDate));
        umsatzsteuersegment(a.getProvider()).ifPresent(slga::add);
        slga.add(String.format("GES+00+%s+%s'", sum, sum));
        slga.add(String.format("GES+99+%s+%s'", sum, sum));
        slga.add(buildProviderNameSegment(a.getProvider()));
        slga.add(String.format("UNT+%06d+%s'", slga.size() + 1, slgaRef));
        lines.addAll(slga);

        lines.add("");

        String sllaRef = "00002";
        List<String> slla = new ArrayList<>();
        slla.add(String.format("UNH+%s+SLLA:%s'", sllaRef, NACHRICHTENVERSION));
        slla.add(fktSegment(rechnung, senderIk, receiverIk));
        slla.add(rechnungssegment(rechnung, serviceDate));
        slla.add(buildInvSegment(a.getPatient(), rechnung));
        // Das URI steht zwischen INV und NAD - Anlage 1, Abschnitt 7.3, und
        // zwar nur dort: "URI darf nur angegeben werden bei Uebermittlung
        // einer Rechnung mit Verarbeitungskennzeichen ungleich 01".
        if (rechnung.istKorrektur()) {
            slla.add(rechnung.bezug().alsSegment());
        }
        slla.add(buildNadSegment(a.getPatient()));
        slla.add(buildEnfSegment(a, menge, leistung));
        slla.add(String.format("BES+%s'", sum));
        slla.add(String.format("UNT+%06d+%s'", slla.size() + 1, sllaRef));
        lines.addAll(slla);

        // Die Anzahl ergibt sich aus den erzeugten Nachrichten und wird nicht
        // mehr fest angenommen.
        long nachrichten = lines.stream().filter(zeile -> zeile.startsWith("UNH+")).count();
        lines.add(String.format("UNZ+%06d+%s'", nachrichten, interchangeRef));

        return String.join("\n", lines) + "\n";
    }

    /**
     * Das {@code UST}-Segment - oder keines.
     *
     * <p><b>Hier stand bis zum 17.09.2026 {@code UST+19'}</b>, also ein
     * Umsatzsteuersatz. Das Segment traegt keinen Satz. Nach Anlage 1,
     * Abschnitt 5.5.2 (gleichlautend in Version 21 und 22) besteht es aus:</p>
     *
     * <ol>
     *   <li>dem Segmentkennzeichen {@code UST},</li>
     *   <li>der <b>Steuernummer</b> nach § 14 Abs. 1a UStG oder der
     *       Umsatzsteuer-Identifikationsnummer, hoechstens 20 Stellen,
     *       innerhalb des Segments Pflicht,</li>
     *   <li>der Kennung der UST-Befreiung: <b>{@code J}, wenn befreit gem. § 4
     *       UStG</b>, sonst leer.</li>
     * </ol>
     *
     * <p>Die 19 landete damit im Feld der Steuernummer. Sie stammt aus
     * {@code Information/Valide.DTA}, das an derselben Stelle {@code UST+19}
     * fuehrt - dieselbe Referenzdatei, die dem Projekt schon den
     * Abrechnungscode 61 und den Leistungsbereich H eingetragen hat. <b>Sie ist
     * in sich stimmig und in der Sache dennoch nicht massgeblich.</b></p>
     *
     * <p>Ohne Steuernummer bleibt das Segment weg. Es ist konditional
     * (Segmentart {@code K}, Wiederholungsfaktor 0-1), das Weglassen also
     * erlaubt - eine erfundene Nummer waere es nicht. Dass sie fehlt, meldet
     * {@code DtaDispatchService}.</p>
     */
    static Optional<String> umsatzsteuersegment(ServiceProvider provider) {
        if (provider == null || !provider.hatSteuerangaben()) {
            return Optional.empty();
        }
        return Optional.of(String.format("UST+%s+%s'",
                provider.getSteuernummer().trim(),
                provider.istUmsatzsteuerbefreit() ? "J" : ""));
    }

    private static String buildProviderNameSegment(ServiceProvider provider) {
        if (provider == null) {
            return "NAM+++DR'";
        }
        String last = safe(provider.getLastname()).toUpperCase(Locale.ROOT);
        String first = safe(provider.getFirstname()).toUpperCase(Locale.ROOT);
        return String.format("NAM+%s+%s+DR'", last, first);
    }

    /**
     * Das {@code FKT} mit dem Verarbeitungskennzeichen.
     *
     * <p>Hier stand fest {@code FKT+01}. Fuer Hebammen ist das
     * Korrekturverfahren nach Anlage 1, Abschnitt 7.1 <b>seit dem 01.10.2025
     * verpflichtend</b> - die feste 01 war also keine Vereinfachung, sondern
     * eine Luecke.</p>
     */
    private static String fktSegment(Rechnungsangaben rechnung, String senderIk, String receiverIk) {
        return String.format("FKT+%s++%s+%s+%s+%s'", rechnung.kennzeichenSchluessel(),
                senderIk, receiverIk, receiverIk, senderIk);
    }

    /**
     * Das Rechnungsdatum zu einem Leistungsdatum.
     *
     * <p>Der Vortag: Eine Rechnung kann nicht nach der Leistung datieren, die
     * sie abrechnet. Oeffentlich, weil das Uebermittlungsprotokoll dasselbe
     * Datum festhalten muss - eine Korrektur verweist darauf, und zwei
     * Rechenwege dafuer waeren zwei Gelegenheiten, auseinanderzulaufen.</p>
     */
    public static LocalDate rechnungsdatum(LocalDate leistungsdatum) {
        return leistungsdatum.minusDays(1);
    }

    /**
     * Das {@code REC}: Rechnungsnummer, Rechnungsdatum, Rechnungsart.
     *
     * <p>Das Rechnungsdatum ist der Vortag des Leistungsdatums, wie bisher -
     * eine Rechnung kann nicht nach der Leistung datieren, die sie abrechnet.
     * Die Rechnungsart {@code 1} steht fuer die Erstrechnung nach Anlage 3,
     * Abschnitt 8.1.4.</p>
     */
    private static String rechnungssegment(Rechnungsangaben rechnung, LocalDate serviceDate) {
        return String.format("REC+%s+%s+1'", rechnung.rechnungsnummerFeld(),
                rechnungsdatum(serviceDate).format(BASIC_DATE));
    }

    /**
     * Das {@code INV}: Versichertennummer, Beleginformation, Belegnummer.
     *
     * <p>Die Belegnummer stand hier als {@code "HEB" + Jahr/Monat +
     * Kennnummer der Teilnehmerin} - frei gebildet und ohne Bezug zu
     * irgendetwas Vergebenem. Anlage 1, Abschnitt 5.5.2 verlangt eine
     * "eindeutige Nummer innerhalb der Gesamtrechnung", also <b>weniger</b>
     * als die freie Bildung vorgab: Eindeutig ueber die Zeit ist die
     * Rechnungsnummer im {@code REC}, nicht die Belegnummer.</p>
     */
    private static String buildInvSegment(Patient patient, Rechnungsangaben rechnung) {
        return String.format("INV+%s++1+%s'", zeroPad(patient.getId(), 12),
                rechnung.belegnummerFeld());
    }

    /**
     * Die Angaben zur versicherten Person.
     *
     * <p><b>Fehlt das Geburtsdatum, bleibt das Feld leer.</b> Hier stand bis
     * zum 06.09.2026 ein Ersatzwert {@code 19900101} — ein erfundenes Datum,
     * das an die Kasse gegangen waere, und nebenbei machte es die Regel
     * {@code VERSICHERTER_GEBURTSDATUM} fuer selbst erzeugte Nachrichten zu
     * totem Code: sie konnte nie anschlagen, weil nie etwas fehlte.</p>
     *
     * <p>Jetzt schlaegt sie an, und die Pruefung haelt den Lauf auf, ehe etwas
     * hinausgeht. Das ist der Sinn der vorgeschalteten Pruefung: lieber eine
     * Abrechnung, die nicht losgeht und sagt warum, als eine, die mit einer
     * erfundenen Angabe losgeht.</p>
     */
    private static String buildNadSegment(Patient patient) {
        String last = safe(patient.getLastname()).toUpperCase(Locale.ROOT);
        String first = safe(patient.getFirstname()).toUpperCase(Locale.ROOT);
        String birth = patient.getBirthDate() != null ? patient.getBirthDate().format(BASIC_DATE) : "";
        String street = safe(patient.getStreet()).toUpperCase(Locale.ROOT) + " "
                + safe(patient.getHousenumber()).toUpperCase(Locale.ROOT);
        String plz = String.valueOf(patient.getPlz());
        return String.format("NAD+%s+%s+%s+%s+%s+%s'", last, first, birth, street, plz, ORT_UNBEKANNT);
    }

    private static String buildEnfSegment(Abrechnung a, int menge, Leistungsparameter leistung) {
        String serviceDate = a.getCreatedAt().toLocalDate().format(BASIC_DATE);
        return String.format(Locale.GERMAN, "ENF+01+%s+%s+%s+%s+%s+%s'",
                leistung.leistungserbringergruppe(),
                leistung.positionsnummer(),
                formatQuantity(menge),
                leistung.einzelbetragFormatiert(),
                serviceDate,
                leistung.zuzahlungFormatiert());
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("'", " ").trim();
    }

    private static String zeroPad(int value, int length) {
        String formatted = String.valueOf(Math.abs(value));
        while (formatted.length() < length) {
            formatted = "0" + formatted;
        }
        return formatted;
    }

    private static String formatQuantity(int qty) {
        return String.format(Locale.GERMAN, "%1$.2f", (double) Math.max(0, qty));
    }

    private static String formatAmount(BigDecimal amt) {
        return String.format(Locale.GERMAN, "%1$.2f", amt);
    }

    public static Path writeDtaFile(String content, Path outDir, String filename) throws IOException {
        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }
        Path out = outDir.resolve(filename);
        Files.write(out, content.getBytes(StandardCharsets.UTF_8));
        return out;
    }
}
