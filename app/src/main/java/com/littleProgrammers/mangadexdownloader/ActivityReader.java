package com.littleProgrammers.mangadexdownloader;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.util.Consumer;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.littleProgrammers.mangadexdownloader.utils.BetterSpinner;

public abstract class ActivityReader extends AppCompatActivity {
    protected String baseUrl;
    protected String[] urls = new String[0];
    protected boolean landscape;
    protected boolean showControls = true;
    protected ViewPager2 pager;
    protected BetterSpinner pageSelection;
    protected MaterialButton previous, next, first, last;
    protected LinearProgressIndicator pageProgressIndicator;
    protected Toolbar toolbar;
    protected View controls;

    // Bitmap configuration (applies to this class, all methods)
    public static final BitmapFactory.Options opt;

    static {
        // Bitmap config
        opt = new BitmapFactory.Options();
        opt.inMutable = true;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inSampleSize = 1;
    }
    private WindowInsetsControllerCompat windowInsetsController;

    private View overlay;

    private boolean isShowing = true;

    final OnApplyWindowInsetsListener windowInsetsListener = (v, insets) -> {
        Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
        v.setPadding(i.left, 0, i.right, i.bottom);

        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        p.setMargins(i.left, i.top, i.right, 0);

        return insets;
    };

    final Consumer<Boolean> onTouch = (b) -> {
        if (b == null) b = isShowing;

        if (b) {
            ViewCompat.setOnApplyWindowInsetsListener(controls, null);

            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

            overlay.animate()
                    .alpha(0)
                    .setStartDelay(75)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        ViewCompat.setOnApplyWindowInsetsListener(controls, windowInsetsListener);
                        overlay.setVisibility(View.GONE);
                    });

            /*
            pageProgressIndicator.animate()
                    .alpha(1)
                    .setStartDelay(50)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .withStartAction(() -> pageProgressIndicator.setVisibility(View.VISIBLE));

             */
        }
        else {
            ViewCompat.setOnApplyWindowInsetsListener(controls, null);
            overlay.setVisibility(View.VISIBLE);

            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());

            overlay.animate()
                    .alpha(1)
                    .setStartDelay(75)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() ->
                            ViewCompat.setOnApplyWindowInsetsListener(controls, windowInsetsListener));

            /*
            pageProgressIndicator.animate()
                    .alpha(0)
                    .setStartDelay(50)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> pageProgressIndicator.setVisibility(View.GONE));

             */
        }
        isShowing = !b;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars.
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        );

        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        controls = findViewById(R.id.controls);

        overlay = findViewById(R.id.overlay);

        ViewCompat.setOnApplyWindowInsetsListener(controls, windowInsetsListener);

        ViewCompat.setOnApplyWindowInsetsListener(
                getWindow().getDecorView(),
                ViewCompat::onApplyWindowInsets);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        pageSelection = new BetterSpinner(findViewById(R.id.progressView));
        pageProgressIndicator = findViewById(R.id.pageProgress);
        pager = findViewById(R.id.pager);

        previous = findViewById(R.id.previousButton);
        next = findViewById(R.id.nextButton);
        first = findViewById(R.id.firstButton);
        last = findViewById(R.id.lastButton);

        previous.setOnClickListener(this::PreviousPage);
        next.setOnClickListener(this::NextPage);
        first.setOnClickListener(this::FirstPage);
        last.setOnClickListener(this::LastPage);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("readerRTL", false))
            pager.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        pager.setOffscreenPageLimit(1);

        pager.setSaveEnabled(false);
        pageSelection.spinner.setSaveEnabled(false);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pager == null || pager.getAdapter() == null) return;
        int pos = ((AdapterViewPagerReaderPages) pager.getAdapter()).rawIndexToChapterPage(pager.getCurrentItem());
        outState.putInt("currentPage", pos);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.reader_toolbar, menu);
        menu.getItem(0).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        String volumenavigation = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("readerVolumeKeysNavigation", "dis");
        if ("dis".equals(volumenavigation)) return super.onKeyDown(keyCode, event);
        boolean inverted = "en_rev".equals(volumenavigation);

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (inverted)
                pager.setCurrentItem(pager.getCurrentItem() - 1);
            else
                pager.setCurrentItem(pager.getCurrentItem() + 1);
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (inverted)
                pager.setCurrentItem(pager.getCurrentItem() + 1);
            else
                pager.setCurrentItem(pager.getCurrentItem() - 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void GeneratePageSelectionSpinnerAdapter() {
        int l = urls.length;
        String[] pages = new String[l];
        for (int i = 0; i < l; i++)
            pages[i] = UpdatePageIndicator(i);

        pageSelection.setAdapter(new ArrayAdapter<>(this, R.layout.page_indicator_spinner_item, pages));
    }

    protected String UpdatePageIndicator(int index) {
        return getString(R.string.pageIndicator, index + 1);
    }

    public void NextPage(View v) {
        if (pageSelection.getSelectedItemPosition() < pageSelection.getCount() - 1)
            pageSelection.setSelection(pageSelection.getSelectedItemPosition() + 1);
    }
    public void PreviousPage(View v) {
        if (pageSelection.getSelectedItemPosition() > 0)
            pageSelection.setSelection(pageSelection.getSelectedItemPosition() - 1);
    }
    public void FirstPage(View v) {
        pageSelection.setSelection(0);
    }
    public void LastPage(View v) {
        pageSelection.setSelection(pageSelection.getCount() - 1);
    }

    protected void LockPager() {
        pager.setTag("spinnerControl");
    }
    protected void LockSelectionSpinner() {
        pageSelection.spinner.setTag("pagerLock");
    }
}
