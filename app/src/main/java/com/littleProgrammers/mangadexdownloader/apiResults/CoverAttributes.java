package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class CoverAttributes implements Serializable, Parcelable {
    String volume;
    String fileName;
    String description;
    String locale;

    public CoverAttributes() {}

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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.volume);
        dest.writeString(this.fileName);
        dest.writeString(this.description);
        dest.writeString(this.locale);
    }

    protected CoverAttributes(Parcel in) {
        this.volume = in.readString();
        this.fileName = in.readString();
        this.description = in.readString();
        this.locale = in.readString();
    }

    public static final Creator<CoverAttributes> CREATOR = new Creator<CoverAttributes>() {
        @Override
        public CoverAttributes createFromParcel(Parcel source) {
            return new CoverAttributes(source);
        }

        @Override
        public CoverAttributes[] newArray(int size) {
            return new CoverAttributes[size];
        }
    };
}
