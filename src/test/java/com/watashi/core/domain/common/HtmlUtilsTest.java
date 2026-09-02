package com.watashi.core.domain.common;

import junit.framework.TestCase;

public class HtmlUtilsTest extends TestCase {

    public void testStripHtmlNullOrBlank() {
        assertEquals("", HtmlUtils.stripHtml(null));
        assertEquals("", HtmlUtils.stripHtml(""));
        assertEquals("", HtmlUtils.stripHtml("   "));
    }

    public void testStripHtmlTagsAndEntities() {
        String input =
                "<p>Hello <b>World</b>!</p><br/><a href=\"#\">Link</a> &amp; &lt;tag&gt; &quot;quote&quot; &#39;single&#39;";
        String expected = "Hello World!\n\nLink & <tag> \"quote\" 'single'";
        assertEquals(expected, HtmlUtils.stripHtml(input));
    }
}
