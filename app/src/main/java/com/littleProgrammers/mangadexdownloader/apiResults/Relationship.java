package com.littleProgrammers.mangadexdownloader.apiResults;

import com.fasterxml.jackson.databind.JsonNode;

public class Relationship
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

    String id;
    String type;

    JsonNode attributes;

    public JsonNode getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonNode attributes) {
        this.attributes = attributes;
    }
}