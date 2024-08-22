package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;

import java.io.Serializable;

public class Relationship implements Serializable, Parcelable
{
    String id;
    String type;
    String related;
    RelationshipAttributes attributes;

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
    public String getRelated() {
        return related;
    }

    public void setRelated(String related) {
        this.related = related;
    }

    public RelationshipAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(RelationshipAttributes attributes) {
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
        dest.writeString(this.related);
        dest.writeParcelable(this.attributes, flags);
    }

    public Relationship() {}

    protected Relationship(Parcel in) {
        this.id = in.readString();
        this.type = in.readString();
        this.related = in.readString();
        this.attributes = CompatUtils.GetParcelableFromParcel(in, RelationshipAttributes.class);
    }

    public static final Creator<Relationship> CREATOR = new Creator<Relationship>() {
        @Override
        public Relationship createFromParcel(Parcel source) {
            return new Relationship(source);
        }

        @Override
        public Relationship[] newArray(int size) {
            return new Relationship[size];
        }
    };
}