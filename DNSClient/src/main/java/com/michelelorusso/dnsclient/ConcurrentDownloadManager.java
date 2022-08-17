package com.michelelorusso.dnsclient;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ConcurrentDownloadManager {
    DNSClient client;
    String[] urls;
    int current;
    File destinationDir;
    boolean isDownloading;
    OnDownloadEnd finalOp;


    public ConcurrentDownloadManager(String[] _urls, File _destination) {
        client = new DNSClient(DNSClient.PresetDNS.GOOGLE);
        urls = _urls;
        destinationDir = _destination;
        current = 0;
    }

    public void StartDownload(OnDownloadEnd onEnd) {
        finalOp = onEnd;
        isDownloading = true;
        NextCall(current);
        current++;
        NextCall(current);
    }

    private void NextCall(int pos) {
        if (current >= urls.length) {
            return;
        }

        int lengthOfTotalNumber = (int) (Math.floor(Math.log10(urls.length)) + 1);
        int lengthOfCurrentNumber = (int) (Math.floor(Math.log10(pos + 1)) + 1);
        File dest = new File(destinationDir,
                // Zero fills
                new String(new char[(int) (lengthOfTotalNumber - lengthOfCurrentNumber)]).replace("\0", "0")
                // Current number
                        + (pos + 1)
                // Extension
                        + urls[pos].substring(urls[pos].lastIndexOf(".")));

        client.DownloadFileAsync(urls[pos], dest, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                NextCall(pos);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Log.d("progress", current + "/" + urls.length);
                current++;
                if (current >= urls.length) {
                    isDownloading = false;
                    finalOp.Execute();
                }
                else
                    NextCall(current);
            }
        });
    }

    public interface OnDownloadEnd {
        void Execute();
    }
}
