package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AdapterRecyclerChapters extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final Context ct;
    final ArrayList<Chapter> chapters;
    ShapeAppearanceModel topCard;
    ShapeAppearanceModel bottomCard;
    ShapeAppearanceModel singleCard;
    ShapeAppearanceModel middleCard;

    private final float PIXELS_TO_DP;
    private final ArrayList<Integer> indexMap;
    private final HashMap<Integer, String> volumeStartIndex;
    private final HashMap<String, ChapterDownloadProgressWrapper> downloadProgressMap;

    private static final String UPDATE_PAYLOAD = "update_payload";

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
        downloadProgressMap = new HashMap<>(chapters.size());

        for (int i = 0; i < chapters.size(); i++) {
            String volume = chapters.get(i).getAttributes().getVolume();

            if (i == 0 || !Objects.equals(volume, chapters.get(i - 1).getAttributes().getVolume())) {
                indexMap.add(-1);
                volumeStartIndex.put(indexMap.size() - 1, volume);
            }

            downloadProgressMap.put(chapters.get(i).getId(), new ChapterDownloadProgressWrapper(indexMap.size()));
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
                View view = inflater.inflate(R.layout.item_recycler_chapter, parent, false);
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
                View view = inflater.inflate(R.layout.item_recycler_label, parent, false);
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

            ChapterDownloadProgressWrapper progress = downloadProgressMap.get(chapter.getId());
            assert progress != null;

            switch (progress.state) {
                case -1:
                    chapterViewHolder.downloadButton.setEnabled(true);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_download);
                    chapterViewHolder.progressIndicator.setVisibility(View.INVISIBLE);
                    break;
                case WorkerChapterDownload.PROGRESS_ONGOING:
                    chapterViewHolder.downloadButton.setEnabled(false);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_download);
                    chapterViewHolder.progressIndicator.setVisibility(View.VISIBLE);
                    if (progress.progress < 0) {
                        chapterViewHolder.progressIndicator.setProgress(0, false);
                        chapterViewHolder.progressIndicator.setIndeterminate(true);
                    }
                    else {
                        chapterViewHolder.progressIndicator.setIndeterminate(false);
                        chapterViewHolder.progressIndicator.setProgress((int) progress.progress, false);
                    }
                    break;
                case WorkerChapterDownload.PROGRESS_SUCCESS:
                    chapterViewHolder.downloadButton.setEnabled(true);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_download_completed);
                    chapterViewHolder.progressIndicator.setVisibility(View.INVISIBLE);
                    break;
                case WorkerChapterDownload.PROGRESS_FAILURE:
                    chapterViewHolder.downloadButton.setEnabled(true);
                    chapterViewHolder.progressIndicator.setIndeterminate(false);
                    chapterViewHolder.progressIndicator.setVisibility(View.VISIBLE);
                    chapterViewHolder.progressIndicator.setProgress((int) progress.progress, false);
                    break;
                case WorkerChapterDownload.PROGRESS_ENQUEUED:
                    chapterViewHolder.downloadButton.setEnabled(false);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_pending);
                    chapterViewHolder.progressIndicator.setVisibility(View.VISIBLE);
                    chapterViewHolder.progressIndicator.setIndeterminate(true);
                    break;
            }

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
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        int mappedChapter = indexMap.get(position);

        if (payloads.contains(UPDATE_PAYLOAD) && mappedChapter != -1) {
            ChapterViewHolder chapterViewHolder = (ChapterViewHolder) holder;

            Chapter chapter = chapters.get(mappedChapter);

            ChapterDownloadProgressWrapper progress = downloadProgressMap.get(chapter.getId());
            assert progress != null;

            switch (progress.state) {
                case -1:
                case WorkerChapterDownload.PROGRESS_CANCELED:
                    chapterViewHolder.downloadButton.setEnabled(true);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_download);
                    chapterViewHolder.progressIndicator.setVisibility(View.INVISIBLE);
                    break;
                case WorkerChapterDownload.PROGRESS_ONGOING:
                    chapterViewHolder.downloadButton.setEnabled(false);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_download);
                    chapterViewHolder.progressIndicator.show();
                    if (progress.progress < 0) {
                        chapterViewHolder.progressIndicator.setProgress(0, false);
                        chapterViewHolder.progressIndicator.setIndeterminate(true);
                    }
                    else {
                        chapterViewHolder.progressIndicator.setIndeterminate(false);
                        chapterViewHolder.progressIndicator.setProgress((int) progress.progress, true);
                    }
                    break;
                case WorkerChapterDownload.PROGRESS_SUCCESS:
                    chapterViewHolder.downloadButton.setEnabled(true);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_download_completed);
                    chapterViewHolder.progressIndicator.hide();
                    break;
                case WorkerChapterDownload.PROGRESS_FAILURE:
                    chapterViewHolder.downloadButton.setEnabled(true);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_retry);
                    chapterViewHolder.downloadButton.setIconTintResource(R.color.md_theme_error);
                    chapterViewHolder.progressIndicator.setIndeterminate(false);
                    chapterViewHolder.progressIndicator.setVisibility(View.VISIBLE);
                    chapterViewHolder.progressIndicator.setProgress((int) progress.progress, false);
                    chapterViewHolder.progressIndicator.setIndicatorColor(ct.getColor(R.color.md_theme_error));
                    break;
                case WorkerChapterDownload.PROGRESS_ENQUEUED:
                    chapterViewHolder.downloadButton.setEnabled(false);
                    chapterViewHolder.downloadButton.setIconResource(R.drawable.icon_pending);
                    chapterViewHolder.progressIndicator.show();
                    chapterViewHolder.progressIndicator.setIndeterminate(true);
                    break;
            }
        }
        else
            onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return indexMap.size();
    }

    public boolean HasChapter(String id) {
        return downloadProgressMap.containsKey(id);
    }

    public void UpdateProgress(String chapterId, int state, float progress) {
        ChapterDownloadProgressWrapper p = downloadProgressMap.get(chapterId);

        if (p == null) return;

        p.state = state;

        if (progress != -2)
            p.progress = progress;

        notifyItemChanged(p.pos, UPDATE_PAYLOAD);
    }

    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        final TextView chapterName;
        final TextView group;
        final LinearLayout rowLayout;
        final View divider;
        final MaterialCardView cardView;
        final MaterialButton downloadButton;
        final MaterialButton readButton;
        final CircularProgressIndicator progressIndicator;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterName = itemView.findViewById(R.id.chapterTitle);
            group = itemView.findViewById(R.id.scanlationGroup);
            rowLayout = itemView.findViewById(R.id.rowLayout);
            divider = itemView.findViewById(R.id.dividerView);
            cardView = itemView.findViewById(R.id.cardView);

            downloadButton = itemView.findViewById(R.id.buttonDownload);
            readButton = itemView.findViewById(R.id.buttonRead);
            progressIndicator = itemView.findViewById(R.id.progressBar);

            progressIndicator.setShowAnimationBehavior(CircularProgressIndicator.SHOW_OUTWARD);
            progressIndicator.setHideAnimationBehavior(CircularProgressIndicator.HIDE_ESCAPE);
        }
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {
        final TextView volume;
        public LabelViewHolder(@NonNull View itemView) {
            super(itemView);
            volume = itemView.findViewById(R.id.volume);
        }
    }

    private static class ChapterDownloadProgressWrapper {
        final int pos;
        int state;
        float progress;

        ChapterDownloadProgressWrapper(int pos) {
            this.pos = pos;
            state = -1;
        }
    }
}
