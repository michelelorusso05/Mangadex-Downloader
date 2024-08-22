package com.littleProgrammers.mangadexdownloader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.utilities.Blend;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.Tag;
import com.littleProgrammers.mangadexdownloader.utils.ApiUtils;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;

import io.noties.markwon.Markwon;

public class FragmentMangaDescription extends Fragment {
    private Activity context;
    private Manga manga;
    private ExtendedFloatingActionButton readButton;
    IndeterminateDrawable<CircularProgressIndicatorSpec> progressIndicatorDrawable;
    NestedScrollView scrollView;
    boolean isSearching;
    float oldY = 0;
    boolean fabExpanded;
    private boolean chaptersAvailableHint;
    ViewModelChapterList model;
    Markwon markwon;

    public FragmentMangaDescription() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();

        if (savedInstanceState == null)
            savedInstanceState = getArguments();

        assert savedInstanceState != null;

        Manga m = CompatUtils.GetParcelable(savedInstanceState, "Manga", Manga.class);

        if (m != null)
            manga = m;

        model = new ViewModelProvider((ViewModelStoreOwner) context).get(ViewModelChapterList.class);

        markwon = StaticData.getMarkwon(context);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("Manga", manga);
    }

    @SuppressLint({"RestrictedApi", "PrivateResource"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_manga_description, container, false);

        TextView description = view.findViewById(R.id.mangaDescription);

        String desc = ApiUtils.GetMangaDescription(manga);
        if (desc != null && !desc.isEmpty()) {
            markwon.setMarkdown(description, desc);
        }
        else
            description.setVisibility(View.GONE);

        TextView author = view.findViewById(R.id.authorNameView);
        author.setText(manga.getAttributes().getAuthorString());

        TextView artist = view.findViewById(R.id.artistNameView);
        artist.setText(manga.getAttributes().getArtistString());

        Chip pubYear = view.findViewById(R.id.pubYearChip);

        if (manga.getAttributes().getYear() != null && manga.getAttributes().getYear() != 0) {
            pubYear.setText(String.valueOf(manga.getAttributes().getYear()));
            setChipAttributes(pubYear);
        }
        else
            pubYear.setVisibility(View.GONE);

        Chip pubStatus = view.findViewById(R.id.pubStatusChip);
        setChipAttributes(pubStatus);

        pubStatus.setChipIconVisible(true);
        switch (manga.getAttributes().getStatus()) {
            case "completed":
                pubStatus.setChipIconResource(R.drawable.icon_completed);
                pubStatus.setText(R.string.pub_status_completed);
                break;
            case "hiatus":
                pubStatus.setChipIconResource(R.drawable.icon_pending);
                pubStatus.setText(R.string.pub_status_hiatus);
                break;
            case "ongoing":
                pubStatus.setChipIconResource(R.drawable.icon_ongoing);
                pubStatus.setText(R.string.pub_status_ongoing);
                break;
            case "cancelled":
                pubStatus.setChipIconResource(R.drawable.icon_canceled);
                pubStatus.setText(R.string.pub_status_canceled);
                break;
        }

        // Content rating
        Chip contentRating = view.findViewById(R.id.contentRatingChip);
        Pair<Integer, Integer> rating = ApiUtils.GetMangaRatingString(manga.getAttributes().getContentRating());
        contentRating.setText(rating.first);
        setChipAttributes(contentRating);

        contentRating.setChipIconVisible(true);
        contentRating.setChipIconResource(R.drawable.icon_circle);

        int color = Blend.harmonize(
                ContextCompat.getColor(context, rating.second),
                ContextCompat.getColor(context, R.color.md_theme_primary)
        );

        contentRating.setChipIconTint(ColorStateList.valueOf(color));

        ChipGroup genres = view.findViewById(R.id.genresChipGroup);
        ChipGroup themes = view.findViewById(R.id.themesChipGroup);
        Chip formatChip = view.findViewById(R.id.formatChip);
        boolean formatSet = false;
        Chip demographicChip = view.findViewById(R.id.demographicChip);

        String publicationDemographic = manga.getAttributes().getPublicationDemographic();

        setChipAttributes(demographicChip);

        if (publicationDemographic == null || publicationDemographic.isEmpty() || publicationDemographic.equals("null")) {
            view.findViewById(R.id.demographicChipLabel).setVisibility(View.GONE);
            demographicChip.setVisibility(View.GONE);
        }
        else {
            switch (publicationDemographic) {
                case "shounen":
                    demographicChip.setOnClickListener((v) ->
                        new MaterialAlertDialogBuilder(context)
                            .setIcon(R.drawable.icon_shounen)
                            .setTitle(R.string.about_shounen_title)
                            .setMessage(R.string.about_shounen_body)
                            .setPositiveButton(android.R.string.ok, null)
                            .show());
                    break;
                case "shoujo":
                    demographicChip.setOnClickListener((v) ->
                        new MaterialAlertDialogBuilder(context)
                            .setIcon(R.drawable.icon_shoujo)
                            .setTitle(R.string.about_shoujo_title)
                            .setMessage(R.string.about_shoujo_body)
                            .setPositiveButton(android.R.string.ok, null)
                            .show());
                    break;
                case "seinen":
                    demographicChip.setOnClickListener((v) ->
                        new MaterialAlertDialogBuilder(context)
                            .setIcon(R.drawable.icon_seinen)
                            .setTitle(R.string.about_seinen_title)
                            .setMessage(R.string.about_seinen_body)
                            .setPositiveButton(android.R.string.ok, null)
                            .show());
                    break;
                case "josei":
                    demographicChip.setOnClickListener((v) ->
                        new MaterialAlertDialogBuilder(context)
                            .setIcon(R.drawable.icon_josei)
                            .setTitle(R.string.about_josei_title)
                            .setMessage(R.string.about_josei_body)
                            .setPositiveButton(android.R.string.ok, null)
                            .show());
                    break;
            }

            demographicChip.setRippleColor(context.getColorStateList(com.google.android.material.R.color.m3_chip_ripple_color));

            publicationDemographic = publicationDemographic.substring(0, 1).toUpperCase() + publicationDemographic.substring(1);
            demographicChip.setText(publicationDemographic);
        }

        // Other chips are the actual tags
        for (Tag tag : manga.getAttributes().getTags()) {
            Chip tagChip;

            switch (tag.getAttributes().getGroup())
            {
                case "genre":
                    tagChip = (Chip) getLayoutInflater().inflate(R.layout.layout_tag_chip, genres, false);
                    tagChip.setText(ApiUtils.GetTagTranslatedName(requireContext(), tag));
                    setChipAttributes(tagChip);
                    genres.addView(tagChip);
                    break;
                case "theme":
                    tagChip = (Chip) getLayoutInflater().inflate(R.layout.layout_tag_chip, themes, false);
                    tagChip.setText(ApiUtils.GetTagTranslatedName(requireContext(), tag));
                    setChipAttributes(tagChip);
                    themes.addView(tagChip);
                    break;
                case "format":
                    formatSet = true;
                    formatChip.setText(ApiUtils.GetTagTranslatedName(requireContext(), tag));
                    break;
            }
        }

        if (!formatSet) {
            view.findViewById(R.id.formatChipLabel).setVisibility(View.GONE);
            formatChip.setVisibility(View.GONE);
        }

        setChipAttributes(formatChip);

        if (genres.getChildCount() == 0) {
            view.findViewById(R.id.genresChipGenresLabel).setVisibility(View.GONE);
            genres.setVisibility(View.GONE);
        }
        if (themes.getChildCount() == 0) {
            view.findViewById(R.id.themesChipGroupLabel).setVisibility(View.GONE);
            themes.setVisibility(View.GONE);
        }

        final TextView[] linkGroupsLabels = {
                view.findViewById(R.id.readChipGroupLabel),
                view.findViewById(R.id.trackChipGroupLabel)
        };
        final ChipGroup[] linkGroups = {
            view.findViewById(R.id.readChipGroup),
            view.findViewById(R.id.trackChipGroup)
        };

        final String[][] sortedLinkIds = {
                { "raw", "engtl", "bw", "amz", "ebj", "cdj" },
                { "mu", "ap", "al", "kt", "mal", "nu" }
        };

        for (int i = 0; i < 2; i++) {
            boolean set = false;

            for (int j = 0; j < sortedLinkIds[i].length; j++) {
                String key = sortedLinkIds[i][j];
                String slug = manga.getAttributes().getLinks().get(key);

                if (slug == null) continue;

                set = true;

                Chip linkChip = (Chip) getLayoutInflater().inflate(R.layout.layout_tag_chip, linkGroups[i], false);
                setChipAttributes(linkChip);

                ApiUtils.LinkInfo resources = ApiUtils.GetLinkResources(context, key);

                linkChip.setText(resources.name);
                if (resources.icon != null) {
                    linkChip.setChipIconVisible(true);
                    linkChip.setChipIcon(resources.icon);
                }

                final String url = String.format(resources.urlFormat, slug);

                linkChip.setOnClickListener((v) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    context.startActivity(intent);
                });

                linkChip.setRippleColor(context.getColorStateList(com.google.android.material.R.color.m3_chip_ripple_color));

                linkGroups[i].addView(linkChip);
            }

            if (!set) {
                linkGroupsLabels[i].setVisibility(View.GONE);
                linkGroups[i].setVisibility(View.GONE);
            }
        }


        readButton = view.findViewById(R.id.readButton);

        CircularProgressIndicatorSpec spec =
                new CircularProgressIndicatorSpec(requireContext(), null, 0,
                        com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
        progressIndicatorDrawable =
                IndeterminateDrawable.createCircularDrawable(requireContext(), spec);

        readButton.setIcon(progressIndicatorDrawable);

        SetChapterRetrieveState(STATE_SEARCHING);

        fabExpanded = true;

        model.getUiState().observe(getViewLifecycleOwner(), chapterListState -> {
            int state = chapterListState.isSearchCompleted();
            if (state == ViewModelChapterList.ChapterListState.SEARCH_COMPLETED) {
                if (chapterListState.getMangaChapters() == null || chapterListState.getMangaChapters().isEmpty()) {
                    SetChapterRetrieveState(STATE_EMPTY);
                    return;
                }

                int bookmark = chapterListState.getBookmarkedIndex();
                Chapter chapter = chapterListState.getMangaChapters().get(bookmark);

                String url = chapter.getAttributes().getExternalUrl();

                readButton.setOnClickListener((v) -> {
                    if (url != null && !url.isEmpty())
                        ((ActivityManga) context).OpenURL(url);
                    else
                        ((ActivityManga) context).ReadChapter(bookmark);
                });

                if (chapter.getAttributes().getExternalUrl() != null && !chapter.getAttributes().getExternalUrl().isEmpty())
                    readButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.icon_languages));
                else
                    readButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.icon_read));

                if (chapter.getAttributes().getChapter() == null || chapter.getAttributes().getChapter().isEmpty()) {
                    if (chapter.getAttributes().getTitle() == null || chapter.getAttributes().getTitle().isEmpty())
                        readButton.setText(getString(R.string.chapter_oneshot));
                    else
                        readButton.setText(chapter.getAttributes().getTitle());
                }
                else
                    readButton.setText(getString(R.string.noName, chapter.getAttributes().getChapter()));

                SetChapterRetrieveState(STATE_COMPLETE);
            }
            else if (state == ViewModelChapterList.ChapterListState.SEARCH_ERROR) {
                SetChapterRetrieveState(STATE_FAIL);
            }
        });

        scrollView = view.findViewById(R.id.scrollView);

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollView.getScrollY() < oldY || scrollView.getChildAt(0).getHeight() == scrollView.getScrollY() + scrollView.getHeight()) {
                readButton.extend();
            }
            else if (scrollView.getScrollY() > oldY) {
                readButton.shrink();
            }

            oldY = scrollView.getScrollY();
        });

        LinearLayout scrollableContent = view.findViewById(R.id.scrollableContent);

        ViewCompat.setOnApplyWindowInsetsListener(scrollableContent, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                v.setPadding(0, 0, 0, i.bottom + CompatUtils.ConvertDpToPixel(16, context));

                return insets;
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(readButton, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

                params.setMargins(0, 0, params.rightMargin, i.bottom + CompatUtils.ConvertDpToPixel(16, context));

                return insets;
            }
        });

        return view;
    }

    private static void setChipAttributes(Chip chip) {
        chip.setEnsureMinTouchTargetSize(false);
        chip.setFocusable(false);
        chip.setClickable(false);
    }

    private final static int STATE_SEARCHING = 0;
    private final static int STATE_COMPLETE = 1;
    private final static int STATE_FAIL = 2;
    private final static int STATE_EMPTY = 3;
    private void SetChapterRetrieveState(int state) {
        switch (state) {
            case STATE_SEARCHING:
                readButton.setIcon(progressIndicatorDrawable);
                readButton.setText(null);
                readButton.shrink();
                isSearching = true;
                break;
            case STATE_COMPLETE:
                readButton.setClickable(true);
                readButton.setFocusable(true);
                readButton.extend();
                isSearching = false;
                break;
            case STATE_FAIL:
                readButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.icon_no_internet));
                readButton.setText(R.string.err_no_conn_short);
                readButton.extend();
                isSearching = false;
                break;
            case STATE_EMPTY:
                readButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.icon_no_chapters));
                readButton.setText(R.string.err_no_chapters_short);
                readButton.extend();
                isSearching = false;
                break;
        }
    }
}