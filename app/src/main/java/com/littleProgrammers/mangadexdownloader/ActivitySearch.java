package com.littleProgrammers.mangadexdownloader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaResults;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ActivitySearch extends AppCompatActivity
{
    // UI elements
    RecyclerView recyclerView;
    AdapterRecyclerMangas adapter;

    FragmentSearchBar fragmentSearchBar;
    private int searchOffset = 0;
    private boolean isLoadingMore;
    private boolean canLoadMore;

    private String cachedType;
    private String cachedQuery;

    DNSClient client;


    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        fragmentSearchBar = new FragmentSearchBar();

        client = StaticData.getClient(this);

        recyclerView = findViewById(R.id.results);

        Toolbar t = findViewById(R.id.home_toolbar);
        setSupportActionBar(t);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        ViewCompat.setOnApplyWindowInsetsListener(t, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) t.getLayoutParams();
            params.setMargins(i.left, 0, i.right, 0);

            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                recyclerView.setPadding(recyclerView.getPaddingLeft(),
                                        recyclerView.getPaddingTop(),
                                        recyclerView.getPaddingRight(),
                                        i.bottom);

                return insets;
            }
        });

        adapter = new AdapterRecyclerMangas(ActivitySearch.this, viewID -> {
            try {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentSearchBar.isAdded()) {
                    fragmentManager.popBackStackImmediate(FragmentSearchBar.TAG, 0);

                } else {
                    fragmentManager.beginTransaction()
                            .replace(viewID, fragmentSearchBar, FragmentSearchBar.TAG)
                            .commit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        final int columns = landscape ? 2 : 1;
        GridLayoutManager manager = new GridLayoutManager(ActivitySearch.this, columns);
        recyclerView.setLayoutManager(manager);

        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (!landscape)
                    return 1;

                if (position == 0)
                    return 2;

                if (adapter.getItemViewType(position) == AdapterRecyclerMangas.ITEM_LOADING_PLACEHOLDER)
                    return 2;

                return 1;
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (isLoadingMore || !canLoadMore) return;

                if (manager.findLastCompletelyVisibleItemPosition() >= adapter.getItemCount() - 5) {
                    isLoadingMore = true;

                    searchOffset += 20;

                    if ("fav".contentEquals(cachedQuery))
                        GetResultFavorites(true);
                    else
                        GetResultsQuery(cachedQuery, true);
                }
            }
        });

        if (client.GetStatus() != 0)
            Snackbar.make(recyclerView, R.string.DNSNotFound, Snackbar.LENGTH_SHORT).show();

        /*
        if (savedInstanceState != null) {
            String savedQuery = savedInstanceState.getString("searchQuery", "");
            if (savedQuery.equals("__fav")) {
                searchOffset = 0;
                getResults(FavouriteManager.GetFavouritesIDs(this));
            }
            else if (savedQuery.equals("__trn") || savedQuery.isEmpty()) {
                searchBar.setText("");
                getResults(null);
            }
            else {
                searchBar.setText(savedQuery);
                getResults();
            }
        }
        else {
        }
         */
        String startPage = PreferenceManager.getDefaultSharedPreferences(this).getString("startupView", "__trn");
        switch (startPage) {
            case "__mty":
            case "__trn":
                GetResultsQuery("", false);
                break;
            case "__fav":
                GetResultFavorites(false);
                break;
        }

        getSupportFragmentManager().setFragmentResult("searchStart", Bundle.EMPTY);

        getSupportFragmentManager().setFragmentResultListener("search", this, (requestKey, result) -> {
            String type = result.getString("type", "query");
            switch (type) {
                case "query":
                    searchOffset = 0;
                    String query = result.getString("query", "");
                    GetResultsQuery(query, false);
                    break;
                case "fav":
                    searchOffset = 0;
                    GetResultFavorites(false);
                    break;
            }
        });
    }

    /*
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (customIDs != null)
            outState.putString("searchQuery", "__fav");
        else
            outState.putString("searchQuery", searchBar.getText().toString());
        outState.putInt("searchOffset", searchOffset);
        super.onSaveInstanceState(outState);
    }

     */

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.search_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, ActivitySettings.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_downloadedfiles) {
            startActivity(new Intent(this, ActivityDownloadedFiles.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void GetResultsQuery(String query, boolean append) {
        cachedType = "query";
        cachedQuery = query;

        StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga?&limit=20&offset=" + searchOffset + "&includes[]=cover_art&includes[]=author&includes[]=artist");

        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(ActivitySearch.this).getStringSet("contentFilter", null);
        if (ratings != null) {
            for (String s : ratings) {
                urlString.append("&contentRating[]=").append(s);
            }
        }

        urlString.append("&title=").append(query.replaceAll("[\\[\\]&=]+", ""));

        GetResults(urlString.toString(), append);
    }
    public void GetResultFavorites(boolean append) {
        cachedType = "fav";

        Set<String> fav = FavouriteManager.GetFavouritesIDs(this);

        StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga?&limit=20&offset=" + searchOffset + "&includes[]=cover_art&includes[]=author&includes[]=artist");

        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(ActivitySearch.this).getStringSet("contentFilter", null);
        if (ratings != null) {
            for (String s : ratings) {
                urlString.append("&contentRating[]=").append(s);
            }
        }
        urlString.append("&order%5Btitle%5D=asc");
        for (String id : fav)
            urlString.append("&ids[]=").append(id);

        GetResults(urlString.toString(), append);
    }

    private void GetResults(String url, boolean append) {

        client.HttpRequestAsync(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                ActivitySearch.this.runOnUiThread(() -> {
                    getSupportFragmentManager().setFragmentResult("searchEnd", Bundle.EMPTY);
                    Snackbar.make(recyclerView, R.string.errNoConnection, Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream bodyString;
                bodyString = Objects.requireNonNull(response.body()).byteStream();

                MangaResults _mResults;
                try {
                    _mResults = StaticData.getMapper().readValue(bodyString, MangaResults.class);
                } catch (JsonParseException | JsonMappingException e) {
                    runOnUiThread(() -> {
                        getSupportFragmentManager().setFragmentResult("searchEnd", Bundle.EMPTY);
                        Snackbar.make(recyclerView, R.string.errServerFailed, Snackbar.LENGTH_SHORT).show();
                    });
                    return;
                }

                final MangaResults mResults = _mResults;

                for (Manga manga : mResults.getData()) {
                    manga.autofillInformation();
                }

                runOnUiThread(() -> {
                    int resultsLength = mResults.getData().size();
                    canLoadMore = mResults.getTotal() > mResults.getOffset() + resultsLength;

                    if (append)
                        adapter.AddMangas(mResults.getData(), canLoadMore);
                    else
                        adapter.ReplaceMangas(mResults.getData(), canLoadMore);

                    getSupportFragmentManager().setFragmentResult("searchEnd", Bundle.EMPTY);

                    isLoadingMore = false;
                });
                response.close();
            }
        }, false);
    }

    /*
    public void getRandomManga(View view) {
        randomButton.setEnabled(false);

        StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga/random?includes[]=author&includes[]=cover_art");

        Set<String> ratings = new HashSet<>(4);
        PreferenceManager.getDefaultSharedPreferences(ActivitySearch.this).getStringSet("contentFilter", ratings);

        for (String s : ratings) {
            urlString.append("&contentRating[]=").append(s);
        }

        // status.setText(R.string.searchLoading);
        SetStatus(StatusType.SEARCHING);

        client.HttpRequestAsync(urlString.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                ActivitySearch.this.runOnUiThread(() -> {
                    // status.setText(getString(R.string.errNoConnection));
                    SetStatus(StatusType.NAY_RESULTS);
                    randomButton.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                MangaResults mResults = StaticData.getMapper().readValue(Objects.requireNonNull(response.body()).string(), MangaResults.class);

                if (mResults.getResult().equals("error")) {
                    ActivitySearch.this.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), R.string.errServerFailed, Toast.LENGTH_SHORT).show();
                        // status.setText(R.string.searchDefault);
                        SetStatus(StatusType.BEGIN);
                        randomButton.setEnabled(true);
                    });
                    return;
                }

                Manga randomManga = mResults.getData().get(0);
                randomManga.autofillInformation();

                ActivitySearch.this.runOnUiThread(() -> {
                    StaticData.sharedCover = null;
                    Intent intent = new Intent(getApplicationContext(), ActivityManga.class);
                    intent.putExtra("MangaData", randomManga);
                    startActivity(intent);
                    // status.setText(R.string.searchDefault);
                    SetStatus(StatusType.BEGIN);

                    randomButton.setEnabled(true);
                });

            }
        }, false);
    }

     */
}