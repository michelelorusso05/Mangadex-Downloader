package com.littleProgrammers.mangadexdownloader;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.AtHomeResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.ChapterResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.michelelorusso.dnsclient.DNSClient;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
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

    DNSClient client = new DNSClient(DNSClient.PresetDNS.GOOGLE);

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        // UI binding
        title = findViewById(R.id.mangaTitle);
        author = findViewById(R.id.authorView);
        description = findViewById(R.id.mangaDescription);
        cover = findViewById(R.id.cover);
        chapterSelection = findViewById(R.id.chapterSelection);
        downloadButton = findViewById(R.id.buttonDownload);
        readButton = findViewById(R.id.buttonRead);
        progressBar = findViewById(R.id.readLoadingBar);

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
            title.setText(FormattingUtilities.FormatFromHtml(selectedManga.getAttributes().getTitleS()));
            String descriptionString = selectedManga.getAttributes().getDescriptionS();
            if (!descriptionString.isEmpty())
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

    private void getCover() throws JSONException {
        boolean lowQualityCover = PreferenceManager.getDefaultSharedPreferences(ChapterDownloaderActivity.this).getBoolean("lowQualityCovers", false);
        final String coverUrl = "https://uploads.mangadex.org/covers/" + selectedManga.getId() + "/" + selectedManga.getAttributes().getCoverUrl() + ((lowQualityCover) ? ".512.jpg" : "");

        client.ImageIntoViewAsync(coverUrl, cover, ChapterDownloaderActivity.this);
    }

    @NonNull
    private String formatTitle(String title) {
        return (title == null || title.isEmpty()) ? getString(R.string.noName).concat(" ") : title;
    }
    @NonNull
    private String formatChapter(String chapter) {
        return (chapter == null) ? "" : chapter.concat(" - ");
    }

    public void getMangaChapterList() throws JSONException, InterruptedException {
        final String lang = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("chapterLanguages", "en");
        if (!selectedManga.getAttributes().isLanguageAvailable(lang)) {
            chapterSelection.setAdapter(new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                    new Pair<>(getString(R.string.mangaNoEntries), getString(R.string.mangaNoEntriesSubtext))));
            return;
        }

        String mangaID = selectedManga.getId();

        ChapterSelectionAdapter adapter = new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                new Pair<>(getString(R.string.fetchingString), getString(R.string.wait)));
        chapterSelection.setAdapter(adapter);

        String baseUrl = "https://api.mangadex.org/manga/" + mangaID + "/feed?translatedLanguage[]=" + lang + "&order%5Bvolume%5D=asc&order%5Bchapter%5D=asc&order%5BpublishAt%5D=asc&includes[]=scanlation_group";
        client.AsyncHttpRequest(baseUrl.concat("&limit=250"), new Callback() {
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
                        client.AsyncHttpRequest(baseUrl.concat("&limit=250&offset=").concat(String.valueOf(i)), new Callback() {
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
        boolean allowDuplicate = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("chapterDuplicate", true);

        Chapter previous;
        Chapter current = null;

        for (ListIterator<Chapter> iterator = mangaChapters.listIterator(); iterator.hasNext();) {
            previous = current;
            current = iterator.next();

            // Format title
            String currentChapter = current.getAttributes().getChapter();

            // Add chapter if it's the first one in the list, if allowDuplicate is set to true, if currentChapter is null (it means that the current chapter is a oneshot) or if it's different from the last one
            if (currentChapter == null || allowDuplicate || previous == null ||
                    !currentChapter.equals(previous.getAttributes().getChapter())) {
                String currentTitle = formatTitle(current.getAttributes().getTitle());
                String fName = formatChapter(currentChapter) + currentTitle;
                current.getAttributes().setFormattedName(fName);
                for (Relationship r : current.getRelationships()) {
                    if (r.getType().equals("scanlation_group")) {
                        current.getAttributes().setScanlationGroupString(r.getAttributes().get("name").textValue());
                        break;
                    }
                }
                if (current.getAttributes().getScanlationGroupString() == null)
                    current.getAttributes().setScanlationGroupString("-");
            }
            else
                iterator.remove();
        }

        // Move the oneshots at the beginning of the array
        int currentIteration = 0;
        while (mangaChapters.get(mangaChapters.size() - 1).getAttributes().getChapter() == null
                && currentIteration < mangaChapters.size()) {
            Chapter c = mangaChapters.remove(mangaChapters.size() - 1);
            mangaChapters.add(0, c);
            currentIteration++;
        }

        ChapterDownloaderActivity.this.runOnUiThread(() -> {
            ChapterSelectionAdapter adapter = new ChapterSelectionAdapter(ChapterDownloaderActivity.this, mangaChapters.toArray(new Chapter[0]));
            chapterSelection.setAdapter(adapter);
            chapterSelection.setSelection(mangaChapters.size() - 1);
            findViewById(R.id.buttonDownload).setEnabled(true);
            findViewById(R.id.buttonRead).setEnabled(true);
        });
    }

    public void downloadChapter (View view) {
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

    public void readChapter (View view) {
        Chapter selectedChapter = mangaChapters.get(chapterSelection.getSelectedItemPosition());

        if (checkForNoPages(selectedChapter)) return;

        String chapterID = selectedChapter.getId();

        readButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        client.AsyncHttpRequest("https://api.mangadex.org/at-home/server/" + chapterID, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), R.string.errNoConnection, Toast.LENGTH_SHORT).show();
                        readButton.setEnabled(true);
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    AtHomeResults hResults = mapper.readValue(Objects.requireNonNull(response.body()).string(), AtHomeResults.class);

                    String baseUrl;
                    String[] images;

                    boolean HQ = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dataSaver", false);
                    if (HQ) {
                        baseUrl = hResults.getBaseUrl() + "/data/";
                        images = hResults.getChapter().getData();
                    }
                    else {
                        baseUrl = hResults.getBaseUrl() + "/data-saver/";
                        images = hResults.getChapter().getDataSaver();
                    }

                    baseUrl += hResults.getChapter().getHash();
                    Intent intent = new Intent(getApplicationContext(), ReaderActivity.class);
                    intent.putExtra("baseUrl", baseUrl);
                    intent.putExtra("urls", images);

                    startActivity(intent);

                    runOnUiThread(() -> {
                        readButton.setEnabled(true);
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                }
        });

    }

    private boolean checkForNoPages(@NonNull Chapter chapter) {
        String externalUrl = chapter.getAttributes().getExternalUrl();
        if (externalUrl != null) {
            new AlertDialog.Builder(ChapterDownloaderActivity.this)
                    .setTitle(R.string.noPagesDialogTitle)
                    .setMessage(FormattingUtilities.FormatFromHtml(getString(R.string.noPagesDialog, externalUrl)))

                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(externalUrl));
                        startActivity(i);
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        }
        return false;
    }
}