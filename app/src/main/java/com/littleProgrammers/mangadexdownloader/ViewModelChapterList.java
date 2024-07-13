package com.littleProgrammers.mangadexdownloader;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.littleProgrammers.mangadexdownloader.apiResults.Chapter;

import java.util.ArrayList;

public class ViewModelChapterList extends ViewModel {
    private final MutableLiveData<ChapterListState> uiState =
            new MutableLiveData<>(new ChapterListState(null, 0, 0));
    public LiveData<ChapterListState> getUiState() {
        return uiState;
    }

    public void updateChapterList(@NonNull ArrayList<Chapter> chapters, int state, int bookmark) {
        uiState.setValue(new ChapterListState(chapters, state, bookmark));
    }

    public static class ChapterListState {
        public ChapterListState(ArrayList<Chapter> chapters, int alreadySearched, int bookmark) {
            mangaChapters = chapters;
            searchState = alreadySearched;
            bookmarkedIndex = bookmark;
        }
        public static final int SEARCH_NOT_COMPLETED = 0;
        public static final int SEARCH_COMPLETED = 1;
        public static final int SEARCH_ERROR = 2;

        private final ArrayList<Chapter> mangaChapters;
        private final int searchState;
        private final int bookmarkedIndex;

        public ArrayList<Chapter> getMangaChapters() {
            return mangaChapters;
        }

        public int isSearchCompleted() {
            return searchState;
        }

        public int getBookmarkedIndex() {
            return bookmarkedIndex;
        }
    }
}
