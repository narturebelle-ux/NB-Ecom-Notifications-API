package com.narturebelle.mails.email;

/** Sends one email or throws. Resend when RESEND_API_KEY is set, otherwise a log-only fallback. */
public interface EmailSender {

    /** The provider's ID for the sent email, for tracing it later; null when there is none. */
    String send(EmailMessage message);

    /** False for the log-only fallback, so callers don't record a send that never happened. */
    boolean delivers();
}
