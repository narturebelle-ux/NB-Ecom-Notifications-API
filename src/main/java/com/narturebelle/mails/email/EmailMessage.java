package com.narturebelle.mails.email;

import java.util.List;

/** One outgoing email. replyTo may be null. */
public record EmailMessage(String to, String replyTo, String subject, String text, List<Attachment> attachments) {

    public record Attachment(String fileName, byte[] content) {}

    public EmailMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
