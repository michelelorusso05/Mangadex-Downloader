package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.widget.ArrayAdapter;

import java.util.List;

public class AdapterSpinnerIndexes extends ArrayAdapter<String> {
    private final List<String> indexes;

    @Override
    public long getItemId(int position) {
        return indexes.get(position).hashCode();
    }

    public AdapterSpinnerIndexes(Activity context, List<String> elements) {
        super(context, R.layout.item_spinner_page_indicator, elements);

        indexes = elements;
    }
}
