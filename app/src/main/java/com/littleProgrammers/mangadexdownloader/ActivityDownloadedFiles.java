package com.littleProgrammers.mangadexdownloader;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.littleProgrammers.mangadexdownloader.db.DbMangaDAO;
import com.littleProgrammers.mangadexdownloader.db.MangaDatabase;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.FolderUtilities;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ActivityDownloadedFiles extends AppCompatActivity implements ActionMode.Callback {
    RecyclerView recyclerView;
    AdapterRecyclerDownloadedMangas adapter;
    View emptyView;
    TextView emoticonView;
    Toolbar t;
    private SelectionTracker<Long> selectionTracker;
    private ActionMode actionMode;
    DbMangaDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded_files);

        t = findViewById(R.id.home_toolbar);
        setSupportActionBar(t);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.downloadedMangas);
        emptyView = findViewById(R.id.emptyView);
        emoticonView = findViewById(R.id.emoticonView);

        adapter = new AdapterRecyclerDownloadedMangas(ActivityDownloadedFiles.this);
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(ActivityDownloadedFiles.this));

        ViewCompat.setOnApplyWindowInsetsListener(t, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) t.getLayoutParams();
            params.setMargins(i.left, 0, i.right, 0);

            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                recyclerView.setPadding(i.left,
                        recyclerView.getPaddingTop(),
                        i.right,
                        i.bottom + CompatUtils.ConvertDpToPixel(16, ActivityDownloadedFiles.this));

                return insets;
            }
        });

        dao = MangaDatabase.GetDatabase(this).MangaDAO();

        dao.LoadMangasWithAttributes().observe(this, mangas -> adapter.AddMangas(mangas));

        dao.GetOrderedChapters().observe(this, mangaChapterSchemas -> {
            DbMangaDAO.MangaChapterSchema lastAdded = null;
            for (int i = 0; i < mangaChapterSchemas.size(); i++) {
                if (i == 0 || !mangaChapterSchemas.get(i).manga_id.contentEquals(mangaChapterSchemas.get(i - 1).manga_id)) {
                    lastAdded = new DbMangaDAO.MangaChapterSchema();
                    lastAdded.manga_id = mangaChapterSchemas.get(i).manga_id;

                    mangaChapterSchemas.add(i, lastAdded);
                }
                else {
                    lastAdded.num_pages++;
                    lastAdded.size += mangaChapterSchemas.get(i).size;
                }
            }

            adapter.AddChapters(mangaChapterSchemas);
        });

        selectionTracker =
                new SelectionTracker.Builder<>(
                        "delete_selection",
                        recyclerView,
                        new AdapterRecyclerDownloadedMangas.KeyProvider(),
                        new AdapterRecyclerDownloadedMangas.DetailsLookup(recyclerView),
                        StorageStrategy.createLongStorage())
                        .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                        .build();

        adapter.SetSelectionTracker(selectionTracker);

        selectionTracker.addObserver(
                new SelectionTracker.SelectionObserver<Long>() {
                    @Override
                    public void onSelectionChanged() {
                        if (!selectionTracker.getSelection().isEmpty()) {
                            if (actionMode == null) {
                                actionMode = startSupportActionMode(ActivityDownloadedFiles.this);
                            }
                            assert actionMode != null;
                            actionMode.setTitle(String.valueOf(selectionTracker.getSelection().size()));
                        } else if (actionMode != null) {
                            actionMode.finish();
                        }
                    }
                });


        adapter.AddDifferListener((previousList, currentList) -> {
            if (currentList.isEmpty()) {
                emoticonView.setText(CompatUtils.GetRandomStringFromStringArray(this, R.array.emoticons_confused));
                emptyView.setVisibility(View.VISIBLE);
            }
            else
                emptyView.setVisibility(View.GONE);
        });
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {

        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.downloaded_files_action_toolbar, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_downloadedfiles_delete) {
            new MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.icon_delete)
                    .setTitle("Cancella")
                    .setMessage("Vuoi cancellare gli elementi selezionati?")
                    .setPositiveButton("Cancella", (v, c) -> {
                        Selection<Long> selection = selectionTracker.getSelection();

                        List<DbMangaDAO.MangaChapterSchema> list = adapter.GetList();
                        HashMap<String, DbMangaDAO.MangaChapterSizeAndNumber> map = adapter.GetMangaMap();

                        Iterator<Long> iterator = selection.iterator();

                        while (iterator.hasNext()) {
                            Long index = iterator.next();

                            DbMangaDAO.MangaChapterSchema s = list.get(index.intValue());
                            DbMangaDAO.MangaChapterSizeAndNumber m = map.get(s.manga_id);
                            assert m != null;

                            if (s.id == null) {
                                DeleteManga(s.manga_id);

                                if (adapter.IsExpanded(s.manga_id)) {
                                    for (int i = 0; i < m.number; i++) {
                                        iterator.next();
                                        iterator.remove();
                                    }
                                }
                            }
                            else {
                                DeleteChapter(s.id, s.manga_id);
                            }
                        }
                        actionMode.finish();
                    })
                    .setNegativeButton(android.R.string.cancel, (v, c) -> v.cancel())
                    .show();

        }

        return false;
    }

    private void DeleteChapter(String id, String mangaId) {
        MangaDatabase.databaseWriteExecutor.execute(() ->
                dao.DeleteChapterFromId(id));

        File targetToDelete = new File(getExternalFilesDir(null) + "/Manga/" + mangaId + "/" + id + "/");
        FolderUtilities.DeleteFolder(targetToDelete);
    }

    private void DeleteManga(String mangaId) {
        MangaDatabase.databaseWriteExecutor.execute(() ->
                dao.DeleteMangaAndChapters(mangaId));

        File targetToDelete = new File(getExternalFilesDir(null) + "/Manga/" + mangaId + "/");
        FolderUtilities.DeleteFolder(targetToDelete);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        selectionTracker.clearSelection();
        this.actionMode = null;
    }
}