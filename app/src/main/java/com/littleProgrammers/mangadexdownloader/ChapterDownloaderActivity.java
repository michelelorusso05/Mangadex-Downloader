package com.littleProgrammers.mangadexdownloader;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.transition.Fade;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.ChapterResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaAttributes;
import com.littleProgrammers.mangadexdownloader.apiResults.Tag;
import com.littleProgrammers.mangadexdownloader.utils.ChapterUtilities;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.littleProgrammers.mangadexdownloader.utils.FormattingUtilities;
import com.michelelorusso.dnsclient.DNSClient;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

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

    ImageButton downloadButton, readButton;
    View continueReading;
    ChipGroup tags;

    DNSClient client;

    boolean markedFavourite;
    int bookmarkFavouriteIndex = -1;
    boolean shouldRefreshChapters;

    ActivityResultLauncher<String> requestPermissionLauncher;

    Set<String> languages;


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

        Fade fade = new Fade();

        getWindow().setSharedElementsUseOverlay(false);

        // Set an enter transition
        getWindow().setEnterTransition(new Fade());
        getWindow().setSharedElementExitTransition(fade);
        // Set an exit transition
        getWindow().setExitTransition(null);

        // Make status bar transparent
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_download);

        client = new DNSClient(DNSClient.PresetDNS.CLOUDFLARE);

        // UI binding
        title = findViewById(R.id.mangaTitle);
        author = findViewById(R.id.authorView);
        description = findViewById(R.id.mangaDescription);
        cover = findViewById(R.id.cover);
        chapterSelection = findViewById(R.id.chapterSelection);
        continueReading = findViewById(R.id.continueReading);
        tags = findViewById(R.id.tags);

        downloadButton = findViewById(R.id.buttonDownload);
        downloadButton.setEnabled(false);

        readButton = findViewById(R.id.buttonRead);
        readButton.setEnabled(false);

        Toolbar t = findViewById(R.id.home_toolbar);
        setSupportActionBar(t);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Move actionbar under notch and chapter selection bar over navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(t, (v, insets) -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) t.getLayoutParams();
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            params.setMargins(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0);

            params = (ViewGroup.MarginLayoutParams) findViewById(R.id.linearLayout).getLayoutParams();
            params.setMargins(params.getMarginStart(), 0, params.getMarginEnd(),
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom +
                            (int) (16 * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT)));

            return WindowInsetsCompat.CONSUMED;
        });

        ChapterSelectionAdapter adapter = new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                new Pair<>(getString(R.string.fetchingString), getString(R.string.wait)));
        chapterSelection.setAdapter(adapter);
        chapterSelection.setEnabled(false);

        chapterSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mangaChapters.size() == 0) return;
                if (mangaChapters.get(position).getAttributes().getExternalUrl() != null)
                    downloadButton.setVisibility(View.GONE);
                else
                    downloadButton.setVisibility(View.VISIBLE);
                if (position != bookmarkFavouriteIndex)
                    continueReading.setVisibility(View.GONE);
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
            // Set author, name and description
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

            // First chip is the content rating
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, tags, false);
            Pair<Integer, Integer> rating = MangaAttributes.getRatingString(selectedManga.getAttributes().getContentRating());
            chip.setText(rating.first);
            chip.setFocusable(false);
            chip.setChipStrokeColorResource(rating.second);
            chip.setTextColor(ContextCompat.getColor(this, rating.second));
            tags.addView(chip);

            // Other chips are the actual tags
            for (Tag tag : selectedManga.getAttributes().getTags()) {
                Chip tagChip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, tags, false);
                tagChip.setText(tag.getAttributes().getName().get("en"));
                tagChip.setFocusable(false);
                tags.addView(tagChip);
            }

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
        menu.getItem(1).setIcon(markedFavourite ? R.drawable.ic_baseline_bookmark_24 : R.drawable.ic_baseline_bookmark_disabled_24);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // supportFinishAfterTransition();
            finish();
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
        else if (item.getItemId() == R.id.action_show_languages) {

            StringBuilder b = new StringBuilder();
            b.append("\n- ").append(MangaAttributes.getLangString(this, selectedManga.getAttributes().getOriginalLanguage())).append(getString(R.string.dialogAvailableLanguagesOriginal));

            for (String lang : selectedManga.getAttributes().getAvailableTranslatedLanguages())
                b.append("\n- ").append(MangaAttributes.getLangString(this, lang));

            new MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_baseline_public_24)
                    .setTitle(R.string.dialogAvailableLanguagesTitle)
                    .setMessage(b.toString())
                    .setNeutralButton(R.string.dialogAvailableLanguagesOpenSettings, (dialog, which) -> {
                        Bundle opts = new Bundle();
                        opts.putString("highlightSetting", "lang");
                        shouldRefreshChapters = true;
                        startActivity(new Intent(this, SettingsActivity.class), opts);
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .create()
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (shouldRefreshChapters)
        {
            try {
                getMangaChapterList();
            } catch (JSONException | InterruptedException e) {
                e.printStackTrace();
            }
            shouldRefreshChapters = false;
        }

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

        client.GetImageBitmapAsync(coverUrl, (bm, success) -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, R.string.errNoConnection, Toast.LENGTH_SHORT).show();
                return;
            }
            cover.setImageBitmap(bm);
            ImageView background = findViewById(R.id.coverBackground);
            if (background != null)
                background.setImageBitmap(bm);
        }));
    }

    public void getMangaChapterList() throws JSONException, InterruptedException {
        languages = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("languagePreference", null);
        assert languages != null;

        StringBuilder langUrl = new StringBuilder();

        boolean available = false;
        for (String lang : languages)
        {
            if (selectedManga.getAttributes().isLanguageAvailable(lang))
                available = true;

            langUrl.append("translatedLanguage[]=").append(lang).append("&");
        }
        if (!available)
        {
            chapterSelection.setAdapter(new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                    new Pair<>(getString(R.string.mangaNoEntries), getString(R.string.mangaNoEntriesSubtext))));
            downloadButton.setVisibility(View.GONE);
            readButton.setVisibility(View.GONE);
            return;
        }

        String mangaID = selectedManga.getId();

        StringBuilder baseUrlBuilder = new StringBuilder("https://api.mangadex.org/manga/" + mangaID + "/feed?" + langUrl + "order%5Bvolume%5D=asc&order%5Bchapter%5D=asc&order%5BpublishAt%5D=asc&includes[]=scanlation_group");

        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(ChapterDownloaderActivity.this).getStringSet("contentFilter", null);

        if (ratings != null) {
            for (String s : ratings) {
                baseUrlBuilder.append("&contentRating[]=").append(s);
            }
        }

        String baseUrl = baseUrlBuilder.toString();

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

        boolean allowDuplicates = languages.size() > 1 || !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("chapterDuplicate", true);

        ChapterUtilities.FormatChapterList(ChapterDownloaderActivity.this, mangaChapters, new ChapterUtilities.FormattingOptions(
                allowDuplicates,
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("hideExternal", true),
                bookmark));

        if (mangaChapters.size() == 0) {
            runOnUiThread(() -> {
                chapterSelection.setAdapter(new ChapterSelectionAdapter(ChapterDownloaderActivity.this,
                        new Pair<>(getString(R.string.mangaNoEntriesFilter), getString(R.string.mangaNoEntriesSubtext))));
                downloadButton.setVisibility(View.GONE);
                readButton.setVisibility(View.GONE);
            });
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

            downloadButton.setEnabled(true);
            readButton.setEnabled(true);
            chapterSelection.setEnabled(true);
        });
    }

    public void DownloadChapter(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            boolean disableNotifications = this.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE).getBoolean("refusedNotifications", false);

            if (!disableNotifications)
            {
                new MaterialAlertDialogBuilder(this)
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
            new MaterialAlertDialogBuilder(ChapterDownloaderActivity.this)
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