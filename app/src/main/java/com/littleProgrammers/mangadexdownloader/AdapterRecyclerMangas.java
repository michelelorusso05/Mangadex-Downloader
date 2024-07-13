package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaAttributes;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.littleProgrammers.mangadexdownloader.apiResults.Tag;
import com.littleProgrammers.mangadexdownloader.utils.GlideApp;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.noties.markwon.Markwon;

public class AdapterRecyclerMangas extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Activity ct;

    // Dataset
    ArrayList<Manga> mangas;
    // Get title string from position
    HashMap<Integer, String> titleIndexes;
    // Adapter position to type-actual dataset index map
    ArrayList<Pair<Integer, Integer>> adapterPositionToArrayMap;

    Consumer<Integer> openFragment;
    boolean hasInfiniteScrolling;

    DNSClient client;
    Markwon markwon;

    public static final int ITEM_MANGA = 0;
    public static final int ITEM_TITLE = 1;
    public static final int ITEM_FRAGMENT = 2;
    public static final int ITEM_LOADING_PLACEHOLDER = 3;

    public AdapterRecyclerMangas(Activity _ct, Consumer<Integer> o) {
        ct = _ct;
        mangas = new ArrayList<>();
        titleIndexes = new HashMap<>();
        adapterPositionToArrayMap = new ArrayList<>();

        client = StaticData.getClient(ct);
        markwon = StaticData.getMarkwon(ct);

        openFragment = o;
        hasInfiniteScrolling = false;
    }

    public AdapterRecyclerMangas(Activity _ct) {
        this(_ct, null);
    }

    @Override
    public int getItemViewType(int position) {
        if (openFragment != null && position == 0)
            return ITEM_FRAGMENT;

        if (hasInfiniteScrolling && !adapterPositionToArrayMap.isEmpty() && position - getStart() == adapterPositionToArrayMap.size())
            return ITEM_LOADING_PLACEHOLDER;

        return adapterPositionToArrayMap.get(position - getStart()).first;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ct);
        RecyclerView.ViewHolder viewHolder;

        switch (viewType) {
            case ITEM_MANGA:
            {
                View view = inflater.inflate(R.layout.recyclerviewitem_manga, parent, false);

                MangaViewHolder mangaViewHolder = new MangaViewHolder(view);

                mangaViewHolder.rowLayout.setOnClickListener(v -> {
                    int position = adapterPositionToArrayMap.get(mangaViewHolder.getAdapterPosition() - getStart()).second;

                    Intent intent = new Intent(ct, ActivityManga.class);
                    intent.putExtra("MangaData", mangas.get(position));

                    ct.startActivity(intent);
                });

                viewHolder = mangaViewHolder;
                break;
            }
            case ITEM_TITLE: {
                View view = inflater.inflate(R.layout.recyclerviewitem_volume_label, parent, false);
                viewHolder = new LabelViewHolder(view);
                break;
            }
            case ITEM_FRAGMENT: {
                viewHolder = new FragmentHolder(LayoutInflater.from(ct).inflate(R.layout.recyclerviewitem_fragment, parent, false));
                break;
            }
            case ITEM_LOADING_PLACEHOLDER: {
                viewHolder = new PlaceholderHolder(LayoutInflater.from(ct).inflate(R.layout.recyclerviewitem_loading_placeholder, parent, false));
                break;
            }
            default:
                throw new IllegalStateException("Only chapter, label and fragment rows are supposed to be here.");
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        switch (getItemViewType(pos)) {
            case ITEM_MANGA:
            {
                int position = adapterPositionToArrayMap.get(pos - getStart()).second;

                MangaViewHolder holder = (MangaViewHolder) h;
                Manga current = mangas.get(position);
                holder.mangaTitle.setText(current.getAttributes().getTitleS());
                holder.mangaAuthor.setText(current.getAttributes().getAuthorString());

                markwon.setMarkdown(holder.description, current.getDescription());
                holder.description.setMovementMethod(null);
                // holder.description.setText(current.getCanonicalDescription());


                GlideApp
                        .with(ct.getApplicationContext())
                        .load("https://uploads.mangadex.org/covers/" + current.getId() + "/" + current.getAttributes().getCoverUrl() + ".512.jpg")
                        .placeholder(R.drawable.cover_placeholder)
                        .into(holder.cover);


                Tag[] tags = current.getAttributes().getTags();
                holder.chips[0].setText(MangaAttributes.getRatingString(current.getAttributes().getContentRating()).first);

                for (int i = 1; i < holder.chips.length; i++) {
                    Chip chip = holder.chips[i];

                    if (i >= tags.length) {
                        chip.setVisibility(View.GONE);
                    }
                    else {
                        chip.setVisibility(View.VISIBLE);
                        chip.setText(tags[i - 1].getTranslatedName(ct));
                    }
                }

                break;
            }
            case ITEM_TITLE:
            {
                int position = adapterPositionToArrayMap.get(pos - getStart()).second;

                LabelViewHolder holder = (LabelViewHolder) h;
                holder.volume.setText(Relationship.getTranslatedRelationship(ct, titleIndexes.get(position)));
                break;
            }
            case ITEM_FRAGMENT:
            {
                assert h instanceof FragmentHolder;
                openFragment.accept(((FragmentHolder) h).fragmentContainer.getId());
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        int c = adapterPositionToArrayMap.size();

        if (openFragment != null)
            c++;
        if (hasInfiniteScrolling && !adapterPositionToArrayMap.isEmpty())
            c++;

        return c;
    }

    @Override
    public long getItemId(int position) {
        // Search bar
        if (openFragment != null && position == 0)
            return 0;

        position -= getStart();

        // Loading placeholder
        if (position == adapterPositionToArrayMap.size())
            return 1;

        return mangas.get(adapterPositionToArrayMap.get(position).second).hashCode() ^ adapterPositionToArrayMap.get(position).first;
    }

    public ArrayList<Pair<Integer, Integer>> getAdapterPositionToArrayMap() {
        return adapterPositionToArrayMap;
    }

    public void AddMangas(Collection<Manga> mangasToAdd, Map<Integer, String> titleIndexesToAdd, boolean hasMore) {
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

        boolean oldInf = hasInfiniteScrolling;
        hasInfiniteScrolling = hasMore;

        if (oldInf && !hasMore) {
            notifyItemRangeInserted(startPos + getStart(), mangasToAdd.size() + titleIndexesToAdd.size() - 1);
            notifyItemChanged(startPos + getStart() + mangasToAdd.size() + titleIndexesToAdd.size() - 1);
        }
        else {
            notifyItemRangeInserted(startPos + getStart(), mangasToAdd.size() + titleIndexesToAdd.size());
        }
    }

    public void AddMangas(Collection<Manga> mangasToAdd, boolean hasMore) {
        AddMangas(mangasToAdd, Collections.emptyMap(), hasMore);
    }

    public void ReplaceMangas(Collection<Manga> mangasToAdd, Map<Integer, String> titleIndexesToAdd, boolean hasMore) {
        int oldSize = adapterPositionToArrayMap.size();
        int newSize = mangasToAdd.size() + titleIndexesToAdd.size();
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

        boolean oldInf = hasInfiniteScrolling;
        hasInfiniteScrolling = hasMore;

        if (oldInf && !hasMore)
            delta--;
        else if (!oldInf && hasMore)
            delta++;

        if (delta < 0) {
            notifyItemRangeRemoved(getStart() + newSize, -delta);

            if (newSize > 0)
                notifyItemRangeChanged(getStart(), newSize);
        }
        if (delta == 0) {
            notifyItemRangeChanged(getStart(), newSize);
        }
        if (delta > 0) {
            notifyItemRangeInserted(getStart() + newSize, delta);

            if (oldSize > 0)
                notifyItemRangeChanged(getStart(), newSize);
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

        notifyItemRangeRemoved(getStart(), count + (oldInf ? 1 : 0));
    }

    private int getStart() {
        if (openFragment != null)
            return 1;
        return 0;
    }

    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        TextView mangaTitle, mangaAuthor, description;
        ShapeableImageView cover;
        View rowLayout;
        Chip[] chips;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            mangaTitle = itemView.findViewById(R.id.cardTitle);
            mangaAuthor = itemView.findViewById(R.id.cardAuthor);
            description = itemView.findViewById(R.id.description);
            cover = itemView.findViewById(R.id.cover);
            rowLayout = itemView.findViewById(R.id.rowLayout);
            ChipGroup chipGroup = itemView.findViewById(R.id.chipGroup);

            chips = new Chip[5];

            for (int i = 0; i < chipGroup.getChildCount(); i++)
                chips[i] = (Chip) chipGroup.getChildAt(i);
        }
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {
        TextView volume;
        public LabelViewHolder(@NonNull View itemView) {
            super(itemView);
            volume = itemView.findViewById(R.id.volume);
        }
    }

    static class FragmentHolder extends RecyclerView.ViewHolder {
        FrameLayout fragmentContainer;
        FragmentHolder(View itemView) {
            super(itemView);
            fragmentContainer = itemView.findViewById(R.id.fragment_container_adapter);
            fragmentContainer.setId(View.generateViewId());
        }
    }

    static class PlaceholderHolder extends RecyclerView.ViewHolder {
        PlaceholderHolder(View itemView) {
            super(itemView);
        }
    }
}
