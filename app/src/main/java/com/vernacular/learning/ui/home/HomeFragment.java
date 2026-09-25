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
    private TextView tvLanguageDirectionDetails;
    private String currentMotherTongue = "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)";
    private boolean isHindiToMotherTongue = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        ImageView btnDrawerMenu = root.findViewById(R.id.btnDrawerMenu);
        tvLanguagePairDisplay = root.findViewById(R.id.tvLanguagePairDisplay);
        tvLanguageDirectionDetails = root.findViewById(R.id.tvLanguageDirectionDetails);
        MaterialCardView btnHomeSwapDirection = root.findViewById(R.id.btnHomeSwapDirection);
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

        // Direction Swap Button
        if (btnHomeSwapDirection != null) {
            btnHomeSwapDirection.setOnClickListener(v -> {
                isHindiToMotherTongue = !isHindiToMotherTongue;
                updateLanguagePairDisplay();
                Toast.makeText(requireContext(), isHindiToMotherTongue
                        ? "दिशा: हिंदी ➔ संथाली"
                        : "दिशा: संथाली ➔ हिंदी", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. CHANGE LANGUAGE Button Click
        btnChangeLanguage.setOnClickListener(v -> showLanguagePickerDialog());

        // 3. Live Translation Card Click
        cardLiveTranslation.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new VoiceTranslationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 4. Curriculum Card Click -> Switches to Curriculum Tab
        cardCurriculum.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_lessons);
            }
        });

        // 5. Worksheets Card Click -> Switches to Worksheets Tab
        cardWorksheetsToolkit.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_materials);
            }
        });

        // 6. Flashcards Card Click -> Opens Flashcards Module
        cardFlashcardsToolkit.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FlashcardActivity.class);
            startActivity(intent);
        });

        // 7. Recent Study Material Section Clicks
        TextView tvViewAllCurriculum = root.findViewById(R.id.tvViewAllCurriculum);
        if (tvViewAllCurriculum != null) {
            tvViewAllCurriculum.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(R.id.nav_lessons);
                }
            });
        }

        MaterialCardView cardRecentStudyPlants = root.findViewById(R.id.cardRecentStudyPlants);
        if (cardRecentStudyPlants != null) {
            cardRecentStudyPlants.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.vernacular.learning.ui.curriculum.detail.StudyMaterialDetailActivity.class);
                intent.putExtra("CLASS_NAME", "Class 3");
                intent.putExtra("SUBJECT_NAME", "EVS");
                intent.putExtra("TOPIC_NAME", "Plants Around Us");
                startActivity(intent);
            });
        }

        MaterialCardView cardRecentWorksheetMath = root.findViewById(R.id.cardRecentWorksheetMath);
        if (cardRecentWorksheetMath != null) {
            cardRecentWorksheetMath.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.vernacular.learning.ui.worksheets.detail.WorksheetDetailActivity.class);
                intent.putExtra("CLASS_NAME", "Class 3");
                intent.putExtra("SUBJECT_NAME", "Mathematics");
                intent.putExtra("TOPIC_NAME", "Numbers and Addition");
                startActivity(intent);
            });
        }

        MaterialCardView cardRecentFlashcardsPlant = root.findViewById(R.id.cardRecentFlashcardsPlant);
        if (cardRecentFlashcardsPlant != null) {
            cardRecentFlashcardsPlant.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), FlashcardActivity.class);
                intent.putExtra("CLASS_NAME", "Class 3");
                intent.putExtra("SUBJECT_NAME", "EVS");
                intent.putExtra("TOPIC_NAME", "Plants Around Us");
                startActivity(intent);
            });
        }

        MaterialCardView cardRecentMyResources = root.findViewById(R.id.cardRecentMyResources);
        if (cardRecentMyResources != null) {
            cardRecentMyResources.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.vernacular.learning.ui.resources.MyResourcesActivity.class);
                startActivity(intent);
            });
        }

        return root;
    }

    private void updateLanguagePairDisplay() {
        if (tvLanguagePairDisplay == null) return;

        String mtLabel;
        if (currentMotherTongue.contains("Mundari")) {
            mtLabel = "मुंडारी (Mundari)";
        } else if (currentMotherTongue.contains("Ho")) {
            mtLabel = "हो (Ho)";
        } else {
            mtLabel = "संथाली (Santali)";
        }

        if (isHindiToMotherTongue) {
            tvLanguagePairDisplay.setText("हिंदी (Hindi) ➔ " + mtLabel);
            if (tvLanguageDirectionDetails != null) {
                tvLanguageDirectionDetails.setText("इनपुट: हिंदी (Teacher)  •  आउटपुट: " + mtLabel + " (Student)");
            }
        } else {
            tvLanguagePairDisplay.setText(mtLabel + " ➔ हिंदी (Hindi)");
            if (tvLanguageDirectionDetails != null) {
                tvLanguageDirectionDetails.setText("इनपुट: " + mtLabel + " (Teacher)  •  आउटपुट: हिंदी (Student)");
            }
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
