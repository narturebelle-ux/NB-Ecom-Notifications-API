package com.narturebelle.mails.service;

import com.narturebelle.mails.email.EmailMessage;
import com.narturebelle.mails.email.EmailSender;
import com.narturebelle.mails.internal.CareClient;
import com.narturebelle.mails.internal.InternalDtos.EmailRequest;
import com.narturebelle.mails.internal.InternalDtos.EmailResult;
import com.narturebelle.mails.model.EmailLog;
import com.narturebelle.mails.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Sends one email at most once per event ID. Holds no transaction while calling Resend or the Care API, so a
 * slow provider can't hold a database connection. Logs IDs only, never addresses or message text.
 */
@Service
public class EmailDeliveryService {

    /** Another attempt newer than this is assumed to be still running; an older one is assumed to have died. */
    private static final Duration SENDING_TIMEOUT = Duration.ofMinutes(2);

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final EmailLogRepository emailLogRepository;
    private final EmailSender emailSender;
    private final CareClient careClient;

    public EmailDeliveryService(EmailLogRepository emailLogRepository, EmailSender emailSender, CareClient careClient) {
        this.emailLogRepository = emailLogRepository;
        this.emailSender = emailSender;
        this.careClient = careClient;
    }

    /** Throws when the email couldn't be sent, so the caller keeps its row and retries with the same event ID. */
    public EmailResult deliver(EmailRequest request) {
        Optional<EmailResult> repeat = claim(request);
        if (repeat.isPresent()) {
            return repeat.get();
        }
        EmailLog entry = emailLogRepository.findById(request.eventId()).orElseThrow();
        try {
            String to = request.userId() != null ? careClient.emailOf(request.userId()) : request.to();
            if (to == null || to.isBlank()) {
                return finish(entry, EmailLog.Status.NO_ADDRESS);
            }
            emailSender.send(new EmailMessage(to, request.replyTo(), request.subject(), request.text(), null));
            return finish(entry, emailSender.delivers() ? EmailLog.Status.SENT : EmailLog.Status.LOGGED_ONLY);
        } catch (RuntimeException e) {
            entry.finish(EmailLog.Status.FAILED);
            emailLogRepository.save(entry);
            log.warn("Email failed for event {}: {}", request.eventId(), e.getClass().getSimpleName());
            throw e;
        }
    }

    /** Empty means this call now owns the send. A present result means the event ID was already handled. */
    private Optional<EmailResult> claim(EmailRequest request) {
        Optional<EmailLog> existing = emailLogRepository.findById(request.eventId());
        if (existing.isEmpty()) {
            try {
                emailLogRepository.saveAndFlush(new EmailLog(request.eventId(), request.eventType()));
                return Optional.empty();
            } catch (DataIntegrityViolationException raced) {
                throw new EmailInProgressException();
            }
        }
        EmailLog entry = existing.get();
        switch (entry.getStatus()) {
            case SENT:
                return Optional.of(new EmailResult("DUPLICATE", true));
            case LOGGED_ONLY:
            case NO_ADDRESS:
                return Optional.of(new EmailResult("DUPLICATE", false));
            case SENDING:
                if (entry.getUpdatedAt().isAfter(LocalDateTime.now().minus(SENDING_TIMEOUT))) {
                    throw new EmailInProgressException();
                }
                break;
            default:
                break;
        }
        entry.retry();
        emailLogRepository.save(entry);
        return Optional.empty();
    }

    private EmailResult finish(EmailLog entry, EmailLog.Status status) {
        entry.finish(status);
        emailLogRepository.save(entry);
        return new EmailResult(status.name(), status == EmailLog.Status.SENT);
    }

    /** Another attempt for this event ID is running now. The caller should simply try again later. */
    public static class EmailInProgressException extends RuntimeException {
    }
}
