package com.narturebelle.mails.email;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The branded frame around an email's plain text, for every email with no designed template (EmailTemplates).
 * Same logo, card and footer as the designed ones; the text itself goes out unchanged alongside this HTML.
 */
public final class EmailLayout {

    private static final String SITE = "https://www.narturebelle.com";
    private static final Pattern LINK = Pattern.compile("https?://[^\\s<]+[^\\s<.,;:!?)]");

    private EmailLayout() {}

    public static String html(String subject, String text) {
        String paragraphs = Arrays.stream(text.strip().split("\\R\\s*\\R"))
                .map(paragraph -> "<p style=\"margin:0 0 16px;font-family:" + BrandedEmail.SANS + ";font-size:16px;line-height:26px;color:"
                        + BrandedEmail.TEXT + "\">" + linkify(BrandedEmail.escape(paragraph.strip())).replaceAll("\\R", "<br>") + "</p>")
                .collect(Collectors.joining());
        String card = "<tr><td class=\"px\" bgcolor=\"#ffffff\" style=\"background:#ffffff;border-radius:20px;padding:36px 44px 24px\">"
                + paragraphs + "</td></tr>";
        return BrandedEmail.page(SITE, subject, null, card, "");
    }

    private static String linkify(String escaped) {
        Matcher matcher = LINK.matcher(escaped);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group();
            matcher.appendReplacement(out, Matcher.quoteReplacement(
                    "<a href=\"" + url + "\" style=\"color:" + BrandedEmail.SAGE_700 + "\">" + url + "</a>"));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
