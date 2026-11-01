package com.narturebelle.mails.dto;

import com.narturebelle.mails.model.Reminder;
import com.narturebelle.mails.model.ReminderSourceType;
import com.narturebelle.mails.model.ReminderStatus;
import com.narturebelle.mails.model.ReminderType;

import java.time.LocalDateTime;

public class ReminderResponse {

    private Long id;
    private ReminderType type;
    private String title;
    private String body;
    private LocalDateTime dueAt;
    private ReminderSourceType sourceType;
    private Long sourceId;
    private ReminderStatus status;
    private LocalDateTime createdAt;

    public static ReminderResponse from(Reminder reminder) {
        ReminderResponse dto = new ReminderResponse();
        dto.id = reminder.getId();
        dto.type = reminder.getType();
        dto.title = reminder.getTitle();
        dto.body = reminder.getBody();
        dto.dueAt = reminder.getDueAt();
        dto.sourceType = reminder.getSourceType();
        dto.sourceId = reminder.getSourceId();
        dto.status = reminder.getStatus();
        dto.createdAt = reminder.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public ReminderType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public LocalDateTime getDueAt() { return dueAt; }
    public ReminderSourceType getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public ReminderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
