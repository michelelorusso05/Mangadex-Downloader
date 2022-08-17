package com.littleProgrammers.mangadexdownloader;

import android.text.Spannable;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

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
        s = s.replace("[", "");
        s = s.replaceAll("][(].*[)]", "");

        return s;
    }
    private static String SinglePatternToHTML(@NonNull String s, String singlePattern, String open, String close) {
        boolean end = false;
        String[] parts = s.split(singlePattern);
        String r = parts[0];
        for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
            String str = parts[i];
            r = r.concat((end ? close : open).concat(str));
            end = !end;
        }
        return r;
    }
}
