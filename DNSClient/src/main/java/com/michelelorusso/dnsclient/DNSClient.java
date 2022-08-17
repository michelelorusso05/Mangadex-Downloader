package com.michelelorusso.dnsclient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.dnsoverhttps.DnsOverHttps;
import okio.BufferedSink;
import okio.Okio;

class DNSPreset {
    DNSPreset(String _r, String _p, String _s) {
        resolver = _r;
        primary = _p;
        secondary = _s;
    }
    public String resolver;
    public String primary;
    public String secondary;
}

public class DNSClient {
    private OkHttpClient client;
    private int status = 0;

    public enum PresetDNS {
        GOOGLE,
        CLOUDFLARE
    }
    private static final DNSPreset[] presets = {
        new DNSPreset("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4"),
        new DNSPreset("https://cloudflare-dns.com/dns-query", "1.1.1.1", "1.0.0.1")
    };

    public DNSClient(@NonNull PresetDNS dns, @Nullable Context ctx, boolean cached, int maxRequests) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequests);

        DNSPreset selectedDNS = presets[dns.ordinal()];
        OkHttpClient bootstrapClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .build();

        Dns DNS;
        try {
            DNS = new DnsOverHttps.Builder().client(bootstrapClient)
                    .url(HttpUrl.Companion.get(selectedDNS.resolver))
                    .bootstrapDnsHosts(InetAddress.getByName(selectedDNS.secondary), InetAddress.getByName(selectedDNS.primary))
                    .build();

            OkHttpClient.Builder builder = bootstrapClient.newBuilder().dns(DNS).dispatcher(dispatcher)
                    .connectionPool(new ConnectionPool(100, 15000, TimeUnit.MILLISECONDS))
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS);

            if (cached && ctx != null) {
                File httpCacheDirectory = new File(ctx.getExternalCacheDir(), "http-cache");
                int cacheSize = 10 * 1024 * 1024; // 10 MiB
                Cache cache = new Cache(httpCacheDirectory, cacheSize);
                builder
                        .addNetworkInterceptor(new CacheInterceptor())
                        .cache(cache);
            }

            client = builder.build();
        } catch (UnknownHostException e) {
            status = -1;
            client = new OkHttpClient.Builder().build();
        }
        // Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    }
    public DNSClient(@NonNull PresetDNS dns, @Nullable Context ctx, boolean cached) {
        this(dns, ctx, cached, 100);
    }
    public DNSClient(PresetDNS dns) {
        this(dns, null, false);
    }

    public Response HttpRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        return client.newCall(request).execute();
    }
    public void AsyncHttpRequest(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public Bitmap ImageIntoView(String url) throws IOException {
        Response response = HttpRequest(url);
        ResponseBody body = Objects.requireNonNull(response.body());
        return BitmapFactory.decodeByteArray(body.bytes(), 0, (int) body.contentLength());
    }

    public interface GetImage {
        void Execute(Bitmap bm);
    }
    public void GetImageBitmapAsync(String url, GetImage action, BitmapFactory.Options opts) {
        AsyncHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                ResponseBody body = Objects.requireNonNull(response.body());
                Bitmap bm = BitmapFactory.decodeStream(body.byteStream(), null, opts);
                action.Execute(bm);
                response.close();
            }
        });
    }
    public void GetImageBitmapAsync(String url, GetImage action) {
        GetImageBitmapAsync(url, action, null);
    }

    public void ImageIntoViewAsync(String url, ImageView view, Activity caller, Callback callback) {
        AsyncHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null)
                    callback.onFailure(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = Objects.requireNonNull(response.body());
                Bitmap bm = BitmapFactory.decodeByteArray(body.bytes(), 0, (int) body.contentLength());
                caller.runOnUiThread(() -> view.setImageBitmap(bm));

                if (callback != null)
                    callback.onResponse(call, response);

                response.close();
            }
        });
    }
    public void ImageIntoViewAsync(String url, ImageView view, Activity caller) {
        ImageIntoViewAsync(url, view, caller, null);
    }

    public void DownloadFile(String url, File destination) throws IOException {
        Response response = HttpRequest(url);
        ResponseBody body = Objects.requireNonNull(response.body());
        BufferedSink sink = Okio.buffer(Okio.sink(destination));
        sink.writeAll(body.source());
        sink.close();

        response.close();
    }

    public void DownloadFileAsync(String url, File destination, Callback onDownloadEnd) {
        AsyncHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                onDownloadEnd.onFailure(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    if (response.body() == null)
                        Log.d("Ahia", "Ahimè");
                    else
                    {
                        try {
                            BufferedSink sink = Okio.buffer(Okio.sink(destination));
                            ResponseBody body = Objects.requireNonNull(response.body());
                            sink.writeAll(body.source());
                            sink.close();
                            onDownloadEnd.onResponse(call, response);
                        } catch (Exception e) {
                            Log.d("non posso scrivere", "aiut");
                            e.printStackTrace();
                            onDownloadEnd.onFailure(call, new IOException());
                        }
                    }
                }
                else {
                    Log.d("Unable to download", "ahimè");
                    onDownloadEnd.onFailure(call, new UnknownHostException());
                }
                response.close();
            }
        });
    }

    public void CancelAllPendingRequests() {
        client.dispatcher().cancelAll();
    }

    public int GetStatus() { return status; }

    public static class CacheInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());

            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(30, TimeUnit.MINUTES)
                    .build();

            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        }
    }
}
