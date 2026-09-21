package com.vernacular.learning.ui.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import androidx.appcompat.app.AlertDialog;
import com.vernacular.learning.R;
import com.vernacular.learning.utils.ThemeHelper;

public class ThemeSelectionDialog {
    public static void show(Context context, Runnable onThemeChanged) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_theme_selection, null);

        View choiceLight = view.findViewById(R.id.dialogChoiceLight);
        View choiceDark = view.findViewById(R.id.dialogChoiceDark);
        View choiceSystem = view.findViewById(R.id.dialogChoiceSystem);

        RadioButton radioLight = view.findViewById(R.id.dialogRadioLight);
        RadioButton radioDark = view.findViewById(R.id.dialogRadioDark);
        RadioButton radioSystem = view.findViewById(R.id.dialogRadioSystem);

        String currentTheme = ThemeHelper.getSavedTheme(context);
        radioLight.setChecked(ThemeHelper.THEME_LIGHT.equals(currentTheme));
        radioDark.setChecked(ThemeHelper.THEME_DARK.equals(currentTheme));
        radioSystem.setChecked(ThemeHelper.THEME_SYSTEM.equals(currentTheme));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        choiceLight.setOnClickListener(v -> {
            ThemeHelper.setTheme(context, ThemeHelper.THEME_LIGHT);
            dialog.dismiss();
            if (onThemeChanged != null) onThemeChanged.run();
        });

        choiceDark.setOnClickListener(v -> {
            ThemeHelper.setTheme(context, ThemeHelper.THEME_DARK);
            dialog.dismiss();
            if (onThemeChanged != null) onThemeChanged.run();
        });

        choiceSystem.setOnClickListener(v -> {
            ThemeHelper.setTheme(context, ThemeHelper.THEME_SYSTEM);
            dialog.dismiss();
            if (onThemeChanged != null) onThemeChanged.run();
        });

        dialog.show();
    }
}
