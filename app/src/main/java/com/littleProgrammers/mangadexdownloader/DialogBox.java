package com.littleProgrammers.mangadexdownloader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

public class DialogBox extends AppCompatDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.noPagesDialogTitle))
                .setMessage(getString(R.string.noPagesDialog))
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                });
        return builder.create();
    }
}
