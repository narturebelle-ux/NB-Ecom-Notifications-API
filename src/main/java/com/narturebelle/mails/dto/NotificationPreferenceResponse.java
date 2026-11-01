package com.narturebelle.mails.dto;

import com.narturebelle.mails.model.NotificationChannelType;

public class NotificationPreferenceResponse {

    private NotificationChannelType channel;
    private boolean enabled;

    public static NotificationPreferenceResponse of(NotificationChannelType channel, boolean enabled) {
        NotificationPreferenceResponse dto = new NotificationPreferenceResponse();
        dto.channel = channel;
        dto.enabled = enabled;
        return dto;
    }

    public NotificationChannelType getChannel() { return channel; }
    public boolean isEnabled() { return enabled; }
}
