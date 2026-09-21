package com.vernacular.learning.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    public static final String PREFS_NAME = "vernacular_prefs";
    public static final String KEY_THEME = "app_theme_mode";

    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";
    public static final String THEME_SYSTEM = "SYSTEM";

    /**
     * Applies the saved theme immediately upon app startup or switch.
     */
    public static void applyTheme(Context context) {
        String theme = getSavedTheme(context);
        switch (theme) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Updates theme preference and applies it immediately across the app without restart.
     */
    public static void setTheme(Context context, String themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeMode).apply();
        applyTheme(context);
    }

    /**
     * Returns the currently saved theme.
     */
    public static String getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_LIGHT);
    }

    public static boolean isDarkMode(Context context) {
        String theme = getSavedTheme(context);
        if (THEME_DARK.equals(theme)) {
            return true;
        } else if (THEME_LIGHT.equals(theme)) {
            return false;
        }
        int nightMode = context.getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}
