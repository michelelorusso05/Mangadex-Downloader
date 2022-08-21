package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public class FavouriteManager {
    public static void AddFavourite(@NonNull Context context, String id) {
        HashSet<String> savedIDs = GetFavourites(context);
        savedIDs.add(id);
        SaveFavourites(context, savedIDs);
    }
    public static void RemoveFavourite(@NonNull Context context, String id) {
        HashSet<String> savedIDs = GetFavourites(context);
        savedIDs.remove(id);
        SaveFavourites(context, savedIDs);
    }
    public static boolean IsFavourite(@NonNull Context context, String id) {
        Set<String> savedIDs = GetFavourites(context);
        return savedIDs.contains(id);
    }
    @NonNull
    public static HashSet<String> GetFavourites(@NonNull Context context) {
        return (HashSet<String>) context.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE)
                .getStringSet("Favourites", new HashSet<>());
    }
    public static void SaveFavourites(@NonNull Context context, @NonNull Set<String> favourites) {
        SharedPreferences.Editor editor = context.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE)
                .edit();
        editor.clear();
        editor.putStringSet("Favourites", favourites.isEmpty() ? null : favourites);
        editor.apply();
    }
}
