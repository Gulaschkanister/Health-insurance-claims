package de.gkvtransmitter.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Gesamtergebnis einer Pruefung.
 *
 * <p>Ersetzt die zuvor leere Klasse {@code ValidationResult}, die nie ueber
 * einen Rumpf hinausgekommen ist. Der Bericht sammelt alle Befunde, statt beim
 * ersten Fehler abzubrechen: wer eine Abrechnung korrigiert, soll alle
 * Beanstandungen auf einmal sehen und nicht nach jeder Korrektur erneut
 * anstossen muessen.</p>
 */
public final class ValidationReport {

    private final List<ValidationMessage> messages;

    private ValidationReport(List<ValidationMessage> messages) {
        this.messages = List.copyOf(messages);
    }

    public static ValidationReport of(List<ValidationMessage> messages) {
        return new ValidationReport(Objects.requireNonNull(messages, "messages must not be null"));
    }

    public static ValidationReport leer() {
        return new ValidationReport(List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ValidationMessage> getMessages() {
        return messages;
    }

    public List<ValidationMessage> mitGewicht(ValidationSeverity severity) {
        return messages.stream().filter(m -> m.severity() == severity).toList();
    }

    public List<ValidationMessage> getErrors() {
        return mitGewicht(ValidationSeverity.ERROR);
    }

    public List<ValidationMessage> getWarnings() {
        return mitGewicht(ValidationSeverity.WARNING);
    }

    /** Ob mindestens ein Befund den Versand aufhaelt. */
    public boolean hatFehler() {
        return messages.stream().anyMatch(m -> m.severity() == ValidationSeverity.ERROR);
    }

    /** Ob die Nachricht versendet werden darf. Warnungen stehen dem nicht entgegen. */
    public boolean istVersandfaehig() {
        return !hatFehler();
    }

    public boolean istLeer() {
        return messages.isEmpty();
    }

    /** Fuegt zwei Berichte zusammen, etwa ueber mehrere Nachrichten hinweg. */
    public ValidationReport plus(ValidationReport weiterer) {
        Objects.requireNonNull(weiterer, "weiterer must not be null");
        List<ValidationMessage> zusammen = new ArrayList<>(this.messages);
        zusammen.addAll(weiterer.messages);
        return new ValidationReport(zusammen);
    }

    /** Mehrzeilige Darstellung fuer Anzeige und Protokoll. */
    public String alsText() {
        if (messages.isEmpty()) {
            return "Keine Beanstandungen.";
        }
        return messages.stream().map(ValidationMessage::toString).collect(Collectors.joining(System.lineSeparator()));
    }

    /** Einzeilige Zusammenfassung, etwa fuer eine Statuszeile. */
    public String kurzfassung() {
        if (messages.isEmpty()) {
            return "Pruefung ohne Beanstandung";
        }
        return "%d Fehler, %d Warnungen".formatted(getErrors().size(), getWarnings().size());
    }

    @Override
    public String toString() {
        return "ValidationReport[" + kurzfassung() + "]";
    }

    /** Sammelt Befunde waehrend eines Pruefdurchlaufs. */
    public static final class Builder {

        private final List<ValidationMessage> gesammelt = new ArrayList<>();

        public Builder add(ValidationMessage message) {
            gesammelt.add(Objects.requireNonNull(message, "message must not be null"));
            return this;
        }

        /**
         * Eine Beanstandung, deren Text aus dem {@link Befundkatalog} kommt.
         *
         * <p><b>Der Text stand bis zum 18.09.2026 in der Regel selbst</b> -
         * und damit ein Satz eines Befunds im Quelltext, waehrend seine beiden
         * anderen (Fundstelle und Handlungsanweisung) schon in
         * {@code befunde/befunde.json} standen. Drei Saetze zu einer Sache an
         * zwei Orten: Wer einen umformulierte, fand die anderen nicht.</p>
         *
         * <p>Eigene Namen und keine Ueberladung von {@code error}: Eine
         * Ueberladung mit {@code Object...} haette die bisherigen Aufrufe
         * stillschweigend weiterlaufen lassen, und der dritte Wert waere
         * weiterhin als Text durchgereicht worden statt als Wert.</p>
         *
         * @param werte was in die Formatzeichenkette eingesetzt wird
         */
        public Builder fehler(String code, String ort, Object... werte) {
            return error(code, ort, Befundkatalog.standard().text(code, werte));
        }

        /** Eine Warnung, siehe {@link #fehler}. */
        public Builder warnung(String code, String ort, Object... werte) {
            return warning(code, ort, Befundkatalog.standard().text(code, werte));
        }

        /** Ein Hinweis, siehe {@link #fehler}. */
        public Builder hinweis(String code, String ort, Object... werte) {
            return info(code, ort, Befundkatalog.standard().text(code, werte));
        }

        public Builder error(String code, String ort, String text) {
            return add(ValidationMessage.error(code, ort, text));
        }

        public Builder warning(String code, String ort, String text) {
            return add(ValidationMessage.warning(code, ort, text));
        }

        public Builder info(String code, String ort, String text) {
            return add(ValidationMessage.info(code, ort, text));
        }

        public ValidationReport build() {
            return new ValidationReport(gesammelt);
        }
    }
}
