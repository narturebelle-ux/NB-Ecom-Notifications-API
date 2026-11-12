package com.narturebelle.mails.service;

import com.narturebelle.mails.internal.InternalDtos.ReminderRequest;
import com.narturebelle.mails.model.NotificationChannelType;
import com.narturebelle.mails.model.Reminder;
import com.narturebelle.mails.model.ReminderSourceType;
import com.narturebelle.mails.model.ReminderStatus;
import com.narturebelle.mails.model.ReminderType;
import com.narturebelle.mails.notification.NotificationChannel;
import com.narturebelle.mails.repository.ReminderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The in-app channel is always the Reminder row itself -- no separate dispatch step. Other channels
 * (email/SMS/WhatsApp) are only invoked if the user's NotificationPreference enables them, and today
 * every one of those implementations is a NoOp that logs intent (see com.narturebelle.mails.notification).
 */
@Service
public class NotificationService {

    private final ReminderRepository reminderRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final Map<NotificationChannelType, NotificationChannel> channels;

    public NotificationService(ReminderRepository reminderRepository,
                                NotificationPreferenceService notificationPreferenceService,
                                List<NotificationChannel> channelBeans) {
        this.reminderRepository = reminderRepository;
        this.notificationPreferenceService = notificationPreferenceService;
        this.channels = new EnumMap<>(NotificationChannelType.class);
        for (NotificationChannel channel : channelBeans) {
            this.channels.put(channel.type(), channel);
        }
    }

    /** A reminder the Care API says is due. The wording lives here and stays generic: no health details. */
    public void notifyDue(ReminderRequest request) {
        String title = switch (request.type()) {
            case BOOKING_UPCOMING -> "Upcoming care session";
            case JOURNEY_NEXT_ACTION -> "New recommended next steps";
        };
        String body = switch (request.type()) {
            case BOOKING_UPCOMING -> "You have an upcoming care session scheduled in the next 24 hours.";
            case JOURNEY_NEXT_ACTION -> "Your care journey has recommended next steps waiting -- check My Journey for details.";
        };
        notify(request.userId(), request.type(), title, body, request.sourceType(), request.sourceId(), request.dueAt(), request.dedupeKey());
    }

    public void notify(Long userId, ReminderType type, String title, String body,
                        ReminderSourceType sourceType, Long sourceId, LocalDateTime dueAt) {
        notify(userId, type, title, body, sourceType, sourceId, dueAt, null);
    }

    /** Idempotent: a repeated call for the same (userId, sourceType, sourceId, dedupeKey) is a no-op whether the
     * earlier reminder is still ACTIVE or was DISMISSED, so a scheduled job can call this on every run without
     * creating duplicates or bringing a dismissed reminder back. A new dedupeKey can earn a new reminder, once the
     * source has no ACTIVE one left (the partial unique index in the Care API's V12). */
    public void notify(Long userId, ReminderType type, String title, String body,
                        ReminderSourceType sourceType, Long sourceId, LocalDateTime dueAt, String dedupeKey) {
        if (reminderRepository.existsByUserIdAndSourceTypeAndSourceIdAndDedupeKey(userId, sourceType, sourceId, dedupeKey)) {
            return;
        }
        if (dedupeKey != null) {
            // A row from before dedupe keys existed counts as this one, or a dismissed reminder would come back at cut-over.
            Optional<Reminder> older = reminderRepository
                    .findFirstByUserIdAndSourceTypeAndSourceIdAndDedupeKeyIsNull(userId, sourceType, sourceId);
            if (older.isPresent()) {
                older.get().setDedupeKey(dedupeKey);
                reminderRepository.save(older.get());
                return;
            }
        }

        Reminder reminder = new Reminder();
        reminder.setUserId(userId);
        reminder.setType(type);
        reminder.setTitle(title);
        reminder.setBody(body);
        reminder.setDueAt(dueAt);
        reminder.setSourceType(sourceType);
        reminder.setSourceId(sourceId);
        reminder.setDedupeKey(dedupeKey);
        reminder.setStatus(ReminderStatus.ACTIVE);
        try {
            reminderRepository.save(reminder);
        } catch (DataIntegrityViolationException raced) {
            return; // A concurrent run created it first; the partial unique index caught the repeat.
        }

        for (NotificationChannelType channelType : List.of(
                NotificationChannelType.EMAIL, NotificationChannelType.SMS, NotificationChannelType.WHATSAPP)) {
            if (notificationPreferenceService.isEnabled(userId, channelType)) {
                NotificationChannel channel = channels.get(channelType);
                if (channel != null) {
                    channel.send(userId, title, body);
                }
            }
        }
    }

    /** Transactional emails (e.g. "here's your signed document") are not reminders -- they don't
     * create a dismissible Reminder row, and they bypass the EMAIL channel preference, since the
     * user's own action (signing) is the real consent here, not a general notification opt-in.
     * Only reached by a caller that sends no event ID, and still a NoOp: InternalController sends the real
     * email through EmailDeliveryService whenever an event ID is present. */
    public void sendTransactionalEmail(Long userId, String title, String body) {
        NotificationChannel email = channels.get(NotificationChannelType.EMAIL);
        if (email != null) {
            email.send(userId, title, body);
        }
    }
}
