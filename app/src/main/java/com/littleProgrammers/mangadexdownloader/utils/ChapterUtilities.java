package com.littleProgrammers.mangadexdownloader.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.littleProgrammers.mangadexdownloader.R;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

public class ChapterUtilities {
    @NonNull
    private static String FormatTitle(Context ctx, String title, String placeholder) {
        if (title == null || title.isEmpty()) {
            if (placeholder == null || placeholder.isEmpty())
                return ctx.getString(R.string.chapter_oneshot);
            else
                return ctx.getString(R.string.noName, placeholder).concat(" ");
        }
        else
            return title;
    }
    @NonNull
    private static String FormatChapter(String chapter) {
        return (chapter == null) ? "" : chapter.concat(" - ");
    }

    public static void FormatChapterList(@NonNull Context ctx, @NonNull ArrayList<Chapter> mangaChapters, @NonNull FormattingOptions options) {
        // Remove external chapters
        for (ListIterator<Chapter> iterator = mangaChapters.listIterator(); iterator.hasNext();) {
            Chapter current = iterator.next();
            if (options.hideExternal && current.getAttributes().getExternalUrl() != null) iterator.remove();
            else SetChapterNameAndGroup(ctx, current);
        }
        if (options.allowDuplicate || mangaChapters.isEmpty()) return;

        Chapter targetChapter = FindChapterWithID(mangaChapters, options.keepThisForMe);
        if (targetChapter != null)
            SetChapterNameAndGroup(ctx, targetChapter);

        ArrayList<Chapter> filteredChapters = new ArrayList<>();
        ArrayList<Chapter> workingList = new ArrayList<>(Collections.singletonList(mangaChapters.get(0)));

        for (int i = 1; i < mangaChapters.size(); i++) {
            if (!AreChaptersEqual(mangaChapters.get(i - 1), mangaChapters.get(i))) {
                filteredChapters.add(ProcessWorkingList(workingList, targetChapter));
            }
            workingList.add(mangaChapters.get(i));
        }

        filteredChapters.add(ProcessWorkingList(workingList, targetChapter));

        mangaChapters.clear();
        mangaChapters.addAll(filteredChapters);
    }
    private static Chapter ProcessWorkingList(@NonNull ArrayList<Chapter> wList, @Nullable Chapter targetChapter) {
        Chapter toAdd = null;
        if (targetChapter == null)
            toAdd = wList.get(0);
        else {
            for (Chapter c : wList) {
                if (c.getId().equals(targetChapter.getId())) {
                    toAdd = c;
                    break;
                }
                if (c.getAttributes().getScanlationGroupID().equals(targetChapter.getAttributes().getScanlationGroupID())) {
                    toAdd = c;
                }
            }
            if (toAdd == null) toAdd = wList.get(0);
        }

        wList.clear();
        return toAdd;
    }
    private static boolean AreChaptersEqual(@NonNull Chapter c1, @NonNull Chapter c2) {
        if (c1.getAttributes().getChapter() == null || c2.getAttributes().getChapter() == null) return false;
        return c1.getAttributes().getChapter().equals(c2.getAttributes().getChapter());
    }

    private static void SetChapterNameAndGroup(@NonNull Context ctx, @NonNull Chapter current) {
        String currentChapter = current.getAttributes().getChapter();
        String currentTitle = FormatTitle(ctx, current.getAttributes().getTitle(), currentChapter);

        String fName = FormatChapter(currentChapter) + currentTitle;
        current.getAttributes().setFormattedName(fName);
        for (Relationship r : current.getRelationships()) {
            if (r.getType().equals("scanlation_group")) {
                current.getAttributes().setScanlationGroupID(r.getId());
                current.getAttributes().setScanlationGroupString(r.getAttributes().get("name").textValue());
                break;
            }
        }
        if (current.getAttributes().getScanlationGroupString() == null) {
            current.getAttributes().setScanlationGroupID("012345678901234567890123456789012345");
            current.getAttributes().setScanlationGroupString(ctx.getString(R.string.group_null));
        }
    }

    @Nullable
    public static Chapter FindChapterWithID(@NonNull ArrayList<Chapter> chapterList, @Nullable String id) {
        if (id == null) return null;
        for (Chapter c : chapterList) {
            if (id.equals(c.getId())) return c;
        }
        return null;
    }

    public static class FormattingOptions {
        public boolean allowDuplicate;
        public boolean hideExternal;
        public String keepThisForMe;
        public FormattingOptions(boolean allowDuplicate, boolean hideExternal, String keepThisForMe) {
            this.allowDuplicate = allowDuplicate;
            this.hideExternal = hideExternal;
            this.keepThisForMe = keepThisForMe;
        }
    }
}
