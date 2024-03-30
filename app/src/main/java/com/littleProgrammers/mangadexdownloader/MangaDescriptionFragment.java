package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaAttributes;
import com.littleProgrammers.mangadexdownloader.apiResults.Tag;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;

public class MangaDescriptionFragment extends Fragment {
    private Activity context;
    private Manga manga;
    private TextView chapterName;
    private TextView chapterGroup;
    private View clickableLayout;
    private ImageButton readIcon;
    private boolean chaptersAvailableHint;
    ChapterListViewModel model;

    public MangaDescriptionFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();

        if (savedInstanceState == null)
            savedInstanceState = getArguments();

        assert savedInstanceState != null;

        Manga m = CompatUtils.GetSerializable(savedInstanceState, "Manga", Manga.class);

        if (m != null)
            manga = m;

        model = new ViewModelProvider((ViewModelStoreOwner) context).get(ChapterListViewModel.class);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("Manga", manga);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_manga_description, container, false);

        TextView description = view.findViewById(R.id.mangaDescription);

        Spanned desc = manga.getCanonicalDescription();
        if (desc != null)
            description.setText(desc);

        description.setMovementMethod(new LinkMovementMethod());

        TextView author = view.findViewById(R.id.authorNameView);
        author.setText(manga.getAttributes().getAuthorString());

        TextView artist = view.findViewById(R.id.artistNameView);
        artist.setText(manga.getAttributes().getArtistString());

        // Content rating
        Chip contentRating = view.findViewById(R.id.contentRatingChip);
        Pair<Integer, Integer> rating = MangaAttributes.getRatingString(manga.getAttributes().getContentRating());
        contentRating.setText(rating.first);
        setChipAttributes(contentRating);
        contentRating.setTextColor(ContextCompat.getColor(requireContext(), rating.second));

        ChipGroup genres = view.findViewById(R.id.genresChipGroup);
        ChipGroup themes = view.findViewById(R.id.themesChipGroup);
        Chip formatChip = view.findViewById(R.id.formatChip);
        boolean formatSet = false;
        Chip demographicChip = view.findViewById(R.id.demographicChip);

        String publicationDemographic = manga.getAttributes().getPublicationDemographic();

        if (publicationDemographic == null || publicationDemographic.isEmpty() || publicationDemographic.equals("null"))
            demographicChip.setText(R.string.valueNotSpecified);
        else {
            publicationDemographic = publicationDemographic.substring(0, 1).toUpperCase() + publicationDemographic.substring(1);
            demographicChip.setText(publicationDemographic);
        }

        setChipAttributes(demographicChip);

        // Other chips are the actual tags
        for (Tag tag : manga.getAttributes().getTags()) {
            Chip tagChip;

            switch (tag.getAttributes().getGroup())
            {
                case "genre":
                    tagChip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, genres, false);
                    tagChip.setText(tag.getTranslatedName(requireContext()));
                    setChipAttributes(tagChip);
                    genres.addView(tagChip);
                    break;
                case "theme":
                    tagChip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, themes, false);
                    tagChip.setText(tag.getTranslatedName(requireContext()));
                    setChipAttributes(tagChip);
                    themes.addView(tagChip);
                    break;
                case "format":
                    formatSet = true;
                    formatChip.setText(tag.getTranslatedName(requireContext()));
                    break;
            }
        }

        if (!formatSet)
            formatChip.setText(R.string.valueNotSpecified);

        setChipAttributes(formatChip);

        if (genres.getChildCount() == 0) {
            Chip tagChip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, genres, false);
            tagChip.setText(R.string.valueEmpty);
            setChipAttributes(tagChip);
            genres.addView(tagChip);
        }
        if (themes.getChildCount() == 0) {
            Chip tagChip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, themes, false);
            tagChip.setText(R.string.valueEmpty);
            setChipAttributes(tagChip);
            themes.addView(tagChip);
        }

        chapterName = view.findViewById(R.id.chapterTitle);
        chapterGroup = view.findViewById(R.id.scanlationGroup);
        clickableLayout = view.findViewById(R.id.clickableLayout);
        readIcon = view.findViewById(R.id.readIcon);

        readIcon.setEnabled(false);

        model.getUiState().observe(getViewLifecycleOwner(), chapterListState -> {
            int state = chapterListState.isSearchCompleted();
            if (state == ChapterListViewModel.ChapterListState.SEARCH_COMPLETED) {
                if (chapterListState.getMangaChapters() == null || chapterListState.getMangaChapters().isEmpty()) {
                    SetChapterRetrieveState(STATE_EMPTY);
                    return;
                }

                int bookmark = chapterListState.getBookmarkedIndex();
                Chapter chapter = chapterListState.getMangaChapters().get(bookmark);

                chapterName.setText(chapter.getAttributes().getFormattedName());
                chapterGroup.setText(chapter.getAttributes().getScanlationGroupString());

                String url = chapter.getAttributes().getExternalUrl();

                clickableLayout.setOnClickListener((v) -> {
                    if (url != null && !url.isEmpty())
                        ((ChapterDownloaderActivity) context).OpenURL(url);
                    else
                        ((ChapterDownloaderActivity) context).ReadChapter(bookmark);
                });

                SetChapterRetrieveState(STATE_COMPLETE);

            }
            else if (state == ChapterListViewModel.ChapterListState.SEARCH_ERROR) {
                SetChapterRetrieveState(STATE_FAIL);
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
                chapterName.setText(R.string.searchLoading);
                chapterGroup.setText(R.string.wait);
                clickableLayout.setClickable(false);
                break;
            case STATE_COMPLETE:
                readIcon.setEnabled(true);
                readIcon.setClickable(false);
                break;
            case STATE_FAIL:
                chapterName.setText(R.string.errNoConnection);
                chapterGroup.setText(R.string.mangaNoEntriesSubtext);
                clickableLayout.setClickable(false);
                break;
            case STATE_EMPTY:
                chapterName.setText(R.string.mangaNoEntriesFilter);
                chapterGroup.setText(R.string.mangaNoEntriesSubtext);
                clickableLayout.setClickable(false);
                break;
        }
    }
}