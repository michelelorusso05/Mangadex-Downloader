package com.littleProgrammers.mangadexdownloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.chip.ChipGroup;

public class ViewSingleLineChipGroup extends ChipGroup {
    public ViewSingleLineChipGroup(Context context) {
        super(context);
    }

    public ViewSingleLineChipGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewSingleLineChipGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onLayout(boolean sizeChanged, int left, int top, int right, int bottom) {
        if (getChildCount() == 0) {
            // Do not re-layout when there are no children.
            return;
        }

        boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
        int paddingStart = isRtl ? getPaddingRight() : getPaddingLeft();
        int paddingEnd = isRtl ? getPaddingLeft() : getPaddingRight();
        int childStart = paddingStart;
        int childTop = getPaddingTop();
        int childBottom = childTop;
        int childEnd;

        final int maxChildEnd = right - left - paddingEnd;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == View.GONE) {
                continue;
            }

            ViewGroup.LayoutParams lp = child.getLayoutParams();
            int startMargin = 0;
            int endMargin = 0;
            if (lp instanceof MarginLayoutParams) {
                MarginLayoutParams marginLp = (MarginLayoutParams) lp;
                startMargin = MarginLayoutParamsCompat.getMarginStart(marginLp);
                endMargin = MarginLayoutParamsCompat.getMarginEnd(marginLp);
            }

            childEnd = childStart + startMargin + child.getMeasuredWidth();

            if (childEnd > right)
                child.setVisibility(GONE);

            childBottom = childTop + child.getMeasuredHeight();

            if (isRtl) {
                child.layout(
                        maxChildEnd - childEnd, childTop, maxChildEnd - childStart - startMargin, childBottom);
            } else {
                child.layout(childStart + startMargin, childTop, childEnd, childBottom);
            }

            childStart += (startMargin + endMargin + child.getMeasuredWidth()) + getItemSpacing();
        }
    }
}
