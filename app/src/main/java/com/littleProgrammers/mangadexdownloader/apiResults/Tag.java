package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;

import java.io.Serializable;

public class Tag implements Serializable, Parcelable {
    String id;
    String type;
    TagAttributes attributes;

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

    public TagAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(TagAttributes attributes) {
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
    }

    public Tag() {}

    protected Tag(Parcel in) {
        this.id = in.readString();
        this.type = in.readString();
        this.attributes = CompatUtils.GetParcelableFromParcel(in, TagAttributes.class);
    }

    public static final Creator<Tag> CREATOR = new Creator<Tag>() {
        @Override
        public Tag createFromParcel(Parcel source) {
            return new Tag(source);
        }

        @Override
        public Tag[] newArray(int size) {
            return new Tag[size];
        }
    };
}
