package com.narturebelle.mails.email;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplatesTest {

    private final EmailTemplates templates = new EmailTemplates("https://www.narturebelle.com");

    @ParameterizedTest
    @CsvSource({
            "MOTHER_SIGNUP_WELCOME, Welcome to NartureBelle, https://www.narturebelle.com/onboarding",
            "ONBOARDING_COMPLETE, You're all set, https://www.narturebelle.com/journey",
            "PROVIDER_SIGNUP_WELCOME, Welcome to NartureBelle, https://www.narturebelle.com/provider/apply",
            "PROVIDER_APPROVED, You're approved, https://www.narturebelle.com/provider/setup",
            "PROVIDER_LIVE, You're live, https://www.narturebelle.com/provider"
    })
    void eachWelcome_hasItsSubject_greetsByName_andLinksToItsPage(String eventType, String subjectStart, String link) {
        RenderedEmail email = templates.render(eventType, "Asha").orElseThrow();

        assertTrue(email.subject().startsWith(subjectStart), email.subject());
        assertTrue(email.html().contains("Asha"));
        assertTrue(email.html().contains("href=\"" + link + "\""));
        assertTrue(email.text().contains(link), "the plain-text version carries the link too");
        assertTrue(email.html().contains("https://www.narturebelle.com/web-app-manifest-192x192.png"), "logo");
    }

    @Test
    void aNameIsEscaped_soItCanNeverBecomeMarkup() {
        RenderedEmail email = templates.render("ONBOARDING_COMPLETE", "<b>Asha</b>").orElseThrow();
        assertFalse(email.html().contains("<b>Asha</b>"));
        assertTrue(email.html().contains("&lt;b&gt;Asha&lt;/b&gt;"));
    }

    @Test
    void withoutAName_theGreetingStillReadsWell() {
        RenderedEmail email = templates.render("ONBOARDING_COMPLETE", "  ").orElseThrow();
        assertTrue(email.html().contains(">You&#39;re all set</h1>"));
        assertFalse(email.text().contains("set, \n"));
    }

    @Test
    void otherEventTypes_haveNoTemplate() {
        assertTrue(templates.render("PROVIDER_APPLICATION_STATUS", "Asha").isEmpty());
    }

    @Test
    void theSiteAddress_isConfigurable_forStage() {
        RenderedEmail email = new EmailTemplates("https://stage-narturebelle.com/").render("PROVIDER_LIVE", null).orElseThrow();
        assertTrue(email.html().contains("href=\"https://stage-narturebelle.com/provider\""));
    }
}
