package com.michelelorusso.dnsclient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
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
import okhttp3.tls.HandshakeCertificates;
import okio.BufferedSink;
import okio.Okio;

/**
 * A class for custom DNS presets.
 */
class DNSPreset {

    /**
     * Create a custom DNS Preset.
     * @param _r Resolver URL
     * @param _p Primary DNS
     * @param _s Secondary DNS
     */
    DNSPreset(String _r, String _p, String _s) {
        resolver = _r;
        primary = _p;
        secondary = _s;
    }
    public final String resolver;
    public final String primary;
    public final String secondary;
}


/**
 * A custom OkHttp client that implements DNS-over-HTTPS.
 */
public class DNSClient {
    private OkHttpClient client;
    private Cache cache;
    private int status = 0;

    public enum PresetDNS {
        GOOGLE,
        CLOUDFLARE
    }
    private static final DNSPreset[] presets = {
        new DNSPreset("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4"),
        new DNSPreset("https://cloudflare-dns.com/dns-query", "1.1.1.1", "1.0.0.1")
    };

    private static final CacheControl noCache;

    static {
        noCache = new CacheControl.Builder()
                .noCache()
                .build();
    }


    /**
     * Creates a DNSClient.
     * @param dns The preset DNS to be used.
     * @param ctx Used for caching. Must be non-null if parameter cached is set to true.
     * @param cached Enable or disable caching.
     * @param maxRequests Override max async requests in queue.
     * @see DNSPreset
     */
    public DNSClient(@NonNull PresetDNS dns, @Nullable Context ctx, boolean cached, int maxRequests, boolean forceHTTP1_1) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);

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

            OkHttpClient.Builder builder = bootstrapClient
                    .newBuilder()
                    .dns(DNS)
                    .dispatcher(dispatcher)
                    //.connectionPool(new ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
                    .protocols(forceHTTP1_1 ? Collections.singletonList(Protocol.HTTP_1_1) : Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS);

            if (cached && ctx != null) {
                File httpCacheDirectory = new File(ctx.getExternalCacheDir(), "http-cache");
                int cacheSize = 100 * 1024 * 1024; // 100 MiB
                cache = new Cache(httpCacheDirectory, cacheSize);
                builder
                        .addNetworkInterceptor(new CacheInterceptor())
                        .cache(cache);
            }

            // Manually patching the R3 certificate on Android 7.1.1 and lower, since it isn't
            // supported by default.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                String cert = "-----BEGIN CERTIFICATE-----\n" +
                        "MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw\n" +
                        "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
                        "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4\n" +
                        "WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu\n" +
                        "ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY\n" +
                        "MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc\n" +
                        "h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+\n" +
                        "0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U\n" +
                        "A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW\n" +
                        "T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH\n" +
                        "B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC\n" +
                        "B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv\n" +
                        "KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn\n" +
                        "OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn\n" +
                        "jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw\n" +
                        "qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI\n" +
                        "rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV\n" +
                        "HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq\n" +
                        "hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL\n" +
                        "ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ\n" +
                        "3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK\n" +
                        "NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5\n" +
                        "ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur\n" +
                        "TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC\n" +
                        "jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc\n" +
                        "oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq\n" +
                        "4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA\n" +
                        "mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d\n" +
                        "emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=\n" +
                        "-----END CERTIFICATE-----\n";

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate isgCertificate = cf.generateCertificate(new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8)));

                HandshakeCertificates certificates = new HandshakeCertificates.Builder()
                        .addTrustedCertificate((X509Certificate) isgCertificate)
                        .build();
                builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager());
            }
            client = builder.build();
        } catch (UnknownHostException | CertificateException e) {
            status = -1;
            client = new OkHttpClient.Builder().build();
        }

        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.ALL);
    }

    /**
     * Creates a DNSClient.
     * @param dns The preset DNS to be used.
     * @param ctx Used for caching. Must be non-null if parameter cached is set to true.
     * @param cached Enable or disable caching.
     * @see DNSPreset
     */
    public DNSClient(@NonNull PresetDNS dns, @Nullable Context ctx, boolean cached) {
        this(dns, ctx, cached, 100, false);
    }
    /**
     * Creates a DNSClient.
     * @param dns The preset DNS to be used.
     * @see DNSPreset
     */
    public DNSClient(PresetDNS dns) {
        this(dns, null, false);
    }


    /**
     * Starts a synchronous, thread blocking HTTP GET request.
     * @param url The target url for the request.
     * @return The fethced response. Must be closed by calling response.close() after consuming.
     * @throws IOException If the request could not be executed due to cancellation, a connectivity problem or timeout.
     */
    public Response HttpRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        return client.newCall(request).execute();
    }

    /**
     * Enqueues an asynchronous request.
     * @param url The target url for the request.
     * @param callback The callback to be executed after the response was received.
     * @see Callback
     */
    public void HttpRequestAsync(String url, @NonNull Callback callback) {
        HttpRequestAsync(url, callback, true);
    }
    /**
     * Enqueues an asynchronous request.
     * @param url The target url for the request.
     * @param callback The callback to be executed after the response was received.
     * @param cache If cache should be enabled for this request.
     * @see Callback
     */
    public void HttpRequestAsync(String url, @NonNull Callback callback, boolean cache) {
        Request.Builder b = new Request.Builder()
                //.header("Connection", "keep-alive")
                //.header("Transfer-Encoding", "chunked")
                .url(url);

        if (!cache)
            b.cacheControl(noCache);

        Request request = b.build();

        client.newCall(request)
                .enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                callback.onResponse(call, response);
                response.close();
            }
        });
    }


    public interface GetImage {
        void Execute(Bitmap bm, boolean success);
    }

    /**
     * Gets an image from an URL, and executes a callback after receiving in which the resulting Bitmap can be processed.
     * @param url The image URL.
     * @param action The action to be executed after the image was received. Can be replaced with a lambda: (Bitmap bm) -> {...}
     * @param opts Custom options to be applied to the fetched Bitmap.
     * @see BitmapFactory.Options
     */
    public void GetImageBitmapAsync(String url, GetImage action, BitmapFactory.Options opts) {
        HttpRequestAsync(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                action.Execute(null, false);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                ResponseBody body = Objects.requireNonNull(response.body());
                Bitmap bm = BitmapFactory.decodeStream(body.byteStream(), null, opts);
                action.Execute(bm, true);
                response.close();
            }
        }, false);
    }
    /**
     * Gets an image from an URL, and executes a callback after receiving in which the resulting Bitmap can be processed.
     * @param url The image URL.
     * @param action The action to be executed after the image was received. Can be replaced with a lambda: (Bitmap bm) -> {...}
     */
    public void GetImageBitmapAsync(String url, GetImage action) {
        GetImageBitmapAsync(url, action, null);
    }


    /**
     * Gets an image from a URL in a thread-blocking way.
     * @param url The image URL.
     * @return The requested image in a Bitmap object.
     * @throws IOException If the request could not be completed.
     */
    public Bitmap GetImageBitmap(String url) throws IOException {
        Response response = HttpRequest(url);
        ResponseBody body = Objects.requireNonNull(response.body());
        Bitmap b = BitmapFactory.decodeByteArray(body.bytes(), 0, (int) body.contentLength());
        response.close();
        return b;
    }


    /**
     * Gets an image from a URL and loads it in a ImageView.
     * @param url The image URL.
     * @param view The target ImageView.
     * @param caller The Activity that manages the target view.
     * @param callback Additional operations to be executed after the image fetching.
     * @param opts Additional options to pass to BitmapFactory.
     * @see Callback
     */
    public void ImageIntoViewAsync(String url, ImageView view, Activity caller, Callback callback, BitmapFactory.Options opts) {
        HttpRequestAsync(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null)
                    callback.onFailure(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = Objects.requireNonNull(response.body());
                Bitmap bm = BitmapFactory.decodeByteArray(body.bytes(), 0, (int) body.contentLength(), opts);
                caller.runOnUiThread(() -> view.setImageBitmap(bm));

                if (callback != null)
                    callback.onResponse(call, response);

                response.close();
            }
        });
    }
    public void ImageIntoViewAsync(String url, ImageView view, Activity caller, Callback callback) {
        ImageIntoViewAsync(url, view, caller, callback, null);
    }

    /**
     * Downloads a file in a thread-blocking way.
     * @param url The file URL.
     * @param destination The target destination as a File object.
     * @throws IOException If the request could not be executed.
     * @see File
     */
    public void DownloadFile(String url, File destination) throws IOException {
        Response response = HttpRequest(url);
        ResponseBody body = Objects.requireNonNull(response.body());
        BufferedSink sink = Okio.buffer(Okio.sink(destination));
        sink.writeAll(body.source());
        sink.close();

        response.close();
    }

    public interface DownloadCallback {
        void onFailure(@NonNull Call call, @NonNull IOException e);
        void onResponse(@NonNull Call call, @NonNull Response response, long bytes);
    }

    /**
     * Downloads a file in the background.
     * @param url The file URL.
     * @param destination The target destination as a File object.
     * @param onDownloadEnd Custom actions to be executed after the download.
     * @see File
     * @see Callback
     */
    public void DownloadFileAsync(String url, File destination, DownloadCallback onDownloadEnd) {
        HttpRequestAsync(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                onDownloadEnd.onFailure(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        BufferedSink sink = Okio.buffer(Okio.sink(destination));
                        ResponseBody body = Objects.requireNonNull(response.body());
                        long size = sink.writeAll(body.source());
                        sink.close();
                        onDownloadEnd.onResponse(call, response, size);
                    } catch (Exception e) {
                        e.printStackTrace();
                        onDownloadEnd.onFailure(call, new IOException());
                    }
                }
                else {
                    onDownloadEnd.onFailure(call, new UnknownHostException());
                }
                response.close();
            }
        });
    }


    /**
     * Cancels any pending async request.
     */
    public void CancelAllPendingRequests() {
        client.dispatcher().cancelAll();
    }


    /**
     * Checks if all the custom properties of the OkHttpClient were applied.
     * @return -1 if something failed during the constructor call and the resulting DNSClient is just a simple OkHttpClient.
     */
    public int GetStatus() { return status; }
    public List<Call> GetRunningRequests() {
        return client.dispatcher().runningCalls();
    }
    public List<Call> GetQueuedRequests() {
        return client.dispatcher().queuedCalls();
    }

    @NonNull
    public Cache getCache() {
        if (cache == null) throw new NullPointerException("Cache has not been set for this client.");
        return cache;
    }

    /**
     * Shutdowns client, closes cache (if open), and refuses further calls.
     */
    public void shutdown() {
        CancelAllPendingRequests();
        client.dispatcher().executorService().shutdown();
        try {
            if (cache != null) cache.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static class CacheInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());

            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(Integer.MAX_VALUE, TimeUnit.SECONDS)
                    .maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS)
                    .noTransform()
                    .immutable()
                    .build();

            Response.Builder builder = response.newBuilder();

            return builder
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        }
    }

    public OkHttpClient getClient() {
        return client;
    }
}
