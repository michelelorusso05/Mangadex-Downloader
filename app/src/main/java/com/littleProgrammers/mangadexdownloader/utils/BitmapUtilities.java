package com.littleProgrammers.mangadexdownloader.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BitmapUtilities {

    public static Bitmap combineBitmaps(@NonNull Bitmap left, @Nullable Bitmap right) {
        return combineBitmaps(left, right, false);
    }
    @NonNull
    public static Bitmap combineBitmaps(@NonNull Bitmap left, @Nullable Bitmap right, boolean inverse) {
        if (inverse) {
            if (right == null) return combineBitmaps(left, null, false);
            return combineBitmaps(right, left, false);
        }

        // If right is null, then only the last (lone) page is left to render
        if (right == null) return left;

        int width;

        if ((float) Math.min(left.getHeight(), right.getHeight()) / Math.max(left.getHeight(), right.getHeight()) < 0.3) {
            if (left.getHeight() < right.getHeight()) {
                left = ResizeBitmap(left, left.getWidth() * right.getHeight() / left.getHeight(), right.getHeight());
            }
            else {
                right = ResizeBitmap(right, right.getWidth() * left.getHeight() / right.getHeight(), left.getHeight());
            }
        }
        else {
            // Normal resize
            if (left.getHeight() > right.getHeight()) {
                left = ResizeBitmap(left, left.getWidth() * right.getHeight() / left.getHeight(), right.getHeight());
            }
            else {
                right = ResizeBitmap(right, right.getWidth() * left.getHeight() / right.getHeight(), left.getHeight());
            }
        }
        width = left.getWidth() + right.getWidth();


        int height = Math.max(left.getHeight(), right.getHeight());

        // Create a Bitmap large enough to hold both input images and a canvas to draw to this
        // combined bitmap.
        Bitmap combined = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(combined);

        // Render both input images into the combined bitmap and return it.
        canvas.drawBitmap(left, 0f, 0f, null);
        canvas.drawBitmap(right, left.getWidth(), 0f, null);

        return combined;
    }
    public static Bitmap ResizeBitmap(Bitmap src, int nWidth, int nHeight) {
        return Bitmap.createScaledBitmap(src, nWidth, nHeight, true);
    }
}
