package de.gkvtransmitter.dispatch;

public class BillingOfficeResponse {
    private final BillingOfficeResponseType type;
    private final String message;
    private final String rawContent;

    public BillingOfficeResponse(BillingOfficeResponseType type, String message, String rawContent) {
        this.type = type;
        this.message = message;
        this.rawContent = rawContent;
    }

    public BillingOfficeResponseType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getRawContent() {
        return rawContent;
    }
}