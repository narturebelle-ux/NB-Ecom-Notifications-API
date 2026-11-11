package com.narturebelle.mails;

import com.narturebelle.mails.email.EmailMessage;
import com.narturebelle.mails.email.EmailSender;
import com.narturebelle.mails.internal.CareClient;
import com.narturebelle.mails.repository.EmailLogRepository;
import com.narturebelle.mails.security.InternalTokenFilter;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** EMAIL_REDIRECT_TO, for local runs: the email goes to the test inbox, marked as a test. */
@SpringBootTest(properties = "email.redirect-to=inbox@example.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailRedirectTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmailLogRepository emailLogRepository;

    @MockBean private CareClient careClient;
    @MockBean private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailLogRepository.deleteAll();
        when(emailSender.delivers()).thenReturn(true);
    }

    @Test
    void everyEmail_goesToTheTestInbox_markedAsATest() throws Exception {
        when(careClient.emailOf(7L)).thenReturn("dev@localhost");
        mockMvc.perform(post("/internal/v1/emails").header(InternalTokenFilter.HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"e-redirect\",\"eventType\":\"TEST\",\"userId\":7,\"subject\":\"Welcome\",\"text\":\"Body\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(sent.capture());
        assertEquals("inbox@example.com", sent.getValue().to());
        assertEquals("[Test] Welcome", sent.getValue().subject());
    }
}
