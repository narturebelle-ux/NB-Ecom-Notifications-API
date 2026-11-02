package com.narturebelle.mails.notification;

import com.narturebelle.mails.model.NotificationChannelType;

/** One implementation per NotificationChannelType. IN_APP doesn't need one -- the Reminder row
 * itself IS the in-app notification; NotificationService creates it directly. */
public interface NotificationChannel {
    NotificationChannelType type();
    void send(Long userId, String title, String body);
}
