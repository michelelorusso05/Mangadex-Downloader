package com.littleProgrammers.mangadexdownloader;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.littleProgrammers.mangadexdownloader.utils.BetterSpinner;

public abstract class ReaderActivity extends AppCompatActivity {
    protected String baseUrl;
    protected String[] urls = new String[0];
    protected boolean landscape;
    protected boolean showControls = true;
    protected ViewPager2 pager;
    protected BetterSpinner pageSelection;
    protected ImageButton previous, next, first, last;
    protected ProgressBar pageProgressIndicator;

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
    private ActionBar actionBar;
    private Button showControlsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars.
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        Toolbar t = findViewById(R.id.toolbar);
        setSupportActionBar(t);
        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        showControlsButton = findViewById(R.id.showControls);
        showControlsButton.setOnClickListener((v) -> {
            exitFullscreen();
        });

        pageSelection = new BetterSpinner(findViewById(R.id.progressView));
        pageProgressIndicator = findViewById(R.id.pageProgress);
        pager = findViewById(R.id.pager);

        previous = findViewById(R.id.previousButton);
        next = findViewById(R.id.nextButton);
        first = findViewById(R.id.firstButton);
        last = findViewById(R.id.lastButton);

        previous.setOnClickListener(this::previousPage);
        next.setOnClickListener(this::nextPage);
        first.setOnClickListener(this::firstPage);
        last.setOnClickListener(this::lastPage);

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
        int pos = ((ReaderPagesAdapter) pager.getAdapter()).rawIndexToChapterPage(pager.getCurrentItem());
        outState.putInt("currentPage", pos);
        Log.d("Saving", String.valueOf(pos));
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.reader_toolbar, menu);
        menu.getItem(1).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.action_show_hide_controls) {
            if (showControls)
                enterFullscreen();
            else
                exitFullscreen();

            item.setIcon(showControls ? R.drawable.ic_baseline_navigation_on_24 : R.drawable.ic_baseline_navigation_off_24);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enterFullscreen() {
        showControls = false;
        // showControlsButton.setVisibility(View.VISIBLE);
        View controls = findViewById(R.id.controls);
        controls.setVisibility(View.INVISIBLE);
        // windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // actionBar.hide();
    }
    private void exitFullscreen() {
        showControls = true;
        // showControlsButton.setVisibility(View.GONE);
        View controls = findViewById(R.id.controls);
        controls.setVisibility(View.VISIBLE);
        // windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
        // WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        // actionBar.show();
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

    public void nextPage(View v) {
        if (pageSelection.getSelectedItemPosition() < pageSelection.getCount() - 1)
            pageSelection.setSelection(pageSelection.getSelectedItemPosition() + 1);
    }
    public void previousPage(View v) {
        if (pageSelection.getSelectedItemPosition() > 0)
            pageSelection.setSelection(pageSelection.getSelectedItemPosition() - 1);
    }
    public void firstPage(View v) {
        pageSelection.setSelection(0);
    }
    public void lastPage(View v) {
        pageSelection.setSelection(pageSelection.getCount() - 1);
    }

    protected void LockPager() {
        pager.setTag("spinnerControl");
    }
    protected void LockSelectionSpinner() {
        pageSelection.spinner.setTag("pagerLock");
    }
}
