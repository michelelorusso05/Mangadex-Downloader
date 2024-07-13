package com.littleProgrammers.mangadexdownloader;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.littleProgrammers.mangadexdownloader.utils.Animations;
import com.littleProgrammers.mangadexdownloader.utils.FolderUtilities;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;

public class AdapterRecyclerDownloadedMangas extends RecyclerView.Adapter<AdapterRecyclerDownloadedMangas.ViewHolder> {
    Context ct;
    private final ArrayList<MangaHolderWrapper> downloadedMangas;
    RecyclerView self;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mangaTitle;
        TextView chaptersAndSizes;
        ImageButton button;
        RecyclerView rView;
        View touchArea;
        LinearLayout expandableView;

        public ViewHolder(View view) {
            super(view);

            mangaTitle = view.findViewById(R.id.downloadedMangaName);
            rView = view.findViewById(R.id.downloadedChapters);
            button = view.findViewById(R.id.dropdown);
            touchArea = view.findViewById(R.id.touchArea);
            expandableView = view.findViewById(R.id.expandableView);
            chaptersAndSizes = view.findViewById(R.id.chaptersAndSizes);
        }
    }

    public AdapterRecyclerDownloadedMangas(Context _ct, @NonNull File[] _downloadedFolders, RecyclerView _self) {
        ct = _ct;
        downloadedMangas = new ArrayList<>();
        for (File f : _downloadedFolders)
            downloadedMangas.add(new MangaHolderWrapper(f, false));
        self = _self;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(ct)
                .inflate(R.layout.downloaded_manga, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public long getItemId(int position) {
        return downloadedMangas.get(position).hashCode();
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        String curElement = downloadedMangas.get(viewHolder.getAdapterPosition()).file.toString();
        viewHolder.mangaTitle.setText(curElement.substring(curElement.lastIndexOf("/") + 1));

        MangaHolderWrapper mHW = downloadedMangas.get(viewHolder.getAdapterPosition());

        viewHolder.expandableView.setVisibility(mHW.isExpanded ? View.VISIBLE : View.GONE);
        // viewHolder.chaptersAndSizes.setVisibility(mHW.isExpanded ? View.GONE : View.VISIBLE);
        Animations.toggleArrow(viewHolder.button, mHW.isExpanded);

        View.OnClickListener commonListener = v -> {
            mHW.isExpanded = !mHW.isExpanded;
            notifyItemChanged(viewHolder.getAdapterPosition());
        };
        viewHolder.button.setOnClickListener(commonListener);
        viewHolder.touchArea.setOnClickListener(commonListener);

        File file = downloadedMangas.get(viewHolder.getAdapterPosition()).file;
        String size = new DecimalFormat("#.##").format((double) FolderUtilities.SizeOfFolder(file) / (1024 * 1024));
        int chapterNo = Objects.requireNonNull(file.list()).length;
        viewHolder.chaptersAndSizes.setText(ct.getString((chapterNo == 1) ? R.string.chaptersAndSizesSingle : R.string.chaptersAndSizes, chapterNo, size));

        viewHolder.touchArea.setOnLongClickListener(v -> {
            new MaterialAlertDialogBuilder(ct)
                    .setTitle(R.string.deleteManga)
                    .setMessage(R.string.deleteMangaDescription)

                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        DeleteMangaAtPos(viewHolder.getAdapterPosition(), false);
                        notifyItemRemoved(viewHolder.getAdapterPosition());
                    })

                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });

        viewHolder.rView.setAdapter(new AdapterRecyclerDownloadedChapters(ct, FolderUtilities.GetOrderedFilesInPath(file),
                (boolean allClear) -> {
                    if (allClear) {
                        DeleteMangaAtPos(viewHolder.getAdapterPosition(), false);
                        notifyItemRemoved(viewHolder.getAdapterPosition());
                    }
                    else {
                        String _size = new DecimalFormat("#.##").format((double) FolderUtilities.SizeOfFolder(file) / (1024 * 1024));
                        int _chapterNo = Objects.requireNonNull(file.list()).length;
                        viewHolder.chaptersAndSizes.setText(ct.getString((chapterNo == 1) ? R.string.chaptersAndSizesSingle : R.string.chaptersAndSizes, _chapterNo, _size));
                        notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                }));
        viewHolder.rView.setLayoutManager(new LinearLayoutManager(ct));
        // viewHolder.rView.addItemDecoration(new DividerItemDecoration(ct, DividerItemDecoration.VERTICAL));
    }


    @Override
    public int getItemCount() {
        return downloadedMangas.size();
    }

    public void DeleteMangaAtPos(int pos, boolean debug) {
        if (!debug) {
            File file = downloadedMangas.get(pos).file;
            FolderUtilities.DeleteFolder(file);
        }

        // Delete adapter
        downloadedMangas.remove(pos);
    }

    public static class MangaItemAnimator extends DefaultItemAnimator {
        @Override
        public boolean animateRemove(@NonNull RecyclerView.ViewHolder holder) {
            holder.itemView.clearAnimation();
            float x = holder.itemView.getTranslationX();
            holder.itemView.setPivotY(0);
            holder.itemView.animate()
                    .setStartDelay(0)
                    .alpha(0)
                    .scaleY(0)
                    .setInterpolator(new AccelerateInterpolator(2.f))
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            dispatchRemoveFinished(holder);
                            holder.itemView.setAlpha(1);
                            holder.itemView.setTranslationX(x);
                            holder.itemView.setScaleY(1);
                        }
                    })
                    .start();
            return false;
        }

        @Override
        public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
            oldHolder.itemView.clearAnimation();
            oldHolder.itemView.setAlpha(0);
            newHolder.itemView.setAlpha(1);
            dispatchChangeFinished(newHolder, false);
            return true;
        }
    }
}

class MangaHolderWrapper {
    MangaHolderWrapper(File _f, boolean _e) {
        file = _f;
        isExpanded = _e;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < file.getName().length(); i++)
            hash += file.getName().charAt(i) * i;
        return hash;
    }

    public File file;
    public boolean isExpanded;
}
