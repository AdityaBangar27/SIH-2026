package com.vernacular.learning.ui.worksheets;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.WorksheetItem;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.ui.flashcards.FlashcardActivity;
import java.util.List;

public class WorksheetsFragment extends Fragment {
    private TextView tabWorksheets;
    private TextView tabFlashcards;
    private RecyclerView rvWorksheets;
    private WorksheetAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_worksheets, container, false);

        ImageView btnBack = root.findViewById(R.id.btnWorksheetsBack);
        tabWorksheets = root.findViewById(R.id.tabWorksheets);
        tabFlashcards = root.findViewById(R.id.tabFlashcards);
        rvWorksheets = root.findViewById(R.id.rvWorksheets);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        setupTabs();
        setupRecyclerView();

        return root;
    }

    private void setupTabs() {
        tabWorksheets.setOnClickListener(v -> selectWorksheetsTab());

        tabFlashcards.setOnClickListener(v -> {
            // Open dedicated full Flashcard screen matching reference
            Intent intent = new Intent(requireContext(), FlashcardActivity.class);
            startActivity(intent);
        });
    }

    private void selectWorksheetsTab() {
        tabWorksheets.setBackgroundResource(R.drawable.bg_tab_selected);
        tabWorksheets.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tabFlashcards.setBackgroundResource(R.drawable.bg_tab_unselected);
    }

    private void setupRecyclerView() {
        rvWorksheets.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<WorksheetItem> items = LearningRepository.getInstance(requireContext()).getSampleWorksheets();
        adapter = new WorksheetAdapter(items);
        rvWorksheets.setAdapter(adapter);
    }
}
