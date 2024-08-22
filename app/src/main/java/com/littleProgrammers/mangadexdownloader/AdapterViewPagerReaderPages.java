package com.littleProgrammers.mangadexdownloader;

import static androidx.recyclerview.widget.RecyclerView.NO_ID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jsibbold.zoomage.ZoomageView;
import com.littleProgrammers.mangadexdownloader.utils.BitmapUtilities;
import com.littleProgrammers.mangadexdownloader.utils.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** @noinspection FieldCanBeLocal, FieldCanBeLocal , FieldCanBeLocal */
public abstract class AdapterViewPagerReaderPages extends RecyclerView.Adapter<AdapterViewPagerReaderPages.PageViewHolder> {
    final Activity context;
    final String baseUrl;
    final String[] urls;
    final ArrayList<Pair<Integer, Integer>> indexes;

    private final int navigationMode;

    private final int windowHeight;
    private final int windowWidth;
    private final int windowMin;
    private final int windowMax;
    final boolean orientationLandscape;
    final boolean rtl;
    final Runnable onPageTouch;

    final Queue<Boolean> isLandscape = new ConcurrentLinkedQueue<>();

    private final int VIEW_TYPE_PAGE = R.layout.item_pager_image;
    private final int VIEW_TYPE_PREVIOUS_CHAPTER = R.layout.item_pager_previous;
    private final int VIEW_TYPE_NEXT_CHAPTER = R.layout.item_pager_next;
    public static final int NAVIGATION_LEFT = 1;
    public static final int NAVIGATION_RIGHT = 1 << 1;
    public static final int NAVIGATION_ONESHOT = 0;

    @FunctionalInterface
    public interface OnIndexesUpdated {
        void execute(ArrayList<Pair<Integer, Integer>> indexes, ArrayList<Pair<Integer, Integer>> removed);
    }
    OnIndexesUpdated onPageUpdatedCallback;
    @SuppressLint("deprecation")
    public AdapterViewPagerReaderPages(Activity ctx,
                                       String baseUrl,
                                       String[] urls,
                                       int navigationMode,
                                       boolean landscape,
                                       Runnable onPageTouch
    ) {
        this.baseUrl = baseUrl;
        this.urls = urls;

        indexes = new ArrayList<>(this.urls.length);
        for (int i = 0; i < this.urls.length; i++)
            indexes.add(new Pair<>(i, null));

        this.context = ctx;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowWidth = context.getWindowManager().getCurrentWindowMetrics().getBounds().width();
            windowHeight = context.getWindowManager().getCurrentWindowMetrics().getBounds().height();
        }
        else {
            Point size = new Point();
            context.getWindowManager().getDefaultDisplay().getSize(size);
            windowWidth = size.x;
            windowHeight = size.y;
        }

        windowMin = Math.min(windowHeight, windowWidth);
        windowMax = Math.max(windowHeight, windowWidth);

        this.navigationMode = navigationMode;
        this.orientationLandscape = landscape;

        rtl = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("readerRTL", false);

        this.onPageTouch = onPageTouch;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view;

        if (viewType == VIEW_TYPE_PREVIOUS_CHAPTER)
            view = inflater.inflate(R.layout.item_pager_previous, parent, false);
        else if (viewType == VIEW_TYPE_NEXT_CHAPTER)
            view = inflater.inflate(R.layout.item_pager_next, parent, false);
        else
            view = inflater.inflate(R.layout.item_pager_image, parent, false);

        view.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        PageViewHolder holder = new PageViewHolder(view);

        if (holder.image != null)
            holder.image.SetOnSingleTapConfirmedEvent(onPageTouch);

        return holder;
    }

    @Override
    public void onViewRecycled(@NonNull PageViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.image != null) {
            holder.image.setImageBitmap(null);
        }
    }

    @Override
    public long getItemId(int position) {
        if ((position == 0 && checkNavigationParameter(NAVIGATION_LEFT)) || (position == getItemCount() - 1 && checkNavigationParameter(NAVIGATION_RIGHT))) return NO_ID;
        if (!checkNavigationParameter(NAVIGATION_LEFT)) position++;
        return urls[indexes.get(position - 1).first].hashCode() ^ (indexes.get(position - 1).second != null ? urls[indexes.get(position - 1).second].hashCode() : 0);
    }


    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        if (getItemViewType(position) != VIEW_TYPE_PAGE) return;

        holder.itemView.setAlpha(1);

        holder.progressBar.setVisibility(View.VISIBLE);
        //holder.image.setImageBitmap(null);

        Integer key = position;
        holder.progressBar.setTag(key);

        position = rawPositionToChapterPosition(holder.getBindingAdapterPosition());

        Dispatcher<Bitmap> dispatcher = new Dispatcher<>(2, (ArrayList<Bitmap> bitmaps) -> new Thread(() -> {
            Bitmap combined = BitmapUtilities.combineBitmaps(bitmaps.get(0), bitmaps.get(1), rtl);

            context.runOnUiThread(() -> {
                holder.progressBar.setVisibility(View.INVISIBLE);
                holder.image.setImageBitmap(combined);
            });
        }).start());


        synchronized (indexes) {
            loadBitmapAtPosition(indexes.get(position).first, (Bitmap b) -> dispatcher.UpdateProgress(b, 0));
            Integer secondPos = indexes.get(position).second;
            if (secondPos != null)
                loadBitmapAtPosition(secondPos, (Bitmap b) -> dispatcher.UpdateProgress(b, 1));
            else
                dispatcher.UpdateProgress(null, 1);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position, @NonNull List<Object> payloads) {
        position = holder.getBindingAdapterPosition();
        if (payloads.isEmpty() || indexes.get(position).second != null) {
            onBindViewHolder(holder, position);
        }

        // No need to update
    }
    public void setOnPageUpdatedCallback(OnIndexesUpdated callback) {
        onPageUpdatedCallback = callback;
    }
    public interface LoadBitmap { void onLoadFinished(Bitmap b); }
    protected abstract void loadBitmapAtPosition(int pos, LoadBitmap callback);
    protected abstract void preloadImageAtPosition(int pos, Consumer<Boolean> callback);

    static final int IMAGE_PRELOAD_BLOCK = 4;
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicBoolean shouldPushUpdates = new AtomicBoolean(true);
    private boolean firstPage = true;

    private synchronized void constructIndexes(boolean more) {
        if (!orientationLandscape) return;

        ArrayList<Pair<Integer, Integer>> removedIndexes = new ArrayList<>(2);

        do {
            boolean cur = Boolean.TRUE.equals(isLandscape.poll());

            if (firstPage || cur) {
                firstPage = false;
                counter.getAndIncrement();
            } else {
                Boolean next = isLandscape.peek();
                if (next == null) {
                    if (more) break;
                    counter.getAndIncrement();
                    break;
                }

                if (next) {
                    counter.getAndIncrement();
                } else {
                    notifyItemChanged(counter.get(), "ChangeToDouble");

                    final int c = counter.getAndIncrement();

                    indexes.set(c, new Pair<>(indexes.get(c).first, indexes.get(c + 1).first));
                    removedIndexes.add(indexes.get(c));
                    notifyItemRemoved(c + 1);
                    indexes.remove(c + 1);
                    isLandscape.remove();
                }
            }
        } while (isLandscape.size() > 1);

        if (onPageUpdatedCallback != null && shouldPushUpdates.get()) onPageUpdatedCallback.execute(indexes, removedIndexes);
    }
    protected void startPreload() {
        preloadBlock(0);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        detach();
    }
    public void detach() {
        shouldPushUpdates.set(false);
    }

    private void preloadBlock(int start) {
        if (!shouldPushUpdates.get()) return;

        int last = start + IMAGE_PRELOAD_BLOCK;
        boolean more = true;

        if (last >= urls.length) {
            last = urls.length;
            more = false;
        }

        boolean continuePreloading = more;
        Dispatcher<Boolean> dispatcher = new Dispatcher<>(last - start, (booleans) -> {
            isLandscape.addAll(booleans);

            context.runOnUiThread(() -> constructIndexes(continuePreloading));
            if (continuePreloading)
                preloadBlock(start + IMAGE_PRELOAD_BLOCK);
        });

        for (int i = start; i < last; i++) {
            final int index = i - start;
            preloadImageAtPosition(i, (b) -> dispatcher.UpdateProgress(b, index));
        }
    }
    private void onPreloadFinished() {

    }
    protected Bitmap fromBytes(byte[] bytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        // Takes the biggest side as the target resolution. However, some chapters feature long strips as images.
        // In such cases, take the smallest side instead.
        int imageTarget = (options.outHeight > options.outWidth ? options.outHeight / options.outWidth : options.outWidth / options.outHeight) < 3 ?
                Math.max(options.outHeight, options.outWidth) : Math.min(options.outHeight, options.outWidth);

        options.inJustDecodeBounds = false;
        options.inSampleSize = (int) Math.ceil(Math.log10((float) imageTarget / windowMin) / Math.log10(2));

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    private boolean checkNavigationParameter(int compareAgainst) {
        return (navigationMode & compareAgainst) > 0;
    }

    @Override
    public int getItemCount() {
        return getTotalElements() +
                (checkNavigationParameter(NAVIGATION_LEFT) ? 1 : 0) +
                (checkNavigationParameter(NAVIGATION_RIGHT) ? 1 : 0);
    }

    public int getTotalElements() {
        return indexes.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && checkNavigationParameter(NAVIGATION_LEFT)) return VIEW_TYPE_PREVIOUS_CHAPTER;

        if (position == getTotalElements() + (checkNavigationParameter(NAVIGATION_LEFT) ? 1 : 0)
                && checkNavigationParameter(NAVIGATION_RIGHT)) return VIEW_TYPE_NEXT_CHAPTER;
        return VIEW_TYPE_PAGE;
    }

    /**
     * Returns the first chapter page.
     * @return The first page that contains an image.
     */
    public int getFirstPage() {
        return checkNavigationParameter(NAVIGATION_LEFT) ? 1 : 0;
    }
    /**
     * Returns the last chapter page.
     * @return The last page that contains an image.
     */
    public int getLastPage() {
        int n = indexes.size() - 1;
        if (checkNavigationParameter(NAVIGATION_LEFT)) n++;
        return n;
    }

    public int rawIndexToChapterPage(int index)
    {
        int n = rawPositionToChapterPosition(index);
        return indexes.get(n).first + 1;
    }

    public int rawPositionToChapterPosition(int position) {
        if (checkNavigationParameter(NAVIGATION_LEFT)) position--;
        return position;
    }
    public int chapterPositionToRawPosition(int position) {
        if (checkNavigationParameter(NAVIGATION_LEFT)) position++;
        return position;
    }

    public static int BooleanToParameter(boolean left, boolean right) {
        int n = 0;
        if (left) n += NAVIGATION_LEFT;
        if (right) n += NAVIGATION_RIGHT;

        return n;
    }

    public static class PageViewHolder extends RecyclerView.ViewHolder {
        public final ZoomageView image;
        public final View progressBar;
        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            progressBar = itemView.findViewById(R.id.pageProgress);
        }
    }
}
