package com.littleProgrammers.mangadexdownloader.apiResults;


import android.content.Context;
import android.util.Pair;

import com.littleProgrammers.mangadexdownloader.R;

import java.util.HashMap;

public class MangaAttributes {

    public static HashMap<String, Pair<Integer, Integer>> contentRatingStrings;
    public static HashMap<String, String> languages;

    static {
        contentRatingStrings = new HashMap<>(4);
        contentRatingStrings.put("safe", Pair.create(R.string.ratingSafe, R.color.rating_safe));
        contentRatingStrings.put("suggestive", Pair.create(R.string.ratingSuggestive, R.color.rating_suggestive));
        contentRatingStrings.put("erotica", Pair.create(R.string.ratingErotica, R.color.rating_erotica));
        contentRatingStrings.put("pornographic", Pair.create(R.string.ratingPornographic, R.color.rating_pornographic));
    }

    public static Pair<Integer, Integer> getRatingString(String key) {
        Pair<Integer, Integer> r = contentRatingStrings.get(key);
        if (r != null) return r;
        return Pair.create(R.string.errorUnknown, R.color.white);
    }

    public static String getLangString(Context ctx, String key) {
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

    HashMap<String, String> title = new HashMap<>();
    HashMap<String, String> description = new HashMap<>();
    String originalLanguage;

    String authorID;
    String authorString = null;
    String coverUrl;

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAuthorID() {
        return authorID;
    }

    public void setAuthorID(String authorID) {
        this.authorID = authorID;
    }

    public String getAuthorString() {
        return authorString;
    }

    public void setAuthorString(String authorString) {
        this.authorString = authorString;
    }

    public String[] getAvailableTranslatedLanguages() {
        return availableTranslatedLanguages;
    }

    public void setAvailableTranslatedLanguages(String[] availableTranslatedLanguages) {
        this.availableTranslatedLanguages = availableTranslatedLanguages;
    }

    String[] availableTranslatedLanguages;

    public boolean isLanguageAvailable(String lang) {
        if (lang.equals(originalLanguage)) return true;
        if (availableTranslatedLanguages == null) return false;
        for (String translatedLanguage : availableTranslatedLanguages) {
            if (translatedLanguage.equals(lang)) {
                return true;
            }
        }
        return false;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    Integer year;

    public HashMap<String, String> getTitle() {
        return title;
    }
    public String getTitleS() { return title.entrySet().iterator().next().getValue(); }
    public void setTitle(HashMap<String, String> title) {
        this.title = title;
    }

    public HashMap<String, String> getDescription() {
        if (description == null || description.isEmpty()) setDescription(null);
        return description;
    }
    public void setDescription(HashMap<String, String> description) {
        if (description == null)
            this.description.put("en", "");
        else
            this.description = description;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    String contentRating;

    public String getContentRating() {
        return contentRating;
    }

    public void setContentRating(String contentRating) {
        this.contentRating = contentRating;
    }

    Tag[] tags;

    public Tag[] getTags() {
        return tags;
    }

    public void setTags(Tag[] tags) {
        this.tags = tags;
    }
}

