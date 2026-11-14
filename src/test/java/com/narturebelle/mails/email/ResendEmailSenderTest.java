package com.narturebelle.mails.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pins the exact request sent to Resend, without calling it. */
class ResendEmailSenderTest {

    private MockRestServiceServer resend;
    private ResendEmailSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        resend = MockRestServiceServer.bindTo(builder).build();
        sender = new ResendEmailSender(builder, "re_test_key", "NartureBelle <no-reply@mail.narturebelle.com>",
                "support-care@narturebelle.com");
    }

    @Test
    void sendsTextAndHtml_withTheKeyAndTheEventId_andReturnsResendsId() {
        resend.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test_key"))
                .andExpect(header("Idempotency-Key", "care-123"))
                .andExpect(jsonPath("$.from").value("NartureBelle <no-reply@mail.narturebelle.com>"))
                .andExpect(jsonPath("$.to[0]").value("member@example.com"))
                .andExpect(jsonPath("$.subject").value("Your application"))
                .andExpect(jsonPath("$.text").value("Hello <you>"))
                .andExpect(jsonPath("$.html").value(org.hamcrest.Matchers.containsString("Hello &lt;you&gt;")))
                .andExpect(jsonPath("$.reply_to").value("support-care@narturebelle.com"))
                .andRespond(withSuccess("{\"id\":\"re_49a3\"}", MediaType.APPLICATION_JSON));

        String id = sender.send(new EmailMessage("member@example.com", null, "Your application", "Hello <you>", null, "care-123", null));

        assertEquals("re_49a3", id);
        resend.verify();
    }

    @Test
    void aDesignedEmail_sendsItsOwnHtml() {
        resend.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.html").value("<p>Designed</p>"))
                .andRespond(withSuccess("{\"id\":\"re_2\"}", MediaType.APPLICATION_JSON));

        sender.send(new EmailMessage("member@example.com", null, "Welcome", "Plain", null, "care-7", "<p>Designed</p>"));
        resend.verify();
    }

    @Test
    void theCallersReplyTo_winsOverTheDefault() {
        resend.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.reply_to").value("applicant@example.com"))
                .andRespond(withSuccess("{\"id\":\"re_1\"}", MediaType.APPLICATION_JSON));

        sender.send(new EmailMessage("hello@narturebelle.com", "applicant@example.com", "New application", "Body", List.of(), "care-9", null));
        resend.verify();
    }

    @Test
    void aRefusal_becomesAnErrorThatNamesOnlyTheStatusAndTheErrorName() {
        resend.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":429,\"name\":\"daily_quota_exceeded\",\"message\":\"Quota reached for member@example.com\"}"));

        EmailSendException e = assertThrows(EmailSendException.class,
                () -> sender.send(new EmailMessage("member@example.com", null, "s", "t", null, "care-1", null)));

        assertEquals(429, e.getStatus());
        assertEquals("daily_quota_exceeded", e.getErrorName());
        assertFalse(e.getMessage().contains("@"), "an address must never reach the logs");
    }

    @Test
    void anUnreadableErrorBody_stillFails() {
        resend.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("<html>oops</html>"));

        EmailSendException e = assertThrows(EmailSendException.class,
                () -> sender.send(new EmailMessage("member@example.com", null, "s", "t", null, "care-2", null)));
        assertEquals(502, e.getStatus());
        assertNull(e.getErrorName());
    }
}
