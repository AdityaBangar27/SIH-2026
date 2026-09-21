package com.vernacular.learning;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vernacular.learning.ui.home.HomeFragment;
import com.vernacular.learning.ui.materials.MaterialsFragment;
import com.vernacular.learning.ui.settings.SettingsFragment;
import com.vernacular.learning.ui.worksheets.WorksheetsFragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_lessons) {
                selectedFragment = new WorksheetsFragment();
            } else if (itemId == R.id.nav_materials) {
                selectedFragment = new MaterialsFragment();
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

        // Set initial fragment to Home
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        }
    }

    public void selectTab(int navItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navItemId);
        }
    }
}
