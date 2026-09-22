package com.vernacular.learning.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.MainActivity;
import com.vernacular.learning.R;
import com.vernacular.learning.ui.flashcards.FlashcardActivity;
import com.vernacular.learning.ui.lesson.LessonActivity;
import com.vernacular.learning.ui.translation.VoiceTranslationFragment;
import com.vernacular.learning.utils.PreferenceHelper;

public class HomeFragment extends Fragment {
    private TextView tvLanguagePairDisplay;
    private String currentMotherTongue = "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        ImageView btnDrawerMenu = root.findViewById(R.id.btnDrawerMenu);
        tvLanguagePairDisplay = root.findViewById(R.id.tvLanguagePairDisplay);
        MaterialButton btnChangeLanguage = root.findViewById(R.id.btnChangeLanguage);
        MaterialCardView cardLiveTranslation = root.findViewById(R.id.cardLiveTranslation);
        MaterialCardView cardCurriculum = root.findViewById(R.id.cardCurriculum);
        MaterialCardView cardWorksheetsToolkit = root.findViewById(R.id.cardWorksheetsToolkit);
        MaterialCardView cardFlashcardsToolkit = root.findViewById(R.id.cardFlashcardsToolkit);

        // Restore saved language
        currentMotherTongue = PreferenceHelper.getSelectedMotherTongue(requireContext());
        updateLanguagePairDisplay();

        // 1. Drawer Menu Icon Click
        btnDrawerMenu.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_settings);
            }
        });

        // 2. CHANGE LANGUAGE Button Click
        btnChangeLanguage.setOnClickListener(v -> showLanguagePickerDialog());

        // 3. Live Translation Card Click
        cardLiveTranslation.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new VoiceTranslationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 4. Curriculum Card Click
        cardCurriculum.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LessonActivity.class);
            intent.putExtra("CLASS_NAME", "Class 1");
            intent.putExtra("SUBJECT_NAME", "Mathematics");
            intent.putExtra("MOTHER_TONGUE", currentMotherTongue);
            startActivity(intent);
        });

        // 5. Worksheets Card Click
        cardWorksheetsToolkit.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_materials);
            }
        });

        // 6. Flashcards Card Click
        cardFlashcardsToolkit.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FlashcardActivity.class);
            startActivity(intent);
        });

        return root;
    }

    private void updateLanguagePairDisplay() {
        if (tvLanguagePairDisplay == null) return;

        if (currentMotherTongue.contains("Mundari")) {
            tvLanguagePairDisplay.setText("Hindi   ➔   Mundari  मुंडारी");
        } else if (currentMotherTongue.contains("Ho")) {
            tvLanguagePairDisplay.setText("Hindi   ➔   Ho  हो");
        } else {
            tvLanguagePairDisplay.setText("Hindi   ➔   Santhali  संथाली");
        }
    }

    private void showLanguagePickerDialog() {
        String[] languages = getResources().getStringArray(R.array.mother_tongues_array);
        int checkedItem = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentMotherTongue)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Mother Tongue")
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    currentMotherTongue = languages[which];
                    PreferenceHelper.savePreferences(
                            requireContext(),
                            PreferenceHelper.getSelectedClass(requireContext()),
                            PreferenceHelper.getSelectedSubject(requireContext()),
                            currentMotherTongue
                    );
                    com.vernacular.learning.data.repository.LearningRepository.getInstance(requireContext()).saveSetting(
                            "selected_mother_tongue", currentMotherTongue
                    );
                    updateLanguagePairDisplay();
                    Toast.makeText(requireContext(), "Active language pair updated!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            currentMotherTongue = PreferenceHelper.getSelectedMotherTongue(requireContext());
            updateLanguagePairDisplay();
        }
    }
}
