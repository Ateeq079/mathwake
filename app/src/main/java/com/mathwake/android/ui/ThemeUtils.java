package com.mathwake.android.ui;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

public final class ThemeUtils {
    private ThemeUtils() {
    }

    public static int getColor(Context context, int resId) {
        return ContextCompat.getColor(context, resId);
    }

    public static int getColor(Context context, String hex) {
        switch (hex.toUpperCase()) {
            case "#6C63FF":
                return getPrimaryColor(context);
            case "#FF6584":
                return getSecondaryColor(context);
            case "#43E97B":
                return getAccentColor(context);
            case "#FFBE3B":
                return getWarningColor(context);
            case "#F7F8FC":
                return getBackgroundColor(context);
            case "#FFFFFF":
            case "#FFFFFFFF":
                return getSurfaceColor(context);
            case "#F1F1F6":
                return getSurfaceVariantColor(context);
            case "#FFF6E5":
                return getSurfaceAltColor(context);
            case "#33FFFFFF":
                return getSurfaceTranslucentColor(context);
            case "#24FFFFFF":
                return getWhite15Color(context);
            case "#A0FFFFFF":
                return getWhite63Color(context);
            case "#38F9D7":
                return getAccentVariantColor(context);
            case "#D6FBE4":
                return getTextSuccessColor(context);
            case "#E8E8EE":
                return getDividerColor(context);
            case "#2D2D44":
                return getTextPrimaryColor(context);
            case "#6D6A80":
                return getTextSecondaryColor(context);
            case "#A5A2B5":
                return getTextDisabledColor(context);
            default:
                return Color.parseColor(hex);
        }
    }

    public static int getSurfaceColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.surface);
    }

    public static int getSurfaceVariantColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.surface_variant);
    }

    public static int getSurfaceAltColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.surface_alt);
    }

    public static int getSurfaceTranslucentColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.surface_translucent);
    }

    public static int getWhite15Color(Context context) {
        return getColor(context, com.mathwake.android.R.color.white_15);
    }

    public static int getWhite20Color(Context context) {
        return getColor(context, com.mathwake.android.R.color.white_20);
    }

    public static int getWhite63Color(Context context) {
        return getColor(context, com.mathwake.android.R.color.white_63);
    }

    public static int getPrimaryColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.primary);
    }

    public static int getSecondaryColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.secondary);
    }

    public static int getAccentColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.accent);
    }

    public static int getWarningColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.warning);
    }

    public static int getAccentVariantColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.accent_variant);
    }

    public static int getOnPrimaryColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.on_primary);
    }

    public static int getTextSuccessColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.text_success);
    }

    public static int getBackgroundColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.background);
    }

    public static int getDividerColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.divider);
    }

    public static int getTextPrimaryColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.text_primary);
    }

    public static int getTextSecondaryColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.text_secondary);
    }

    public static int getTextDisabledColor(Context context) {
        return getColor(context, com.mathwake.android.R.color.text_disabled);
    }
}
