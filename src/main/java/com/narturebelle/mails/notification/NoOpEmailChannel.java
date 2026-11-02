package com.narturebelle.mails.notification;

import com.narturebelle.mails.model.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * NO REAL EMAIL IS EVER SENT. No email provider has been chosen yet (decision log #9 is blank) and
 * no SMTP/provider credentials exist in any environment. This logs intent only, so a user who
 * enables the EMAIL channel sees nothing delivered -- that's the honest behavior until a real
 * provider is wired in behind this same interface.
 */
@Component
public class NoOpEmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailChannel.class);

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void send(Long userId, String title, String body) {
        log.info("[NO-OP EMAIL] Would have emailed user {}: {}", userId, title);
    }
}
