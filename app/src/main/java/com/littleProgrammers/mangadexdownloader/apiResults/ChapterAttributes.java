package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ChapterAttributes implements Serializable, Parcelable {
    String title;
    String volume;
    String chapter;
    int pages;
    String translatedLanguage;
    String scanlationGroupString;
    String scanlationGroupID;
    String externalUrl;

    // Cached values
    String formattedName;

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

    public String getFormattedName() {
        return formattedName;
    }

    public void setFormattedName(String formattedName) {
        this.formattedName = formattedName;
    }


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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.volume);
        dest.writeString(this.chapter);
        dest.writeInt(this.pages);
        dest.writeString(this.translatedLanguage);
        dest.writeString(this.scanlationGroupString);
        dest.writeString(this.scanlationGroupID);
        dest.writeString(this.externalUrl);
        dest.writeString(this.formattedName);
    }

    public ChapterAttributes() {
    }

    protected ChapterAttributes(Parcel in) {
        this.title = in.readString();
        this.volume = in.readString();
        this.chapter = in.readString();
        this.pages = in.readInt();
        this.translatedLanguage = in.readString();
        this.scanlationGroupString = in.readString();
        this.scanlationGroupID = in.readString();
        this.externalUrl = in.readString();
        this.formattedName = in.readString();
    }

    public static final Creator<ChapterAttributes> CREATOR = new Creator<ChapterAttributes>() {
        @Override
        public ChapterAttributes createFromParcel(Parcel source) {
            return new ChapterAttributes(source);
        }

        @Override
        public ChapterAttributes[] newArray(int size) {
            return new ChapterAttributes[size];
        }
    };
}
