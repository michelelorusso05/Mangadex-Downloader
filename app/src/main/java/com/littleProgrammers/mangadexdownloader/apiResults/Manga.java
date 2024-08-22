package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;

import java.io.Serializable;

public class Manga implements Serializable, Parcelable {
    String id;
    String type;
    MangaAttributes attributes;
    Relationship[] relationships;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MangaAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(MangaAttributes attributes) {
        this.attributes = attributes;
    }

    public Relationship[] getRelationships() {
        return relationships;
    }

    public void setRelationships(Relationship[] relationships) {
        this.relationships = relationships;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.type);
        dest.writeParcelable(this.attributes, flags);
        dest.writeTypedArray(this.relationships, flags);
    }

    public Manga() {
    }

    protected Manga(Parcel in) {
        this.id = in.readString();
        this.type = in.readString();
        this.attributes = CompatUtils.GetParcelableFromParcel(in, MangaAttributes.class);
        this.relationships = in.createTypedArray(Relationship.CREATOR);
    }

    public static final Creator<Manga> CREATOR = new Creator<Manga>() {
        @Override
        public Manga createFromParcel(Parcel source) {
            return new Manga(source);
        }

        @Override
        public Manga[] newArray(int size) {
            return new Manga[size];
        }
    };
}
