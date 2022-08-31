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
    int indexToBookmark;

    boolean setLastPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chapterPrevious = findViewById(R.id.previousChapterButton);
        chapterNext = findViewById(R.id.nextChapterButton);
        chapterSelection = findViewById(R.id.chapterSelection);

        chapterPrevious.setOnClickListener(this::ChapterPrevious);
        chapterNext.setOnClickListener(this::ChapterNext);

        findViewById(R.id.chapterNavigation).setVisibility(View.VISIBLE);

        client = new DNSClient(DNSClient.PresetDNS.GOOGLE, this, true);
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
                FetchAtHome(position);
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
        pageSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                turnPage(position * pageStep());
                if (bookmarkingEnabled && position >= pageSelection.getCount() - 2) {
                    FavouriteManager.SetBookmarkForFavourite(OnlineReaderActivity.this, mangaID, chapterIDs[indexToBookmark == chapterSelection.getCount() - 1 ? indexToBookmark : indexToBookmark + 1]);
                }
                pageProgressIndicator.setIndeterminate(false);
                pageProgressIndicator.setProgress((int) ((position + 1f) / pageSelection.getCount() * 100f));
                turnPage(position * pageStep());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (savedInstanceState == null) {
            pageSelection.setSelection(0);
            FolderUtilities.DeleteFolderContents(getExternalCacheDir());
        }
    }

    private void FetchAtHome(int position) {
        SetChapterControlsEnabled(false);
        SetPageControlsEnabled(false);
        pageProgressIndicator.setIndeterminate(true);
        client.HttpRequestAsync("https://api.mangadex.org/at-home/server/" + chapterIDs[position], new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    SetChapterControlsEnabled(true);
                    SetPageControlsEnabled(true);
                    chapterNext.setEnabled(position < chapterSelection.getCount() - 1);
                    chapterPrevious.setEnabled(position > 0);
                    pageProgressIndicator.setIndeterminate(false);
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    SetChapterControlsEnabled(true);
                    SetPageControlsEnabled(true);
                    chapterNext.setEnabled(position < chapterSelection.getCount() - 1);
                    chapterPrevious.setEnabled(position > 0);
                    pageProgressIndicator.setIndeterminate(false);
                });

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
                response.close();
                runOnUiThread(() -> {
                    GeneratePageSelectionSpinnerAdapter();
                    if (setLastPage) {
                        setLastPage = false;
                        pageSelection.setSelection(pageSelection.getCount() - 1);
                    }
                    else
                        pageSelection.setSelection(0);
                });
                if (bookmarkingEnabled) {
                    indexToBookmark = position;
                    FavouriteManager.SetBookmarkForFavourite(OnlineReaderActivity.this, mangaID, chapterIDs[position]);
                }
            }
        });
    }

    private void SetChapterControlsEnabled(boolean e) {
        chapterSelection.setEnabled(e);
        chapterNext.setEnabled(e);
        chapterPrevious.setEnabled(e);
    }
    private void SetPageControlsEnabled(boolean e) {
        pageSelection.setEnabled(e);
        next.setEnabled(e);
        previous.setEnabled(e);
        first.setEnabled(e);
        last.setEnabled(e);
    }

    private void UpdateButtonsState(final int index) {
        first.setEnabled(index > 0);
        last.setEnabled(index < urls.length - pageStep());

        previous.setEnabled(chapterPrevious.isEnabled() || first.isEnabled());
        next.setEnabled(chapterNext.isEnabled() || last.isEnabled());
    }

    public void turnPage(final int index) {
        if (index >= urls.length) return;

        display.setVisibility(View.INVISIBLE);
        pBar.setVisibility(View.VISIBLE);
        UpdateButtonsState(index);

        if (!landscape) {
            client.GetImageBitmapAsync(baseUrl + "/" + urls[index], bm -> runOnUiThread(() -> BitmapRetrieveDone(bm, null, index)), opt);
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
                    if (isWorkDone[0]) BitmapRetrieveDone(images[0], images[1], index);
                    else isWorkDone[0] = true;
                }, opt);
            }
            else {
                isWorkDone[0] = true;
                images[1] = null;
            }
            client.GetImageBitmapAsync(baseUrl + "/" + urls[index], bm -> {
                images[0] = bm;
                if (isWorkDone[0]) BitmapRetrieveDone(images[0], images[1], index);
                else isWorkDone[0] = true;
            }, opt);

            // Preload
            if (index < urls.length - 2)
                PreloadImage(baseUrl + "/" + urls[index + 2]);
            if (index < urls.length - 3)
                PreloadImage(baseUrl + "/" + urls[index + 3]);
        }
    }

    protected void BitmapRetrieveDone(Bitmap b1, Bitmap b2, int i) {
        if (i == GetCurIndex())
            super.BitmapRetrieveDone(b1, b2);
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

    @Override
    public void nextPage(View v) {
        if (pageSelection.getSelectedItemPosition() < pageSelection.getCount() - 1)
            pageSelection.setSelection(pageSelection.getSelectedItemPosition() + 1);
        else
            ChapterNext(v);
    }
    @Override
    public void previousPage(View v) {
        if (pageSelection.getSelectedItemPosition() > 0)
            pageSelection.setSelection(pageSelection.getSelectedItemPosition() - 1);
        else {
            ChapterPrevious(v);
            setLastPage = true;
        }
    }
}