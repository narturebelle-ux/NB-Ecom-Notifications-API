package com.narturebelle.mails.model;

import jakarta.persistence.*;

/** A missing row for a (userId, channel) pair means the default for that channel applies
 * (IN_APP defaults on, everything else defaults off) -- rows only exist once a user has
 * explicitly changed a channel away from its default. */
@Entity
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "channel"}))
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType channel;

    @Column(nullable = false)
    private boolean enabled;

    public NotificationPreference() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public NotificationChannelType getChannel() { return channel; }
    public void setChannel(NotificationChannelType channel) { this.channel = channel; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
