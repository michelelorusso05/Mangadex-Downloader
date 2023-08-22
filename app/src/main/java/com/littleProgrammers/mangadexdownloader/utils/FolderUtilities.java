package com.littleProgrammers.mangadexdownloader.utils;

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
    public static void MoveFile(File src, File dst) {
        if (!src.renameTo(dst))
            Log.w("Unable to rename file", "src: " + src + " dst: " + dst);
    }
    public static void DeleteFolderContents(@NonNull File f) {
        if (f.isDirectory()) {
            for (String child : Objects.requireNonNull(f.list())) {
                File cur = new File(f, child);
                EraseFile(cur);
            }
        }
    }
    public static void DeleteFolder(@NonNull File f) {
        DeleteFolderContents(f);
        if (!f.delete())
            Log.d("Error", "Unable to delete ".concat(f.getPath()));
    }
    public static void EraseFile(@NonNull File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            assert children != null;
            for (File subFile : children) {
                EraseFile(subFile);
            }
        }
        if (!f.delete())
            Log.w("Deletion error", "Unable to delete " + f);
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
            int endIndex = f.getName().indexOf(" ");
            if (endIndex == -1) endIndex = f.getName().lastIndexOf('.');
            String s = f.getName().substring(0, endIndex);
            n = Float.parseFloat(s);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
            n = -1;
        }
        return n;
    }
}
