package com.littleProgrammers.mangadexdownloader.apiResults;

public class ScanlationGroup {
    String id;
    String type;
    ScanlationGroupAttributes attributes;

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

    public ScanlationGroupAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(ScanlationGroupAttributes attributes) {
        this.attributes = attributes;
    }
}
