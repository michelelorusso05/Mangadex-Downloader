package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.transition.ChangeImageTransform;
import android.transition.Explode;
import android.transition.Fade;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity
{
    // UI elements
    RecyclerView recyclerView;
    EditText searchBar;
    TextView status;
    ProgressBar pBar;

    ImageButton searchButton, randomButton, favouriteButton;
    View controlsContainer;
    ImageButton nextButton, previousButton;
    private int searchOffset = 0;

    DNSClient client;
    private ObjectMapper mapper;

    private Set<String> customIDs;

    @Override
    protected void onResume() {
        super.onResume();
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0)
            SetStatus(StatusType.BEGIN);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("cat", false))
            findViewById(R.id.catWarning).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        SplashScreen.Companion.installSplashScreen(this);
        setTheme(R.style.Theme_MangadexDownloader_NoActionBar);

        super.onCreate(savedInstanceState);

        client = new DNSClient(DNSClient.PresetDNS.CLOUDFLARE);

        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        Fade fade = new Fade();

        getWindow().setSharedElementsUseOverlay(false);

        // Set an enter transition
        getWindow().setEnterTransition(null);
        // Set an exit transition
        getWindow().setExitTransition(fade);
        getWindow().setSharedElementExitTransition(fade);

        setContentView(R.layout.activity_search);

        Toolbar t = findViewById(R.id.home_toolbar);
        setSupportActionBar(t);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        recyclerView = findViewById(R.id.results);
        searchBar = findViewById(R.id.searchBar);
        status = findViewById(R.id.status);
        pBar = findViewById(R.id.progressBar);
        searchButton = findViewById(R.id.searchButton);
        randomButton = findViewById(R.id.randomButton);
        favouriteButton = findViewById(R.id.favouriteButton);
        nextButton = findViewById(R.id.nextButton);
        previousButton = findViewById(R.id.previousButton);
        controlsContainer = findViewById(R.id.controlsContainer);

        searchButton.setOnClickListener((View v) -> {
            controlsContainer.setVisibility(View.INVISIBLE);
            searchOffset = 0;
            getResults();
        });
        randomButton.setOnClickListener(this::getRandomManga);
        favouriteButton.setOnClickListener((View v) -> {
            controlsContainer.setVisibility(View.INVISIBLE);
            searchOffset = 0;
            getResults(FavouriteManager.GetFavouritesIDs(this));
        });
        nextButton.setOnClickListener(v1 -> QueryNext());
        previousButton.setOnClickListener(v1 -> QueryPrevious());

        recyclerView.setAdapter(new MangaAdapter(this));
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int columns = landscape ? 4 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(SearchActivity.this, columns));

        SetStatus(StatusType.BEGIN);

        if (client.GetStatus() != 0) Toast.makeText(getApplicationContext(), R.string.DNSNotFound, Toast.LENGTH_SHORT).show();
        searchBar.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                searchOffset = 0;
                getResults();
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(searchBar.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return true;
            }
            return false;
        });

        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        if (savedInstanceState != null) {
            String savedQuery = savedInstanceState.getString("searchQuery", "");
            if (savedQuery.equals("__fav")) {
                controlsContainer.setVisibility(View.INVISIBLE);
                searchOffset = 0;
                getResults(FavouriteManager.GetFavouritesIDs(this));
            }
            else if (savedQuery.equals("__trn")) {
                controlsContainer.setVisibility(View.INVISIBLE);
                searchBar.setText("");
                getResults(null);
            }
            else if (savedQuery.length() != 0) {
                searchBar.setText(savedQuery);
                getResults();
            }
        }
        else {
            String startPage = PreferenceManager.getDefaultSharedPreferences(this).getString("startupView", "__mty");
            switch (startPage) {
                case "__mty":
                    break;
                case "__trn":
                    getResults(null);
                    break;
                case "__fav":
                    getResults(FavouriteManager.GetFavouritesIDs(this));
                    break;
            }
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("cat", false))
            findViewById(R.id.catWarning).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (customIDs != null)
            outState.putString("searchQuery", "__fav");
        else
            outState.putString("searchQuery", searchBar.getText().toString());
        outState.putInt("searchOffset", searchOffset);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.search_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("extraParams", searchBar.getText().toString().trim());
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_downloadedfiles) {
            startActivity(new Intent(this, DownloadedFiles.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getResults() { getResults(null); }
    public void getResults(@Nullable final Set<String> customList) {
        customIDs = customList;

        if (customList != null && customList.size() == 0) {
            SetStatus(StatusType.FAVOURITE_EMPTY);
            return;
        }

        searchButton.setEnabled(false);
        favouriteButton.setEnabled(false);

        StringBuilder urlString = new StringBuilder("https://api.mangadex.org/manga?&limit=20&offset=" + searchOffset + "&includes[]=cover_art&includes[]=author");
        if (customIDs == null)
            urlString.append("&title=").append(searchBar.getText().toString().trim());
        else {
            urlString.append("&order%5Btitle%5D=asc");
            for (String id : customIDs)
                urlString.append("&ids[]=").append(id);
        }

        status.setText(R.string.searchLoading);
        SetStatus(StatusType.SEARCHING);

        client.HttpRequestAsync(urlString.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                Log.d("failURL", call.request().url().toString());
                SearchActivity.this.runOnUiThread(() -> {
                    status.setText(getString(R.string.errNoConnection));
                    SetStatus(StatusType.NAY_RESULTS);
                    searchButton.setEnabled(true);
                    favouriteButton.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream bodyString;
                bodyString = Objects.requireNonNull(response.body()).byteStream();

                MangaResults _mResults;
                try {
                    _mResults = mapper.readValue(bodyString, MangaResults.class);
                } catch (JsonParseException | JsonMappingException e) {
                    runOnUiThread(() -> {
                        status.setText(getString(R.string.serverManteinance));
                        SetStatus(StatusType.NAY_RESULTS);
                        searchButton.setEnabled(true);
                        favouriteButton.setEnabled(true);
                    });
                    return;
                }
                final MangaResults mResults = _mResults;

                runOnUiThread(() -> {
                    // Update button state
                    previousButton.setEnabled(mResults.getOffset() > 0);
                    nextButton.setEnabled(mResults.getTotal() > mResults.getOffset() + mResults.getData().length);
                });

                for (Manga manga : mResults.getData()) {
                    for (Relationship relationship : manga.getRelationships()) {
                        if (relationship.getType().equals("author")) {
                            String author = (relationship.getAttributes() != null) ? relationship.getAttributes().get("name").textValue() : "-";
                            if (manga.getAttributes().getAuthorString() == null)
                                manga.getAttributes().setAuthorString(author);
                            else {
                                String a = manga.getAttributes().getAuthorString()
                                        .concat(", ")
                                        .concat(author);
                                manga.getAttributes().setAuthorString(a);
                            }
                        }
                        else if (relationship.getType().equals("cover_art")) {
                            String coverUrl = relationship.getAttributes().get("fileName").textValue();
                            manga.getAttributes().setCoverUrl(coverUrl);
                        }
                    }
                }
                runOnUiThread(() -> {
                    int resultsLength = mResults.getData().length;
                    status.setText(R.string.searchLoading);
                    if (resultsLength == 0) {
                        SetStatus(StatusType.NAY_RESULTS);
                        status.setText(getString(R.string.searchFoundEmpty));
                    }
                    else {
                        SetStatus(StatusType.YAY_RESULTS);
                        status.setText(getString(R.string.searchFound, mResults.getOffset() + 1, mResults.getOffset() + resultsLength, mResults.getTotal()));
                    }
                    MangaAdapter adapter = new MangaAdapter(SearchActivity.this, mResults.getData());
                    adapter.setHasStableIds(true);
                    recyclerView.setAdapter(adapter);

                    searchButton.setEnabled(true);
                    favouriteButton.setEnabled(true);
                });
                response.close();
            }
        });
    }
    public void QueryNext() {
        searchOffset += 20;
        getResults(customIDs);
        NestedScrollView v = findViewById(R.id.scrollView);
        v.smoothScrollTo(0, 0);
    }
    public void QueryPrevious() {
        searchOffset -= 20;
        getResults(customIDs);
        NestedScrollView v = findViewById(R.id.scrollView);
        v.smoothScrollTo(0, 0);
    }

    public void getRandomManga(View view) {
        TextView status = findViewById(R.id.status);
        randomButton.setEnabled(false);

        String urlString = "https://api.mangadex.org/manga/random?includes[]=author&includes[]=cover_art";

        status.setText(R.string.searchLoading);
        SetStatus(StatusType.SEARCHING);

        client.HttpRequestAsync(urlString, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                SearchActivity.this.runOnUiThread(() -> {
                    status.setText(getString(R.string.errNoConnection));
                    SetStatus(StatusType.NAY_RESULTS);
                    randomButton.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                MangaResults mResults = mapper.readValue(Objects.requireNonNull(response.body()).string(), MangaResults.class);

                if (mResults.getResult().equals("error")) {
                    SearchActivity.this.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), R.string.errServerFailed, Toast.LENGTH_SHORT).show();
                        status.setText(R.string.searchDefault);
                        SetStatus(StatusType.BEGIN);
                        randomButton.setEnabled(true);
                    });
                    return;
                }

                for (Relationship relationship : mResults.getData()[0].getRelationships()) {
                    if (relationship.getType().equals("author")) {
                        String author = relationship.getAttributes().get("name").textValue();
                        mResults.getData()[0].getAttributes().setAuthorString(author);
                    }
                    else if (relationship.getType().equals("cover_art")) {
                        String coverUrl = relationship.getAttributes().get("fileName").textValue();
                        mResults.getData()[0].getAttributes().setCoverUrl(coverUrl);
                    }
                }

                SearchActivity.this.runOnUiThread(() -> {
                    try {
                        StaticData.sharedCover = null;
                        Intent intent = new Intent(getApplicationContext(), ChapterDownloaderActivity.class);
                        intent.putExtra("MangaData", mapper.writeValueAsString(mResults.getData()[0]));
                        startActivity(intent);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    status.setText(R.string.searchDefault);
                    SetStatus(StatusType.BEGIN);

                    randomButton.setEnabled(true);
                });

            }
        });
    }

    public enum StatusType {
        BEGIN,
        NAY_RESULTS,
        YAY_RESULTS,
        SEARCHING,
        FAVOURITE_EMPTY
    }

    public void SetStatus(StatusType _status) {
        switch (_status) {
            case BEGIN:
            case NAY_RESULTS:
            case FAVOURITE_EMPTY:
                pBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.INVISIBLE);
                controlsContainer.setVisibility(View.INVISIBLE);
                break;
            case YAY_RESULTS:
                recyclerView.setVisibility(View.VISIBLE);
                pBar.setVisibility(View.GONE);
                controlsContainer.setVisibility(View.VISIBLE);
                break;
            case SEARCHING:
                pBar.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.INVISIBLE);
                nextButton.setEnabled(false);
                previousButton.setEnabled(false);
                break;
        }
    }
}