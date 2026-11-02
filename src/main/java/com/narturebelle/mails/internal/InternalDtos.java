package com.narturebelle.mails.internal;

import com.narturebelle.mails.model.ReminderSourceType;
import com.narturebelle.mails.model.ReminderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class InternalDtos {

    private InternalDtos() {}

    /** To a user ID, or to a raw address for careers and public forms; never both. */
    public record EmailRequest(
            @NotBlank @Size(max = 120) String eventId,
            @NotBlank @Size(max = 80) String eventType,
            Long userId,
            @Size(max = 320) String to,
            @Size(max = 320) String replyTo,
            @NotBlank String subject,
            @NotBlank String text) {

        @AssertTrue(message = "exactly one of userId and to is required")
        public boolean isOneRecipient() {
            return (userId == null) != (to == null || to.isBlank());
        }
    }

    /** delivered is true only when the email really went out, so callers don't record a send that never happened. */
    public record EmailResult(String status, boolean delivered) {}

    public record TransactionalRequest(@NotNull Long userId, @NotBlank String title, @NotBlank String body) {}

    public record ReminderRequest(@NotNull Long userId, @NotNull ReminderType type, @NotNull ReminderSourceType sourceType,
                                  @NotNull Long sourceId, LocalDateTime dueAt, @Size(max = 64) String dedupeKey) {}

    public record ReminderBatch(@NotNull @Size(max = 500) List<@Valid ReminderRequest> reminders) {}
}
