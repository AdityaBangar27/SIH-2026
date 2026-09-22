package com.vernacular.learning.ui.flashcards;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.local.entities.VocabularyEntity;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.utils.AudioHelper;
import com.vernacular.learning.utils.PreferenceHelper;
import com.vernacular.learning.utils.ThemeHelper;
import java.util.ArrayList;
import java.util.List;

public class FlashcardActivity extends AppCompatActivity {
    private final List<VocabularyEntity> vocabularyList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isTranslationRevealed = false;

    private MaterialCardView cardMain;
    private LinearLayout flashcardControls;
    private LinearLayout layoutEmptyFlashcards;
    private ImageView ivVisual;
    private TextView tvHindi;
    private TextView tvTransliteration;
    private TextView tvEnglish;
    private TextView tvMotherTongue;
    private TextView tvCardCounter;
    private TextView tvTapHint;
    private ImageView btnPrev;
    private ImageView btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        ImageView btnBack = findViewById(R.id.btnFlashcardBack);
        cardMain = findViewById(R.id.cardFlashcardMain);
        flashcardControls = findViewById(R.id.flashcardControls);
        layoutEmptyFlashcards = findViewById(R.id.layoutEmptyFlashcards);
        ivVisual = findViewById(R.id.ivFlashcardVisual);
        tvHindi = findViewById(R.id.tvFlashcardHindi);
        tvTransliteration = findViewById(R.id.tvFlashcardTransliteration);
        tvEnglish = findViewById(R.id.tvFlashcardEnglish);
        tvMotherTongue = findViewById(R.id.tvFlashcardMotherTongue);
        ImageView btnAudio = findViewById(R.id.btnFlashcardAudio);
        tvCardCounter = findViewById(R.id.tvCardCounter);
        tvTapHint = findViewById(R.id.tvTapHint);
        btnPrev = findViewById(R.id.btnPrevCard);
        btnNext = findViewById(R.id.btnNextCard);

        btnBack.setOnClickListener(v -> finish());

        // Tap to reveal interaction
        cardMain.setOnClickListener(v -> {
            isTranslationRevealed = !isTranslationRevealed;
            tvMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
            tvTapHint.setText(isTranslationRevealed ? "Tap again to hide" : getString(R.string.tap_to_reveal));
        });

        // Pronunciation button
        btnAudio.setOnClickListener(v -> {
            if (!vocabularyList.isEmpty() && currentIndex < vocabularyList.size()) {
                VocabularyEntity card = vocabularyList.get(currentIndex);
                AudioHelper.playPronunciation(FlashcardActivity.this, card.word, null);
            }
        });

        // Previous Card
        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                isTranslationRevealed = false;
                displayCard();
            }
        });

        // Next Card
        btnNext.setOnClickListener(v -> {
            if (currentIndex < vocabularyList.size() - 1) {
                currentIndex++;
                isTranslationRevealed = false;
                displayCard();
            }
        });

        loadVocabularyFromDatabase();
    }

    private void loadVocabularyFromDatabase() {
        String motherTongue = PreferenceHelper.getSelectedMotherTongue(this);
        LearningRepository.getInstance(this).getAllVocabularyLiveData().observe(this, list -> {
            vocabularyList.clear();
            if (list != null && !list.isEmpty()) {
                vocabularyList.addAll(list);
                currentIndex = 0;
                cardMain.setVisibility(View.VISIBLE);
                flashcardControls.setVisibility(View.VISIBLE);
                if (layoutEmptyFlashcards != null) layoutEmptyFlashcards.setVisibility(View.GONE);
                displayCard();
            } else {
                // Empty state when database has zero preloaded vocabulary
                cardMain.setVisibility(View.GONE);
                flashcardControls.setVisibility(View.GONE);
                if (layoutEmptyFlashcards != null) layoutEmptyFlashcards.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayCard() {
        if (vocabularyList.isEmpty() || currentIndex >= vocabularyList.size()) {
            cardMain.setVisibility(View.GONE);
            flashcardControls.setVisibility(View.GONE);
            if (layoutEmptyFlashcards != null) layoutEmptyFlashcards.setVisibility(View.VISIBLE);
            return;
        }

        VocabularyEntity card = vocabularyList.get(currentIndex);
        ivVisual.setImageResource(R.drawable.ic_language);
        tvHindi.setText(card.word != null ? card.word : "");
        tvTransliteration.setText(card.transliteration != null ? card.transliteration : "");
        tvEnglish.setText(card.meaning != null ? card.meaning : "");
        tvMotherTongue.setText(card.meaning != null ? card.meaning : "");
        tvMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
        tvTapHint.setText(getString(R.string.tap_to_reveal));
        tvCardCounter.setText((currentIndex + 1) + " / " + vocabularyList.size());
    }
}
