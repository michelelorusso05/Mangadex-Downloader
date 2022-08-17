package com.littleProgrammers.mangadexdownloader;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class FolderUtilities {
    public static void CopyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
    public static void DeleteFolder(File f) {
        if (f.isDirectory()) {
            for (String child : Objects.requireNonNull(f.list())) {
                File cur = new File(f, child);
                DeleteFolder(cur);
            }
        }
        if (!f.delete())
            Log.d("Error", "Unable to delete ".concat(f.getName()));
    }
    public static long SizeOfFolder(File f) {
        long s = 0;
        if (f.isDirectory()) {
            for (String child : Objects.requireNonNull(f.list())) {
                File cur = new File(f, child);
                s += SizeOfFolder(cur);
            }
        }
        else {
            return f.length();
        }
        return s;
    }
    @NonNull
    public static File[] GetOrderedFilesInPath(@NonNull File dirPath) {
        File[] files = dirPath.listFiles();
        if (files == null || files.length == 0)
            return new File[0];

        Arrays.sort(files);
        return files;
    }
}
