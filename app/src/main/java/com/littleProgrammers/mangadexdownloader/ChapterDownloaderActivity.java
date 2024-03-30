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
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.ChapterResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaAttributes;
import com.littleProgrammers.mangadexdownloader.utils.ChapterUtilities;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.littleProgrammers.mangadexdownloader.utils.FormattingUtilities;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class ChapterDownloaderActivity extends AppCompatActivity
{

    // Unique notification id
    public static final int NOTIFICATION_ID = 6271;

    // Manga to download chapters of (passed through intent)
    Manga selectedManga;
    TextView title;
    TextView author;
    ImageView cover;

    DNSClient client;

    boolean markedFavourite;
    boolean shouldRefreshChapters;

    ActivityResultLauncher<String> requestPermissionLauncher;

    Set<String> languages;

    ChapterListViewModel model;
    Integer cachedChapterPos;


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
        }
         requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                SharedPreferences.Editor e = this.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE).edit();
                e.putBoolean("refusedNotifications", !isGranted);
                e.apply();

                DownloadChapter(cachedChapterPos);
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getWindow().setDecorFitsSystemWindows(false);
        else
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_download_new);

        // Set custom toolbar
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

            params = (ViewGroup.MarginLayoutParams) findViewById(R.id.fragmentContainer).getLayoutParams();
            params.setMargins(params.getMarginStart(), 0, params.getMarginEnd(),
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom +
                            (int) (16 * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT)));

            return WindowInsetsCompat.CONSUMED;
        });

        // Retrieve Manga object from args/savedState
        // UI initialization (and recover from previous instance if needed)
        if (savedInstanceState == null) {
            Bundle bundle = getIntent().getExtras();
            assert bundle != null && bundle.containsKey("MangaData");
            selectedManga = CompatUtils.GetSerializable(bundle, "MangaData", Manga.class);
        }
        else {
            assert savedInstanceState.containsKey("MangaData");
            selectedManga = CompatUtils.GetSerializable(savedInstanceState, "MangaData", Manga.class);
        }

        // Initialize Web client
        client = StaticData.getClient(this);

        title = findViewById(R.id.mangaTitle);
        author = findViewById(R.id.authorView);
        cover = findViewById(R.id.cover);

        author.setText(selectedManga.getAttributes().getAuthorString());
        author.setMovementMethod(new ScrollingMovementMethod());
        title.setText(FormattingUtilities.FormatFromHtml(selectedManga.getAttributes().getTitleS()));
        ViewPager2 pager = findViewById(R.id.fragmentContainer);
        pager.setAdapter(new MangaViewActivityFragmentManager(this, selectedManga));
        pager.setOffscreenPageLimit(2);

        TabLayout tabLayout = findViewById(R.id.tabSelection);

        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            tab.setIcon(MangaViewActivityFragmentManager.tabIconResIDs[position]);
            tab.setText(MangaViewActivityFragmentManager.tabLabelResIDs[position]);
        }).attach();

        getCover();

        model = new ViewModelProvider(this).get(ChapterListViewModel.class);

        if (model.getUiState().getValue() == null || model.getUiState().getValue().isSearchCompleted() != ChapterListViewModel.ChapterListState.SEARCH_COMPLETED)
            GetMangaChapterList();

        createNotificationChannel();
    }

    /*
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ViewPager2 fragmentContainer = findViewById(R.id.fragmentContainer);
        fragmentContainer.post(() -> {
            int pixels = fragmentContainer.getHeight();
            float dp = CompatUtils.convertPixelsToDp(pixels, this);

            if (dp < 64)
                findViewById(R.id.tabSelection).setVisibility(View.GONE);
        });
    }
    */

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("MangaData", selectedManga);
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

    private void getCover() {
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

            StaticData.sharedCover = bm;

            cover.setImageBitmap(bm);
            ImageView background = findViewById(R.id.coverBackground);
            if (background != null)
                background.setImageBitmap(bm);
        }), ReaderActivity.opt);
    }

    public void GetMangaChapterList() {
        ArrayList<Chapter> chapters = new ArrayList<>();
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
            model.updateChapterList(chapters, ChapterListViewModel.ChapterListState.SEARCH_COMPLETED, 0);
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> model.updateChapterList(new ArrayList<>(), ChapterListViewModel.ChapterListState.SEARCH_ERROR, 0));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ChapterResults cResults = StaticData.getMapper().readValue(Objects.requireNonNull(response.body()).string(), ChapterResults.class);
                final int totalChapters = cResults.getTotal();

                // We can't fit all the chapters in one request
                if (totalChapters > 250) {
                    chapters.addAll(Arrays.asList(new Chapter[totalChapters]));

                    // Copy the first 250 elements
                    for (int i = 0; i < 250; i++)
                        chapters.set(i, cResults.getData()[i]);

                    // Query the others
                    final int[] remainingChapters = {totalChapters - 250};
                    for (int i = 250; i < totalChapters; i += 250) {
                        final int offset = i;
                        client.HttpRequestAsync(baseUrl.concat("&limit=250&offset=").concat(String.valueOf(i)), new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> model.updateChapterList(new ArrayList<>(), ChapterListViewModel.ChapterListState.SEARCH_ERROR, 0));
                            }
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                ChapterResults _cResults = StaticData.getMapper().readValue(Objects.requireNonNull(response.body()).string(), ChapterResults.class);
                                // Copy the retrieved elements
                                for (int j = 0; j < _cResults.getData().length; j++)
                                    chapters.set(offset + j, _cResults.getData()[j]);

                                remainingChapters[0] -= 250;
                                if (remainingChapters[0] < 0)
                                    OnChapterRetrievingEnd(chapters);
                            }
                        }, false);
                    }
                }
                else {
                    chapters.addAll(Arrays.asList(cResults.getData()));
                    OnChapterRetrievingEnd(chapters);
                }
            }
        }, false);
    }

    private void OnChapterRetrievingEnd(ArrayList<Chapter> chapters) {
        Pair<String, Boolean> savedBookmark = FavouriteManager.GetBookmarkForFavourite(this, selectedManga.getId());
        String bookmark = (savedBookmark != null) ? savedBookmark.first : null;

        boolean allowDuplicates = ((languages.size() > 1) || (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("chapterDuplicate", true)));

        ChapterUtilities.FormatChapterList(ChapterDownloaderActivity.this, chapters, new ChapterUtilities.FormattingOptions(
                allowDuplicates,
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("hideExternal", true),
                bookmark));

        // Move the oneshots at the beginning of the array
        int currentIteration = 0;
        while (chapters.get(chapters.size() - 1).getAttributes().getChapter() == null
                && currentIteration < chapters.size()) {
            Chapter c = chapters.remove(chapters.size() - 1);
            chapters.add(0, c);
            currentIteration++;
        }

        int bookmarkFavouriteIndex = 0;

        if (currentIteration != chapters.size())
            bookmarkFavouriteIndex = currentIteration;

        if (savedBookmark != null) {
            final boolean queueNext = savedBookmark.second;
            for (int i = 0; i < chapters.size(); i++) {
                if (chapters.get(i).getId().equals(savedBookmark.first)) {
                    if (queueNext && i < chapters.size() - 1)
                        bookmarkFavouriteIndex = i + 1;
                    else
                        bookmarkFavouriteIndex = i;
                    break;
                }
            }
        }

        final int index = bookmarkFavouriteIndex;
        runOnUiThread(() -> model.updateChapterList(chapters, ChapterListViewModel.ChapterListState.SEARCH_COMPLETED, index));
    }

    public void OpenURL(String url) {
        new MaterialAlertDialogBuilder(ChapterDownloaderActivity.this)
                .setTitle(R.string.noPagesDialogTitle)
                .setMessage(FormattingUtilities.FormatFromHtml(ChapterDownloaderActivity.this.getString(R.string.noPagesDialog, url)))

                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    ChapterDownloaderActivity.this.startActivity(i);
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    public void ReadChapter(Integer pos) {
        ChapterListViewModel.ChapterListState m = model.getUiState().getValue();
        assert m != null;

        ArrayList<Chapter> foundChapters = m.getMangaChapters();
        Chapter selectedChapter = foundChapters.get(pos);

        ArrayList<Chapter> dummyChapters = new ArrayList<>(foundChapters);
        ChapterUtilities.FormatChapterList(ChapterDownloaderActivity.this, dummyChapters, new ChapterUtilities.FormattingOptions(
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

        Intent intent = new Intent(ChapterDownloaderActivity.this, OnlineReaderActivity.class);
        intent.putExtra("chapterNames", chapterNames);
        intent.putExtra("chapterIDs", chapterIDs);
        intent.putExtra("targetChapter", selectedChapter.getId());
        intent.putExtra("mangaID", selectedManga.getId());

        startActivity(intent);
    }
    public void DownloadChapter(Integer pos) {
        ChapterListViewModel.ChapterListState m = model.getUiState().getValue();
        assert m != null;

        ArrayList<Chapter> foundChapters = m.getMangaChapters();
        Chapter selectedChapter = foundChapters.get(pos);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                ChapterDownloaderActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            boolean disableNotifications = ChapterDownloaderActivity.this.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE).getBoolean("refusedNotifications", false);

            if (!disableNotifications)
            {
                cachedChapterPos = pos;
                new MaterialAlertDialogBuilder(ChapterDownloaderActivity.this)
                        .setTitle(R.string.notificationsExplainationTitle)
                        .setMessage(R.string.notificationsExplainationDesc)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                        .show();
                return;
            }
            else
            {
                Toast.makeText(ChapterDownloaderActivity.this, R.string.downloadStarted, Toast.LENGTH_SHORT).show();
            }
        }

        if (selectedChapter.getAttributes().getExternalUrl() != null) return;

        String chapterID = selectedChapter.getId();

        WorkManager wm = WorkManager.getInstance(ChapterDownloaderActivity.this);

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
}