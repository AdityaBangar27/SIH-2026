package com.vernacular.learning.ui.worksheets.detail;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.study.StudyModels.*;
import com.vernacular.learning.data.repository.StudyMaterialRepository;
import com.vernacular.learning.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorksheetDetailActivity extends AppCompatActivity {

    private WorksheetData worksheetData;
    private StudyMaterialRepository repository;

    private TextView tvWorksheetTopTitle;
    private TextView tvDifficultyBadge;
    private TextView tvWorksheetHeading;
    private TextView tvWorksheetMetadata;
    private LinearLayout layoutScoreBanner;
    private TextView tvScoreResult;
    private TextView tvScoreFeedback;
    private LinearLayout layoutQuestionsContainer;

    private MaterialButton btnCheckAnswers;
    private MaterialButton btnSaveWorksheet;
    private MaterialButton btnResetWorksheet;
    private MaterialButton btnDeleteWorksheet;

    // Track user input views
    private final Map<Integer, RadioGroup> mcqViews = new HashMap<>();
    private final Map<Integer, EditText> fillViews = new HashMap<>();
    private final Map<Integer, TextView> explanationViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worksheet_detail);

        repository = StudyMaterialRepository.getInstance(this);

        initViews();
        loadWorksheetData();
        renderQuestions();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        tvWorksheetTopTitle = findViewById(R.id.tvWorksheetTopTitle);
        tvDifficultyBadge = findViewById(R.id.tvDifficultyBadge);
        tvWorksheetHeading = findViewById(R.id.tvWorksheetHeading);
        tvWorksheetMetadata = findViewById(R.id.tvWorksheetMetadata);
        layoutScoreBanner = findViewById(R.id.layoutScoreBanner);
        tvScoreResult = findViewById(R.id.tvScoreResult);
        tvScoreFeedback = findViewById(R.id.tvScoreFeedback);
        layoutQuestionsContainer = findViewById(R.id.layoutQuestionsContainer);

        btnCheckAnswers = findViewById(R.id.btnCheckAnswers);
        btnSaveWorksheet = findViewById(R.id.btnSaveWorksheet);
        btnResetWorksheet = findViewById(R.id.btnResetWorksheet);
        btnDeleteWorksheet = findViewById(R.id.btnDeleteWorksheet);

        btnBack.setOnClickListener(v -> finish());
        btnCheckAnswers.setOnClickListener(v -> checkAnswers());
        btnSaveWorksheet.setOnClickListener(v -> saveWorksheet());
        btnResetWorksheet.setOnClickListener(v -> resetWorksheet());
        btnDeleteWorksheet.setOnClickListener(v -> confirmDeleteWorksheet());
    }

    private void loadWorksheetData() {
        if (getIntent() != null) {
            if (getIntent().hasExtra("WORKSHEET_DATA")) {
                worksheetData = (WorksheetData) getIntent().getSerializableExtra("WORKSHEET_DATA");
            } else if (getIntent().hasExtra("WORKSHEET_ID")) {
                String id = getIntent().getStringExtra("WORKSHEET_ID");
                worksheetData = repository.getWorksheetById(id);
            } else {
                String className = getIntent().getStringExtra("CLASS_NAME");
                String subjectName = getIntent().getStringExtra("SUBJECT_NAME");
                String topicName = getIntent().getStringExtra("TOPIC_NAME");
                if (className == null) className = "Class 3";
                if (subjectName == null) subjectName = "EVS";
                if (topicName == null) topicName = "Plants Around Us";

                worksheetData = repository.generateWorksheet(
                        className, subjectName, topicName, "Easy", 5, "Hindi"
                );
            }
        }

        if (worksheetData == null) {
            worksheetData = repository.generateWorksheet(
                    "Class 3", "EVS", "Plants Around Us", "Easy", 5, "Hindi"
            );
        }

        tvWorksheetTopTitle.setText(worksheetData.title);
        tvDifficultyBadge.setText(worksheetData.difficulty != null ? worksheetData.difficulty : "मानक");
        tvWorksheetHeading.setText(worksheetData.title);
        tvWorksheetMetadata.setText(worksheetData.className + " • " + worksheetData.subject + " • " +
                worksheetData.questions.size() + " प्रश्न");
    }

    private void renderQuestions() {
        layoutQuestionsContainer.removeAllViews();
        mcqViews.clear();
        fillViews.clear();
        explanationViews.clear();

        List<StudyQuestion> questions = worksheetData.questions;
        if (questions == null || questions.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("इस अभ्यास पत्रक में कोई प्रश्न उपलब्ध नहीं है।");
            emptyTv.setPadding(16, 24, 16, 24);
            layoutQuestionsContainer.addView(emptyTv);
            return;
        }

        for (int i = 0; i < questions.size(); i++) {
            StudyQuestion q = questions.get(i);
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(14));
            card.setCardElevation(dpToPx(1));
            card.setStrokeColor(ContextCompat.getColor(this, R.color.border_light));
            card.setStrokeWidth(dpToPx(1));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg_light));

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
            card.setLayoutParams(cardParams);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));

            // Question Header
            TextView tvQText = new TextView(this);
            tvQText.setText((i + 1) + ".  " + q.text);
            tvQText.setTextSize(14f);
            tvQText.setTypeface(null, Typeface.BOLD);
            tvQText.setTextColor(ContextCompat.getColor(this, R.color.text_primary_light));
            content.addView(tvQText);

            // Question Type Inputs
            if ("MCQ".equalsIgnoreCase(q.type) || "TRUE_FALSE".equalsIgnoreCase(q.type)) {
                RadioGroup rg = new RadioGroup(this);
                rg.setOrientation(RadioGroup.VERTICAL);
                rg.setPadding(0, dpToPx(8), 0, dpToPx(4));

                List<String> options = q.options;
                if ("TRUE_FALSE".equalsIgnoreCase(q.type) && (options == null || options.isEmpty())) {
                    options = new ArrayList<>();
                    options.add("सत्य");
                    options.add("असत्य");
                }

                if (options != null) {
                    for (int optIdx = 0; optIdx < options.size(); optIdx++) {
                        String opt = options.get(optIdx);
                        RadioButton rb = new RadioButton(this);
                        rb.setText(opt);
                        rb.setTextSize(13f);
                        rb.setId(View.generateViewId());
                        rg.addView(rb);
                    }
                }
                mcqViews.put(i, rg);
                content.addView(rg);
            } else if ("FILL".equalsIgnoreCase(q.type)) {
                EditText et = new EditText(this);
                et.setHint("यहाँ अपना उत्तर लिखें...");
                et.setTextSize(13f);
                et.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
                et.setBackgroundResource(R.drawable.bg_tab_unselected);
                LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44)
                );
                etParams.setMargins(0, dpToPx(10), 0, dpToPx(4));
                et.setLayoutParams(etParams);

                fillViews.put(i, et);
                content.addView(et);
            }

            // Explanation / Answer Reveal container
            TextView tvExpl = new TextView(this);
            tvExpl.setTextSize(12f);
            tvExpl.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
            tvExpl.setVisibility(View.GONE);
            explanationViews.put(i, tvExpl);
            content.addView(tvExpl);

            card.addView(content);
            layoutQuestionsContainer.addView(card);
        }
    }

    private void checkAnswers() {
        List<StudyQuestion> questions = worksheetData.questions;
        if (questions == null || questions.isEmpty()) return;

        int correctCount = 0;

        for (int i = 0; i < questions.size(); i++) {
            StudyQuestion q = questions.get(i);
            boolean isCorrect = false;
            String userAnswer = "";

            if ("MCQ".equalsIgnoreCase(q.type) || "TRUE_FALSE".equalsIgnoreCase(q.type)) {
                RadioGroup rg = mcqViews.get(i);
                if (rg != null) {
                    int checkedId = rg.getCheckedRadioButtonId();
                    if (checkedId != -1) {
                        RadioButton rb = rg.findViewById(checkedId);
                        if (rb != null) {
                            userAnswer = rb.getText().toString().trim();
                        }
                    }
                }
            } else if ("FILL".equalsIgnoreCase(q.type)) {
                EditText et = fillViews.get(i);
                if (et != null) {
                    userAnswer = et.getText().toString().trim();
                }
            }

            if (userAnswer.equalsIgnoreCase(q.correctAnswer.trim())) {
                isCorrect = true;
                correctCount++;
            }

            TextView tvExpl = explanationViews.get(i);
            if (tvExpl != null) {
                tvExpl.setVisibility(View.VISIBLE);
                if (isCorrect) {
                    tvExpl.setText("✔ सही उत्तर!  " + q.explanation);
                    tvExpl.setTextColor(Color.parseColor("#1B5E20"));
                    tvExpl.setBackgroundColor(Color.parseColor("#E8F5E9"));
                } else {
                    tvExpl.setText("✘ सही उत्तर: " + q.correctAnswer + "\nव्याख्या: " + q.explanation);
                    tvExpl.setTextColor(Color.parseColor("#B71C1C"));
                    tvExpl.setBackgroundColor(Color.parseColor("#FFEBEE"));
                }
            }
        }

        // Display Score Banner
        layoutScoreBanner.setVisibility(View.VISIBLE);
        int total = questions.size();
        int percent = (int) (((float) correctCount / total) * 100);
        tvScoreResult.setText("अंक: " + correctCount + " / " + total + " (" + percent + "%)");

        if (percent >= 80) {
            tvScoreFeedback.setText("उत्कृष्ट कार्य! 🌟");
        } else if (percent >= 50) {
            tvScoreFeedback.setText("अच्छा प्रयास! 👍");
        } else {
            tvScoreFeedback.setText("पुनः अभ्यास करें 📚");
        }

        Toast.makeText(this, "अंक: " + correctCount + " / " + total, Toast.LENGTH_SHORT).show();
    }

    private void saveWorksheet() {
        if (worksheetData != null) {
            repository.saveWorksheet(worksheetData);
            Toast.makeText(this, "अभ्यास पत्रक सहेज लिया गया!", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetWorksheet() {
        layoutScoreBanner.setVisibility(View.GONE);
        for (RadioGroup rg : mcqViews.values()) {
            rg.clearCheck();
        }
        for (EditText et : fillViews.values()) {
            et.setText("");
        }
        for (TextView tv : explanationViews.values()) {
            tv.setVisibility(View.GONE);
        }
        Toast.makeText(this, "अभ्यास पत्रक रीसेट किया गया।", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteWorksheet() {
        new AlertDialog.Builder(this)
                .setTitle("अभ्यास पत्रक हटाएँ")
                .setMessage("क्या आप वाकई इस अभ्यास पत्रक को हटाना चाहते हैं?")
                .setPositiveButton("हटाएँ", (dialog, which) -> {
                    if (worksheetData != null) {
                        repository.deleteWorksheet(worksheetData.id);
                        Toast.makeText(this, "अभ्यास पत्रक हटा दिया गया।", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
