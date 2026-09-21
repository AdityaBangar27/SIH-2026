package com.vernacular.learning.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {
    private static final String PREF_NAME = "vernacular_user_prefs";
    private static final String KEY_CLASS = "selected_class";
    private static final String KEY_SUBJECT = "selected_subject";
    private static final String KEY_MOTHER_TONGUE = "selected_mother_tongue";

    public static void savePreferences(Context context, String studentClass, String subject, String motherTongue) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_CLASS, studentClass)
                .putString(KEY_SUBJECT, subject)
                .putString(KEY_MOTHER_TONGUE, motherTongue)
                .apply();
    }

    public static String getSelectedClass(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_CLASS, "Class 1");
    }

    public static String getSelectedSubject(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SUBJECT, "Mathematics");
    }

    public static String getSelectedMotherTongue(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_MOTHER_TONGUE, "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)");
    }
}
