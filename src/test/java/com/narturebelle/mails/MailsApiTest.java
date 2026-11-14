package com.narturebelle.mails;

import com.narturebelle.mails.email.EmailMessage;
import com.narturebelle.mails.email.EmailSendException;
import com.narturebelle.mails.email.EmailSender;
import com.narturebelle.mails.internal.CareClient;
import com.narturebelle.mails.internal.CareUnavailableException;
import com.narturebelle.mails.model.EmailLog;
import com.narturebelle.mails.model.NotificationChannelType;
import com.narturebelle.mails.model.ReminderSourceType;
import com.narturebelle.mails.model.ReminderStatus;
import com.narturebelle.mails.model.ReminderType;
import com.narturebelle.mails.repository.EmailLogRepository;
import com.narturebelle.mails.repository.NotificationPreferenceRepository;
import com.narturebelle.mails.repository.ReminderRepository;
import com.narturebelle.mails.security.FirebaseTokenVerifier;
import com.narturebelle.mails.security.InternalTokenFilter;
import com.narturebelle.mails.security.VerifiedFirebaseToken;
import com.narturebelle.mails.service.NotificationPreferenceService;
import com.narturebelle.mails.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailsApiTest {

    private static final String TOKEN = "notif-token-a";
    private static final String TOKEN_OTHER = "notif-token-other";
    private static final String NEW_USER_TOKEN = "notif-token-new";
    private static final long USER = 101L;
    private static final long OTHER_USER = 102L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired private EmailLogRepository emailLogRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationPreferenceService notificationPreferenceService;

    @MockBean private FirebaseTokenVerifier firebaseTokenVerifier;
    @MockBean private CareClient careClient;
    @MockBean private EmailSender emailSender;

    @BeforeEach
    void setUp() throws Exception {
        reminderRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        emailLogRepository.deleteAll();

        when(emailSender.delivers()).thenReturn(true);
        when(firebaseTokenVerifier.verify(TOKEN))
                .thenReturn(new VerifiedFirebaseToken("uid-a", "a@example.com", true, null, null));
        when(firebaseTokenVerifier.verify(TOKEN_OTHER))
                .thenReturn(new VerifiedFirebaseToken("uid-other", "other@example.com", true, null, null));
        when(firebaseTokenVerifier.verify(NEW_USER_TOKEN))
                .thenReturn(new VerifiedFirebaseToken("uid-new", "new@example.com", true, null, null));
        when(careClient.identity("uid-a")).thenReturn(new CareClient.Identity(USER, Set.of("MOTHER")));
        when(careClient.identity("uid-other")).thenReturn(new CareClient.Identity(OTHER_USER, Set.of("MOTHER")));
    }

    private MockHttpServletRequestBuilder internal(MockHttpServletRequestBuilder request) {
        return request.header(InternalTokenFilter.HEADER, "test-internal-token");
    }

    private MockHttpServletRequestBuilder email(String eventId, String recipientJson) {
        return internal(post("/internal/v1/emails").contentType(MediaType.APPLICATION_JSON).content(
                "{\"eventId\":\"" + eventId + "\",\"eventType\":\"TEST\"," + recipientJson
                        + ",\"subject\":\"Hello\",\"text\":\"Body\"}"));
    }

    // ---- moved from the Care API's NotificationsTest ----

    @Test
    void notificationPreferences_defaultInAppOnEverythingElseOff_andCanBeOverridden() throws Exception {
        mockMvc.perform(get("/api/v1/me/notification-preferences").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.channel=='IN_APP')].enabled").value(true))
                .andExpect(jsonPath("$[?(@.channel=='EMAIL')].enabled").value(false));

        mockMvc.perform(put("/api/v1/me/notification-preferences/EMAIL")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        assertTrue(notificationPreferenceService.isEnabled(USER, NotificationChannelType.EMAIL));
    }

    @Test
    void notifyService_isIdempotent_forSameSource() {
        notificationService.notify(USER, ReminderType.BOOKING_UPCOMING, "Upcoming care session",
                "You have an upcoming care session.", ReminderSourceType.BOOKING, 999L, LocalDateTime.now());
        notificationService.notify(USER, ReminderType.BOOKING_UPCOMING, "Upcoming care session",
                "You have an upcoming care session.", ReminderSourceType.BOOKING, 999L, LocalDateTime.now());

        assertEquals(1, reminderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER, ReminderStatus.ACTIVE).size());
    }

    @Test
    void reminderList_andDismiss_ownershipEnforced() throws Exception {
        notificationService.notify(USER, ReminderType.JOURNEY_NEXT_ACTION, "New recommended next steps",
                "Check your journey.", ReminderSourceType.JOURNEY, 1L, null);

        String body = mockMvc.perform(get("/api/v1/me/reminders").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        Long reminderId = Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(post("/api/v1/me/reminders/" + reminderId + "/dismiss")
                        .header("Authorization", "Bearer " + TOKEN_OTHER))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/me/reminders/" + reminderId + "/dismiss")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));

        mockMvc.perform(get("/api/v1/me/reminders").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    // ---- BUG-16 ----

    @Test
    void dismissedReminder_doesNotComeBack_whenTheJobRepeatsIt() throws Exception {
        String batch = "{\"reminders\":[{\"userId\":" + USER + ",\"type\":\"BOOKING_UPCOMING\",\"sourceType\":\"BOOKING\",\"sourceId\":5}]}";
        mockMvc.perform(internal(post("/internal/v1/reminders")).contentType(MediaType.APPLICATION_JSON).content(batch))
                .andExpect(status().isNoContent());
        Long id = reminderRepository.findByUserIdOrderByCreatedAtDesc(USER).get(0).getId();
        mockMvc.perform(post("/api/v1/me/reminders/" + id + "/dismiss").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(internal(post("/internal/v1/reminders")).contentType(MediaType.APPLICATION_JSON).content(batch))
                .andExpect(status().isNoContent());

        assertTrue(reminderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER, ReminderStatus.ACTIVE).isEmpty());
        assertEquals(1, reminderRepository.findByUserIdOrderByCreatedAtDesc(USER).size());
    }

    @Test
    void journeyReminder_dismissalHoldsForTheStage_andANewStageEarnsANewOne() throws Exception {
        mockMvc.perform(internal(post("/internal/v1/reminders")).contentType(MediaType.APPLICATION_JSON).content(journeyBatch("stage-1")))
                .andExpect(status().isNoContent());
        Long id = reminderRepository.findByUserIdOrderByCreatedAtDesc(USER).get(0).getId();
        mockMvc.perform(post("/api/v1/me/reminders/" + id + "/dismiss").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(internal(post("/internal/v1/reminders")).contentType(MediaType.APPLICATION_JSON).content(journeyBatch("stage-1")))
                .andExpect(status().isNoContent());
        assertTrue(reminderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER, ReminderStatus.ACTIVE).isEmpty());

        mockMvc.perform(internal(post("/internal/v1/reminders")).contentType(MediaType.APPLICATION_JSON).content(journeyBatch("stage-2")))
                .andExpect(status().isNoContent());
        assertEquals(1, reminderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER, ReminderStatus.ACTIVE).size());
    }

    @Test
    void aDismissedRowFromBeforeDedupeKeys_doesNotComeBackAtCutOver() throws Exception {
        notificationService.notify(USER, ReminderType.JOURNEY_NEXT_ACTION, "t", "b", ReminderSourceType.JOURNEY, 9L, null);
        Long id = reminderRepository.findByUserIdOrderByCreatedAtDesc(USER).get(0).getId();
        mockMvc.perform(post("/api/v1/me/reminders/" + id + "/dismiss").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(internal(post("/internal/v1/reminders")).contentType(MediaType.APPLICATION_JSON).content(journeyBatch("stage-1")))
                .andExpect(status().isNoContent());

        assertEquals(1, reminderRepository.findByUserIdOrderByCreatedAtDesc(USER).size());
        assertEquals("stage-1", reminderRepository.findById(id).orElseThrow().getDedupeKey());
        assertEquals(ReminderStatus.DISMISSED, reminderRepository.findById(id).orElseThrow().getStatus());

        mockMvc.perform(internal(post("/internal/v1/reminders")).contentType(MediaType.APPLICATION_JSON).content(journeyBatch("stage-2")))
                .andExpect(status().isNoContent());
        assertEquals(1, reminderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER, ReminderStatus.ACTIVE).size());
    }

    private String journeyBatch(String dedupeKey) {
        return "{\"reminders\":[{\"userId\":" + USER + ",\"type\":\"JOURNEY_NEXT_ACTION\",\"sourceType\":\"JOURNEY\",\"sourceId\":9,\"dedupeKey\":\"" + dedupeKey + "\"}]}";
    }

    // ---- sign-in through the Care API ----

    @Test
    void careOutage_isA503_notA401() throws Exception {
        when(careClient.identity("uid-a")).thenThrow(new CareUnavailableException("down"));
        mockMvc.perform(get("/api/v1/me/reminders").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void validTokenWithoutACareProfile_getsTheSame404AsBefore() throws Exception {
        mockMvc.perform(get("/api/v1/me/reminders").header("Authorization", "Bearer " + NEW_USER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void theWebsiteOrigin_isAllowed_onAllFourPaths() throws Exception {
        String[][] calls = {{"GET", "/api/v1/me/reminders"}, {"POST", "/api/v1/me/reminders/1/dismiss"},
                {"GET", "/api/v1/me/notification-preferences"}, {"PUT", "/api/v1/me/notification-preferences/EMAIL"}};
        for (String[] call : calls) {
            mockMvc.perform(options(call[1])
                            .header("Origin", "http://localhost:4200")
                            .header("Access-Control-Request-Method", call[0])
                            .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                            .string("Access-Control-Allow-Origin", "http://localhost:4200"));
        }
    }

    @Test
    void noToken_is401() throws Exception {
        mockMvc.perform(get("/api/v1/me/reminders")).andExpect(status().isUnauthorized());
    }

    // ---- internal send ----

    @Test
    void internalCalls_needTheToken() throws Exception {
        mockMvc.perform(post("/internal/v1/emails").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/internal/v1/users/1/export").header(InternalTokenFilter.HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(emailSender);
    }

    @Test
    void aSignedInUsersToken_doesNotOpenInternalCalls() throws Exception {
        mockMvc.perform(get("/internal/v1/users/" + USER + "/export").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anEmptyConfiguredToken_refusesEverything() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("");
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/internal/v1/users/1/export");
        request.setServletPath("/internal/v1/users/1/export");
        request.setServletPath(request.getRequestURI()); // What Tomcat sets; a bare mock request has none.
        request.addHeader(InternalTokenFilter.HEADER, "");
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain chain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void repeatedEventId_sendsOnce() throws Exception {
        mockMvc.perform(email("e-repeat", "\"to\":\"x@example.com\"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true));
        mockMvc.perform(email("e-repeat", "\"to\":\"x@example.com\"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.delivered").value(true));
        verify(emailSender, times(1)).send(any());
    }

    @Test
    void providerDown_failsThenARetrySends() throws Exception {
        doThrow(new RuntimeException("provider down")).doReturn("re_1").when(emailSender).send(any());
        mockMvc.perform(email("e-retry", "\"to\":\"x@example.com\"")).andExpect(status().isBadGateway());
        assertEquals(EmailLog.Status.FAILED, emailLogRepository.findById("e-retry").orElseThrow().getStatus());

        mockMvc.perform(email("e-retry", "\"to\":\"x@example.com\"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true));
        assertEquals(2, emailLogRepository.findById("e-retry").orElseThrow().getAttempts());
    }

    @Test
    void userIdSend_looksUpTheAddress_atSendTime() throws Exception {
        when(careClient.contactOf(USER)).thenReturn(new CareClient.Contact("looked-up@example.com", null));
        mockMvc.perform(email("e-user", "\"userId\":" + USER)).andExpect(status().isOk());

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(sent.capture());
        assertEquals("looked-up@example.com", sent.getValue().to());
        assertNull(sent.getValue().html(), "an event type with no template keeps the caller's text, in the plain layout");
    }

    @Test
    void aWelcomeEventType_sendsItsDesignedEmail_greetingTheUserByName() throws Exception {
        when(careClient.contactOf(USER)).thenReturn(new CareClient.Contact("member@example.com", "Asha"));
        mockMvc.perform(internal(post("/internal/v1/emails")).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"eventId\":\"e-welcome\",\"eventType\":\"ONBOARDING_COMPLETE\",\"userId\":" + USER
                                + ",\"subject\":\"Plain subject\",\"text\":\"Plain text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true));

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(sent.capture());
        assertEquals("You're all set, welcome to NartureBelle", sent.getValue().subject());
        assertTrue(sent.getValue().html().contains("You&#39;re all set, Asha"));
        assertFalse(sent.getValue().text().contains("Plain text"));
    }

    @Test
    void userWithNoAddress_isNotAnError_andSendsNothing() throws Exception {
        mockMvc.perform(email("e-none", "\"userId\":" + USER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_ADDRESS"))
                .andExpect(jsonPath("$.delivered").value(false));
        verify(emailSender, never()).send(any());
    }

    @Test
    void needsExactlyOneRecipient() throws Exception {
        mockMvc.perform(email("e-both", "\"userId\":1,\"to\":\"x@example.com\"")).andExpect(status().isBadRequest());
        mockMvc.perform(email("e-neither", "\"replyTo\":\"x@example.com\"")).andExpect(status().isBadRequest());
    }

    @Test
    void logOnlySender_isNotReportedAsDelivered() throws Exception {
        when(emailSender.delivers()).thenReturn(false);
        mockMvc.perform(email("e-log", "\"to\":\"x@example.com\"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(false));
    }

    @Test
    void theEventId_isTheProvidersIdempotencyKey_andItsMessageIdIsKept() throws Exception {
        when(emailSender.send(any())).thenReturn("re_abc");
        mockMvc.perform(email("e-key", "\"to\":\"x@example.com\"")).andExpect(status().isOk());

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(sent.capture());
        assertEquals("e-key", sent.getValue().idempotencyKey());
        assertEquals("re_abc", emailLogRepository.findById("e-key").orElseThrow().getProviderMessageId());
    }

    @Test
    void aRefusedSend_isA502_soTheCallerRetries() throws Exception {
        when(emailSender.send(any())).thenThrow(new EmailSendException(429, "daily_quota_exceeded"));
        mockMvc.perform(email("e-quota", "\"to\":\"x@example.com\"")).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SEND_FAILED"));
        assertEquals(EmailLog.Status.FAILED, emailLogRepository.findById("e-quota").orElseThrow().getStatus());
    }

    @Test
    void transactionalWithAnEventId_reallySends_once() throws Exception {
        when(careClient.contactOf(USER)).thenReturn(new CareClient.Contact("member@example.com", null));
        String body = "{\"userId\":" + USER + ",\"title\":\"Your signed onboarding document\",\"body\":\"Signed.\",\"eventId\":\"care-t1\"}";

        mockMvc.perform(internal(post("/internal/v1/notifications/transactional")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true));
        mockMvc.perform(internal(post("/internal/v1/notifications/transactional")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, times(1)).send(sent.capture());
        assertEquals("member@example.com", sent.getValue().to());
        assertEquals("care-t1", sent.getValue().idempotencyKey());
    }

    @Test
    void transactionalWithoutAnEventId_staysTheOldNoOp() throws Exception {
        mockMvc.perform(internal(post("/internal/v1/notifications/transactional")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + USER + ",\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isNoContent());
        verify(emailSender, never()).send(any());
    }

    @Test
    void export_returnsBothLists() throws Exception {
        notificationService.notify(USER, ReminderType.BOOKING_UPCOMING, "t", "b", ReminderSourceType.BOOKING, 3L, null);
        mockMvc.perform(internal(get("/internal/v1/users/" + USER + "/export")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminders", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.notificationPreferences", org.hamcrest.Matchers.hasSize(0)));
    }
}
