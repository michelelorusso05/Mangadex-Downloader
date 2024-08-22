package com.littleProgrammers.mangadexdownloader.utils;

import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormattingUtilities {
    @NonNull
    public static Spanned FormatFromHtml(String s) {
        return HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }
    @NonNull
    public static String MarkdownLite(String s) {
        // There must be a non tag character at the end of the string.
        s = s.concat(" ");
        // Parse triple * (bold and italic)
        s = SinglePatternToHTML(s, "[*]{3}", "<i><b>", "</b></i>");
        // Parse double * (bold)
        s = SinglePatternToHTML(s, "[*]{2}", "<b>", "</b>");
        // Parse single * (italic)
        s = SinglePatternToHTML(s, "[*]{1}", "<i>", "</i>");
        // Remove all #
        s = s.replace("#", "");

        // Link formatting
        s = ParseLinks(s);

        // Remove --- (horizontal lines are not supported)
        s = s.replace("---", "");

        // Bullet lists
        s = s.replace("\n-", "\n&#8226;");

        // Line breaks
        s = s.replace("\n", "<br />");

        return s;
    }
    private static String ParseLinks(@NonNull String s) {
        // Matches every instance of [...](...)
        StringBuilder spannedString = new StringBuilder(s);

        Pattern pattern = Pattern.compile("\\[[^]]+]\\([^)]+\\)");
        Matcher matcher = pattern.matcher(s);

        int charDifference = 0;

        while (matcher.find()) {
            int indexOfClosingBracket = s.indexOf(']', matcher.start());
            String linkName = s.substring(matcher.start() + 1, indexOfClosingBracket);
            String linkUrl = s.substring(indexOfClosingBracket + 2, matcher.end() - 1);

            String link = "<a href=\"" + linkUrl + "\">" + linkName + "</a>";

            // TODO: Make internal links open the manga in another activity
            if (linkUrl.contains("mangadex.org")) {
                spannedString.replace(matcher.start() + charDifference, matcher.end() + charDifference, linkName);
            }
            else {
                spannedString.replace(matcher.start() + charDifference, matcher.end() + charDifference, link);
            }

            charDifference = spannedString.length() - s.length();
        }

        return spannedString.toString();
    }
    private static String SinglePatternToHTML(@NonNull String s, String singlePattern, String open, String close) {
        boolean end = false;
        String[] parts = s.split(singlePattern);
        StringBuilder r = new StringBuilder(parts[0]);
        for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
            String str = parts[i];
            r.append(end ? close : open).append(str);
            end = !end;
        }
        return r.toString();
    }
}
