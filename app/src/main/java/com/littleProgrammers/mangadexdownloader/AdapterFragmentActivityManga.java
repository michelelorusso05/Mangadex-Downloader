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

import java.util.ArrayList;
import java.util.Set;


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
    Context context;
    Manga manga;
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
                args.putSerializable("Manga", manga);
                args.putBoolean("AreChaptersAvailableHint", areChaptersAvailable());
                fragmentMangaDescription.setArguments(args);
                return fragmentMangaDescription;
            }
            case 1: {
                fragmentMangaChapters = new FragmentMangaChapters();
                Bundle args = new Bundle();
                args.putString("MangaID", manga.getId());
                args.putString("MangaTitle", manga.getAttributes().getTitleS());
                args.putBoolean("AreChaptersAvailableHint", areChaptersAvailable());
                fragmentMangaChapters.setArguments(args);
                return fragmentMangaChapters;
            }
            case 2: {
                FragmentMangaRelated fragment = new FragmentMangaRelated();
                Bundle args = new Bundle();

                ArrayList<String> relatedMangaIDs = new ArrayList<>();
                ArrayList<String> relationTypes = new ArrayList<>();
                for (Relationship r : manga.getRelationships()) {
                    if ("manga".equals(r.getType())) {
                        relatedMangaIDs.add(r.getId());
                        relationTypes.add(r.getRelated());
                    }
                }

                args.putStringArrayList("relatedMangaIDs", relatedMangaIDs);
                args.putStringArrayList("relationTypes", relationTypes);

                fragment.setArguments(args);
                return fragment;
            }
        }

        throw new IllegalStateException("There are only three fragments in this ViewPager2.");
    }

    private boolean areChaptersAvailable() {
        Set<String> languages = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("languagePreference", Set.of("en"));

        for (String lang : languages) {
            if (manga.getAttributes().isLanguageAvailable(lang))
                return true;
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
