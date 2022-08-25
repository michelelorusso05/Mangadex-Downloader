package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
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
        this(context, ChapterToStringPairArray(names));
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

        // convertView which is recyclable view
        View currentItemView = convertView;

        // of the recyclable view is null then inflate the custom layout for the same
        if (currentItemView == null) {
            currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.chapter_spinner_item, parent, false);
        }

        // get the position of the view from the ArrayAdapter
        Pair<String, String> currentObjectPosition = getItem(position);

        // then according to the position of the view assign the desired TextView 1 for the same
        TextView textView1 = currentItemView.findViewById(R.id.chapterTitle);
        textView1.setText(currentObjectPosition.first);

        // then according to the position of the view assign the desired TextView 2 for the same
        TextView textView2 = currentItemView.findViewById(R.id.scanlationGroup);
        if (currentObjectPosition.second != null && !ignoreSecond)
            textView2.setText(currentObjectPosition.second);
        else
            textView2.setVisibility(View.GONE);

        // then return the recyclable view
        return currentItemView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.chapter_spinner_item, parent, false);
        }
        Pair<String, String> rowItem = getItem(position);
        TextView txtTitle = convertView.findViewById(R.id.chapterTitle);
        txtTitle.setText(rowItem.first);
        TextView groupView = convertView.findViewById(R.id.scanlationGroup);
        groupView.setText(rowItem.second);
        return convertView;
    }
}
