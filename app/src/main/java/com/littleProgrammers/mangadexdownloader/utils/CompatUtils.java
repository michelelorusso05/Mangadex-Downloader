package com.littleProgrammers.mangadexdownloader.utils;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;

import androidx.annotation.ArrayRes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class CompatUtils {
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T GetSerializable(Bundle bundle, String name, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return bundle.getSerializable(name, clazz);
        else {
            Object obj = bundle.getSerializable(name);
            return (T) obj;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Parcelable> T GetParcelable(Bundle bundle, String name, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return bundle.getParcelable(name, clazz);
        else {
            Object obj = bundle.getParcelable(name);
            return (T) obj;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Parcelable> ArrayList<T> GetParcelableArrayList(Bundle bundle, String name, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return bundle.getParcelableArrayList(name, clazz);
        else {
            ArrayList<Parcelable> obj = bundle.getParcelableArrayList(name);
            return (ArrayList<T>) obj;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Parcelable> T GetParcelableFromParcel(Parcel parcel, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return parcel.readParcelable(clazz.getClassLoader(), clazz);
        else {
            Object obj = parcel.readParcelable(clazz.getClassLoader());
            return (T) obj;
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int ConvertDpToPixel(float dp, Context context) {
        return Math.round(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static int ConvertPixelsToDp(float px, Context context) {
        return Math.round(px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static String GetRandomStringFromStringArray(Context context, @ArrayRes int resID) {
        String[] array = context.getResources().getStringArray(resID);
        int index = ThreadLocalRandom.current().nextInt(array.length);
        return array[index];
    }
}
