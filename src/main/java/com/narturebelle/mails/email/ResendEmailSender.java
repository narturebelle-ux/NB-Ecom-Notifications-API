package com.narturebelle.mails.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Real delivery through Resend's HTTP API (https://resend.com/docs/api-reference/emails/send-email).
 * The from-address domain must be verified in Resend (SPF/DKIM DNS records), or Resend rejects the send. */
@Component
@ConditionalOnExpression("!'${email.resend.api-key:}'.isEmpty()")
public class ResendEmailSender implements EmailSender {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;
    private final String from;
    private final String defaultReplyTo;

    @Autowired
    public ResendEmailSender(@Value("${email.resend.api-key}") String apiKey, @Value("${email.from}") String from,
                             @Value("${email.reply-to:}") String defaultReplyTo) {
        this(RestClient.builder().requestFactory(timeouts()), apiKey, from, defaultReplyTo);
    }

    /** For tests, which bind a mock server to the builder. */
    ResendEmailSender(RestClient.Builder builder, String apiKey, String from, String defaultReplyTo) {
        this.from = from;
        this.defaultReplyTo = defaultReplyTo == null || defaultReplyTo.isBlank() ? null : defaultReplyTo.strip();
        this.restClient = builder
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(5000);
        timeouts.setReadTimeout(10000);
        return timeouts;
    }

    @Override
    public String send(EmailMessage message) {
        Map<String, Object> body = new HashMap<>();
        body.put("from", from);
        body.put("to", List.of(message.to()));
        body.put("subject", message.subject());
        body.put("text", message.text());
        body.put("html", EmailLayout.html(message.subject(), message.text()));
        String replyTo = message.replyTo() != null ? message.replyTo() : defaultReplyTo;
        if (replyTo != null) {
            body.put("reply_to", replyTo);
        }
        if (!message.attachments().isEmpty()) {
            body.put("attachments", message.attachments().stream()
                    .map(a -> Map.of("filename", a.fileName(), "content", Base64.getEncoder().encodeToString(a.content())))
                    .toList());
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/emails")
                    .headers(headers -> {
                        // Resend keeps a key for 24 hours and won't send the same key twice in that time.
                        if (message.idempotencyKey() != null) {
                            headers.set("Idempotency-Key", message.idempotencyKey());
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            Object id = response == null ? null : response.get("id");
            return id == null ? null : id.toString();
        } catch (RestClientResponseException e) {
            throw new EmailSendException(e.getStatusCode().value(), errorName(e.getResponseBodyAsString()));
        }
    }

    /** Only Resend's error name, such as daily_quota_exceeded. Its message can quote an address, so it's dropped. */
    private static String errorName(String responseBody) {
        try {
            JsonNode name = JSON.readTree(responseBody).get("name");
            return name == null || !name.isTextual() ? null : name.asText();
        } catch (Exception unreadable) {
            return null;
        }
    }

    @Override
    public boolean delivers() {
        return true;
    }
}
