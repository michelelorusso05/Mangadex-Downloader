package com.littleProgrammers.mangadexdownloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.WorkManager;

public class DownloadStopper extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null) return;
        String workID = intent.getStringExtra("workID");
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelUniqueWork(workID);
    }
}
