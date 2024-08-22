package com.littleProgrammers.mangadexdownloader;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewFixedAspectRatioSlottedLayout extends ViewGroup {
    boolean pivotVertical;
    boolean orientationVertical;
    float aspectRatio;
    int gap;

    public ViewFixedAspectRatioSlottedLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ViewFixedAspectRatioSlottedLayout,
                0, 0);

        String aspectRatioString;
        int orientation;

        try {
            aspectRatioString = a.getString(R.styleable.ViewFixedAspectRatioSlottedLayout_aspectRatio);
            // Default is vertical
            orientation = a.getInt(R.styleable.ViewFixedAspectRatioSlottedLayout_orientation, 1);
            gap = a.getDimensionPixelSize(R.styleable.ViewFixedAspectRatioSlottedLayout_gap, 0);
        } finally {
            a.recycle();
        }

        orientationVertical = orientation == 1;

        if (aspectRatioString == null) throw new IllegalArgumentException("aspectRatio must be defined");

        int colonPos = aspectRatioString.indexOf(':');

        char pivot = aspectRatioString.charAt(0);

        if (pivot != 'w' && pivot != 'h')
            throw new IllegalArgumentException("Invalid pivot mode");

        pivotVertical = pivot == 'h';

        int numerator = Integer.parseInt(
                aspectRatioString.substring(aspectRatioString.indexOf(',') + 1,
                        colonPos)
        );
        int denominator = Integer.parseInt(
                (aspectRatioString.substring(colonPos + 1))
        );

        aspectRatio = (float) numerator / denominator;
    }

    public ViewFixedAspectRatioSlottedLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ViewFixedAspectRatioSlottedLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        l += getPaddingLeft();
        t += getPaddingTop();
        b -= getPaddingBottom();
        r -= getPaddingEnd();
        int from = (orientationVertical) ? t : l;
        int to;

        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) continue;

            // Lay out vertically
            if (orientationVertical) {
                to = from + child.getMeasuredHeight();
                child.layout(l, from, r, to);
            }
            // Lay out horizontally
            else {
                to = from + child.getMeasuredWidth();
                child.layout(from, t, to, b);
            }

            from = to + gap;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Pivot is HEIGHT, scale width
        if (pivotVertical)
            width = (int) (height * aspectRatio);
        // Pivot is WIDTH, scale height
        else
            height = (int) (width * aspectRatio);

        int paddedWidth = width - (getPaddingStart() + getPaddingEnd());
        int paddedHeight = height - (getPaddingTop() + getPaddingBottom());

        final int count = getChildCount();

        int availableDimension = (orientationVertical ? paddedHeight : paddedWidth) - gap * (count - 1);

        int w, h;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();

            // Split vertically
            if (orientationVertical) {
                params.width = paddedWidth;
                params.height = (int) (availableDimension * params.slotSize);
            }
            // Split horizontally
            else {
                params.width = (int) (availableDimension * params.slotSize);
                params.height = paddedHeight;
            }

            w = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
            h = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);

            child.measure(w, h);
        }

        setMeasuredDimension(width, height);
    }



    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewFixedAspectRatioSlottedLayout.LayoutParams(getContext(), attrs);
    }
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        float slotSize;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ViewFixedAspectRatioSlottedLayout_Layout);
            slotSize = a.getFloat(R.styleable.ViewFixedAspectRatioSlottedLayout_Layout_slotSizePercent, 0);

            if (slotSize == 0)
                throw new IllegalArgumentException("slotSize must be defined and non-zero");

            a.recycle();
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }
        public LayoutParams(@NonNull ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }
}
