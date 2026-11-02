package com.narturebelle.mails.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** NO REAL EMAIL IS SENT. Active until RESEND_API_KEY is configured; logs the attachment count only, so
 * submissions are still saved and visible to admins in the meantime. */
@Component
@ConditionalOnExpression("'${email.resend.api-key:}'.isEmpty()")
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("[NO-OP EMAIL] Would have sent an email ({} attachment(s))", message.attachments().size());
    }

    @Override
    public boolean delivers() {
        return false;
    }
}
