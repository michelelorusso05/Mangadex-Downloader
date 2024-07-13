package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class AdapterRecyclerChapters extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context ct;
    ArrayList<Chapter> chapters;
    ShapeAppearanceModel topCard;
    ShapeAppearanceModel bottomCard;
    ShapeAppearanceModel singleCard;
    ShapeAppearanceModel middleCard;

    private final float PIXELS_TO_DP;
    private final ArrayList<Integer> indexMap;
    private final HashMap<Integer, String> volumeStartIndex;

    private static final int ROW_CHAPTER = 0;
    private static final int ROW_VOLUME_LABEL = 1;
    private final Consumer<String> openURL;
    private final Consumer<Integer> read;
    private final Consumer<Integer> download;
    public AdapterRecyclerChapters(Context _ct, ArrayList<Chapter> _chapters,
                                   Consumer<String> openURL,
                                   Consumer<Integer> read,
                                   Consumer<Integer> download) {
        ct = _ct;
        chapters = _chapters;
        this.openURL = openURL;
        this.read = read;
        this.download = download;

        Resources r = ct.getResources();
        PIXELS_TO_DP = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1,
            r.getDisplayMetrics()
        );

        volumeStartIndex = new HashMap<>();
        indexMap = new ArrayList<>(chapters.size());

        for (int i = 0; i < chapters.size(); i++) {
            String volume = chapters.get(i).getAttributes().getVolume();

            if (i == 0 || !Objects.equals(volume, chapters.get(i - 1).getAttributes().getVolume())) {
                indexMap.add(-1);
                volumeStartIndex.put(indexMap.size() - 1, volume);
            }
            indexMap.add(i);
        }
    }

    public ArrayList<Integer> getIndexMap() {
        return indexMap;
    }

    @Override
    public int getItemViewType(int position) {
        if (indexMap.get(position) == -1)
            return ROW_VOLUME_LABEL;

        return ROW_CHAPTER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ct);
        RecyclerView.ViewHolder holder;

        switch (viewType) {
            case ROW_CHAPTER: {
                View view = inflater.inflate(R.layout.recyclerviewitem_chapter, parent, false);
                ChapterViewHolder chapterViewHolder = new ChapterViewHolder(view);

                chapterViewHolder.downloadButton.setOnClickListener((v) ->
                        download.accept(indexMap.get(chapterViewHolder.getAdapterPosition())));

                chapterViewHolder.readButton.setOnClickListener((v) -> {
                    Chapter c = chapters.get(indexMap.get(chapterViewHolder.getAdapterPosition()));
                    if (c.getAttributes().getExternalUrl() != null)
                        openURL.accept(c.getAttributes().getExternalUrl());
                    else
                        read.accept(indexMap.get(chapterViewHolder.getAdapterPosition()));
                });

                holder = chapterViewHolder;

                // Init custom shape appearence models
                if (topCard == null)
                {
                    ShapeAppearanceModel.Builder model = ((ChapterViewHolder) holder).cardView.getShapeAppearanceModel().toBuilder();

                    middleCard = model.build();
                    model
                            .setTopLeftCornerSize(24 * PIXELS_TO_DP)
                            .setTopRightCornerSize(24 * PIXELS_TO_DP);

                    topCard = model.build();
                    model
                            .setBottomLeftCornerSize(24 * PIXELS_TO_DP)
                            .setBottomRightCornerSize(24 * PIXELS_TO_DP);

                    singleCard = model.build();
                    model
                            .setTopLeftCornerSize(0)
                            .setTopRightCornerSize(0);

                    bottomCard = model.build();
                }
                break;
            }
            case ROW_VOLUME_LABEL: {
                View view = inflater.inflate(R.layout.recyclerviewitem_volume_label, parent, false);
                holder = new LabelViewHolder(view);
                break;
            }
            default:
                throw new IllegalStateException("Only chapter and label rows are supposed to be here.");
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int mappedChapter = indexMap.get(position);

        if (mappedChapter == -1) {
            LabelViewHolder labelViewHolder = (LabelViewHolder) holder;
            String volume = volumeStartIndex.get(position);
            if (volume == null || volume.isEmpty())
                labelViewHolder.volume.setText(R.string.volume_none);
            else if (volume.equalsIgnoreCase("oneshot"))
                labelViewHolder.volume.setText(ct.getString(R.string.chapter_oneshots));
            else
                labelViewHolder.volume.setText(ct.getResources().getString(R.string.volume_numbered, volume));
        }
        else {
            ChapterViewHolder chapterViewHolder = (ChapterViewHolder) holder;

            Chapter chapter = chapters.get(mappedChapter);

            chapterViewHolder.chapterName.setText(chapter.getAttributes().getFormattedName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                chapterViewHolder.chapterName.setTooltipText(chapter.getAttributes().getFormattedName());

            chapterViewHolder.group.setText(chapter.getAttributes().getScanlationGroupString());

            chapterViewHolder.downloadButton.setVisibility(chapter.getAttributes().getExternalUrl() != null ? View.GONE : View.VISIBLE);

            boolean startOfVolume = indexMap.get(position - 1) == -1;
            boolean endOfVolume = (position == indexMap.size() - 1) || indexMap.get(position + 1) == -1;

            if (startOfVolume && endOfVolume) {
                chapterViewHolder.cardView.setShapeAppearanceModel(singleCard);
                chapterViewHolder.divider.setVisibility(View.GONE);
            }
            else if (startOfVolume) {
                chapterViewHolder.cardView.setShapeAppearanceModel(topCard);
                chapterViewHolder.divider.setVisibility(View.GONE);
            }
            else if (endOfVolume) {
                chapterViewHolder.cardView.setShapeAppearanceModel(bottomCard);
                chapterViewHolder.divider.setVisibility(View.VISIBLE);
            }
            else {
                chapterViewHolder.cardView.setShapeAppearanceModel(middleCard);
                chapterViewHolder.divider.setVisibility(View.VISIBLE);
            }

            /*
            String externalURL = chapter.getAttributes().getExternalUrl();
            if (externalURL != null) {
                chapterViewHolder.downloadButton.setVisibility(View.GONE);
                chapterViewHolder.readButton.setOnClickListener((v) -> {
                    openURL.accept(externalURL);
                });
            }
            else {
                chapterViewHolder.downloadButton.setVisibility(View.VISIBLE);
                chapterViewHolder.readButton.setOnClickListener((v) -> {
                    read.accept(mappedChapter);
                });
                chapterViewHolder.downloadButton.setOnClickListener((v) -> {
                    download.accept(mappedChapter);
                });
            }

             */
        }
    }

    @Override
    public int getItemCount() {
        return indexMap.size();
    }

    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView chapterName;
        TextView group;
        LinearLayout rowLayout;
        View divider;
        MaterialCardView cardView;
        MaterialButton downloadButton;
        MaterialButton readButton;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterName = itemView.findViewById(R.id.chapterTitle);
            group = itemView.findViewById(R.id.scanlationGroup);
            rowLayout = itemView.findViewById(R.id.rowLayout);
            divider = itemView.findViewById(R.id.dividerView);
            cardView = itemView.findViewById(R.id.cardView);

            downloadButton = itemView.findViewById(R.id.buttonDownload);
            readButton = itemView.findViewById(R.id.buttonRead);
        }
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {
        TextView volume;
        public LabelViewHolder(@NonNull View itemView) {
            super(itemView);
            volume = itemView.findViewById(R.id.volume);
        }
    }
}
