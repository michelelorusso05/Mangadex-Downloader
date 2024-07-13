package com.littleProgrammers.mangadexdownloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BroadcastReceiverDownloadRetry extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(intent.getIntExtra("NotificationToCancel", 0));

        String chapterID = intent.getStringExtra("Chapter");
        String mangaName = intent.getStringExtra("Manga");
        String selectedChapterTitle = intent.getStringExtra("Title");

        WorkManager wm = WorkManager.getInstance(context);

        Data.Builder data = new Data.Builder();
        data.putString("Chapter", chapterID);
        data.putString("Manga", mangaName);
        data.putString("Title", selectedChapterTitle);

        assert chapterID != null;
        OneTimeWorkRequest downloadWorkRequest = new OneTimeWorkRequest.Builder(WorkerChapterDownload.class)
                .addTag(chapterID)
                .setInputData(data.build())
                .build();

        wm.enqueueUniqueWork(chapterID, ExistingWorkPolicy.KEEP, downloadWorkRequest);
    }
}
