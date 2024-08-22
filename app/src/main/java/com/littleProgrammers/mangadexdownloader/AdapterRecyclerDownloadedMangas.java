package com.littleProgrammers.mangadexdownloader;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.littleProgrammers.mangadexdownloader.db.DbMangaDAO;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AdapterRecyclerDownloadedMangas extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    ShapeAppearanceModel topCard;
    ShapeAppearanceModel bottomCard;
    ShapeAppearanceModel singleCard;
    ShapeAppearanceModel middleCard;

    private final static int VIEW_MANGA = 0;
    private final static int VIEW_CHAPTER = 1;

    private final static String EXPAND_PAYLOAD = "payload";

    private final ArrayList<DbMangaDAO.MangaChapterSchema> shadow;
    private final HashMap<String, Integer> shadowIDMap;
    private ArrayList<DbMangaDAO.MangaChapterSchema> actualList;

    private final AsyncListDiffer<DbMangaDAO.MangaChapterSchema> mDiffer;
    private final HashMap<String, Boolean> isExpanded;
    private final HashMap<String, DbMangaDAO.MangaChapterSizeAndNumber> mangaMap;

    private final AtomicInteger chapterGeneration;
    private final AtomicInteger mangaGeneration;
    private final AtomicBoolean chapterUpdateQueued;

    private int queuedToUpdatePos = -1;

    private SelectionTracker<Long> selectionTracker;
    private final AtomicInteger notUserInitiatedChapterSelection;
    private final AtomicBoolean notUserInitiatedMangaSelection;

    final Context ct;

    public AdapterRecyclerDownloadedMangas(Context _ct) {
        ct = _ct;
        isExpanded = new HashMap<>();
        mangaMap = new HashMap<>();
        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK);
        shadow = new ArrayList<>();
        shadowIDMap = new HashMap<>();

        chapterGeneration = new AtomicInteger(0);
        mangaGeneration = new AtomicInteger(0);
        chapterUpdateQueued = new AtomicBoolean(false);
        notUserInitiatedChapterSelection = new AtomicInteger(0);
        notUserInitiatedMangaSelection = new AtomicBoolean(false);

        mDiffer.addListListener((previousList, currentList) -> {
            if (queuedToUpdatePos != -1) {
                notifyItemChanged(queuedToUpdatePos, EXPAND_PAYLOAD);
                queuedToUpdatePos = -1;
            }
        });

        setHasStableIds(true);
    }

    public void AddDifferListener(AsyncListDiffer.ListListener<DbMangaDAO.MangaChapterSchema> l) {
        mDiffer.addListListener(l);
    }

    public void RemoveDifferListener(AsyncListDiffer.ListListener<DbMangaDAO.MangaChapterSchema> l) {
        mDiffer.removeListListener(l);
    }

    public void SetSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
        selectionTracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
            @Override
            public void onItemStateChanged(@NonNull Long key, boolean selected) {
                super.onItemStateChanged(key, selected);

                // Under particular circumstances, the selected parameter and what the
                // selectionTracker reports are different, so let's just stick with the selectionTracker
                selected = selectionTracker.isSelected(key);

                DbMangaDAO.MangaChapterSchema s = actualList.get(key.intValue());
                // Select/Deselect every chapter item when a manga is selected, if it's expanded
                // and the action wasn't initiated by the other piece of this function
                if (s.id == null) {
                    if (!IsExpanded(s.manga_id) || notUserInitiatedMangaSelection.getAndSet(false)) return;

                    DbMangaDAO.MangaChapterSizeAndNumber m = mangaMap.get(s.manga_id);
                    assert m != null;

                    int incMutex = 0;

                    for (long i = key + 1; i < key + m.number + 1; i++) {
                        if (selected) {
                            if (!selectionTracker.isSelected(i)) {
                                notUserInitiatedChapterSelection.incrementAndGet();
                                selectionTracker.select(i);
                            }
                        }
                        else
                        {
                            if (selectionTracker.isSelected(i)) {
                                notUserInitiatedChapterSelection.incrementAndGet();
                                selectionTracker.deselect(i);
                            }
                        }
                    }

                    notUserInitiatedChapterSelection.set(incMutex);
                }
                // Select/Deselect the manga item when all chapters are selected/one chapter is
                // deselected from a full selection
                else {
                    if (notUserInitiatedChapterSelection.get() > 0) {
                        notUserInitiatedChapterSelection.decrementAndGet();
                        return;
                    }

                    int i = key.intValue() + 1;
                    while (i < actualList.size() && actualList.get(i).id != null) {
                        if (!selectionTracker.isSelected((long) i)) return;
                        i++;
                    }

                    i = key.intValue() - 1;
                    while (actualList.get(i).id != null) {
                        if (!selectionTracker.isSelected((long) i)) return;
                        i--;
                    }

                    notUserInitiatedMangaSelection.set(true);

                    if (selected)
                        selectionTracker.select((long) i);
                    else
                        selectionTracker.deselect((long) i);
                }
            }
        });
    }



    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder;

        switch (viewType) {
            case VIEW_MANGA: {
                View view = LayoutInflater.from(ct)
                        .inflate(R.layout.item_recycler_downloaded_manga, viewGroup, false);

                MangaViewHolder m = new MangaViewHolder(view);
                m.cardView.setCheckable(true);
                m.cardView.setOnClickListener((v) -> m.expand.performClick());

                m.expand.setOnClickListener((v) -> {
                    DbMangaDAO.MangaChapterSchema c = actualList.get(m.getBindingAdapterPosition());
                    DbMangaDAO.MangaChapterSizeAndNumber manga = mangaMap.get(c.manga_id);
                    assert manga != null;

                    boolean e = !IsExpanded(c.manga_id);
                    isExpanded.put(c.manga_id, e);

                    notifyItemChanged(m.getBindingAdapterPosition(), EXPAND_PAYLOAD);

                    actualList = new ArrayList<>(actualList);

                    if (e) {
                        Integer pos = shadowIDMap.get(c.manga_id);
                        assert pos != null;

                        actualList.addAll(m.getBindingAdapterPosition() + 1, shadow.subList(pos + 1, pos + 1 + manga.number));
                        mDiffer.submitList(actualList);
                    }
                    else {
                        actualList.subList(m.getBindingAdapterPosition() + 1, m.getBindingAdapterPosition() + 1 + manga.number).clear();
                        mDiffer.submitList(actualList);
                    }
                });

                viewHolder = m;

                // Init custom shape appearence models
                if (topCard == null)
                {
                    ShapeAppearanceModel.Builder model = m.cardView.getShapeAppearanceModel().toBuilder();

                    middleCard = model.build();
                    model
                            .setTopLeftCornerSize(CompatUtils.ConvertDpToPixel(24, ct))
                            .setTopRightCornerSize(CompatUtils.ConvertDpToPixel(24, ct));

                    topCard = model.build();
                    model
                            .setBottomLeftCornerSize(CompatUtils.ConvertDpToPixel(24, ct))
                            .setBottomRightCornerSize(CompatUtils.ConvertDpToPixel(24, ct));

                    singleCard = model.build();
                    model
                            .setTopLeftCornerSize(0)
                            .setTopRightCornerSize(0);

                    bottomCard = model.build();
                }
                break;
            }
            case VIEW_CHAPTER: {
                View view = LayoutInflater.from(ct)
                        .inflate(R.layout.item_recycler_downloaded_file, viewGroup, false);

                ChapterViewHolder chapterViewHolder = new ChapterViewHolder(view);

                chapterViewHolder.cardView.setOnClickListener((v) -> chapterViewHolder.readButton.performClick());
                chapterViewHolder.readButton.setOnClickListener(v -> {
                    DbMangaDAO.MangaChapterSchema c = actualList.get(chapterViewHolder.getBindingAdapterPosition());

                    File targetFolder = new File(ct.getExternalFilesDir(null) + "/Manga/" + c.manga_id + "/" + c.id + "/");

                    String[] files = targetFolder.list();
                    files = (files != null ? files : new String[0]);
                    Arrays.sort(files);


                    Intent intent = new Intent(ct, ActivityOfflineReader.class);
                    intent.putExtra("baseUrl", targetFolder.getAbsolutePath());
                    intent.putExtra("urls", files);
                    ct.startActivity(intent);
                });

                viewHolder = chapterViewHolder;
                break;
            }
            default:
                throw new IllegalArgumentException();
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder vh, final int position) {
        switch (getItemViewType(position)) {
            case VIEW_MANGA: {
                MangaViewHolder viewHolder = (MangaViewHolder) vh;

                DbMangaDAO.MangaChapterSchema current = mDiffer.getCurrentList().get(position);
                DbMangaDAO.MangaChapterSizeAndNumber currentManga = mangaMap.get(current.manga_id);
                assert currentManga != null;
                viewHolder.mangaTitle.setText(currentManga.title);
                viewHolder.mangaAuthor.setText(currentManga.author);
                viewHolder.mangaArtist.setText(currentManga.artist);

                String size = new DecimalFormat("#.##").format((double) currentManga.size / (1024f * 1024));
                viewHolder.chaptersAndSizes.setText(ct.getResources().getQuantityString(R.plurals.chapters, currentManga.number, currentManga.number, size));

                viewHolder.cardView.setShapeAppearanceModel(IsExpanded(current.manga_id) ? topCard : singleCard);

                if (selectionTracker != null) {
                    viewHolder.details.position = viewHolder.getBindingAdapterPosition();
                    Long l = viewHolder.details.getSelectionKey();
                    viewHolder.cardView.setChecked(selectionTracker.isSelected(l));
                    viewHolder.checkOverlay.setVisibility(selectionTracker.isSelected(l) ? View.VISIBLE : View.GONE);
                }
                break;
            }
            case VIEW_CHAPTER: {
                ChapterViewHolder viewHolder = (ChapterViewHolder) vh;

                DbMangaDAO.MangaChapterSchema current = mDiffer.getCurrentList().get(position);
                viewHolder.chapterName.setText(current.title);
                viewHolder.chapterScanGroup.setText(current.scanlation_group);

                String size = new DecimalFormat("#.##").format((double) current.size / (1024f * 1024));
                viewHolder.fileSize.setText(ct.getResources().getQuantityString(R.plurals.pages, current.num_pages, current.num_pages, size));
                viewHolder.cardView.setShapeAppearanceModel((position == mDiffer.getCurrentList().size() - 1 || !Objects.equals(current.manga_id, mDiffer.getCurrentList().get(position + 1).manga_id)) ? bottomCard : middleCard);

                if (selectionTracker != null) {
                    viewHolder.details.position = viewHolder.getBindingAdapterPosition();
                    Long l = viewHolder.details.getSelectionKey();
                    viewHolder.cardView.setChecked(selectionTracker.isSelected(l));
                    viewHolder.checkOverlay.setVisibility(selectionTracker.isSelected(l) ? View.VISIBLE : View.GONE);
                }
                break;
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (getItemViewType(position) != VIEW_MANGA || !payloads.contains(EXPAND_PAYLOAD)) {
            onBindViewHolder(holder, position);
            return;
        }

        MangaViewHolder viewHolder = (MangaViewHolder) holder;
        DbMangaDAO.MangaChapterSchema current = mDiffer.getCurrentList().get(position);


        viewHolder.cardView.setShapeAppearanceModel(IsExpanded(current.manga_id) ? topCard : singleCard);
    }

    @Override
    public long getItemId(int position) {
        DbMangaDAO.MangaChapterSchema c = mDiffer.getCurrentList().get(position);
        return c.id == null ? c.manga_id.hashCode() : c.id.hashCode();
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mDiffer.getCurrentList().get(position).id == null)
            return VIEW_MANGA;

        Boolean expanded = isExpanded.getOrDefault(mDiffer.getCurrentList().get(position).manga_id, false);
        assert expanded != null;

        return VIEW_CHAPTER;
    }

    public void AddChapters(List<DbMangaDAO.MangaChapterSchema> list) {
        if (list != shadow) {
            shadow.clear();
            shadow.addAll(list);
        }

        if (chapterGeneration.incrementAndGet() > mangaGeneration.get()) {
            chapterUpdateQueued.set(true);
            return;
        }

        shadowIDMap.clear();
        actualList = new ArrayList<>();

        for (int i = 0; i < shadow.size(); i++) {
            DbMangaDAO.MangaChapterSchema m = shadow.get(i);
            if (m.id == null) {
                DbMangaDAO.MangaChapterSizeAndNumber s = mangaMap.get(m.manga_id);
                assert s != null;

                shadowIDMap.put(m.manga_id, i);
                actualList.add(m);

                if (IsExpanded(m.manga_id)) {
                    actualList.addAll(shadow.subList(i + 1, i + s.number + 1));
                    queuedToUpdatePos = actualList.size() - 2;
                }

                i += s.number;
            }
        }

        mDiffer.submitList(actualList);
    }

    public List<DbMangaDAO.MangaChapterSchema> GetList() {
        return actualList;
    }

    public HashMap<String, DbMangaDAO.MangaChapterSizeAndNumber> GetMangaMap() {
        return mangaMap;
    }

    public void AddMangas(List<DbMangaDAO.MangaChapterSizeAndNumber> list) {
        for (DbMangaDAO.MangaChapterSizeAndNumber dbManga : list) {
            mangaMap.put(dbManga.id, dbManga);
        }

        mangaGeneration.incrementAndGet();

        if (chapterUpdateQueued.getAndSet(false)) {
            chapterGeneration.decrementAndGet();
            AddChapters(shadow);
        }
    }

    public boolean IsExpanded(String mangaId) {
        Boolean expanded = isExpanded.getOrDefault(mangaId, false);
        if (expanded == null)
            expanded = false;

        return expanded;
    }


    public static class MangaViewHolder extends ItemViewHolder {
        final TextView mangaTitle;
        final TextView mangaAuthor;
        final TextView mangaArtist;
        final TextView chaptersAndSizes;
        final MaterialButton expand;

        public MangaViewHolder(View view) {
            super(view);

            cardView = view.findViewById(R.id.cardView);
            mangaTitle = view.findViewById(R.id.downloadedMangaName);
            mangaAuthor = view.findViewById(R.id.downloadedMangaAuthor);
            mangaArtist = view.findViewById(R.id.downloadedMangaArtist);
            chaptersAndSizes = view.findViewById(R.id.chaptersAndSizes);
            expand = view.findViewById(R.id.expand);
            checkOverlay = itemView.findViewById(R.id.checkedOverlay);
        }
    }

    public static class ChapterViewHolder extends ItemViewHolder {
        final TextView chapterName;
        final TextView chapterScanGroup;
        final TextView fileSize;
        final View divider;
        final MaterialButton readButton;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            chapterName = itemView.findViewById(R.id.chapterTitle);
            chapterScanGroup = itemView.findViewById(R.id.scanlationGroup);
            fileSize = itemView.findViewById(R.id.fileSize);
            divider = itemView.findViewById(R.id.dividerView);
            readButton = itemView.findViewById(R.id.buttonRead);
            checkOverlay = itemView.findViewById(R.id.checkedOverlay);
        }
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        final Details details;
        MaterialCardView cardView;
        MaterialButton checkOverlay;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            details = new Details();
        }

        public Details GetDetails() {
            details.position = getBindingAdapterPosition();
            return details;
        }
    }

    public final DiffUtil.ItemCallback<DbMangaDAO.MangaChapterSchema> DIFF_CALLBACK
            = new DiffUtil.ItemCallback<DbMangaDAO.MangaChapterSchema>() {
        @Override
        public boolean areItemsTheSame(
                @NonNull DbMangaDAO.MangaChapterSchema oldManga, @NonNull DbMangaDAO.MangaChapterSchema newManga) {
            return Objects.equals(oldManga.id, newManga.id) && Objects.equals(oldManga.manga_id, newManga.manga_id);
        }
        @Override
        public boolean areContentsTheSame(
                @NonNull DbMangaDAO.MangaChapterSchema oldManga, @NonNull DbMangaDAO.MangaChapterSchema newManga) {
            return oldManga.size == newManga.size && oldManga.num_pages == newManga.num_pages;
        }
    };


     static class DetailsLookup extends ItemDetailsLookup<Long> {

        private final RecyclerView recyclerView;

        DetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);

                if (viewHolder instanceof ItemViewHolder) {
                    ItemViewHolder v = (ItemViewHolder) viewHolder;
                    v.details.position = v.getBindingAdapterPosition();

                    return v.GetDetails();
                }
            }
            return null;
        }
    }

    static class KeyProvider extends ItemKeyProvider<Long> {
        KeyProvider() {
            super(ItemKeyProvider.SCOPE_MAPPED);
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            return (long) position;
        }

        @Override
        public int getPosition(@NonNull Long key) {
            long value = key;
            return (int) value;
        }
    }

    static class Details extends ItemDetailsLookup.ItemDetails<Long> {
        long position;

        Details() {}

        @Override
        public int getPosition() {
            return (int) position;
        }

        @Nullable
        @Override
        public Long getSelectionKey() {
            return position;
        }

        @Override
        public boolean inDragRegion(@NonNull MotionEvent e) {
            return true;
        }
    }
}
