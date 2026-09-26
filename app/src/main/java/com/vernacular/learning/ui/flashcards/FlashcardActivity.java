package com.vernacular.learning.ui.flashcards;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.vernacular.learning.utils.AudioHelper;
import com.vernacular.learning.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class FlashcardActivity extends AppCompatActivity {

    private String currentClassName = "Class 3";
    private String currentSubjectName = "EVS";
    private String currentTopicName = "Plants Around Us";

    private final List<StudyFlashcard> currentDeck = new ArrayList<>();
    private final List<String> availableDeckTitles = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isBackShowing = false;

    private TextView tvDeckTitle;
    private Spinner spnSelectDeck;
    private MaterialButton btnCreateFlashcardDeck;
    private MaterialCardView cardMain;
    private TextView tvCardSideBadge;
    private ImageView btnAudio;
    private TextView tvFlashcardEmoji;
    private TextView tvFlashcardConcept;
    private TextView tvFlashcardMainText;
    private TextView tvFlashcardSubText;
    private TextView tvTapHint;
    private LinearLayout flashcardControls;
    private ImageView btnPrev;
    private ImageView btnNext;
    private TextView tvCardCounter;
    private TextView btnRestartDeck;
    private LinearLayout layoutEmptyFlashcards;

    private StudyMaterialRepository repository;
    private ArrayAdapter<String> deckSpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        repository = StudyMaterialRepository.getInstance(this);

        if (getIntent() != null) {
            if (getIntent().hasExtra("CLASS_NAME")) currentClassName = getIntent().getStringExtra("CLASS_NAME");
            if (getIntent().hasExtra("SUBJECT_NAME")) currentSubjectName = getIntent().getStringExtra("SUBJECT_NAME");
            if (getIntent().hasExtra("TOPIC_NAME")) currentTopicName = getIntent().getStringExtra("TOPIC_NAME");
        }

        initViews();
        setupDeckSpinner();
        loadDeck(currentClassName, currentSubjectName, currentTopicName, 10);
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnFlashcardBack);
        tvDeckTitle = findViewById(R.id.tvDeckTitle);
        spnSelectDeck = findViewById(R.id.spnSelectDeck);
        btnCreateFlashcardDeck = findViewById(R.id.btnCreateFlashcardDeck);
        cardMain = findViewById(R.id.cardFlashcardMain);
        tvCardSideBadge = findViewById(R.id.tvCardSideBadge);
        btnAudio = findViewById(R.id.btnFlashcardAudio);
        tvFlashcardEmoji = findViewById(R.id.tvFlashcardEmoji);
        tvFlashcardConcept = findViewById(R.id.tvFlashcardConcept);
        tvFlashcardMainText = findViewById(R.id.tvFlashcardMainText);
        tvFlashcardSubText = findViewById(R.id.tvFlashcardSubText);
        tvTapHint = findViewById(R.id.tvTapHint);
        flashcardControls = findViewById(R.id.flashcardControls);
        btnPrev = findViewById(R.id.btnPrevCard);
        btnNext = findViewById(R.id.btnNextCard);
        tvCardCounter = findViewById(R.id.tvCardCounter);
        btnRestartDeck = findViewById(R.id.btnRestartDeck);
        layoutEmptyFlashcards = findViewById(R.id.layoutEmptyFlashcards);

        btnBack.setOnClickListener(v -> finish());

        // Tap to flip
        cardMain.setOnClickListener(v -> {
            AudioHelper.stopPlayback();
            isBackShowing = !isBackShowing;
            updateCardDisplay();
        });

        // Prev
        btnPrev.setOnClickListener(v -> {
            AudioHelper.stopPlayback();
            if (currentIndex > 0) {
                currentIndex--;
                isBackShowing = false;
                updateCardDisplay();
            }
        });

        // Next
        btnNext.setOnClickListener(v -> {
            AudioHelper.stopPlayback();
            if (currentIndex < currentDeck.size() - 1) {
                currentIndex++;
                isBackShowing = false;
                updateCardDisplay();
            }
        });

        // Restart
        btnRestartDeck.setOnClickListener(v -> {
            AudioHelper.stopPlayback();
            currentIndex = 0;
            isBackShowing = false;
            updateCardDisplay();
            Toast.makeText(this, "डेक पुनः आरंभ किया गया", Toast.LENGTH_SHORT).show();
        });

        // Pronounce
        btnAudio.setOnClickListener(v -> {
            if (!currentDeck.isEmpty() && currentIndex < currentDeck.size()) {
                StudyFlashcard card = currentDeck.get(currentIndex);
                String textToRead = isBackShowing ? card.back : card.front;
                AudioHelper.playPronunciation(this, textToRead, null);
            }
        });

        // Create Deck
        btnCreateFlashcardDeck.setOnClickListener(v -> showCreateDeckDialog());
    }

    private void setupDeckSpinner() {
        availableDeckTitles.clear();
        availableDeckTitles.add("हमारे आसपास के पौधे (कक्षा 3 • EVS)");
        availableDeckTitles.add("हमारे आसपास के जानवर (कक्षा 3 • EVS)");
        availableDeckTitles.add("जल (कक्षा 3 • EVS)");
        availableDeckTitles.add("हमारा पर्यावरण (कक्षा 3 • EVS)");
        availableDeckTitles.add("जोड़ (कक्षा 3 • Math)");
        availableDeckTitles.add("घटाव (कक्षा 3 • Math)");
        availableDeckTitles.add("शब्द और वाक्य (कक्षा 3 • Hindi)");

        deckSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, availableDeckTitles);
        spnSelectDeck.setAdapter(deckSpinnerAdapter);

        // Select initial
        int selectedIndex = 0;
        for (int i = 0; i < availableDeckTitles.size(); i++) {
            if (availableDeckTitles.get(i).contains(currentTopicName)) {
                selectedIndex = i;
                break;
            }
        }
        spnSelectDeck.setSelection(selectedIndex, false);

        spnSelectDeck.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String chosen = availableDeckTitles.get(position);
                if (chosen.contains("पौधे") || chosen.contains("Plants")) {
                    loadDeck("Class 3", "EVS", "हमारे आसपास के पौधे", 10);
                } else if (chosen.contains("जानवर") || chosen.contains("Animals")) {
                    loadDeck("Class 3", "EVS", "हमारे आसपास के जानवर", 10);
                } else if (chosen.contains("जल") || chosen.contains("Water")) {
                    loadDeck("Class 3", "EVS", "जल", 10);
                } else if (chosen.contains("पर्यावरण") || chosen.contains("Environment")) {
                    loadDeck("Class 3", "EVS", "हमारा पर्यावरण", 10);
                } else if (chosen.contains("जोड़") || chosen.contains("Addition")) {
                    loadDeck("Class 3", "Math", "जोड़", 10);
                } else if (chosen.contains("घटाव") || chosen.contains("Subtraction")) {
                    loadDeck("Class 3", "Math", "घटाव", 10);
                } else if (chosen.contains("शब्द")) {
                    loadDeck("Class 3", "Hindi", "शब्द और वाक्य", 10);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDeck(String className, String subject, String topic, int count) {
        currentClassName = className;
        currentSubjectName = subject;
        currentTopicName = topic;
        tvDeckTitle.setText(topic + " • फ्लैशकार्ड");

        currentDeck.clear();
        List<StudyFlashcard> cards = repository.getFlashcardsForTopic(className, subject, topic, count);
        if (cards != null && !cards.isEmpty()) {
            currentDeck.addAll(cards);
            currentIndex = 0;
            isBackShowing = false;
            cardMain.setVisibility(View.VISIBLE);
            flashcardControls.setVisibility(View.VISIBLE);
            layoutEmptyFlashcards.setVisibility(View.GONE);
            updateCardDisplay();
        } else {
            cardMain.setVisibility(View.GONE);
            flashcardControls.setVisibility(View.GONE);
            layoutEmptyFlashcards.setVisibility(View.VISIBLE);
        }
    }

    private void updateCardDisplay() {
        if (currentDeck.isEmpty() || currentIndex >= currentDeck.size()) {
            return;
        }

        StudyFlashcard card = currentDeck.get(currentIndex);

        tvFlashcardEmoji.setText(card.visualHint != null ? card.visualHint : "📚");
        tvFlashcardConcept.setText(card.concept != null ? card.concept.toUpperCase() : "मूल अवधारणा");

        if (!isBackShowing) {
            // FRONT OF CARD
            tvCardSideBadge.setText("आगे (प्रश्न)");
            tvCardSideBadge.setBackgroundResource(R.drawable.bg_tab_unselected);
            tvCardSideBadge.setTextColor(ContextCompat.getColor(this, R.color.primary_forest_green));
            tvFlashcardMainText.setText(card.front);
            tvFlashcardSubText.setText("उत्तर देखने के लिए कार्ड पर टैप करें");
            tvTapHint.setText("पलटने के लिए टैप करें 🔄");
        } else {
            // BACK OF CARD
            tvCardSideBadge.setText("पीछे (उत्तर)");
            tvCardSideBadge.setBackgroundResource(R.drawable.bg_tab_selected);
            tvCardSideBadge.setTextColor(ContextCompat.getColor(this, R.color.white));
            tvFlashcardMainText.setText(card.back);
            tvFlashcardSubText.setText("मुख्य उत्तर व व्याख्या");
            tvTapHint.setText("वापस पलटने के लिए टैप करें 🔄");
        }

        tvCardCounter.setText("कार्ड " + (currentIndex + 1) + " / " + currentDeck.size());
        btnPrev.setEnabled(currentIndex > 0);
        btnPrev.setAlpha(currentIndex > 0 ? 1.0f : 0.4f);
        btnNext.setEnabled(currentIndex < currentDeck.size() - 1);
        btnNext.setAlpha(currentIndex < currentDeck.size() - 1 ? 1.0f : 0.4f);
    }

    private void showCreateDeckDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_flashcard_deck, null);
        Spinner spnClass = dialogView.findViewById(R.id.spnDeckClass);
        Spinner spnSubject = dialogView.findViewById(R.id.spnDeckSubject);
        Spinner spnTopic = dialogView.findViewById(R.id.spnDeckTopic);
        Spinner spnCards = dialogView.findViewById(R.id.spnDeckCards);

        // Populate options
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Class 3", "Class 1", "Class 2"});
        spnClass.setAdapter(classAdapter);

        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Environmental Studies (EVS)", "Mathematics", "Hindi"});
        spnSubject.setAdapter(subAdapter);

        List<String> topics = new ArrayList<>();
        topics.add("Plants Around Us");
        topics.add("Animals Around Us");
        topics.add("Water");
        topics.add("Our Environment");
        topics.add("Numbers and Addition");
        topics.add("Subtraction");
        topics.add("शब्द और वाक्य");

        ArrayAdapter<String> topicAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, topics);
        spnTopic.setAdapter(topicAdapter);

        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"10 Cards", "5 Cards"});
        spnCards.setAdapter(countAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Create Flashcard Deck")
                .setView(dialogView)
                .setPositiveButton("Create Deck", (dialog, which) -> {
                    String selectedTopic = (String) spnTopic.getSelectedItem();
                    int count = spnCards.getSelectedItemPosition() == 1 ? 5 : 10;
                    String selectedSub = spnSubject.getSelectedItemPosition() == 1 ? "Math" :
                            (spnSubject.getSelectedItemPosition() == 2 ? "Hindi" : "EVS");
                    String selectedCls = (String) spnClass.getSelectedItem();

                    loadDeck(selectedCls, selectedSub, selectedTopic, count);
                    Toast.makeText(this, "Created deck: " + selectedTopic, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AudioHelper.stopPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioHelper.stopPlayback();
    }
}
