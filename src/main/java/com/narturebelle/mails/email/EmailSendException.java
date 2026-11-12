package com.narturebelle.mails.email;

/**
 * The provider refused an email. The message holds only the HTTP status and the provider's error name
 * (e.g. 429 daily_quota_exceeded), never an address or message text, so it is safe to log.
 */
public class EmailSendException extends RuntimeException {

    private final int status;
    private final String errorName;

    public EmailSendException(int status, String errorName) {
        super("Email provider refused the send: " + status + (errorName == null ? "" : " " + errorName));
        this.status = status;
        this.errorName = errorName;
    }

    public int getStatus() { return status; }
    public String getErrorName() { return errorName; }
}
