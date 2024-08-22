package com.littleProgrammers.mangadexdownloader;

import static com.littleProgrammers.mangadexdownloader.FragmentMangaResults.ComputeAndSetSpanCount;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.littleProgrammers.mangadexdownloader.utils.ApiUtils;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
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
    private GridLayoutManager manager;

    private TextView emoticonView;
    private TextView errorView;
    private View progressBar;

    ArrayList<Relationship> relatedMangasList;
    HashMap<String, Pair<Integer, String>> idToPositionAndType;
    boolean sorted;
    int searchOffset;

    RecyclerView.OnScrollListener onScrollListener;
    RecyclerViewPreloader<String> preloader;

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

        relatedMangasList = CompatUtils.GetParcelableArrayList(savedInstanceState, "relatedMangas", Relationship.class);
        sorted = savedInstanceState.getBoolean("sorted", false);

        if (!sorted) {
            String[] relatedTypes = getResources().getStringArray(R.array.manga_relationships_enum);
            HashMap<String, Integer> relatedTypeToValueSort = new HashMap<>(relatedTypes.length);
            for (int i = 0; i < relatedTypes.length; i++)
                relatedTypeToValueSort.put(relatedTypes[i], i);

            relatedMangasList.sort(Comparator.comparingInt(r -> {
                Integer i = relatedTypeToValueSort.get(r.getRelated());
                assert i != null;
                return i;
            }));
        }

        idToPositionAndType = new HashMap<>(relatedMangasList.size());

        for (int i = 0; i < relatedMangasList.size(); i++) {
            Relationship relationship = relatedMangasList.get(i);
            idToPositionAndType.put(relationship.getId(), Pair.create(i, relationship.getRelated()));
        }

        searchOffset = 0;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("relatedMangas", relatedMangasList);
        outState.putBoolean("sorted", sorted);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manga_item_list, container, false);

        recyclerView = view.findViewById(R.id.resultsList);
        recyclerView.setHasFixedSize(true);

        final boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        manager = new GridLayoutManager(context, ComputeAndSetSpanCount(context, landscape ? 0.75f : 1));

        ((FrameLayout) view.findViewById(R.id.container)).addView(new View(context) {
            @Override
            protected void onConfigurationChanged(Configuration newConfig) {
                super.onConfigurationChanged(newConfig);
                int col = ComputeAndSetSpanCount(context, landscape ? 0.75f : 1);
                if (manager.getSpanCount() != col)
                    manager.setSpanCount(col);
            }
        });
        recyclerView.setLayoutManager(manager);

        adapter = new AdapterRecyclerMangas(context);
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        manager.setSpanSizeLookup(adapter.GetSpanLookup(manager));

        emoticonView = view.findViewById(R.id.emoticonView);
        errorView = view.findViewById(R.id.errorDescription);
        progressBar = view.findViewById(R.id.progressBar);

        SetChapterRetrieveState(STATE_SEARCHING);

        model = new ViewModelProvider(this).get(ViewModelMangaList.class);

        if (!model.HasData())
            SearchRelatedMangas();

        model.getUiState().observe(getViewLifecycleOwner(), mangaListState -> {
            int state = mangaListState.getLastSearchState();
            if (state >= ViewModelMangaList.MangaListState.SEARCH_COMPLETED) {
                if (mangaListState.getMangas() == null || mangaListState.getMangas().isEmpty()) {
                    SetChapterRetrieveState(STATE_EMPTY);
                    return;
                }

                boolean hasMoreToLoad = state == ViewModelMangaList.MangaListState.SEARCH_COMPLETED_HAS_MORE;

                if (adapter.IsEmpty())
                    adapter.ReplaceMangas(mangaListState.getMangas(), mangaListState.getSectionMap(), hasMoreToLoad);
                else
                    adapter.AddMangas(mangaListState.getMangaDiff(), mangaListState.getSectionsDiff(), hasMoreToLoad);

                if (hasMoreToLoad && onScrollListener != null)
                    recyclerView.addOnScrollListener(onScrollListener);

                SetChapterRetrieveState(STATE_COMPLETE);
            }
            else if (state == ViewModelChapterList.ChapterListState.SEARCH_ERROR) {
                if (!mangaListState.getMangas().isEmpty())
                    adapter.SetError();
                else
                    SetChapterRetrieveState(STATE_FAIL);
            }
        });

        adapter.SetOnRetry(this::SearchRelatedMangas);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                v.setPadding(0, 0, 0, i.bottom);

                return insets;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (onScrollListener == null) {
            onScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    if (manager.findLastCompletelyVisibleItemPosition() >= adapter.getItemCount() - 5) {
                        searchOffset += 20;
                        SearchRelatedMangas();
                    }
                }
            };
        }
        if (adapter.hasInfiniteScrolling)
            recyclerView.addOnScrollListener(onScrollListener);

        if (preloader == null) {
            preloader = new RecyclerViewPreloader<>(context, adapter, adapter.getPreloadSizeProvider(), manager.getSpanCount() * 20);
        }

        recyclerView.addOnScrollListener(preloader);
    }

    @Override
    public void onPause() {
        super.onPause();
        recyclerView.clearOnScrollListeners();
    }

    private void SearchRelatedMangas() {
        recyclerView.removeOnScrollListener(onScrollListener);

        if (relatedMangasList.isEmpty()) {
            model.UpdateMangaList(null, ViewModelMangaList.MangaListState.SEARCH_COMPLETED);
            return;
        }

        StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga?&limit=20&includes[]=cover_art&includes[]=author&includes[]=artist");

        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("contentFilter", null);
        if (ratings != null) {
            for (String s : ratings) {
                urlString.append("&contentRating[]=").append(s);
            }
        }

        for (int i = searchOffset; i < Math.min(relatedMangasList.size(), searchOffset + 20); i++) {
            Relationship related = relatedMangasList.get(i);
            urlString.append("&ids[]=").append(related.getId());
        }

        client.HttpRequestAsync(urlString.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                context.runOnUiThread(() -> model.UpdateMangaList(null, ViewModelMangaList.MangaListState.SEARCH_ERROR));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream bodyString;
                bodyString = Objects.requireNonNull(response.body()).byteStream();

                MangaResults _mResults;
                try {
                    _mResults = StaticData.getMapper().readValue(bodyString, MangaResults.class);
                } catch (JsonParseException | JsonMappingException e) {
                    context.runOnUiThread(() -> model.UpdateMangaList(null, ViewModelMangaList.MangaListState.SEARCH_ERROR));
                    return;
                }
                final MangaResults mResults = _mResults;

                mResults.getData().sort((m1, m2) -> {
                    Pair<Integer, String> pos1 = idToPositionAndType.get(m1.getId());
                    Pair<Integer, String> pos2 = idToPositionAndType.get(m2.getId());
                    assert pos1 != null && pos2 != null;

                    return Integer.compare(pos1.first, pos2.first);
                });

                HashMap<Integer, String> positionToTitle = new HashMap<>();

                for (int i = 0; i < mResults.getData().size(); i++) {
                    Manga current = mResults.getData().get(i);
                    Pair<Integer, String> type = idToPositionAndType.get(current.getId());
                    assert type != null;

                    ApiUtils.SetMangaAttributes(current);

                    if (i == 0) {
                        if (searchOffset == 0 || !relatedMangasList.get(type.first - 1).getRelated().contentEquals(type.second))
                            positionToTitle.put(i, type.second);
                    }
                    else {
                        Manga previous = mResults.getData().get(i - 1);
                        Pair<Integer, String> typePrev = idToPositionAndType.get(previous.getId());
                        assert typePrev != null;

                        if (!type.second.contentEquals(typePrev.second))
                            positionToTitle.put(i, type.second);
                    }
                }

                model.UpdateMangaList(mResults.getData(), positionToTitle,
                        relatedMangasList.size() > searchOffset + 20 ? ViewModelMangaList.MangaListState.SEARCH_COMPLETED_HAS_MORE : ViewModelMangaList.MangaListState.SEARCH_COMPLETED);
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
                emoticonView.setText(CompatUtils.GetRandomStringFromStringArray(context, R.array.emoticons_angry));
                break;
            case STATE_EMPTY:
                recyclerView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                emoticonView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                errorView.setText(R.string.noRelatedMangas);
                emoticonView.setText(CompatUtils.GetRandomStringFromStringArray(context, R.array.emoticons_confused));
                break;
        }
    }
}