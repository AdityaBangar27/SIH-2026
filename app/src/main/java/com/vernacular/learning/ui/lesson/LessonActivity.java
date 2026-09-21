package com.vernacular.learning.ui.lesson;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.utils.AudioHelper;

public class LessonActivity extends AppCompatActivity {
    private int currentStep = 1;
    private final int totalSteps = 5;

    private TextView tvLessonSubjectHeader;
    private TextView tvLessonChapter;
    private TextView tvLessonProgressBadge;
    private TextView tvLessonNumbers;
    private TextView tvLessonHindi;
    private TextView tvLessonVernacular;
    private TextView tvLessonInstruction;
    private MaterialButton btnPrevious;
    private MaterialButton btnNext;

    private String selectedMotherTongue = "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.vernacular.learning.utils.ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);

        String className = getIntent().getStringExtra("CLASS_NAME");
        String subjectName = getIntent().getStringExtra("SUBJECT_NAME");
        String motherTongue = getIntent().getStringExtra("MOTHER_TONGUE");
        if (motherTongue != null) {
            selectedMotherTongue = motherTongue;
        }

        initViews(className, subjectName);
        updateLessonContent();
    }

    private void initViews(String className, String subjectName) {
        ImageView btnBack = findViewById(R.id.btnLessonBack);
        tvLessonSubjectHeader = findViewById(R.id.tvLessonSubjectHeader);
        tvLessonChapter = findViewById(R.id.tvLessonChapter);
        tvLessonProgressBadge = findViewById(R.id.tvLessonProgressBadge);
        tvLessonNumbers = findViewById(R.id.tvLessonNumbers);
        tvLessonHindi = findViewById(R.id.tvLessonHindi);
        tvLessonVernacular = findViewById(R.id.tvLessonVernacular);
        tvLessonInstruction = findViewById(R.id.tvLessonInstruction);
        MaterialCardView btnPlayAudio = findViewById(R.id.btnPlayLessonAudio);
        btnPrevious = findViewById(R.id.btnPreviousLesson);
        btnNext = findViewById(R.id.btnNextLesson);
        TextView tvBackToLessons = findViewById(R.id.tvBackToLessons);

        if (className != null && subjectName != null) {
            tvLessonSubjectHeader.setText(subjectName + " - " + className);
        }

        btnBack.setOnClickListener(v -> finish());
        tvBackToLessons.setOnClickListener(v -> finish());

        btnPrevious.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateLessonContent();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentStep < totalSteps) {
                currentStep++;
                updateLessonContent();
            } else {
                finish();
            }
        });

        btnPlayAudio.setOnClickListener(v -> {
            String textToPlay = tvLessonHindi.getText().toString();
            AudioHelper.playPronunciation(LessonActivity.this, textToPlay, null);
        });
    }

    private void updateLessonContent() {
        tvLessonProgressBadge.setText("Lesson " + currentStep + "/" + totalSteps);

        if (currentStep == 1) {
            tvLessonChapter.setText("1. Numbers and Counting");
            tvLessonNumbers.setText("1   2   3");
            tvLessonHindi.setText("एक, दो, तीन");
            if (selectedMotherTongue.contains("Mundari")) {
                tvLessonVernacular.setText("मियाद, बारिया, आपी (ᱢᱤᱭᱟᱫᱽ, ᱵᱟᱨᱤᱭᱟ, ᱟᱯᱤ)");
            } else if (selectedMotherTongue.contains("Ho")) {
                tvLessonVernacular.setText("मियद, बारिया, आपे (ᱢᱤᱭᱟᱫᱽ, ᱵᱟᱨᱤᱭᱟ, ᱟᱯᱮ)");
            } else {
                tvLessonVernacular.setText("ᱮᱠ, ᱫᱳ, ᱛᱤᱱ (ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ)");
            }
            tvLessonInstruction.setText("Let's count the objects. Count and say aloud.");
            btnPrevious.setEnabled(false);
            btnPrevious.setAlpha(0.5f);
            btnNext.setText(R.string.btn_next);
        } else if (currentStep == 2) {
            tvLessonChapter.setText("1. Numbers and Counting");
            tvLessonNumbers.setText("4   5   6");
            tvLessonHindi.setText("चार, पाँच, छह");
            if (selectedMotherTongue.contains("Mundari")) {
                tvLessonVernacular.setText("उपोनिया, मोड़ेया, तुरूइया");
            } else if (selectedMotherTongue.contains("Ho")) {
                tvLessonVernacular.setText("उपुन, मोड़े, तुरूय");
            } else {
                tvLessonVernacular.setText("ᱯᱩᱱ, ᱢᱚᱬᱮ, ᱛᱩᱨᱩᱭ (Pun, Mone, Turuy)");
            }
            tvLessonInstruction.setText("Great job! Now count four, five, and six.");
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
            btnNext.setText(R.string.btn_next);
        } else if (currentStep == 3) {
            tvLessonChapter.setText("1. Numbers and Counting");
            tvLessonNumbers.setText("7   8   9   10");
            tvLessonHindi.setText("सात, आठ, नौ, दस");
            if (selectedMotherTongue.contains("Mundari")) {
                tvLessonVernacular.setText("एरेया, इरलिया, आरेया, गेलिया");
            } else if (selectedMotherTongue.contains("Ho")) {
                tvLessonVernacular.setText("ऐया, इरल, आरे, गेल");
            } else {
                tvLessonVernacular.setText("ᱮᱭᱟᱭ, ᱤᱨᱟᱹᱞ, ᱟᱨᱮ, ᱜᱮᱞ (Eyay, Iral, Are, Gel)");
            }
            tvLessonInstruction.setText("Keep going! Count all the way up to ten.");
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
            btnNext.setText(R.string.btn_next);
        } else if (currentStep == 4) {
            tvLessonChapter.setText("2. Addition Fundamentals");
            tvLessonNumbers.setText("1 + 2 = 3");
            tvLessonHindi.setText("एक और दो मिलकर तीन");
            tvLessonVernacular.setText("ᱢᱤᱫ ᱟᱨ ᱵᱟᱨ ᱢᱮᱥᱟ ᱠᱟᱛᱮ ᱯᱮ");
            tvLessonInstruction.setText("Combining objects together makes addition!");
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
            btnNext.setText(R.string.btn_next);
        } else {
            tvLessonChapter.setText("3. Review & Mastery");
            tvLessonNumbers.setText("✓ 1 to 10");
            tvLessonHindi.setText("शाबाश! आपने पाठ पूरा किया।");
            tvLessonVernacular.setText("ᱟᱹᱰᱤ ᱱᱟᱯᱟᱭ! ᱯᱟᱲᱦᱟᱣ ᱢᱩᱪᱟᱹᱫ ᱮᱱᱟ।");
            tvLessonInstruction.setText("You have mastered counting in your mother tongue!");
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
            btnNext.setText("Complete Lesson ✓");
        }
    }
}
