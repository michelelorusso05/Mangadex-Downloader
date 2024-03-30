package com.littleProgrammers.mangadexdownloader.apiResults;

import android.content.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.littleProgrammers.mangadexdownloader.R;

import java.io.Serializable;
import java.util.HashMap;

public class Relationship implements Serializable
{
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

    String id;
    String type;
    String related;

    JsonNode attributes;

    public JsonNode getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonNode attributes) {
        this.attributes = attributes;
    }

    static HashMap<String, Integer> idToStringResourceIndex;

    private static void initLUP(Context context) {
        String[] arr = context.getResources().getStringArray(R.array.manga_relationships_enum);

        idToStringResourceIndex = new HashMap<>(arr.length);

        for (int i = 0; i < arr.length; i++)
            idToStringResourceIndex.put(arr[i], i);
    }
    public static String getTranslatedRelationship(Context context, String key) {
        if (idToStringResourceIndex == null)
            initLUP(context);

        Integer index = idToStringResourceIndex.get(key);

        if (index == null)
            return key;

        return context.getResources().getStringArray(R.array.related_manga_type)[index];
    }
}