package com.littleProgrammers.mangadexdownloader;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.transition.Fade;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.ChapterResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.michelelorusso.dnsclient.DNSClient;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class ChapterDownloaderActivity extends AppCompatActivity
{
    private ObjectMapper mapper;

    // Unique notification id
    public static final int NOTIFICATION_ID = 6271;

    // Manga to download chapters of (passed through intent)
    Manga selectedManga;

    // Array to hold filtered chapters
    ArrayList<Chapter> mangaChapters = new ArrayList<>();

    TextView title;
    TextView author;
    TextView description;
    ImageView cover;
    Spinner chapterSelection;

    Button downloadButton, readButton;
    View progressBar;
    View continueReading;

    DNSClient client;

    boolean markedFavourite;
    int bookmarkFavouriteIndex = -1;

    ActivityResultLauncher<String> requestPermissionLauncher;


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.downloading);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(String.valueOf(NOTIFICATION_ID), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            // notificationManager.cancelAll();
        }
         requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                SharedPreferences.Editor e = this.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE).edit();
                e.putBoolean("refusedNotifications", !isGranted);
                e.apply();

                DownloadChapter(null);
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        // set an enter transition
        getWindow().setEnterTransition(new Fade());
        // set an exit transition
        getWindow().setExitTransition(new Fade());
        setContentView(R.layout.activity_download);

        client = new DNSClient(DNSClient.PresetDNS.GOOGLE, this, true);

        // UI binding
        title = findViewById(R.id.mangaTitle);
        author = findViewById(R.id.authorView);
        description = findViewById(R.id.mangaDescription);
        cover = findViewById(R.id.cover);
        chapterSelection = findViewById(R.id.chapterSelection);
        downloadButton = findViewById(R.id.buttonDownload);
        readButton = findViewById(R.id.buttonRead);
        progressBar = findViewById(R.id.readLoadingBar);
        continueReading = findViewById(R.id.continueReading);

        Toolbar t = findViewById(R.id.home_toolbar);
        setSupportActionBar(t);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        ChapterSelectionAdapter adapter = new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                new Pair<>(getString(R.string.fetchingString), getString(R.string.wait)));
        chapterSelection.setAdapter(adapter);

        chapterSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mangaChapters.size() == 0) return;
                if (mangaChapters.get(position).getAttributes().getExternalUrl() != null)
                    downloadButton.setVisibility(View.GONE);
                else
                    downloadButton.setVisibility(View.VISIBLE);
                if (position != bookmarkFavouriteIndex)
                    continueReading.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        downloadButton.setOnClickListener(this::DownloadChapter);
        readButton.setOnClickListener(this::ReadChapter);

        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        // UI initialization (and recover from previous instance if needed)
        if (savedInstanceState == null) {
            if (getIntent().hasExtra("MangaData")) {
                try {
                    selectedManga = mapper.readValue(getIntent().getStringExtra("MangaData"), Manga.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            try {
                selectedManga = mapper.readValue(savedInstanceState.getString("MangaData"), Manga.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        try {
            author.setText(selectedManga.getAttributes().getAuthorString());
            author.setMovementMethod(new ScrollingMovementMethod());
            title.setText(FormattingUtilities.FormatFromHtml(selectedManga.getAttributes().getTitleS()));

            HashMap<String, String> desc = selectedManga.getAttributes().getDescription();
            String descriptionString = (desc.containsKey("en")) ?
                    desc.get("en") :
                    desc.entrySet().iterator().next().getValue();

            if (descriptionString != null && !descriptionString.isEmpty())
                description.setText(FormattingUtilities.FormatFromHtml(FormattingUtilities.MarkdownLite(descriptionString)));
            description.setMovementMethod(new ScrollingMovementMethod());

            getCover();
            getMangaChapterList();
        } catch (JSONException | InterruptedException e) {
            e.printStackTrace();
        }

        createNotificationChannel();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putString("MangaData", mapper.writeValueAsString(selectedManga));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.chapter_download_toolbar, menu);
        markedFavourite = FavouriteManager.IsFavourite(this, selectedManga.getId());
        menu.getItem(0).setIcon(markedFavourite ? R.drawable.ic_baseline_bookmark_24 : R.drawable.ic_baseline_bookmark_disabled_24);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }
        else if (item.getItemId() == R.id.action_favourite) {
            markedFavourite = !markedFavourite;
            item.setIcon(markedFavourite ? R.drawable.ic_baseline_bookmark_24 : R.drawable.ic_baseline_bookmark_disabled_24);
            if (markedFavourite)
                FavouriteManager.AddFavourite(this, selectedManga.getId());
            else
                FavouriteManager.RemoveFavourite(this, selectedManga.getId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mangaChapters.size() != 0) {
            Pair<String, Boolean> savedBookmark = FavouriteManager.GetBookmarkForFavourite(this, selectedManga.getId());

            if (savedBookmark != null) {
                final boolean queueNext = savedBookmark.second;
                for (int i = 0; i < mangaChapters.size(); i++) {
                    if (mangaChapters.get(i).getId().equals(savedBookmark.first)) {
                        if (queueNext && i < mangaChapters.size() - 1)
                            bookmarkFavouriteIndex = i + 1;
                        else
                            bookmarkFavouriteIndex = i;
                        break;
                    }
                }
            }
            if (bookmarkFavouriteIndex == -1)
                chapterSelection.setSelection(chapterSelection.getCount() - 1);
            else {
                chapterSelection.setSelection(bookmarkFavouriteIndex);
                continueReading.setVisibility(View.VISIBLE);
            }
        }
    }

    private void getCover() throws JSONException {
        // Load already cached image
        if (StaticData.sharedCover != null) {
            cover.setImageBitmap(StaticData.sharedCover);
            ImageView background = findViewById(R.id.coverBackground);
            if (background != null)
                background.setImageBitmap(StaticData.sharedCover);
            return;
        }

        boolean lowQualityCover = PreferenceManager.getDefaultSharedPreferences(ChapterDownloaderActivity.this).getBoolean("lowQualityCovers", false);
        final String coverUrl = "https://uploads.mangadex.org/covers/" + selectedManga.getId() + "/" + selectedManga.getAttributes().getCoverUrl() + ((lowQualityCover) ? ".256.jpg" : ".512.jpg");

        client.GetImageBitmapAsync(coverUrl, (bm) -> runOnUiThread(() -> {
            cover.setImageBitmap(bm);
            ImageView background = findViewById(R.id.coverBackground);
            if (background != null)
                background.setImageBitmap(bm);
        }));
    }

    public void getMangaChapterList() throws JSONException, InterruptedException {
        final String lang = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("chapterLanguages", "en");
        if (!selectedManga.getAttributes().isLanguageAvailable(lang)) {
            chapterSelection.setAdapter(new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                    new Pair<>(getString(R.string.mangaNoEntries), getString(R.string.mangaNoEntriesSubtext))));
            return;
        }

        String mangaID = selectedManga.getId();

        String baseUrl = "https://api.mangadex.org/manga/" + mangaID + "/feed?translatedLanguage[]=" + lang + "&order%5Bvolume%5D=asc&order%5Bchapter%5D=asc&order%5BpublishAt%5D=asc&includes[]=scanlation_group";
        client.HttpRequestAsync(baseUrl.concat("&limit=250"), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ChapterResults cResults = mapper.readValue(Objects.requireNonNull(response.body()).string(), ChapterResults.class);
                final int totalChapters = cResults.getTotal();

                // We can't fit all the chapters in one request
                if (totalChapters > 250) {
                    mangaChapters = new ArrayList<>(Arrays.asList(new Chapter[totalChapters]));

                    // Copy the first 250 elements
                    for (int i = 0; i < 250; i++)
                        mangaChapters.set(i, cResults.getData()[i]);

                    // Query the others
                    final int[] remainingChapters = {totalChapters - 250};
                    for (int i = 250; i < totalChapters; i += 250) {
                        final int offset = i;
                        client.HttpRequestAsync(baseUrl.concat("&limit=250&offset=").concat(String.valueOf(i)), new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                ChapterResults _cResults = mapper.readValue(Objects.requireNonNull(response.body()).string(), ChapterResults.class);
                                // Copy the retrieved elements
                                for (int j = 0; j < _cResults.getData().length; j++)
                                    mangaChapters.set(offset + j, _cResults.getData()[j]);

                                remainingChapters[0] -= 250;
                                if (remainingChapters[0] < 0)
                                    OnChapterRetrievingEnd();
                            }
                        });
                    }
                }
                else {
                    mangaChapters = new ArrayList<>(Arrays.asList(cResults.getData()));
                    OnChapterRetrievingEnd();
                }
            }
        });
    }

    public void OnChapterRetrievingEnd() {
        Pair<String, Boolean> savedBookmark = FavouriteManager.GetBookmarkForFavourite(this, selectedManga.getId());
        String bookmark = (savedBookmark != null) ? savedBookmark.first : null;

        ChapterUtilities.FormatChapterList(ChapterDownloaderActivity.this, mangaChapters, new ChapterUtilities.FormattingOptions(
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("chapterDuplicate", true),
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("hideExternal", true),
                bookmark));

        if (mangaChapters.size() == 0) {
            runOnUiThread(() -> chapterSelection.setAdapter(new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                    new Pair<>(getString(R.string.mangaNoEntriesFilter), getString(R.string.mangaNoEntriesSubtext)))));
            return;
        }

        // Move the oneshots at the beginning of the array
        int currentIteration = 0;
        while (mangaChapters.get(mangaChapters.size() - 1).getAttributes().getChapter() == null
                && currentIteration < mangaChapters.size()) {
            Chapter c = mangaChapters.remove(mangaChapters.size() - 1);
            mangaChapters.add(0, c);
            currentIteration++;
        }

        if (savedBookmark != null) {
            final boolean queueNext = savedBookmark.second;
            for (int i = 0; i < mangaChapters.size(); i++) {
                if (mangaChapters.get(i).getId().equals(savedBookmark.first)) {
                    if (queueNext && i < mangaChapters.size() - 1)
                        bookmarkFavouriteIndex = i + 1;
                    else
                        bookmarkFavouriteIndex = i;
                    break;
                }
            }
        }

        final int index = bookmarkFavouriteIndex;
        ChapterDownloaderActivity.this.runOnUiThread(() -> {
            ChapterSelectionAdapter adapter = new ChapterSelectionAdapter(ChapterDownloaderActivity.this, mangaChapters.toArray(new Chapter[0]));
            chapterSelection.setAdapter(adapter);
            if (index == -1)
                chapterSelection.setSelection(0);
            else {
                chapterSelection.setSelection(index);
                continueReading.setVisibility(View.VISIBLE);
            }

            findViewById(R.id.buttonDownload).setEnabled(true);
            findViewById(R.id.buttonRead).setEnabled(true);
        });
    }

    public void DownloadChapter(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            boolean disableNotifications = this.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE).getBoolean("refusedNotifications", false);

            if (!disableNotifications)
            {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.notificationsExplainationTitle)
                        .setMessage(R.string.notificationsExplainationDesc)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                        .show();
                return;
            }
            else
            {
                Toast.makeText(this, R.string.downloadStarted, Toast.LENGTH_SHORT).show();
            }
        }


        Chapter selectedChapter = mangaChapters.get(chapterSelection.getSelectedItemPosition());

        if (checkForNoPages(selectedChapter)) return;

        String chapterID = selectedChapter.getId();

        WorkManager wm = WorkManager.getInstance(this);

        Data.Builder data = new Data.Builder();
        data.putString("Chapter", chapterID);
        data.putString("Manga", selectedManga.getAttributes().getTitleS());
        data.putString("Title", selectedChapter.getAttributes().getFormattedName());

        OneTimeWorkRequest downloadWorkRequest = new OneTimeWorkRequest.Builder(ChapterDownloadWorker.class)
                .addTag(chapterID)
                .setInputData(data.build())
                .build();

        wm.enqueueUniqueWork(chapterID, ExistingWorkPolicy.KEEP, downloadWorkRequest);
    }

    public void ReadChapter(View view) {
        Chapter selectedChapter = mangaChapters.get(chapterSelection.getSelectedItemPosition());

        if (checkForNoPages(selectedChapter)) return;

        ArrayList<Chapter> dummyChapters = new ArrayList<>(mangaChapters);
        ChapterUtilities.FormatChapterList(this, dummyChapters, new ChapterUtilities.FormattingOptions(
                false,
                true,
                selectedChapter.getId()
        ));
        String[] chapterNames = new String[dummyChapters.size()];
        String[] chapterIDs = new String[dummyChapters.size()];
        for (int i = 0; i < dummyChapters.size(); i++) {
            chapterNames[i] = dummyChapters.get(i).getAttributes().getFormattedName();
            chapterIDs[i] = dummyChapters.get(i).getId();
        }

        Intent intent = new Intent(getApplicationContext(), OnlineReaderActivity.class);
        intent.putExtra("chapterNames", chapterNames);
        intent.putExtra("chapterIDs", chapterIDs);
        intent.putExtra("targetChapter", selectedChapter.getId());
        intent.putExtra("mangaID", selectedManga.getId());

        startActivity(intent);
    }

    private boolean checkForNoPages(@NonNull Chapter chapter) {
        String externalUrl = chapter.getAttributes().getExternalUrl();
        if (externalUrl != null) {
            new AlertDialog.Builder(ChapterDownloaderActivity.this)
                    .setTitle(R.string.noPagesDialogTitle)
                    .setMessage(FormattingUtilities.FormatFromHtml(getString(R.string.noPagesDialog, externalUrl)))

                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(externalUrl));
                        startActivity(i);
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        return false;
    }
}