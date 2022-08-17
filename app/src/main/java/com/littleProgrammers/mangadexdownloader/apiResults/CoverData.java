package com.littleProgrammers.mangadexdownloader.apiResults;

public class CoverData {
    String id;
    String type;
    CoverAttributes attributes;

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
}

