package com.vernacular.learning.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.vernacular.learning.R;
import com.vernacular.learning.ui.about.AboutActivity;
import com.vernacular.learning.ui.downloads.DownloadContentActivity;
import com.vernacular.learning.utils.PreferenceHelper;
import com.vernacular.learning.utils.ThemeHelper;

public class SettingsFragment extends Fragment {
    private TextView tvCurrentThemeLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        LinearLayout rowLanguage = root.findViewById(R.id.rowSettingLanguage);
        LinearLayout rowDownload = root.findViewById(R.id.rowSettingDownload);
        LinearLayout rowTheme = root.findViewById(R.id.rowSettingTheme);
        LinearLayout rowAbout = root.findViewById(R.id.rowSettingAbout);
        LinearLayout rowHelp = root.findViewById(R.id.rowSettingHelp);
        tvCurrentThemeLabel = root.findViewById(R.id.tvCurrentThemeLabel);

        updateThemeLabel();

        // Language settings picker dialog
        rowLanguage.setOnClickListener(v -> showLanguagePickerDialog());

        // Download Content Screen
        rowDownload.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DownloadContentActivity.class);
            startActivity(intent);
        });

        // Theme Settings Screen
        rowTheme.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ThemeSettingsActivity.class);
            startActivity(intent);
        });

        // About Us Screen
        rowAbout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AboutActivity.class);
            startActivity(intent);
        });

        // Help & Support dialog
        rowHelp.setOnClickListener(v -> showHelpDialog());

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateThemeLabel();
    }

    private void updateThemeLabel() {
        if (tvCurrentThemeLabel != null && getContext() != null) {
            String theme = ThemeHelper.getSavedTheme(requireContext());
            if (ThemeHelper.THEME_DARK.equals(theme)) {
                tvCurrentThemeLabel.setText(R.string.theme_dark);
            } else if (ThemeHelper.THEME_LIGHT.equals(theme)) {
                tvCurrentThemeLabel.setText(R.string.theme_light);
            } else {
                tvCurrentThemeLabel.setText(R.string.theme_system);
            }
        }
    }

    private void showLanguagePickerDialog() {
        String[] languages = getResources().getStringArray(R.array.mother_tongues_array);
        String currentLang = PreferenceHelper.getSelectedMotherTongue(requireContext());
        int checkedItem = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLang)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.setting_language)
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selected = languages[which];
                    PreferenceHelper.savePreferences(
                            requireContext(),
                            PreferenceHelper.getSelectedClass(requireContext()),
                            PreferenceHelper.getSelectedSubject(requireContext()),
                            selected
                    );
                    Toast.makeText(requireContext(), "Mother Tongue set to: " + selected, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.setting_help_support)
                .setMessage("Vernacular Learning is an offline-first educational application built for primary school teachers in Jharkhand.\n\n" +
                        "For support, lesson additions, or dialect inquiries:\nEmail: support.sih2026@vernacularlearning.in")
                .setPositiveButton("OK", null)
                .show();
    }
}
