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
        boolean isDark = isDarkMode(context);
        int targetMode = isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    /**
     * Updates theme preference for dark mode and applies it immediately across the app.
     */
    public static void setDarkMode(Context context, boolean isDark) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, isDark ? THEME_DARK : THEME_LIGHT).commit();
        int targetMode = isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(targetMode);
    }

    /**
     * Updates theme preference and applies it immediately across the app without restart.
     */
    public static void setTheme(Context context, String themeMode) {
        boolean isDark = THEME_DARK.equalsIgnoreCase(themeMode);
        setDarkMode(context, isDark);
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
        return THEME_DARK.equalsIgnoreCase(theme);
    }

    public static int getThemeColor(Context context, int attrResId) {
        if (context == null) return 0;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return 0;
    }
}
