package com.vernacular.learning.ui.flashcards;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.FlashcardItem;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.utils.AudioHelper;
import java.util.List;

public class FlashcardActivity extends AppCompatActivity {
    private List<FlashcardItem> flashcards;
    private int currentIndex = 0;
    private boolean isTranslationRevealed = false;

    private ImageView ivVisual;
    private TextView tvHindi;
    private TextView tvTransliteration;
    private TextView tvEnglish;
    private TextView tvMotherTongue;
    private TextView tvCardCounter;
    private TextView tvTapHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.vernacular.learning.utils.ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        flashcards = LearningRepository.getInstance(this).getSampleFlashcards();

        ImageView btnBack = findViewById(R.id.btnFlashcardBack);
        MaterialCardView cardMain = findViewById(R.id.cardFlashcardMain);
        ivVisual = findViewById(R.id.ivFlashcardVisual);
        tvHindi = findViewById(R.id.tvFlashcardHindi);
        tvTransliteration = findViewById(R.id.tvFlashcardTransliteration);
        tvEnglish = findViewById(R.id.tvFlashcardEnglish);
        tvMotherTongue = findViewById(R.id.tvFlashcardMotherTongue);
        ImageView btnAudio = findViewById(R.id.btnFlashcardAudio);
        tvCardCounter = findViewById(R.id.tvCardCounter);
        tvTapHint = findViewById(R.id.tvTapHint);
        ImageView btnPrev = findViewById(R.id.btnPrevCard);
        ImageView btnNext = findViewById(R.id.btnNextCard);

        btnBack.setOnClickListener(v -> finish());

        // Tap to reveal interaction
        cardMain.setOnClickListener(v -> {
            isTranslationRevealed = !isTranslationRevealed;
            tvMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
            tvTapHint.setText(isTranslationRevealed ? "Tap again to hide" : getString(R.string.tap_to_reveal));
        });

        // Pronunciation button
        btnAudio.setOnClickListener(v -> {
            FlashcardItem card = flashcards.get(currentIndex);
            AudioHelper.playPronunciation(FlashcardActivity.this, card.hindiWord, null);
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
            if (currentIndex < flashcards.size() - 1) {
                currentIndex++;
                isTranslationRevealed = false;
                displayCard();
            }
        });

        displayCard();
    }

    private void displayCard() {
        if (flashcards == null || flashcards.isEmpty()) return;

        FlashcardItem card = flashcards.get(currentIndex);
        ivVisual.setImageResource(card.imageResId);
        tvHindi.setText(card.hindiWord);
        tvTransliteration.setText(card.transliteration);
        tvEnglish.setText(card.englishWord);
        tvMotherTongue.setText(card.santhaliWord);
        tvMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
        tvTapHint.setText(getString(R.string.tap_to_reveal));
        tvCardCounter.setText((currentIndex + 1) + " / " + flashcards.size());
    }
}
