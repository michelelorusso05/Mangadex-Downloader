package com.littleProgrammers.mangadexdownloader.apiResults;


import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;

public class MangaAttributes implements Serializable, Parcelable {
    HashMap<String, String> title = new HashMap<>();
    HashMap<String, String> description = new HashMap<>();
    HashMap<String, String> links = new HashMap<>();
    String originalLanguage;

    String publicationDemographic;
    String status;
    Integer year;
    String contentRating;
    String[] availableTranslatedLanguages;
    Tag[] tags;

    // Additional fields for caching purposes
    String authorString = null;
    String artistString = null;
    String coverUrl;
    String shortDescription;


    public String[] getAvailableTranslatedLanguages() {
        return availableTranslatedLanguages;
    }

    public void setAvailableTranslatedLanguages(String[] availableTranslatedLanguages) {
        this.availableTranslatedLanguages = availableTranslatedLanguages;
    }

    public String getContentRating() {
        return contentRating;
    }

    public void setContentRating(String contentRating) {
        this.contentRating = contentRating;
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

    public HashMap<String, String> getLinks() {
        return links;
    }

    public void setLinks(HashMap<String, String> links) {
        this.links = links;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public String getPublicationDemographic() {
        return publicationDemographic;
    }

    public void setPublicationDemographic(String publicationDemographic) {
        this.publicationDemographic = publicationDemographic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Tag[] getTags() {
        return tags;
    }

    public void setTags(Tag[] tags) {
        this.tags = tags;
    }

    public HashMap<String, String> getTitle() {
        return title;
    }

    public void setTitle(HashMap<String, String> title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }


    public String getArtistString() {
        return artistString;
    }

    public void setArtistString(String artistString) {
        this.artistString = artistString;
    }

    public String getAuthorString() {
        return authorString;
    }

    public void setAuthorString(String authorString) {
        this.authorString = authorString;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public MangaAttributes() {}


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(this.title);
        dest.writeMap(this.description);
        dest.writeMap(this.links);
        dest.writeString(this.originalLanguage);
        dest.writeString(this.publicationDemographic);
        dest.writeString(this.status);
        dest.writeValue(this.year);
        dest.writeString(this.contentRating);
        dest.writeStringArray(this.availableTranslatedLanguages);
        dest.writeTypedArray(this.tags, flags);
        dest.writeString(this.authorString);
        dest.writeString(this.artistString);
        dest.writeString(this.coverUrl);
        dest.writeString(this.shortDescription);
    }

    @SuppressWarnings("unchecked")
    protected MangaAttributes(Parcel in) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            this.title = in.readHashMap(String.class.getClassLoader(), String.class, String.class);
        else
            this.title = (HashMap<String, String>) in.readHashMap(String.class.getClassLoader());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            this.description = in.readHashMap(String.class.getClassLoader(), String.class, String.class);
        else
            this.description = (HashMap<String, String>) in.readHashMap(String.class.getClassLoader());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            this.links = in.readHashMap(String.class.getClassLoader(), String.class, String.class);
        else
            this.links = (HashMap<String, String>) in.readHashMap(String.class.getClassLoader());
        this.originalLanguage = in.readString();
        this.publicationDemographic = in.readString();
        this.status = in.readString();
        this.year = (Integer) in.readValue(Integer.class.getClassLoader());
        this.contentRating = in.readString();
        this.availableTranslatedLanguages = in.createStringArray();
        this.tags = in.createTypedArray(Tag.CREATOR);
        this.authorString = in.readString();
        this.artistString = in.readString();
        this.coverUrl = in.readString();
        this.shortDescription = in.readString();
    }

    public static final Creator<MangaAttributes> CREATOR = new Creator<MangaAttributes>() {
        @Override
        public MangaAttributes createFromParcel(Parcel source) {
            return new MangaAttributes(source);
        }

        @Override
        public MangaAttributes[] newArray(int size) {
            return new MangaAttributes[size];
        }
    };
}

