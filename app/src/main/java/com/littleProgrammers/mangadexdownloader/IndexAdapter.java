package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.widget.ArrayAdapter;

import java.util.List;

public class IndexAdapter extends ArrayAdapter<String> {
    private final List<String> indexes;
    private final Activity ctx;

    @Override
    public long getItemId(int position) {
        return indexes.get(position).hashCode();
    }

    public IndexAdapter(Activity context, List<String> elements) {
        super(context, R.layout.page_indicator_spinner_item, elements);

        indexes = elements;
        ctx = context;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
