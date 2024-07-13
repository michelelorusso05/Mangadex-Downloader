package com.littleProgrammers.mangadexdownloader.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelelorusso.dnsclient.DNSClient;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonPlugin;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;

public class StaticData {
    private static DNSClient sharedClient;
    private static ObjectMapper sharedMapper;
    private static Markwon sharedMarkwon;

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
    public static Markwon getMarkwon(Context context) {
        if (sharedMarkwon == null) {
            int p = CompatUtils.convertDpToPixel(1, context);

            sharedMarkwon = Markwon.builder(context)
                    .usePlugin(new AbstractMarkwonPlugin() {
                        @Override
                        public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                            builder
                                    .thematicBreakHeight(p);
                        }
                    })
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TablePlugin.create(context))
                    .build();
        }
        return sharedMarkwon;
    }
}
