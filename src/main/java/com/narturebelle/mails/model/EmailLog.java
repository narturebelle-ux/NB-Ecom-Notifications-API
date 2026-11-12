package com.narturebelle.mails.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** One row per event ID: the guard against sending twice. Never holds an address or message text. */
@Entity
@Table(name = "email_log", schema = "mails")
public class EmailLog {

    public enum Status { SENDING, SENT, LOGGED_ONLY, NO_ADDRESS, FAILED }

    @Id
    @Column(length = 120)
    private String eventId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime sentAt;

    /** The provider's own ID for the email, to trace a complaint or a bounce. Not personal data. */
    @Column(length = 100)
    private String providerMessageId;

    public EmailLog() {}

    public EmailLog(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.status = Status.SENDING;
        this.attempts = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }

    public void retry() {
        this.status = Status.SENDING;
        this.attempts++;
        this.updatedAt = LocalDateTime.now();
    }

    public void finish(Status status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == Status.SENT) {
            this.sentAt = this.updatedAt;
        }
    }
}
