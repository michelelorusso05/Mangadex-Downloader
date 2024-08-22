package com.littleProgrammers.mangadexdownloader.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.util.Pair;

import androidx.appcompat.content.res.AppCompatResources;

import com.littleProgrammers.mangadexdownloader.R;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.littleProgrammers.mangadexdownloader.apiResults.Tag;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiUtils {
    public static String GetMangaDescription(Manga manga) {
        HashMap<String, String> desc = manga.getAttributes().getDescription();
        return (desc.containsKey("en")) ? desc.get("en") : desc.entrySet().iterator().next().getValue();
    }

    public static Spanned GetMangaCanonicalDescription(Manga manga) {
        String descriptionString = GetMangaDescription(manga);
        if (descriptionString != null && !descriptionString.isEmpty())
            return FormattingUtilities.FormatFromHtml(FormattingUtilities.MarkdownLite(descriptionString));

        return null;
    }

    public static void SetMangaAttributes(Manga manga) {
        StringBuilder authorString = new StringBuilder();
        StringBuilder artistString = new StringBuilder();

        for (Relationship relationship : manga.getRelationships()) {
            switch (relationship.getType()) {
                case "author": {
                    String name = (relationship.getAttributes() != null) ? relationship.getAttributes().getName() : "-";

                    if (authorString.length() == 0)
                        authorString.append(name);
                    else
                        authorString.append(", ").append(name);
                    break;
                }
                case "artist": {
                    String name = (relationship.getAttributes() != null) ? relationship.getAttributes().getName() : "-";

                    if (artistString.length() == 0)
                        artistString.append(name);
                    else
                        artistString.append(", ").append(name);
                    break;
                }
                case "cover_art":
                    String coverUrl = relationship.getAttributes().getFileName();
                    manga.getAttributes().setCoverUrl(coverUrl);
                    break;
            }
        }

        manga.getAttributes().setAuthorString(authorString.toString());
        manga.getAttributes().setArtistString(artistString.toString());

        String originalDescription = GetMangaDescription(manga);

        if (originalDescription.isEmpty()) return;

        // Find next double newline starting from 150 chars
        originalDescription = originalDescription.substring(0, Math.max(originalDescription.length(), originalDescription.indexOf("\n\n", 150)));

        // Truncate at tables
        Pattern pattern = Pattern.compile("\\|.*\\n\\|[-:\\s]+\\|");
        Matcher matcher = pattern.matcher(originalDescription);

        if (matcher.find()) {
            originalDescription = originalDescription.substring(0, matcher.start());
        }

        // Remove Markdown
        originalDescription = originalDescription.replaceAll("(\\*{1,3}|-{3}|#{1,6}|_)", "");

        // Remove links
        StringBuilder parsedString = new StringBuilder(originalDescription);

        pattern = Pattern.compile("\\[[^]]+]\\([^)]+\\)");
        matcher = pattern.matcher(originalDescription);

        int charDifference = 0;

        while (matcher.find()) {
            int indexOfClosingBracket = originalDescription.indexOf(']', matcher.start());
            String linkName = originalDescription.substring(matcher.start() + 1, indexOfClosingBracket);

            parsedString.replace(matcher.start() + charDifference, matcher.end() + charDifference, linkName);

            charDifference = parsedString.length() - originalDescription.length();
        }

        manga.getAttributes().setShortDescription(parsedString.substring(0, Math.min(parsedString.length(), 150)));
    }

    public static final HashMap<String, Pair<Integer, Integer>> contentRatingStrings;
    public static HashMap<String, String> languages;

    static {
        contentRatingStrings = new HashMap<>(4);
        contentRatingStrings.put("safe", Pair.create(R.string.ratingSafe, R.color.rating_safe));
        contentRatingStrings.put("suggestive", Pair.create(R.string.ratingSuggestive, R.color.rating_suggestive));
        contentRatingStrings.put("erotica", Pair.create(R.string.ratingErotica, R.color.rating_erotica));
        contentRatingStrings.put("pornographic", Pair.create(R.string.ratingPornographic, R.color.rating_pornographic));
    }

    public static Pair<Integer, Integer> GetMangaRatingString(String key) {
        Pair<Integer, Integer> r = contentRatingStrings.get(key);
        if (r != null) return r;
        return Pair.create(R.string.errorUnknown, android.R.color.white);
    }

    public static String GetMangaLangString(Context ctx, String key) {
        if (languages == null) {
            languages = new HashMap<>();
            String[] langKeys = ctx.getResources().getStringArray(R.array.languageValues);
            String[] langStrings = ctx.getResources().getStringArray(R.array.languageEntries);

            for (int i = 0; i < langKeys.length; i++) {
                languages.put(langKeys[i], langStrings[i]);
            }
        }

        String found = languages.get(key);
        return found != null ? found : key;
    }

    public static boolean IsMangaLanguageAvailable(Manga manga, String lang) {
        if (lang.equals(manga.getAttributes().getOriginalLanguage())) return true;
        if (manga.getAttributes().getAvailableTranslatedLanguages() == null) return false;
        for (String translatedLanguage : manga.getAttributes().getAvailableTranslatedLanguages()) {
            if (translatedLanguage.equals(lang)) {
                return true;
            }
        }
        return false;
    }

    public static String GetMangaTitleString(Manga manga) {
        return manga.getAttributes().getTitle().entrySet().iterator().next().getValue();
    }

    static HashMap<String, Integer> idToStringResourceIndex;

    private static void InitTagLUP(Context context) {
        String[] arr = context.getResources().getStringArray(R.array.tag_ids);

        idToStringResourceIndex = new HashMap<>(arr.length);

        for (int i = 0; i < arr.length; i++)
            idToStringResourceIndex.put(arr[i], i);
    }
    public static String GetTagTranslatedName(Context context, Tag tag) {
        if (idToStringResourceIndex == null)
            InitTagLUP(context);

        Integer index = idToStringResourceIndex.get(tag.getId());

        if (index == null)
            return tag.getAttributes().getName().get("en");

        return context.getResources().getStringArray(R.array.tags)[index];
    }

    static HashMap<String, Integer> relationshipIdToStringResourceIndex;

    private static void InitRelationshipLUP(Context context) {
        String[] arr = context.getResources().getStringArray(R.array.manga_relationships_enum);

        relationshipIdToStringResourceIndex = new HashMap<>(arr.length);

        for (int i = 0; i < arr.length; i++)
            relationshipIdToStringResourceIndex.put(arr[i], i);
    }
    public static String GetTranslatedRelationship(Context context, String key) {
        if (relationshipIdToStringResourceIndex == null)
            InitRelationshipLUP(context);

        Integer index = relationshipIdToStringResourceIndex.get(key);

        if (index == null)
            return key;

        return context.getResources().getStringArray(R.array.related_manga_type)[index];
    }

    static HashMap<String, LinkInfo> linkKeyToResources;

    private static void InitLinkLUP(Context context) {

        String[] linkKeys = context.getResources().getStringArray(R.array.link_key);
        String[] linkNames = context.getResources().getStringArray(R.array.link_site_names);
        String[] linkUrls = context.getResources().getStringArray(R.array.link_url_formats);
        TypedArray icons = context.getResources().obtainTypedArray(R.array.link_drawables);

        linkKeyToResources = new HashMap<>(linkKeys.length);

        for (int i = 0; i < linkKeys.length; i++) {
            int iconId = icons.getResourceId(i, 0);

            Drawable drawable = iconId == 0 ? null : AppCompatResources.getDrawable(context, iconId);

            linkKeyToResources.put(linkKeys[i], LinkInfo.create(linkNames[i], linkUrls[i], drawable));
        }

        icons.recycle();
    }

    public static class LinkInfo {
        public final String name;
        public final String urlFormat;
        public final Drawable icon;

        public static LinkInfo create(String n, String f, Drawable i) {
            return new LinkInfo(n, f, i);
        }

        public LinkInfo(String name, String urlFormat, Drawable icon) {
            this.urlFormat = urlFormat;
            this.name = name;
            this.icon = icon;
        }
    }

    public static LinkInfo GetLinkResources(Context context, String key) {
        if (linkKeyToResources == null)
            InitLinkLUP(context);

        return linkKeyToResources.get(key);
    }
}
