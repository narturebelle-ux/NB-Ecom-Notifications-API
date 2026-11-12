package com.narturebelle.mails.internal;

import com.narturebelle.mails.internal.InternalDtos.EmailRequest;
import com.narturebelle.mails.internal.InternalDtos.EmailResult;
import com.narturebelle.mails.internal.InternalDtos.ReminderBatch;
import com.narturebelle.mails.internal.InternalDtos.TransactionalRequest;
import com.narturebelle.mails.repository.NotificationPreferenceRepository;
import com.narturebelle.mails.repository.ReminderRepository;
import com.narturebelle.mails.service.EmailDeliveryService;
import com.narturebelle.mails.service.NotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Calls from the other NartureBelle services, guarded by InternalTokenFilter. */
@RestController
@RequestMapping("/internal/v1")
public class InternalController {

    private static final Logger log = LoggerFactory.getLogger(InternalController.class);

    private final EmailDeliveryService emailDeliveryService;
    private final NotificationService notificationService;
    private final ReminderRepository reminderRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public InternalController(EmailDeliveryService emailDeliveryService, NotificationService notificationService,
                              ReminderRepository reminderRepository,
                              NotificationPreferenceRepository notificationPreferenceRepository) {
        this.emailDeliveryService = emailDeliveryService;
        this.notificationService = notificationService;
        this.reminderRepository = reminderRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @PostMapping("/emails")
    public EmailResult sendEmail(@Valid @RequestBody EmailRequest request) {
        return emailDeliveryService.deliver(request);
    }

    /** Sent like any other email, at most once per event ID. Without one a repeat can't be stopped, so it stays the old no-op. */
    @PostMapping("/notifications/transactional")
    public ResponseEntity<EmailResult> sendTransactional(@Valid @RequestBody TransactionalRequest request) {
        if (request.eventId() == null || request.eventId().isBlank()) {
            notificationService.sendTransactionalEmail(request.userId(), request.title(), request.body());
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(emailDeliveryService.deliver(new EmailRequest(request.eventId(), "TRANSACTIONAL",
                request.userId(), null, null, request.title(), request.body())));
    }

    /** Care decides what's due and checks consent. Repeats are safe, so Care can resend a whole batch. */
    @PostMapping("/reminders")
    public ResponseEntity<Void> createReminders(@Valid @RequestBody ReminderBatch batch) {
        batch.reminders().forEach(notificationService::notifyDue);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/export")
    public Map<String, Object> export(@PathVariable Long userId) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("reminders", reminderRepository.findByUserIdOrderByCreatedAtDesc(userId));
        export.put("notificationPreferences", notificationPreferenceRepository.findByUserId(userId));
        return export;
    }

    @ExceptionHandler(EmailDeliveryService.EmailInProgressException.class)
    public ResponseEntity<Map<String, String>> handleInProgress() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "EMAIL_IN_PROGRESS"));
    }

    /** The send failed. The caller keeps its row and retries; the detail stays in this service's log. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleFailure(RuntimeException e) {
        log.warn("Internal call failed: {}", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("code", "SEND_FAILED"));
    }
}
