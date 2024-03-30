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


public class MangaViewActivityFragmentManager extends FragmentStateAdapter {
    public static final int[] tabIconResIDs = {
            R.drawable.description_24px,
            R.drawable.chapter_list,
            R.drawable.related
    };
    public static final int[] tabLabelResIDs = {
            R.string.sectionOverview,
            R.string.sectionChapters,
            R.string.sectionRelated
    };
    Context context;
    Manga manga;
    MangaDescriptionFragment mangaDescriptionFragment;
    MangaChaptersFragment mangaChaptersFragment;
    public MangaViewActivityFragmentManager(FragmentActivity ctx, Manga m) {
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
                mangaDescriptionFragment = new MangaDescriptionFragment();
                Bundle args = new Bundle();
                args.putSerializable("Manga", manga);
                args.putBoolean("AreChaptersAvailableHint", areChaptersAvailable());
                mangaDescriptionFragment.setArguments(args);
                return mangaDescriptionFragment;
            }
            case 1: {
                mangaChaptersFragment = new MangaChaptersFragment();
                Bundle args = new Bundle();
                args.putString("MangaID", manga.getId());
                args.putString("MangaTitle", manga.getAttributes().getTitleS());
                args.putBoolean("AreChaptersAvailableHint", areChaptersAvailable());
                mangaChaptersFragment.setArguments(args);
                return mangaChaptersFragment;
            }
            case 2: {
                MangaRelatedFragment fragment = new MangaRelatedFragment();
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
        Set<String> languages = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("languagePreference", null);
        assert languages != null;
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
