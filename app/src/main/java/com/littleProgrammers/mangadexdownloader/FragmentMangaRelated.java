package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaResults;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FragmentMangaRelated extends Fragment {
    DNSClient client;
    Activity context;
    private RecyclerView recyclerView;
    private AdapterRecyclerMangas adapter;
    private TextView emoticonView;
    private TextView errorView;
    private View progressBar;

    HashMap<String, String> relatedMangas;
    ArrayList<String> relatedMangaIDs;
    ArrayList<String> relationTypes;
    ViewModelMangaList model;

    public FragmentMangaRelated() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();
        client = StaticData.getClient(context);

        if (savedInstanceState == null)
            savedInstanceState = getArguments();

        assert savedInstanceState != null;

        relatedMangas = new HashMap<>();

        relatedMangaIDs = savedInstanceState.getStringArrayList("relatedMangaIDs");
        relationTypes = savedInstanceState.getStringArrayList("relationTypes");

        if (relatedMangaIDs != null && relationTypes != null) {
            for (int i = 0; i < relatedMangaIDs.size(); i++) {
                relatedMangas.put(relatedMangaIDs.get(i), relationTypes.get(i));
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList("relatedMangaIDs", relatedMangaIDs);
        outState.putStringArrayList("relationTypes", relationTypes);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manga_item_list, container, false);

        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int columns = landscape ? 2 : 1;

        recyclerView = view.findViewById(R.id.resultsList);
        adapter = new AdapterRecyclerMangas(context);
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(context, columns));
        recyclerView.setHasFixedSize(true);

        emoticonView = view.findViewById(R.id.emoticonView);
        errorView = view.findViewById(R.id.errorDescription);
        progressBar = view.findViewById(R.id.progressBar);

        SetChapterRetrieveState(STATE_SEARCHING);

        model = new ViewModelProvider(this).get(ViewModelMangaList.class);

        if (model.getUiState().getValue() == null || model.getUiState().getValue().isSearchCompleted() != ViewModelChapterList.ChapterListState.SEARCH_COMPLETED)
            SearchRelatedMangas();

        model.getUiState().observe(getViewLifecycleOwner(), mangaListState -> {
            int state = mangaListState.isSearchCompleted();
            if (state == ViewModelChapterList.ChapterListState.SEARCH_COMPLETED) {
                if (mangaListState.getMangas() == null || mangaListState.getMangas().isEmpty()) {
                    SetChapterRetrieveState(STATE_EMPTY);
                    return;
                }
                adapter.AddMangas(mangaListState.getMangas(), mangaListState.getSectionMap(), false);

                GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
                assert manager != null;

                if (manager.getSpanCount() != 1) {
                    manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {
                            return adapter.getAdapterPositionToArrayMap().get(position).first == AdapterRecyclerMangas.ITEM_MANGA ? 1 : 2;
                        }
                    });
                }
                SetChapterRetrieveState(STATE_COMPLETE);
            }
            else if (state == ViewModelChapterList.ChapterListState.SEARCH_ERROR) {
                SetChapterRetrieveState(STATE_FAIL);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                v.setPadding(0, 0, 0, i.bottom);

                return insets;
            }
        });

        return view;
    }

    private void SearchRelatedMangas() {
        if (relatedMangaIDs.isEmpty()) {
            model.updateMangaList(null, ViewModelMangaList.MangaListState.SEARCH_COMPLETED);
            return;
        }

        StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga?&limit=20" + "&includes[]=cover_art&includes[]=author&includes[]=artist");

        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("contentFilter", null);
        if (ratings != null) {
            for (String s : ratings) {
                urlString.append("&contentRating[]=").append(s);
            }
        }

        for (String id : relatedMangas.keySet()) {
            urlString.append("&ids[]=").append(id);
        }

        client.HttpRequestAsync(urlString.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                context.runOnUiThread(() -> model.updateMangaList(null, ViewModelMangaList.MangaListState.SEARCH_ERROR));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream bodyString;
                bodyString = Objects.requireNonNull(response.body()).byteStream();

                MangaResults _mResults;
                try {
                    _mResults = StaticData.getMapper().readValue(bodyString, MangaResults.class);
                } catch (JsonParseException | JsonMappingException e) {
                    context.runOnUiThread(() -> model.updateMangaList(null, ViewModelMangaList.MangaListState.SEARCH_ERROR));
                    return;
                }
                final MangaResults mResults = _mResults;

                String[] stringArray = getResources().getStringArray(R.array.manga_relationships_enum);
                ArrayList<Manga> data = new ArrayList<>();

                HashMap<Integer, String> map = new HashMap<>();

                for (String s : getResources().getStringArray(R.array.manga_relationships_enum)) {
                    int index = data.size();
                    boolean mangaInserted = false;
                    for (Manga m : mResults.getData()) {
                        if (s.equals(relatedMangas.get(m.getId()))) {
                            mangaInserted = true;
                            m.autofillInformation();
                            data.add(m);
                        }
                    }
                    if (mangaInserted)
                        map.put(index, s);
                }

                context.runOnUiThread(() -> model.updateMangaList(data, map, ViewModelMangaList.MangaListState.SEARCH_COMPLETED));
            }
        }, false);
    }

    private final static int STATE_SEARCHING = 0;
    private final static int STATE_COMPLETE = 1;
    private final static int STATE_FAIL = 2;
    private final static int STATE_EMPTY = 3;
    private void SetChapterRetrieveState(int state) {
        switch (state) {
            case STATE_SEARCHING:
                recyclerView.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                emoticonView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case STATE_COMPLETE:
                recyclerView.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                emoticonView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                break;
            case STATE_FAIL:
                recyclerView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                emoticonView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                errorView.setText(R.string.errNoConnection);
                emoticonView.setText(CompatUtils.getRandomStringFromStringArray(context, R.array.emoticons_angry));
                break;
            case STATE_EMPTY:
                recyclerView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                emoticonView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                errorView.setText(R.string.noRelatedMangas);
                emoticonView.setText(CompatUtils.getRandomStringFromStringArray(context, R.array.emoticons_confused));
                break;
        }
    }
}