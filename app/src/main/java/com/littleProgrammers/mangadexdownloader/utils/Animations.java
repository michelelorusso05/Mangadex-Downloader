package com.littleProgrammers.mangadexdownloader.utils;

import android.view.View;

public class Animations {
    public static void toggleArrow(View view, boolean isExpanded) {
        if (isExpanded) {
            view.animate().setDuration(200).rotation(180);
        } else {
            view.animate().setDuration(200).rotation(0);
        }
    }
}
