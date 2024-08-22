package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.littleProgrammers.mangadexdownloader.utils.StaticData;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class FragmentMangaSearch extends FragmentMangaResults {
    FragmentSearchBar searchBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();
        searchBar = new FragmentSearchBar();

        client = StaticData.getClient(context);
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

        getChildFragmentManager().setFragmentResultListener("search", this, (requestKey, result) -> {
            searchOffset = 0;
            String query = result.getString("query", "");
            GetResultsQuery(query, false);
        });

        if (!model.HasData()) {
            String timeOffset = OffsetDateTime.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            timeOffset = timeOffset.substring(0, timeOffset.lastIndexOf('.'));
            try {
                timeOffset = URLEncoder.encode(timeOffset, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                Log.w("Encoding error", "UTF-8 not supported (?!), reverting to deprecated function");
                timeOffset = URLEncoder.encode(timeOffset);
            }
            String mostPopularQuery = "https://api.mangadex.org/manga?&limit=" + manager.getSpanCount() * ROWS_PER_LOAD + "&includes[]=cover_art&includes[]=artist&includes[]=author&order[followedCount]=desc&hasAvailableChapters=true&createdAtSince=" + timeOffset;
            GetResults(mostPopularQuery, false);
        }
    }

    public void GetResultsQuery(String query, boolean append) {StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga?&limit=" + manager.getSpanCount() * ROWS_PER_LOAD + "&offset=" + searchOffset + "&includes[]=cover_art&includes[]=author&includes[]=artist");
        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("contentFilter", null);
        if (ratings != null) {
            for (String s : ratings) {
                urlString.append("&contentRating[]=").append(s);
            }
        }

        urlString.append("&title=").append(query.replaceAll("[\\[\\]&=]+", ""));

        GetResults(urlString.toString(), append);
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
                if (searchBar.isAdded()) {
                    fragmentManager.popBackStackImmediate(FragmentSearchBar.TAG, 0);
                } else {
                    fragmentManager.beginTransaction()
                            .replace(viewID, searchBar, FragmentSearchBar.TAG)
                            .commit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Math.abs((FragmentSearchBar.TAG).hashCode()), recyclerView);
    }

    public static final String TAG = FragmentMangaSearch.class.getSimpleName();

    @Override
    protected int GetTag() {
        return Math.abs(TAG.hashCode());
    }
}
