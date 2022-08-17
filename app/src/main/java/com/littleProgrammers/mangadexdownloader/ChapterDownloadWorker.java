package com.littleProgrammers.mangadexdownloader;


import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.littleProgrammers.mangadexdownloader.apiResults.AtHomeResults;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Semaphore;

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

    private int totalPages;
    private int pagesLeft;

    private final int uniqueID;
    private final Semaphore mutex;
    private boolean success = false;
    private boolean interrupted = false;

    @Override
    public void onStopped() {
        super.onStopped();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(uniqueID);

        success = false;
        interrupted = true;
        client.CancelAllPendingRequests();
        mutex.release();

        DeleteTempFolder();
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

        client = new DNSClient(DNSClient.PresetDNS.GOOGLE);

        assert selectedChapterID != null;
        uniqueID = selectedChapterID.hashCode() + new Random().nextInt();
        mutex = new Semaphore(0);
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
        i.setAction("com.michelelorusso.mangadexdownloader.STOP_DOWNLOAD_".concat(selectedChapterID));
        i.putExtra("workID", selectedChapterID);

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent stopDownload = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create notification
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), String.valueOf(NOTIFICATION_ID))
                .setSilent(true)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(pagesLeft, 0, true)
                .addAction(R.drawable.ic_baseline_stop_24, context.getString(R.string.stopDownload), stopDownload);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(uniqueID, builder.build());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        client.AsyncHttpRequest("https://api.mangadex.org/at-home/server/" + selectedChapterID, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mutex.release();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (interrupted) {
                    response.close();
                    return;
                }

                AtHomeResults hResults = mapper.readValue(Objects.requireNonNull(response.body()).string(), AtHomeResults.class);

                // Clean temporary directory
                File dir = new File(context.getExternalFilesDir(null) +"/mangadexDownloaderTemp/" + selectedChapterID + "/");
                if (dir.isDirectory()) {
                    String[] children = dir.list();
                    assert children != null;
                    for (String child : children) {
                        if (!new File(dir, child).delete())
                            Log.w("Filesystem problem", "Unable to delete ".concat(child));
                    }
                }

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

                totalPages = pagesLeft = images.length;

                // Create notification
                builder
                        .setContentText("0%")
                        .setProgress(pagesLeft, 0, false);
                notificationManager.notify(uniqueID, builder.build());

                int lengthOfNumberOfPages = (int) (Math.floor(Math.log10(totalPages)) + 1);

                for (int i = 0; i < totalPages; i++) {
                    double lengthOfI = Math.floor(Math.log10(i + 1)) + 1;
                    String fileExtension = images[i].substring(images[i].lastIndexOf("."));
                    String url = baseUrl + hResults.getChapter().getHash() + "/" + images[i];

                    client.DownloadFileAsync(url, new File(context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/"
                            + new String(new char[(int) (lengthOfNumberOfPages - lengthOfI)]).replace("\0", "0") + (i + 1)
                            + fileExtension), new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            OnDownloadFail();
                            mutex.release();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) {
                            if (interrupted) {
                                response.close();
                                return;
                            }
                            pagesLeft--;
                            // Update notification
                            builder
                                    .setContentText(Math.round((float) (totalPages - pagesLeft) / totalPages * 100) + "%")
                                    .setProgress(totalPages, totalPages - pagesLeft, false);
                            notificationManager.notify(uniqueID, builder.build());

                            if (pagesLeft == 0) {
                                OnDownloadEnd();
                                success = true;
                                mutex.release();
                            }
                        }
                    });
                }
                response.close();
            }
        });
    }
    // Returns false if IO operations failed
    private void OnDownloadEnd() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(uniqueID);

        // Create download folder
        boolean HQ = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dataSaver", false);
        File targetFolder = new File(context.getExternalFilesDir(null) + "/Manga/" + selectedMangaName + "/" + formattedTitle + (HQ ? ".hq" : ".lq") + "/");
        File tempFolder = new File(context.getExternalFilesDir(null) +"/mangadexDownloaderTemp/" + selectedChapterID + "/");
        String[] files = tempFolder.list();
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            Log.d("Error", "Unable to create folder");
            OnDownloadFail();
            return;
        }
        try {
            if (targetFolder.exists() && tempFolder.exists() && files != null) {
                for (String file : files) {
                    FolderUtilities.CopyFile(new File(tempFolder, file), new File(targetFolder, file));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            OnDownloadFail();
            return;
        }

        String[] images = targetFolder.list();
        images = (images != null ? images : new String[0]);
        Arrays.sort(images);

        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra("baseUrl", targetFolder.getAbsolutePath());
        intent.putExtra("urls", images);
        intent.putExtra("sourceIsStorage", true);

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builderCompleted = new NotificationCompat.Builder(getApplicationContext(), String.valueOf(NOTIFICATION_ID))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.downloadCompleted))
                .setContentText(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent);
        notificationManager.notify(uniqueID, builderCompleted.build());

        DeleteTempFolder();
    }

    private void OnDownloadFail() {
        // ALl of the cleanup is already taken care of in the overrided onStopped
        if (interrupted) return;

        client.CancelAllPendingRequests();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        Context context = getApplicationContext();
        notificationManager.cancel(uniqueID);

        NotificationCompat.Builder builderCompleted = new NotificationCompat.Builder(getApplicationContext(), String.valueOf(NOTIFICATION_ID))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.downloadFailed))
                .setContentText(formattedTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        notificationManager.notify(uniqueID, builderCompleted.build());

        DeleteTempFolder();
    }

    private void DeleteTempFolder() {
        File tempFolder = new File( context.getExternalFilesDir(null) + "/mangadexDownloaderTemp/" + selectedChapterID + "/");
        FolderUtilities.DeleteFolder(tempFolder);
    }
}

