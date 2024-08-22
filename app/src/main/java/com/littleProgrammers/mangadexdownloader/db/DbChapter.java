package com.littleProgrammers.mangadexdownloader.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

@Entity(indices = {@Index(value = {"parsedVolume", "originalVolume", "parsedChapter", "originalChapter"})})
public class DbChapter {
    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "manga_id")
    @NonNull
    public final String mangaId;

    public final String title;

    @ColumnInfo(name = "scanlation_group")
    public final String scanlationGroup;

    @ColumnInfo(name = "num_pages")
    public final int numPages;

    public final long size;

    @ColumnInfo(name = "is_hq")
    public final boolean isHQ;

    public final float parsedVolume;
    public final String originalVolume;
    public final float parsedChapter;
    public final String originalChapter;

    public DbChapter(@NonNull String id, @NonNull String mangaId, String title, String scanlationGroup, int numPages, long size, boolean isHQ, String volume, String chapter) {
        this.id = id;
        this.mangaId = mangaId;
        this.title = title;
        this.scanlationGroup = scanlationGroup;
        this.numPages = numPages;
        this.size = size;
        this.isHQ = isHQ;
        this.parsedVolume = Parse(volume);
        this.originalVolume = volume;
        this.parsedChapter = Parse(chapter);
        this.originalChapter = chapter;
    }

    public DbChapter(@NonNull String id, boolean isHQ, @NonNull String mangaId, int numPages, String originalChapter, String originalVolume, float parsedChapter, float parsedVolume, String scanlationGroup, long size, String title) {
        this.id = id;
        this.isHQ = isHQ;
        this.mangaId = mangaId;
        this.numPages = numPages;
        this.originalChapter = originalChapter;
        this.originalVolume = originalVolume;
        this.parsedChapter = parsedChapter;
        this.parsedVolume = parsedVolume;
        this.scanlationGroup = scanlationGroup;
        this.size = size;
        this.title = title;
    }

    private static float Parse(String s) {
        if (s == null)
            return Float.POSITIVE_INFINITY;

        NumberFormat df = NumberFormat.getInstance(Locale.ROOT);
        try {
            Number parsed = df.parse(s);
            if (parsed == null) return Float.POSITIVE_INFINITY;

            return parsed.floatValue();
        } catch (ParseException e) {
            return Float.POSITIVE_INFINITY;
        }
    }
}
