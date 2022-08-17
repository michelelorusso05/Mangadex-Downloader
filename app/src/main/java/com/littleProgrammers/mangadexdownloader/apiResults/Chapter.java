package com.littleProgrammers.mangadexdownloader.apiResults;

public class Chapter {
    String id;
    String type;
    ChapterAttributes attributes;
    Relationship[] relationships;

    public Relationship[] getRelationships() {
        return relationships;
    }

    public void setRelationships(Relationship[] relationships) {
        this.relationships = relationships;
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

    public ChapterAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(ChapterAttributes attributes) {
        this.attributes = attributes;
    }
}
