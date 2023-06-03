package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import java.util.ArrayList;

public class OfflinePagesAdapter extends ReaderPagesAdapter {
    public OfflinePagesAdapter(Activity ctx, String folder, String[] files, int navigationMode, boolean landscape, ArrayList<Pair<Integer, Integer>> indexes) {
        super(ctx, folder, files, navigationMode, landscape);
        if (landscape) {
            this.indexes.clear();
            this.indexes.addAll(indexes);
        }
    }

    @Override
    protected void loadBitmapAtPosition(int pos, LoadBitmap callback) {
        new Thread(() -> callback.onLoadFinished(BitmapFactory.decodeFile(baseUrl + "/" + urls[pos], ReaderActivity.opt))).start();
    }

    @Override
    protected void preloadImageAtPosition(int pos, Consumer<Boolean> callback) {
        new Thread(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(baseUrl + "/" + urls[pos], options);

            callback.accept(options.outWidth >= options.outHeight);
        }).start();
    }
}
