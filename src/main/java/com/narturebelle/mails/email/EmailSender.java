package com.narturebelle.mails.email;

/** Sends one email or throws. Resend when RESEND_API_KEY is set, otherwise a log-only fallback. */
public interface EmailSender {

    void send(EmailMessage message);

    /** False for the log-only fallback, so callers don't record a send that never happened. */
    boolean delivers();
}
