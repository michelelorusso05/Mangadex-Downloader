package com.littleProgrammers.mangadexdownloader;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.littleProgrammers.mangadexdownloader.apiResults.Manga;

import java.util.ArrayList;
import java.util.HashMap;

public class ViewModelMangaList extends ViewModel {
    private final MutableLiveData<MangaListState> uiState =
            new MutableLiveData<>(new MangaListState());
    public LiveData<MangaListState> getUiState() {
        return uiState;
    }

    public int GetCurrentSearchState() {
        if (uiState.getValue() == null)
            return MangaListState.SEARCH_NOT_COMPLETED;

        return uiState.getValue().lastSearchState;
    }

    public boolean HasData() {
        return uiState.getValue() != null && (!uiState.getValue().mangas.isEmpty() || uiState.getValue().getCachedQuery() != null);
    }

    public void ReplaceMangaList(ArrayList<Manga> mangas, int state, String query) {
        ReplaceMangaList(mangas, null, state, query);
    }
    public void ReplaceMangaList(ArrayList<Manga> mangas, HashMap<Integer, String> sectionMap, int state, String query) {
        MangaListState mangaListState = uiState.getValue();

        if (mangaListState == null)
            mangaListState = new MangaListState();

        mangaListState.ReplaceMangas(mangas, sectionMap, state, query);

        uiState.postValue(mangaListState);
    }

    public void UpdateMangaList(ArrayList<Manga> mangas, int state) {
        UpdateMangaList(mangas, null, state);
    }
    public void UpdateMangaList(ArrayList<Manga> mangas, HashMap<Integer, String> sectionMap, int state) {
        MangaListState mangaListState = uiState.getValue();

        if (mangaListState == null)
            mangaListState = new MangaListState();

        mangaListState.AddMangas(mangas, sectionMap, state);
        uiState.postValue(mangaListState);
    }

    public static class MangaListState {
        public MangaListState() {
            mangas = new ArrayList<>();
            mangaDiff = new ArrayList<>();

            sectionMap = new HashMap<>();
            sectionsDiff = new HashMap<>();

            lastSearchState = SEARCH_NOT_COMPLETED;
        }

        public static final int SEARCH_NOT_COMPLETED = 0;
        public static final int SEARCH_ERROR = 1;
        public static final int SEARCH_COMPLETED = 2;
        public static final int SEARCH_COMPLETED_HAS_MORE = 3;

        private final ArrayList<Manga> mangas;
        private final ArrayList<Manga> mangaDiff;

        private final HashMap<Integer, String> sectionMap;
        private final HashMap<Integer, String> sectionsDiff;

        private int lastSearchState;
        private int lastOffset;

        private boolean newSearch;
        private String cachedQuery;

        public void ReplaceMangas(ArrayList<Manga> m, HashMap<Integer, String> s, int state, String query) {
            mangas.clear();
            sectionMap.clear();
            lastOffset = 0;

            AddMangas(m, s, state);
            newSearch = true;
            cachedQuery = query;

        }

        public void AddMangas(ArrayList<Manga> m, HashMap<Integer, String> s, int state) {
            mangaDiff.clear();
            if (m != null) {
                mangaDiff.addAll(m);
                mangas.addAll(mangaDiff);
            }

            sectionsDiff.clear();
            if (s != null) {
                sectionsDiff.putAll(s);
                sectionMap.putAll(sectionsDiff);
            }

            lastSearchState = state;
            newSearch = false;

            lastOffset += mangaDiff.size();
        }

        public ArrayList<Manga> getMangaDiff() {
            return mangaDiff;
        }

        public ArrayList<Manga> getMangas() {
            return mangas;
        }

        public int getLastSearchState() {
            return lastSearchState;
        }

        public HashMap<Integer, String> getSectionMap() {
            return sectionMap;
        }

        public HashMap<Integer, String> getSectionsDiff() {
            return sectionsDiff;
        }

        public boolean isNewSearch() {
            return newSearch;
        }

        public String getCachedQuery() {
            return cachedQuery;
        }

        public int getLastOffset() {
            return lastOffset;
        }
    }
}
