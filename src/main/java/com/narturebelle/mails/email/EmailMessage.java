package com.narturebelle.mails.email;

import java.util.List;

/**
 * One outgoing email. replyTo may be null. idempotencyKey, when set, lets the provider recognise a retry of an
 * email it already accepted, so a timeout after a successful send can't send it twice. html is the designed
 * version (EmailTemplates); when null, the sender wraps the text in EmailLayout.
 */
public record EmailMessage(String to, String replyTo, String subject, String text, List<Attachment> attachments,
                           String idempotencyKey, String html) {

    public record Attachment(String fileName, byte[] content) {}

    public EmailMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
