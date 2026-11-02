package com.narturebelle.mails.controller;

import com.narturebelle.mails.dto.ReminderResponse;
import com.narturebelle.mails.exception.UserProfileNotFoundException;
import com.narturebelle.mails.model.Reminder;
import com.narturebelle.mails.security.AuthorizationService;
import com.narturebelle.mails.security.FirebaseUserPrincipal;
import com.narturebelle.mails.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final AuthorizationService authorizationService;

    public ReminderController(ReminderService reminderService, AuthorizationService authorizationService) {
        this.reminderService = reminderService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<ReminderResponse>> list() {
        Long userId = requireUserId(authorizationService.currentPrincipal());
        List<ReminderResponse> reminders = reminderService.listActive(userId).stream()
                .map(ReminderResponse::from).toList();
        return ResponseEntity.ok(reminders);
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ReminderResponse> dismiss(@PathVariable Long id) {
        Long userId = requireUserId(authorizationService.currentPrincipal());
        Reminder reminder = reminderService.find(id).orElse(null);
        if (reminder == null) {
            return ResponseEntity.notFound().build();
        }
        Reminder dismissed = reminderService.dismiss(reminder, userId);
        return ResponseEntity.ok(ReminderResponse.from(dismissed));
    }

    private Long requireUserId(FirebaseUserPrincipal principal) {
        if (principal.userId() == null) {
            throw new UserProfileNotFoundException("Call POST /api/v1/auth/firebase first to create your profile.");
        }
        return principal.userId();
    }
}
