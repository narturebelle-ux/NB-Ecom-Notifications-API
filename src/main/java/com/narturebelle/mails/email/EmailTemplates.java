package com.narturebelle.mails.email;

import com.narturebelle.mails.email.BrandedEmail.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * The designed emails, chosen by event type. This service owns their wording; the caller's own subject and text
 * are used only for event types with no template here. No health details, only what the person can do next.
 */
@Component
public class EmailTemplates {

    private static final List<String> MOTHER_ASSURANCES = List.of(
            "Credential-verified professionals", "You decide who sees your records", "Join at any stage");
    private static final List<String> PROVIDER_ASSURANCES = List.of(
            "A fixed fee for every completed session", "Sessions and earnings in one dashboard", "Our team is here to help");

    private final String siteUrl;

    public EmailTemplates(@Value("${email.site-url:https://www.narturebelle.com}") String siteUrl) {
        this.siteUrl = siteUrl;
    }

    /** The designed email for this event type, or empty when it has none. firstName may be null. */
    public Optional<RenderedEmail> render(String eventType, String firstName) {
        String name = firstName == null || firstName.isBlank() ? null : firstName.strip();
        return Optional.ofNullable(switch (eventType) {
            case "MOTHER_SIGNUP_WELCOME" -> motherSignup(name);
            case "ONBOARDING_COMPLETE" -> motherOnboarded(name);
            case "PROVIDER_SIGNUP_WELCOME" -> providerSignup(name);
            case "PROVIDER_APPROVED" -> providerApproved(name);
            case "PROVIDER_LIVE" -> providerLive(name);
            default -> null;
        });
    }

    private RenderedEmail motherSignup(String name) {
        return BrandedEmail.on(siteUrl)
                .subject("Welcome to NartureBelle, let's get you set up")
                .preheader("A few short steps and your care team is ready.")
                .hero("Welcome to NartureBelle™", name == null ? "Your account is ready" : "Hi " + name + ", your account is ready",
                        "Every stage of motherhood, with a team that knows you.")
                .paragraph("From your first trimester to your child's seventh birthday, NartureBelle connects you with verified doctors, "
                        + "nurses and specialists, and keeps your plan, appointments and records together in one private place.")
                .steps("Your next steps", List.of(
                        new Item("Tell us about you", "Your stage and the support you'd like, in a few minutes."),
                        new Item("Choose your care plan", "We recommend one based on your answers."),
                        new Item("Meet your care coordinator", "A short call to plan your care together."),
                        new Item("Sign and you're in", "Sign your onboarding agreement and your care team is ready.")))
                .button("Continue setting up", "/onboarding")
                .note("Your progress is saved, so you can pick up where you left off at any time.")
                .assurances(MOTHER_ASSURANCES)
                .build();
    }

    private RenderedEmail motherOnboarded(String name) {
        return BrandedEmail.on(siteUrl)
                .subject("You're all set, welcome to NartureBelle")
                .preheader("Your care team, your plan and your records, together in one private place.")
                .hero("Onboarding complete", name == null ? "You're all set" : "You're all set, " + name,
                        "Everything on NartureBelle is now open to you.")
                .paragraph("Your care team, your plan and your records now live together in one private place. Here's what you can do from today:")
                .features(null, List.of(
                        new Item("Book sessions with your care team", "Verified doctors, nurses and specialists, online or in person."),
                        new Item("Keep your records in one place", "Upload reports and decide exactly who can see them."),
                        new Item("Follow your journey", "Your next steps, stage by stage, in My Journey.")))
                .button("Go to my journey", "/journey")
                .note("Questions? Just reply to this email and our care team will help.")
                .assurances(MOTHER_ASSURANCES)
                .build();
    }

    private RenderedEmail providerSignup(String name) {
        return BrandedEmail.on(siteUrl)
                .subject("Welcome to NartureBelle, let's complete your application")
                .preheader("One application for every care profession, saved as you go.")
                .hero("Welcome to NartureBelle™", name == null ? "Thank you for applying" : "Hi " + name + ", thank you for applying",
                        "Join a team that cares for mothers at every stage.")
                .paragraph("Your application is open. It adapts to your profession, and your answers save as you go, "
                        + "so you can stop and come back at any time.")
                .steps("How it works", List.of(
                        new Item("Complete your application", "Your details, registration and documents."),
                        new Item("We check your details", "Our Operations team verifies your identity and licence."),
                        new Item("Medical review", "A Medical Reviewer makes the final decision."),
                        new Item("Set up and go live", "Sign your agreement, add your availability, and mothers can book you.")))
                .button("Continue my application", "/provider/apply")
                .note("We'll email you at each step, so you always know where your application stands.")
                .assurances(PROVIDER_ASSURANCES)
                .build();
    }

    private RenderedEmail providerApproved(String name) {
        return BrandedEmail.on(siteUrl)
                .subject("You're approved, welcome to NartureBelle")
                .preheader("Finish your setup and mothers can start booking you.")
                .hero("Application approved", name == null ? "Welcome aboard" : "Welcome aboard, " + name,
                        "Congratulations, your application to join NartureBelle is approved.")
                .paragraph("Our Operations and Medical Reviewers have checked your details. A few setup steps remain before mothers can see and book you.")
                .steps("Finish your setup", List.of(
                        new Item("Sign your professional agreement", "Read the fixed fee and terms, then sign with your name."),
                        new Item("Add bank and tax details", "We confirm them with a small test deposit."),
                        new Item("Complete the short training", "Privacy, emergency escalation and our care standards."),
                        new Item("Add your services and availability", "At least one service and one open slot."),
                        new Item("Confirm your profile", "Check how mothers will see you.")))
                .button("Finish my setup", "/provider/setup")
                .note("Once everything is done, our Operations team sets you live and we'll email you.")
                .assurances(PROVIDER_ASSURANCES)
                .build();
    }

    private RenderedEmail providerLive(String name) {
        return BrandedEmail.on(siteUrl)
                .subject("You're live on NartureBelle")
                .preheader("Mothers can now find and book sessions with you.")
                .hero("You're live", name == null ? "Congratulations" : "Congratulations, " + name,
                        "Your profile is live, and mothers can now book sessions with you.")
                .paragraph("Thank you for joining a team that cares for mothers at every stage. Your dashboard has everything you need:")
                .features(null, List.of(
                        new Item("Your sessions", "Today's and upcoming sessions, with what you need for each."),
                        new Item("Availability and services", "Add slots and update your services at any time."),
                        new Item("Earnings", "A fixed fee for every completed session, tracked in one place.")))
                .button("Open my dashboard", "/provider")
                .note("Keep your licence details current under Profile and documents, so your profile stays live.")
                .assurances(PROVIDER_ASSURANCES)
                .build();
    }
}
