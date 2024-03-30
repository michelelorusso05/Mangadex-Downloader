package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.littleProgrammers.mangadexdownloader.apiResults.MangaAttributes;
import com.littleProgrammers.mangadexdownloader.apiResults.Relationship;
import com.littleProgrammers.mangadexdownloader.apiResults.Tag;
import com.michelelorusso.dnsclient.DNSClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MangaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Activity ct;
    Manga[] mangas;
    HashMap<Integer, String> titleIndexes;

    ArrayList<Pair<Integer, Integer>> adapterPositionToArrayMap;
    Bitmap[] covers;
    DNSClient client;

    public static final int ITEM_MANGA = 0;
    public static final int ITEM_TITLE = 1;

    public MangaAdapter(Activity _ct, Manga[] _mangas, HashMap<Integer, String> _titleIndexes) {
        ct = _ct;
        mangas = _mangas;
        titleIndexes = _titleIndexes;
        client = StaticData.getClient(ct);

        adapterPositionToArrayMap = new ArrayList<>(mangas.length + titleIndexes.size());

        boolean lowQualityCover = PreferenceManager.getDefaultSharedPreferences(ct).getBoolean("lowQualityCovers", false);

        covers = new Bitmap[mangas.length];
        for (int i = 0, mangasLength = mangas.length; i < mangasLength; i++) {
            // Construct indexes
            if (titleIndexes.containsKey(i))
                adapterPositionToArrayMap.add(Pair.create(ITEM_TITLE, i));
            adapterPositionToArrayMap.add(Pair.create(ITEM_MANGA, i));

            Manga m = mangas[i];
            int finalI = i;
            int finalAdapterPos = adapterPositionToArrayMap.size() - 1;
            client.GetImageBitmapAsync("https://uploads.mangadex.org/covers/" + m.getId() + "/" + m.getAttributes().getCoverUrl() + ((lowQualityCover) ? ".256.jpg" : ".512.jpg"), (Bitmap bm, boolean success) -> {
               covers[finalI] = bm;
               ct.runOnUiThread(() -> notifyItemChanged(finalAdapterPos, "coverReady"));
            });
        }
    }
    public MangaAdapter(Activity _ct, Manga[] _mangas) {
        this(_ct, _mangas, new HashMap<>());
    }
    public MangaAdapter(Activity _ct) {
        this(_ct, new Manga[0]);
    }

    @Override
    public int getItemViewType(int position) {
        return adapterPositionToArrayMap.get(position).first;
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
                mangaViewHolder.description.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mangaViewHolder.description.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int noOfLinesVisible = mangaViewHolder.description.getHeight() / mangaViewHolder.description.getLineHeight();

                        mangaViewHolder.description.setMaxLines(noOfLinesVisible);
                        mangaViewHolder.description.setEllipsize(TextUtils.TruncateAt.END);
                        mangaViewHolder.description.setGravity(Gravity.TOP);
                    }
                });

                viewHolder = mangaViewHolder;
                break;
            }
            case ITEM_TITLE: {
                View view = inflater.inflate(R.layout.recyclerviewitem_volume_label, parent, false);
                viewHolder = new LabelViewHolder(view);
                break;
            }
            default:
                throw new IllegalStateException("Only chapter and label rows are supposed to be here.");

        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        switch (getItemViewType(pos)) {
            case ITEM_MANGA:
            {
                int position = adapterPositionToArrayMap.get(pos).second;

                MangaViewHolder holder = (MangaViewHolder) h;
                Manga current = mangas[position];
                holder.mangaTitle.setText(HtmlCompat.fromHtml(current.getAttributes().getTitleS(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                holder.mangaAuthor.setText(current.getAttributes().getAuthorString());
                holder.description.setText(current.getCanonicalDescription());

                holder.cover.setImageBitmap(covers[position]);

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

                holder.rowLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(ct, ChapterDownloaderActivity.class);
                    intent.putExtra("MangaData", mangas[position]);
                    StaticData.sharedCover = null;

                    if (covers[position] != null)
                        StaticData.sharedCover = covers[position];

                    // RIP to the buggiest Mangadex Downloader feature. Even though you were ugly as fuck
                    // under certain aspects, you had a charm that will not be matched, ever. (22/01/2024)
                    /*
                    if (covers[position] != null) {
                        StaticData.sharedCover = covers[position];
                        Bundle bundle;

                        View statusBar = ct.findViewById(android.R.id.statusBarBackground);
                        View navigation = ct.findViewById(android.R.id.navigationBarBackground);

                        if (statusBar == null && navigation == null)
                            bundle = ActivityOptions.makeSceneTransitionAnimation(ct,
                                    Pair.create(ct.findViewById(R.id.home_toolbar), "toolbar"),
                                    Pair.create(holder.cover, "cover")).toBundle();
                        else if (statusBar != null && navigation == null)
                            bundle = ActivityOptions.makeSceneTransitionAnimation(ct,
                                    Pair.create(ct.findViewById(R.id.home_toolbar), "toolbar"),
                                    Pair.create(holder.cover, "cover"),
                                    Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME)).toBundle();
                        else if (statusBar == null)
                            bundle = ActivityOptions.makeSceneTransitionAnimation(ct,
                                    Pair.create(ct.findViewById(R.id.home_toolbar), "toolbar"),
                                    Pair.create(holder.cover, "cover"),
                                    Pair.create(navigation, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME)).toBundle();
                        else
                            bundle = ActivityOptions.makeSceneTransitionAnimation(ct,
                                    Pair.create(ct.findViewById(R.id.home_toolbar), "toolbar"),
                                    Pair.create(holder.cover, "cover"),
                                    Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME),
                                    Pair.create(navigation, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME)).toBundle();

                        ct.startActivity(intent, bundle);
                    }
                    else
                        ct.startActivity(intent);
                     */
                    // If, for whatever reason, I am ever bringing this back, don't forget to add this
                    // At the very beginning of ChapterDownloaderActivity.java#onCreate.
                    /*
                getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
                getWindow().setSharedElementsUseOverlay(false);

                Fade fade = new Fade();
                // Set an enter transition
                getWindow().setEnterTransition(new Fade());
                getWindow().setSharedElementExitTransition(fade);
                // Set an exit transition
                getWindow().setExitTransition(null);
             */

                    ct.startActivity(intent);
                });
                break;
            }
            case ITEM_TITLE:
            {
                int position = adapterPositionToArrayMap.get(pos).second;

                LabelViewHolder holder = (LabelViewHolder) h;
                holder.volume.setText(Relationship.getTranslatedRelationship(ct, titleIndexes.get(position)));
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        int pos = adapterPositionToArrayMap.get(position).second;
        ((MangaViewHolder) holder).cover.setImageBitmap(covers[pos]);
    }

    @Override
    public int getItemCount() {
        return adapterPositionToArrayMap.size();
    }

    @Override
    public long getItemId(int position) {
        return mangas[adapterPositionToArrayMap.get(position).second].hashCode() ^ adapterPositionToArrayMap.get(position).first;
    }

    public ArrayList<Pair<Integer, Integer>> getAdapterPositionToArrayMap() {
        return adapterPositionToArrayMap;
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
}
