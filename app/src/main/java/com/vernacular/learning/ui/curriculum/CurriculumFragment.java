package com.vernacular.learning.ui.curriculum;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.study.StudyModels.CurriculumChapter;
import com.vernacular.learning.data.repository.StudyMaterialRepository;
import com.vernacular.learning.ui.curriculum.detail.StudyMaterialDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class CurriculumFragment extends Fragment {

    private TextView tvActiveGradeBadge;
    private ChipGroup chipGroupClasses;
    private ChipGroup chipGroupSubjects;
    private TextView tvChaptersHeader;
    private TextView tvChapterCount;
    private RecyclerView rvChapters;
    private LinearLayout layoutEmptyCurriculum;

    private String selectedClass = "Class 3";
    private String selectedSubject = "EVS";
    private final List<CurriculumChapter> currentChapters = new ArrayList<>();
    private CurriculumAdapter adapter;
    private StudyMaterialRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_curriculum, container, false);

        tvActiveGradeBadge = root.findViewById(R.id.tvActiveGradeBadge);
        chipGroupClasses = root.findViewById(R.id.chipGroupClasses);
        chipGroupSubjects = root.findViewById(R.id.chipGroupSubjects);
        tvChaptersHeader = root.findViewById(R.id.tvChaptersHeader);
        tvChapterCount = root.findViewById(R.id.tvChapterCount);
        rvChapters = root.findViewById(R.id.rvChapters);
        layoutEmptyCurriculum = root.findViewById(R.id.layoutEmptyCurriculum);

        repository = StudyMaterialRepository.getInstance(requireContext());

        setupRecyclerView();
        setupFilters();
        loadChapters();

        return root;
    }

    private void setupRecyclerView() {
        rvChapters.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CurriculumAdapter(currentChapters, chapter -> {
            Intent intent = new Intent(requireContext(), StudyMaterialDetailActivity.class);
            intent.putExtra("CLASS_NAME", selectedClass);
            intent.putExtra("SUBJECT_NAME", selectedSubject);
            intent.putExtra("TOPIC_NAME", chapter.title);
            startActivity(intent);
        });
        rvChapters.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupClasses.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipClass1) {
                selectedClass = "Class 1";
            } else if (checkedId == R.id.chipClass2) {
                selectedClass = "Class 2";
            } else {
                selectedClass = "Class 3";
            }
            tvActiveGradeBadge.setText("Class 3".equals(selectedClass) ? "कक्षा 3" :
                    ("Class 1".equals(selectedClass) ? "कक्षा 1" : "कक्षा 2"));
            loadChapters();
        });

        chipGroupSubjects.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipSubjectMath) {
                selectedSubject = "Math";
            } else if (checkedId == R.id.chipSubjectHindi) {
                selectedSubject = "Hindi";
            } else {
                selectedSubject = "EVS";
            }
            loadChapters();
        });
    }

    private void loadChapters() {
        currentChapters.clear();
        List<CurriculumChapter> chapters = repository.getChaptersForSubject(selectedClass, selectedSubject);
        if (chapters != null && !chapters.isEmpty()) {
            currentChapters.addAll(chapters);
            rvChapters.setVisibility(View.VISIBLE);
            layoutEmptyCurriculum.setVisibility(View.GONE);
            tvChapterCount.setText(currentChapters.size() + " अध्याय");
        } else {
            rvChapters.setVisibility(View.GONE);
            layoutEmptyCurriculum.setVisibility(View.VISIBLE);
            tvChapterCount.setText("0 अध्याय");
        }
        adapter.notifyDataSetChanged();
    }
}
