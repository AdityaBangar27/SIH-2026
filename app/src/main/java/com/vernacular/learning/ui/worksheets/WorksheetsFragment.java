package com.vernacular.learning.ui.worksheets;

import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.WorksheetItem;
import com.vernacular.learning.data.models.study.StudyModels.StudyFlashcard;
import com.vernacular.learning.data.models.study.StudyModels.WorksheetData;
import com.vernacular.learning.data.repository.StudyMaterialRepository;
import com.vernacular.learning.ui.flashcards.FlashcardActivity;
import com.vernacular.learning.ui.worksheets.create.CreateWorksheetActivity;
import com.vernacular.learning.ui.worksheets.detail.WorksheetDetailActivity;
import com.vernacular.learning.utils.AudioHelper;

import java.util.ArrayList;
import java.util.List;

public class WorksheetsFragment extends Fragment {
    // Top Tabs
    private TextView tabWorksheets;
    private TextView tabFlashcards;

    // Content Containers
    private View layoutWorksheetsContent;
    private View layoutFlashcardsContent;

    // Worksheets Views
    private RecyclerView rvWorksheets;
    private LinearLayout layoutEmptyWorksheets;
    private MaterialButton btnCreateWorksheet;
    private WorksheetAdapter worksheetAdapter;
    private final List<WorksheetItem> worksheetItems = new ArrayList<>();
    private final List<WorksheetData> currentWorksheetDataList = new ArrayList<>();

    // Flashcards Views
    private MaterialCardView cardFlashcardMain;
    private LinearLayout flashcardControls;
    private LinearLayout layoutEmptyFlashcards;
    private TextView tvTabFlashcardVisual;
    private TextView tvFlashcardHindi;
    private TextView tvFlashcardTransliteration;
    private TextView tvFlashcardEnglish;
    private TextView tvFlashcardMotherTongue;
    private TextView tvCardCounter;
    private TextView tvTapHint;
    private ImageView btnPrevCard;
    private ImageView btnNextCard;
    private ImageView btnFlashcardAudio;
    private MaterialButton btnOpenFullFlashcardModule;

    // Flashcards State
    private final List<StudyFlashcard> flashcardList = new ArrayList<>();
    private int currentFlashcardIndex = 0;
    private boolean isTranslationRevealed = false;

    private StudyMaterialRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_worksheets, container, false);

        repository = StudyMaterialRepository.getInstance(requireContext());

        // Tabs
        tabWorksheets = root.findViewById(R.id.tabWorksheets);
        tabFlashcards = root.findViewById(R.id.tabFlashcards);

        // Content Containers
        layoutWorksheetsContent = root.findViewById(R.id.layoutWorksheetsContent);
        layoutFlashcardsContent = root.findViewById(R.id.layoutFlashcardsContent);

        // Worksheets Components
        rvWorksheets = root.findViewById(R.id.rvWorksheets);
        layoutEmptyWorksheets = root.findViewById(R.id.layoutEmptyWorksheets);
        btnCreateWorksheet = root.findViewById(R.id.btnCreateWorksheet);

        // Flashcards Components
        cardFlashcardMain = root.findViewById(R.id.cardFlashcardMain);
        flashcardControls = root.findViewById(R.id.flashcardControls);
        layoutEmptyFlashcards = root.findViewById(R.id.layoutEmptyFlashcards);
        tvTabFlashcardVisual = root.findViewById(R.id.tvTabFlashcardVisual);
        tvFlashcardHindi = root.findViewById(R.id.tvFlashcardHindi);
        tvFlashcardTransliteration = root.findViewById(R.id.tvFlashcardTransliteration);
        tvFlashcardEnglish = root.findViewById(R.id.tvFlashcardEnglish);
        tvFlashcardMotherTongue = root.findViewById(R.id.tvFlashcardMotherTongue);
        tvCardCounter = root.findViewById(R.id.tvCardCounter);
        tvTapHint = root.findViewById(R.id.tvTapHint);
        btnPrevCard = root.findViewById(R.id.btnPrevCard);
        btnNextCard = root.findViewById(R.id.btnNextCard);
        btnFlashcardAudio = root.findViewById(R.id.btnFlashcardAudio);
        btnOpenFullFlashcardModule = root.findViewById(R.id.btnOpenFullFlashcardModule);

        setupTabs();
        setupWorksheetsRecyclerView();
        setupFlashcardInteractions();

        btnCreateWorksheet.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateWorksheetActivity.class);
            startActivity(intent);
        });

        if (btnOpenFullFlashcardModule != null) {
            btnOpenFullFlashcardModule.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), FlashcardActivity.class);
                startActivity(intent);
            });
        }

        loadWorksheets();
        loadFlashcards();

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
        worksheetAdapter = new WorksheetAdapter(worksheetItems, (item, position) -> {
            if (position < currentWorksheetDataList.size()) {
                WorksheetData data = currentWorksheetDataList.get(position);
                Intent intent = new Intent(requireContext(), WorksheetDetailActivity.class);
                intent.putExtra("WORKSHEET_DATA", data);
                startActivity(intent);
            }
        });
        rvWorksheets.setAdapter(worksheetAdapter);
    }

    private void setupFlashcardInteractions() {
        cardFlashcardMain.setOnClickListener(v -> {
            isTranslationRevealed = !isTranslationRevealed;
            tvFlashcardMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
            tvTapHint.setText(isTranslationRevealed ? "वापस पलटने के लिए टैप करें 🔄" : "उत्तर देखने के लिए टैप करें 🔄");
        });

        btnFlashcardAudio.setOnClickListener(v -> {
            if (!flashcardList.isEmpty() && currentFlashcardIndex < flashcardList.size()) {
                StudyFlashcard card = flashcardList.get(currentFlashcardIndex);
                String textToRead = isTranslationRevealed ? card.back : card.front;
                AudioHelper.playPronunciation(requireContext(), textToRead, null);
            }
        });

        btnPrevCard.setOnClickListener(v -> {
            if (currentFlashcardIndex > 0) {
                currentFlashcardIndex--;
                isTranslationRevealed = false;
                displayFlashcard();
            }
        });

        btnNextCard.setOnClickListener(v -> {
            if (currentFlashcardIndex < flashcardList.size() - 1) {
                currentFlashcardIndex++;
                isTranslationRevealed = false;
                displayFlashcard();
            }
        });
    }

    private void loadWorksheets() {
        List<WorksheetData> all = repository.getAllWorksheets();
        currentWorksheetDataList.clear();
        currentWorksheetDataList.addAll(all);

        worksheetItems.clear();
        int idx = 1;
        for (WorksheetData ws : all) {
            String subtitle = ws.className + " • " + ws.subject + " • " + ws.questions.size() + " प्रश्न";
            int previewRes = R.drawable.ic_worksheet_quick;
            if (ws.subject != null && ws.subject.toLowerCase().contains("math")) {
                previewRes = R.drawable.ic_apples_three;
            } else if (ws.subject != null && ws.subject.toLowerCase().contains("evs")) {
                previewRes = R.drawable.ic_leaf_logo;
            }

            worksheetItems.add(new WorksheetItem(
                    idx++,
                    ws.title,
                    subtitle,
                    previewRes,
                    null,
                    true
            ));
        }

        if (worksheetItems.isEmpty()) {
            rvWorksheets.setVisibility(View.GONE);
            if (layoutEmptyWorksheets != null) layoutEmptyWorksheets.setVisibility(View.VISIBLE);
        } else {
            rvWorksheets.setVisibility(View.VISIBLE);
            if (layoutEmptyWorksheets != null) layoutEmptyWorksheets.setVisibility(View.GONE);
        }
        worksheetAdapter.notifyDataSetChanged();
    }

    private void loadFlashcards() {
        flashcardList.clear();
        List<StudyFlashcard> cards = repository.getFlashcardsForTopic("Class 3", "EVS", "Plants Around Us", 10);
        if (cards != null) {
            flashcardList.addAll(cards);
        }
        currentFlashcardIndex = 0;
        isTranslationRevealed = false;
        displayFlashcard();
    }

    private void displayFlashcard() {
        if (flashcardList.isEmpty() || currentFlashcardIndex >= flashcardList.size()) {
            cardFlashcardMain.setVisibility(View.GONE);
            flashcardControls.setVisibility(View.GONE);
            if (layoutEmptyFlashcards != null) layoutEmptyFlashcards.setVisibility(View.VISIBLE);
            return;
        }

        cardFlashcardMain.setVisibility(View.VISIBLE);
        flashcardControls.setVisibility(View.VISIBLE);
        if (layoutEmptyFlashcards != null) layoutEmptyFlashcards.setVisibility(View.GONE);

        StudyFlashcard card = flashcardList.get(currentFlashcardIndex);
        if (tvTabFlashcardVisual != null) {
            tvTabFlashcardVisual.setText(card.visualHint != null ? card.visualHint : "🌱");
        }
        tvFlashcardHindi.setText(card.front != null ? card.front : "");
        tvFlashcardTransliteration.setText(card.concept != null ? card.concept.toUpperCase() : "मूल अवधारणा");
        tvFlashcardMotherTongue.setText(card.back != null ? card.back : "");
        tvFlashcardMotherTongue.setVisibility(isTranslationRevealed ? View.VISIBLE : View.INVISIBLE);
        tvTapHint.setText(isTranslationRevealed ? "वापस पलटने के लिए टैप करें 🔄" : "उत्तर देखने के लिए टैप करें 🔄");
        tvCardCounter.setText((currentFlashcardIndex + 1) + " / " + flashcardList.size());
    }

    private int getThemeColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(requireContext(), R.color.white);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWorksheets();
    }
}
