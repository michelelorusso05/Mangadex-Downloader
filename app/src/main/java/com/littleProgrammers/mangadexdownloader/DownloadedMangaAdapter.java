package com.littleProgrammers.mangadexdownloader;


import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class DownloadedMangaAdapter extends RecyclerView.Adapter<DownloadedMangaAdapter.ViewHolder> {
    Context ct;
    private final ArrayList<MangaHolderWrapper> downloadedMangas;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mangaTitle;
        ImageButton button;
        RecyclerView rView;
        View touchArea;
        View expandableView;

        public ViewHolder(View view) {
            super(view);

            mangaTitle = view.findViewById(R.id.downloadedMangaName);
            rView = view.findViewById(R.id.downloadedChapters);
            button = view.findViewById(R.id.dropdown);
            touchArea = view.findViewById(R.id.touchArea);
            expandableView = view.findViewById(R.id.expandableView);
        }
    }

    public DownloadedMangaAdapter(Context _ct, File[] _downloadedFolders) {
        ct = _ct;
        downloadedMangas = new ArrayList<>();
        for (File f : _downloadedFolders)
            downloadedMangas.add(new MangaHolderWrapper(f, false));
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
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        String curElement = downloadedMangas.get(viewHolder.getAdapterPosition()).file.toString();
        viewHolder.mangaTitle.setText(curElement.substring(curElement.lastIndexOf("/") + 1));

        MangaHolderWrapper mHW = downloadedMangas.get(viewHolder.getAdapterPosition());

        viewHolder.expandableView.setVisibility(mHW.isExpanded ? View.VISIBLE : View.GONE);
        Animations.toggleArrow(viewHolder.button, mHW.isExpanded);

        View.OnClickListener commonListener = v -> {
            mHW.isExpanded = !mHW.isExpanded;
            notifyItemChanged(viewHolder.getAdapterPosition());
        };
        viewHolder.button.setOnClickListener(commonListener);
        viewHolder.touchArea.setOnClickListener(commonListener);

        File file = downloadedMangas.get(viewHolder.getAdapterPosition()).file;

        viewHolder.touchArea.setOnLongClickListener(v -> {
            new AlertDialog.Builder(ct)
                    .setTitle(R.string.deleteManga)
                    .setMessage(R.string.deleteMangaDescription)

                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        DeleteMangaAtPos(viewHolder.getAdapterPosition(), false);
                        notifyItemRemoved(viewHolder.getAdapterPosition());
                    })

                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        });

        viewHolder.rView.setAdapter(new DownloadedChapterAdapter(ct, FolderUtilities.GetOrderedFilesInPath(file),
                (boolean allClear) -> {
                    if (allClear) {
                        DeleteMangaAtPos(viewHolder.getAdapterPosition(), false);
                        notifyItemRemoved(viewHolder.getAdapterPosition());
                    }
                    else
                        notifyItemChanged(viewHolder.getAdapterPosition());
                }));
        viewHolder.rView.setLayoutManager(new LinearLayoutManager(ct));
    }

    // Return the size of your dataset (invoked by the layout manager)
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
