package com.littleProgrammers.mangadexdownloader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

public class ConfirmExitDialog extends AppCompatDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.openDownloadFolder))
                .setMessage(getString(R.string.downloadedFilesDialog))
                .setNegativeButton(R.string.DialogExitCancel, (dialog, which) -> dismiss())
                .setPositiveButton(R.string.DialogExitConfirm, (dialogInterface, i) -> {
                    dismiss();
                    requireActivity().finish();
                });
        return builder.create();
    }
}
