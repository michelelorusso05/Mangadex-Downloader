package com.littleProgrammers.mangadexdownloader.apiResults;

import java.util.HashMap;

public class Tag {
    String id;
    String type;
    TagAttributes attributes;

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

    public static class TagAttributes {
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
