package com.littleProgrammers.mangadexdownloader.apiResults;

import android.text.Spanned;

import com.littleProgrammers.mangadexdownloader.utils.FormattingUtilities;

import java.io.Serializable;
import java.util.HashMap;

public class Manga implements Serializable {
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

    public MangaAttributes getAttributes() {
        return attributes;
    }

    public Spanned getCanonicalDescription() {
        HashMap<String, String> desc = attributes.getDescription();
        String descriptionString = (desc.containsKey("en")) ?
                desc.get("en") :
                desc.entrySet().iterator().next().getValue();
        if (descriptionString != null && !descriptionString.isEmpty())
            return FormattingUtilities.FormatFromHtml(FormattingUtilities.MarkdownLite(descriptionString));

        return null;
    }

    public void setAttributes(MangaAttributes attributes) {
        this.attributes = attributes;
    }

    /**
     * Autofills author, artist and cover art information
     */
    public void autofillInformation() {
        StringBuilder authorString = new StringBuilder();
        StringBuilder artistString = new StringBuilder();

        for (Relationship relationship : getRelationships()) {
            switch (relationship.getType()) {
                case "author": {
                    String name = (relationship.getAttributes() != null) ? relationship.getAttributes().get("name").textValue() : "-";

                    if (authorString.length() == 0)
                        authorString.append(name);
                    else
                        authorString.append(", ").append(name);
                    break;
                }
                case "artist": {
                    String name = (relationship.getAttributes() != null) ? relationship.getAttributes().get("name").textValue() : "-";

                    if (artistString.length() == 0)
                        artistString.append(name);
                    else
                        artistString.append(", ").append(name);
                    break;
                }
                case "cover_art":
                    String coverUrl = relationship.getAttributes().get("fileName").textValue();
                    attributes.setCoverUrl(coverUrl);
                    break;
            }
        }

        attributes.setAuthorString(authorString.toString());
        attributes.setArtistString(artistString.toString());
    }

    public Relationship[] getRelationships() {
        return relationships;
    }

    public void setRelationships(Relationship[] relationships) {
        this.relationships = relationships;
    }

    String id;
    String type;
    MangaAttributes attributes;
    Relationship[] relationships;
}
