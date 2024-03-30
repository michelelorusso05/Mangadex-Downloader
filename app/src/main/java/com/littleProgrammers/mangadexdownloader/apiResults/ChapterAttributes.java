package com.littleProgrammers.mangadexdownloader.apiResults;

import java.io.Serializable;

public class ChapterAttributes implements Serializable {
    String title;
    String volume;
    String chapter;
    int pages;
    String translatedLanguage;
    String scanlationGroupString;
    String scanlationGroupID;

    public String getScanlationGroupID() {
        return scanlationGroupID;
    }

    public void setScanlationGroupID(String scanlationGroupID) {
        this.scanlationGroupID = scanlationGroupID;
    }

    public String getScanlationGroupString() {
        return scanlationGroupString;
    }

    public void setScanlationGroupString(String scanlationGroupString) {
        this.scanlationGroupString = scanlationGroupString;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    String externalUrl;

    public String getFormattedName() {
        return formattedName;
    }

    public void setFormattedName(String formattedName) {
        this.formattedName = formattedName;
    }

    String formattedName;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public String getTranslatedLanguage() {
        return translatedLanguage;
    }

    public void setTranslatedLanguage(String translatedLanguage) {
        this.translatedLanguage = translatedLanguage;
    }
}
