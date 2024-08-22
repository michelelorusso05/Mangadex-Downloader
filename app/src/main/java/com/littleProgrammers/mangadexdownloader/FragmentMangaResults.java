package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.window.core.layout.WindowSizeClass;
import androidx.window.core.layout.WindowWidthSizeClass;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.android.material.snackbar.Snackbar;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaResults;
import com.littleProgrammers.mangadexdownloader.utils.ApiUtils;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.GlideApp;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public abstract class FragmentMangaResults extends Fragment {
    FragmentActivity context;

    RecyclerView recyclerView;
    AdapterRecyclerMangas adapter;
    GridLayoutManager manager;
    RecyclerView.OnScrollListener onScrollListener;
    RecyclerViewPreloader<String> preloader;
    protected ViewModelMangaList model;

    DNSClient client;

    protected int searchOffset = 0;

    private String lastQuery;
    private boolean lastAppend;

    protected final static int ROWS_PER_LOAD = 20;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();
        client = StaticData.getClient(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setHasFixedSize(true);

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                v.setPadding(i.left + CompatUtils.ConvertDpToPixel(8, context),
                        v.getPaddingTop(),
                        i.right + CompatUtils.ConvertDpToPixel(8, context),
                        v.getPaddingBottom());

                return insets;
            }
        });

        manager = new GridLayoutManager(context, ComputeAndSetSpanCount(context, 1));

        ((FrameLayout) view.findViewById(R.id.container)).addView(new View(context) {
            @Override
            protected void onConfigurationChanged(Configuration newConfig) {
                super.onConfigurationChanged(newConfig);
                int col = ComputeAndSetSpanCount(context, 1);
                if (manager.getSpanCount() != col)
                    manager.setSpanCount(col);
            }
        });

        recyclerView.setLayoutManager(manager);

        adapter = SetupAdapter();
        adapter.setHasStableIds(true);

        recyclerView.setAdapter(adapter);
        adapter.SetOnRetry(() -> GetResults(lastQuery, lastAppend));
        manager.setSpanSizeLookup(adapter.GetSpanLookup(manager));

        preloader =
                new RecyclerViewPreloader<>(
                        GlideApp.with(context.getBaseContext()), adapter, adapter.getPreloadSizeProvider(), manager.getSpanCount() * ROWS_PER_LOAD);

        recyclerView.addOnScrollListener(preloader);

        model = new ViewModelProvider(this).get(ViewModelMangaList.class);

        model.getUiState().observe(getViewLifecycleOwner(), mangaListState -> {
            if (adapter.IsEmpty() && model.HasData()) {
                // Retrieve cached query and append state
                lastQuery = mangaListState.getCachedQuery();
                lastAppend = !mangaListState.isNewSearch();
                searchOffset = mangaListState.getLastOffset();
            }

            int state = mangaListState.getLastSearchState();
            if (model.HasData()) {
                if (mangaListState.getMangas() == null || mangaListState.getMangas().isEmpty()) {
                    if (mangaListState.isNewSearch())
                        adapter.ClearMangas();
                    adapter.SetNoResults();
                }
                else {
                    boolean hasMoreToLoad = state == ViewModelMangaList.MangaListState.SEARCH_COMPLETED_HAS_MORE;

                    if (adapter.IsEmpty() || mangaListState.isNewSearch())
                        adapter.ReplaceMangas(mangaListState.getMangas(), mangaListState.getSectionMap(), hasMoreToLoad);
                    else
                        adapter.AddMangas(mangaListState.getMangaDiff(), mangaListState.getSectionsDiff(), hasMoreToLoad);

                    if (hasMoreToLoad && onScrollListener != null)
                        recyclerView.addOnScrollListener(onScrollListener);
                }
            }

            if (state == ViewModelChapterList.ChapterListState.SEARCH_ERROR) {
                if (mangaListState.isNewSearch())
                    adapter.ClearMangas();
                adapter.SetError();
            }

            OnSearchEnd();
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

                    if (manager.findLastCompletelyVisibleItemPosition() >= adapter.getItemCount() - manager.getSpanCount() * 4) {
                        searchOffset += manager.getSpanCount() * ROWS_PER_LOAD;

                        GetResults(lastQuery, true);
                    }
                }
            };
        }
        if (adapter.hasInfiniteScrolling || model.GetCurrentSearchState() == ViewModelMangaList.MangaListState.SEARCH_COMPLETED_HAS_MORE)
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

    protected String GetOffsetString(String initialUrl, int offset) {
        return initialUrl + "&offset=" + offset;
    }

    protected final void GetResults(String url, boolean append) {
        recyclerView.removeOnScrollListener(onScrollListener);

        lastQuery = url;
        lastAppend = append;

        url = GetOffsetString(url, searchOffset);

        client.HttpRequestAsync(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (append)
                    model.UpdateMangaList(null, ViewModelMangaList.MangaListState.SEARCH_ERROR);
                else
                    model.ReplaceMangaList(null, ViewModelMangaList.MangaListState.SEARCH_ERROR, lastQuery);

                /*
                context.runOnUiThread(() -> {
                    if (!append)
                        adapter.ClearMangas();

                    if (append)
                    adapter.SetError();
                    OnSearchEnd();
                });

                 */
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream bodyString;
                bodyString = Objects.requireNonNull(response.body()).byteStream();

                MangaResults _mResults;
                try {
                    _mResults = StaticData.getMapper().readValue(bodyString, MangaResults.class);
                } catch (JsonParseException | JsonMappingException e) {
                    e.printStackTrace();
                    context.runOnUiThread(() -> {
                        Snackbar.make(recyclerView, R.string.errServerFailed, Snackbar.LENGTH_SHORT).show();
                        OnSearchEnd();
                    });
                    return;
                }

                final MangaResults mResults = _mResults;

                for (Manga manga : mResults.getData()) {
                    ApiUtils.SetMangaAttributes(manga);
                }

                int resultsLength = mResults.getData().size();
                final boolean canLoadMore = mResults.getTotal() > mResults.getOffset() + resultsLength;

                if (append) {
                    model.UpdateMangaList(mResults.getData(),
                            canLoadMore ? ViewModelMangaList.MangaListState.SEARCH_COMPLETED_HAS_MORE : ViewModelMangaList.MangaListState.SEARCH_COMPLETED);
                }
                else {
                    model.ReplaceMangaList(mResults.getData(),
                            canLoadMore ? ViewModelMangaList.MangaListState.SEARCH_COMPLETED_HAS_MORE : ViewModelMangaList.MangaListState.SEARCH_COMPLETED,
                            lastQuery);
                }


                /*
                context.runOnUiThread(() -> {
                    int resultsLength = mResults.getData().size();

                    if (!append && resultsLength == 0) {
                        adapter.SetNoResults();
                    }
                    else {
                        canLoadMore = mResults.getTotal() > mResults.getOffset() + resultsLength;
                        if (canLoadMore && onScrollListener != null)
                            recyclerView.addOnScrollListener(onScrollListener);

                        if (append)
                            adapter.AddMangas(mResults.getData(), canLoadMore);
                        else
                            adapter.ReplaceMangas(mResults.getData(), canLoadMore);

                        isLoadingMore = false;
                    }

                    OnSearchEnd();
                });

                 */
                response.close();
            }
        }, false);
    }

    public static int ComputeAndSetSpanCount(Context context, float percentage) {
        int columns;

        WindowMetrics metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(context);

        int width = metrics.getBounds().width();
        float density = context.getResources().getDisplayMetrics().density;

        // Manual adjustment to make edge cases look good (e.g. 863dp is too close to the
        // lower limit of EXPANDED (840dp), and the three columns layout looks bad)
        final float offset = 90;
        WindowSizeClass windowSizeClass = WindowSizeClass.compute(((width / density) - offset) * percentage, 0);
        // COMPACT, MEDIUM, or EXPANDED
        WindowWidthSizeClass widthWindowSizeClass = windowSizeClass.getWindowWidthSizeClass();

        if (WindowWidthSizeClass.COMPACT.equals(widthWindowSizeClass))
            columns = 1;
        else if (WindowWidthSizeClass.MEDIUM.equals(widthWindowSizeClass))
            columns = 2;
        else if (WindowWidthSizeClass.EXPANDED.equals(widthWindowSizeClass))
            columns = 3;
        else {
            Log.w("WindowMetrics", "WindowWidthSizeClass didn't match anything (?!); falling back to one column.");
            columns = 1;
        }

        return columns;
    }

    protected abstract void OnSearchEnd();

    protected abstract AdapterRecyclerMangas SetupAdapter();

    public final RecyclerView GetRecyclerView() {
        return recyclerView;
    }

    public final GridLayoutManager GetManager() {
        return manager;
    }

    protected abstract int GetTag();
}
