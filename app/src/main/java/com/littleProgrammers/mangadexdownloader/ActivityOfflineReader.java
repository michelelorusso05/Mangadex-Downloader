package com.littleProgrammers.mangadexdownloader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.littleProgrammers.mangadexdownloader.utils.FolderUtilities;
import com.littleProgrammers.mangadexdownloader.utils.PDFHelper;

import java.io.File;
import java.util.ArrayList;

public class ActivityOfflineReader extends ActivityReader {
    ActivityResultLauncher<Intent> launchShareForResult;
    private boolean pdfShareEnqueued;
    AdapterViewPagerOfflinePages adapter;

    View progressView;
    CircularProgressIndicator progressIndicator;

    String mangaID;
    ArrayList<String> chapterNames;
    ArrayList<String> chapterIDs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        baseUrl = extras.getString("baseUrl");
        urls = extras.getStringArray("urls");
        if (urls == null) urls = new String[0];

        chapterNames = getIntent().getStringArrayListExtra("chapterNames");
        chapterIDs = getIntent().getStringArrayListExtra("chapterIDs");
        mangaID = getIntent().getStringExtra("mangaID");
        String targetChapter = getIntent().getStringExtra("targetChapter");

        launchShareForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    FolderUtilities.DeleteFolder(new File(getExternalFilesDir(null), "exportedPDFs"));
                    pdfShareEnqueued = false;
                });

        pageProgressIndicator.setIndeterminate(false);

        pageSelection.spinner.setSaveEnabled(false);
        pageSelection.setOnItemSelectedListener(pos -> {
            LockPager();
            pager.setCurrentItem(adapter.chapterPositionToRawPosition(pos));
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
            int pos = adapter.rawPositionToChapterPosition(position);
            if (pager.getTag() != null) {
                pager.setTag(null);
            }
            else {
                pageSelection.setVisualSelection(pos);
            }

            pageProgressIndicator.setProgressCompat((int) ((100f / adapter.getTotalElements()) * (pos + 1)), true);
            }
        });

        int page = savedInstanceState != null ? savedInstanceState.getInt("currentPage", 1) : 1;
        int currentPage = page - 1;

        if (landscape) {
            new Thread(() -> {
                // Indexes are generated one time, here, in a non-iterative fashion (all the pages are preloaded here).

                ArrayList<Pair<Integer, Integer>> indexes = new ArrayList<>();
                boolean[] isLandscape = new boolean[urls.length];

                for (int i = 0; i < urls.length; i++) {
                    String s = urls[i];
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(baseUrl + File.separator + s, options);

                    isLandscape[i] = options.outWidth >= options.outHeight;
                }

                for (int i = 0; i < isLandscape.length; i++) {
                    if (isLandscape[i] || i == 0) indexes.add(new Pair<>(i, null));
                    else {
                        if (i + 1 >= isLandscape.length || isLandscape[i + 1]) indexes.add(new Pair<>(i, null));
                        else {
                            indexes.add(new Pair<>(i, i + 1));
                            i++;
                        }
                    }
                }

                adapter = new AdapterViewPagerOfflinePages(
                        ActivityOfflineReader.this, baseUrl, urls,
                        AdapterViewPagerReaderPages.NAVIGATION_ONESHOT,
                        landscape, indexes, () -> onTouch.accept(null));

                adapter.setHasStableIds(true);

                runOnUiThread(() -> pager.setAdapter(adapter));

                int pageToSet = 0;

                String[] pages = new String[indexes.size()];
                for (int i = 0; i < indexes.size(); i++) {
                    Pair<Integer, Integer> p = indexes.get(i);
                    pages[i] = p.second == null ? getString(R.string.pageIndicator, p.first + 1) : getString(R.string.doublePageIndicator, p.first + 1, p.second + 1);

                    if (currentPage == p.first || (p.second != null && currentPage == p.second))
                        pageToSet = i;
                }


                final int savedPage = pageToSet;
                runOnUiThread(() -> {
                    pageSelection.setAdapter(new ArrayAdapter<>(ActivityOfflineReader.this, R.layout.item_spinner_page_indicator, pages));
                    pager.setCurrentItem(savedPage, false);
                });
            }).start();
        }
        else {
            GeneratePageSelectionSpinnerAdapter();
            adapter = new AdapterViewPagerOfflinePages(
                    ActivityOfflineReader.this, baseUrl, urls,
                    AdapterViewPagerReaderPages.NAVIGATION_ONESHOT, landscape,
                    null, () -> onTouch.accept(null));
            pager.setAdapter(adapter);
            pager.setCurrentItem(currentPage, false);
        }

        progressView = findViewById(R.id.pdfShareProgress);
        progressIndicator = findViewById(R.id.pdfShareProgressBar);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.reader_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sharePDF && !pdfShareEnqueued) {
            pdfShareEnqueued = true;
            new Thread(this::CreateAndSharePDF).start();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void CreateAndSharePDF() {
        runOnUiThread(() -> {
            progressView.setAlpha(0);
            ObjectAnimator animation = ObjectAnimator.ofFloat(progressView, "alpha", 1f);
            animation.setDuration(200);
            animation.start();
            progressView.setVisibility(View.VISIBLE);
            progressIndicator.setIndeterminate(false);
            progressIndicator.setMax(100);
            progressIndicator.setProgress(0);
        });

        File currentDir = new File(baseUrl);
        String name = currentDir.getName().substring(0, currentDir.getName().length() - 3);

        File createdPDF = PDFHelper.GeneratePDF(new File(getExternalFilesDir(null), "exportedPDFs"), new File(baseUrl), name,
                (p) -> runOnUiThread(() -> {
                    if (p >= 0)
                        progressIndicator.setProgressCompat(p.intValue(), true);
                }));

        if (createdPDF == null) return;
        runOnUiThread(() -> {
            ObjectAnimator animation = ObjectAnimator.ofFloat(progressView, "alpha", 0f);
            animation.setDuration(200);
            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    progressView.setVisibility(View.GONE);
                }
            });
            animation.start();
        });

        Intent intentShareFile = new ShareCompat.IntentBuilder(this)
                .setType("application/pdf")
                .setStream(FileProvider.getUriForFile(this, "com.littleProgrammers.mangadexdownloader.provider", createdPDF))
                .setChooserTitle(R.string.shareAsPDF)
                .createChooserIntent();

        launchShareForResult.launch(intentShareFile);
    }
}
