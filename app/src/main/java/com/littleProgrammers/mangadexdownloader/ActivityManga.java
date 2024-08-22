package com.littleProgrammers.mangadexdownloader;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
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

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.ChapterResults;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.utils.ApiUtils;
import com.littleProgrammers.mangadexdownloader.utils.ChapterUtilities;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;
import com.littleProgrammers.mangadexdownloader.utils.FormattingUtilities;
import com.littleProgrammers.mangadexdownloader.utils.GlideApp;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jp.wasabeef.blurry.Blurry;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class ActivityManga extends AppCompatActivity
{
    // Unique notification id
    public static final int NOTIFICATION_ID = 6271;

    // Manga to download chapters of (passed through intent)
    Manga selectedManga;
    TextView title;
    TextView author;
    ImageView cover;
    MaterialCardView coverContainer;
    Toolbar toolbar;

    DNSClient client;

    boolean markedFavourite;
    boolean shouldRefreshChapters;

    ActivityResultLauncher<String> requestPermissionLauncher;

    Set<String> languages;

    ViewModelChapterList model;
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
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);

        // Set custom toolbar
        toolbar = findViewById(R.id.home_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Retrieve Manga object from args/savedState
        // UI initialization (and recover from previous instance if needed)
        if (savedInstanceState == null) {
            Bundle bundle = getIntent().getExtras();
            assert bundle != null && bundle.containsKey("MangaData");
            selectedManga = CompatUtils.GetParcelable(bundle, "MangaData", Manga.class);
        } else {
            assert savedInstanceState.containsKey("MangaData");
            selectedManga = CompatUtils.GetParcelable(savedInstanceState, "MangaData", Manga.class);
        }

        // Initialize Web client
        client = StaticData.getClient(this);

        title = findViewById(R.id.mangaTitle);
        author = findViewById(R.id.authorView);
        cover = findViewById(R.id.cover);
        coverContainer = findViewById(R.id.coverContainer);

        author.setText(selectedManga.getAttributes().getAuthorString());

        title.setText(ApiUtils.GetMangaTitleString(selectedManga));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            title.setTooltipText(ApiUtils.GetMangaTitleString(selectedManga));

        ViewPager2 pager = findViewById(R.id.fragmentContainer);
        pager.setAdapter(new AdapterFragmentActivityManga(this, selectedManga));
        pager.setOffscreenPageLimit(2);

        TabLayout tabLayout = findViewById(R.id.tabSelection);

        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            tab.setIcon(AdapterFragmentActivityManga.tabIconResIDs[position]);
            tab.setText(AdapterFragmentActivityManga.tabLabelResIDs[position]);
        }).attach();

        GetCover();

        model = new ViewModelProvider(this).get(ViewModelChapterList.class);

        if (model.getUiState().getValue() == null || model.getUiState().getValue().isSearchCompleted() < ViewModelChapterList.ChapterListState.SEARCH_COMPLETED)
            GetMangaChapterList();

        createNotificationChannel();

        HandleInsets();
    }

    protected void HandleInsets() {
        // Move actionbar under notch and chapter selection bar over navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.setMargins(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0);

            v.post(v::requestLayout);
            return insets;
        });

        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        // Additional insets for the landscape variation
        if (landscape) {
            ViewCompat.setOnApplyWindowInsetsListener(coverContainer, (v, insets) -> {
                Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();

                params.leftMargin = CompatUtils.ConvertDpToPixel(16, ActivityManga.this) + systemBarInsets.left;
                params.bottomMargin = CompatUtils.ConvertDpToPixel(16, ActivityManga.this) + systemBarInsets.bottom;

                v.post(v::requestLayout);
                return insets;
            });

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content), (v, insets) -> {
                Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.setMargins(params.leftMargin, params.topMargin, systemBarInsets.right, 0);

                v.post(v::requestLayout);
                return insets;
            });
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("MangaData", selectedManga);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (model.getUiState().getValue() != null && model.getUiState().getValue().isSearchCompleted() >= ViewModelChapterList.ChapterListState.SEARCH_COMPLETED) {
            Pair<String, Boolean> savedBookmark = FavouriteManager.GetBookmarkForFavourite(this, selectedManga.getId());

            ArrayList<Chapter> chapters = model.getUiState().getValue().getMangaChapters();

            int bookmarkFavouriteIndex = 0;

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

            // Update only if necessary
            if (index != model.getUiState().getValue().getBookmarkedIndex())
                runOnUiThread(() -> model.updateBookmark(index));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.chapter_download_toolbar, menu);
        markedFavourite = FavouriteManager.IsFavourite(this, selectedManga.getId());
        menu.getItem(1).setIcon(markedFavourite ? R.drawable.icon_bookmark_saved : R.drawable.icon_bookmark);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.action_favourite) {
            markedFavourite = !markedFavourite;
            item.setIcon(markedFavourite ? R.drawable.icon_bookmark_saved : R.drawable.icon_bookmark);
            if (markedFavourite)
                FavouriteManager.AddFavourite(this, selectedManga.getId());
            else
                FavouriteManager.RemoveFavourite(this, selectedManga.getId());
        }
        else if (item.getItemId() == R.id.action_show_languages) {
            StringBuilder b = new StringBuilder();
            b.append(ApiUtils.GetMangaLangString(this, selectedManga.getAttributes().getOriginalLanguage())).append(getString(R.string.dialogAvailableLanguagesOriginal));

            for (String lang : selectedManga.getAttributes().getAvailableTranslatedLanguages())
                b.append("\n").append(ApiUtils.GetMangaLangString(this, lang));

            new MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.icon_languages)
                    .setTitle(R.string.dialogAvailableLanguagesTitle)
                    .setMessage(b.toString())
                    .setNeutralButton(R.string.dialogAvailableLanguagesOpenSettings, (dialog, which) -> {
                        Bundle opts = new Bundle();
                        opts.putString("highlightSetting", "lang");
                        shouldRefreshChapters = true;
                        startActivity(new Intent(this, ActivitySettings.class), opts);
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .create()
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void GetCover() {
        boolean lowQualityCover = PreferenceManager.getDefaultSharedPreferences(ActivityManga.this).getBoolean("lowQualityCovers", false);
        final String coverUrl = "https://uploads.mangadex.org/covers/" + selectedManga.getId() + "/" + selectedManga.getAttributes().getCoverUrl() + ((lowQualityCover) ? ".256.jpg" : ".512.jpg");

        GlideApp
                .with(this)
                .load(coverUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        ImageView background = findViewById(R.id.coverBackground);
                        if (background != null) {
                            Blurry
                                    .with(ActivityManga.this)
                                    .sampling(2)
                                    .radius(10)
                                    // .async()
                                    .from(((BitmapDrawable) resource).getBitmap())
                                    .into(background);
                        }
                        return false;
                    }
                })
                .into(cover);

        coverContainer.setOnClickListener((v) -> {
            Intent i = new Intent(ActivityManga.this, ActivityCoverViewer.class);
            i.putExtra("mangaID", selectedManga.getId());
            startActivity(i);
        });
    }

    public void GetMangaChapterList() {
        ArrayList<Chapter> chapters = new ArrayList<>();
        languages = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("languagePreference", Set.of("en"));


        StringBuilder langUrl = new StringBuilder();

        boolean available = false;
        for (String lang : languages)
        {
            if (ApiUtils.IsMangaLanguageAvailable(selectedManga, lang))
                available = true;

            langUrl.append("translatedLanguage[]=").append(lang).append("&");
        }
        if (!available)
        {
            model.updateChapterList(chapters, ViewModelChapterList.ChapterListState.SEARCH_COMPLETED, 0);
            return;
        }

        String mangaID = selectedManga.getId();

        StringBuilder baseUrlBuilder = new StringBuilder("https://api.mangadex.org/manga/" + mangaID + "/feed?" + langUrl + "order%5Bvolume%5D=asc&order%5Bchapter%5D=asc&order%5BpublishAt%5D=asc&includes[]=scanlation_group");

        Set<String> ratings = PreferenceManager.getDefaultSharedPreferences(ActivityManga.this).getStringSet("contentFilter", null);

        if (ratings != null) {
            for (String s : ratings) {
                baseUrlBuilder.append("&contentRating[]=").append(s);
            }
        }

        String baseUrl = baseUrlBuilder.toString();

        client.HttpRequestAsync(baseUrl.concat("&limit=250"), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> model.updateChapterList(new ArrayList<>(), ViewModelChapterList.ChapterListState.SEARCH_ERROR, 0));
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
                                runOnUiThread(() -> model.updateChapterList(new ArrayList<>(), ViewModelChapterList.ChapterListState.SEARCH_ERROR, 0));
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

        ChapterUtilities.FormatChapterList(ActivityManga.this, chapters, new ChapterUtilities.FormattingOptions(
                allowDuplicates,
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("hideExternal", true),
                bookmark));

        int bookmarkFavouriteIndex = 0;

        if (!chapters.isEmpty()) {
            // Move the oneshots at the beginning of the array
            int currentIteration = 0;
            while (chapters.get(chapters.size() - 1).getAttributes().getChapter() == null
                    && currentIteration < chapters.size()) {
                Chapter c = chapters.remove(chapters.size() - 1);

                c.getAttributes().setVolume("oneshot");

                chapters.add(0, c);
                currentIteration++;
            }

            // Set bookmark to the first non-oneshot chapter available
            if (currentIteration != chapters.size())
                bookmarkFavouriteIndex = currentIteration;
            // Retrieve bookmark from shared preferences
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
        }

        final int index = bookmarkFavouriteIndex;
        runOnUiThread(() -> model.updateChapterList(chapters, ViewModelChapterList.ChapterListState.SEARCH_COMPLETED, index));
    }

    public void OpenURL(String url) {
        new MaterialAlertDialogBuilder(ActivityManga.this)
                .setTitle(R.string.noPagesDialogTitle)
                .setMessage(FormattingUtilities.FormatFromHtml(ActivityManga.this.getString(R.string.noPagesDialog, url)))

                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    ActivityManga.this.startActivity(i);
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    public void ReadChapter(Integer pos) {
        ViewModelChapterList.ChapterListState m = model.getUiState().getValue();
        assert m != null;

        ArrayList<Chapter> foundChapters = m.getMangaChapters();
        Chapter selectedChapter = foundChapters.get(pos);

        ArrayList<Chapter> dummyChapters = new ArrayList<>(foundChapters);
        ChapterUtilities.FormatChapterList(ActivityManga.this, dummyChapters, new ChapterUtilities.FormattingOptions(
                false,
                true,
                selectedChapter.getId()
        ));
        ArrayList<String> chapterNames = new ArrayList<>(dummyChapters.size());
        ArrayList<String> chapterIDs = new ArrayList<>(dummyChapters.size());
        for (int i = 0; i < dummyChapters.size(); i++) {
            chapterNames.add(dummyChapters.get(i).getAttributes().getFormattedName());
            chapterIDs.add(dummyChapters.get(i).getId());
        }

        Intent intent = new Intent(ActivityManga.this, ActivityOnlineReader.class);
        intent.putExtra("chapterNames", chapterNames);
        intent.putExtra("chapterIDs", chapterIDs);
        intent.putExtra("targetChapter", selectedChapter.getId());
        intent.putExtra("mangaID", selectedManga.getId());

        startActivity(intent);
    }
    public void DownloadChapter(Integer pos) {
        ViewModelChapterList.ChapterListState m = model.getUiState().getValue();
        assert m != null;

        ArrayList<Chapter> foundChapters = m.getMangaChapters();
        Chapter selectedChapter = foundChapters.get(pos);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                ActivityManga.this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            boolean disableNotifications = ActivityManga.this.getSharedPreferences("com.littleProgrammers.mangadexdownloader", Context.MODE_PRIVATE).getBoolean("refusedNotifications", false);

            if (!disableNotifications)
            {
                cachedChapterPos = pos;
                new MaterialAlertDialogBuilder(ActivityManga.this)
                        .setTitle(R.string.notificationsExplainationTitle)
                        .setMessage(R.string.notificationsExplainationDesc)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                        .show();
                return;
            }
            else
            {
                Toast.makeText(ActivityManga.this, R.string.downloadStarted, Toast.LENGTH_SHORT).show();
            }
        }

        if (selectedChapter.getAttributes().getExternalUrl() != null) return;

        String chapterID = selectedChapter.getId();

        WorkManager wm = WorkManager.getInstance(ActivityManga.this);

        Data.Builder data = new Data.Builder();
        data.putString("Chapter", chapterID);
        data.putString("ScanGroup", selectedChapter.getAttributes().getScanlationGroupString());
        data.putString("Manga", ApiUtils.GetMangaTitleString(selectedManga));
        data.putString("Author", selectedManga.getAttributes().getAuthorString());
        data.putString("Artist", selectedManga.getAttributes().getArtistString());
        data.putString("MangaID", selectedManga.getId());
        data.putString("Title", selectedChapter.getAttributes().getFormattedName());
        data.putString("Volume", selectedChapter.getAttributes().getVolume());
        data.putString("ChapterNumber", selectedChapter.getAttributes().getChapter());

        OneTimeWorkRequest downloadWorkRequest = new OneTimeWorkRequest.Builder(WorkerChapterDownload.class)
                .setId(UUID.fromString(chapterID))
                .addTag("chapter")
                .addTag(chapterID)
                .setInputData(data.build())
                .build();

        wm.enqueueUniqueWork(chapterID, ExistingWorkPolicy.KEEP, downloadWorkRequest);
    }
}