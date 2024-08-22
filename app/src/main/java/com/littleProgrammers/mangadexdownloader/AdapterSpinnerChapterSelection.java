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

public class AdapterSpinnerChapterSelection extends ArrayAdapter<String> {
    ArrayList<String> titles;
    ArrayList<String> scanlationGroups;

    public AdapterSpinnerChapterSelection(@NonNull Context context, ArrayList<String> t, ArrayList<String> s) {
        super(context, R.layout.item_spinner_chapter, R.id.chapterTitle, t);

        titles = t;
        scanlationGroups = s;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View currentItemView = convertView;

        if (currentItemView == null) {
            currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.item_spinner_chapter, parent, false);
        }

        TextView name = currentItemView.findViewById(R.id.chapterTitle);
        name.setText(titles.get(position));

        TextView group = currentItemView.findViewById(R.id.scanlationGroup);

        if (scanlationGroups != null)
            group.setText(scanlationGroups.get(position));
        else
            group.setVisibility(View.GONE);

        return currentItemView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
