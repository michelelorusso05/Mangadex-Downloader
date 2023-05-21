package com.littleProgrammers.mangadexdownloader;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.AtHomeResults;
import com.littleProgrammers.mangadexdownloader.utils.FolderUtilities;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChapterDownloadWorker extends Worker {
    private static final int NOTIFICATION_ID = 6271;
    private final DNSClient client;
    private final String selectedChapterID;
    private final String selectedMangaName;
    private final String formattedTitle;

    private final Context context;
    private final int uniqueID;
    private final Semaphore mutex;
    private boolean success = false;
    private boolean interrupted = false;
    private int failedDownloads;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final boolean hasNotificationPermissions;

    @Override
    public void onStopped() {
        super.onStopped();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(uniqueID);

        success = false;
        interrupted = true;
        client.CancelAllPendingRequests();
        DeleteTempFolder();

        mutex.release();
    }

    public ChapterDownloadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

        Data data = getInputData();

        selectedChapterID = data.getString("Chapter");
        selectedMangaName = data.getString("Manga");
        formattedTitle = data.getString("Title");
        this.context = context;

        client = new DNSClient(DNSClient.PresetDNS.CLOUDFLARE, context, false, 64, false);

        assert selectedChapterID != null;
        uniqueID = selectedChapterID.hashCode();
        mutex = new Semaphore(0);

        hasNotificationPermissions = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    @Override
    public Result doWork() {
        downloadChapter();
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return success ? Result.success() : Result.failure();
    }

    @SuppressLint({"MissingPermission", "InlinedApi"})
    private void downloadChapter() {
        boolean HQ = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dataSaver", false);

        File cacheFolder = new File(context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/");
        if (!cacheFolder.exists()) {
            if (!cacheFolder.mkdirs()) Log.d("Error", "Unable to create folder");
        }

        File mangaFolder = new File(context.getExternalFilesDir(null) + "/Manga/");
        if (!mangaFolder.exists()) {
            if (!mangaFolder.mkdir()) Log.d("Error", "Unable to create folder");
        }

        Intent i = new Intent(context, DownloadStopper.class);
        i.setAction("com.michelelorusso.mangadexdownloader.STOP_DOWNLOAD");
        i.putExtra("workID", selectedChapterID);

        PendingIntent stopDownload;
        stopDownload = PendingIntent.getBroadcast(context, uniqueID, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create notification
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), String.valueOf(NOTIFICATION_ID))
                .setSilent(true)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(0, 0, true)
                .addAction(R.drawable.ic_baseline_stop_24, context.getString(R.string.stopDownload), stopDownload);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (hasNotificationPermissions)
            notificationManager.notify(uniqueID, builder.build());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        client.HttpRequestAsync("https://api.mangadex.org/at-home/server/" + selectedChapterID, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (failedDownloads <= MAX_FAILED_ATTEMPTS) {
                    call.clone().enqueue(this);
                    Log.w("Richiesta non riuscita", "Tentativi effettuati: " + failedDownloads + "/5");
                    failedDownloads++;
                }
                else {
                    OnDownloadFail();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (interrupted) {
                    response.close();
                    return;
                }
                if (!response.isSuccessful()) {
                    OnDownloadFail();
                }
                AtHomeResults hResults = mapper.readValue(Objects.requireNonNull(response.body()).string(), AtHomeResults.class);
                response.close();

                // Clean temporary directory
                File dir = new File(context.getExternalFilesDir(null) +"/mangadexDownloaderTemp/" + selectedChapterID + "/");
                FolderUtilities.DeleteFolderContents(dir);

                String baseUrl;
                String[] images;

                if (HQ) {
                    baseUrl = hResults.getBaseUrl() + "/data/";
                    images = hResults.getChapter().getData();
                }
                else {
                    baseUrl = hResults.getBaseUrl() + "/data-saver/";
                    images = hResults.getChapter().getDataSaver();
                }

                // Create notification
                builder
                        .setContentText("0%")
                        .setProgress(images.length, 0, false);
                if (hasNotificationPermissions)
                    notificationManager.notify(uniqueID, builder.build());

                String[] urls = new String[images.length];
                for (int i = 0; i < urls.length; i++)
                    urls[i] = baseUrl + hResults.getChapter().getHash() + "/" + images[i];

                MultipleFileDownloader downloader = new MultipleFileDownloader(client, urls,
                        new File(context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/"));
                downloader.setOnDownloadCompleted(() -> OnDownloadEnd());
                downloader.setOnDownloadFailCallback(() -> OnDownloadFail());
                downloader.setOnProgressUpdate((progress, total) -> {
                    // Update notification
                    builder
                            .setContentText(Math.round((float) (progress) / total * 100) + "%")
                            .setProgress(total, progress, false);
                    if (hasNotificationPermissions)
                        notificationManager.notify(uniqueID, builder.build());
                });
                downloader.start();
            }
        });
    }
    @SuppressLint("MissingPermission")
    private void OnDownloadEnd() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(uniqueID);

        // Create download folder
        boolean HQ = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dataSaver", false);
        File targetFolder = new File(context.getExternalFilesDir(null) + "/Manga/" + selectedMangaName + "/" + formattedTitle + (HQ ? ".hq" : ".lq") + "/");
        File tempFolder = new File(context.getExternalFilesDir(null) +"/mangadexDownloaderTemp/" + selectedChapterID + "/");

        Log.d("FOLDER", tempFolder.toString());

        String[] files = tempFolder.list();
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            Log.d("Error", "Unable to create folder");
            OnDownloadFail();
            return;
        }
        if (targetFolder.exists() && tempFolder.exists() && files != null) {
            for (String file : files) {
                FolderUtilities.MoveFile(new File(tempFolder, file), new File(targetFolder, file));
            }
        }

        String[] images = targetFolder.list();
        images = (images != null ? images : new String[0]);
        Arrays.sort(images);

        Intent intent = new Intent(context, OfflineReaderActivity.class);
        intent.setAction("com.michelelorusso.mangadexdownloader.OPEN_DOWNLOAD");
        intent.putExtra("baseUrl", targetFolder.getAbsolutePath());
        intent.putExtra("urls", images);

        @SuppressLint("InlinedApi")
        PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builderCompleted = new NotificationCompat.Builder(getApplicationContext(), String.valueOf(NOTIFICATION_ID))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.downloadCompleted))
                .setContentText(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (hasNotificationPermissions)
            notificationManager.notify(uniqueID, builderCompleted.build());

        client.shutdown();
        DeleteTempFolder();
        success = true;
        mutex.release();
    }
    @SuppressLint({"MissingPermission", "InlinedApi"})
    private void OnDownloadFail() {
        // ALl of the cleanup is already taken care of in the overrided onStopped
        if (interrupted) return;
        interrupted = true;

        client.CancelAllPendingRequests();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        Context context = getApplicationContext();
        notificationManager.cancel(uniqueID);

        Intent i = new Intent(context, DownloadRetryer.class);
        i.setAction("com.michelelorusso.mangadexdownloader.RETRY_DOWNLOAD");
        i.putExtra("workID", selectedChapterID);
        i.putExtra("Chapter", selectedChapterID);
        i.putExtra("Manga", selectedMangaName);
        i.putExtra("Title", formattedTitle);

        PendingIntent retryDownload;
        retryDownload = PendingIntent.getBroadcast(context, uniqueID, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builderCompleted = new NotificationCompat.Builder(getApplicationContext(), String.valueOf(NOTIFICATION_ID))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.downloadFailed))
                .setContentText(context.getString(R.string.downloadMoreInfo))
                .setSubText(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_baseline_file_download_24, context.getString(R.string.downloadRetry), retryDownload);
        if (hasNotificationPermissions)
            notificationManager.notify(uniqueID, builderCompleted.build());

        client.shutdown();
        DeleteTempFolder();
        mutex.release();
    }

    private void DeleteTempFolder() {
        Log.d("DELETING FOLDER", "AAA");
        File tempFolder = new File( context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/");
        FolderUtilities.DeleteFolder(tempFolder);
    }
}