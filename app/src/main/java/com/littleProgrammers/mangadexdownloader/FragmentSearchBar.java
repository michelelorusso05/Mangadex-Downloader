package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.BaseProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class FragmentSearchBar extends Fragment {
    public static final String TAG = FragmentSearchBar.class.getSimpleName();
    Context context;
    EditText searchBar;
    MaterialButton searchButton, favoriteButton;
    CircularProgressIndicator progressBar;

    public FragmentSearchBar() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_bar, container, false);

        searchBar = view.findViewById(R.id.searchBar);
        searchButton = view.findViewById(R.id.searchButton);
        favoriteButton = view.findViewById(R.id.favouriteButton);
        progressBar = view.findViewById(R.id.progressBar);

        // progressBar.setHideAnimationBehavior(BaseProgressIndicator.HIDE_OUTWARD);
        progressBar.setShowAnimationBehavior(BaseProgressIndicator.SHOW_OUTWARD);

        searchButton.setOnClickListener((v) -> {
            Bundle bundle = new Bundle();
            bundle.putString("type", "query");
            bundle.putString("query", searchBar.getText().toString().trim());

            getParentFragmentManager().setFragmentResult("search", bundle);

            UpdateUIBeginSearch();
        });

        favoriteButton.setOnClickListener((v) -> {
            Bundle bundle = new Bundle();
            bundle.putString("type", "fav");

            getParentFragmentManager().setFragmentResult("search", bundle);

            UpdateUIBeginSearch();
        });

        getParentFragmentManager().setFragmentResultListener("searchEnd", this, (requestKey, result) -> {
            progressBar.hide();
            searchButton.setEnabled(true);
            favoriteButton.setEnabled(true);
            searchBar.setEnabled(true);
        });

        getParentFragmentManager().setFragmentResultListener("searchStart", this, (requestKey, result) -> {
            UpdateUIBeginSearch();
        });

        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchButton.performClick();
                HideKeyboard(v);
                return true;
            }
            return false;
        });

        return view;
    }

    private void UpdateUIBeginSearch() {
        progressBar.setVisibility(View.VISIBLE);
        searchButton.setEnabled(false);
        favoriteButton.setEnabled(false);
        searchBar.setEnabled(false);
    }

    private void HideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}