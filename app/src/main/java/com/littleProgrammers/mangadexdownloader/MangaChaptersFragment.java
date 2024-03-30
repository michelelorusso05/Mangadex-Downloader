package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.michelelorusso.dnsclient.DNSClient;

import java.util.ArrayList;


public class MangaChaptersFragment extends Fragment {

    DNSClient client;
    Activity context;
    private String mangaID;
    private String mangaTitle;
    private RecyclerView recyclerView;
    private TextView emoticonView;
    private TextView errorView;
    private View progressBar;
    private boolean chaptersAvailableHint;
    ChapterListViewModel model;

    public MangaChaptersFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();
        client = StaticData.getClient(context);

        if (savedInstanceState == null)
            savedInstanceState = getArguments();

        assert savedInstanceState != null;

        mangaID = savedInstanceState.getString("MangaID");
        mangaTitle = savedInstanceState.getString("MangaTitle");
        chaptersAvailableHint = savedInstanceState.getBoolean("AreChaptersAvailableHint", true);

        model = new ViewModelProvider((ViewModelStoreOwner) context).get(ChapterListViewModel.class);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("MangaID", mangaID);
        outState.putString("MangaTitle", mangaTitle);
        outState.putBoolean("AreChaptersAvailableHint", chaptersAvailableHint);
    }

    // Called after onCreate()
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manga_item_list, container, false);

        recyclerView = view.findViewById(R.id.resultsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);

        emoticonView = view.findViewById(R.id.emoticonView);
        errorView = view.findViewById(R.id.errorDescription);
        progressBar = view.findViewById(R.id.progressBar);

        SetChapterRetrieveState(STATE_SEARCHING);

        model.getUiState().observe(getViewLifecycleOwner(), chapterListState -> {
            int state = chapterListState.isSearchCompleted();
            if (state == ChapterListViewModel.ChapterListState.SEARCH_COMPLETED) {
                if (chapterListState.getMangaChapters() == null || chapterListState.getMangaChapters().isEmpty()) {
                    SetChapterRetrieveState(STATE_EMPTY);
                    return;
                }
                FoundChaptersAdapter adapter = new FoundChaptersAdapter(context, chapterListState.getMangaChapters(),
                        (s) -> ((ChapterDownloaderActivity) context).OpenURL(s),
                        (i) -> ((ChapterDownloaderActivity) context).ReadChapter(i),
                        (i) -> ((ChapterDownloaderActivity) context).DownloadChapter(i)
                );
                recyclerView.setAdapter(adapter);

                ArrayList<Integer> map = adapter.getIndexMap();
                int bookmarkedPos = map.indexOf(chapterListState.getBookmarkedIndex());
                int positionToScrollTo = map.get(bookmarkedPos - 1) == -1 ? bookmarkedPos - 1 : bookmarkedPos;

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                assert manager != null;
                manager.scrollToPositionWithOffset(positionToScrollTo, 1);

                SetChapterRetrieveState(STATE_COMPLETE);
            }
            else if (state == ChapterListViewModel.ChapterListState.SEARCH_ERROR) {
                SetChapterRetrieveState(STATE_FAIL);
            }
        });

        return view;
    }

    private final static int STATE_SEARCHING = 0;
    private final static int STATE_COMPLETE = 1;
    private final static int STATE_FAIL = 2;
    private final static int STATE_EMPTY = 3;
    private void SetChapterRetrieveState(int state) {
        switch (state) {
            case STATE_SEARCHING:
                recyclerView.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                emoticonView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case STATE_COMPLETE:
                recyclerView.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                emoticonView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                break;
            case STATE_FAIL:
                recyclerView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                emoticonView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                errorView.setText(R.string.errNoConnection);
                emoticonView.setText(CompatUtils.getRandomStringFromStringArray(context, R.array.emoticons_angry));
                break;
            case STATE_EMPTY:
                recyclerView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                emoticonView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                errorView.setText(R.string.mangaNoEntriesFilter);
                emoticonView.setText(CompatUtils.getRandomStringFromStringArray(context, R.array.emoticons_confused));
                break;
        }
    }
}