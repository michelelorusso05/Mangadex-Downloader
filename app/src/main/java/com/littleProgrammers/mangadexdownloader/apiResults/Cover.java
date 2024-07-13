package com.littleProgrammers.mangadexdownloader.apiResults;

import java.io.Serializable;

public class Cover implements Serializable {
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
}
