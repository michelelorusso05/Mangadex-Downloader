package com.littleProgrammers.mangadexdownloader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.AtHomeResults;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class OnlineReaderActivity extends ReaderActivity {
    DNSClient client;

    String mangaID;
    String[] chapterNames;
    String[] chapterIDs;
    ObjectMapper mapper;

    Spinner chapterSelection;
    ImageButton chapterNext, chapterPrevious;

    boolean bookmarkingEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chapterPrevious = findViewById(R.id.previousChapterButton);
        chapterNext = findViewById(R.id.nextChapterButton);
        chapterSelection = findViewById(R.id.chapterSelection);

        chapterPrevious.setOnClickListener(this::ChapterPrevious);
        chapterNext.setOnClickListener(this::ChapterNext);

        findViewById(R.id.chapterNavigation).setVisibility(View.VISIBLE);

        client = new DNSClient(DNSClient.PresetDNS.GOOGLE, this, true, 5);
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        chapterNames = getIntent().getStringArrayExtra("chapterNames");
        chapterIDs = getIntent().getStringArrayExtra("chapterIDs");
        mangaID = getIntent().getStringExtra("mangaID");

        bookmarkingEnabled = FavouriteManager.IsFavourite(this, mangaID);

        chapterSelection.setAdapter(new ChapterSelectionAdapter(this, chapterNames));
        chapterSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chapterNext.setEnabled(position < chapterSelection.getCount() - 1);
                chapterPrevious.setEnabled(position > 0);
                client.HttpRequestAsync("https://api.mangadex.org/at-home/server/" + chapterIDs[position], new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        AtHomeResults hResults = mapper.readValue(Objects.requireNonNull(response.body()).string(), AtHomeResults.class);

                        String _baseUrl;
                        String[] _images;

                        boolean HQ = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dataSaver", false);
                        if (HQ) {
                            _baseUrl = hResults.getBaseUrl() + "/data/";
                            _images = hResults.getChapter().getData();
                        }
                        else {
                            _baseUrl = hResults.getBaseUrl() + "/data-saver/";
                            _images = hResults.getChapter().getDataSaver();
                        }

                        _baseUrl += hResults.getChapter().getHash();
                        baseUrl = _baseUrl;
                        urls = _images;
                        runOnUiThread(() -> {
                            GeneratePageSelectionSpinnerAdapter();
                            progress.setSelection(0);
                        });
                        if (bookmarkingEnabled) {
                            int indexToBookmark = (position == chapterIDs.length - 1) ? position : position + 1;
                            FavouriteManager.SetBookmarkForFavourite(OnlineReaderActivity.this, mangaID, chapterIDs[indexToBookmark]);
                        }
                    }
                });
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String targetChapter = getIntent().getStringExtra("targetChapter");
        if (targetChapter == null)
            chapterSelection.setSelection(0);
        else {
            for (int i = 0; i < chapterIDs.length; i++) {
                if (chapterIDs[i].equals(targetChapter)) {
                    chapterSelection.setSelection(i);
                    break;
                }
            }
        }

        GeneratePageSelectionSpinnerAdapter();
        progress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                turnPage(position * pageStep());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (savedInstanceState == null) {
            progress.setSelection(0);
            FolderUtilities.DeleteFolderContents(getExternalCacheDir());
        }
    }

    public void turnPage(int index) {
        if (index >= urls.length) return;

        display.setVisibility(View.INVISIBLE);
        pBar.setVisibility(View.VISIBLE);
        previous.setEnabled(index > 0);
        first.setEnabled(previous.isEnabled());
        next.setEnabled(index < urls.length - pageStep());
        last.setEnabled(next.isEnabled());

        client.CancelAllPendingRequests();
        if (!landscape) {
            client.GetImageBitmapAsync(baseUrl + "/" + urls[index], bm -> runOnUiThread(() -> {
                display.setImageBitmap(bm);
                display.setVisibility(View.VISIBLE);
                pBar.setVisibility(View.GONE);
            }), opt);
            // Preload
            if (index < urls.length - 1)
                PreloadImage(baseUrl + "/" + urls[index + 1]);
        }
        else {
            final boolean[] isWorkDone = new boolean[1];
            final Bitmap[] images = new Bitmap[2];
            if (index < urls.length - 1) {
                client.GetImageBitmapAsync(baseUrl + "/" + urls[index + 1], bm -> {
                    images[1] = bm;
                    if (isWorkDone[0]) BitmapRetrieveDone(images[0], images[1]);
                    else isWorkDone[0] = true;
                }, opt);
            }
            else {
                isWorkDone[0] = true;
                images[1] = null;
            }
            client.GetImageBitmapAsync(baseUrl + "/" + urls[index], bm -> {
                images[0] = bm;
                if (isWorkDone[0]) BitmapRetrieveDone(images[0], images[1]);
                else isWorkDone[0] = true;
            }, opt);

            // Preload
            if (index < urls.length - 2)
                PreloadImage(baseUrl + "/" + urls[index + 2]);
            if (index < urls.length - 3)
                PreloadImage(baseUrl + "/" + urls[index + 3]);
        }
    }

    private void PreloadImage(String url) {
        client.HttpRequestAsync(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("Preloading", "Preloading of " + url + " failed");
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    Objects.requireNonNull(response.body()).bytes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.close();
            }
        });
    }

    public void ChapterNext(View v) {
        if (chapterSelection.getSelectedItemPosition() < chapterSelection.getCount() - 1)
            chapterSelection.setSelection(chapterSelection.getSelectedItemPosition() + 1);
    }
    public void ChapterPrevious(View v) {
        if (chapterSelection.getSelectedItemPosition() > 0)
            chapterSelection.setSelection(chapterSelection.getSelectedItemPosition() - 1);
    }
}