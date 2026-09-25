package com.vernacular.learning;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vernacular.learning.ui.curriculum.CurriculumFragment;
import com.vernacular.learning.ui.home.HomeFragment;
import com.vernacular.learning.ui.settings.SettingsFragment;
import com.vernacular.learning.ui.worksheets.WorksheetsFragment;
import com.vernacular.learning.utils.ThemeHelper;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_SELECTED_TAB = "KEY_SELECTED_TAB";
    private BottomNavigationView bottomNav;
    private String currentThemeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        currentThemeMode = ThemeHelper.getSavedTheme(this);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_lessons) {
                // Curriculum Module
                selectedFragment = new CurriculumFragment();
            } else if (itemId == R.id.nav_materials) {
                // Worksheets Module
                selectedFragment = new WorksheetsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set initial fragment or restore tab
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        } else {
            int savedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_home);
            bottomNav.setSelectedItemId(savedTab);
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNav != null) {
            outState.putInt(KEY_SELECTED_TAB, bottomNav.getSelectedItemId());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String savedTheme = ThemeHelper.getSavedTheme(this);
        if (currentThemeMode != null && !currentThemeMode.equals(savedTheme)) {
            currentThemeMode = savedTheme;
            recreate();
        }
    }

    public void selectTab(int navItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navItemId);
        }
    }
}
