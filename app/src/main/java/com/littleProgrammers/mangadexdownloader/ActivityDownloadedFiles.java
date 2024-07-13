package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ActivityDownloadedFiles extends AppCompatActivity {
    RecyclerView downloadedMangas;
    TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded_files);

        Toolbar t = findViewById(R.id.home_toolbar);
        setSupportActionBar(t);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        downloadedMangas = findViewById(R.id.downloadedMangas);
        emptyView = findViewById(R.id.emptyView);
        File path = new File(getExternalFilesDir(null) + "/Manga/");
        File[] mangas = GetFilesInPath(path);

        if (mangas.length == 0) {
            downloadedMangas.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
        else {
            AdapterRecyclerDownloadedMangas adapter = new AdapterRecyclerDownloadedMangas(ActivityDownloadedFiles.this, mangas, downloadedMangas);
            adapter.setHasStableIds(true);
            downloadedMangas.setAdapter(adapter);
            downloadedMangas.setLayoutManager(new LinearLayoutManager(ActivityDownloadedFiles.this));
            // downloadedMangas.setItemAnimator(new AdapterRecyclerDownloadedMangas.MangaItemAnimator());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.downloaded_files_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.action_downloadedfiles_help) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.help)
                    .setMessage(R.string.downloadedChaptersHelp)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private File[] GetFilesInPath(@NonNull File dirPath)
    {
        String[] paths = dirPath.list();
        if (paths == null || paths.length == 0)
            return new File[0];
        Arrays.sort(paths);

        ArrayList<File> dirs = new ArrayList<>();
        for (String path : paths) {
            File dir = new File(dirPath + "/" + path);
            if (dir.isDirectory())
                dirs.add(dir);
        }
        return dirs.toArray(new File[0]);
    }
}