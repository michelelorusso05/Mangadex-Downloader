package com.littleProgrammers.mangadexdownloader;

import android.graphics.Bitmap;

import androidx.core.util.Consumer;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Dispatcher<T> {
    private final int target;
    private int progress;
    private final ArrayList<T> objs;


    private final Consumer<ArrayList<T>> onEndCallback;

    Dispatcher(int target, Consumer<ArrayList<T>> onEnd) {
        this.target = target;
        objs = new ArrayList<T>(target);
        for (int i = 0; i < target; i++)
            objs.add(null);

        onEndCallback = onEnd;
    }

    public synchronized void UpdateProgress(T toInsert, int index) {
        objs.set(index, toInsert);
        progress++;

        if (progress == target)
            onEndCallback.accept(objs);
    }
}
