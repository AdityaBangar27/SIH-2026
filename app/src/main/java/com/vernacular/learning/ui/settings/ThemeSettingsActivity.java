package com.vernacular.learning.ui.settings;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.vernacular.learning.R;
import com.vernacular.learning.utils.ThemeHelper;

public class ThemeSettingsActivity extends AppCompatActivity {
    private SwitchMaterial switchDarkMode;
    private ImageView ivThemeIcon;
    private TextView tvThemeStatusLabel;
    private TextView tvThemeDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_settings);

        ImageView btnBack = findViewById(R.id.btnThemeBack);
        MaterialCardView cardThemeSwitch = findViewById(R.id.cardThemeSwitch);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        ivThemeIcon = findViewById(R.id.ivThemeIcon);
        tvThemeStatusLabel = findViewById(R.id.tvThemeStatusLabel);
        tvThemeDescription = findViewById(R.id.tvThemeDescription);

        boolean isDark = ThemeHelper.isDarkMode(this);
        switchDarkMode.setChecked(isDark);
        updateStatusViews(isDark);

        btnBack.setOnClickListener(v -> finish());

        // Tapping the card or switch immediately toggles the theme
        cardThemeSwitch.setOnClickListener(v -> {
            boolean nextState = !switchDarkMode.isChecked();
            applyThemeToggle(nextState);
        });

        switchDarkMode.setOnClickListener(v -> {
            applyThemeToggle(switchDarkMode.isChecked());
        });
    }

    private void applyThemeToggle(boolean enableDark) {
        switchDarkMode.setChecked(enableDark);
        ThemeHelper.setDarkMode(ThemeSettingsActivity.this, enableDark);
        com.vernacular.learning.data.repository.LearningRepository.getInstance(ThemeSettingsActivity.this).saveSetting(
                ThemeHelper.KEY_THEME, enableDark ? ThemeHelper.THEME_DARK : ThemeHelper.THEME_LIGHT
        );
        Toast.makeText(ThemeSettingsActivity.this,
                enableDark ? "Dark mode enabled" : "Light mode enabled",
                Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void updateStatusViews(boolean isDark) {
        if (ivThemeIcon != null) {
            ivThemeIcon.setImageResource(isDark ? R.drawable.ic_moon : R.drawable.ic_sun);
        }
        if (tvThemeStatusLabel != null) {
            tvThemeStatusLabel.setText(isDark ? R.string.dark_mode_status_on : R.string.dark_mode_status_off);
        }
        if (tvThemeDescription != null) {
            tvThemeDescription.setText(isDark ? R.string.theme_dark_desc : R.string.theme_light_desc);
        }
    }
}
