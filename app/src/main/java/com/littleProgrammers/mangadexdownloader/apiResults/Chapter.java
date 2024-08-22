package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;

public class Chapter implements Parcelable {
    String id;
    String type;
    ChapterAttributes attributes;
    Relationship[] relationships;

    public Relationship[] getRelationships() {
        return relationships;
    }

    public void setRelationships(Relationship[] relationships) {
        this.relationships = relationships;
    }

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

    public ChapterAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(ChapterAttributes attributes) {
        this.attributes = attributes;
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

    public Chapter() {
    }

    protected Chapter(Parcel in) {
        this.id = in.readString();
        this.type = in.readString();
        this.attributes = CompatUtils.GetParcelableFromParcel(in, ChapterAttributes.class);
        this.relationships = in.createTypedArray(Relationship.CREATOR);
    }

    public static final Creator<Chapter> CREATOR = new Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel source) {
            return new Chapter(source);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };
}
