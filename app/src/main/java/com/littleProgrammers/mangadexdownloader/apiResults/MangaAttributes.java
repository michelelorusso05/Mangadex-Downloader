package com.littleProgrammers.mangadexdownloader.apiResults;

import java.util.HashMap;

public class MangaAttributes {
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
}

