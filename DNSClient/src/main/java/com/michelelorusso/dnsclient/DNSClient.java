package com.michelelorusso.dnsclient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
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
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import okhttp3.tls.HandshakeCertificates;
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
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS);

            if (cached && ctx != null) {
                File httpCacheDirectory = new File(ctx.getExternalCacheDir(), "http-cache");
                int cacheSize = 10 * 1024 * 1024; // 10 MiB
                Cache cache = new Cache(httpCacheDirectory, cacheSize);
                builder
                        .addNetworkInterceptor(new CacheInterceptor())
                        .cache(cache);
            }

            // Unfortunately, mangadex.org uses letsencrypt's R3 certificates. That means that
            // the root CA certificates won't be accepted on Android versions prior to 7.1.1.
            // Because of this, I have to patch the certificate manually.
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
                        // Uncomment to allow connection to any site generally, but could possibly cause
                        // noticeable memory pressure in Android apps.
//              .addPlatformTrustedCertificates()
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
