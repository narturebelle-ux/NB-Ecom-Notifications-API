package com.narturebelle.mails.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailLayoutTest {

    @Test
    void escapesTheText_soItCanNeverBecomeMarkup() {
        String html = EmailLayout.html("A <b>subject</b>", "Hi <script>alert(1)</script> & \"you\"");

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("Hi &lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;you&quot;"));
        assertTrue(html.contains("<title>A &lt;b&gt;subject&lt;/b&gt;</title>"));
    }

    @Test
    void keepsParagraphsAndLineBreaks() {
        String html = EmailLayout.html("s", "First line\nsecond line\n\nNext paragraph\n\n-- The NartureBelle team");

        assertTrue(html.contains(">First line<br>second line</p>"));
        assertTrue(html.contains(">Next paragraph</p>"));
        assertTrue(html.contains(">-- The NartureBelle team</p>"));
    }

    @Test
    void turnsAddressesIntoLinks_withoutTheSentencesFullStop() {
        String html = EmailLayout.html("s", "Download it at https://narturebelle.com/admin/careers?id=4&tab=1.");

        assertTrue(html.contains("<a href=\"https://narturebelle.com/admin/careers?id=4&amp;tab=1\""));
        assertTrue(html.contains("tab=1</a>.</p>"));
    }
}
