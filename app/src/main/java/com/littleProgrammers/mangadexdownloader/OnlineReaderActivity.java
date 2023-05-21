package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.AtHomeResults;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.util.ArrayList;
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

    boolean startedPreloading;
    final ArrayList<Boolean> isLandscape = new ArrayList<>();
    ArrayList<Pair<Integer, Integer>> indexes = new ArrayList<>();

    Spinner chapterSelection;
    ImageButton chapterNext, chapterPrevious;

    boolean bookmarkingEnabled;
    int indexToBookmark;

    boolean setLastPage;

    ReaderPagesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chapterPrevious = findViewById(R.id.previousChapterButton);
        chapterNext = findViewById(R.id.nextChapterButton);
        chapterSelection = findViewById(R.id.chapterSelection);

        chapterPrevious.setOnClickListener(this::ChapterPrevious);
        chapterNext.setOnClickListener(this::ChapterNext);

        findViewById(R.id.chapterNavigation).setVisibility(View.VISIBLE);

        client = StaticData.getClient(getApplicationContext());
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
                startedPreloading = false;
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
                if (pageSelection.getTag() != null) {
                    pageSelection.setTag(null);
                }
                else {
                    LockPager();
                    pager.setCurrentItem(position + 1);
                }


                if (bookmarkingEnabled && position >= pageSelection.getCount() - 2) {
                    FavouriteManager.SetBookmarkForFavourite(OnlineReaderActivity.this, mangaID,
                            chapterIDs[indexToBookmark == chapterSelection.getCount() - 1 ? indexToBookmark : indexToBookmark + 1],
                            indexToBookmark == chapterSelection.getCount() - 1);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position < adapter.getFirstPage()) {
                    setLastPage = true;
                    ChapterPrevious(null);
                    return;
                }
                if (position > adapter.getLastPage()) {
                    setLastPage = false;
                    ChapterNext(null);
                    return;
                }
                int pos = adapter.rawPositionToChapterPosition(position);
                if (pager.getTag() != null) {
                    pager.setTag(null);
                }
                else {
                    LockSelectionSpinner();
                    pageSelection.setSelection(pos);
                }

                pageProgressIndicator.setProgress((int) ((100f / adapter.getTotalElements()) * (pos + 1)));
            }
        });

        if (savedInstanceState == null) {
            try {
                client.getCache().evictAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
            pageSelection.setSelection(0);
        }

        next.setOnClickListener((v) -> pager.setCurrentItem(pager.getCurrentItem() + 1));
        previous.setOnClickListener((v) -> pager.setCurrentItem(pager.getCurrentItem() - 1));
        first.setOnClickListener((v) -> pager.setCurrentItem(adapter.getFirstPage()));
        last.setOnClickListener((v) -> pager.setCurrentItem(adapter.getLastPage()));
    }

    private void FetchAtHome(int position) {
        client.CancelAllPendingRequests();

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
                AtHomeResults hResults = mapper.readValue(Objects.requireNonNull(response.body()).charStream(), AtHomeResults.class);
                response.close();

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

                final boolean lastFlag = setLastPage;

                runOnUiThread(() -> {
                    SetChapterControlsEnabled(true);
                    SetPageControlsEnabled(true);
                    chapterNext.setEnabled(position < chapterSelection.getCount() - 1);
                    chapterPrevious.setEnabled(position > 0);
                    pageProgressIndicator.setIndeterminate(false);
                    pageProgressIndicator.setProgress((int) (100f / chapterSelection.getCount()));
                    pageProgressIndicator.setMax(100);

                    adapter = new OnlinePagesAdapter(OnlineReaderActivity.this,
                            baseUrl, urls, client, ReaderPagesAdapter.BooleanToParameter(
                                    chapterPrevious.isEnabled(), chapterNext.isEnabled()), landscape);
                    adapter.setHasStableIds(true);
                    adapter.setOnPageUpdatedCallback((indexes) -> {
                        LockSelectionSpinner();

                        String[] pages = new String[indexes.size()];

                        for (int i = 0; i < indexes.size(); i++) {
                            Pair<Integer, Integer> p = indexes.get(i);
                            pages[i] = p.second == null ? getString(R.string.pageIndicator, p.first + 1) : getString(R.string.doublePageIndicator, p.first + 1, p.second + 1);
                        }

                        runOnUiThread(() -> pageSelection.setAdapter(new ArrayAdapter<>(OnlineReaderActivity.this, R.layout.page_indicator_spinner_item, pages)));
                    });
                    pager.setAdapter(adapter);
                    pager.setCurrentItem(lastFlag ? adapter.getLastPage() : adapter.getFirstPage(), false);

                    GeneratePageSelectionSpinnerAdapter();

                    if (lastFlag) {
                        LockSelectionSpinner();
                        pageSelection.setSelection(urls.length - 1);
                    }
                });
                if (bookmarkingEnabled) {
                    indexToBookmark = position;
                    FavouriteManager.SetBookmarkForFavourite(OnlineReaderActivity.this, mangaID, chapterIDs[position],
                            position == chapterIDs.length - 1);
                }

                setLastPage = false;
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
        last.setEnabled(index < urls.length - 1);

        previous.setEnabled(chapterPrevious.isEnabled() || first.isEnabled());
        next.setEnabled(chapterNext.isEnabled() || last.isEnabled());
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

