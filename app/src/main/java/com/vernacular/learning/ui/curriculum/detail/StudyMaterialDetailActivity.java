package com.vernacular.learning.ui.curriculum.detail;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.study.StudyModels.*;
import com.vernacular.learning.data.repository.StudyMaterialRepository;
import com.vernacular.learning.ui.flashcards.FlashcardActivity;
import com.vernacular.learning.ui.worksheets.detail.WorksheetDetailActivity;
import com.vernacular.learning.utils.ThemeHelper;

public class StudyMaterialDetailActivity extends AppCompatActivity {

    private String className = "Class 3";
    private String subjectName = "EVS";
    private String topicName = "Plants Around Us";
    private CurriculumChapter chapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_material_detail);

        if (getIntent() != null) {
            if (getIntent().hasExtra("CLASS_NAME")) className = getIntent().getStringExtra("CLASS_NAME");
            if (getIntent().hasExtra("SUBJECT_NAME")) subjectName = getIntent().getStringExtra("SUBJECT_NAME");
            if (getIntent().hasExtra("TOPIC_NAME")) topicName = getIntent().getStringExtra("TOPIC_NAME");
        }

        chapter = StudyMaterialRepository.getInstance(this).getChapter(className, subjectName, topicName);

        initViews();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvHeaderBadge = findViewById(R.id.tvHeaderBadge);
        TextView tvTopicTitle = findViewById(R.id.tvTopicTitle);
        TextView tvTopicSubtitle = findViewById(R.id.tvTopicSubtitle);
        TextView tvLearningObjective = findViewById(R.id.tvLearningObjective);
        TextView tvExplanation = findViewById(R.id.tvExplanation);

        LinearLayout layoutKeyConcepts = findViewById(R.id.layoutKeyConceptsContainer);
        LinearLayout layoutKeyPoints = findViewById(R.id.layoutKeyPointsContainer);
        LinearLayout layoutExamples = findViewById(R.id.layoutExamplesContainer);
        LinearLayout layoutVocabulary = findViewById(R.id.layoutVocabularyContainer);
        LinearLayout layoutQuickCheck = findViewById(R.id.layoutQuickCheckContainer);

        MaterialButton btnQuickWorksheet = findViewById(R.id.btnQuickWorksheet);
        MaterialButton btnQuickFlashcards = findViewById(R.id.btnQuickFlashcards);
        MaterialButton btnViewWorksheetBottom = findViewById(R.id.btnViewWorksheetBottom);
        MaterialButton btnPracticeFlashcardsBottom = findViewById(R.id.btnPracticeFlashcardsBottom);

        btnBack.setOnClickListener(v -> finish());

        if (chapter != null) {
            tvHeaderBadge.setText(className + " • " + subjectName);
            tvTopicTitle.setText(chapter.title);
            tvTopicSubtitle.setText("Chapter " + chapter.chapterNumber + " • " + subjectName + " • " + className);
            tvLearningObjective.setText(chapter.learningObjective != null ? chapter.learningObjective : "Understand essential concepts of this chapter.");
            tvExplanation.setText(chapter.explanation != null ? chapter.explanation : "");

            // 1. Key Concepts
            layoutKeyConcepts.removeAllViews();
            if (chapter.studyMaterial != null) {
                for (StudyPart part : chapter.studyMaterial) {
                    LinearLayout itemLayout = new LinearLayout(this);
                    itemLayout.setOrientation(LinearLayout.VERTICAL);
                    itemLayout.setPadding(0, 8, 0, 8);

                    TextView tvPart = new TextView(this);
                    tvPart.setText("• " + part.part);
                    tvPart.setTextSize(14f);
                    tvPart.setTypeface(null, Typeface.BOLD);
                    tvPart.setTextColor(ContextCompat.getColor(this, R.color.primary_forest_green));

                    TextView tvDesc = new TextView(this);
                    tvDesc.setText(part.desc);
                    tvDesc.setTextSize(13f);
                    tvDesc.setPadding(20, 2, 0, 0);

                    itemLayout.addView(tvPart);
                    itemLayout.addView(tvDesc);
                    layoutKeyConcepts.addView(itemLayout);
                }
            }

            // 2. Key Points
            layoutKeyPoints.removeAllViews();
            if (chapter.keyPoints != null) {
                for (String kp : chapter.keyPoints) {
                    TextView tv = new TextView(this);
                    tv.setText("✔  " + kp);
                    tv.setTextSize(13f);
                    tv.setPadding(0, 4, 0, 4);
                    layoutKeyPoints.addView(tv);
                }
            }

            // 3. Examples
            layoutExamples.removeAllViews();
            if (chapter.examples != null) {
                for (String ex : chapter.examples) {
                    TextView tv = new TextView(this);
                    tv.setText("📌 " + ex);
                    tv.setTextSize(13f);
                    tv.setPadding(0, 4, 0, 4);
                    layoutExamples.addView(tv);
                }
            }

            // 4. Vocabulary
            layoutVocabulary.removeAllViews();
            if (chapter.vocabulary != null) {
                for (VocabularyItem voc : chapter.vocabulary) {
                    LinearLayout vRow = new LinearLayout(this);
                    vRow.setOrientation(LinearLayout.HORIZONTAL);
                    vRow.setPadding(0, 6, 0, 6);

                    TextView tvWord = new TextView(this);
                    tvWord.setText(voc.word + "  ➔  " + voc.hindi + " / " + voc.santali);
                    tvWord.setTextSize(13f);
                    tvWord.setTypeface(null, Typeface.BOLD);
                    tvWord.setTextColor(ContextCompat.getColor(this, R.color.primary_forest_green));

                    vRow.addView(tvWord);
                    layoutVocabulary.addView(vRow);

                    if (voc.meaning != null && !voc.meaning.isEmpty()) {
                        TextView tvMeaning = new TextView(this);
                        tvMeaning.setText("   " + voc.meaning);
                        tvMeaning.setTextSize(12f);
                        layoutVocabulary.addView(tvMeaning);
                    }
                }
            }

            // 5. Quick Check Questions
            layoutQuickCheck.removeAllViews();
            if (chapter.questions != null) {
                int qNum = 1;
                for (StudyQuestion q : chapter.questions) {
                    if (qNum > 3) break; // Display top 3 quick checks
                    LinearLayout qLayout = new LinearLayout(this);
                    qLayout.setOrientation(LinearLayout.VERTICAL);
                    qLayout.setPadding(0, 8, 0, 8);

                    TextView tvQ = new TextView(this);
                    tvQ.setText(qNum + ". " + q.text);
                    tvQ.setTextSize(13f);
                    tvQ.setTypeface(null, Typeface.BOLD);

                    TextView tvAnswer = new TextView(this);
                    tvAnswer.setText("Answer: " + q.correctAnswer + " (" + q.explanation + ")");
                    tvAnswer.setTextSize(12f);
                    tvAnswer.setPadding(16, 4, 0, 0);
                    tvAnswer.setTextColor(ContextCompat.getColor(this, R.color.primary_forest_green));
                    tvAnswer.setVisibility(View.GONE);

                    TextView btnReveal = new TextView(this);
                    btnReveal.setText("Show Answer ▼");
                    btnReveal.setTextSize(11f);
                    btnReveal.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_light));
                    btnReveal.setPadding(16, 2, 0, 0);

                    btnReveal.setOnClickListener(v -> {
                        if (tvAnswer.getVisibility() == View.VISIBLE) {
                            tvAnswer.setVisibility(View.GONE);
                            btnReveal.setText("Show Answer ▼");
                        } else {
                            tvAnswer.setVisibility(View.VISIBLE);
                            btnReveal.setText("Hide Answer ▲");
                        }
                    });

                    qLayout.addView(tvQ);
                    qLayout.addView(btnReveal);
                    qLayout.addView(tvAnswer);
                    layoutQuickCheck.addView(qLayout);
                    qNum++;
                }
            }
        }

        // Action Handlers
        View.OnClickListener worksheetAction = v -> {
            Intent intent = new Intent(StudyMaterialDetailActivity.this, WorksheetDetailActivity.class);
            intent.putExtra("CLASS_NAME", className);
            intent.putExtra("SUBJECT_NAME", subjectName);
            intent.putExtra("TOPIC_NAME", topicName);
            startActivity(intent);
        };

        View.OnClickListener flashcardsAction = v -> {
            Intent intent = new Intent(StudyMaterialDetailActivity.this, FlashcardActivity.class);
            intent.putExtra("CLASS_NAME", className);
            intent.putExtra("SUBJECT_NAME", subjectName);
            intent.putExtra("TOPIC_NAME", topicName);
            startActivity(intent);
        };

        btnQuickWorksheet.setOnClickListener(worksheetAction);
        btnViewWorksheetBottom.setOnClickListener(worksheetAction);

        btnQuickFlashcards.setOnClickListener(flashcardsAction);
        btnPracticeFlashcardsBottom.setOnClickListener(flashcardsAction);
    }
}
