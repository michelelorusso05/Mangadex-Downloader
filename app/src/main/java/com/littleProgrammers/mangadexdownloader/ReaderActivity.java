package com.littleProgrammers.mangadexdownloader;

import static com.littleProgrammers.mangadexdownloader.BitmapUtilities.combineBitmaps;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jsibbold.zoomage.ZoomageView;

public class ReaderActivity extends AppCompatActivity {
    protected String baseUrl;
    protected String[] urls = new String[0];
    protected boolean landscape;
    protected boolean showControls = true;

    protected Spinner pageSelection;
    protected ImageButton previous, next, first, last;
    protected ZoomageView display;
    protected View pBar;
    protected ProgressBar pageProgressIndicator;

    // Bitmap configuration (applies to this class, all methods)
    protected final BitmapFactory.Options opt = new BitmapFactory.Options();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // Bitmap config
        opt.inMutable = true;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inSampleSize = 1;

        display = findViewById(R.id.displayView);

        display.SetOnSingleTapConfirmedEvent((MotionEvent e) -> {
            if (e.getX() < (float) display.getWidth() / 2)
                previousPage(null);
            else
                nextPage(null);
        });

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        pageSelection = findViewById(R.id.progressView);
        pBar = findViewById(R.id.loadingImage);
        pageProgressIndicator = findViewById(R.id.pageProgress);

        previous = findViewById(R.id.previousButton);
        next = findViewById(R.id.nextButton);
        first = findViewById(R.id.firstButton);
        last = findViewById(R.id.lastButton);

        previous.setOnClickListener(this::previousPage);
        next.setOnClickListener(this::nextPage);
        first.setOnClickListener(this::firstPage);
        last.setOnClickListener(this::lastPage);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int savedPage = savedInstanceState.getInt("currentPage", 0);
        pageSelection.setSelection(savedPage);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (landscape)
            outState.putInt("currentPage", pageSelection.getSelectedItemPosition() * 2);
        else
            outState.putInt("currentPage", (int) Math.floor((float) pageSelection.getSelectedItemPosition() / 2));
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
            showControls = !showControls;
            item.setIcon(showControls ? R.drawable.ic_baseline_navigation_on_24 : R.drawable.ic_baseline_navigation_off_24);
            View controls = findViewById(R.id.controls);
            controls.setVisibility(showControls ? View.VISIBLE : View.INVISIBLE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void GeneratePageSelectionSpinnerAdapter() {
        int l = (int) Math.ceil((float) urls.length / pageStep());
        String[] pages = new String[l];
        for (int i = 0; i < l; i++)
            pages[i] = UpdatePageIndicator(i * pageStep());

        pageSelection.setAdapter(new ArrayAdapter<>(this, R.layout.page_indicator_spinner_item, pages));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected String UpdatePageIndicator(int index) {
        if (landscape && index < urls.length - 1)
            return getString(R.string.doublePageIndicator, index + 1, index + 2);
        return getString(R.string.pageIndicator, index + 1);
    }

    protected void turnPage(int index) {
        // To be implemented in inherithed classes
    }

    protected void BitmapRetrieveDone(Bitmap b1, Bitmap b2) {
        new Thread(() -> {
            Bitmap b = combineBitmaps(b1, b2);
            runOnUiThread(() -> {
                display.setVisibility(View.VISIBLE);
                pBar.setVisibility(View.GONE);
                display.setImageBitmap(b);
            });
        }).start();
    }

    protected int GetCurIndex() {
        return pageSelection.getSelectedItemPosition() * pageStep();
    }


    protected int pageStep() {
        return landscape ? 2 : 1;
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
}
