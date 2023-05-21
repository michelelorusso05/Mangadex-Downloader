package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.graphics.Bitmap;

import com.michelelorusso.dnsclient.DNSClient;

public class StaticData {
    public static Bitmap sharedCover;
    private static DNSClient sharedClient;

    public static DNSClient getClient(Context context) {
        if (sharedClient == null)
            sharedClient = new DNSClient(DNSClient.PresetDNS.CLOUDFLARE, context, true);

        return sharedClient;
    }
}
