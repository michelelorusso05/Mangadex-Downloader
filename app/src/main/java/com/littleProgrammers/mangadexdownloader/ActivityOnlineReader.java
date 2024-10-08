package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.littleProgrammers.mangadexdownloader.apiResults.AtHomeResults;
import com.littleProgrammers.mangadexdownloader.utils.BetterSpinner;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ActivityOnlineReader extends ActivityReader {
    DNSClient client;

    String mangaID;
    ArrayList<String> chapterNames;
    ArrayList<String> chapterIDs;

    boolean bookmarkingEnabled;
    int indexToBookmark;

    boolean setLastPage;
    final AtomicInteger targetPage = new AtomicInteger(-1);

    AdapterViewPagerReaderPages adapter;
    ArrayAdapter<String> indexAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chapterPrevious.setOnClickListener(this::ChapterPrevious);
        chapterNext.setOnClickListener(this::ChapterNext);

        findViewById(R.id.chapterNavigation).setVisibility(View.VISIBLE);

        client = StaticData.getClient(getApplicationContext());

        chapterNames = getIntent().getStringArrayListExtra("chapterNames");
        chapterIDs = getIntent().getStringArrayListExtra("chapterIDs");
        mangaID = getIntent().getStringExtra("mangaID");
        String targetChapter = getIntent().getStringExtra("targetChapter");


        bookmarkingEnabled = FavouriteManager.IsFavourite(this, mangaID);
        if (savedInstanceState != null)
            targetPage.set(savedInstanceState.getInt("currentPage", -1) - 1);

        chapterSelection.setAdapter(new AdapterSpinnerChapterSelection(this, chapterNames, null));
        chapterSelection.setOnItemSelectedListener((pos) -> {
            if (adapter != null) adapter.detach();
            FetchAtHome(pos);
        });

        if (targetChapter == null)
            chapterSelection.setSelection(0);
        else {
            for (int i = 0; i < chapterIDs.size(); i++) {
                if (chapterIDs.get(i).equals(targetChapter)) {
                    chapterSelection.setSelection(i);
                    break;
                }
            }
        }

        pageSelection.setOnItemSelectedListener(pos -> {
            if (pageSelection.spinner.getTag() != null) {
                pageSelection.spinner.setTag(null);
                return;
            }
            LockPager();
            pager.setCurrentItem(adapter.chapterPositionToRawPosition(pos), false);

            // Update bookmark
            if (bookmarkingEnabled && pos >= pageSelection.getCount() - 2) {
                FavouriteManager.SetBookmarkForFavourite(ActivityOnlineReader.this, mangaID,
                        chapterIDs.get(indexToBookmark == chapterSelection.getCount() - 1 ? indexToBookmark : indexToBookmark + 1),
                        indexToBookmark == chapterSelection.getCount() - 1);
            }
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

                pageProgressIndicator.setProgressCompat((int) ((100f / adapter.getTotalElements()) * (pos + 1)), true);
            }
        });

        if (savedInstanceState == null) {
            try {
                client.getCache().evictAll();
            } catch (IOException ignored) {}
        }

        next.setOnClickListener((v) -> pager.setCurrentItem(pager.getCurrentItem() + 1));
        previous.setOnClickListener((v) -> pager.setCurrentItem(pager.getCurrentItem() - 1));
        first.setOnClickListener((v) -> pager.setCurrentItem(adapter.getFirstPage(), false));
        last.setOnClickListener((v) -> pager.setCurrentItem(adapter.getLastPage(), false));
    }

    private void FetchAtHome(int position) {
        client.CancelAllPendingRequests();

        SetChapterControlsEnabled(false);
        SetPageControlsEnabled(false);
        pageProgressIndicator.setIndeterminate(true);
        client.HttpRequestAsync("https://api.mangadex.org/at-home/server/" + chapterIDs.get(position), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    SetChapterControlsEnabled(true);
                    SetPageControlsEnabled(true);
                    chapterNext.setEnabled(position < chapterSelection.getCount() - 1);
                    chapterPrevious.setEnabled(position > 0);
                    pageProgressIndicator.setIndeterminate(false);

                    // TODO: retry button
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                AtHomeResults hResults = StaticData.getMapper().readValue(Objects.requireNonNull(response.body()).charStream(), AtHomeResults.class);
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
                    pageProgressIndicator.setProgressCompat((int) (100f / chapterSelection.getCount()), true);
                    pageProgressIndicator.setMax(100);

                    adapter = new AdapterViewPagerOnlinePages(ActivityOnlineReader.this,
                            baseUrl, urls, client, AdapterViewPagerReaderPages.BooleanToParameter(
                                    chapterPrevious.isEnabled(), chapterNext.isEnabled()), landscape,
                            () -> onTouch.accept(null));
                    adapter.setHasStableIds(true);

                    int l = urls.length;
                    final ArrayList<String> pages = new ArrayList<>(l);
                    for (int i = 0; i < l; i++)
                        pages.add(UpdatePageIndicator(i));

                    indexAdapter = new AdapterSpinnerIndexes(ActivityOnlineReader.this, pages);
                    pageSelection.setAdapter(indexAdapter);

                    adapter.setOnPageUpdatedCallback((indexes, removed) -> runOnUiThread(() -> {
                        int pos = pageSelection.getSelectedItemPosition();
                        boolean matchString = false;
                        Integer index = null;
                        try {
                            index = Integer.parseInt((String) pageSelection.getSelectedItem());
                        } catch (NumberFormatException e) {
                            matchString = true;
                        }

                        int targetPos = pos;

                        final String[] newPages = new String[indexes.size()];

                        for (int i = 0; i < indexes.size(); i++) {
                            Pair<Integer, Integer> p = indexes.get(i);
                            newPages[i] = p.second == null ? getString(R.string.pageIndicator, p.first + 1) : getString(R.string.doublePageIndicator, p.first + 1, p.second + 1);

                            if (matchString) {
                                if (newPages[i].equals(pageSelection.getSelectedItem()))
                                    targetPos = i;
                            }
                            else {
                                if (index.equals(p.first + 1) || index.equals((p.second == null ? -2 : p.second) + 1))
                                    targetPos = i;
                            }
                        }

                        indexAdapter.clear();
                        indexAdapter.addAll(newPages);
                        indexAdapter.notifyDataSetChanged();

                        if (pos == targetPos) return;
                        LockSelectionSpinner();
                        pageSelection.setSelection(targetPos);

                    }));
                    pager.setAdapter(adapter);

                    if (targetPage.get() == -1)
                        pager.setCurrentItem(lastFlag ? adapter.getLastPage() : adapter.getFirstPage(), false);
                    else {
                        pager.setCurrentItem(adapter.chapterPositionToRawPosition(targetPage.get()), false);
                        targetPage.set(-1);
                    }
                });
                if (bookmarkingEnabled) {
                    indexToBookmark = position;
                    FavouriteManager.SetBookmarkForFavourite(ActivityOnlineReader.this, mangaID, chapterIDs.get(position),
                            position == chapterIDs.size() - 1);
                }

                setLastPage = false;
            }
        }, true);
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

    private void SetChapterRelative(int delta) {
        if (chapterSelection.getSelectedItemPosition() + delta < chapterSelection.getCount() &&
            chapterSelection.getSelectedItemPosition() + delta >= 0) {
            // If a spinner (or any of its parent) has its visibility set to GONE the onItemSelected event won't execute
            if (!chapterSelection.spinner.isShown()) {
                FetchAtHome(chapterSelection.getSelectedItemPosition() + delta);
                chapterSelection.setVisualSelection(chapterSelection.getSelectedItemPosition() + delta);
            }
            else
                chapterSelection.setSelection(chapterSelection.getSelectedItemPosition() + delta);
        }
    }

    public void ChapterNext(View v) {
        SetChapterRelative(1);
    }
    public void ChapterPrevious(View v) {
        SetChapterRelative(-1);
    }
}

