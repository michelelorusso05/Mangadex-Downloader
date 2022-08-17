package com.littleProgrammers.mangadexdownloader.apiResults;

public class Author {
    String id;
    String type;
    AuthorAttributes attributes;

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

    public AuthorAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(AuthorAttributes attributes) {
        this.attributes = attributes;
    }
}
