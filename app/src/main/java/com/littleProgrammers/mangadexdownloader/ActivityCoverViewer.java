package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.littleProgrammers.mangadexdownloader.apiResults.CoverResults;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ActivityCoverViewer extends ActivityReader {
    DNSClient client;

    String mangaID;
    String[] coverIDs;

    AtomicInteger targetPage = new AtomicInteger(-1);

    AdapterViewPagerReaderPages adapter;
    ArrayAdapter<String> indexAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        client = StaticData.getClient(getApplicationContext());
        mangaID = getIntent().getStringExtra("mangaID");

        if (savedInstanceState != null)
            targetPage.set(savedInstanceState.getInt("currentPage", -1) - 1);

        String targetCover = getIntent().getStringExtra("targetCover");

        pageSelection.setOnItemSelectedListener(pos -> {
            if (pageSelection.spinner.getTag() != null) {
                pageSelection.spinner.setTag(null);
                return;
            }
            LockPager();
            pager.setCurrentItem(adapter.chapterPositionToRawPosition(pos), false);
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
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

        FetchCovers();
    }

    private void FetchCovers() {
        client.CancelAllPendingRequests();

        SetPageControlsEnabled(false);
        pageProgressIndicator.setIndeterminate(true);
        client.HttpRequestAsync("https://api.mangadex.org/cover/?manga[]=" + mangaID + "&limit=100&order%5Bvolume%5D=asc", new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    SetPageControlsEnabled(true);
                    pageProgressIndicator.setIndeterminate(false);
                    // TODO: retry button
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                CoverResults cResults = StaticData.getMapper().readValue(Objects.requireNonNull(response.body()).charStream(), CoverResults.class);
                response.close();

                String _baseUrl = "https://uploads.mangadex.org/covers/" + mangaID + "/";
                String[] _images = new String[cResults.getData().length];

                boolean HQ = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("lowQualityCovers", false);
                if (HQ) {
                    for (int i = 0; i < _images.length; i++) {
                        _images[i] = cResults.getData()[i].getAttributes().getFileName();
                    }
                }
                else {
                    for (int i = 0; i < _images.length; i++) {
                        _images[i] = cResults.getData()[i].getAttributes().getFileName() + ".512.jpg";
                    }
                }
                baseUrl = _baseUrl;
                urls = _images;
                response.close();

                runOnUiThread(() -> {
                    SetPageControlsEnabled(true);
                    pageProgressIndicator.setIndeterminate(false);
                    pageProgressIndicator.setProgressCompat((int) (100f / urls.length), true);
                    pageProgressIndicator.setMax(100);

                    adapter = new AdapterViewPagerOnlinePages(ActivityCoverViewer.this,
                            baseUrl, urls, client, AdapterViewPagerReaderPages.BooleanToParameter(
                            false, false), false, () -> onTouch.accept(null));
                    adapter.setHasStableIds(true);

                    int l = urls.length;
                    final ArrayList<String> pages = new ArrayList<>(l);
                    for (int i = 0; i < l; i++)
                        pages.add("Volume " + cResults.getData()[i].getAttributes().getVolume());

                    indexAdapter = new AdapterSpinnerIndexes(ActivityCoverViewer.this, pages);
                    pageSelection.setAdapter(indexAdapter);
                    pager.setAdapter(adapter);
                });
            }
        }, true);
    }

    private void SetPageControlsEnabled(boolean e) {
        pageSelection.setEnabled(e);
        next.setEnabled(e);
        previous.setEnabled(e);
        first.setEnabled(e);
        last.setEnabled(e);
    }
}
