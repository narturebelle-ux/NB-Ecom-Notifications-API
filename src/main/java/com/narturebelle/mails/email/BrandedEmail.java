package com.narturebelle.mails.email;

import java.util.ArrayList;
import java.util.List;

/**
 * The NartureBelle email design, in the website's colours and type: logo, a deep sage hero with a serif heading,
 * a white card, a pill button and a quiet footer. Built for email clients: a 600 px table layout, inline styles,
 * web fonts with safe fallbacks, light colours only. Builds the plain-text version from the same content.
 * Every caller-supplied word is escaped.
 */
public final class BrandedEmail {

    static final String SAGE_900 = "#15302a";
    static final String SAGE_700 = "#275448";
    static final String SAGE_100 = "#e1f0e8";
    static final String SAGE_50 = "#f1f8f4";
    static final String GOLD_300 = "#e3bd6f";
    static final String TEXT = "#2b3230";
    static final String MUTED = "#6b7470";
    static final String SERIF = "'Playfair Display',Georgia,'Times New Roman',serif";
    static final String SANS = "'Plus Jakarta Sans',Arial,Helvetica,sans-serif";

    public static final String SUPPORT_EMAIL = "support-care@narturebelle.com";

    /** A step or a feature: a short bold title and one line under it. */
    public record Item(String title, String description) {}

    private final String siteUrl;
    private String subject = "";
    private String preheader = "";
    private String eyebrow;
    private String title = "";
    private String lead;
    private final StringBuilder body = new StringBuilder();
    private final StringBuilder text = new StringBuilder();
    private List<String> assurances = List.of();

    private BrandedEmail(String siteUrl) {
        this.siteUrl = siteUrl.replaceAll("/+$", "");
    }

    public static BrandedEmail on(String siteUrl) {
        return new BrandedEmail(siteUrl);
    }

    public BrandedEmail subject(String subject) { this.subject = subject; return this; }

    /** The line inbox apps show after the subject. */
    public BrandedEmail preheader(String preheader) { this.preheader = preheader; return this; }

    public BrandedEmail hero(String eyebrow, String title, String lead) {
        this.eyebrow = eyebrow;
        this.title = title;
        this.lead = lead;
        return this;
    }

    public BrandedEmail paragraph(String words) {
        body.append("<p style=\"margin:0 0 18px;font-family:").append(SANS).append(";font-size:16px;line-height:26px;color:")
                .append(TEXT).append("\">").append(escape(words)).append("</p>");
        text.append(words).append("\n\n");
        return this;
    }

    /** Numbered steps, in order. */
    public BrandedEmail steps(String heading, List<Item> items) {
        return list(heading, items, true);
    }

    /** Things the reader can now do, each with a tick. */
    public BrandedEmail features(String heading, List<Item> items) {
        return list(heading, items, false);
    }

    /** The one call to action, as a pill button. The link is the site address plus this path. */
    public BrandedEmail button(String label, String path) {
        String url = siteUrl + path;
        body.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:30px 0 10px\"><tr>")
                .append("<td align=\"center\" bgcolor=\"").append(SAGE_700).append("\" style=\"border-radius:999px;background:").append(SAGE_700).append("\">")
                .append("<a href=\"").append(escape(url)).append("\" target=\"_blank\" style=\"display:inline-block;padding:15px 32px;font-family:")
                .append(SANS).append(";font-size:15px;line-height:20px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:999px\">")
                .append(escape(label)).append(" &rarr;</a></td></tr></table>");
        text.append(label).append(": ").append(url).append("\n\n");
        return this;
    }

    /** A small, quiet line, such as what happens next. */
    public BrandedEmail note(String words) {
        body.append("<p style=\"margin:14px 0 0;font-family:").append(SANS).append(";font-size:14px;line-height:22px;color:")
                .append(MUTED).append("\">").append(escape(words)).append("</p>");
        text.append(words).append("\n\n");
        return this;
    }

    /** The short promises shown under the card, as on the website's home page. */
    public BrandedEmail assurances(List<String> assurances) {
        this.assurances = List.copyOf(assurances);
        return this;
    }

    public RenderedEmail build() {
        StringBuilder hero = new StringBuilder();
        hero.append("<tr><td class=\"px\" bgcolor=\"").append(SAGE_900).append("\" style=\"background:").append(SAGE_900)
                .append(";border-radius:20px 20px 0 0;padding:44px 44px 40px\">");
        if (eyebrow != null) {
            hero.append("<p style=\"margin:0 0 14px;font-family:").append(SANS).append(";font-size:12px;line-height:16px;font-weight:700;")
                    .append("letter-spacing:1.6px;text-transform:uppercase;color:").append(GOLD_300).append("\">").append(escape(eyebrow)).append("</p>");
        }
        hero.append("<h1 class=\"h1\" style=\"margin:0;font-family:").append(SERIF).append(";font-size:34px;line-height:42px;font-weight:600;color:#ffffff\">")
                .append(escape(title)).append("</h1>");
        if (lead != null) {
            hero.append("<p style=\"margin:16px 0 0;font-family:").append(SANS).append(";font-size:17px;line-height:27px;color:")
                    .append(SAGE_100).append("\">").append(escape(lead)).append("</p>");
        }
        hero.append("</td></tr>");

        String card = "<tr><td class=\"px\" bgcolor=\"#ffffff\" style=\"background:#ffffff;border-radius:0 0 20px 20px;padding:40px 44px 40px\">"
                + body + "</td></tr>";

        StringBuilder plain = new StringBuilder();
        if (eyebrow != null) plain.append(eyebrow.toUpperCase()).append("\n\n");
        plain.append(title).append("\n");
        if (lead != null) plain.append(lead).append("\n");
        plain.append("\n").append(text);
        for (String assurance : assurances) plain.append("- ").append(assurance).append("\n");
        if (!assurances.isEmpty()) plain.append("\n");
        plain.append(footerText());

        return new RenderedEmail(subject, page(siteUrl, subject, preheader, hero + card, assuranceRow()), plain.toString());
    }

    // ---- shared frame, also used by EmailLayout for every other email ----

    static String page(String siteUrl, String title, String preheader, String cardRows, String afterCard) {
        String site = siteUrl.replaceAll("/+$", "");
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<meta name=\"x-apple-disable-message-reformatting\">"
                + "<meta name=\"color-scheme\" content=\"light\"><meta name=\"supported-color-schemes\" content=\"light\">"
                + "<title>" + escape(title) + "</title>"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600&amp;family=Plus+Jakarta+Sans:wght@400;600;700&amp;display=swap\" rel=\"stylesheet\">"
                + "<style>body{margin:0;padding:0}a{color:" + SAGE_700 + "}"
                + "@media (max-width:620px){.px{padding-left:24px!important;padding-right:24px!important}"
                + ".h1{font-size:28px!important;line-height:35px!important}.col{display:block!important;width:100%!important;padding:6px 0!important}}</style>"
                + "</head><body style=\"margin:0;padding:0;background:" + SAGE_50 + "\">"
                + "<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all\">" + escape(preheader == null ? "" : preheader)
                + "&#8199;&#847;&#8199;&#847;&#8199;&#847;&#8199;&#847;&#8199;&#847;&#8199;&#847;&#8199;&#847;&#8199;&#847;</div>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"" + SAGE_50 + "\" style=\"background:" + SAGE_50 + "\">"
                + "<tr><td align=\"center\" style=\"padding:32px 12px 40px\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;max-width:600px\">"
                + "<tr><td style=\"padding:0 4px 20px\"><a href=\"" + escape(site) + "\" target=\"_blank\" style=\"text-decoration:none\">"
                + "<img src=\"" + escape(site) + "/web-app-manifest-192x192.png\" width=\"44\" height=\"44\" alt=\"NartureBelle\" "
                + "style=\"display:inline-block;vertical-align:middle;border:0;border-radius:12px\">"
                + "<span style=\"display:inline-block;vertical-align:middle;padding-left:12px;font-family:" + SERIF + ";font-size:22px;font-weight:600;color:" + SAGE_900 + "\">"
                + "NartureBelle&trade;</span></a></td></tr>"
                + cardRows
                + afterCard
                + "<tr><td align=\"center\" style=\"padding:28px 24px 0;font-family:" + SANS + ";font-size:13px;line-height:21px;color:" + MUTED + "\">"
                + "Questions? Reply to this email or write to <a href=\"mailto:" + SUPPORT_EMAIL + "\" style=\"color:" + SAGE_700 + "\">" + SUPPORT_EMAIL + "</a>.<br>"
                + "NartureBelle&trade; &middot; Professional-led care, pregnancy to postpartum<br>"
                + "<a href=\"" + escape(site) + "\" target=\"_blank\" style=\"color:" + SAGE_700 + "\">" + escape(site.replaceFirst("^https?://", "")) + "</a>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    static String footerText() {
        return "Questions? Reply to this email or write to " + SUPPORT_EMAIL + ".\nNartureBelle - Professional-led care, pregnancy to postpartum\n";
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    // ---- helpers ----

    private BrandedEmail list(String heading, List<Item> items, boolean numbered) {
        if (heading != null) {
            body.append("<p style=\"margin:30px 0 16px;font-family:").append(SERIF).append(";font-size:21px;line-height:28px;font-weight:600;color:")
                    .append(SAGE_900).append("\">").append(escape(heading)).append("</p>");
            text.append(heading).append("\n");
        }
        body.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:")
                .append(heading == null ? "8px" : "0").append(" 0 4px\">");
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            String marker = numbered ? String.valueOf(i + 1) : "&#10003;";
            body.append("<tr><td width=\"44\" valign=\"top\" style=\"padding:0 0 18px\">")
                    .append("<div style=\"width:30px;height:30px;border-radius:15px;background:").append(SAGE_100)
                    .append(";color:").append(SAGE_700).append(";font-family:").append(SANS)
                    .append(";font-size:14px;line-height:30px;font-weight:700;text-align:center\">").append(marker).append("</div></td>")
                    .append("<td valign=\"top\" style=\"padding:3px 0 18px\">")
                    .append("<p style=\"margin:0;font-family:").append(SANS).append(";font-size:16px;line-height:22px;font-weight:700;color:")
                    .append(SAGE_900).append("\">").append(escape(item.title())).append("</p>")
                    .append("<p style=\"margin:4px 0 0;font-family:").append(SANS).append(";font-size:14px;line-height:22px;color:")
                    .append(MUTED).append("\">").append(escape(item.description())).append("</p></td></tr>");
            lines.add((numbered ? (i + 1) + ". " : "- ") + item.title() + ": " + item.description());
        }
        body.append("</table>");
        text.append(String.join("\n", lines)).append("\n\n");
        return this;
    }

    private String assuranceRow() {
        if (assurances.isEmpty()) {
            return "";
        }
        StringBuilder row = new StringBuilder("<tr><td style=\"padding:22px 8px 0\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");
        int width = 100 / assurances.size();
        for (String assurance : assurances) {
            row.append("<td class=\"col\" width=\"").append(width).append("%\" align=\"center\" valign=\"top\" style=\"padding:0 6px;font-family:")
                    .append(SANS).append(";font-size:13px;line-height:19px;color:").append(SAGE_700).append("\">")
                    .append("<span style=\"color:").append(SAGE_700).append(";font-weight:700\">&#10003;</span> ")
                    .append(escape(assurance)).append("</td>");
        }
        return row.append("</tr></table></td></tr>").toString();
    }
}
