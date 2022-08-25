package com.littleProgrammers.mangadexdownloader;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    String extraParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        extraParameters = getIntent().hasExtra("extraParams") ? getIntent().getStringExtra("extraParams") : "";
        SettingsFragment fragment = new SettingsFragment();

        Bundle args = new Bundle();
        args.putString("params", extraParameters);
        fragment.setArguments(args);
        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, fragment)
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        String extraParams;
        public SettingsFragment() {
            super();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            extraParams = getArguments() != null ? getArguments().getString("params", "") : "";
            if (extraParams.equals("cat"))
                addPreferencesFromResource(R.xml.extra_preferences);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals("info")) {
                if (extraParams.equals("special")) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.aboutRealTitle)
                            .setMessage(R.string.aboutRealParagraph)

                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                }
                else {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.about)
                            .setMessage(R.string.aboutParagraph)

                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                }

                return true;
            }
            return false;
        }
    }
}