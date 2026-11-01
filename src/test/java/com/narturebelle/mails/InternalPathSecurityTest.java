package com.narturebelle.mails;

import com.narturebelle.mails.email.EmailSender;
import com.narturebelle.mails.internal.CareClient;
import com.narturebelle.mails.security.InternalTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

/** Real Tomcat and a raw socket on purpose: MockMvc and URI clients change the path before the filter would see it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalPathSecurityTest {

    private static final String EMAIL = "{\"eventId\":\"raw-1\",\"eventType\":\"T\",\"to\":\"x@example.com\",\"subject\":\"s\",\"text\":\"t\"}";

    @LocalServerPort private int port;

    @MockBean private CareClient careClient;
    @MockBean private EmailSender emailSender;

    private int status(String method, String path, String header, String body) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            String request = method + " " + path + " HTTP/1.1\r\nHost: localhost\r\n" + header
                    + "Content-Type: application/json\r\nContent-Length: " + body.length() + "\r\nConnection: close\r\n\r\n" + body;
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            String line = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
            return Integer.parseInt(line.split(" ")[1]);
        }
    }

    @Test
    void noSpellingOfThePath_canSendMailWithoutTheToken() throws IOException {
        List<String> paths = List.of(
                "/internal/v1/emails",
                "/%69nternal/v1/emails",
                "/%69%6e%74%65%72%6e%61%6c/v1/emails",
                "/internal%2fv1/emails",
                "//internal/v1/emails",
                "/./internal/v1/emails",
                "/x/../internal/v1/emails",
                "/internal;a=b/v1/emails",
                "/%2e/internal/v1/emails");
        for (String path : paths) {
            int status = status("POST", path, "", EMAIL);
            assertFalse(status >= 200 && status < 300, path + " returned " + status);
        }
        assertEquals(401, status("POST", "/%69nternal/v1/emails", "", EMAIL));
        verifyNoInteractions(emailSender);
    }

    @Test
    void theRealPathWithTheToken_stillWorks() throws IOException {
        assertEquals(200, status("GET", "/internal/v1/users/1/export", InternalTokenFilter.HEADER + ": test-internal-token\r\n", ""));
    }

    @Test
    void prodAndStage_haveNoDefaults() throws IOException {
        for (String file : List.of("application-prod.yml", "application-stage.yml")) {
            String yml = new ClassPathResource(file).getContentAsString(StandardCharsets.UTF_8);
            assertTrue(yml.contains("${INTERNAL_API_TOKEN}"), file);
            assertTrue(yml.contains("${CARE_API_URL}"), file);
        }
        assertTrue(new ClassPathResource("application-prod.yml").getContentAsString(StandardCharsets.UTF_8).contains("${RESEND_API_KEY}"));
    }
}
