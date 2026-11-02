package com.narturebelle.mails.controller;

import com.narturebelle.mails.dto.NotificationPreferenceResponse;
import com.narturebelle.mails.dto.UpdateNotificationPreferenceRequest;
import com.narturebelle.mails.exception.UserProfileNotFoundException;
import com.narturebelle.mails.model.NotificationChannelType;
import com.narturebelle.mails.security.AuthorizationService;
import com.narturebelle.mails.security.FirebaseUserPrincipal;
import com.narturebelle.mails.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;
    private final AuthorizationService authorizationService;

    public NotificationPreferenceController(NotificationPreferenceService notificationPreferenceService,
                                              AuthorizationService authorizationService) {
        this.notificationPreferenceService = notificationPreferenceService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceResponse>> list() {
        Long userId = requireUserId(authorizationService.currentPrincipal());
        Map<NotificationChannelType, Boolean> effective = notificationPreferenceService.effectivePreferences(userId);
        List<NotificationPreferenceResponse> response = effective.entrySet().stream()
                .map(entry -> NotificationPreferenceResponse.of(entry.getKey(), entry.getValue()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{channel}")
    public ResponseEntity<NotificationPreferenceResponse> update(@PathVariable NotificationChannelType channel,
                                                                   @Valid @RequestBody UpdateNotificationPreferenceRequest req) {
        Long userId = requireUserId(authorizationService.currentPrincipal());
        notificationPreferenceService.updatePreference(userId, channel, req.getEnabled());
        return ResponseEntity.ok(NotificationPreferenceResponse.of(channel, req.getEnabled()));
    }

    private Long requireUserId(FirebaseUserPrincipal principal) {
        if (principal.userId() == null) {
            throw new UserProfileNotFoundException("Call POST /api/v1/auth/firebase first to create your profile.");
        }
        return principal.userId();
    }
}
