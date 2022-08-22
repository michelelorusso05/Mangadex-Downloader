package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import com.michelelorusso.dnsclient.DNSClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ReaderActivity extends AppCompatActivity {
    private String baseUrl;
    private String[] urls;
    private boolean offlineReading;
    private boolean landscape;

    DNSClient client;

    Spinner progress;
    ImageButton previous, next, first, last;
    ImageView display;
    View pBar;

    // Bitmap configuration (applies to this class, all methods)
    final BitmapFactory.Options opt = new BitmapFactory.Options();

    ActivityResultLauncher<Intent> launchShareForResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // Bitmap config
        opt.inMutable = true;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inSampleSize = 1;

        display = findViewById(R.id.displayView);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        progress = findViewById(R.id.progressView);
        pBar = findViewById(R.id.loadingImage);

        previous = findViewById(R.id.previousButton);
        next = findViewById(R.id.nextButton);
        first = findViewById(R.id.firstButton);
        last = findViewById(R.id.lastButton);

        Bundle extras = getIntent().getExtras();
        baseUrl = extras.getString("baseUrl");
        urls = extras.getStringArray("urls");
        offlineReading = extras.getBoolean("sourceIsStorage", false);

        // Cancel notification if reader was launched via notification click
        int optionalNotificationToCancel = extras.getInt("notificationToCancel");
        if (optionalNotificationToCancel != 0) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.cancel(optionalNotificationToCancel);
        }

        if (!offlineReading)
            client = new DNSClient(DNSClient.PresetDNS.GOOGLE, this, true, 5);
        else {
            launchShareForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d("PDF share", "Shared");
                    } else {
                        Log.d("PDF share", "Canceled");
                    }
                    FolderUtilities.DeleteFolder(new File(getExternalFilesDir(null), "exportedPDFs"));
                });
        }

        previous.setOnClickListener(this::previousPage);
        next.setOnClickListener(this::nextPage);
        first.setOnClickListener(this::firstPage);
        last.setOnClickListener(this::lastPage);

        int l = (int) Math.ceil((float) urls.length / pageStep());
        String[] pages = new String[l];
        for (int i = 0; i < l; i++)
            pages[i] = UpdatePageIndicator(i * pageStep());

        progress.setAdapter(new ArrayAdapter<>(this, R.layout.page_indicator_spinner_item, pages));
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

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int savedPage = savedInstanceState.getInt("currentPage", 0);
        progress.setSelection(savedPage);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (landscape)
            outState.putInt("currentPage", progress.getSelectedItemPosition() * 2);
        else
            outState.putInt("currentPage", (int) Math.floor((float) progress.getSelectedItemPosition() / 2));
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.reader_toolbar, menu);
        return offlineReading;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.action_sharePDF) {
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private String UpdatePageIndicator(int index) {
        if (landscape && index < urls.length - 1)
            return getString(R.string.doublePageIndicator, index + 1, index + 2);
        return getString(R.string.pageIndicator, index + 1);
    }

    public void turnPage(int index) {
        display.setVisibility(View.INVISIBLE);
        pBar.setVisibility(View.VISIBLE);
        previous.setEnabled(index > 0);
        first.setEnabled(previous.isEnabled());
        next.setEnabled(index < urls.length - pageStep());
        last.setEnabled(next.isEnabled());

        if (!offlineReading) {
            client.CancelAllPendingRequests();
            if (!landscape) {
                client.GetImageBitmapAsync(baseUrl + "/" + urls[index], bm -> runOnUiThread(() -> {
                    display.setImageBitmap(bm);
                    display.setVisibility(View.VISIBLE);
                    pBar.setVisibility(View.GONE);
                }), opt);
                // Preloading
                if (index < urls.length - 1)
                    client.AsyncHttpRequest(baseUrl + "/" + urls[index + 1], new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {}

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
                    client.AsyncHttpRequest(baseUrl + "/" + urls[index + 2], new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {}

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
                if (index < urls.length - 3)
                    client.AsyncHttpRequest(baseUrl + "/" + urls[index + 3], new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {}

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
        }
        // Read image from internal storage
        else {
            new Thread(() -> {
                Bitmap b1 = BitmapFactory.decodeFile(baseUrl + "/" + urls[index], opt);
                Bitmap b2 = null;
                if (landscape && index < urls.length - 1)
                    b2 = BitmapFactory.decodeFile(baseUrl + "/" + urls[index + 1], opt);
                BitmapRetrieveDone(b1, b2);
            }).start();
        }
    }
    @NonNull
    private Bitmap combineBitmaps(@NonNull Bitmap left, @Nullable Bitmap right) {
        // If right is null, then only the last (lone) page is left to render
        if (right == null) return left;

        // Get the size of the images combined side by side.
        if (left.getHeight() > right.getHeight()) {
            left = ResizeBitmap(left, left.getWidth() * right.getHeight() / left.getHeight(), right.getHeight());
        }
        else {
            right = ResizeBitmap(right, right.getWidth() * left.getHeight() / right.getHeight(), left.getHeight());
        }

        int width = left.getWidth() + right.getWidth();
        int height = Math.max(left.getHeight(), right.getHeight());

        // Create a Bitmap large enough to hold both input images and a canvas to draw to this
        // combined bitmap.
        Bitmap combined = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(combined);

        // Render both input images into the combined bitmap and return it.
        canvas.drawBitmap(left, 0f, 0f, null);
        canvas.drawBitmap(right, left.getWidth(), 0f, null);

        return combined;
    }
    private Bitmap ResizeBitmap(Bitmap src, int nWidth, int nHeight) {
        return Bitmap.createScaledBitmap(src, nWidth, nHeight, true);
    }
    private void BitmapRetrieveDone(Bitmap b1, Bitmap b2) {
        new Thread(() -> {
            Bitmap b = combineBitmaps(b1, b2);
            runOnUiThread(() -> {
                display.setVisibility(View.VISIBLE);
                pBar.setVisibility(View.GONE);
                display.setImageBitmap(b);
            });
        }).start();
    }

    private int pageStep() {
        return landscape ? 2 : 1;
    }

    public void nextPage(View v) {
        progress.setSelection(progress.getSelectedItemPosition() + 1);
    }
    public void previousPage(View v) {
        progress.setSelection(progress.getSelectedItemPosition() - 1);
    }
    public void firstPage(View v) {
        progress.setSelection(0);
    }
    public void lastPage(View v) {
        progress.setSelection(progress.getCount() - 1);
    }
}