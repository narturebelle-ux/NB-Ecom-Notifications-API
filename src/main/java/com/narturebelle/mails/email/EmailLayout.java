package com.narturebelle.mails.email;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The one branded HTML layout around an email's plain text. Callers still send text only; the text itself goes
 * out unchanged alongside this HTML version. Inline styles and tables, because email clients ignore stylesheets.
 */
public final class EmailLayout {

    private static final Pattern LINK = Pattern.compile("https?://[^\\s<]+[^\\s<.,;:!?)]");

    private static final String SAGE_900 = "#15302a";
    private static final String SAGE_700 = "#275448";
    private static final String SAGE_50 = "#f1f8f4";
    private static final String TEXT = "#2b3230";
    private static final String MUTED = "#6b7470";

    private EmailLayout() {}

    public static String html(String subject, String text) {
        String paragraphs = Arrays.stream(text.strip().split("\\R\\s*\\R"))
                .map(paragraph -> "<p style=\"margin:0 0 16px\">" + linkify(escape(paragraph.strip())).replaceAll("\\R", "<br>") + "</p>")
                .collect(Collectors.joining());
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title></head>
                <body style="margin:0;padding:0;background:%s">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:%s">
                <tr><td align="center" style="padding:24px 12px">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border-radius:12px">
                <tr><td style="background:%s;border-radius:12px 12px 0 0;padding:20px 28px;font-family:Georgia,'Times New Roman',serif;font-size:22px;color:#ffffff">NartureBelle&trade;</td></tr>
                <tr><td style="padding:28px;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:%s">%s</td></tr>
                <tr><td style="padding:16px 28px 24px;border-top:1px solid %s;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.5;color:%s">This email was sent by NartureBelle. <a href="https://narturebelle.com" style="color:%s">narturebelle.com</a></td></tr>
                </table>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(escape(subject), SAGE_50, SAGE_50, SAGE_900, TEXT, paragraphs, SAGE_50, MUTED, SAGE_700);
    }

    private static String linkify(String escaped) {
        Matcher matcher = LINK.matcher(escaped);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group();
            matcher.appendReplacement(out, Matcher.quoteReplacement(
                    "<a href=\"" + url + "\" style=\"color:" + SAGE_700 + "\">" + url + "</a>"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
