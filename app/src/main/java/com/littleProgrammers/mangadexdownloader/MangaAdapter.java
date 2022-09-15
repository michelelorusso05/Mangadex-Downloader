package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.imageview.ShapeableImageView;
import com.littleProgrammers.mangadexdownloader.apiResults.Manga;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.ByteArrayOutputStream;

public class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.viewHolder> {
    Activity ct;
    Manga[] mangas;
    Bitmap[] covers;
    DNSClient client;

    public MangaAdapter(Activity _ct, Manga[] _mangas) {
        ct = _ct;
        mangas = _mangas;
        client = new DNSClient(DNSClient.PresetDNS.GOOGLE, ct, true);

        boolean lowQualityCover = PreferenceManager.getDefaultSharedPreferences(ct).getBoolean("lowQualityCovers", false);

        covers = new Bitmap[mangas.length];
        for (int i = 0, mangasLength = mangas.length; i < mangasLength; i++) {
            Manga m = mangas[i];
            int finalI = i;
            client.GetImageBitmapAsync("https://uploads.mangadex.org/covers/" + m.getId() + "/" + m.getAttributes().getCoverUrl() + ((lowQualityCover) ? ".256.jpg" : ".512.jpg"), (Bitmap bm) -> {
               covers[finalI] = bm;
               ct.runOnUiThread(() -> notifyItemChanged(finalI));
            });
        }
    }

    public MangaAdapter(Activity _ct) {
        ct = _ct;
        mangas = new Manga[0];
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ct);
        View view = inflater.inflate(R.layout.manga_result_row, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Manga current = mangas[position];
        holder.mangaTitle.setText(HtmlCompat.fromHtml(current.getAttributes().getTitleS(), HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.mangaAuthor.setText(current.getAttributes().getAuthorString());
        new Thread(() -> ct.runOnUiThread(() -> holder.cover.setImageBitmap(covers[position]))).start();

        ObjectMapper mapper = new ObjectMapper();
        try {
            final String manga = mapper.writeValueAsString(mangas[position]);

            holder.rowLayout.setOnClickListener(v -> {
                Intent intent = new Intent(ct, ChapterDownloaderActivity.class);
                intent.putExtra("MangaData", manga);
                StaticData.sharedCover = null;
                if (covers[position] != null) {
                    StaticData.sharedCover = covers[position];
                    //ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    //covers[position].compress(Bitmap.CompressFormat.PNG, 100, stream);
                    //byte[] image = stream.toByteArray();
                    //Log.d("Size", String.valueOf(image.length));
                    //intent.putExtra("cachedCover", image);
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(ct, holder.cover, "cover");

                    ct.startActivity(intent, options.toBundle());
                }
                else
                    ct.startActivity(intent);
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return mangas.length;
    }

    public static class viewHolder extends RecyclerView.ViewHolder {
        TextView mangaTitle, mangaAuthor;
        ShapeableImageView cover;
        ConstraintLayout rowLayout;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            mangaTitle = itemView.findViewById(R.id.cardTitle);
            mangaAuthor = itemView.findViewById(R.id.cardAuthor);
            cover = itemView.findViewById(R.id.cover);
            rowLayout = itemView.findViewById(R.id.rowLayout);
        }
    }
}
