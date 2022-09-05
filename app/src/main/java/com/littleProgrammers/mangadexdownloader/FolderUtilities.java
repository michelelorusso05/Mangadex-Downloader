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
    public static void DeleteFolderContents(@NonNull File f) {
        if (f.isDirectory()) {
            for (String child : Objects.requireNonNull(f.list())) {
                File cur = new File(f, child);
                DeleteFolder(cur);
            }
        }
    }
    public static void DeleteFolder(@NonNull File f) {
        DeleteFolderContents(f);
        if (!f.delete())
            Log.d("Error", "Unable to delete ".concat(f.getName()));
    }
    public static long SizeOfFolder(@NonNull File f) {
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

        Arrays.sort(files, (o1, o2) -> {
            float n1 = ExtractNumberFromFilename(o1);
            float n2 = ExtractNumberFromFilename(o2);
            return Float.compare(n1, n2);
        });
        return files;
    }

    public static float ExtractNumberFromFilename(@NonNull File f) {
        float n;
        try {
            String s = f.getName().substring(0, f.getName().indexOf(" "));
            n = Float.parseFloat(s);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            n = -1;
        }
        return n;
    }
}
