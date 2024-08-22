package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Cover implements Serializable, Parcelable {
    String id;
    String type;
    CoverAttributes attributes;

    public Cover() {}

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

    public CoverAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(CoverAttributes attributes) {
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

    protected Cover(Parcel in) {
        this.id = in.readString();
        this.type = in.readString();
        this.attributes = in.readParcelable(CoverAttributes.class.getClassLoader());
    }

    public static final Creator<Cover> CREATOR = new Creator<Cover>() {
        @Override
        public Cover createFromParcel(Parcel source) {
            return new Cover(source);
        }

        @Override
        public Cover[] newArray(int size) {
            return new Cover[size];
        }
    };
}
