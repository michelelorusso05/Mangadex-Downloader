package com.littleProgrammers.mangadexdownloader;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.littleProgrammers.mangadexdownloader.apiResults.Manga;

import java.util.ArrayList;
import java.util.HashMap;

public class ViewModelMangaList extends ViewModel {
    private final MutableLiveData<MangaListState> uiState =
            new MutableLiveData<>(new MangaListState(null, null, 0));
    public LiveData<MangaListState> getUiState() {
        return uiState;
    }

    public void updateMangaList(ArrayList<Manga> mangas, int state) {
        uiState.setValue(new MangaListState(mangas, null, state));
    }
    public void updateMangaList(ArrayList<Manga> mangas, HashMap<Integer, String> sectionMap, int state) {
        uiState.setValue(new MangaListState(mangas, sectionMap, state));
    }

    public static class MangaListState {
        public MangaListState(ArrayList<Manga> m, HashMap<Integer, String> s, int alreadySearched) {
            mangas = m;
            sectionMap = s;
            searchState = alreadySearched;
        }
        public static final int SEARCH_NOT_COMPLETED = 0;
        public static final int SEARCH_COMPLETED = 1;
        public static final int SEARCH_ERROR = 2;

        private final ArrayList<Manga> mangas;


        private final HashMap<Integer, String> sectionMap;
        private final int searchState;

        public ArrayList<Manga> getMangas() {
            return mangas;
        }

        public HashMap<Integer, String> getSectionMap() {
            return sectionMap;
        }

        public int isSearchCompleted() {
            return searchState;
        }
    }
}
