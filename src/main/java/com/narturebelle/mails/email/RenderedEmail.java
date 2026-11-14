package com.narturebelle.mails.email;

/** A finished email: its subject, the designed HTML, and the plain-text version sent alongside it. */
public record RenderedEmail(String subject, String html, String text) {}
