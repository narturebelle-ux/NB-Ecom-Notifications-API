package com.narturebelle.mails.notification;

import com.narturebelle.mails.model.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** NO REAL WHATSAPP MESSAGE IS EVER SENT -- no WhatsApp provider decided yet (decision log #9). Logs intent only. */
@Component
public class NoOpWhatsAppChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(NoOpWhatsAppChannel.class);

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.WHATSAPP;
    }

    @Override
    public void send(Long userId, String title, String body) {
        log.info("[NO-OP WHATSAPP] Would have messaged user {}: {}", userId, title);
    }
}
