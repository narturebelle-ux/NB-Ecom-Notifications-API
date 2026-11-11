package com.narturebelle.mails.email;

import java.util.List;

/**
 * One outgoing email. replyTo may be null. idempotencyKey, when set, lets the provider recognise a retry of an
 * email it already accepted, so a timeout after a successful send can't send it twice.
 */
public record EmailMessage(String to, String replyTo, String subject, String text, List<Attachment> attachments,
                           String idempotencyKey) {

    public record Attachment(String fileName, byte[] content) {}

    public EmailMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
