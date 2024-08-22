package com.littleProgrammers.mangadexdownloader.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.utils.ApiUtils;

@Entity(indices = {@Index(value = {"title"})})
public class DbManga {
    @PrimaryKey
    @NonNull
    public final String id;

    public final String title;
    public final String author;
    public final String artist;

    public DbManga(String artist, String author, @NonNull String id, String title) {
        this.artist = artist;
        this.author = author;
        this.id = id;
        this.title = title;
    }

    public DbManga(Manga manga) {
        this.id = manga.getId();
        this.title = ApiUtils.GetMangaTitleString(manga);
        this.author = manga.getAttributes().getAuthorString();
        this.artist = manga.getAttributes().getArtistString();
    }
}
