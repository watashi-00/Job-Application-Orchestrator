package com.watashi.core.domain.common;

public final class HtmlUtils {

    private HtmlUtils() {
        // Utility class
    }

    public static String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String clean = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("\u00a0", " ")
                .replaceAll("\\r?\\n\\s*\\r?\\n", "\n\n");
        return clean.strip();
    }
}
