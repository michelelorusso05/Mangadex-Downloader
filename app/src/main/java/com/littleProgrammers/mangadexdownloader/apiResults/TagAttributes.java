package com.littleProgrammers.mangadexdownloader.apiResults;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;

public class TagAttributes implements Serializable, Parcelable {
    HashMap<String, String> name = new HashMap<>();
    HashMap<String, String> description = new HashMap<>();
    String group;
    Integer version;

    public HashMap<String, String> getName() {
        return name;
    }

    public void setName(HashMap<String, String> name) {
        this.name = name;
    }

    public HashMap<String, String> getDescription() {
        return description;
    }

    public void setDescription(HashMap<String, String> description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(this.name);
        dest.writeMap(this.description);
        dest.writeString(this.group);
        dest.writeValue(this.version);
    }

    public TagAttributes() {}

    @SuppressWarnings("unchecked")
    protected TagAttributes(Parcel in) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            this.name = in.readHashMap(String.class.getClassLoader(), String.class, String.class);
        else
            this.name = (HashMap<String, String>) in.readHashMap(String.class.getClassLoader());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            this.description = in.readHashMap(String.class.getClassLoader(), String.class, String.class);
        else
            this.description = (HashMap<String, String>) in.readHashMap(String.class.getClassLoader());
        this.group = in.readString();
        this.version = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    public static final Parcelable.Creator<TagAttributes> CREATOR = new Parcelable.Creator<TagAttributes>() {
        @Override
        public TagAttributes createFromParcel(Parcel source) {
            return new TagAttributes(source);
        }

        @Override
        public TagAttributes[] newArray(int size) {
            return new TagAttributes[size];
        }
    };
}
