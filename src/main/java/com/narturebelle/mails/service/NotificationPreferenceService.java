package com.narturebelle.mails.service;

import com.narturebelle.mails.model.NotificationChannelType;
import com.narturebelle.mails.model.NotificationPreference;
import com.narturebelle.mails.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationPreferenceService {

    /** A channel with no stored row falls back to this default. */
    private static final Map<NotificationChannelType, Boolean> DEFAULTS = Map.of(
            NotificationChannelType.IN_APP, true,
            NotificationChannelType.EMAIL, false,
            NotificationChannelType.SMS, false,
            NotificationChannelType.WHATSAPP, false
    );

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    public Map<NotificationChannelType, Boolean> effectivePreferences(Long userId) {
        List<NotificationPreference> overrides = notificationPreferenceRepository.findByUserId(userId);
        Map<NotificationChannelType, Boolean> effective = new java.util.EnumMap<>(DEFAULTS);
        for (NotificationPreference override : overrides) {
            effective.put(override.getChannel(), override.isEnabled());
        }
        return effective;
    }

    public boolean isEnabled(Long userId, NotificationChannelType channel) {
        return notificationPreferenceRepository.findByUserIdAndChannel(userId, channel)
                .map(NotificationPreference::isEnabled)
                .orElseGet(() -> DEFAULTS.get(channel));
    }

    public NotificationPreference updatePreference(Long userId, NotificationChannelType channel, boolean enabled) {
        NotificationPreference preference = notificationPreferenceRepository.findByUserIdAndChannel(userId, channel)
                .orElseGet(() -> {
                    NotificationPreference fresh = new NotificationPreference();
                    fresh.setUserId(userId);
                    fresh.setChannel(channel);
                    return fresh;
                });
        preference.setEnabled(enabled);
        return notificationPreferenceRepository.save(preference);
    }
}
