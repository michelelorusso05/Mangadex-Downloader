package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Objects;

public class ChapterUtilities {
    public static final int OPTION_ALLOWDUPLICATE = 0b00000001;
    public static final int OPTION_REMOVEEXTERNAL = 0b00000010;

    @NonNull
    private static String formatTitle(Context ctx, String title) {
        return (title == null || title.isEmpty()) ? ctx.getString(R.string.noName).concat(" ") : title;
    }
    @NonNull
    private static String formatChapter(String chapter) {
        return (chapter == null) ? "" : chapter.concat(" - ");
    }
    public static void formatChapterList(Context ctx, @NonNull ArrayList<Chapter> mangaChapters, int options) {
        formatChapterList(ctx, mangaChapters, options, null);
    }

    public static void formatChapterList(Context ctx, @NonNull ArrayList<Chapter> mangaChapters, int options, @Nullable String keepThisIDforMe) {
        Chapter previous;
        Chapter current = null;

        boolean allowDuplicate = (options & OPTION_ALLOWDUPLICATE) == 1;
        boolean hideExternal = ((options >> 1) & OPTION_ALLOWDUPLICATE) == 1;

        int keepChapterAtIndex = -1;

        for (ListIterator<Chapter> iterator = mangaChapters.listIterator(); iterator.hasNext();) {
            previous = current;
            int currentIndex = iterator.nextIndex();
            current = iterator.next();

            // Format title
            String currentChapter = current.getAttributes().getChapter();

            if (hideExternal && current.getAttributes().getExternalUrl() != null)
                iterator.remove();
                // Add chapter if it's the first one in the list, if allowDuplicate is set to true, if currentChapter is null (it means that the current chapter is a oneshot) or if it's different from the last one
            else if (currentChapter == null || allowDuplicate || previous == null
                    || current.getId().equals(keepThisIDforMe)
                    || !currentChapter.equals(previous.getAttributes().getChapter())) {

                if (current.getId().equals(keepThisIDforMe))
                    keepChapterAtIndex = currentIndex - 1;
                String currentTitle = formatTitle(ctx, current.getAttributes().getTitle());
                String fName = formatChapter(currentChapter) + currentTitle;
                current.getAttributes().setFormattedName(fName);
                for (Relationship r : current.getRelationships()) {
                    if (r.getType().equals("scanlation_group")) {
                        current.getAttributes().setScanlationGroupString(r.getAttributes().get("name").textValue());
                        break;
                    }
                }
                if (current.getAttributes().getScanlationGroupString() == null)
                    current.getAttributes().setScanlationGroupString("-");
            }
            else
                iterator.remove();
        }
        if (keepThisIDforMe != null) {
            if (keepChapterAtIndex == -1) {
                Log.w("Chapter filtering", "Couldn't keep chapter with ID " + keepThisIDforMe + ": No such chapter was found");
            }
            else {
                while (Objects.equals(mangaChapters.get(keepChapterAtIndex).getAttributes().getChapter(),
                        mangaChapters.get(keepChapterAtIndex + 1).getAttributes().getChapter()) && keepChapterAtIndex >= 0) {
                    mangaChapters.remove(keepChapterAtIndex);
                    keepChapterAtIndex--;
                }
            }
        }
    }
}
