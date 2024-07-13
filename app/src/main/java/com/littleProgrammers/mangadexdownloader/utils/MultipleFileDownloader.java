package com.littleProgrammers.mangadexdownloader.utils;

import androidx.annotation.NonNull;

import com.michelelorusso.dnsclient.DNSClient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MultipleFileDownloader {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final DNSClient client;
    private final Queue<String> urls;
    private final File dst;
    private final AtomicInteger failedAttempts;
    private final AtomicInteger progress;
    private final int total;
    private Runnable onDownloadFail;
    @FunctionalInterface
    public interface OnProgressUpdate { void progress(int progress, int total); }
    private OnProgressUpdate onProgressUpdate;
    private Runnable onDownloadCompleted;
    private boolean stopped;

    private int concurrentDownloads;
    public MultipleFileDownloader(DNSClient client, String[] urls, File dst, int concurrentDownloads) {
        this.client = client;
        this.urls = new LinkedBlockingQueue<>(Arrays.asList(urls));
        this.dst = dst;
        this.failedAttempts = new AtomicInteger(0);
        this.total = urls.length;
        this.progress = new AtomicInteger(0);
        this.concurrentDownloads = concurrentDownloads;
    }
    public MultipleFileDownloader(DNSClient client, String[] urls, File dst) {
        this(client, urls, dst, 1);
    }
    public void setOnDownloadFailCallback(Runnable runnable) {
        this.onDownloadFail = runnable;
    }
    public void setOnProgressUpdate(OnProgressUpdate onProgressUpdate) {
        this.onProgressUpdate = onProgressUpdate;
    }

    public void setOnDownloadCompleted(Runnable onDownloadCompleted) {
        this.onDownloadCompleted = onDownloadCompleted;
    }

    public void start() {
        if (concurrentDownloads == -1) throw new IllegalStateException("Already executing.");
        for (int i = 0; i < concurrentDownloads; i++) {
            execute();
        }
        concurrentDownloads = -1;
    }
    private void execute() {
        int pos;
        String url;
        synchronized (urls) {
            pos = total - urls.size();
            url = urls.poll();
        }
        if (url == null) return;
        synchronized (client) {
            client.DownloadFileAsync(url, new File(dst, nameFromPosition(url, pos, total)), new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();

                    if (stopped) return;

                    if (failedAttempts.incrementAndGet() <= MAX_FAILED_ATTEMPTS) {
                        call.clone().enqueue(this);
                    }
                    else {
                        if (onDownloadFail != null) onDownloadFail.run();
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (stopped) return;

                    int curProgress = progress.incrementAndGet();
                    if (onProgressUpdate != null) onProgressUpdate.progress(curProgress, total);
                    if (curProgress == total) {
                        if (onDownloadCompleted != null) onDownloadCompleted.run();
                    }
                    else
                        execute();
                }
            });
        }
    }
    public void stop() {
        client.CancelAllPendingRequests();
        stopped = true;
    }

    private static String nameFromPosition(String originalFilename, int pos, int total) {
        int lengthOfNumberOfPages = (int) (Math.floor(Math.log10(total)) + 1);
        int lengthOfI = (int) (Math.floor(Math.log10(pos + 1)) + 1);
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));

        return new String(new char[(int) (lengthOfNumberOfPages - lengthOfI)]).replace("\0", "0") + (pos + 1)
                + fileExtension;
    }
}
