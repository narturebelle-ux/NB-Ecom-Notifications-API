package com.narturebelle.mails.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Real delivery through Resend's HTTP API (https://resend.com/docs/api-reference/emails/send-email).
 * The from-address domain must be verified in Resend (SPF/DKIM DNS records), or Resend rejects the send. */
@Component
@ConditionalOnExpression("!'${email.resend.api-key:}'.isEmpty()")
public class ResendEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String from;

    public ResendEmailSender(@Value("${email.resend.api-key}") String apiKey, @Value("${email.from}") String from) {
        this.from = from;
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(5000);
        timeouts.setReadTimeout(10000);
        this.restClient = RestClient.builder()
                .requestFactory(timeouts)
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void send(EmailMessage message) {
        Map<String, Object> body = new HashMap<>();
        body.put("from", from);
        body.put("to", List.of(message.to()));
        body.put("subject", message.subject());
        body.put("text", message.text());
        if (message.replyTo() != null) {
            body.put("reply_to", message.replyTo());
        }
        if (!message.attachments().isEmpty()) {
            body.put("attachments", message.attachments().stream()
                    .map(a -> Map.of("filename", a.fileName(), "content", Base64.getEncoder().encodeToString(a.content())))
                    .toList());
        }
        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public boolean delivers() {
        return true;
    }
}
