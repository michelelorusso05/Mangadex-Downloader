package com.littleProgrammers.mangadexdownloader;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.littleProgrammers.mangadexdownloader.apiResults.AtHomeResults;
import com.littleProgrammers.mangadexdownloader.db.DbChapter;
import com.littleProgrammers.mangadexdownloader.db.DbManga;
import com.littleProgrammers.mangadexdownloader.db.DbMangaDAO;
import com.littleProgrammers.mangadexdownloader.db.MangaDatabase;
import com.littleProgrammers.mangadexdownloader.utils.FolderUtilities;
import com.littleProgrammers.mangadexdownloader.utils.MultipleFileDownloader;
import com.littleProgrammers.mangadexdownloader.utils.StaticData;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WorkerChapterDownload extends Worker {
    public static final int PROGRESS_ONGOING = 0;
    public static final int PROGRESS_SUCCESS = 1;
    public static final int PROGRESS_FAILURE = 2;
    public static final int PROGRESS_CANCELED = 3;
    public static final int PROGRESS_ENQUEUED = 4;

    private static final int NOTIFICATION_ID = 6271;

    private final DNSClient client;
    private final String selectedChapterID;
    private final String selectedChapterScanGroup;
    private final String selectedMangaID;
    private final String selectedMangaName;
    private final String selectedMangaAuthor;
    private final String selectedMangaArtist;
    private final String formattedTitle;
    private final String volume;
    private final String chapter;
    private long downloadedBytes;

    private final NotificationCompat.Builder notificationBuilder;
    private final NotificationManagerCompat notificationManager;
    private final Context context;
    private final int uniqueID;
    private final Semaphore mutex;
    private boolean success = false;
    private boolean interrupted = false;
    private int failedDownloads;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final boolean hasNotificationPermissions;
    private boolean hasExceededQuota;
    private MultipleFileDownloader downloader;
    private long lastNotificationMillis;

    private final Data.Builder progressData;

    @Override
    public void onStopped() {
        super.onStopped();
        if (downloader != null)
            downloader.stop();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(uniqueID);

        success = false;
        interrupted = true;
        client.CancelAllPendingRequests();
        DeleteTempFolder();

        progressData.putInt("state", PROGRESS_CANCELED);

        mutex.release();
    }

    public WorkerChapterDownload(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

        Data data = getInputData();

        selectedChapterID = data.getString("Chapter");
        selectedChapterScanGroup = data.getString("ScanGroup");
        selectedMangaID = data.getString("MangaID");
        selectedMangaName = data.getString("Manga");
        selectedMangaAuthor = data.getString("Author");
        selectedMangaArtist = data.getString("Artist");
        formattedTitle = data.getString("Title");
        volume = data.getString("Volume");
        chapter = data.getString("ChapterNumber");

        this.context = context;

        client = new DNSClient(DNSClient.PresetDNS.CLOUDFLARE, context, false, 64, false);

        assert selectedChapterID != null;
        uniqueID = selectedChapterID.hashCode();
        mutex = new Semaphore(0);

        notificationManager = NotificationManagerCompat.from(context);

        PendingIntent stopDownload = WorkManager.getInstance(context).createCancelPendingIntent(getId());

        notificationBuilder = new NotificationCompat.Builder(context, String.valueOf(NOTIFICATION_ID))
                .setSilent(true)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setGroup(selectedMangaID)
                .setProgress(0, 0, true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .addAction(R.drawable.icon_stop, context.getString(R.string.stopDownload), stopDownload);

        // Display notification as soon as possible (call needed for Android >= 12)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);

        hasNotificationPermissions = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        progressData = new Data.Builder();
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
        return success ? Result.success(progressData.build()) : Result.failure(progressData.build());
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

        progressData
                .putString("id", selectedChapterID)
                .putFloat("progress", -1)
                .putInt("state", PROGRESS_ONGOING);

        setProgressAsync(progressData.build());

        SendNotificationCompat();

        client.HttpRequestAsync("https://api.mangadex.org/at-home/server/" + selectedChapterID, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (failedDownloads <= MAX_FAILED_ATTEMPTS) {
                    call.clone().enqueue(this);
                    Log.w("Richiesta non riuscita", "Tentativi effettuati: " + failedDownloads + "/5");
                    failedDownloads++;
                } else {
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
                AtHomeResults hResults = StaticData.getMapper().readValue(Objects.requireNonNull(response.body()).string(), AtHomeResults.class);
                response.close();

                // Clean temporary directory
                File dir = new File(context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/");
                FolderUtilities.DeleteFolderContents(dir);

                String baseUrl;
                String[] images;

                if (HQ) {
                    baseUrl = hResults.getBaseUrl() + "/data/";
                    images = hResults.getChapter().getData();
                } else {
                    baseUrl = hResults.getBaseUrl() + "/data-saver/";
                    images = hResults.getChapter().getDataSaver();
                }

                // Create notification
                notificationBuilder
                        .setContentText("0%")
                        .setProgress(images.length, 0, false);

                SendNotificationCompat();

                progressData.putFloat("progress", 0);
                setProgressAsync(progressData.build());

                lastNotificationMillis = System.currentTimeMillis();

                String[] urls = new String[images.length];
                for (int i = 0; i < urls.length; i++)
                    urls[i] = baseUrl + hResults.getChapter().getHash() + "/" + images[i];

                downloader = new MultipleFileDownloader(client, urls,
                        new File(context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/"));
                downloader.setOnDownloadCompleted(() -> OnDownloadEnd());
                downloader.setOnDownloadFailCallback(() -> OnDownloadFail());
                downloader.setOnProgressUpdate((progress, total) -> {
                    progressData.putFloat("progress", Math.round((float) (progress) / total * 100));
                    setProgressAsync(progressData.build());

                    // Manually cap the notification update limit to avoid Android's own rate limits
                    if (System.currentTimeMillis() - lastNotificationMillis < 1000)
                        return;

                    lastNotificationMillis = System.currentTimeMillis();

                    // Update notification
                    notificationBuilder
                            .setContentText(Math.round((float) (progress) / total * 100) + "%")
                            .setProgress(total, progress, false);

                    SendNotificationCompat();
                });
                downloader.start();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void OnDownloadEnd() {
        notificationManager.cancel(uniqueID);

        // Create download folder
        final boolean HQ = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dataSaver", false);
        File targetFolder = new File(context.getExternalFilesDir(null) + "/Manga/" + selectedMangaID + "/" + selectedChapterID + "/");
        File tempFolder = new File(context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/");

        String[] files = tempFolder.list();
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
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

        Intent intent = new Intent(context, ActivityOfflineReader.class);
        intent.setAction("com.michelelorusso.mangadexdownloader.OPEN_DOWNLOAD");
        intent.putExtra("baseUrl", targetFolder.getAbsolutePath());
        intent.putExtra("urls", images);

        @SuppressLint("InlinedApi")
        PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.downloadCompleted))
                .setContentText(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .clearActions()
                .setContentIntent(pendingIntent);

        if (hasNotificationPermissions)
            notificationManager.notify(uniqueID + 1, notificationBuilder.build());

        // Update is sent through WorkResult
        progressData
                .putFloat("progress", 100)
                .putInt("state", PROGRESS_SUCCESS);

        client.shutdown();
        DeleteTempFolder();
        success = true;

        DbMangaDAO dao = MangaDatabase.GetDatabase(getApplicationContext()).MangaDAO();

        final int length = images.length;
        final long folderSize = FolderUtilities.SizeOfFolder(targetFolder);

        MangaDatabase.databaseWriteExecutor.execute(() ->
                dao.InsertMangas(new DbManga(selectedMangaArtist, selectedMangaAuthor, selectedMangaID, selectedMangaName)));

        MangaDatabase.databaseWriteExecutor.execute(() ->
                dao.InsertChapters(new DbChapter(
                        selectedChapterID,
                        selectedMangaID,
                        formattedTitle,
                        selectedChapterScanGroup,
                        length,
                        folderSize,
                        HQ,
                        volume,
                        chapter
                )));

        mutex.release();
    }

    @SuppressLint({"MissingPermission", "InlinedApi"})
    private void OnDownloadFail() {
        // All of the cleanup is already taken care of in the overrided onStopped
        if (interrupted)
            return;

        interrupted = true;

        client.CancelAllPendingRequests();

        notificationManager.cancel(uniqueID);

        Intent i = new Intent(context, BroadcastReceiverDownloadRetry.class);
        i.setAction("com.michelelorusso.mangadexdownloader.RETRY_DOWNLOAD");
        i.putExtra("workID", selectedChapterID);
        i.putExtra("Chapter", selectedChapterID);
        i.putExtra("Manga", selectedMangaName);
        i.putExtra("Title", formattedTitle);

        PendingIntent retryDownload = PendingIntent.getBroadcast(context, uniqueID, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.downloadFailed))
                .setContentText(context.getString(R.string.downloadMoreInfo))
                .setSubText(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .clearActions()
                .addAction(R.drawable.icon_download, context.getString(R.string.downloadRetry), retryDownload);

        if (hasNotificationPermissions)
            notificationManager.notify(uniqueID + 1, notificationBuilder.build());

        progressData.putInt("state", PROGRESS_FAILURE);

        client.shutdown();
        DeleteTempFolder();
        mutex.release();
    }

    private void DeleteTempFolder() {
        File tempFolder = new File(context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/");
        FolderUtilities.DeleteFolder(tempFolder);
    }

    private void SendNotificationCompat() {
        if (hasExceededQuota && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            notificationManager.notify(uniqueID, notificationBuilder.build());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setForegroundAsync(new ForegroundInfo(uniqueID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC));
            }
            else {
                setForegroundAsync(new ForegroundInfo(uniqueID, notificationBuilder.build()));
            }
        } catch (IllegalStateException e) {
            hasExceededQuota = true;
        }
    }
}