package com.vernacular.learning.ui.worksheets;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.local.entities.VocabularyEntity;
import com.vernacular.learning.data.local.entities.WorksheetEntity;
import com.vernacular.learning.data.models.WorksheetItem;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.utils.AudioHelper;
import java.util.ArrayList;
import java.util.List;

public class WorksheetsFragment extends Fragment {
    // Top Tabs
    private TextView tabWorksheets;
    private TextView tabFlashcards;

    // Content Containers (In-Page Toggle)
    private View layoutWorksheetsContent;
    private View layoutFlashcardsContent;

    // Worksheets Views
    private RecyclerView rvWorksheets;
    private LinearLayout layoutEmptyWorksheets;
    private WorksheetAdapter worksheetAdapter;
    private final List<WorksheetItem> worksheetItems = new ArrayList<>();

    // Flashcards Views
    private MaterialCardView cardFlashcardMain;
    private LinearLayout flashcardControls;
    private LinearLayout layoutEmptyFlashcards;
    private ImageView ivFlashcardVisual;
    private TextView tvFlashcardHindi;
    private TextView tvFlashcardTransliteration;
    private TextView tvFlashcardEnglish;
    private TextView tvFlashcardMotherTongue;
    private TextView tvCardCounter;
    private TextView tvTapHint;
    private ImageView btnPrevCard;
    private ImageView btnNextCard;
    private ImageView btnFlashcardAudio;

    // Flashcards State
    private final List<VocabularyEntity> vocabularyList = new ArrayList<>();
    private int currentFlashcardIndex = 0;
    private boolean isTranslationRevealed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_worksheets, container, false);

        // Tabs
        tabWorksheets = root.findViewById(R.id.tabWorksheets);
        tabFlashcards = root.findViewById(R.id.tabFlashcards);

        // Content Containers
        layoutWorksheetsContent = root.findViewById(R.id.layoutWorksheetsContent);
        layoutFlashcardsContent = root.findViewById(R.id.layoutFlashcardsContent);

        // Worksheets Components
        rvWorksheets = root.findViewById(R.id.rvWorksheets);
        layoutEmptyWorksheets = root.findViewById(R.id.layoutEmptyWorksheets);

        // Flashcards Components
        cardFlashcardMain = root.findViewById(R.id.cardFlashcardMain);
        flashcardControls = root.findViewById(R.id.flashcardControls);
        layoutEmptyFlashcards = root.findViewById(R.id.layoutEmptyFlashcards);
        ivFlashcardVisual = root.findViewById(R.id.ivFlashcardVisual);
        tvFlashcardHindi = root.findViewById(R.id.tvFlashcardHindi);
        tvFlashcardTransliteration = root.findViewById(R.id.tvFlashcardTransliteration);
        tvFlashcardEnglish = root.findViewById(R.id.tvFlashcardEnglish);
        tvFlashcardMotherTongue = root.findViewById(R.id.tvFlashcardMotherTongue);
        tvCardCounter = root.findViewById(R.id.tvCardCounter);
        tvTapHint = root.findViewById(R.id.tvTapHint);
        btnPrevCard = root.findViewById(R.id.btnPrevCard);
        btnNextCard = root.findViewById(R.id.btnNextCard);
        btnFlashcardAudio = root.findViewById(R.id.btnFlashcardAudio);

        setupTabs();
        setupWorksheetsRecyclerView();
        setupFlashcardInteractions();

        observeWorksheets();
        observeVocabulary();

        return root;
    }

    private void setupTabs() {
        tabWorksheets.setOnClickListener(v -> selectWorksheetsTab());
        tabFlashcards.setOnClickListener(v -> selectFlashcardsTab());
    }

    private void selectWorksheetsTab() {
        tabWorksheets.setBackgroundResource(R.drawable.bg_tab_selected);
        tabWorksheets.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tabFlashcards.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabFlashcards.setTextColor(getThemeColor(R.attr.customTextSecondaryColor));

        if (layoutWorksheetsContent != null) layoutWorksheetsContent.setVisibility(View.VISIBLE);
        if (layoutFlashcardsContent != null) layoutFlashcardsContent.setVisibility(View.GONE);
    }

    private void selectFlashcardsTab() {
        tabFlashcards.setBackgroundResource(R.drawable.bg_tab_selected);
        tabFlashcards.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tabWorksheets.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabWorksheets.setTextColor(getThemeColor(R.attr.customTextSecondaryColor));

        if (layoutWorksheetsContent != null) layoutWorksheetsContent.setVisibility(View.GONE);
        if (layoutFlashcardsContent != null) layoutFlashcardsContent.setVisibility(View.VISIBLE);

        displayFlashcard();
    }

    private void setupWorksheetsRecyclerView() {
        rvWorksheets.setLayoutManager(new LinearLayoutManager(requireContext()));
        worksheetAdapter = new WorksheetAdapter(worksheetItems);
        rvWorksheets.setAdapter(worksheetAdapter);
    }

    private void setupFlashcardInteractions() {
        // Tap to reveal mother tongue translation
        cardFlashcardMain.setOnClickListener(v -> {
            isTranslationRevealed = !isTranslationRevealed;
            tvFlashcardMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
            tvTapHint.setText(isTranslationRevealed ? "Tap again to hide" : getString(R.string.tap_to_reveal));
        });

        // Audio pronunciation
        btnFlashcardAudio.setOnClickListener(v -> {
            if (!vocabularyList.isEmpty() && currentFlashcardIndex < vocabularyList.size()) {
                VocabularyEntity card = vocabularyList.get(currentFlashcardIndex);
                AudioHelper.playPronunciation(requireContext(), card.word, null);
            }
        });

        // Previous Card
        btnPrevCard.setOnClickListener(v -> {
            if (currentFlashcardIndex > 0) {
                currentFlashcardIndex--;
                isTranslationRevealed = false;
                displayFlashcard();
            }
        });

        // Next Card
        btnNextCard.setOnClickListener(v -> {
            if (currentFlashcardIndex < vocabularyList.size() - 1) {
                currentFlashcardIndex++;
                isTranslationRevealed = false;
                displayFlashcard();
            }
        });
    }

    private void observeWorksheets() {
        LearningRepository.getInstance(requireContext())
                .getAllWorksheetsLiveData()
                .observe(getViewLifecycleOwner(), entities -> {
                    worksheetItems.clear();
                    if (entities != null && !entities.isEmpty()) {
                        int index = 1;
                        for (WorksheetEntity entity : entities) {
                            String subtitle = (entity.className != null ? entity.className : "") +
                                    (entity.subject != null ? " • " + entity.subject : "");
                            boolean isDownloaded = "DOWNLOADED".equalsIgnoreCase(entity.downloadStatus);
                            worksheetItems.add(new WorksheetItem(
                                    index++,
                                    entity.title != null ? entity.title : "Worksheet",
                                    subtitle,
                                    0,
                                    null,
                                    isDownloaded
                            ));
                        }
                        worksheetAdapter.notifyDataSetChanged();
                        rvWorksheets.setVisibility(View.VISIBLE);
                        if (layoutEmptyWorksheets != null) {
                            layoutEmptyWorksheets.setVisibility(View.GONE);
                        }
                    } else {
                        worksheetAdapter.notifyDataSetChanged();
                        rvWorksheets.setVisibility(View.GONE);
                        if (layoutEmptyWorksheets != null) {
                            layoutEmptyWorksheets.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void observeVocabulary() {
        LearningRepository.getInstance(requireContext())
                .getAllVocabularyLiveData()
                .observe(getViewLifecycleOwner(), list -> {
                    vocabularyList.clear();
                    if (list != null && !list.isEmpty()) {
                        vocabularyList.addAll(list);
                        currentFlashcardIndex = 0;
                        isTranslationRevealed = false;
                    }
                    displayFlashcard();
                });
    }

    private void displayFlashcard() {
        if (vocabularyList.isEmpty() || currentFlashcardIndex >= vocabularyList.size()) {
            cardFlashcardMain.setVisibility(View.GONE);
            flashcardControls.setVisibility(View.GONE);
            if (layoutEmptyFlashcards != null) layoutEmptyFlashcards.setVisibility(View.VISIBLE);
            return;
        }

        cardFlashcardMain.setVisibility(View.VISIBLE);
        flashcardControls.setVisibility(View.VISIBLE);
        if (layoutEmptyFlashcards != null) layoutEmptyFlashcards.setVisibility(View.GONE);

        VocabularyEntity card = vocabularyList.get(currentFlashcardIndex);
        ivFlashcardVisual.setImageResource(R.drawable.ic_language);
        tvFlashcardHindi.setText(card.word != null ? card.word : "");
        tvFlashcardTransliteration.setText(card.transliteration != null ? card.transliteration : "");
        tvFlashcardEnglish.setText(card.meaning != null ? card.meaning : "");
        tvFlashcardMotherTongue.setText(card.meaning != null ? card.meaning : "");
        tvFlashcardMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
        tvTapHint.setText(getString(R.string.tap_to_reveal));
        tvCardCounter.setText((currentFlashcardIndex + 1) + " / " + vocabularyList.size());
    }

    private int getThemeColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(requireContext(), R.color.white);
    }
}
