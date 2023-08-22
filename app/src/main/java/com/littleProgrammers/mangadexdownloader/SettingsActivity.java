package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashSet;
import java.util.Set;

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

            Context ctx = getContext();
            assert ctx != null;

            Set<String> l = PreferenceManager.getDefaultSharedPreferences(ctx).getStringSet("languagePreference", new HashSet<>());

            updateDuplicateChapterState(l.size() > 1);

            MultiSelectListPreference lPref = findPreference("languagePreference");

            if (lPref != null) {
                lPref.setOnPreferenceChangeListener((preference, value) -> {
                    Set<?> selectedLanguages = (Set<?>) value;
                    if (selectedLanguages.isEmpty())
                        return false;

                    updateDuplicateChapterState(selectedLanguages.size() > 1);
                    return true;
                });
            }

            MultiSelectListPreference rPref = findPreference("contentFilter");

            if (rPref != null) {
                rPref.setOnPreferenceChangeListener((preference, value) -> {
                    Set<?> ratings = (Set<?>) value;
                    return !ratings.isEmpty();
                });
            }

            if (getArguments().getString("highlightSetting", "skip").equals("lang")) {
                scrollToPreference("languagePreference");
            }
        }

        private void updateDuplicateChapterState(boolean disabled)
        {
            SwitchPreferenceCompat duplicateChapters = findPreference("chapterDuplicate");

            // Disabled state
            if (duplicateChapters == null) duplicateChapters = findPreference("chapterDuplicateDummy");
            assert duplicateChapters != null;

            if (disabled)
            {
                duplicateChapters.setKey("chapterDuplicateDummy");
                duplicateChapters.setChecked(true);
                duplicateChapters.setEnabled(false);
                duplicateChapters.setSummary(R.string.settingsDuplicateChaptersDisabled);
            }
            else
            {
                Context ctx = getContext();
                assert ctx != null;

                duplicateChapters.setChecked(PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("chapterDuplicate", true));
                duplicateChapters.setKey("chapterDuplicate");
                duplicateChapters.setEnabled(true);
                duplicateChapters.setSummary(R.string.settingsChapterShowSameChapterDes);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals("info")) {
                if (extraParams.equals("special")) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.aboutRealTitle)
                            .setMessage(R.string.aboutRealParagraph)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .create()
                            .show();
                }
                else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.about)
                            .setMessage(R.string.aboutParagraph)

                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .create()
                            .show();
                }

                return true;
            }
            return false;
        }
    }
}