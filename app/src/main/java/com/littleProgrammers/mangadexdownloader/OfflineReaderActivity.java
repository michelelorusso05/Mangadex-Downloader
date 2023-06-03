package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.littleProgrammers.mangadexdownloader.utils.FolderUtilities;
import com.littleProgrammers.mangadexdownloader.utils.PDFHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class OfflineReaderActivity extends ReaderActivity {
    ActivityResultLauncher<Intent> launchShareForResult;
    private boolean pdfShareEnqueued;
    OfflinePagesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        baseUrl = extras.getString("baseUrl");
        urls = extras.getStringArray("urls");
        if (urls == null) urls = new String[0];

        launchShareForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d("PDF share", "Shared");
                    } else {
                        Log.d("PDF share", "Canceled");
                    }
                    FolderUtilities.DeleteFolder(new File(getExternalFilesDir(null), "exportedPDFs"));
                    pdfShareEnqueued = false;
                });

        pageProgressIndicator.setIndeterminate(false);

        pageSelection.spinner.setSaveEnabled(false);
        pageSelection.setOnItemSelectedListener(pos -> {
            if (pageSelection.spinner.getTag() != null) {
                pageSelection.spinner.setTag(null);
                return;
            }

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
                    LockSelectionSpinner();
                    pageSelection.setSelection(pos);
                }

                pageProgressIndicator.setProgress((int) ((100f / adapter.getTotalElements()) * (pos + 1)));
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

                adapter = new OfflinePagesAdapter(OfflineReaderActivity.this, baseUrl, urls, ReaderPagesAdapter.NAVIGATION_ONESHOT, landscape, indexes);
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


                final int savedPage  = pageToSet;
                runOnUiThread(() -> {
                    LockSelectionSpinner();
                    pageSelection.setAdapter(new ArrayAdapter<>(OfflineReaderActivity.this, R.layout.page_indicator_spinner_item, pages));
                    pager.setCurrentItem(savedPage, false);
                });
            }).start();
        }
        else {
            GeneratePageSelectionSpinnerAdapter();
            adapter = new OfflinePagesAdapter(OfflineReaderActivity.this, baseUrl, urls, ReaderPagesAdapter.NAVIGATION_ONESHOT, landscape, null);
            pager.setAdapter(adapter);
            pager.setCurrentItem(currentPage, false);
        }
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
        PDFHelper helper = new PDFHelper();
        File currentDir = new File(baseUrl);
        helper.SetPDFName(currentDir.getName().substring(0, currentDir.getName().length() - 3));
        helper.SetSourcePath(baseUrl);
        helper.SetDestinationPath(new File(getExternalFilesDir(null), "exportedPDFs").getAbsolutePath());

        try {
            helper.CreatePDF();
        } catch (FileNotFoundException | MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        File createdPDF = helper.getDownloadedPDFFilePath();
        Intent intentShareFile = new ShareCompat.IntentBuilder(this)
                .setType("application/pdf")
                .setStream(FileProvider.getUriForFile(this, "com.littleProgrammers.mangadexdownloader.provider", createdPDF))
                .setChooserTitle(R.string.shareAsPDF)
                .createChooserIntent();

        launchShareForResult.launch(intentShareFile);
    }
}
