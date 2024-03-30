package com.littleProgrammers.mangadexdownloader.apiResults;

import android.content.Context;

import com.littleProgrammers.mangadexdownloader.R;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Tag implements Serializable {
    String id;
    String type;
    TagAttributes attributes;

    static HashMap<String, Integer> idToStringResourceIndex;

    private static void initLUP(Context context) {
        String[] arr = context.getResources().getStringArray(R.array.tag_ids);

        idToStringResourceIndex = new HashMap<>(arr.length);

        for (int i = 0; i < arr.length; i++)
            idToStringResourceIndex.put(arr[i], i);
    }
    public String getTranslatedName(Context context) {
        if (idToStringResourceIndex == null)
            initLUP(context);

        Integer index = idToStringResourceIndex.get(getId());

        if (index == null)
            return getAttributes().getName().get("en");

        return context.getResources().getStringArray(R.array.tags)[index];
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

    public TagAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(TagAttributes attributes) {
        this.attributes = attributes;
    }

    public static class TagAttributes implements Serializable {
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
    }
}
