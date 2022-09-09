package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class FavouriteManager {
    public static void AddFavourite(@NonNull Context context, String id) {
        HashSet<String> savedIDs = GetFavourites(context);
        if (!Contains(savedIDs, id))
            savedIDs.add(id);
        SaveFavourites(context, savedIDs);
    }
    public static void RemoveFavourite(@NonNull Context context, String id) {
        HashSet<String> savedIDs = GetFavourites(context);
        savedIDs.remove(GetFromID(savedIDs, id));
        SaveFavourites(context, savedIDs);
    }
    public static boolean IsFavourite(@NonNull Context context, String id) {
        Set<String> savedIDs = GetFavourites(context);
        return Contains(savedIDs, id);
    }

    @Nullable
    public static Pair<java.lang.String, Boolean> GetBookmarkForFavourite(@NonNull Context context, String id) {
        HashSet<String> savedIDs = GetFavourites(context);
        String s = GetFromID(savedIDs, id);
        if (s != null && s.length() > 36) {
            return new Pair<>(s.substring(36, 72), s.length() > 72);
        }
        return null;
    }
    public static void SetBookmarkForFavourite(@NonNull Context context, String id, String bookmark, boolean queueNext) {
        HashSet<String> savedIDs = GetFavourites(context);
        String s = GetFromID(savedIDs, id);
        if (s != null) {
            savedIDs.remove(s);
            savedIDs.add(id.concat(bookmark).concat(queueNext ? "_" : ""));
            SaveFavourites(context, savedIDs);
        }
    }
    @NonNull
    public static HashSet<String> GetFavourites(@NonNull Context context) {
        return (HashSet<String>) context.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE)
                .getStringSet("Favourites", new HashSet<>());
    }
    @NonNull
    public static HashSet<String> GetFavouritesIDs(@NonNull Context context) {
        HashSet<String> savedFavourites = GetFavourites(context);
        HashSet<String> savedIDs = new HashSet<>();
        for (String s : savedFavourites)
            savedIDs.add(s.substring(0, 36));
        return savedIDs;
    }
    public static void SaveFavourites(@NonNull Context context, @NonNull Set<String> favourites) {
        SharedPreferences.Editor editor = context.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE)
                .edit();
        editor.clear();
        editor.putStringSet("Favourites", favourites.isEmpty() ? null : favourites);
        editor.apply();
    }
    private static boolean Contains(@NonNull Set<String> favourites, String id) {
        for (String s : favourites) {
            if (s.substring(0, 36).equals(id))
                return true;
        }
        return false;
    }
    @Nullable
    private static String GetFromID(@NonNull Set<String> favourites, String id) {
        for (String s : favourites) {
            if (s.substring(0, 36).equals(id))
                return s;
        }
        return null;
    }
}
