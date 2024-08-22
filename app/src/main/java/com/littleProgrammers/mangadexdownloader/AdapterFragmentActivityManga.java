package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.littleProgrammers.mangadexdownloader.utils.ApiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class AdapterFragmentActivityManga extends FragmentStateAdapter {
    public static final int[] tabIconResIDs = {
            R.drawable.icon_description,
            R.drawable.icon_chapter_list,
            R.drawable.icon_related
    };
    public static final int[] tabLabelResIDs = {
            R.string.sectionOverview,
            R.string.sectionChapters,
            R.string.sectionRelated
    };
    final Context context;
    final Manga manga;
    FragmentMangaDescription fragmentMangaDescription;
    FragmentMangaChapters fragmentMangaChapters;
    public AdapterFragmentActivityManga(FragmentActivity ctx, Manga m) {
        super(ctx);
        context = ctx;
        manga = m;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position)
        {
            case 0: {
                fragmentMangaDescription = new FragmentMangaDescription();
                Bundle args = new Bundle();
                args.putParcelable("Manga", manga);
                args.putBoolean("AreChaptersAvailableHint", AreChaptersAvailable());
                fragmentMangaDescription.setArguments(args);
                return fragmentMangaDescription;
            }
            case 1: {
                fragmentMangaChapters = new FragmentMangaChapters();
                Bundle args = new Bundle();
                args.putString("MangaID", manga.getId());
                args.putString("MangaTitle", ApiUtils.GetMangaTitleString(manga));
                args.putBoolean("AreChaptersAvailableHint", AreChaptersAvailable());
                fragmentMangaChapters.setArguments(args);
                return fragmentMangaChapters;
            }
            case 2: {
                FragmentMangaRelated fragment = new FragmentMangaRelated();
                Bundle args = new Bundle();

                ArrayList<Relationship> relatedMangas =
                        Arrays.stream(manga.getRelationships())
                        .filter(relationship -> "manga".equals(relationship.getType()))
                        .collect(Collectors.toCollection(ArrayList::new));
                
                args.putParcelableArrayList("relatedMangas", relatedMangas);

                fragment.setArguments(args);
                return fragment;
            }
        }

        throw new IllegalStateException("There are only three fragments in this ViewPager2.");
    }

    private boolean AreChaptersAvailable() {
        Set<String> languages = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("languagePreference", Set.of("en"));

        for (String lang : languages) {
            if (ApiUtils.IsMangaLanguageAvailable(manga, lang))
                return true;
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
