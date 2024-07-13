package com.littleProgrammers.mangadexdownloader;

import static android.view.View.GONE;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.littleProgrammers.mangadexdownloader.utils.FolderUtilities;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class AdapterRecyclerDownloadedChapters extends RecyclerView.Adapter<AdapterRecyclerDownloadedChapters.viewHolder> {
    Context ct;
    ArrayList<File> chapters;

    final int LQ = R.drawable.icon_low_quality;
    final int HQ = R.drawable.icon_high_quality;

    public interface OnChapterDeletedCallback {
        void Execute(boolean allClear);
    }

    OnChapterDeletedCallback cb;

    public AdapterRecyclerDownloadedChapters(Context _ct, @NonNull File[] _chapters, OnChapterDeletedCallback _cb) {
        ct = _ct;
        chapters = new ArrayList<>();
        for (File file : _chapters) {
            if (file.isDirectory())
                chapters.add(file);
        }
        cb = _cb;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ct);
        View view = inflater.inflate(R.layout.downloaded_file, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {

        if (position == chapters.size() - 1)
            holder.divider.setVisibility(GONE);

        File curFile = chapters.get(holder.getAdapterPosition());

        String name = curFile.getName();
        holder.chapterName.setText(name.substring(0, name.length() - 3));
        String size = new DecimalFormat("#.##").format((double) FolderUtilities.SizeOfFolder(curFile) / (1024 * 1024));
        holder.fileSize.setText(size.concat(" MB"));
        holder.hqIcon.setImageDrawable(AppCompatResources.getDrawable(ct, (name.charAt(name.length() - 2) == 'h' ? HQ : LQ)));
        holder.rowLayout.setOnClickListener(v -> {
            String[] files = curFile.list();
            files = (files != null ? files : new String[0]);
            Arrays.sort(files);

            Intent intent = new Intent(ct, ActivityOfflineReader.class);
            intent.putExtra("baseUrl", curFile.getAbsolutePath());
            intent.putExtra("urls", files);
            ct.startActivity(intent);
        });
        holder.rowLayout.setOnLongClickListener((View v) -> {
            new MaterialAlertDialogBuilder(ct)
                    .setTitle(R.string.deleteChapter)
                    .setMessage(R.string.deleteChapterDescription)

                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // Delete file
                        FolderUtilities.DeleteFolder(curFile);

                        // Delete adapter
                        chapters.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());

                        cb.Execute(chapters.size() == 0);
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    public static class viewHolder extends RecyclerView.ViewHolder {
        TextView chapterName;
        TextView fileSize;
        ImageView hqIcon;
        View rowLayout;
        View divider;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            chapterName = itemView.findViewById(R.id.chapterName);
            rowLayout = itemView.findViewById(R.id.rowLayout);
            fileSize = itemView.findViewById(R.id.fileSize);
            hqIcon = itemView.findViewById(R.id.hqIcon);
            divider = itemView.findViewById(R.id.dividerView);
        }
    }
}
