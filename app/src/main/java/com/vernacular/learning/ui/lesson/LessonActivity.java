package com.vernacular.learning.ui.lesson;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.local.entities.LessonEntity;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.utils.AudioHelper;
import com.vernacular.learning.utils.PreferenceHelper;
import com.vernacular.learning.utils.ThemeHelper;
import java.util.ArrayList;
import java.util.List;

public class LessonActivity extends AppCompatActivity {
    private int currentStepIndex = 0;
    private final List<LessonEntity> lessonList = new ArrayList<>();

    private TextView tvLessonSubjectHeader;
    private TextView tvLessonChapter;
    private TextView tvLessonProgressBadge;
    private TextView tvLessonNumbers;
    private TextView tvLessonHindi;
    private TextView tvLessonVernacular;
    private TextView tvLessonInstruction;
    private ImageView ivLessonVisual;
    private MaterialButton btnPrevious;
    private MaterialButton btnNext;
    private ScrollView scrollLessonContent;
    private LinearLayout layoutEmptyLessons;

    private String currentClassName = "Class 1";
    private String currentSubjectName = "Mathematics";
    private String selectedMotherTongue = "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);

        String className = getIntent().getStringExtra("CLASS_NAME");
        if (className != null && !className.isEmpty()) {
            currentClassName = className;
        } else {
            currentClassName = PreferenceHelper.getSelectedClass(this);
        }

        String subjectName = getIntent().getStringExtra("SUBJECT_NAME");
        if (subjectName != null && !subjectName.isEmpty()) {
            currentSubjectName = subjectName;
        } else {
            currentSubjectName = PreferenceHelper.getSelectedSubject(this);
        }

        String motherTongue = getIntent().getStringExtra("MOTHER_TONGUE");
        if (motherTongue != null && !motherTongue.isEmpty()) {
            selectedMotherTongue = motherTongue;
        } else {
            selectedMotherTongue = PreferenceHelper.getSelectedMotherTongue(this);
        }

        initViews();
        loadLessonsFromDatabase();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnLessonBack);
        tvLessonSubjectHeader = findViewById(R.id.tvLessonSubjectHeader);
        tvLessonChapter = findViewById(R.id.tvLessonChapter);
        tvLessonProgressBadge = findViewById(R.id.tvLessonProgressBadge);
        tvLessonNumbers = findViewById(R.id.tvLessonNumbers);
        tvLessonHindi = findViewById(R.id.tvLessonHindi);
        tvLessonVernacular = findViewById(R.id.tvLessonVernacular);
        tvLessonInstruction = findViewById(R.id.tvLessonInstruction);
        ivLessonVisual = findViewById(R.id.ivLessonVisual);
        MaterialCardView btnPlayAudio = findViewById(R.id.btnPlayLessonAudio);
        btnPrevious = findViewById(R.id.btnPreviousLesson);
        btnNext = findViewById(R.id.btnNextLesson);
        TextView tvBackToLessons = findViewById(R.id.tvBackToLessons);
        scrollLessonContent = findViewById(R.id.scrollLessonContent);
        layoutEmptyLessons = findViewById(R.id.layoutEmptyLessons);

        tvLessonSubjectHeader.setText(currentSubjectName + " - " + currentClassName);

        btnBack.setOnClickListener(v -> finish());
        tvBackToLessons.setOnClickListener(v -> finish());

        btnPrevious.setOnClickListener(v -> {
            if (currentStepIndex > 0) {
                currentStepIndex--;
                displayCurrentLesson();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentStepIndex < lessonList.size() - 1) {
                currentStepIndex++;
                displayCurrentLesson();
            } else {
                finish();
            }
        });

        btnPlayAudio.setOnClickListener(v -> {
            String textToPlay = tvLessonHindi.getText().toString();
            AudioHelper.playPronunciation(LessonActivity.this, textToPlay, null);
        });
    }

    private void loadLessonsFromDatabase() {
        LearningRepository.getInstance(this).getLessonsLiveData(
                currentClassName, currentSubjectName, selectedMotherTongue
        ).observe(this, lessons -> {
            lessonList.clear();
            if (lessons != null && !lessons.isEmpty()) {
                lessonList.addAll(lessons);
                currentStepIndex = 0;
                scrollLessonContent.setVisibility(View.VISIBLE);
                layoutEmptyLessons.setVisibility(View.GONE);
                displayCurrentLesson();
            } else {
                // Display empty state cleanly without throwing an error
                scrollLessonContent.setVisibility(View.GONE);
                layoutEmptyLessons.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayCurrentLesson() {
        if (lessonList.isEmpty()) {
            scrollLessonContent.setVisibility(View.GONE);
            layoutEmptyLessons.setVisibility(View.VISIBLE);
            return;
        }

        LessonEntity current = lessonList.get(currentStepIndex);
        int total = lessonList.size();
        tvLessonProgressBadge.setText("Lesson " + (currentStepIndex + 1) + "/" + total);

        String chapterStr = (current.chapterNumber != null && !current.chapterNumber.isEmpty()) ?
                current.chapterNumber + ". " + (current.title != null ? current.title : "") :
                (current.title != null ? current.title : "");
        tvLessonChapter.setText(chapterStr);

        tvLessonHindi.setText(current.hindiContent != null ? current.hindiContent : "");
        tvLessonVernacular.setText(current.motherTongueContent != null ? current.motherTongueContent : "");
        tvLessonInstruction.setText(current.instruction != null ? current.instruction : "");

        if (current.contentReference != null && !current.contentReference.isEmpty()) {
            tvLessonNumbers.setText(current.contentReference);
            tvLessonNumbers.setVisibility(View.VISIBLE);
        } else {
            tvLessonNumbers.setVisibility(View.GONE);
        }

        // Previous button state
        if (currentStepIndex == 0) {
            btnPrevious.setEnabled(false);
            btnPrevious.setAlpha(0.5f);
        } else {
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
        }

        // Next button state
        if (currentStepIndex == total - 1) {
            btnNext.setText(R.string.downloaded_success_banner);
        } else {
            btnNext.setText(R.string.btn_next);
        }
    }
}
