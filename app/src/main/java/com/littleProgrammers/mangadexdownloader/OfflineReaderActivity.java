package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

public class OfflineReaderActivity extends ReaderActivity {
    ActivityResultLauncher<Intent> launchShareForResult;
    private boolean pdfShareEnqueued;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        baseUrl = extras.getString("baseUrl");
        urls = extras.getStringArray("urls");
        if (urls == null) urls = new String[0];

        // Cancel notification if reader was launched via notification click
        int optionalNotificationToCancel = extras.getInt("notificationToCancel");
        if (optionalNotificationToCancel != 0) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.cancel(optionalNotificationToCancel);
        }

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

        GeneratePageSelectionSpinnerAdapter();
        pageSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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

    @Override
    public void turnPage(int index) {
        if (index >= urls.length) return;

        display.setVisibility(View.INVISIBLE);
        pBar.setVisibility(View.VISIBLE);
        previous.setEnabled(index > 0);
        first.setEnabled(previous.isEnabled());
        next.setEnabled(index < urls.length - pageStep());
        last.setEnabled(next.isEnabled());

        final int fIndex = index;
        new Thread(() -> {
            Bitmap b1 = BitmapFactory.decodeFile(baseUrl + "/" + urls[index], opt);
            Bitmap b2 = null;
            if (landscape && index < urls.length - 1)
                b2 = BitmapFactory.decodeFile(baseUrl + "/" + urls[index + 1], opt);
            BitmapRetrieveDone(b1, b2, fIndex);
        }).start();
    }

    protected void BitmapRetrieveDone(Bitmap b1, Bitmap b2, int i) {
        if (i == GetCurIndex())
            super.BitmapRetrieveDone(b1, b2);
    }
}
