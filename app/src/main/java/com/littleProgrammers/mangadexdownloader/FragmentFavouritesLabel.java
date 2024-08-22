package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.BaseProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;

import java.util.Set;

public class FragmentFavouritesLabel extends Fragment {
    public static final String TAG = FragmentFavouritesLabel.class.getSimpleName();
    Context context;
    TextView label;
    MaterialButton icon;
    CircularProgressIndicator progressBar;

    public FragmentFavouritesLabel() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireContext();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (label != null)
            UpdateLabel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favs_label, container, false);

        label = view.findViewById(R.id.label);
        progressBar = view.findViewById(R.id.progressBar);
        icon = view.findViewById(R.id.icon);

        // progressBar.setHideAnimationBehavior(BaseProgressIndicator.HIDE_OUTWARD);
        progressBar.setShowAnimationBehavior(BaseProgressIndicator.SHOW_OUTWARD);

        getParentFragmentManager().setFragmentResultListener("searchStart", this, (requestKey, result) -> {
            progressBar.setVisibility(View.VISIBLE);
            icon.setEnabled(false);
        });

        getParentFragmentManager().setFragmentResultListener("searchEnd", this, (requestKey, result) -> {
            progressBar.hide();
            icon.setEnabled(true);
        });

        UpdateLabel();

        return view;
    }

    private void UpdateLabel() {
        Set<String> fav = FavouriteManager.GetFavouritesIDs(context);
        label.setText(getString(R.string.favs_label, fav.size()));
    }
}
