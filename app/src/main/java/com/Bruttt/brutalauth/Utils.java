package com.Bruttt.brutalauth;

import android.content.Context;
import android.util.DisplayMetrics;

public class Utils {
    private final float density;
    public Utils(Context ctx) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        density = dm.density;
    }
    public int FixDP(int dp) { return Math.round(dp * density); }
}
