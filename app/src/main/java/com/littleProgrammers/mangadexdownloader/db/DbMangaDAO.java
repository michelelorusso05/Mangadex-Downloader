package com.littleProgrammers.mangadexdownloader.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class DbMangaDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void InsertMangas(DbManga... mangas);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void InsertChapters(DbChapter... chapters);

    @Delete
    public abstract void DeleteManga(DbManga manga);

    @Delete
    public abstract void DeleteChapter(DbChapter chapter);

    @Query("DELETE FROM DbChapter WHERE id = :id")
    public abstract void DeleteChapterFromId(String id);

    @Query("DELETE FROM DbChapter WHERE id IN (:ids)")
    public abstract void DeleteChapterFromId(List<String> ids);

    @Query("DELETE FROM DbChapter WHERE manga_id = :id")
    public abstract void DeleteChaptersFromMangaId(String id);

    @Query("DELETE FROM DbManga WHERE id = :id")
    public abstract void DeleteMangaFromId(String id);

    @Transaction
    public void DeleteMangaAndChapters(String id) {
        DeleteMangaFromId(id);
        DeleteChaptersFromMangaId(id);
    }

    @Query("SELECT * FROM dbmanga")
    public abstract LiveData<List<DbManga>> GetAll();

    public static class MangaChapterSizeAndNumber {
        public String id;
        public String title;
        public String author;
        public String artist;
        public long size;
        public int number;
    }

    @Query("SELECT DbManga.id AS id, DbManga.title AS title, DbManga.author AS author, DbManga.artist AS artist, COUNT(*) AS number, SUM(DbChapter.size) AS size FROM DbManga, DbChapter WHERE DbManga.id = DbChapter.manga_id GROUP BY DbManga.id")
    public abstract LiveData<List<MangaChapterSizeAndNumber>> LoadMangasWithAttributes();

    @Query("SELECT * FROM DbChapter WHERE DbChapter.manga_id = :id")
    public abstract LiveData<List<DbChapter>> LoadChaptersOfManga(String id);

    public static class MangaChapterSchema {
        public String id;
        public String manga_id;
        public String title;
        public String scanlation_group;
        public int num_pages;
        public long size;
        public boolean is_hq;
        public String original_chapter;
        public String original_volume;
    }

    @Transaction
    @Query("SELECT DbChapter.id, manga_id, DbChapter.title, scanlation_group, num_pages, size, is_hq, originalChapter, originalVolume\n" +
            "FROM DbManga, DbChapter\n" +
            "WHERE DbManga.id = DbChapter.manga_id\n" +
            "ORDER BY DbManga.title, parsedVolume, originalVolume, parsedChapter, originalChapter")
    public abstract LiveData<List<MangaChapterSchema>> GetOrderedChapters();
}
