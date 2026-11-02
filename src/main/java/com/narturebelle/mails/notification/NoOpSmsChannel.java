package com.narturebelle.mails.notification;

import com.narturebelle.mails.model.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** NO REAL SMS IS EVER SENT -- no SMS provider decided yet (decision log #9). Logs intent only. */
@Component
public class NoOpSmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(NoOpSmsChannel.class);

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.SMS;
    }

    @Override
    public void send(Long userId, String title, String body) {
        log.info("[NO-OP SMS] Would have texted user {}: {}", userId, title);
    }
}
