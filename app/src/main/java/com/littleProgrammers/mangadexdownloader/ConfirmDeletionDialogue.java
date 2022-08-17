package com.littleProgrammers.mangadexdownloader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

public class ConfirmDeletionDialogue extends AppCompatDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.deleteChapter))
                .setMessage(getString(R.string.deleteChapterDescription))
                .setNegativeButton(R.string.DialogExitCancel, (dialog, which) -> dismiss())
                .setPositiveButton(R.string.genericConfirm, (dialogInterface, i) -> {
                    dismiss();
                    requireActivity().finish();
                });
        return builder.create();
    }
}
