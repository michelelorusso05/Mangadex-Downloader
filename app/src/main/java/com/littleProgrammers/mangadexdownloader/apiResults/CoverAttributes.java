package com.littleProgrammers.mangadexdownloader.apiResults;

import java.io.Serializable;

public class CoverAttributes implements Serializable {
    String volume;
    String fileName;
    String description;
    String locale;

    public CoverAttributes() {
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
