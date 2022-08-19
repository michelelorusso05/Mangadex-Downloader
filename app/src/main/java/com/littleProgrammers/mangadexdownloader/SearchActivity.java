package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

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

    View emptyView;
    ImageView emptyViewImage;
    TextView emptyViewDescription;

    DNSClient client = new DNSClient(DNSClient.PresetDNS.GOOGLE);
    private ObjectMapper mapper;

    @Override
    protected void onResume() {
        super.onResume();
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0)
            SetStatus(StatusType.BEGIN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        recyclerView = findViewById(R.id.results);
        searchBar = findViewById(R.id.searchBar);
        status = findViewById(R.id.status);
        pBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        emptyViewImage = findViewById(R.id.emptyViewImage);
        emptyViewDescription = findViewById(R.id.emptyViewText);

        recyclerView.setAdapter(new MangaAdapter(this));
        recyclerView.setLayoutManager(new GridLayoutManager(SearchActivity.this, 2));


        SetStatus(StatusType.BEGIN);

        if (client.GetStatus() != 0) Toast.makeText(getApplicationContext(), R.string.DNSNotFound, Toast.LENGTH_SHORT).show();
        searchBar.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                getResults(null);
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
            if (savedQuery.length() != 0) {
                searchBar.setText(savedQuery);
                getResults(null);
            }
        }

        warmupRequest();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("searchQuery", searchBar.getText().toString());
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

    public void warmupRequest() {
        client.AsyncHttpRequest("https://api.mangadex.org/", new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                Log.d("Smoke test", "Warmup request failed");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Log.d("Smoke test", "Warmup request succeded");
                response.close();
            }
        });
    }

    public void getResults(View view) {
        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setEnabled(false);

        String urlString = "https://api.mangadex.org/manga?title=" + searchBar.getText().toString().trim() + "&limit=20&includes[]=cover_art&includes[]=author";

        status.setText(R.string.searchLoading);
        SetStatus(StatusType.SEARCHING);

        client.AsyncHttpRequest(urlString, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                Log.d("failURL", call.request().url().toString());
                SearchActivity.this.runOnUiThread(() -> {
                    status.setText(getString(R.string.errNoConnection));
                    SetStatus(StatusType.NAY_RESULTS);
                    searchButton.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream bodyString;
                bodyString = Objects.requireNonNull(response.body()).byteStream();

                final MangaResults mResults = mapper.readValue(bodyString, MangaResults.class);

                for (Manga manga : mResults.getData()) {
                    for (Relationship relationship : manga.getRelationships()) {
                        if (relationship.getType().equals("author")) {
                            String author = relationship.getAttributes().get("name").textValue();
                            manga.getAttributes().setAuthorString(author);
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
                    if (resultsLength == 0) SetStatus(StatusType.NAY_RESULTS);
                    else SetStatus(StatusType.YAY_RESULTS);
                    status.setText(getString(R.string.searchFound, resultsLength));
                    MangaAdapter adapter = new MangaAdapter(SearchActivity.this, mResults.getData());
                    recyclerView.setAdapter(adapter);

                    ImageButton searchButton = findViewById(R.id.searchButton);
                    searchButton.setEnabled(true);
                });
                response.close();
            }
        });
    }

    public void getRandomManga(View view) {
        TextView status = findViewById(R.id.status);
        ImageButton randomButton = findViewById(R.id.randomButton);
        randomButton.setEnabled(false);

        String urlString = "https://api.mangadex.org/manga/random/?includes[]=author&includes[]=cover_art";

        status.setText(R.string.searchLoading);
        SetStatus(StatusType.SEARCHING);

        client.AsyncHttpRequest(urlString, new Callback() {
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
        SEARCHING
    }

    public void SetStatus(StatusType _status) {
        switch (_status) {
            case BEGIN:
                pBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.INVISIBLE);
                emptyViewImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.smiling));
                emptyViewDescription.setText(R.string.searchBegin);
                break;
            case NAY_RESULTS:
                pBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.INVISIBLE);
                emptyViewImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.distressed));
                emptyViewDescription.setText(R.string.emptyList);
                break;
            case YAY_RESULTS:
                recyclerView.setVisibility(View.VISIBLE);
                pBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                break;
            case SEARCHING:
                pBar.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.INVISIBLE);
                emptyView.setVisibility(View.GONE);
                break;
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("cat", false))
            emptyViewImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.cat));
    }
}