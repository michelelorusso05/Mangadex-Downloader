package com.littleProgrammers.mangadexdownloader.utils;

import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.core.util.Consumer;

public class BetterSpinner {
    public final Spinner spinner;
    private boolean userInitiatedAction;

    public BetterSpinner(Spinner impl) {
        spinner = impl;

        spinner.setOnTouchListener((v, event) -> {
            userInitiatedAction = true;
            v.performClick();
            return false;
        });
    }

    public void setOnItemSelectedListener(Consumer<Integer> action) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userInitiatedAction) {
                    action.accept(position);
                    userInitiatedAction = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    public void setSelection(int position) {
        userInitiatedAction = true;
        spinner.setSelection(position);
    }
    public int getCount() {
        return spinner.getCount();
    }
    public void setEnabled(boolean e) {
        spinner.setEnabled(e);
    }
    public int getSelectedItemPosition() {
        return spinner.getSelectedItemPosition();
    }
    public Object getSelectedItem() {
        return spinner.getSelectedItem();
    }
    public SpinnerAdapter getAdapter() {
        return spinner.getAdapter();
    }
    public void setAdapter(SpinnerAdapter spinnerAdapter) {
        spinner.setAdapter(spinnerAdapter);
    }
}
