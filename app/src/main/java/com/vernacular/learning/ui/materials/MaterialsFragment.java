package com.vernacular.learning.ui.materials;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.MaterialItem;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.ui.lesson.LessonActivity;
import java.util.ArrayList;
import java.util.List;

public class MaterialsFragment extends Fragment {
    private List<MaterialItem> allMaterials;
    private final List<MaterialItem> filteredMaterials = new ArrayList<>();
    private MaterialAdapter adapter;
    private LinearLayout layoutEmpty;
    private String currentCategory = "All";
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_materials, container, false);

        EditText etSearch = root.findViewById(R.id.etSearchMaterials);
        ChipGroup chipGroup = root.findViewById(R.id.chipGroupFilters);
        RecyclerView rvMaterials = root.findViewById(R.id.rvMaterials);
        layoutEmpty = root.findViewById(R.id.layoutEmptyMaterials);

        allMaterials = LearningRepository.getInstance(requireContext()).getSampleMaterials();
        filteredMaterials.addAll(allMaterials);

        rvMaterials.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MaterialAdapter(filteredMaterials, item -> {
            Intent intent = new Intent(requireContext(), LessonActivity.class);
            intent.putExtra("CLASS_NAME", "Class 1");
            intent.putExtra("SUBJECT_NAME", item.category);
            startActivity(intent);
        });
        rvMaterials.setAdapter(adapter);

        // Search text watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter chips
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipMath) {
                currentCategory = "Mathematics";
            } else if (checkedId == R.id.chipLanguages) {
                currentCategory = "Languages";
            } else if (checkedId == R.id.chipScience) {
                currentCategory = "Science";
            } else {
                currentCategory = "All";
            }
            applyFilter();
        });

        return root;
    }

    private void applyFilter() {
        filteredMaterials.clear();
        for (MaterialItem item : allMaterials) {
            boolean matchesCat = currentCategory.equals("All") || item.category.equalsIgnoreCase(currentCategory);
            boolean matchesQuery = currentQuery.isEmpty() ||
                    item.title.toLowerCase().contains(currentQuery) ||
                    item.subtitle.toLowerCase().contains(currentQuery);

            if (matchesCat && matchesQuery) {
                filteredMaterials.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        layoutEmpty.setVisibility(filteredMaterials.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
