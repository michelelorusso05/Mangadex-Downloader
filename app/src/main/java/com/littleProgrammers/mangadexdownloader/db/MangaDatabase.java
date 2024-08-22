package com.littleProgrammers.mangadexdownloader.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {DbManga.class, DbChapter.class}, version = 5, exportSchema = false)
public abstract class MangaDatabase extends RoomDatabase {

    public abstract DbMangaDAO MangaDAO();

    private static volatile MangaDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static MangaDatabase GetDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MangaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MangaDatabase.class, "downloaded_manga_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}