package com.vernacular.learning.ui.settings;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.utils.ThemeHelper;

public class ThemeSettingsActivity extends AppCompatActivity {
    private MaterialCardView cardOptionLight;
    private MaterialCardView cardOptionDark;
    private MaterialCardView cardOptionSystem;

    private RadioButton radioLight;
    private RadioButton radioDark;
    private RadioButton radioSystem;

    private String selectedTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_settings);

        ImageView btnBack = findViewById(R.id.btnThemeBack);
        cardOptionLight = findViewById(R.id.cardOptionLight);
        cardOptionDark = findViewById(R.id.cardOptionDark);
        cardOptionSystem = findViewById(R.id.cardOptionSystem);

        radioLight = findViewById(R.id.radioLight);
        radioDark = findViewById(R.id.radioDark);
        radioSystem = findViewById(R.id.radioSystem);

        MaterialButton btnApply = findViewById(R.id.btnApplyTheme);

        selectedTheme = ThemeHelper.getSavedTheme(this);
        updateSelectionUI();

        btnBack.setOnClickListener(v -> finish());

        cardOptionLight.setOnClickListener(v -> {
            selectedTheme = ThemeHelper.THEME_LIGHT;
            updateSelectionUI();
        });

        cardOptionDark.setOnClickListener(v -> {
            selectedTheme = ThemeHelper.THEME_DARK;
            updateSelectionUI();
        });

        cardOptionSystem.setOnClickListener(v -> {
            selectedTheme = ThemeHelper.THEME_SYSTEM;
            updateSelectionUI();
        });

        btnApply.setOnClickListener(v -> {
            ThemeHelper.setTheme(ThemeSettingsActivity.this, selectedTheme);
            Toast.makeText(ThemeSettingsActivity.this, "Theme applied successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateSelectionUI() {
        boolean isLight = ThemeHelper.THEME_LIGHT.equals(selectedTheme);
        boolean isDark = ThemeHelper.THEME_DARK.equals(selectedTheme);
        boolean isSystem = ThemeHelper.THEME_SYSTEM.equals(selectedTheme);

        radioLight.setChecked(isLight);
        radioDark.setChecked(isDark);
        radioSystem.setChecked(isSystem);

        int activeStrokeColor = ContextCompat.getColor(this, isDark ? R.color.primary_bright_teal : R.color.primary_forest_green);
        int inactiveStrokeColor = ContextCompat.getColor(this, isDark ? R.color.border_dark : R.color.border_light);

        cardOptionLight.setStrokeColor(isLight ? activeStrokeColor : inactiveStrokeColor);
        cardOptionLight.setStrokeWidth(isLight ? 4 : 2);

        cardOptionDark.setStrokeColor(isDark ? activeStrokeColor : inactiveStrokeColor);
        cardOptionDark.setStrokeWidth(isDark ? 4 : 2);

        cardOptionSystem.setStrokeColor(isSystem ? activeStrokeColor : inactiveStrokeColor);
        cardOptionSystem.setStrokeWidth(isSystem ? 4 : 2);
    }
}
