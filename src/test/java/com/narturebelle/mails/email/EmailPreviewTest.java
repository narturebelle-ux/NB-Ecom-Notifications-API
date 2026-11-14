package com.narturebelle.mails.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes every designed email as an .html file to look at in a browser. Runs only when asked:
 * set EMAIL_PREVIEW_DIR, then `gradlew test --tests "*EmailPreviewTest"`.
 */
@EnabledIfEnvironmentVariable(named = "EMAIL_PREVIEW_DIR", matches = ".+")
class EmailPreviewTest {

    @Test
    void writePreviews() throws Exception {
        Path dir = Path.of(System.getenv("EMAIL_PREVIEW_DIR"));
        Files.createDirectories(dir);
        EmailTemplates templates = new EmailTemplates("https://www.narturebelle.com");
        for (String type : List.of("MOTHER_SIGNUP_WELCOME", "ONBOARDING_COMPLETE", "PROVIDER_SIGNUP_WELCOME", "PROVIDER_APPROVED", "PROVIDER_LIVE")) {
            RenderedEmail email = templates.render(type, "Asha").orElseThrow();
            Files.writeString(dir.resolve(type.toLowerCase() + ".html"), email.html(), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(type.toLowerCase() + ".txt"), email.subject() + "\n\n" + email.text(), StandardCharsets.UTF_8);
        }
        Files.writeString(dir.resolve("other_emails.html"), EmailLayout.html("Your NartureBelle application",
                "Hello,\n\nThank you for applying. Our team will check your details and let you know the outcome.\n\n"
                        + "Sign in any time: https://www.narturebelle.com/provider/apply\n\n-- The NartureBelle team"), StandardCharsets.UTF_8);
    }
}
