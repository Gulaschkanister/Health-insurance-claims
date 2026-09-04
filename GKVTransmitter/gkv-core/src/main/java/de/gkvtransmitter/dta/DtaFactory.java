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
public class DtaFactory {

    private static final DateTimeFormatter HEADER_TIME = DateTimeFormatter.ofPattern("yyyyMMdd:HHmm");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    /** Ortsangabe, solange die Stammdaten keinen Ort fuehren. */
    private static final String ORT_UNBEKANNT = "ORT";

    /** Umsatzsteuersatz in Prozent, wie im Referenzbeispiel. */
    private static final String UMSATZSTEUERSATZ = "19";

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
     * @param a                  die Abrechnung
     * @param interchangeRefValue laufende Datenaustauschreferenz
     * @param senderIk           IK des Absenders
     * @param receiverIk         IK des Empfaengers
     * @param leistung           Betrag und Schluessel der Leistungszeile
     */
    public static String buildDtaFor(Abrechnung a, long interchangeRefValue, String senderIk, String receiverIk,
            Leistungsparameter leistung) {
        LocalDateTime now = a.getCreatedAt();
        LocalDate serviceDate = now.toLocalDate();
        String interchangeRef = String.format("%05d", interchangeRefValue);
        String applicationRef = buildApplicationRef(now, interchangeRefValue);

        // Menge und Summe muessen aus derselben Zahl entstehen. Zuvor rechnete
        // die Fallsumme mit mindestens einem Termin, die Menge im ENF aber mit
        // der tatsaechlichen Anzahl - bei null Terminen entstand dadurch eine
        // Rechnung ueber 15.000,00 mit einer Leistungsmenge von 0,00.
        int menge = Math.max(0, a.getAppointments());
        BigDecimal fallSum = leistung.einzelbetrag().multiply(BigDecimal.valueOf(menge));
        String sum = formatAmount(fallSum);

        List<String> lines = new ArrayList<>();
        lines.add(String.format("UNB+UNOC:3+%s+%s+%s+%s+H+%s+1'",
                senderIk,
                receiverIk,
                now.format(HEADER_TIME),
                interchangeRef,
                applicationRef));

        String slgaRef = "00001";
        List<String> slga = new ArrayList<>();
        slga.add(String.format("UNH+%s+SLGA:21:0:0'", slgaRef));
        slga.add(String.format("FKT+01++%s+%s+%s+%s'", senderIk, receiverIk, receiverIk, senderIk));
        slga.add(String.format("REC+00000000:0+%s+1'", serviceDate.minusDays(1).format(BASIC_DATE)));
        slga.add(String.format("UST+%s'", UMSATZSTEUERSATZ));
        slga.add(String.format("GES+00+%s+%s'", sum, sum));
        slga.add(String.format("GES+99+%s+%s'", sum, sum));
        slga.add(buildProviderNameSegment(a.getProvider()));
        slga.add(String.format("UNT+%06d+%s'", slga.size() + 1, slgaRef));
        lines.addAll(slga);

        lines.add("");

        String sllaRef = "00002";
        List<String> slla = new ArrayList<>();
        slla.add(String.format("UNH+%s+SLLA:21:0:0'", sllaRef));
        slla.add(String.format("FKT+01++%s+%s+%s+%s'", senderIk, receiverIk, receiverIk, senderIk));
        slla.add(String.format("REC+00000000:0+%s+1'", serviceDate.minusDays(1).format(BASIC_DATE)));
        slla.add(buildInvSegment(a.getPatient(), serviceDate));
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

    public static String buildApplicationRef(LocalDateTime now, long interchangeSeq) {
        return "HEB" + now.format(DateTimeFormatter.ofPattern("yyMMdd")) + String.format("%02d", interchangeSeq % 100);
    }

    private static String buildProviderNameSegment(ServiceProvider provider) {
        if (provider == null) {
            return "NAM+++DR'";
        }
        String last = safe(provider.getLastname()).toUpperCase(Locale.ROOT);
        String first = safe(provider.getFirstname()).toUpperCase(Locale.ROOT);
        return String.format("NAM+%s+%s+DR'", last, first);
    }

    private static String buildInvSegment(Patient patient, LocalDate serviceDate) {
        String versichertennummer = zeroPad(patient.getId(), 12);
        String belegnummer = "HEB" + serviceDate.format(DateTimeFormatter.ofPattern("yyMM"))
                + zeroPad(patient.getId(), 3);
        return String.format("INV+%s++1+%s'", versichertennummer, belegnummer);
    }

    private static String buildNadSegment(Patient patient) {
        String last = safe(patient.getLastname()).toUpperCase(Locale.ROOT);
        String first = safe(patient.getFirstname()).toUpperCase(Locale.ROOT);
        String birth = patient.getBirthDate() != null ? patient.getBirthDate().format(BASIC_DATE) : "19900101";
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
