package com.littleProgrammers.mangadexdownloader.apiResults;

import android.text.Html;

import java.util.HashMap;

public class MangaAttributes {
    HashMap<String, String> title = new HashMap<>();
    HashMap<String, String> description = new HashMap<>();
    String originalLanguage;

    String authorID;
    String authorString;
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

    int year;

    public HashMap<String, String> getTitle() {
        return title;
    }
    public String getTitleS() { return title.entrySet().iterator().next().getValue(); }
    public void setTitle(HashMap<String, String> title) {
        this.title = title;
    }

    public HashMap<String, String> getDescription() {
        return description;
    }
    public String getDescriptionS() { return (description.containsKey("en")) ? description.get("en") : description.entrySet().iterator().next().getValue(); }
    public String formatDescription() {
        String descriptionString = getDescriptionS();

        // Format description (remove all tags)
        while (descriptionString.contains("[")) {
            int startIndex = descriptionString.indexOf('[');
            int endIndex = descriptionString.indexOf(']');
            String toBeReplaced = descriptionString.substring(startIndex , endIndex + 1);
            descriptionString = descriptionString.replace(toBeReplaced, "");
        }

        return Html.fromHtml(descriptionString).toString();
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
}

