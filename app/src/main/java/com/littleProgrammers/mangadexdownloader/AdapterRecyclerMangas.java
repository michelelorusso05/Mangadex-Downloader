package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.Tag;
import com.littleProgrammers.mangadexdownloader.utils.ApiUtils;
import com.littleProgrammers.mangadexdownloader.utils.GlideApp;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.noties.markwon.Markwon;

public class AdapterRecyclerMangas extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ListPreloader.PreloadModelProvider<String> {
    final Activity ct;

    // Dataset
    final ArrayList<Manga> mangas;
    // Get title string from position
    final HashMap<Integer, String> titleIndexes;
    // Adapter position to type-actual dataset index map
    final ArrayList<Pair<Integer, Integer>> adapterPositionToArrayMap;

    final LinkedList<MangaViewHolder> pooledHolders;

    final Consumer<Integer> openFragment;
    final int fragmentViewId;
    boolean hasInfiniteScrolling;
    boolean forceLoadView;
    boolean errorSet;

    int showWarning;

    Runnable onRetryCallback;

    final DNSClient client;
    final Markwon markwon;

    public static final int ITEM_MANGA = 0;
    public static final int ITEM_TITLE = 1;
    public static final int ITEM_FRAGMENT = 2;
    public static final int ITEM_LOADING_PLACEHOLDER = 3;
    public static final int ITEM_ERROR = 4;
    public static final int ITEM_NO_FAVS = 5;
    public static final int ITEM_NO_RESULTS = 6;

    private final FixedPreloadSizeProvider<String> preloadSizeProvider;

    public AdapterRecyclerMangas(Activity _ct, Consumer<Integer> o, int f, RecyclerView recyclerView) {
        ct = _ct;
        mangas = new ArrayList<>();
        titleIndexes = new HashMap<>();
        adapterPositionToArrayMap = new ArrayList<>();

        client = StaticData.getClient(ct);
        markwon = StaticData.getMarkwon(ct);

        openFragment = o;
        fragmentViewId = f;
        hasInfiniteScrolling = false;
        errorSet = false;
        forceLoadView = false;
        showWarning = -1;

        preloadSizeProvider = new FixedPreloadSizeProvider<>(384, 512);

        pooledHolders = new LinkedList<>();

        if (recyclerView != null) {
            for (int i = 0; i < 24; i++) {
                recyclerView.getRecycledViewPool().putRecycledView(CreateMangaHolder(recyclerView));
            }
            recyclerView.getRecycledViewPool().setMaxRecycledViews(ITEM_MANGA, 24);
        }
    }

    public AdapterRecyclerMangas(Activity _ct) {
        this(_ct, null, 0, null);
    }

    private MangaViewHolder CreateMangaHolder(ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(ct);

        View view = inflater.inflate(R.layout.item_recycler_manga_test, viewGroup, false);

        MangaViewHolder mangaViewHolder = new MangaViewHolder(view);

        mangaViewHolder.rowLayout.setOnClickListener(v -> {
            int position = adapterPositionToArrayMap.get(mangaViewHolder.getBindingAdapterPosition() - GetStart()).second;

            Intent intent = new Intent(ct, ActivityManga.class);
            intent.putExtra("MangaData", (Parcelable) mangas.get(position));

            ct.startActivity(intent);
        });

        return mangaViewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        if (openFragment != null && position == 0)
            return ITEM_FRAGMENT;

        if (showWarning != -1 && position == GetStart()) {
            // The manga list must be empty to show a warning
            assert adapterPositionToArrayMap.isEmpty();
            return showWarning;
        }

        if (position - GetStart() == adapterPositionToArrayMap.size()) {
            if (hasInfiniteScrolling && (forceLoadView || !adapterPositionToArrayMap.isEmpty()))
                return ITEM_LOADING_PLACEHOLDER;
            else if (errorSet)
                return ITEM_ERROR;
        }

        return adapterPositionToArrayMap.get(position - GetStart()).first;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ct);
        RecyclerView.ViewHolder viewHolder;

        switch (viewType) {
            case ITEM_MANGA:
            {
                MangaViewHolder mangaViewHolder;
                if (pooledHolders.isEmpty())
                    mangaViewHolder = CreateMangaHolder(parent);
                else
                    mangaViewHolder = pooledHolders.pop();

                viewHolder = mangaViewHolder;
                break;
            }
            case ITEM_TITLE: {
                View view = inflater.inflate(R.layout.item_recycler_label, parent, false);
                viewHolder = new LabelViewHolder(view);
                break;
            }
            case ITEM_FRAGMENT: {
                FragmentHolder fragmentHolder = new FragmentHolder(LayoutInflater.from(ct).inflate(R.layout.item_recycler_fragment_placeholder, parent, false));
                fragmentHolder.fragmentContainer.setId(fragmentViewId);
                viewHolder = fragmentHolder;
                break;
            }
            case ITEM_LOADING_PLACEHOLDER: {
                viewHolder = new PlaceholderHolder(LayoutInflater.from(ct).inflate(R.layout.item_recycler_loading, parent, false));
                break;
            }
            case ITEM_ERROR: {
                ErrorHolder errorHolder = new ErrorHolder(inflater.inflate(R.layout.item_recycler_err_no_internet, parent, false));

                boolean landscape = ct.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                if (landscape) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) errorHolder.icon.getLayoutParams();
                    params.matchConstraintPercentWidth = 0.075f;
                }

                errorHolder.retryButton.setOnClickListener((v) -> {
                    errorSet = false;
                    hasInfiniteScrolling = true;
                    forceLoadView = true;

                    notifyItemChanged(GetStart() + adapterPositionToArrayMap.size());

                    if (onRetryCallback != null)
                        onRetryCallback.run();
                });

                viewHolder = errorHolder;
                break;
            }
            case ITEM_NO_FAVS: {
                ErrorHolder errorHolder = new ErrorHolder(LayoutInflater.from(ct).inflate(R.layout.item_recycler_err_no_favs, parent, false));
                boolean landscape = ct.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                if (landscape) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) errorHolder.icon.getLayoutParams();
                    params.matchConstraintPercentWidth = 0.075f;
                }

                viewHolder = errorHolder;
                break;
            }
            case ITEM_NO_RESULTS: {
                ErrorHolder errorHolder = new ErrorHolder(LayoutInflater.from(ct).inflate(R.layout.item_recycler_err_no_results, parent, false));
                boolean landscape = ct.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                if (landscape) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) errorHolder.icon.getLayoutParams();
                    params.matchConstraintPercentWidth = 0.075f;
                }

                viewHolder = errorHolder;
                break;
            }
            default:
                throw new IllegalStateException("Only chapter, label and fragment rows are supposed to be here.");
        }

        return viewHolder;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof MangaViewHolder)
            GlideApp
                    .with(ct.getBaseContext())
                    .clear(((MangaViewHolder) holder).cover);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        switch (getItemViewType(pos)) {
            case ITEM_MANGA:
            {
                int position = adapterPositionToArrayMap.get(pos - GetStart()).second;

                MangaViewHolder holder = (MangaViewHolder) h;
                Manga current = mangas.get(position);

                holder.mangaTitle.setText(ApiUtils.GetMangaTitleString(current));
                holder.mangaAuthor.setText(current.getAttributes().getAuthorString());

                // markwon.setMarkdown(holder.description, ApiUtils.GetMangaDescription(current));
                // holder.description.setMovementMethod(null);
                holder.description.setText(current.getAttributes().getShortDescription());

                GlideApp
                        .with(ct.getBaseContext())
                        .load("https://uploads.mangadex.org/covers/" + current.getId() + "/" + current.getAttributes().getCoverUrl() + ".512.jpg")
                        //.override(192, 256)
                        .placeholder(R.drawable.cover_placeholder)
                        .into(holder.cover);


                Tag[] tags = current.getAttributes().getTags();
                holder.chips[0].setText(ApiUtils.GetMangaRatingString(current.getAttributes().getContentRating()).first);

                for (int i = 1; i < holder.chips.length; i++) {
                    Chip chip = holder.chips[i];

                    if (i >= tags.length) {
                        chip.setVisibility(View.GONE);
                    }
                    else {
                        chip.setVisibility(View.VISIBLE);
                        chip.setText(ApiUtils.GetTagTranslatedName(ct, tags[i - 1]));
                    }
                }

                break;
            }
            case ITEM_TITLE:
            {
                int position = adapterPositionToArrayMap.get(pos - GetStart()).second;

                LabelViewHolder holder = (LabelViewHolder) h;
                holder.volume.setText(ApiUtils.GetTranslatedRelationship(ct, titleIndexes.get(position)));
                break;
            }
            case ITEM_FRAGMENT:
            {
                assert h instanceof FragmentHolder;
                FragmentHolder fragmentHolder = (FragmentHolder) h;
                fragmentHolder.fragmentContainer.setId(fragmentViewId);

                // Sometimes the to-replace view hasn't been initialized yet, so the fragment callback has to wait
                fragmentHolder.fragmentContainer.post(() -> openFragment.accept(fragmentViewId));
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        int c = adapterPositionToArrayMap.size();

        c += GetStart() + GetTrailerCount();

        return c;
    }

    @Override
    public long getItemId(int position) {
        // Search bar
        if (openFragment != null && position == 0)
            return 0;

        position -= GetStart();

        // Loading placeholder or error view
        if (position == adapterPositionToArrayMap.size())
            return errorSet ? 2 : 1;

        return mangas.get(adapterPositionToArrayMap.get(position).second).hashCode() ^ adapterPositionToArrayMap.get(position).first;
    }

    public ArrayList<Pair<Integer, Integer>> getAdapterPositionToArrayMap() {
        return adapterPositionToArrayMap;
    }

    public boolean IsEmpty() {
        return adapterPositionToArrayMap.isEmpty();
    }

    public void AddMangas(Collection<Manga> mangasToAdd, Map<Integer, String> titleIndexesToAdd, boolean hasMore) {
        ClearError();

        int offset = mangas.size();
        int startPos = adapterPositionToArrayMap.size();

        mangas.addAll(mangasToAdd);
        titleIndexesToAdd.forEach((index, title) -> titleIndexes.put(index + offset, title));

        for (int i = offset, mangasLength = mangasToAdd.size(); i < mangasLength + offset; i++) {
            // Construct indexes
            if (titleIndexes.containsKey(i))
                adapterPositionToArrayMap.add(Pair.create(ITEM_TITLE, i));
            adapterPositionToArrayMap.add(Pair.create(ITEM_MANGA, i));
        }

        boolean hadInfiniteScrolling = hasInfiniteScrolling;
        hasInfiniteScrolling = hasMore;

        if (hadInfiniteScrolling && !hasMore) {
            notifyItemRangeInserted(startPos + GetStart(), mangasToAdd.size() + titleIndexesToAdd.size() - 1);
            notifyItemChanged(startPos + GetStart() + mangasToAdd.size() + titleIndexesToAdd.size() - 1);
        }
        else {
            notifyItemRangeInserted(startPos + GetStart(), mangasToAdd.size() + titleIndexesToAdd.size());
        }
    }

    public void AddMangas(Collection<Manga> mangasToAdd, boolean hasMore) {
        AddMangas(mangasToAdd, Collections.emptyMap(), hasMore);
    }

    public void ReplaceMangas(Collection<Manga> mangasToAdd, Map<Integer, String> titleIndexesToAdd, boolean hasMore) {
        ClearError();
        showWarning = -1;

        int oldSize = adapterPositionToArrayMap.size() + GetTrailerCount();
        int newSize = mangasToAdd.size() + titleIndexesToAdd.size() + (hasMore ? 1 : 0);
        int delta = newSize - oldSize;

        mangas.clear();
        titleIndexes.clear();
        adapterPositionToArrayMap.clear();

        int offset = mangas.size();

        mangas.addAll(mangasToAdd);
        titleIndexesToAdd.forEach((index, title) -> titleIndexes.put(index + offset, title));

        for (int i = offset, mangasLength = mangasToAdd.size(); i < mangasLength + offset; i++) {
            // Construct indexes
            if (titleIndexes.containsKey(i))
                adapterPositionToArrayMap.add(Pair.create(ITEM_TITLE, i));
            adapterPositionToArrayMap.add(Pair.create(ITEM_MANGA, i));
        }

        hasInfiniteScrolling = hasMore;

        if (delta < 0) {
            notifyItemRangeRemoved(GetStart() + newSize, -delta);

            if (newSize > 0)
                notifyItemRangeChanged(GetStart(), newSize);
        }
        if (delta == 0) {
            notifyItemRangeChanged(GetStart(), newSize);
        }
        if (delta > 0) {
            notifyItemRangeInserted(GetStart() + newSize, delta);

            if (oldSize > 0)
                notifyItemRangeChanged(GetStart(), newSize);
        }
    }

    public void ReplaceMangas(Collection<Manga> mangasToAdd, boolean hasMore) {
        ReplaceMangas(mangasToAdd, Collections.emptyMap(), hasMore);
    }

    public void ClearMangas() {
        int count = adapterPositionToArrayMap.size();

        mangas.clear();
        titleIndexes.clear();
        adapterPositionToArrayMap.clear();

        boolean oldInf = hasInfiniteScrolling;
        hasInfiniteScrolling = false;

        notifyItemRangeRemoved(GetStart(), count + (oldInf ? 1 : 0));
    }

    public void SetError() {
        if (errorSet) return;
        boolean hadInfiniteScrolling = GetTrailerCount() > 0;

        showWarning = -1;
        errorSet = true;
        hasInfiniteScrolling = false;

        if (hadInfiniteScrolling) {
            notifyItemChanged(GetStart() + adapterPositionToArrayMap.size());
        }
        else
            notifyItemInserted(GetStart() + adapterPositionToArrayMap.size());
    }

    public void SetOnRetry(Runnable onRetry) {
        onRetryCallback = onRetry;
    }

    private void ClearError() {
        errorSet = false;
        forceLoadView = false;
    }

    private void ClearForWarning(boolean update) {
        // There's already a warning set, just update the existing one
        if (update) {
            notifyItemChanged(GetStart());
            return;
        }

        int count = adapterPositionToArrayMap.size() + GetTrailerCount();

        mangas.clear();
        titleIndexes.clear();
        adapterPositionToArrayMap.clear();

        hasInfiniteScrolling = false;
        errorSet = false;

        if (count == 0)
            notifyItemInserted(GetStart());
        else if (count == 1)
            notifyItemChanged(GetStart());
        else {
            notifyItemRangeRemoved(GetStart() + 1, count - 1);
            notifyItemChanged(GetStart());
        }
    }

    public void SetNoFavs() {
        int oldW = showWarning;
        showWarning = ITEM_NO_FAVS;
        ClearForWarning(oldW != -1);
    }

    public void SetNoResults() {
        int oldW = showWarning;
        showWarning = ITEM_NO_RESULTS;
        ClearForWarning(oldW != -1);
    }

    private int GetStart() {
        return (openFragment != null) ? 1 : 0;
    }

    private int GetTrailerCount() {
        return ((hasInfiniteScrolling && (forceLoadView || !adapterPositionToArrayMap.isEmpty()))
                || errorSet
                || showWarning != -1) ? 1 : 0;
    }

    @NonNull
    @Override
    public List<String> getPreloadItems(int position) {
        if (getItemViewType(position) != ITEM_MANGA) return Collections.emptyList();

        Manga m = mangas.get(adapterPositionToArrayMap.get(position - GetStart()).second);
        String url = "https://uploads.mangadex.org/covers/" + m.getId() + "/" + m.getAttributes().getCoverUrl() + ".512.jpg";

        return Collections.singletonList(url);
    }

    @Nullable
    @Override
    public RequestBuilder<?> getPreloadRequestBuilder(@NonNull String item) {
        return GlideApp
                .with(ct.getBaseContext())
                .load(item)
                //.override(192, 256)
                ;
    }

    public ListPreloader.PreloadSizeProvider<String> getPreloadSizeProvider() {
        return preloadSizeProvider;
    }

    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        final TextView mangaTitle;
        final TextView mangaAuthor;
        final TextView description;
        final ShapeableImageView cover;
        final View rowLayout;
        final Chip[] chips;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);

            rowLayout = itemView.findViewById(R.id.rowLayout);
            cover = itemView.findViewById(R.id.cover);

            mangaTitle = itemView.findViewById(R.id.cardTitle);
            mangaAuthor = itemView.findViewById(R.id.cardAuthor);
            description = itemView.findViewById(R.id.description);
            ChipGroup chipGroup = itemView.findViewById(R.id.chipGroup);

            chips = new Chip[chipGroup.getChildCount()];

            for (int i = 0; i < chipGroup.getChildCount(); i++)
                chips[i] = (Chip) chipGroup.getChildAt(i);
        }
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {
        final TextView volume;
        public LabelViewHolder(@NonNull View itemView) {
            super(itemView);
            volume = itemView.findViewById(R.id.volume);
        }
    }

    static class FragmentHolder extends RecyclerView.ViewHolder {
        final FrameLayout fragmentContainer;
        FragmentHolder(View itemView) {
            super(itemView);
            fragmentContainer = itemView.findViewById(R.id.fragment_container_adapter);
        }
    }

    static class PlaceholderHolder extends RecyclerView.ViewHolder {
        PlaceholderHolder(View itemView) {
            super(itemView);
        }
    }

    static class ErrorHolder extends RecyclerView.ViewHolder {
        final MaterialButton retryButton;
        final ImageView icon;
        ErrorHolder(View itemView) {
            super(itemView);
            retryButton = itemView.findViewById(R.id.retryButton);
            icon = itemView.findViewById(R.id.cover);
        }
    }

    public GridLayoutManager.SpanSizeLookup GetSpanLookup(GridLayoutManager manager) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (getItemViewType(position) == AdapterRecyclerMangas.ITEM_MANGA)
                    return 1;

                return manager.getSpanCount();
            }
        };
    }
}
