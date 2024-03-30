package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.graphics.Bitmap;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelelorusso.dnsclient.DNSClient;

import java.io.IOException;

public class StaticData {
    public static Bitmap sharedCover;
    private static DNSClient sharedClient;
    private static ObjectMapper sharedMapper;

    public static DNSClient getClient(Context context) {
        if (sharedClient == null) {
            sharedClient = new DNSClient(DNSClient.PresetDNS.CLOUDFLARE, context, true);
        }
        return sharedClient;
    }
    public static ObjectMapper getMapper() {
        if (sharedMapper == null) {
            sharedMapper = new ObjectMapper();
            sharedMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            sharedMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
            sharedMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
            sharedMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        }
        return sharedMapper;
    }
}
