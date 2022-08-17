package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class DownloadedFiles extends AppCompatActivity {
    RecyclerView downloadedMangas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded_files);

        downloadedMangas = findViewById(R.id.downloadedMangas);
        File path = new File(getExternalFilesDir(null) + "/Manga/");
        File[] mangas = GetFilesInPath(path);
        DownloadedMangaAdapter adapter = new DownloadedMangaAdapter(DownloadedFiles.this, mangas);
        adapter.setHasStableIds(true);
        downloadedMangas.setAdapter(adapter);
        downloadedMangas.setLayoutManager(new LinearLayoutManager(DownloadedFiles.this));
    }

    private File[] GetFilesInPath(File dirPath)
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