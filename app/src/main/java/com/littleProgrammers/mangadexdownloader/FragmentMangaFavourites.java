package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;

import java.util.ArrayList;
import java.util.Set;

public class FragmentMangaFavourites extends FragmentMangaResults {
    FragmentFavouritesLabel favLabel;
    ArrayList<String> favIDs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();
        favLabel = new FragmentFavouritesLabel();

        client = StaticData.getClient(context);
    }

    @Override
    public void onStart() {
        super.onStart();

        Set<String> fav = FavouriteManager.GetFavouritesIDs(context);

        ArrayList<String> offloadList = new ArrayList<>(fav);

        boolean shouldUpdate = !model.HasData();

        if (favIDs != null && favIDs.size() == offloadList.size()) {
            long fp1 = 0;
            long fp2 = 0;

            for (String favID : favIDs) {
                fp1 += favID.hashCode();
            }

            for (String favID : offloadList) {
                fp2 += favID.hashCode();
            }

            if (fp1 == fp2)
                shouldUpdate = false;
        }

        if (shouldUpdate) {
            favIDs = new ArrayList<>(fav);
            GetResultFavorites(false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        recyclerView = view.findViewById(R.id.results);
        recyclerView.setId(GetTag());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getChildFragmentManager().setFragmentResult("searchStart", Bundle.EMPTY);
    }

    public void GetResultFavorites(boolean append) {
        if (favIDs.isEmpty()) {
            recyclerView.removeOnScrollListener(onScrollListener);
            adapter.SetNoFavs();
            getChildFragmentManager().setFragmentResult("searchEnd", Bundle.EMPTY);
            return;
        }

        StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga?&limit=20&includes[]=cover_art&includes[]=author&includes[]=artist");

        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("contentFilter", null);
        if (ratings != null) {
            for (String s : ratings) {
                urlString.append("&contentRating[]=").append(s);
            }
        }
        urlString.append("&order%5Btitle%5D=asc");

        searchOffset = 0;
        GetResults(urlString.toString(), append);
    }

    @Override
    protected String GetOffsetString(String initialUrl, int offset) {
        StringBuilder b = new StringBuilder(initialUrl);

        for (int i = offset; i < Math.min(favIDs.size(), offset + manager.getSpanCount() * ROWS_PER_LOAD); i++) {
            b.append("&ids[]=").append(favIDs.get(i));
        }

        return b.toString();
    }

    @Override
    protected void OnSearchEnd() {
        getChildFragmentManager().setFragmentResult("searchEnd", Bundle.EMPTY);
    }

    @Override
    protected AdapterRecyclerMangas SetupAdapter() {
        return new AdapterRecyclerMangas(context, viewID -> {
            try {
                FragmentManager fragmentManager = getChildFragmentManager();
                if (favLabel.isAdded()) {
                    fragmentManager.popBackStackImmediate(FragmentFavouritesLabel.TAG, 0);

                } else {
                    fragmentManager.beginTransaction()
                            .replace(viewID, favLabel, FragmentFavouritesLabel.TAG)
                            .commit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Math.abs((TAG + "_fragment").hashCode()), recyclerView);
    }

    public static final String TAG = FragmentMangaFavourites.class.getSimpleName();

    @Override
    protected int GetTag() {
        return Math.abs(TAG.hashCode());
    }
}
