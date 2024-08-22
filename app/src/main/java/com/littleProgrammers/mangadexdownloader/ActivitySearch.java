package com.littleProgrammers.mangadexdownloader;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.littleProgrammers.mangadexdownloader.utils.CompatUtils;
import com.littleProgrammers.mangadexdownloader.utils.FavouriteManager;

public class ActivitySearch extends AppCompatActivity {
    AppBarLayout appBarLayout;
    NavigationRailView navigationRailView;
    ViewPager2 viewPager;
    NavigationBarView navigationView;

    FragmentMangaSearch fragmentMangaSearch;
    FragmentMangaFavourites fragmentMangaFavourites;

    ViewPager2.OnPageChangeCallback changeCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar t = findViewById(R.id.home_toolbar);
        setSupportActionBar(t);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        ViewCompat.setOnApplyWindowInsetsListener(t, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.setMargins(i.left, 0, i.right, 0);

            return insets;
        });

        viewPager = findViewById(R.id.viewpager);

        ViewCompat.setOnApplyWindowInsetsListener(viewPager, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.setMargins(params.leftMargin, 0, 0, (navigationView instanceof NavigationRailView) ? 0 : CompatUtils.ConvertDpToPixel(80, this) + i.bottom);

            return insets;
        });

        navigationView = findViewById(R.id.navigationView);

        appBarLayout = findViewById(R.id.appbarlayout);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        fragmentMangaSearch = new FragmentMangaSearch();
                        return fragmentMangaSearch;
                    case 1:
                        fragmentMangaFavourites = new FragmentMangaFavourites();
                        return fragmentMangaFavourites;
                    default:
                        throw new IllegalArgumentException();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        appBarLayout.setLiftOnScroll(true);

        viewPager.setOffscreenPageLimit(1);

        navigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.section_search)
                viewPager.setCurrentItem(0, true);
            else
                viewPager.setCurrentItem(1, true);
            return true;
        });

        if (!FavouriteManager.GetFavouritesIDs(this).isEmpty()) {
            Menu menu = navigationView.getMenu();
            menu.findItem(R.id.section_favourites).setChecked(true);
            viewPager.setCurrentItem(1, false);
        }

        changeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Menu menu = navigationView.getMenu();
                switch (position) {
                    case 0: {
                        menu.findItem(R.id.section_search).setChecked(true);

                        RecyclerView recyclerView = findViewById(GetRecyclerViewTag(FragmentMangaSearch.TAG));
                        if (recyclerView != null)
                            appBarLayout.setLifted(recyclerView.computeVerticalScrollOffset() > 0);

                        appBarLayout.setLiftOnScrollTargetViewId(GetRecyclerViewTag(FragmentMangaSearch.TAG));

                        break;
                    }
                    case 1: {
                        menu.findItem(R.id.section_favourites).setChecked(true);

                        RecyclerView recyclerView = findViewById(GetRecyclerViewTag(FragmentMangaFavourites.TAG));
                        if (recyclerView != null)
                            appBarLayout.setLifted(recyclerView.computeVerticalScrollOffset() > 0);

                        appBarLayout.setLiftOnScrollTargetViewId(GetRecyclerViewTag(FragmentMangaFavourites.TAG));

                        break;
                    }
                }
            }
        };

        viewPager.registerOnPageChangeCallback(changeCallback);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Force callback
        viewPager.post(() -> changeCallback.onPageSelected(viewPager.getCurrentItem()));
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.search_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, ActivitySettings.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_downloadedfiles) {
            startActivity(new Intent(this, ActivityDownloadedFiles.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int GetRecyclerViewTag(String tag) {
        return Math.abs(tag.hashCode());
    }
}
