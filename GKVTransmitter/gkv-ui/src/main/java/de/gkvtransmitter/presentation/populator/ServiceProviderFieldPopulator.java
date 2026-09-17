package de.gkvtransmitter.presentation.populator;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import de.gkvtransmitter.entity.ServiceProvider;

/**
 * Traegt die Werte eines Dienstleisters ins Formular und wieder heraus.
 *
 * <p><b>Zwei Tabellen statt zweier Verzweigungen.</b> Hier standen zwei
 * {@code switch} ueber den Feldnamen. Solange es acht Felder waren, ging das;
 * mit dem Dienstleisterprofil wurden es vierzehn, und Checkstyle meldete
 * zyklomatische Komplexitaet 15 und 17 gegen eine Obergrenze von 10. Eine
 * Tabelle waechst dagegen in der Breite und nicht in der Tiefe: Ein neues Feld
 * ist eine Zeile hier und eine Zeile dort, und die beiden stehen untereinander,
 * sodass ein vergessenes Gegenstueck auffaellt.</p>
 *
 * <p>Fuer {@code PatientFieldPopulator} gilt dasselbe - dort ist es noch ein
 * {@code switch} und als D6 in der Aufgabenliste vermerkt. Dies hier ist das
 * ausgearbeitete Beispiel dafuer.</p>
 */
public class ServiceProviderFieldPopulator extends EntityFieldPopulator<ServiceProvider> {

    /** Woher der Wert eines Feldes kommt. */
    private static final Map<String, Function<ServiceProvider, String>> LESER = Map.ofEntries(
            Map.entry("firstname", ServiceProvider::getFirstname),
            Map.entry("lastname", ServiceProvider::getLastname),
            Map.entry("street", ServiceProvider::getStreet),
            Map.entry("country", ServiceProvider::getCountry),
            Map.entry("housenumber", ServiceProvider::getHousenumber),
            Map.entry("plz", person -> String.valueOf(person.getPlz())),
            Map.entry("ik", person -> String.valueOf(person.getIk())),
            Map.entry("kassenIk", person -> String.valueOf(person.getKassenIk())),
            Map.entry("abrechnungscode", ServiceProvider::getAbrechnungscode),
            Map.entry("steuernummer", ServiceProvider::getSteuernummer),
            Map.entry("umsatzsteuerbefreit",
                    person -> String.valueOf(person.istUmsatzsteuerbefreit())),
            Map.entry("ansprechpartner", ServiceProvider::getAnsprechpartner),
            Map.entry("telefon", ServiceProvider::getTelefon),
            Map.entry("email", ServiceProvider::getEmail),
            Map.entry("standardBlaupause", ServiceProvider::getStandardBlaupause),
            Map.entry("standardTermine", person -> String.valueOf(person.getStandardTermine())),
            Map.entry("standardGruppengroesse",
                    person -> String.valueOf(person.getStandardGruppengroesse())));

    /**
     * Wohin der Wert eines Feldes geht.
     *
     * <p>Uebernommen wird <b>auch ein geleerter</b> Wert. Siehe
     * {@link PatientFieldPopulator#setEntityFieldValue}: Ein vorangestellter
     * {@code isBlank}-Ausstieg uebersprang ein geleertes Feld still und umging
     * damit die Pruefung vor dem Speichern - eine falsch eingetragene Strasse
     * liess sich aendern, aber nicht entfernen.</p>
     */
    private static final Map<String, BiConsumer<ServiceProvider, String>> SCHREIBER = Map.ofEntries(
            Map.entry("firstname", ServiceProvider::setFirstname),
            Map.entry("lastname", ServiceProvider::setLastname),
            Map.entry("street", ServiceProvider::setStreet),
            Map.entry("country", ServiceProvider::setCountry),
            Map.entry("housenumber", ServiceProvider::setHousenumber),
            Map.entry("plz", (person, text) -> person.setPlz(zahl(text, person.getPlz()))),
            Map.entry("ik", (person, text) -> person.setIk(zahl(text, person.getIk()))),
            Map.entry("kassenIk", (person, text) -> person.setKassenIk(zahl(text, person.getKassenIk()))),
            Map.entry("birthDate", (person, text) -> person.setBirthDate(datum(text))),
            Map.entry("abrechnungscode", ServiceProvider::setAbrechnungscode),
            Map.entry("steuernummer", ServiceProvider::setSteuernummer),
            Map.entry("umsatzsteuerbefreit",
                    (person, text) -> person.setUmsatzsteuerbefreit(Boolean.parseBoolean(text))),
            Map.entry("ansprechpartner", ServiceProvider::setAnsprechpartner),
            Map.entry("telefon", ServiceProvider::setTelefon),
            Map.entry("email", ServiceProvider::setEmail),
            Map.entry("taetigSeit", (person, text) -> person.setTaetigSeit(datum(text))),
            Map.entry("taetigBis", (person, text) -> person.setTaetigBis(datum(text))),
            Map.entry("standardBlaupause", ServiceProvider::setStandardBlaupause),
            Map.entry("standardTermine",
                    (person, text) -> person.setStandardTermine(zahl(text, person.getStandardTermine()))),
            Map.entry("standardGruppengroesse", (person, text) ->
                    person.setStandardGruppengroesse(zahl(text, person.getStandardGruppengroesse()))));

    /** Der Dienstleister fuehrt Felder, die eine Teilnehmerin nicht hat. */
    @Override
    public String zusatzfelder() {
        return de.gkvtransmitter.presentation.Personenfelder.DIENSTLEISTER;
    }

    @Override
    protected String getFieldValue(String fieldName, ServiceProvider serviceProvider) {
        return LESER.getOrDefault(fieldName, person -> "").apply(serviceProvider);
    }

    @Override
    protected void setEntityFieldValue(String fieldName, ServiceProvider serviceProvider, String value) {
        SCHREIBER.getOrDefault(fieldName, (person, text) -> { })
                .accept(serviceProvider, value == null ? "" : value.trim());
    }

    @Override
    public String getDisplayName(ServiceProvider serviceProvider) {
        return getPlainName(serviceProvider) + " (ID: " + serviceProvider.getId() + ")";
    }

    @Override
    public String getPlainName(ServiceProvider serviceProvider) {
        return (serviceProvider.getFirstname() + " " + serviceProvider.getLastname()).strip();
    }

    @Override
    public Object getId(ServiceProvider serviceProvider) {
        return serviceProvider.getId();
    }

    /**
     * Auch der Taetigkeitszeitraum sind Datumsfelder.
     *
     * <p>Ohne diese Ergaenzung liefe der Wert am Kalender vorbei: Die Basis
     * behandelt nur Felder, die {@link #isDateField} nennt, als Datum, und
     * ein {@code DatePicker} ist weder ein Textfeld noch ein Zaehler - der
     * Wert kaeme schlicht nicht an.</p>
     */
    @Override
    protected boolean isDateField(String fieldName) {
        return super.isDateField(fieldName)
                || "taetigSeit".equals(fieldName) || "taetigBis".equals(fieldName);
    }

    @Override
    protected LocalDate getDateFieldValue(String fieldName, ServiceProvider serviceProvider) {
        return switch (fieldName) {
            case "taetigSeit" -> serviceProvider.getTaetigSeit();
            case "taetigBis" -> serviceProvider.getTaetigBis();
            default -> isDateField(fieldName) ? serviceProvider.getBirthDate() : null;
        };
    }
}
