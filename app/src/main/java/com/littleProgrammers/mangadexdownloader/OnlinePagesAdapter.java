package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OnlinePagesAdapter extends ReaderPagesAdapter {

    DNSClient client;

    public OnlinePagesAdapter(Activity ctx, String baseUrl, String[] urls, DNSClient sharedClient, int navigationMode, boolean landscape) {
        super(ctx, baseUrl, urls, navigationMode, landscape);
        client = sharedClient;

        startPreload();
    }
    @Override
    protected void loadBitmapAtPosition(int pos, LoadBitmap callback) {
        client.HttpRequestAsync(baseUrl + "/" + urls[pos], new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = Objects.requireNonNull(response.body());
                byte[] content = body.bytes();
                Bitmap bm = fromBytes(content);
                response.close();
                callback.onLoadFinished(bm);
            }
        }, true);
    }

    @Override
    protected void preloadImageAtPosition(int pos, Consumer<Boolean> callback) {
        String url = baseUrl + "/" + urls[pos];

        client.HttpRequestAsync(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("Preloading", "Preloading of " + url + " failed");
                call.clone().enqueue(this);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                try {
                    ResponseBody body = Objects.requireNonNull(response.body());
                    // Preload the entire file
                    BitmapFactory.decodeByteArray(body.bytes(), 0, (int) body.contentLength(), options);
                    // Preload just enough to get image's height and width
                    // BitmapFactory.decodeStream(body.byteStream(), null, options);
                    response.close();
                } catch (Exception e) {
                    Log.w("HTTP/2 error", "Server has reset stream");
                    call.clone().enqueue(this);
                    return;
                }
                callback.accept(options.outWidth >= options.outHeight);
            }
        }, true);
    }
}
