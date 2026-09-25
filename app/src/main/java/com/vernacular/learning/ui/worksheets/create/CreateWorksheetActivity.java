package com.vernacular.learning.ui.worksheets.create;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.study.StudyModels.*;
import com.vernacular.learning.data.repository.StudyMaterialRepository;
import com.vernacular.learning.ui.worksheets.detail.WorksheetDetailActivity;
import com.vernacular.learning.utils.PreferenceHelper;
import com.vernacular.learning.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class CreateWorksheetActivity extends AppCompatActivity {

    private ChipGroup cgCreateClass;
    private ChipGroup cgCreateSubject;
    private Spinner spnCreateTopic;
    private ChipGroup cgCreateDifficulty;
    private ChipGroup cgCreateCount;
    private TextView tvActiveLanguagePrompt;
    private MaterialButton btnGenerateWorksheet;

    private String selectedClass = "Class 3";
    private String selectedSubject = "EVS";
    private String selectedTopic = "";
    private String selectedDifficulty = "Easy";
    private int selectedCount = 5;

    private StudyMaterialRepository repository;
    private final List<String> topicList = new ArrayList<>();
    private ArrayAdapter<String> topicAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_worksheet);

        repository = StudyMaterialRepository.getInstance(this);

        initViews();
        setupListeners();
        updateTopicsDropdown();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        cgCreateClass = findViewById(R.id.cgCreateClass);
        cgCreateSubject = findViewById(R.id.cgCreateSubject);
        spnCreateTopic = findViewById(R.id.spnCreateTopic);
        cgCreateDifficulty = findViewById(R.id.cgCreateDifficulty);
        cgCreateCount = findViewById(R.id.cgCreateCount);
        tvActiveLanguagePrompt = findViewById(R.id.tvActiveLanguagePrompt);
        btnGenerateWorksheet = findViewById(R.id.btnGenerateWorksheet);

        btnBack.setOnClickListener(v -> finish());

        String motherTongue = PreferenceHelper.getSelectedMotherTongue(this);
        tvActiveLanguagePrompt.setText("Hindi ➔ " + motherTongue);

        topicAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, topicList);
        spnCreateTopic.setAdapter(topicAdapter);

        spnCreateTopic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < topicList.size()) {
                    selectedTopic = topicList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupListeners() {
        cgCreateClass.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCreateClass1) {
                selectedClass = "Class 1";
            } else if (checkedId == R.id.chipCreateClass2) {
                selectedClass = "Class 2";
            } else {
                selectedClass = "Class 3";
            }
            updateTopicsDropdown();
        });

        cgCreateSubject.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCreateSubjectMath) {
                selectedSubject = "Math";
            } else if (checkedId == R.id.chipCreateSubjectHindi) {
                selectedSubject = "Hindi";
            } else {
                selectedSubject = "EVS";
            }
            updateTopicsDropdown();
        });

        cgCreateDifficulty.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDiffMedium) {
                selectedDifficulty = "Medium";
            } else if (checkedId == R.id.chipDiffHard) {
                selectedDifficulty = "Hard";
            } else {
                selectedDifficulty = "Easy";
            }
        });

        cgCreateCount.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCount10) {
                selectedCount = 10;
            } else {
                selectedCount = 5;
            }
        });

        btnGenerateWorksheet.setOnClickListener(v -> generateAndLaunchWorksheet());
    }

    private void updateTopicsDropdown() {
        topicList.clear();
        List<CurriculumChapter> chapters = repository.getChaptersForSubject(selectedClass, selectedSubject);
        if (chapters != null && !chapters.isEmpty()) {
            for (CurriculumChapter ch : chapters) {
                topicList.add(ch.title);
            }
        } else {
            topicList.add("General Lesson Review");
        }
        topicAdapter.notifyDataSetChanged();
        if (!topicList.isEmpty()) {
            spnCreateTopic.setSelection(0);
            selectedTopic = topicList.get(0);
        }
    }

    private void generateAndLaunchWorksheet() {
        if (selectedTopic == null || selectedTopic.trim().isEmpty()) {
            Toast.makeText(this, "Please select a topic.", Toast.LENGTH_SHORT).show();
            return;
        }

        String motherTongue = PreferenceHelper.getSelectedMotherTongue(this);
        WorksheetData generated = repository.generateWorksheet(
                selectedClass, selectedSubject, selectedTopic, selectedDifficulty, selectedCount, motherTongue
        );

        if (generated.questions == null || generated.questions.isEmpty()) {
            Toast.makeText(this, "No questions are available for this topic yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, WorksheetDetailActivity.class);
        intent.putExtra("WORKSHEET_DATA", generated);
        startActivity(intent);
        finish();
    }
}
