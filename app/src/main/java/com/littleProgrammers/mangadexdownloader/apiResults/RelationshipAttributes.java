package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

public class RelationshipAttributes implements Parcelable {
    String name;
    String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.fileName);
    }

    public RelationshipAttributes() {}

    protected RelationshipAttributes(Parcel in) {
        this.name = in.readString();
        this.fileName = in.readString();
    }

    public static final Creator<RelationshipAttributes> CREATOR = new Creator<RelationshipAttributes>() {
        @Override
        public RelationshipAttributes createFromParcel(Parcel source) {
            return new RelationshipAttributes(source);
        }

        @Override
        public RelationshipAttributes[] newArray(int size) {
            return new RelationshipAttributes[size];
        }
    };
}
