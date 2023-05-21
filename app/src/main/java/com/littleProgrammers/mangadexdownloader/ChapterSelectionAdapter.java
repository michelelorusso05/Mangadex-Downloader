package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;

import java.util.ArrayList;
import java.util.Collections;

public class ChapterSelectionAdapter extends ArrayAdapter<Pair<String, String>> {
    boolean ignoreSecond;
    public ChapterSelectionAdapter(@NonNull Context context, ArrayList<Pair<String, String>> data, boolean ignoreSecond) {
        super(context, R.layout.chapter_spinner_item, R.id.chapterTitle, data);
        this.ignoreSecond = ignoreSecond;
    }
    public ChapterSelectionAdapter(@NonNull Context context, ArrayList<Pair<String, String>> data) {
        this(context, data, false);
    }
    public ChapterSelectionAdapter(@NonNull Context context, Pair<String, String> singleton) {
        this(context, new ArrayList<>(Collections.singletonList(singleton)));
    }
    public ChapterSelectionAdapter(@NonNull Context context, Chapter[] data) {
        this(context, ChapterToStringPairArray(data));
    }
    public ChapterSelectionAdapter(@NonNull Context context, String[] names) {
        this(context, ChapterToStringPairArray(names), true);
    }

    @NonNull
    private static ArrayList<Pair<String, String>> ChapterToStringPairArray(@NonNull Chapter[] data) {
        ArrayList<Pair<String, String>> returnData = new ArrayList<>();
        for (Chapter c : data)
            returnData.add(new Pair<>(c.getAttributes().getFormattedName(), c.getAttributes().getScanlationGroupString()));

        return returnData;
    }
    @NonNull
    private static ArrayList<Pair<String, String>> ChapterToStringPairArray(@NonNull String[] data) {
        ArrayList<Pair<String, String>> returnData = new ArrayList<>();
        for (String c : data)
            returnData.add(new Pair<>(c, null));

        return returnData;
    }



    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View currentItemView = convertView;

        if (currentItemView == null) {
            currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.chapter_spinner_item, parent, false);
        }

        Pair<String, String> currentObjectPosition = getItem(position);

        TextView name = currentItemView.findViewById(R.id.chapterTitle);
        name.setText(currentObjectPosition.first);

        TextView group = currentItemView.findViewById(R.id.scanlationGroup);
        if (!ignoreSecond && currentObjectPosition.second != null)
            group.setText(currentObjectPosition.second);
        else
            group.setVisibility(View.GONE);

        return currentItemView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
